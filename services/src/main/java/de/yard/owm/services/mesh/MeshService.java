package de.yard.owm.services.mesh;

import de.yard.owm.configuration.ApplicationContextHolder;
import de.yard.owm.services.persistence.*;
import de.yard.threed.core.Degree;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.MeshServiceFacade;
import de.yard.threed.osm2scenery.polygon20.*;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
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
        // We should have some kind of order, at least for test reliability
        Iterator<PersistedMeshPolygon> ps = mesh.getPolygons().stream().sorted(Comparator.comparing(PersistedMeshPolygon::getId)).iterator();
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

            p.getNodesSortedByIndex().forEach(n -> {

                if (!points.contains(n)) {
                    points.add(n);
                    //((PersistedMeshNode)l.getTo()).projection=gridCellBounds.getProjection().getBaseProjection();
                }
            });
        }

        if (boundary == null) {
            throw new MeshInconsistencyException("no mesh boundary found");
        }
        GridCellBounds gridCellBounds = new GridCellBounds(boundary);

        TerrainMesh terrainMesh = TerrainMesh.init(gridCellBounds, points, polygons);
        terrainMesh.meshService = buildMeshServiceFacade();
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
    public TerrainMesh addWay(String meshName, long osmWayId, Pair<GeoCoordinate, GeoCoordinate> fromConnector, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine, Pair<GeoCoordinate, GeoCoordinate> toConnector, int lanes) throws OsmProcessException, MeshInconsistencyException {

        log.debug("Adding way {} to mesh", osmWayId);

        TerrainMesh terrainMesh = loadMesh(meshName);
        PersistedMesh mesh = meshRepository.findByName(meshName);
        if (mesh == null) {
            throw new RuntimeException("mesh not found:" + meshName);
        }
        /*TODO Polygon polygon = JtsUtil.createPolygonFromWayOutlines(new CoordinateList(rightLine), new CoordinateList(leftLine));

        List<MeshLine> linesToDelete = new ArrayList<>();
        for (MeshPolygon p : terrainMesh.polygons) {
            if (crosses(p., polygon)) {
                // intersection found. If it is not a BG line, this is a failure. Either the way overlaps some existing area or the (sub)mesh is too small.
                //if (!MeshLine.isBackgroundTriangulation(line.getType())) {
                    throw new OsmProcessException("polygon crosses other polygon " + line);
                //}
                //linesToDelete.add(line);
            }
        }*/
       /* linesToDelete.forEach(l -> deleteLineFromMesh(l));

        AbstractArea/*SceneryFlatObject* / leftArea = null;
        AbstractArea/*SceneryFlatObject* / rightArea = null;
*/
        //MeshArea meshArea = addArea();
        PersistedMeshPolygon meshPolygon = new PersistedMeshPolygon();
        meshPolygon.setType(MeshPolygonType.WAY);
        meshPolygon.setOsmId(osmWayId);
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
        JtsUtil.processWayOutlines(leftLine, rightLine,(geoCoordinate)->{
            PersistedMeshNode n = buildMeshNode(geoCoordinate, mesh);
            meshPolygon.addNode(n);
        });

        meshPolygon.close();
        // keep relation sync, otherwise reuse inside this TX won't know it.
        mesh.getPolygons().add(meshPolygon);
        meshPolygonRepository.save(meshPolygon);

        // No validate and rollback in case of failure (exception)
        terrainMesh = loadMesh(meshName);
        terrainMesh.validate();
        // no longer connect ot background mesh
        return terrainMesh;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TerrainMesh addConnector(String meshName, long osmNodeId, List<Pair<GeoCoordinate, Long>> polygon, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints) throws MeshInconsistencyException {

        log.debug("Adding connector {} with {} nodes and {} attach pairs to mesh", osmNodeId, polygon.size() - 1, wayAttachPoints.size());

        // equals is not yet implemented in GeoCoordinate, so this is ...???
        if (!polygon.get(0).getFirst().equals(polygon.get(polygon.size() - 1).getFirst())) {
            throw new MeshInconsistencyException("polygon not closed");
        }

        PersistedMesh mesh = meshRepository.findByName(meshName);
        if (mesh == null) {
            throw new RuntimeException("mesh not found:" + meshName);
        }

        PersistedMeshPolygon meshPolygon = new PersistedMeshPolygon();
        meshPolygon.setType(MeshPolygonType.CONNECTOR);
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addFailure(String meshName, String message, String sourceRef, GeoPolygon geoPolygon) {
        PersistedMesh mesh = meshRepository.findByName(meshName);
        if (mesh == null) {
            return;
        }
        PersistedMeshFailure failure = new PersistedMeshFailure();
        failure.setMessage(message);
        failure.setSourceRef(sourceRef);
        // Use WKT with geo corrinates
        if (geoPolygon != null) {
            failure.setPolygon(geoPolygon.toText());
        }
        failure.setPersistedMesh(mesh);
        meshFailureRepository.save(failure);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MeshPolygon/*Connector*/ getConnector(long osmNodeId) {
        // TODO also find by type CONNECTOR
        List<PersistedMeshPolygon> ps = meshPolygonRepository.findByOsmId(osmNodeId);
        if (ps.isEmpty()) {
            return null;
        }
        PersistedMeshPolygon persistedMeshPolygon=ps.get(0);
        return persistedMeshPolygon;
    }



    private PersistedMeshNode buildMeshNode(GeoCoordinate coordinate, PersistedMesh mesh) {
        PersistedMeshNode newNode = new PersistedMeshNode(null, coordinate);
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
            public TerrainMesh addWay(String meshName, long osmWayId, Pair<GeoCoordinate, GeoCoordinate> fromConnector, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine, Pair<GeoCoordinate, GeoCoordinate> toConnector, int lanes) throws OsmProcessException, MeshInconsistencyException {
                return ms.addWay(meshName, osmWayId, fromConnector, leftLine, rightLine, toConnector, lanes);
            }

            @Override
            public TerrainMesh addConnector(String meshName, long osmNodeId, List<Pair<GeoCoordinate, Long>> polygon, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints) throws MeshInconsistencyException {
                return ms.addConnector(meshName, osmNodeId, polygon, wayAttachPoints);
            }

            @Override
            public TerrainMesh loadMesh(String meshName) throws MeshInconsistencyException {
                return ms.loadMesh(meshName);
            }

            @Override
            public void addFailure(String meshName, String message, String sourceref, GeoPolygon polygon) {
                ms.addFailure(meshName, message, sourceref, polygon);
            }

            @Override
            public MeshPolygon/*WayConnector*/ getConnector(long osmNodeId) {
                return ms.getConnector(osmNodeId);
            }
        };
    }
}

