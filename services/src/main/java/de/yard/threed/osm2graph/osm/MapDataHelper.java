package de.yard.threed.osm2graph.osm;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Util;
import de.yard.threed.osm2scenery.util.PolygonMetadata;
import de.yard.threed.osm2scenery.util.SmartPolygon;
import de.yard.threed.osm2world.InvalidGeometryException;
import de.yard.threed.osm2world.JTSConversionUtil;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.MapWaySegment;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.SimplePolygonXZ;
import de.yard.threed.osm2world.VectorXZ;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 01.06.18.
 */
public class MapDataHelper {

    private final MapData mapData;
    static Logger logger = Logger.getLogger(MapDataHelper.class.getName());


    public MapDataHelper(MapData mapData) {
        this.mapData = mapData;
    }

    public MapNode findOsmNode(long id) {
        for (MapNode n : mapData.getMapNodes()) {
            if (n.getOsmNode().id == id) {
                return n;
            }
        }
        return null;
    }

    /**
     * Aus AbstractNetworkWaySegmentWorldObject.
     * Immer rightnormal zu nehemn ist evtl. nicht ideal, GraphOutline aber auch nocht unbedingt. Das könnte mann noch ausfeilen.
     */
    public static List<VectorXZ> getOutlineXZFromMapWay(MapWay mapWay, double halfWidth, boolean right) {
        List<VectorXZ> outlineXZ = new ArrayList<VectorXZ>();

        for (MapWaySegment mapWaySegment : mapWay.getMapWaySegments()) {
            mapWaySegment.getRightNormal();
            if (outlineXZ.size() == 0) {
                outlineXZ.add(mapWaySegment.getStartNode().getPos().add(mapWaySegment.getRightNormal().mult(((right) ? 1 : -1) * halfWidth)));
            }
            outlineXZ.add(mapWaySegment.getEndNode().getPos().add(mapWaySegment.getRightNormal().mult(((right) ? 1 : -1) * halfWidth)));
        }
        return outlineXZ;
    }

    public static List<VectorXZ> getOutlineXZFromMapWay(MapWaySegment mapWaySegment, double halfWidth, boolean right) {
        List<VectorXZ> outlineXZ = new ArrayList<VectorXZ>();

        mapWaySegment.getRightNormal();
        if (outlineXZ.size() == 0) {
            outlineXZ.add(mapWaySegment.getStartNode().getPos().add(mapWaySegment.getRightNormal().mult(((right) ? 1 : -1) * halfWidth)));
        }
        outlineXZ.add(mapWaySegment.getEndNode().getPos().add(mapWaySegment.getRightNormal().mult(((right) ? 1 : -1) * halfWidth)));
        return outlineXZ;
    }

    public static List<VectorXZ> getOutlineXZ(VectorXZ from, VectorXZ to, double halfWidth, boolean right) {
        List<VectorXZ> outlineXZ = new ArrayList<VectorXZ>();

        VectorXZ dir = to.subtract(from);
            outlineXZ.add(from.add(dir.rightNormal().mult(((right) ? 1 : -1) * halfWidth)));

        outlineXZ.add(to.add(dir.rightNormal().mult(((right) ? 1 : -1) * halfWidth)));
        return outlineXZ;
    }

    /**
     * Aus AbstractNetworkWaySegmentWorldObject.
     * calculate the outline loop
     * <p>
     * 18.7.18: Liefert null bei broken, weil es sinnlos ist mit broken weiterzumachen.
     * 24.7.18: Die Gefahr ist groß, dass es durch die Outlinebildung zu self intersecting Polys kommt.
     * Darum jetzt pro Segment einen Poly ermitteln und dann mergen. Dann entstehen evtl. aber doofe Spitzen.
     * 26.7.18: Mal meinen Outline versuchen (unten). Darum hier deprecated
     * 20.8.18: Nicht mehr deprecated, sondern als Fallback wenn die Erzeugung ueber Graph scheitert, z.B. Roundabout.
     * 30.8.18: Aber auch sonst fuer z.B. Gapfiller. Darf daher nichts irgendow extern/global registrieren.
     * 23.5.19: Ueber NodeList statt mapway
     *
     */
    public static SmartPolygon/*SimplePolygonXZ*/ getOutlinePolygon(List<MapNode> nodelist/*MapWay mapWay*/, double width) {
        boolean persinglepolygon = true;
        if (persinglepolygon) {
            Polygon result = null;
            //Map<Long, List<VectorXZ>> nodemap = null;
            //TODO der parameter ist ungeeignet?
            PolygonMetadata polygonMetadata = new PolygonMetadata(null/*mapWay*/);

            List<VectorXZ> leftOutlineXZ = null, rightOutlineXZ = null;
            //for (MapWaySegment seg : mapWay.getMapWaySegments()) {
            for (int i=0;i<nodelist.size()-1;i++){
                //leftOutlineXZ = getOutlineXZFromMapWay(seg, width / 2, false);
                //rightOutlineXZ = getOutlineXZFromMapWay(seg, width / 2, true);
                leftOutlineXZ = getOutlineXZ(nodelist.get(i).getPos(),nodelist.get(i+1).getPos(), width / 2, false);
                rightOutlineXZ = getOutlineXZ(nodelist.get(i).getPos(),nodelist.get(i+1).getPos(), width / 2, true);

                /*das geht so nicht if (nodemap == null) {
                    nodemap = new HashMap<>();
                    List<VectorXZ> startnodelist = new ArrayList<>();
                    startnodelist.add(leftOutlineXZ.get(0));
                    startnodelist.add(rightOutlineXZ.get(0));
                    nodemap.put(mapWay.getStartNode().getOsmId(), startnodelist);
                }*/
                SimplePolygonXZ p = polygonFromOutline(leftOutlineXZ, rightOutlineXZ);
                if (p == null) {
                    //was soll man machen?
                    return null;
                }
                Polygon jtsp = JTSConversionUtil.polygonXZToJTSPolygon(p);
                if (result == null) {
                    result = jtsp;
                } else {

                    result = (Polygon) result.union(jtsp);
                }
            }
            /*das geht so nicht List<VectorXZ> endnodelist = new ArrayList<>();
            endnodelist.add(leftOutlineXZ.get(1));
            endnodelist.add(rightOutlineXZ.get(1));
            nodemap.put(mapWay.getEndNode().getOsmId(), endnodelist);*/
            // 23.5.19: Das ist doch dann der zweite Fallback?
            polygonMetadata = PolygonMetadata.buildForCorruptedMapWay(/*mapWay*/nodelist, result);

            return new SmartPolygon(result, polygonMetadata);
        }

        //List<VectorXZ> leftOutlineXZ = getOutlineXZFromMapWay(mapWay, width / 2, false);
       // List<VectorXZ> rightOutlineXZ = getOutlineXZFromMapWay(mapWay, width / 2, true);

        return null;//new SmartPolygon(JTSConversionUtil.polygonXZToJTSPolygon(polygonFromOutline(leftOutlineXZ, rightOutlineXZ)));
    }

    /**
     * Einen Polygon für eine Strecke bauen, inkl. der Centervertices.
     *
     * @param from
     * @param to
     * @param width
     * @return
     */
    public static Polygon getOutlinePolygon(VectorXZ from, VectorXZ to, double width) {
        List<VectorXZ> leftOutlineXZ = null, rightOutlineXZ = null;
        leftOutlineXZ = getOutlineXZ(from, to, width / 2, false);
        rightOutlineXZ = getOutlineXZ(from, to, width / 2, true);

        SimplePolygonXZ p = polygonFromOutline(leftOutlineXZ, rightOutlineXZ);
        if (p == null) {
            //was soll man machen?
            return null;
        }
        Polygon jtsp = JTSConversionUtil.polygonXZToJTSPolygon(p);
        return jtsp;
    }

    public static SmartPolygon createSmartPolygon(CoordinateList coors) {
        PolygonMetadata polygonMetadata = new PolygonMetadata(null);

        Coordinate[] uncutcoord = new Coordinate[coors.size()];
        for (int i = 0; i < coors.size(); i++) {
            Coordinate c = coors.get(i);
            uncutcoord[i] = c;
            if (i < coors.size() - 1) {
                polygonMetadata.addPoint(null, uncutcoord[i]);
            }
        }
        SmartPolygon poly;
        GeometryFactory geometryFactory = new GeometryFactory();
        if (uncutcoord.length < 4) {
            // even possible?
            logger.warn("invalid polygon with uncutcoord.length=" + uncutcoord.length);
            // will fail later with NPE
            return null;
        }
        Polygon polygon = geometryFactory.createPolygon(uncutcoord);
        if (polygon.isValid()) {
            poly = new SmartPolygon(polygon, polygonMetadata);
        } else {
            logger.warn("createSmartPolygon: invalid polygon created.");
            return null;
        }
        return poly;
    }

    static SimplePolygonXZ polygonFromOutline(List<VectorXZ> leftOutlineXZ, List<VectorXZ> rightOutlineXZ) {
        List<VectorXZ> outlineLoopXZ = new ArrayList<VectorXZ>();

        outlineLoopXZ.addAll(rightOutlineXZ);

        List<VectorXZ> left = new ArrayList<VectorXZ>(leftOutlineXZ);
        Collections.reverse(left);
        outlineLoopXZ.addAll(left);

        outlineLoopXZ.add(outlineLoopXZ.get(0));

        // check for brokenness
        SimplePolygonXZ outlinePolygonXZ = null;
        boolean broken = false;
        try {
            outlinePolygonXZ = new SimplePolygonXZ(outlineLoopXZ);
            broken = outlinePolygonXZ.isClockwise();
        } catch (InvalidGeometryException e) {
            logger.error("InvalidGeometryException: broken outline");

            broken = true;
            //connectors = EleConnectorGroup.EMPTY;
            return null;
        } catch (Exception e) {
            // IllegalArgumentException kommt auch schon mal
            logger.error("Exception: broken outline", e);

            broken = true;
            //connectors = EleConnectorGroup.EMPTY;
            return null;
        }
        return outlinePolygonXZ;
    }

    /**
     * Duplicate a Way by using dummy nodes.
     * <p>
     * Erstmal nicht, weil der Nutzen unklar ist. Bisher sind Dummies immer GridEnter nodes.
     *
     * @param mapWay
     * @return
     */
    public static MapWay createShadowMapWay(MapWay mapWay) {
        Util.notyet();
        MapNode prevnode = null;
        MapWay dummyway = null;
        for (int i = 0; i < mapWay.getMapNodes().size(); i++) {
            MapNode node = mapWay.getMapNodes().get(i);
            OSMNode gridosmnode = OsmUtil.buildDummyNode(node.getOsmNode().lat, node.getOsmNode().lon);
            VectorXZ xz = node.getPos();
            MapNode dummynode = new MapNode(xz, gridosmnode,null);
            // Orgiginal OSM way mal nicht ablegen
            if (dummyway == null) {
                dummyway = new MapWay(dummynode, null);
            } else {
                MapWaySegment seg = new MapWaySegment(null, prevnode, dummynode);
                dummyway.add(dummynode, seg);
            }
            prevnode = dummynode;


        }
        return dummyway;
    }

    public static boolean isDummyNode(MapNode mapNode) {
        return mapNode.getOsmId() < 0;
    }
}
