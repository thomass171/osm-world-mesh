package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.MeshServiceFacade;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Added for adding a way to the TerrainMesh.
 */
@Slf4j
public class WayTerrainMeshAdder extends TerrainMeshAdder {
   //20.8.26  SceneryWayObject sceneryWayObject;

    /**
     * TODO das ist doch eine zu grosse Dependency, oder?
     *
     * @param sceneryWayObject
     */
    public WayTerrainMeshAdder(String meshName, MeshServiceFacade meshService, SceneryProjection projection/*, SceneryWayObject sceneryWayObject*/) {
        super(meshName,meshService,projection);
        //20.8.26  this.sceneryWayObject = sceneryWayObject;
    }

    /**
     * 26.2.26 Made static for decoupling. tm temporarily still needed.
     */
    /*26.2.26 @Override
    public void addToTerrainMesh(AbstractArea[] areas, TerrainMesh tm) throws OsmProcessException, MeshInconsistencyException {*/
public /*static*/ void persistWay(SceneryWayObject sceneryWayObject) throws MeshInconsistencyException, OsmProcessException {
        /*16.2.26 TODO NPE if (areas[0].isEmpty(tm)) {
            return;
        }*/

        if (sceneryWayObject.mapWay.getOsmId() == 23696494 || sceneryWayObject.mapWay.getOsmId() == 363500734 || sceneryWayObject.mapWay.getOsmId() == 7093390) {
            int h = 9;
        }

        WayArea wayArea = sceneryWayObject.getWayArea();
        if (wayArea == null) {
            // No area. Check for buildOutlinePolygonFromGraph() warnings
            return;
        }

        //wayArea.leftlines = new ArrayList<>();
        //wayArea.rightlines = new ArrayList<>();
        //16.4.26 wayArea.initLeftRightLines();

        List<Coordinate> leftline = new ArrayList<>();
        List<Coordinate> rightline = new ArrayList<>();

        if (sceneryWayObject.innerConnector != null) {
            //List<SceneryWayConnector> innerConnector=new ArrayList(sceneryWayObject.innerConnector.values());
            for (int segment = 0; segment <= sceneryWayObject.innerConnector.size(); segment++) {
                //    end = getWayArea().getPosition(innerconnector.node);
                CoordinatePair[] pairs = wayArea.getPairsOfSegment(/*19.3.26 segment*/);
                if (pairs == null) {
                    log.error("unexpected");
                    return;
                }
                for (CoordinatePair pair : pairs) {
                    if (pair == null) {
                        log.error("unexpected");
                        return;
                    }
                    leftline.add(pair.left());
                    rightline.add(pair.right());
                }
                int conn = 0;
                if (segment < sceneryWayObject.innerConnector.size()) {
                    SceneryWayConnector con = sceneryWayObject.innerConnector.get(segment);
                    if (con.hasMinor()) {
                        if (con.minorHitsLeft(con.minorway0/*, tm*/)) {
                            conn = 1;
                        } else {
                            conn = 2;
                        }
                    }
                }
                /*20.8.26 if (tm.isPreDbStyle()) {
                    Util.nomore();
                    /*26.2.26 boolean endOnGrid = conn == 0 && sceneryWayObject.endMode == SceneryWayObject.WayOuterMode.GRIDBOUNDARY;

                    if (conn == 0 || conn == 1) {
                        boolean startOnGrid = wayArea.getLeftLines(tm).size() == 0 && sceneryWayObject.startMode == SceneryWayObject.WayOuterMode.GRIDBOUNDARY;
                        MeshLine line = tm.registerLine(leftline, null, areas[0], startOnGrid, endOnGrid);
                        wayArea.addLeftline(line);
                        leftline = new ArrayList<>();
                    }
                    if (conn == 0 || conn == 2) {
                        boolean startOnGrid = wayArea.getRightLines(tm).size() == 0 && sceneryWayObject.startMode == SceneryWayObject.WayOuterMode.GRIDBOUNDARY;
                        wayArea.addRightline(tm.registerLine(rightline, areas[0], null, startOnGrid, endOnGrid));
                        rightline = new ArrayList<>();
                    }* /
                } else*/ {
                    //11.2.26 now via service(contained in tm)
                   /* tm.*/registerWay(sceneryWayObject.osmWayId, null, leftline, rightline, null, 2);

                }
            }
        }else{
            // 18.3.26: Needed now here due to no mid connector? Strange. What are these wayarea segments?
            CoordinatePair[] pairs = wayArea.getPairsOfSegment(/*19.3.26 0*/);
            if (pairs == null) {
                log.error("unexpected");
                return;
            }
            for (CoordinatePair pair : pairs) {
                if (pair == null) {
                    log.error("unexpected");
                    return;
                }
                leftline.add(pair.left());
                rightline.add(pair.right());
            }

            /*tm.*/registerWay(sceneryWayObject.osmWayId, null, leftline, rightline, null, 2);

        }

        // Also consider dead end. Connect left and right lines from above.
        /*19.8.26 if (tm.isPreDbStyle()) {
            Util.nomore();
                    /*26.2.26 if (sceneryWayObject.startMode == SceneryWayObject.WayOuterMode.DEADEND) {
                CoordinatePair p = wayArea.getStartPair(tm)[0];
                tm.registerLine(JtsUtil.toList(p.left(), p.right()), areas[0], null, false, false);
                tm.addKnownTwoEdger(p.left());
                tm.addKnownTwoEdger(p.right());
            }
            if (sceneryWayObject.endMode == SceneryWayObject.WayOuterMode.DEADEND) {
                CoordinatePair p = wayArea.getEndPair()[0];
                tm.registerLine(JtsUtil.toList(p.left(), p.right()), null, areas[0], false, false);
                tm.addKnownTwoEdger(p.left());
                tm.addKnownTwoEdger(p.right());
            }* /
        }*/
        //  lazy cut isType already registerd in GridBounds, but needs left/right

        /*kann man hier nicht pruefen, weil Connector noch fehlen if (!TerrainMesh.getInstance().isValid(true)){
            log.error("invalid after adding way");
        }*/
    }
}
