package de.yard.owm.services.mesh;

import de.yard.owm.configuration.ApplicationContextHolder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.owm.services.persistence.*;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.LatLon;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.MeshServiceFacade;
import de.yard.threed.osm2scenery.polygon20.*;
import de.yard.threed.osm2scenery.scenery.LandClass;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.JTSConversionUtil;
import de.yard.threed.osm2world.MapWaySegmentAtConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 *
 */
@Service
@Slf4j
public class MeshService /*1.4.26 implements MeshServiceFacade, see below */ {

    @Autowired
    TerrainMeshManager terrainMeshManager;

    @Autowired
    MeshPolygonRepository meshPolygonRepository;

    @Autowired
    MeshNodeRepository meshNodeRepository;

    @Autowired
    MeshRepository meshRepository;

    @Autowired
    MeshNodePairRepository meshNodePairRepository;

    @Autowired
    MeshFailureRepository meshFailureRepository;

    @Autowired
    ApplicationContextHolder applicationContextHolder;

    /**
     * 13.2.26 GeoCoordinate instead of LatLon because we want to consider elevation from the very beginning.
     *
     * @param name
     * @param boundary
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createMesh(String name, List<GeoCoordinate> boundary) {
        PersistedMesh mesh = new PersistedMesh(name);
        meshRepository.save(mesh);

        PersistedMeshPolygon meshPolygon = new PersistedMeshPolygon();
        meshPolygon.setType(MeshPolygonType.BOUNDARY);
        meshPolygon.setMesh(mesh);
        meshPolygonRepository.save(meshPolygon);
        PersistedMeshNode n;
        MeshLine l;
        for (int i = 0; i < boundary.size(); i++) {
            n = buildMeshNode(boundary.get(i), mesh);
            meshPolygon.addNode(n);
        }
        // close polygon
        meshPolygon.close();
    }

    /**
     *
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TerrainMesh loadMesh(String meshName) throws MeshInconsistencyException {

        long startTime = System.currentTimeMillis();

        PersistedMesh mesh = meshRepository.findByName(meshName);
        if (mesh == null) {
            return null;
        }

        List<MeshNode> points = new ArrayList();
        List<MeshPolygon> polygons = new ArrayList();
        List<GeoCoordinate> boundary = null;

        // Reading nodes independent from lines leads to doubled instances. Strange(?).
        // TODO filter mesh inside grid
        /*meshNodeRepository.findAll().forEach(n -> {
            n.projection = gridCellBounds.getProjection().getBaseProjection();
            terrainMesh.points.add(n);
        });*/
        // Result is sorted by id and index
        Iterator<PersistedMeshPolygon> ps = meshPolygonRepository.findByMesh(mesh).iterator();
        while (ps.hasNext()) {
            PersistedMeshPolygon p = ps.next();

            polygons.add(p);
            if (p.getType() == MeshPolygonType.BOUNDARY) {
                boundary = p.getGeoCoordinates();
            }
            /*((PersistedMeshNode) l.getNodes()()).linesOfPoint.add(l);
            if (!terrainMesh.points.contains(l.getFrom())) {
                terrainMesh.points.add(l.getFrom());
                //((PersistedMeshNode)l.getFrom()).projection=gridCellBounds.getProjection().getBaseProjection();
            }
            ((PersistedMeshNode) l.getTo()).linesOfPoint.add(l);*/

            // Collecting the points has no significant impact on performance.
            p.getNodesSortedByIndex().forEach(n -> {

                if (!points.contains(n)) {
                    points.add(n);
                    //((PersistedMeshNode)l.getTo()).projection=gridCellBounds.getProjection().getBaseProjection();
                }
            });
        }
        log.trace("Loaded {} polygons of mesh. Took {} ms.", polygons.size(), System.currentTimeMillis() - startTime);

        if (boundary == null) {
            throw new MeshInconsistencyException("no mesh boundary found");
        }
        GridCellBounds gridCellBounds = new GridCellBounds(boundary);

        TerrainMesh terrainMesh = TerrainMesh.init(gridCellBounds, points, polygons);
        //terrainMesh.meshService = buildMeshServiceFacade();
        terrainMesh.meshName = meshName;
        // TODO make sure to have full outline in mesh(??)
        terrainMesh.points.forEach(p -> {
            PersistedMeshNode pn = (PersistedMeshNode) p;
            pn.coordinate = gridCellBounds.getProjection().getBaseProjection().project(pn.getGeoCoordinate());
        });

        terrainMesh.failures = new ArrayList<>();
        for (PersistedMeshFailure meshFailure : mesh.getFailures()) {
            terrainMesh.failures.add(meshFailure);
        }
        log.debug("Loaded mesh {} with {} polygons and {} nodes. Took {} ms.", meshName, polygons.size(), points.size(), System.currentTimeMillis() - startTime);
        return terrainMesh;
    }

    /**
     * 5.4.24: Three additional register instead of registerLine, which isn't ready for polygons.
     * lanes is used leter to detect ways for triangulateAndTexturize
     * connector might be null, otherwise the connecting part must have been created before.
     * 6.4.24: Well, maybe its easier to keep existing registerLine for a while?
     * 11.2.26 Moved here from TerrainMesh.registerWay()
     *
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TerrainMesh addWay(String meshName, long osmWayId, Pair<GeoCoordinate, GeoCoordinate> fromConnector, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine, Pair<GeoCoordinate, GeoCoordinate> toConnector, int lanes, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException {

        long startTime = System.currentTimeMillis();

        List<GeoCoordinate> outline = buildWayOutline(leftLine, rightLine);
        if (!areasToAdjust.isEmpty()) {
            log.debug("Way {} overlaps {} area(s) that need to be adjusted", osmWayId, areasToAdjust.size());
            List<GeoCoordinate> closedOutline = new ArrayList<>(outline);
            closedOutline.add(outline.get(0));
            GeoPolygon wayPolygon = new GeoPolygon(closedOutline);
            for (MeshPolygon area : areasToAdjust) {
                adjustPolygon(((PersistedMeshPolygon) area).getId(), wayPolygon);
            }
        }

        TerrainMesh terrainMesh = addPolygon(meshName, osmWayId, MeshPolygonType.WAY, landClass, outline.iterator());
        log.debug("Added way {} to mesh. Took {} ms.", osmWayId, System.currentTimeMillis() - startTime);
        return terrainMesh;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TerrainMesh addArea(String meshName, long osmId, MeshPolygonType type, List<GeoCoordinate> geoCoordinates, LandClass landClass) throws MeshInconsistencyException {
        long startTime = System.currentTimeMillis();
        TerrainMesh terrainMesh = addPolygon(meshName, osmId, type, landClass, geoCoordinates.iterator());
        log.debug("Added area {} of type {} to mesh. Took {} ms.", osmId, type, System.currentTimeMillis() - startTime);
        return terrainMesh;
    }

    /**
     * If we decide to maintain background, adding a polygon also requires a cut of background.
     */
    private TerrainMesh addPolygon(String meshName, long osmId, MeshPolygonType type, LandClass landClass, Iterator<GeoCoordinate> nodeProvider) throws MeshInconsistencyException {
        PersistedMesh mesh = meshRepository.findByName(meshName);
        if (mesh == null) {
            throw new RuntimeException("mesh not found:" + meshName);
        }
        PersistedMeshPolygon meshPolygon = buildPersistedMeshPolygon(mesh, osmId, type, landClass, nodeProvider);
        meshPolygonRepository.save(meshPolygon);

        TerrainMesh terrainMesh = loadMesh(meshName);

        boolean useBackground = false;
        if (useBackground){
            // Cut background. Even in 2026 maintaining background is really complex.
            // For now postponed to tile generation. See README.md.
        }

        // Now validate and rollback in case of failure (exception)
        terrainMesh.validate();
        // no longer connect ot background mesh
        return terrainMesh;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TerrainMesh addConnector(String meshName, long osmNodeId, List<Pair<GeoCoordinate, Long>> polygon, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException {

        log.debug("Adding connector {} with {} nodes and {} attach pairs to mesh", osmNodeId, polygon.size() - 1, wayAttachPoints.size());

        // equals is not yet implemented in GeoCoordinate, so this is ...???
        if (!polygon.get(0).getFirst().equals(polygon.get(polygon.size() - 1).getFirst())) {
            throw new MeshInconsistencyException("polygon not closed");
        }

        if (!areasToAdjust.isEmpty()) {
            log.debug("Connector {} overlaps {} area(s) that need to be adjusted", osmNodeId, areasToAdjust.size());
            GeoPolygon connectorPolygon = new GeoPolygon(polygon.stream().map(Pair::getFirst).toList());
            for (MeshPolygon area : areasToAdjust) {
                adjustPolygon(((PersistedMeshPolygon) area).getId(), connectorPolygon);
            }
        }

        PersistedMesh mesh = meshRepository.findByName(meshName);
        if (mesh == null) {
            throw new RuntimeException("mesh not found:" + meshName);
        }

        PersistedMeshPolygon meshPolygon = new PersistedMeshPolygon();
        meshPolygon.setType(MeshPolygonType.CONNECTOR);
        meshPolygon.setLandClass(landClass);
        meshPolygon.setOsmId(osmNodeId);
        meshPolygon.setMesh(mesh);

        List<PersistedMeshNode> points = new ArrayList<>();
        PersistedMeshNode n;
        MeshLine l;
        // Skip closing node but use close() afterwards
        for (int i = 0; i < polygon.size() - 1; i++) {
            n = buildMeshNode(polygon.get(i).getFirst(), mesh);
            meshPolygon.addNode(n);
        }
        //incoming already is closed. But we need to be sure to reuse the same start node at end
        meshPolygon.close();

        // Be sure it is a valid polygon
        if (!meshPolygon.isValidForSave()) {
            throw new MeshInconsistencyException("connector polygon not valid for osmNodeId " + osmNodeId);
        }
        meshPolygonRepository.save(meshPolygon);

        for (MapWaySegmentAtConnector waySegment : wayAttachPoints.keySet()) {
            Pair<Integer, Integer> p = wayAttachPoints.get(waySegment);
            PersistedMeshNodePair np = new PersistedMeshNodePair();
            np.setRight(/*(PersistedMeshNode) meshPolygon.getNodes().get*/(p.getFirst()));
            np.setLeft(/*(PersistedMeshNode) meshPolygon.getNodes().get*/(p.getSecond()));
            np.setOsmId(waySegment.getWayOsmId());
            np.setHeading(waySegment.getHeadingAtconnector().getDegree());
            //TODO np.setOppositeNodeOsmId(waySegment.getOppositeNodeOsmId(osmNodeId));
            np.setMeshPolygon(meshPolygon);
            meshNodePairRepository.save(np);
        }
        // No validate and rollback in case of failure (exception)
        TerrainMesh terrainMesh = loadMesh(meshName);
        terrainMesh.validate();
        return terrainMesh;
    }

    /**
     * Returns false when not found.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deleteMesh(String meshName) {

        log.debug("Delete mesh {}", meshName);
        PersistedMesh mesh = meshRepository.findByName(meshName);
        if (mesh == null) {
            return false;
        }

        // Nodes might be shared, so cannot be deleted cascade. But more critical: NOT NULL constraints in Postgres are not deferred. So doing a cascade delete is hard,
        // so better do it step by step
        meshFailureRepository.deleteByPersistedMesh(mesh);
        meshPolygonRepository.deleteByMesh(mesh);
        meshNodeRepository.deleteByPersistedMesh(mesh);
        // who deletes meshPolygonNodes?
        meshRepository.delete(mesh);


        return true;
    }

    /**
     * Removes all mesh polygons (way/area, possibly split into several segments) that were derived
     * from the given OSM element. Used to retry building a single OSM way/area: the previous result
     * (or partial result) needs to be dropped before it can be built again.
     * Connectors are kept, as they may be shared with other ways and are reused on rebuild.
     * Returns the number of deleted polygons.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deletePolygon(long osmId) {

        List<PersistedMeshPolygon> polygons = meshPolygonRepository.findByOsmId(osmId);
        polygons.removeIf(p -> p.getType() == MeshPolygonType.CONNECTOR || p.getType() == MeshPolygonType.BOUNDARY);

        log.debug("Deleting {} polygon(s) for osmId {}", polygons.size(), osmId);
        for (PersistedMeshPolygon p : polygons) {
            // keep the mesh relation in sync so a reload within the same TX doesn't see the stale polygon
            p.getMesh().getPolygons().remove(p);
            meshPolygonRepository.delete(p);
        }
        return polygons.size();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MeshFailure addFailure(String meshName, String message, String sourceRef, GeoPolygon geoPolygon, String svg) {
        PersistedMesh mesh = meshRepository.findByName(meshName);
        if (mesh == null) {
            return null;
        }
        PersistedMeshFailure failure = new PersistedMeshFailure();
        failure.setMessage(message);
        failure.setSourceRef(sourceRef);
        // Use WKT with geo corrinates
        if (geoPolygon != null) {
            failure.setPolygon(geoPolygon.toText());
        }
        failure.setSvg(svg);
        failure.setPersistedMesh(mesh);
        return meshFailureRepository.save(failure);
    }

    /**
     * Returns the SVG (a full standalone SVG document) attached to a failure, or null if the
     * failure does not exist or has no SVG. The SVG is intentionally not part of the mesh response
     * and only retrievable one at a time via this method.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public String loadFailureSvg(Long failureId) {
        return meshFailureRepository.findById(failureId).map(PersistedMeshFailure::getSvg).orElse(null);
    }

    /**
     * Returns the polygons of a mesh that cover the given geo location (including polygons having the location
     * on their boundary, so a location on a shared edge returns both neighbours).
     * The mesh BOUNDARY polygon is not included, because it trivially covers everything inside the mesh.
     * Returns null if the mesh does not exist.
     * Uses a DB bounding box pre-filter followed by an exact JTS test on the (unprojected) GeoPolygon.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<MeshPolygon> findPolygonsAt(String meshName, LatLon latLon) {
        PersistedMesh mesh = meshRepository.findByName(meshName);
        if (mesh == null) {
            return null;
        }
        double lat = latLon.getLatDeg().getDegree();
        double lon = latLon.getLonDeg().getDegree();
        // GeoPolygon coordinates are x=lon, y=lat
        Point point = JTSConversionUtil.GF.createPoint(new Coordinate(lon, lat));

        List<Long> candidateIds = meshPolygonRepository.findCandidatePolygonIds(mesh, lat, lon);
        List<MeshPolygon> result = new ArrayList<>();
        for (PersistedMeshPolygon p : meshPolygonRepository.findAllById(candidateIds)) {
            if (p.getType() == MeshPolygonType.BOUNDARY) {
                continue;
            }
            GeoPolygon geoPolygon = p.getGeoPolygon();
            // null when inconsistent, which is already logged
            if (geoPolygon != null && geoPolygon.covers(point)) {
                result.add(p);
            }
        }
        result.sort(Comparator.comparing(p -> ((PersistedMeshPolygon) p).getId()));
        log.debug("Found {} polygon(s) of {} candidate(s) at {},{} in mesh {}", result.size(), candidateIds.size(), lat, lon, meshName);
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MeshPolygon/*Connector*/ getConnector(long osmNodeId) {
        // TODO also find by type CONNECTOR
        List<PersistedMeshPolygon> ps = meshPolygonRepository.findByOsmId(osmNodeId);
        if (ps.isEmpty()) {
            return null;
        }
        PersistedMeshPolygon persistedMeshPolygon = ps.get(0);
        return persistedMeshPolygon;
    }

    /**
     * Returns the polygons of type AREA that overlap the polygon a way with these outlines would get in addWay().
     * Areas only touching the way are not included. The mesh isn't modified.
     * Uses a DB bounding box pre-filter followed by the same JTS overlap test as the mesh validation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<MeshPolygon> findAreaIntersectionsOfWay(String meshName, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine) throws MeshInconsistencyException {
        List<GeoCoordinate> outline = buildWayOutline(leftLine, rightLine);
        outline.add(outline.get(0));
        return findAreaIntersections(meshName, new GeoPolygon(outline));
    }

    /**
     * Like findAreaIntersections() for a way, but for a connector polygon (closed, like in addConnector()).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<MeshPolygon> findAreaIntersectionsOfArea(String meshName, List<Pair<GeoCoordinate, Long>> polygon) throws MeshInconsistencyException {
        return findAreaIntersections(meshName, new GeoPolygon(polygon.stream().map(Pair::getFirst).toList()));
    }

    public PersistedMeshNode buildMeshNode(GeoCoordinate coordinate, PersistedMesh mesh) {
        PersistedMeshNode newNode = new PersistedMeshNode(coordinate);
        newNode.setPersistedMesh(mesh);
        //newNode.setPersistedMesh(persistedMesh);
        // persist it to give it an id which is needed for equals.
        //terrainMeshManager.persistNode(newNode);
        meshNodeRepository.save(newNode);
        return newNode;
    }

    /**
     * 1.4.26 Needed because pure interface breaks Spring bean (eg. transaction boundaries)
     */
    public static MeshServiceFacade buildMeshServiceFacade() {
        return new MeshServiceFacade() {
            MeshService ms = (MeshService) ApplicationContextHolder.getBean(MeshService.class);

            @Override
            public void createMesh(String meshName, List<GeoCoordinate> boundary) {
                ms.createMesh(meshName, boundary);
            }

            @Override
            public TerrainMesh addWay(String meshName, long osmWayId, Pair<GeoCoordinate, GeoCoordinate> fromConnector, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine, Pair<GeoCoordinate, GeoCoordinate> toConnector, int lanes, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException {
                return ms.addWay(meshName, osmWayId, fromConnector, leftLine, rightLine, toConnector, lanes, landClass, areasToAdjust);
            }

            @Override
            public List<MeshPolygon> findAreaIntersectionsOfWay(String meshName, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine) throws MeshInconsistencyException {
                return ms.findAreaIntersectionsOfWay(meshName, leftLine, rightLine);
            }

            @Override
            public List<MeshPolygon> findAreaIntersectionsOfArea(String meshName, List<Pair<GeoCoordinate, Long>> polygon) throws MeshInconsistencyException {
                return ms.findAreaIntersectionsOfArea(meshName, polygon);
            }

            @Override
            public TerrainMesh addConnector(String meshName, long osmNodeId, List<Pair<GeoCoordinate, Long>> polygon, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException {
                return ms.addConnector(meshName, osmNodeId, polygon, wayAttachPoints, landClass, areasToAdjust);
            }

            @Override
            public TerrainMesh loadMesh(String meshName) throws MeshInconsistencyException {
                return ms.loadMesh(meshName);
            }

            @Override
            public MeshFailure addFailure(String meshName, String message, String sourceref, GeoPolygon polygon, String svg) {
                return ms.addFailure(meshName, message, sourceref, polygon, svg);
            }

            @Override
            public MeshPolygon/*WayConnector*/ getConnector(long osmNodeId) {
                return ms.getConnector(osmNodeId);
            }

            @Override
            public TerrainMesh addArea(String meshName, long osmId, MeshPolygonType type, List<GeoCoordinate> geoCoordinates, LandClass landClass) throws MeshInconsistencyException {
                return ms.addArea(meshName, osmId, type, geoCoordinates, landClass);
            }
        };
    }

    /**
     * Adjusts (cuts/splits) an existing polygon (typically an AREA) so it no longer overlaps newPolygon by
     * subtracting newPolygon from it. The existing polygon is replaced by one or more new polygons having the
     * same osmId, type and landClass (so its id changes). Nothing is validated here, that is up to the caller
     * after adding newPolygon.
     * Throws MeshInconsistencyException if the result can't be represented (polygon with hole) or
     * the subtraction fails.
     * 28.10.26
     */
    private void adjustPolygon(long polygonToAdjustId, GeoPolygon newPolygon) throws MeshInconsistencyException {
        // Avoid problems with detached entities and reload polygon (the entity instances might have been created before the new TX started).
        PersistedMeshPolygon existing = meshPolygonRepository.findById(polygonToAdjustId)
                .orElseThrow(() -> new MeshInconsistencyException("polygon to adjust not found: " + polygonToAdjustId));
        GeoPolygon existingPolygon = existing.getGeoPolygon();
        if (existingPolygon == null) {
            // already logged
            throw new MeshInconsistencyException("polygon to adjust is inconsistent: " + existing);
        }
        // Use the same overlap limit as JtsUtil.overlaps() for GeoCoordinates
        List<Polygon> parts = JtsUtil.subtractPolygons(existingPolygon, newPolygon, 0.000000001);
        if (parts == null) {
            log.debug("No overlap of {}, so nothing to adjust", existing);
            return;
        }
        if (parts.isEmpty()) {
            // Either completely covered by newPolygon or the subtraction failed (already logged).
            throw new MeshInconsistencyException("adjusting " + existing + " failed (no remaining part)");
        }
        PersistedMesh mesh = existing.getMesh();
        List<PersistedMeshPolygon> newPolygons = new ArrayList<>();
        for (Polygon part : parts) {
            if (part.getNumInteriorRing() > 0) {
                // subtractPolygons() should have split it, so already logged there
                throw new MeshInconsistencyException("adjusting " + existing + " leads to a hole, which isn't supported");
            }
            List<GeoCoordinate> coordinates = JtsUtil.coordinatesToGeoCoordinates(Arrays.asList(part.getExteriorRing().getCoordinates()));
            // skip closing coordinate, buildPolygon() closes itself
            coordinates.remove(coordinates.size() - 1);
            newPolygons.add(buildPersistedMeshPolygon(mesh, existing.getOsmId(), existing.getType(), existing.getLandClass(), coordinates.iterator()));
        }
        // like deletePolygon(), keep the mesh relation in sync
        mesh.getPolygons().remove(existing);
        meshPolygonRepository.delete(existing);
        meshPolygonRepository.saveAll(newPolygons);
        log.debug("Adjusted {} to {} part(s)", existing, newPolygons.size());
    }

    /**
     * Builds a new (not yet saved) polygon from the (not closed) coordinates of nodeProvider and adds it to the mesh.
     * 28.10.26: Extracted from addPolygon() for also being usable in adjustPolygon().
     */
    private PersistedMeshPolygon buildPersistedMeshPolygon(PersistedMesh mesh, long osmId, MeshPolygonType type, LandClass landClass, Iterator<GeoCoordinate> nodeProvider) throws MeshInconsistencyException {
        /*TODO Polygon polygon = JtsUtil.createPolygonFromWayOutlines(new CoordinateList(rightLine), new CoordinateList(leftLine));

         */

        PersistedMeshPolygon meshPolygon = new PersistedMeshPolygon();
        meshPolygon.setType(type);
        meshPolygon.setLandClass(landClass);
        meshPolygon.setOsmId(osmId);
        meshPolygon.setMesh(mesh);
        //TODO meshArea.setOsmWay(osmWay);

        List<PersistedMeshNode> points = new ArrayList<>();
        // List<MeshLine> newLines = new ArrayList<>();
        //PersistedMeshNode n;// = buildMeshNode(leftLine.get(0),mesh);
        //points.add(n);
        MeshLine l;
        /*for (int i = 0; i < leftLine.size(); i++) {
            n = buildMeshNode(leftLine.get(i), mesh);
            meshPolygon.addNode(n);
        }
        for (int i = rightLine.size() - 1; i >= 0; i--) {
            n = buildMeshNode(rightLine.get(i), mesh);
            meshPolygon.addNode(n);
        }*/
        while (nodeProvider.hasNext()) {
            PersistedMeshNode n = buildMeshNode(nodeProvider.next(), mesh);
            meshPolygon.addNode(n);
        }
        ;

        meshPolygon.close();
        // keep relation sync, otherwise reuse inside this TX won't know it.
        mesh.getPolygons().add(meshPolygon);

        // Be sure it is a valid polygon
        if (!meshPolygon.isValidForSave()) {
            throw new MeshInconsistencyException("polygon not valid for osmId " + osmId);
        }
        return meshPolygon;
    }

    /**
     * The (not closed) outline of a way polygon: left line forward, then right line backward.
     */
    private List<GeoCoordinate> buildWayOutline(List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine) {
        List<GeoCoordinate> l = new ArrayList<>();
        JtsUtil.processWayOutlines(leftLine, rightLine, (geoCoordinate) -> {
            l.add(geoCoordinate);
        });
        return l;
    }

    private List<MeshPolygon> findAreaIntersections(String meshName, GeoPolygon wayPolygon) throws MeshInconsistencyException {
        PersistedMesh mesh = meshRepository.findByName(meshName);
        if (mesh == null) {
            throw new RuntimeException("mesh not found:" + meshName);
        }
        // GeoPolygon coordinates are x=lon, y=lat
        Envelope envelope = wayPolygon.getEnvelopeInternal();

        List<Long> candidateIds = meshPolygonRepository.findCandidatePolygonIdsInBox(mesh,
                envelope.getMinY(), envelope.getMaxY(), envelope.getMinX(), envelope.getMaxX());
        List<MeshPolygon> result = new ArrayList<>();
        for (PersistedMeshPolygon p : meshPolygonRepository.findAllById(candidateIds)) {
            if (p.getType() != MeshPolygonType.AREA) {
                continue;
            }
            GeoPolygon geoPolygon = p.getGeoPolygon();
            // null when inconsistent, which is already logged
            if (geoPolygon != null && Boolean.TRUE.equals(JtsUtil.overlaps(wayPolygon, geoPolygon))) {
                result.add(p);
            }
        }
        result.sort(Comparator.comparing(p -> ((PersistedMeshPolygon) p).getId()));
        log.debug("Found {} intersecting area(s) of {} candidate(s) in mesh {}", result.size(), candidateIds.size(), meshName);
        return result;
    }
}

