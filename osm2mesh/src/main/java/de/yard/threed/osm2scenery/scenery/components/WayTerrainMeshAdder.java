package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Added for adding a way to the TerrainMesh.
 */
public class WayTerrainMeshAdder implements TerrainMeshAdder {
    Logger logger = Logger.getLogger(WayTerrainMeshAdder.class);
    SceneryWayObject sceneryWayObject;

    /**
     * TODO das ist doch eine zu grosse Dependency, oder?
     *
     * @param sceneryWayObject
     */
    public WayTerrainMeshAdder(SceneryWayObject sceneryWayObject) {
        this.sceneryWayObject = sceneryWayObject;
    }

    @Override
    public void addToTerrainMesh(AbstractArea[] areas, TerrainMesh tm) throws OsmProcessException {

        if (areas[0].isEmpty(tm)) {
            return;
        }

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
        wayArea.initLeftRightLines();

        List<Coordinate> leftline = new ArrayList<>();
        List<Coordinate> rightline = new ArrayList<>();

        if (sceneryWayObject.innerConnector != null) {
            //List<SceneryWayConnector> innerConnector=new ArrayList(sceneryWayObject.innerConnector.values());
            for (int segment = 0; segment <= sceneryWayObject.innerConnector.size(); segment++) {
                //    end = getWayArea().getPosition(innerconnector.node);
                CoordinatePair[] pairs = wayArea.getPairsOfSegment(segment);
                if (pairs == null) {
                    logger.error("unexpected");
                    return;
                }
                for (CoordinatePair pair : pairs) {
                    if (pair == null) {
                        logger.error("unexpected");
                        return;
                    }
                    leftline.add(pair.left());
                    rightline.add(pair.right());
                }
                int conn = 0;
                if (segment < sceneryWayObject.innerConnector.size()) {
                    SceneryWayConnector con = sceneryWayObject.innerConnector.get(segment);
                    if (con.hasMinor()) {
                        if (con.minorHitsLeft(con.minorway, tm)) {
                            conn = 1;
                        } else {
                            conn = 2;
                        }
                    }
                }
                if (tm.isPreDbStyle()) {
                    boolean endOnGrid = conn == 0 && sceneryWayObject.endMode == SceneryWayObject.WayOuterMode.GRIDBOUNDARY;

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
                    }
                } else {
                  tm.registerWay(null, leftline, rightline ,null,2);
                }
            }
        }

        // Also consider dead end. Connect left and right lines from above.
        if (tm.isPreDbStyle()) {
            if (sceneryWayObject.startMode == SceneryWayObject.WayOuterMode.DEADEND) {
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
            }
        }
        //  lazy cut isType already registerd in GridBounds, but needs left/right

        /*kann man hier nicht pruefen, weil Connector noch fehlen if (!TerrainMesh.getInstance().isValid(true)){
            logger.error("invalid after adding way");
        }*/
    }
}
