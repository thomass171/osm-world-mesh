package de.yard.threed.osm2scenery.elevation;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2world.JTSConversionUtil;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.traffic.geodesy.ElevationProvider;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hier werden bekannte Elevation für Coordinates registriert.
 * Background kann sich dann benötigte rausholen.
 * Wie der mit dem Calculator agiert ist noch nicht ganz klar. Evtl. ist er dessen Datenpool.
 * 2.8.18: Eine Instanz gibt es nur, wenn Elevation aktiv (Provider vorhanden) ist.
 * Eine Defaultelevation gibt es nicht als Vorgabe, das ist doch witzlos. Fuer Tests gibts spezielle ElevationProvider.
 * <p>
 * Created on 26.07.18.
 */
public class ElevationMap {
    Logger logger = Logger.getLogger(ElevationMap.class);
    private static ElevationMap instance = null;
    // unveraenderliche Fixpunkte
    Map fixes;
    //hier kommen nach und nach alle fixierten Groups rein.
    //2.8.18: Ob das noch gebraucht wird, so wie ElevationCalculator jetzt arbeitet?
    //22.8.18: Warum nicht? Allerdings ist die Info hier redundant zu in der Group (isFixed())
    //5.9.18: Ja, das ist einfach eine Liste aller bekannten gefixten Elevations
    private EleConnectorGroupSet fixings = new EleConnectorGroupSet();
    
    //projected nur bis kein Coordinate mapping mehr gebraucht wird.
    @Deprecated
    SceneryProjection mapProjection;
    //28.8.18: das scheint ein guter Ort. Die Sceneryobjects.elevations enthalten nur Referenzen darauf.
    //26.9.18:Jetzt in EleConnectorGroup
    //public static Map<Long, EleConnectorGroup> elegroups = new HashMap<>();
    // Zentrale Ablage fuer die bekannten/fixen Elevations aller TerrainPolygonpunkte. multiple elevations darf/kann es nicht geben
    // Soll nur vom BG verwendet(gelesen) werden!
    // 31.8.18: group ablegen, dann brauchts den EleConnector nicht. Das mapping aller Coordinates->Group ist in der Group.cmap.
    // 05.09.18: Group geht nicht, weil durch Interpolation es Coordinates auf Boundaries geben kann. Die haben keine group. Also doch Elevation.
    // 12.6.19: Ist das nicht doppelt zur cmap in EleGroup? Soll das? Ist das nur für o.a. BoundaryCoors?
    private Map<Coordinate, Double> cmap = new HashMap<>();
    public List<Coordinate> problemlist = new ArrayList<>();

    //private float defaultElevation;
    ElevationProvider elevationProvider;

    private ElevationMap(ElevationProvider elevationProvider, GridCellBounds gridCellBounds, SceneryProjection mapProjection) {
        this.elevationProvider = elevationProvider;
        //this.gridCellBounds = gridCellBounds;
        this.mapProjection = mapProjection;
    }

    /*public static boolean isFixed(EleConnectorGroup eleConnectorGroup) {
        return false;
    }*/

    public static ElevationMap getInstance() {
        if (instance == null) {
            throw new RuntimeException("no inctance of ElevationMap");
        }
        return instance;
    }

    public static boolean hasInstance() {
        return instance != null;
    }

    public void fix(EleConnectorGroup eleConnectorGroup) {
        fixings.add(eleConnectorGroup);
    }

    /**
     * Average der bekannten Fixes liefern. 2.8.18: Wenns keine gibt liefer ich den Default.
     *
     * @return
     */
    public double getAverage() {
        Double average = fixings.getWeightedAverage(null);
        if (average == null) {
            logger.error("no known elavation. Using N0,E0 .");
            // 3.8.18: Eine Default Elevation ist doch Quatsch
            average = elevationProvider.getElevation(0, 0);
            return average;//defaultElevation;
        }
        return average;
    }

    /**
     * Sets/Fixes(?) elevation for all Elegroups? No! Only for grid.
     * Das Grid definiert Eckpunkte und evtl. eine Elevation dazu.
     *
     */
    public static void init(ElevationProvider elevationProvider, GridCellBounds gridCellBounds, SceneryProjection mapProjection) {
        if (!gridCellBounds.isLocked()){
            throw new RuntimeException("not locked");
        }
        instance = new ElevationMap(elevationProvider, gridCellBounds, mapProjection);
        // Aus dem Grid Fixings herleiten. Solange die nicht im Grid selber stehen, über den Provider.
        for (int i = 0; i < gridCellBounds.coords.size(); i++) {
            GridCellBounds.BoundaryNode mapNode = gridCellBounds.basicnodes.get(i);
            LatLon loc = gridCellBounds.coords.get(i);
            //List<EleConnector> el = new ArrayList<>();
            //el.add(new EleConnector(JTSConversionUtil.vectorXZToJTSCoordinate(mapNode.getPos())));
            EleConnectorGroup eleConnectorGroup = EleConnectorGroup.elegroups.get(mapNode.mapNode.getOsmId());
            eleConnectorGroup.fixLocation(EleConnectorGroup.Location.GRIDNODE);
            eleConnectorGroup.fixElevation(elevationProvider.getElevation(loc.getLatDeg().getDegree(), loc.getLonDeg().getDegree()));//instance.getElevation(coor));
            instance.fixings.add(eleConnectorGroup);
            //instance.elegroups.put(mapNode.getOsmId(), eleConnectorGroup);
            //und die auch gleich registrieren. Nein, erst nach Triangulation. In der Group.cmap ist es schon drin.
            //instance.registerElevation(coor, eleConnectorGroup.getElevation(), eleConnectorGroup);

        }
    }

    /**
     * Convenience Methode, um fuer Coordinate zum ElevationProvider zu kommen.
     * Bevorzugt solle die Elevation aber doch ueber die MapNode ermittelt werden!
     * Aber ob das immer so geht, also ob immer eine vorhanden ist; z.B. am Grid?
     * Und nur dafuer braucnhe ich hier die Projection! Darum sofort mal deprecated.
     *
     * @param coor
     * @return
     */
    @Deprecated
    public Double getElevation(Coordinate coor) {
        VectorXZ xz = JTSConversionUtil.vectorXZFromJTSCoordinate(coor);
        LatLon latlon = OsmUtil.calcLatLon(mapProjection,xz);
        //double lon = OsmUtil.calcLon(mapProjection,xz);
        return elevationProvider.getElevation((float) latlon.getLatDeg().getDegree(), (float) latlon.getLonDeg().getDegree());
    }

    public static void drop() {
        instance = null;

    }

    /**
     * Die Elevation anhand des Grid fixen, wenn die Group an einer grid node oder ausserhalb des Grid liegt.
     * 1.9.18: Das wird aber nur fuer Groups mit Mapnode gemacht, also nicht fuer z.Z. nur gapfiller. Es über die
     * Coordinates in der Group zu machen waere nicht eindeutig zuverlässig und fragwürdig.
     * @param eg
     */
    public void fixOuterAndGridnodeEleConnectorGroup(EleConnectorGroup eg) {
        if (eg.mapNode==null){
            return;
        }
        Coordinate c = eg.getCoordinate();
        if (eg.atGridNode() ) {
            Double elevation = getElevation(c);
            //12.7.19 entbehrlich? eg.fixLocation( EleConnectorGroup.Location.GRIDNODE);
            eg.fixElevation(elevation);
            fixings.add(eg);
        }else {
            if (!EleConnectorGroup.gridCellBounds.isInside(eg.mapNode)) {
                Double elevation = getElevation(c);
                //12.7.19 entbehrlich?eg.fixLocation(EleConnectorGroup.Location.OUTSIDEGRID);
                eg.fixElevation(elevation);
                fixings.add(eg);
            }
        }
    }

    /**
     * Das sollte nicht fuer Background gemacht werden. Wohl aber fuer Grid.
     * 31.8.18: Seit anderere EleGroup Nutzung ist das doch obselelt, oder?
     * Mal deprecated und ohne warning.
     * 
     * 05.09.18: Nicht mehr deprecated, weil es durch Interpolation Coordinates auf Boundaries geben kann (z.B. ramp). Die haben keine group. Also doch Elevation.
     * 26.7.19??
     */
    @Deprecated
    public void registerElevation(Coordinate c, Double elevation, EleConnectorGroup sfo) {
        if (cmap.get(c) != null) {
            // kommt z.B. an Gridcellpunkten vor. Und noch bei den Groundfillern.
            //logger.warn("multiple elevation for "+c);
        }
        if (elevation/*sfo.getElevation()*/ == null) {
            logger.warn("elevation isType null");
        }
        cmap.put(c, elevation);//new ElevatedCoordinate(elevation,sfo));
    }

    /**
     * Returns null if unknown. Might happen for triangulated background. The caller must decide
     *
     * @param coordinate
     * @return
     */
    public Double getElevationForCoordinate(Coordinate coordinate) {
        if (!cmap.containsKey(coordinate)) {
            logger.warn("no elevation found for " + coordinate);
            return null;
        }

        /*EleConnectorGroup*/Double e = cmap.get(coordinate);
        if (e == null) {
            logger.warn("no elevation set for " + coordinate);
            return null;
        }
        return e/*.getElevation()*/;
    }

    /**
     * For cases where coordinate wasn't found.
     *
     * @param coordinate
     * @return
     */
    public Double getElevationForClosestCoordinate(Coordinate coordinate) {
        List<Coordinate> clist = new ArrayList<>(cmap.keySet());
        int index = JtsUtil.findClosestVertexIndex(coordinate, clist);
        return getElevationForCoordinate(clist.get(index));
    }

    /**
     * Das Grid definiert ja outer polygon coordinates. Die wurden schon ganz zu Beginn registriert. Nee, hier, wegen nach Triangulation.
     * Die Gridnodes aber noch registrieren, bevor BG elevated werden kann.
     * 26.7.19: Die Coordinate der Gridnode zu registrieren, kann doch nicht richtig sein, weil das gar keine Coordinate eines Polygons ist. Oder? Das lass ich mal weg.
     */
    public void registerGridElevations() {
        for (int i = 0; i < EleConnectorGroup.gridCellBounds.coords.size(); i++) {
            GridCellBounds.BoundaryNode mapNode = EleConnectorGroup.gridCellBounds.basicnodes.get(i);
            //Coordinate coor = EleConnectorGroup.gridCellBounds.vl.coorlist.get(i);
            EleConnectorGroup eleConnectorGroup = EleConnectorGroup.elegroups.get(mapNode.mapNode.getOsmId());
            registerElevation(mapNode.coordinate, /*eleConnectorGroup.getElevation(),*/ eleConnectorGroup.getElevation(),null);
        }
        /*26.7.19 siehe header for (MapNode n : EleConnectorGroup.gridCellBounds.gridnodes) {
            EleConnectorGroup eleConnectorGroup = EleConnectorGroup.elegroups.get(n.getOsmId());
            if (!eleConnectorGroup.isFixed()) {
                logger.warn("not fixed");
            }
            //TODO 30.8.18:die coordinate berechnung ist doof
            registerElevation(JTSConversionUtil.vectorXZToJTSCoordinate(n.getPos()), /*eleConnectorGroup.getElevation(),* / eleConnectorGroup.getElevation(),null);
        }*/
    }

    /**
     * Returns true if the node isType a "grid enter" dummy node.
     *
     * @param node
     * @return
     */
    public static boolean isGridNode(MapNode node) {
        if (node == null || node.getOsmId() > 0) {
            return false;
        }
       /* if (gridCellBounds.gridnodes.contains(node)){
            return true;
        }*/
        return true;
    }
    
    /*public float getDefaultElevation() {
        return defaultElevation;
    }*/

    /**
     * For saving the origin of an elevated coordinate. For analyzing.
     */
    class ElevatedCoordinate {
        Float elevation;
        EleConnectorGroup elevationRegistrator;

        ElevatedCoordinate(Float elevation, EleConnectorGroup elevationRegistrator) {
            this.elevation = elevation;
            this.elevationRegistrator = elevationRegistrator;
        }
    }
}


