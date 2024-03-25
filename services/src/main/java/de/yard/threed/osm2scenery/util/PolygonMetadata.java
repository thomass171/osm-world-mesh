package de.yard.threed.osm2scenery.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.elevation.EleCoordinate;
import de.yard.threed.osm2world.JTSConversionUtil;
import de.yard.threed.osm2world.MapNode;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapping ossmid->coordinates and vice versa in one polygon.
 * 24.5.19 Es gibt nicht immer Mapnode, z.B. Decorations. An das Konzept muss ich noch mal bei.
 *
 * <p>
 * Created on 26.07.18.
 */
public class PolygonMetadata {
    Logger logger = Logger.getLogger(PolygonMetadata.class);
    //Mapping osmnode->Coordinates
    private Map<MapNode, List<Coordinate>> nodemap = new HashMap<>();
    //um die Herkunft einer Coordinate nachzuhalten. Wuerde aber auch ueber Elegroup gehen. Darum deprecated
    //28.9.18: Wer weiss?
    @Deprecated
    private Map<Coordinate, MapNode> coormap = new HashMap<>();
    //31.8.18: map ist jetzt global in EleConnectorGroup. Erst nur deprecatd
    //28.9.18: Unklar, ob es hier nicht auch gebraucht wird.
    private Map<Coordinate, EleCoordinate> eleconnectormap = new HashMap<>();
    //fuer Logging/Test/Analyse
    Object parent;

    public PolygonMetadata(Object parent) {
        this.parent = parent;
    }

    /**
     * mapnode kann (z.B. bei Background) auch null sein.
     * 24.5.19: Auch bei Decorations kann node null sein.
     * @param osmid
     * @param c
     */
    public EleCoordinate addPoint(MapNode osmid, Coordinate c) {
        List<Coordinate> v = nodemap.get(osmid);
        if (v == null) {
            v = new ArrayList<>();
            nodemap.put(osmid, v);
        }
        v.add(c);
        if (coormap.containsKey(c)) {
            logger.warn("Coordinate already registered. Will get lost.");
        }
        coormap.put(c, osmid);
        EleCoordinate e = new EleCoordinate(c);
        eleconnectormap.put(c, e);
        return e;
    }

    /**
     * 16.6.19: deprecated als einstieg in den ausstieg.
     * @param node
     * @return
     */
    /*12.7.19 @Deprecated
    public List<Coordinate> getCoordinated(MapNode node) {
        return nodemap.get(node);
    }*/

    /**
     * Wenn node null ist, werden alle geliefert.
     * 25.4.19: Warum deprecated. Scheint mir grad sinnvoll.
     * @param node
     * @return
     */
    @Deprecated
    public List<EleCoordinate> getEleConnectors(MapNode node) {
        if (node == null) {
            return new ArrayList(eleconnectormap.values());
        }
        List<Coordinate> clist = nodemap.get(node);
        if (clist==null){
            //?? 12.6.19
            logger.warn("no clist??");
            return new ArrayList(eleconnectormap.values());
        }
        List<EleCoordinate> eles = new ArrayList<>();
        for (Coordinate c : clist) {
            EleCoordinate e = eleconnectormap.get(c);
            eles.add(e);
        }
        return eles;
    }

    /**
     * Never returns null.
     *
     * @param coor
     * @return
     */
      /*12.7.19@Deprecated
    public EleCoordinate getEleConnector(Coordinate coor) {
        EleCoordinate e = eleconnectormap.get(coor);
        if (e == null) {
            //Der cut ist dafuer die häufigste Ursache (und Triangulation). Erstmal nicht mehr loggen, weil es zu oft vorkommt.
            //logger.warn("no eleconnector found for coordinate. polygon cut? Using nearest");
            List<Coordinate> allc = new ArrayList<>(eleconnectormap.keySet());
            int index = JtsUtil.findClosestVertexIndex(coor, allc);
            if (index == -1) {
                index = -1;
            }
            e = eleconnectormap.get(allc.get(index));
        }
        return e;
    }*/

    /*29.8.19public List<Coordinate> findClosest(Coordinate coor) {
        List<Coordinate> allc = new ArrayList<>(eleconnectormap.keySet());
        JtsUtil.sortByDistance(allc, coor);
        return allc;
    }*/

    /*public MapNode getOsmId(Coordinate coor) {
        return coormap.get(coor);
    }*/

    /**
     * For entartete MapWay wie z.B. Roundabout und P-förmige.
     * <p>
     * Ordnet jeder MapNode die nächstgelegene Coordinate zu und verteilt die restlichen
     * Coorinates nach ihrem Abstand zu den Nodes.
     * 28.9.18:" Corrupted" ist nicht ricvhtig, weil es durchau regulaer verwendet wird.
     * Das ganze ist eh fragwürdig.
     * 23.5.19: nodelist statt mapway
     * @return
     */
    public static PolygonMetadata buildForCorruptedMapWay(List<MapNode> nodelist/*MapWay mapWay*/, Polygon polygon) {
        PolygonMetadata polygonMetadata = new PolygonMetadata(null/*mapWay*/);
        Coordinate[] coors = polygon.getCoordinates();
        MapNode mapNode = nodelist.get(0);//mapWay.getStartNode();
        Coordinate c = JTSConversionUtil.vectorXZToJTSCoordinate(mapNode.getPos());
        int index = JtsUtil.findClosestVertexIndex(c, coors);
        polygonMetadata.addPoint(mapNode, coors[index]);
        //for (MapWaySegment seg : mapWay.getMapWaySegments()) {
        for (int i=1;i<nodelist.size();i++){
            mapNode = nodelist.get(i);//seg.getEndNode();
            c = JTSConversionUtil.vectorXZToJTSCoordinate(mapNode.getPos());
            index = JtsUtil.findClosestVertexIndex(c, coors);
            polygonMetadata.addPoint(mapNode, coors[index]);
        }
        //die restliche coors erteilen muss vielleicht nicht sein.
        //28.9.18: Doch, sonst werden die Coors nicht registered und bekommen keine Elevation. coormap lass ich aber.
        //und ich pack auch alle an die startnode. Das ganze ist eh fragwürdig.
        for (Coordinate c0 : coors) {
            if (!polygonMetadata.eleconnectormap.containsKey(c0)) {
                polygonMetadata.addPoint(nodelist.get(0)/*mapWay.getStartNode()*/, c0);
            }
        }
        return polygonMetadata;
    }
}
