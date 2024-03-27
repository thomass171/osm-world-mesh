package de.yard.threed.osm2scenery;


import com.vividsolutions.jts.geom.Geometry;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2world.AxisAlignedBoundingBoxXZ;
import de.yard.threed.osm2world.FaultTolerantIterationUtil;
import de.yard.threed.osm2world.GeometryUtil;
import de.yard.threed.osm2world.HardcodedRuleset;
import de.yard.threed.osm2world.InvalidGeometryException;
import de.yard.threed.osm2world.JTSConversionUtil;
import de.yard.threed.osm2world.LineSegmentXZ;
import de.yard.threed.osm2world.MapArea;
import de.yard.threed.osm2world.MapAreaCreateException;
import de.yard.threed.osm2world.MapAreaSegment;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapDataIndex;
import de.yard.threed.osm2world.MapElement;
import de.yard.threed.osm2world.MapIntersectionGrid;
import de.yard.threed.osm2world.MapIntersectionWW;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.MapOverlapAA;
import de.yard.threed.osm2world.MapOverlapNA;
import de.yard.threed.osm2world.MapOverlapType;
import de.yard.threed.osm2world.MapOverlapWA;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.MapWaySegment;
import de.yard.threed.osm2world.MultipolygonAreaBuilder;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.OSMRelation;
import de.yard.threed.osm2world.OSMWay;
import de.yard.threed.osm2world.PolygonWithHolesXZ;
import de.yard.threed.osm2world.Ruleset;
import de.yard.threed.osm2world.SimplePolygonXZ;
import de.yard.threed.osm2world.Tag;
import de.yard.threed.osm2world.VectorXZ;
import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.yard.threed.osm2world.FaultTolerantIterationUtil.iterate;
import static de.yard.threed.osm2world.VectorXZ.distance;
import static java.util.Collections.emptyList;


/**
 * Abgeleitet vom OSM2World OSMToMapDataConverter.java.
 * <p>
 * Die Umwandlung in MapData nutze ich erstmal weiter.
 * <p>
 * Macht
 * - eine 2D Projektion der GeoKoordinaten nach xy (liegt dann in MapData)
 * - Ergänzung von Dummynodes an den Grid boundaries
 *
 * <p>
 * converts { OSMData} into the internal map data representation
 */
public class OSMToSceneryDataConverter {
    Logger logger = Logger.getLogger(OSMToSceneryDataConverter.class.getName());
    private final Ruleset ruleset = new HardcodedRuleset();

    private final SceneryProjection mapProjection;
    //11.4.19 private final Configuration config;

    private static final Tag MULTIPOLYON_TAG = new Tag("type", "multipolygon");
    GridCellBounds targetBounds;
    /* create MapNode for each OSM node */

    final Map<OSMNode, MapNode> nodeMap = new HashMap<OSMNode, MapNode>();


    public OSMToSceneryDataConverter(SceneryProjection mapProjection, GridCellBounds targetBounds/*, Configuration config*/) {
        this.mapProjection = mapProjection;
        this.targetBounds = targetBounds;
        // 11.4.19 this.config = config;
    }

    public MapData createMapData(OSMData osmData/*, GridCellBounds targetBounds*/) throws IOException {

        final List<MapNode> mapNodes = new ArrayList<MapNode>();
        final List<MapWaySegment> mapWaySegs = new ArrayList<MapWaySegment>();
        final List<MapArea> mapAreas = new ArrayList<MapArea>();
        final List<MapWay> mapWays = new ArrayList<MapWay>();

        createMapElements(osmData, mapNodes, mapWaySegs, mapAreas, mapWays, (GridCellBounds) targetBounds);

        MapData mapData = new MapData(mapNodes, mapWaySegs, mapAreas, mapWays,
                calculateFileBoundary(osmData.getBounds()));

        calculateIntersectionsInMapData(mapData);

        return mapData;

    }

    /**
     * creates {@link MapElement}s
     * based on OSM data from an {link OSMFileDataSource}
     * and adds them to collections
     */
    private void createMapElements(OSMData osmData,
                                   final List<MapNode> mapNodes, final List<MapWaySegment> mapWaySegs,
                                   final List<MapArea> mapAreas, List<MapWay> mapWays, GridCellBounds targetBounds) {

        /* create MapNode for each OSM node */

        //final Map<OSMNode, MapNode> nodeMap = new HashMap<OSMNode, MapNode>();

        for (OSMNode node : osmData.getNodes()) {
            createMapNode(node, mapNodes);
        }

        /* create areas ... */

        final Map<OSMWay, MapArea> areaMap = new HashMap<OSMWay, MapArea>();

        /* ... based on multipolygons */

        iterate(osmData.getRelations(), new FaultTolerantIterationUtil.Operation<OSMRelation>() {
            @Override
            public void perform(OSMRelation relation) {

                if (relation.tags.contains(MULTIPOLYON_TAG)) {
                    try {
                        Collection<MapArea> mapareas = MultipolygonAreaBuilder.createAreasForMultipolygon(relation, nodeMap);
                        for (MapArea area : mapareas) {

                            mapAreas.add(area);

                            for (MapNode boundaryMapNode : area.getBoundaryNodes()) {
                                boundaryMapNode.addAdjacentArea(area);
                            }

                            if (area.getOsmObject() instanceof OSMWay) {
                                areaMap.put((OSMWay) area.getOsmObject(), area);
                            }

                        }
                    } catch (MapAreaCreateException e) {
                        logger.error("MapArea create failed:" + e.getMessage());
                    }
                }

            }
        });

        /* ... based on coastline ways */
        //11.9.19: Was der hier macht ist schleierhaft. Auf jeden Fall bekommen die OSMData vier neue Nodes.
        //TODO ich glaube, dass kann weg.

        for (MapArea area : MultipolygonAreaBuilder.createAreasForCoastlines(
                osmData, nodeMap, mapNodes,
                calculateFileBoundary(osmData.getBounds()))) {

            mapAreas.add(area);

            for (MapNode boundaryMapNode : area.getBoundaryNodes()) {
                boundaryMapNode.addAdjacentArea(area);
            }

        }

        /* ... based on closed ways */

        for (OSMWay way : osmData.getWays()) {
            if (way.id == 48703221) {
                int osmid = 6;
            }
            if (way.isClosed() && !areaMap.containsKey(way)) {
                //create MapArea only if at least one tag isType an area tag.
                //die Logik kommt mir nicht passend vor. Ein closed way dürfte meistens eine Area sein, ausser sowas wie z.B. Nürburgring. Also müsste es
                //hinreichende und notwendige Kriterien geben. Un darum in eigene Methode
                //TODO bei der Entscheidung die Modules mit einbeziehen, was die verarbeiten können
                //for (Tag tag : way.tags) {
                //  if (ruleset.isAreaTag(tag)) {
                if (isClosedWayAnArea(way)) {
                    //TODO: check whether this isType old-style MP outer

                    List<MapNode> nodes = new ArrayList<MapNode>(way.getNodes().size());
                    for (OSMNode boundaryOSMNode : way.getNodes()) {
                        nodes.add(nodeMap.get(boundaryOSMNode));
                    }

                    try {

                        MapArea mapArea = new MapArea(way, nodes);

                        mapAreas.add(mapArea);
                        areaMap.put(way, mapArea);

                        for (MapNode boundaryMapNode : mapArea.getBoundaryNodes()) {
                            boundaryMapNode.addAdjacentArea(mapArea);
                        }
                    } catch (MapAreaCreateException e) {
                        logger.error("MapArea create failed:" + e.getMessage());
                    } catch (InvalidGeometryException e) {
                        logger.error("MapArea create failed:" + e.getMessage());
                    }

                    //break;
                    //}
                }
            }
        }

        /* ... for empty terrain */
		
		/*11.7.18: das geht anders AxisAlignedBoundingBoxXZ terrainBoundary =
				calculateFileBoundary(osmData.getBounds());
		
		if (terrainBoundary != null
				&& config.getBoolean("createTerrain", true)) {
			
			EmptyTerrainBuilder.createAreasForEmptyTerrain(
					mapNodes, mapAreas, terrainBoundary);
			
		} else {
			
			//TODO fall back on data boundary if file does not contain bounds
			
		}
		
		if (targetBounds != null){
            EmptyTerrainBuilder.createAreasForTargetBounds(
                    mapNodes, mapAreas, terrainBoundary,targetBounds);
        }*/

        /* finish calculations */

        for (MapNode node : nodeMap.values()) {
            node.calculateAdjacentAreaSegments();
        }

        /* create way segments from remaining ways */


        //collect splitted closed and "P" ways
        List<OSMWay> newWays = new ArrayList<>();
        for (OSMWay way : osmData.getWays()) {

            if (!way.tags.isEmpty() && !areaMap.containsKey(way)) {
                //Evtl. splitten um Polygons with hole zu vermeiden bzw. generell die Vererbaitung einfach zu halten. Geht nur am Anfang/Ende und wenn da sonst nichts dranhängt, ums nicht zu kompliziert zu machen.
                //erstmal nur anfang
                if (way.id == 26927466 || way.id == 38809414) {
                    int osmid = 6;
                }

                if (way.isClosed()) {
                    // moeglichst weit hinten splitten, damit möglichst viel Original erhalten bleibt.
                    int nodeindex = findUniqueNodeInWay(way, way.getNodes().size()-2, -1, osmData.getWays());
                    OSMWay newWay = splitWayAtNode(way, nodeindex, osmData.getWays());
                    if (newWay != null) {
                        newWays.add(newWay);
                    } else {
                        logger.warn("split of closed way failed (too complex??, way will be ignored):" + way.id);
                    }
                } else {
                    Integer pIndex;
                    int nodeindex;
                    if ((pIndex = way.isP()) != null) {
                        int index = pIndex;
                        if (index < 0) {
                            index = -index;
                            //end on index
                            nodeindex = findUniqueNodeInWay(way, index + 1, 1, osmData.getWays());
                        } else {
                            //starts at index
                            nodeindex = findUniqueNodeInWay(way, index - 1, -1, osmData.getWays());
                        }
                        OSMWay newWay = null;
                        if (nodeindex != -1) {
                            newWay = splitWayAtNode(way, nodeindex, osmData.getWays());
                            if (newWay != null) {
                                newWays.add(newWay);
                            }
                        }
                        if (newWay == null) {
                            logger.warn("split of closed way failed (too complex??, way will be ignored):" + way.id);

                        }
                    }
                }
            }
        }
        osmData.getWays().addAll(newWays);

        for (OSMWay way : osmData.getWays()) {
            if (way.id == 26927466) {
                int osmid = 6;
            }
            addWay(way, mapWays, mapWaySegs);
        }
    }

    /**
     * Find an inner node in this way starting at index that isType unique to this way.
     * Don't consider start/end nodes.
     *
     * @return
     */
    private int findUniqueNodeInWay(OSMWay way, int index, int increment, Collection<OSMWay> allWays) {
        do {
            OSMNode node0 = way.getNodes().get(index);
            List<OSMWay> waysofNode0 = findWaysOfNode(node0, allWays);
            if (waysofNode0.size() == 1) {
                return index;
            }
            index += increment;
        } while (index > 0 && index < way.getNodes().size() - 1);
        return -1;
    }

    /**
     * splitten um Polygons with hole zu vermeiden bzw. generell die Verarbeitung einfach zu halten. Geht nur wenn da sonst nichts dranhängt, ums nicht zu kompliziert zu machen.
     * Das muss der Aufrufer pruefen.
     * z.B. bei Circle oder "P" (38809414)
     * nodeindex will be the last in exisitng way and first in new way.
     *
     * @return
     */
    private OSMWay splitWayAtNode(OSMWay way, int nodeindex, Collection<OSMWay> allWays) {
        OSMNode node0 = way.getNodes().get(nodeindex);
        OSMNode node1 = way.getNodes().get(nodeindex + 1);
        // List<OSMWay> waysofNode0 = findWaysOfNode(node0, allWays);
        //List<OSMWay> waysofNode1 = findWaysOfNode(node1, allWays);
        //if (waysofNode0.size() == 1 && waysofNode1.size() == 1 && waysofNode0.get(0) == way && waysofNode1.get(0) == way) {
        int sizeOfNew = way.getNodes().size() - nodeindex;
        List<OSMNode> nl = new ArrayList<>();

        for (int i = nodeindex; i < way.getNodes().size(); i++) {
            //way.nodes.remove(0);
            nl.add(way.getNodes().get(i));
        }
        for (int i = 0; i < nl.size()-1; i++) {
            way.removeNode(way.getNodes().size() - 1);
        }
        OSMWay newWay = OsmUtil.buildDummyWay(way.tags, nl);
        return newWay;
        //logger.warn("split of closed way failed (too complex??, way will be ignored):" + way.id);
        //return null;
    }

    /**
     * Ein closed way dürfte meistens eine Area sein, ausser sowas wie z.B. Nürburgring. Also müsste es
     * inreichende und notwendige Kriterien geben. Un darum in eigene Methode
     *
     * @param way
     * @return
     */
    private boolean isClosedWayAnArea(OSMWay way) {
        //TODO wenn explizit Road etc, dann false
        //if (HighwayMo)
        for (Tag tag : way.tags) {
            if (ruleset.isAreaTag(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Auch um Mapdata nachtraeglich zu erweitern.
     * 30.5.2019
     *
     * @param way
     * @param mapWays
     * @param mapWaySegs
     */
    private void addWay(OSMWay way, List<MapWay> mapWays, Collection<MapWaySegment> mapWaySegs) {
        OSMNode previousNode = null;
        MapWay mapWay = null;
        if (way.id == 26927466) {
            int h = 9;
        }
        for (OSMNode node : way.getNodes()) {
            if (previousNode == null) {
                mapWay = new MapWay(nodeMap.get(node), way);
            } else {
                MapNode mapNode = nodeMap.get(node);
                MapNode prevMapNode = nodeMap.get(previousNode);
                if (targetBounds.isPreDbStyle()) {
                    if (wayCrossesGridBoundary(targetBounds, prevMapNode, mapNode)) {
                        // dann am Schnittpunkt eine Dummy Mapnode einbauen.
                        Geometry gridnodepos = targetBounds.getPolygon().getExteriorRing().intersection(
                                JtsUtil.createLine(JTSConversionUtil.vectorXZToJTSCoordinate(prevMapNode.getPos()),
                                        JTSConversionUtil.vectorXZToJTSCoordinate(mapNode.getPos())));
                        VectorXZ xz = JTSConversionUtil.vectorXZFromJTSCoordinate(gridnodepos.getCoordinates()[0]);
                        OSMNode gridosmnode = OsmUtil.buildDummyNode(mapProjection, xz);
                        MapNode gridnode = new MapNode(xz, gridosmnode, MapNode.Location.GRIDNODE);
                        addSegment(way, prevMapNode, gridnode, mapWaySegs, mapWay);
                        previousNode = gridosmnode;
                        prevMapNode = gridnode;
                        //noch nicht, weil noch gar nicht klar ist ob der way gebraucht wird
                        //targetBounds.gridnodes.add(gridnode);
                    }
                }
                addSegment(way, prevMapNode, mapNode, mapWaySegs, mapWay);
            }

            previousNode = node;
        }
        if (mapWay != null && mapWay.getEndNode() != null) {
            mapWays.add(mapWay);
        }
    }

    void addSegment(OSMWay way, MapNode prevMapNode, MapNode mapNode, Collection<MapWaySegment> mapWaySegs, MapWay mapWay) {
        MapWaySegment mapWaySeg = new MapWaySegment(way, prevMapNode, mapNode);
        mapWaySegs.add(mapWaySeg);
        prevMapNode.addOutboundLine(mapWaySeg);
        mapNode.addInboundLine(mapWaySeg);
        mapWay.add(mapNode/*nodeMap.get(node)*/, mapWaySeg);
    }

    /**
     * Um Mapdata nachtraeglich zu erweitern.
     * 30.5.2019
     */
    public void addWayToMapData(OSMWay osmWay, MapData mapData) {
        for (OSMNode n : osmWay.getNodes()) {
            createMapNode(n, mapData.getMapNodes());
        }
        addWay(osmWay, mapData.getMapWays(), mapData.getMapWaySegments());
    }

    /**
     * Das kann eine haarige Entscheidung sein, wenn eine der Nodes ziemlich genau auf der Boundary liegt.
     * Es wird in dem Sinne entschieden, ob eine DummyNode in den Way aufgenommen werden soll.
     * Also z.B. nicht, wenn eine der Nodes Boundarynode ist.
     */
    private boolean wayCrossesGridBoundary(GridCellBounds targetBounds, MapNode prevMapNode, MapNode mapNode) {
        if (targetBounds == null) {
            return false;
        }
        if (targetBounds.isBoundaryNode(prevMapNode.getPos())) {
            return false;
        }
        if (targetBounds.isBoundaryNode(mapNode.getPos())) {
            return false;
        }
        if (((GridCellBounds) targetBounds).isInside(prevMapNode) != ((GridCellBounds) targetBounds).isInside(mapNode)) {
            // das ist jetzt nur ein einfacher Fall. Es kann auch exotischerere geben?
            // Ja, beiden könnten inner/ausserhalb liegen und trotzdem das Grid schneiden.
            return true;
        }
        return false;
    }

    /**
     * calculates intersections and adds the information to the
     * {@link MapElement}s
     */
    private static void calculateIntersectionsInMapData(MapData mapData) {

        MapDataIndex index = new MapIntersectionGrid(mapData.getDataBoundary());

        for (MapElement e1 : mapData.getMapElements()) {

            /* collect all nearby elements */

            Collection<? extends Iterable<MapElement>> leaves
                    = index.insertAndProbe(e1);

            if (e1 instanceof MapArea) {
                e1 = e1;
            }
            Iterable<MapElement> nearbyElements;

            if (leaves.size() == 1) {
                nearbyElements = leaves.iterator().next();
            } else {
                // collect and de-duplicate elements from all the leaves
                Set<MapElement> elementSet = new HashSet<MapElement>();
                for (Iterable<MapElement> leaf : leaves) {
                    for (MapElement e : leaf) {
                        elementSet.add(e);
                    }
                }
                nearbyElements = elementSet;
            }

            for (MapElement e2 : nearbyElements) {

                if (e1 == e2) {
                    continue;
                }

                addOverlapBetween(e1, e2);

            }

        }

    }

    /**
     * adds the overlap between two {@link MapElement}s
     * to both, if it exists. It calls the appropriate
     * subtype-specific addOverlapBetween method
     */
    private static void addOverlapBetween(MapElement e1, MapElement e2) {

        if (e1 instanceof MapWaySegment
                && e2 instanceof MapWaySegment) {

            addOverlapBetween((MapWaySegment) e1, (MapWaySegment) e2);

        } else if (e1 instanceof MapWaySegment
                && e2 instanceof MapArea) {

            addOverlapBetween((MapWaySegment) e1, (MapArea) e2);

        } else if (e1 instanceof MapArea
                && e2 instanceof MapWaySegment) {

            addOverlapBetween((MapWaySegment) e2, (MapArea) e1);

        } else if (e1 instanceof MapArea
                && e2 instanceof MapArea) {

            addOverlapBetween((MapArea) e1, (MapArea) e2);

        } else if (e1 instanceof MapNode
                && e2 instanceof MapArea) {

            addOverlapBetween((MapNode) e1, (MapArea) e2);

        } else if (e1 instanceof MapArea
                && e2 instanceof MapNode) {

            addOverlapBetween((MapNode) e2, (MapArea) e1);

        }

    }

    /**
     * adds the overlap between two {@link MapWaySegment}s
     * to both, if it exists
     */
    private static void addOverlapBetween(
            MapWaySegment line1, MapWaySegment line2) {

        if (line1.isConnectedTo(line2)) {
            return;
        }

        VectorXZ intersection = GeometryUtil.getLineSegmentIntersection(
                line1.getStartNode().getPos(),
                line1.getEndNode().getPos(),
                line2.getStartNode().getPos(),
                line2.getEndNode().getPos());

        if (intersection != null) {

            /* add the intersection */

            MapIntersectionWW newIntersection =
                    new MapIntersectionWW(line1, line2, intersection);

            line1.addOverlap(newIntersection);
            line2.addOverlap(newIntersection);

        }

    }

    /**
     * adds the overlap between a {@link MapWaySegment}
     * and a {@link MapArea} to both, if it exists
     */
    private static void addOverlapBetween(
            MapWaySegment line, MapArea area) {

        final LineSegmentXZ segmentXZ = line.getLineSegment();

        /* check whether the line corresponds to one of the area segments */

        for (MapAreaSegment areaSegment : area.getAreaSegments()) {
            if (areaSegment.sharesBothNodes(line)) {

                MapOverlapWA newOverlap =
                        new MapOverlapWA(line, area, MapOverlapType.SHARE_SEGMENT,
                                Collections.<VectorXZ>emptyList(),
                                Collections.<MapAreaSegment>emptyList());

                line.addOverlap(newOverlap);
                area.addOverlap(newOverlap);

                return;

            }
        }

        /* calculate whether the line contains or intersects the area (or neither) */

        boolean contains;
        boolean intersects;

        {
            final PolygonWithHolesXZ polygon = area.getPolygon();

            if (!line.isConnectedTo(area)) {

                intersects = polygon.intersects(segmentXZ);
                contains = !intersects && polygon.contains(segmentXZ);

            } else {

                /* check whether the line intersects the area somewhere
                 * else than just at the common node(s).
                 */

                intersects = false;

                double segmentLength = distance(segmentXZ.p1, segmentXZ.p2);

                for (VectorXZ pos : polygon.intersectionPositions(segmentXZ)) {
                    if (distance(pos, segmentXZ.p1) > segmentLength / 100
                            && distance(pos, segmentXZ.p2) > segmentLength / 100) {
                        intersects = true;
                        break;
                    }
                }

                /* check whether the area contains the line's center.
                 * Unless the line intersects the area outline,
                 * this means that the area contains the line itself.
                 */

                contains = !intersects && polygon.contains(segmentXZ.getCenter());

            }

        }

        /* add an overlap if detected */

        if (contains || intersects) {

            /* find out which area segments intersect the way segment */

            List<VectorXZ> intersectionPositions = emptyList();
            List<MapAreaSegment> intersectingSegments = emptyList();

            if (intersects) {

                intersectionPositions = new ArrayList<VectorXZ>();
                intersectingSegments = new ArrayList<MapAreaSegment>();

                for (MapAreaSegment areaSegment : area.getAreaSegments()) {

                    VectorXZ intersection = segmentXZ.getIntersection(
                            areaSegment.getStartNode().getPos(),
                            areaSegment.getEndNode().getPos());

                    if (intersection != null) {
                        intersectionPositions.add(intersection);
                        intersectingSegments.add(areaSegment);
                    }

                }

            }

            /* add the overlap */

            MapOverlapWA newOverlap = new MapOverlapWA(line, area,
                    intersects ? MapOverlapType.INTERSECT : MapOverlapType.CONTAIN,
                    intersectionPositions, intersectingSegments);

            line.addOverlap(newOverlap);
            area.addOverlap(newOverlap);

        }

    }

    /**
     * adds the overlap between two {@link MapArea}s
     * to both, if it exists
     */
    private static void addOverlapBetween(
            MapArea area1, MapArea area2) {

        /* check whether the areas have a shared segment */

        Collection<MapAreaSegment> area1Segments = area1.getAreaSegments();
        Collection<MapAreaSegment> area2Segments = area2.getAreaSegments();

        for (MapAreaSegment area1Segment : area1Segments) {
            for (MapAreaSegment area2Segment : area2Segments) {
                if (area1Segment.sharesBothNodes(area2Segment)) {

                    MapOverlapAA newOverlap =
                            new MapOverlapAA(area1, area2, MapOverlapType.SHARE_SEGMENT);
                    area1.addOverlap(newOverlap);
                    area2.addOverlap(newOverlap);

                    return;

                }
            }
        }

        /* calculate whether one area contains the other
         * or whether their outlines intersect (or neither) */

        boolean contains1 = false;
        boolean contains2 = false;
        boolean intersects = false;

        {
            final PolygonWithHolesXZ polygon1 = area1.getPolygon();
            final PolygonWithHolesXZ polygon2 = area2.getPolygon();

            /* determine common nodes */

            Set<VectorXZ> commonNodes = new HashSet<VectorXZ>();
            for (SimplePolygonXZ p : polygon1.getPolygons()) {
                commonNodes.addAll(p.getVertices());
            }

            Set<VectorXZ> nodes2 = new HashSet<VectorXZ>();
            for (SimplePolygonXZ p : polygon2.getPolygons()) {
                nodes2.addAll(p.getVertices());
            }

            commonNodes.retainAll(nodes2);

            /* check whether the areas' outlines intersects somewhere
             * else than just at the common node(s).
             */

            intersectionPosCheck:
            for (VectorXZ pos : polygon1.intersectionPositions(polygon2)) {
                boolean trueIntersection = true;
                for (VectorXZ commonNode : commonNodes) {
                    if (distance(pos, commonNode) < 0.01) {
                        trueIntersection = false;
                    }
                }
                if (trueIntersection) {
                    intersects = true;
                    break intersectionPosCheck;
                }
            }

            /* check whether one area contains the other */

            if (polygon1.contains(polygon2.getOuter())) {
                contains1 = true;
            } else if (polygon2.contains(polygon1.getOuter())) {
                contains2 = true;
            }

        }

        /* add an overlap if detected */

        if (contains1 || contains2 || intersects) {

            /* add the overlap */

            MapOverlapAA newOverlap = null;

            if (contains1) {
                newOverlap = new MapOverlapAA(area2, area1, MapOverlapType.CONTAIN);
            } else if (contains2) {
                newOverlap = new MapOverlapAA(area1, area2, MapOverlapType.CONTAIN);
            } else {
                newOverlap = new MapOverlapAA(area1, area2, MapOverlapType.INTERSECT);
            }

            area1.addOverlap(newOverlap);
            area2.addOverlap(newOverlap);

        }

    }

    private static void addOverlapBetween(MapNode node, MapArea area) {

        if (area.getPolygon().contains(node.getPos())) {

            /* add the overlap */

            MapOverlapNA newOverlap =
                    new MapOverlapNA(node, area, MapOverlapType.CONTAIN);

            area.addOverlap(newOverlap);

        }

    }

    private AxisAlignedBoundingBoxXZ calculateFileBoundary(
            Collection<Bound> bounds) {

        Collection<VectorXZ> boundedPoints = new ArrayList<VectorXZ>();

        for (Bound bound : bounds) {

            boundedPoints.add(OsmUtil.calcPos(mapProjection, bound.getBottom(), bound.getLeft()));
            boundedPoints.add(OsmUtil.calcPos(mapProjection, bound.getBottom(), bound.getRight()));
            boundedPoints.add(OsmUtil.calcPos(mapProjection, bound.getTop(), bound.getLeft()));
            boundedPoints.add(OsmUtil.calcPos(mapProjection, bound.getTop(), bound.getRight()));

        }

        if (boundedPoints.isEmpty()) {
            return null;
        } else {
            return new AxisAlignedBoundingBoxXZ(boundedPoints);
        }

    }

    private void createMapNode(OSMNode node, Collection<MapNode> mapNodes) {
        VectorXZ nodePos = OsmUtil.calcPos(mapProjection, node.lat, node.lon);
        MapNode.Location location = null;
        if (targetBounds.isPreDbStyle()) {
            if (targetBounds.isBoundaryNode(nodePos)) {
                location = MapNode.Location.GRIDNODE;
            } else {
                location = (targetBounds.isInside(nodePos)) ? MapNode.Location.INSIDEGRID : MapNode.Location.OUTSIDEGRID;
            }
        }
        MapNode mapNode = new MapNode(nodePos, node, location);
        mapNodes.add(mapNode);
        nodeMap.put(node, mapNode);
    }

    public SceneryProjection getProjection() {
        return mapProjection;
    }


    private List<OSMWay> findWaysOfNode(OSMNode node, Collection<OSMWay> ways) {
        List<OSMWay> result = new ArrayList<>();

        for (OSMWay way : ways) {
            if (way.getNodes().contains(node)) {
                result.add(way);
            }
        }
        return result;
    }
}
