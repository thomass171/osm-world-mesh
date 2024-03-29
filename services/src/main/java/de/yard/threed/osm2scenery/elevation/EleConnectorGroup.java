package de.yard.threed.osm2scenery.elevation;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.Phase;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.owm.services.persistence.MeshLine;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.*;
import org.apache.log4j.Logger;


import java.util.*;


/**
 * Eine Menge von EleConnector (Coordinates eines Polygon), die alle dieselbe Elevation haben. Z.B. Strassenenden, Brückenrampen, Generell gegeüberliegende
 * Roads etc Punkte. Seen.
 * Kann auch Sceneryobjekt übergreifen, z.B. bei Supplements, Ramps. Grundsätzlich gibts dafuer bei Ways aber die Connector.
 * Ich sehe auch mal dfen Bezug zu einer MapNode vor, auch wenn noch gar nicht sicher ist, dass es dasi mmer gebebn kann.
 * 1.8.18: Was ist denn dann diese eine Node? Die Nodes sind doch in den Connector. Kann auch null sein, z.B. bei Background.
 * NeeNee, das ist bei Road z.B. die Node die diese Group gebildet hat.
 * <p>
 * 1.8.18: Und ich mappe die MapNode auf alle ihre Groups. Dann komme ich gut an die Neighbor. Naja, vielleicht zu viele hin und her.
 * Aber es ist ja schon wichtig, dass benachbarte Groups dieselbe Elevation haben. Eigentlich sollte es dann aber nicht benachbarte Groups geben,
 * sondern bestehende werden geshared, wie z.B. durch Connector.
 * <p>
 * Wenn dann das Grid auf OsmNode beruht, koennte die node mandatory sein. Aber wer weiss, ob das jemals so ist.
 * Im Zweifel kann es eine Dummynode sein.
 * 26.9.18: Eine EleConnectorGroup gibt es auch ohne Elevation, weil es ja auch connector sind. 12.7.19: Stimmt das wirklich noch?
 * 24.4.19: Erst anlegen, wenn die Polygone fix sind. 12.7.19: NeeNee, passiert vorher.
 */
public class EleConnectorGroup implements Iterable<EleCoordinate> {
    static Logger logger = Logger.getLogger(EleConnectorGroup.class.getName());
    private static boolean initCompleted = false;
    //3.8.18: Brauchen wir die hier wirklich?
    public Coordinate location = null;
    /**
     * Hab ich aus dem Connector hierin verschoben.
     * <p>
     * indicates whether this connector should be connected to the terrain,
     * or isType instead above or below the terrain
     */
    public GroundState groundState;

    // map fuer alle Coordinates->Group, auch nicht fixe. damit hat jede Coordinate genau eine Group
    // 1.9.18: Heikel, denn hier sollen ja nur explizit zugeordnete Coordinates sein. Wies jetzt ist findet er schon wenn die Coors passen.
    // Tja, oder soll das so sein? Results to the question: are two points with equal coordinates the same?
    // Vielleicht brauch man doch einen PolygonPoint (statt Eleconnector)? Dann wäre die Antwort nein.
    private static Map<Coordinate, EleConnectorGroup> cmap = new HashMap<>();
    // 26.9.18: Aus ElevationMap hierhin. Bei Bridges ist das oben an der Ramp.
    public static Map<Long, EleConnectorGroup> elegroups = new HashMap<>();
    //TODO make private
    public final List<EleCoordinate> eleConnectors;
    //mapNode kann auch null sein, z.B. am Grid; ob das sinnvoll ist, sei mal dahingestellt.
    //eher nicht, denn aus der Mapnode ergeben sich die Ursprungskoordinaten, oder?
    //30.8.18: Grid hat jetzt dummies. BG verwendet keine Groups.
    // 01.09.18 Aber es bleiben Gapfiller. Und nur dafuer ShadowNodes einzufuehren ersetzt eine Ausnahme durch die andere
    public MapNode mapNode;
    private Double elevation = null;
    public static GridCellBounds gridCellBounds;
    private int id;
    private static int idc = 1;
    //Um undesired usage finden zu koennen
    public boolean locked = false;
    // 29.3.24 deprecated like mapnode.location
    @Deprecated
    public Location gridlocation;

    public EleConnectorGroup(MapNode mapNode) {
        this(mapNode, new ArrayList<EleCoordinate>());
        id = idc++;
        if (mapNode != null && mapNode.location != null) {
            switch (mapNode.location) {
                case INSIDEGRID:
                    gridlocation = Location.INSIDEGRID;
                    break;
                case OUTSIDEGRID:
                    gridlocation = Location.OUTSIDEGRID;
                    break;
                case GRIDNODE:
                    gridlocation = Location.GRIDNODE;
                    break;
                default:
                    //ist location nicht mandatory?
                    logger.warn("no location in mapnode");
            }
        } else {
            int h = 9;
        }
    }

    public EleConnectorGroup(MapNode mapNode, List<EleCoordinate> eleConnectors) {
        id = idc++;
        if (mapNode == null) {
            mapNode = null;
        }

        this.mapNode = mapNode;
        this.eleConnectors = eleConnectors;
        if (mapNode != null) {
            this.location = JTSConversionUtil.vectorXZToJTSCoordinate(mapNode.getPos());
        }
        for (EleCoordinate e : eleConnectors) {
            e.setGroup(this);
            cmapput(e.coordinate, this);
        }
        if (mapNode != null && mapNode.getOsmId() == 256588789) {
            int h = 9;
        }
    }

    public static void init(GridCellBounds gridCellBounds, SceneryProjection mapProjection) {
        clear();
        EleConnectorGroup.initCompleted = false;
        EleConnectorGroup.gridCellBounds = gridCellBounds;
        if (gridCellBounds == null) {
            return;
        }

        if (gridCellBounds.isPreDbStyle()) {
            // Aus dem Grid Fixings herleiten. Solange die nicht im Grid selber stehen, über den Provider.
            for (int i = 0; i < gridCellBounds.coords.size(); i++) {
                GridCellBounds.BoundaryNode mapNode = gridCellBounds.basicnodes.get(i);
                // LatLon loc = gridCellBounds.coords.get(i);
                List<EleCoordinate> el = new ArrayList<>();
                el.add(new EleCoordinate(JTSConversionUtil.vectorXZToJTSCoordinate(mapNode.mapNode.getPos())));
                EleConnectorGroup eleConnectorGroup = new EleConnectorGroup(mapNode.mapNode, el);
                //eleConnectorGroup.setElevation(elevationProvider.getElevation((float) loc.lat, (float) loc.lon));//instance.getElevation(coor));
                //instance.fixings.add(eleConnectorGroup);
                elegroups.put(mapNode.mapNode.getOsmId(), eleConnectorGroup);
                //und die auch gleich registrieren. Nein, erst nach Triangulation. In der Group.cmap ist es schon drin.
                //instance.registerElevation(coor, eleConnectorGroup.getElevation(), eleConnectorGroup);
                gridCellBounds.elegroups.add(eleConnectorGroup);
            }
        }
        EleConnectorGroup.initCompleted = true;
    }
   

    /*public void addConnectorsFor(Iterable<VectorXZ> positions,
                                 Object reference, GroundState groundState) {

        for (VectorXZ pos : positions) {
            eleConnectors.add(new EleConnector(pos, reference/*, groundState* /));
        }

    }

    public void addConnectorsFor(PolygonWithHolesXZ polygon,
                                 Object reference, GroundState groundState) {

        addConnectorsFor(polygon.getOuter().getVertices(), reference, groundState);

        for (SimplePolygonXZ hole : polygon.getHoles()) {
            addConnectorsFor(hole.getVertices(), reference, groundState);
        }

    }*/

    /*public void addConnectorsForTriangulation(Iterable<TriangleXZ> triangles,
                                              Object reference, GroundState groundState) {
        //TODO check later whether this method isType still necessary

        Set<VectorXZ> positions = new HashSet<VectorXZ>();

        for (TriangleXZ t : triangles) {
            positions.add(t.v1);
            positions.add(t.v2);
            positions.add(t.v3);
        }

        addConnectorsFor(positions, null, groundState);

    }*/

    public void add(EleCoordinate newConnector) {
        if (locked) {
            logger.error("add:already locked");
        }
        if (id == 344) {
            int h = 9;
        }
        if (newConnector.coordinate.distance(new Coordinate(29.7, -92.4)) < 0.5) {
            int h = 9;
        }
        //14.8.19: Mal auf duplicate pruefen. Das dürfte dann nicht mehr konsistent sein, weils dann mehrere Groups für eine Coordinate gibt.
        if (getGroup(newConnector.coordinate) != null) {
            logger.warn("duplicate EleCoordinate " + newConnector.coordinate);
            SceneryContext.getInstance().warnings.add("duplicate");
        }
        eleConnectors.add(newConnector);
        cmapput(newConnector.coordinate, this);
    }

    public void addAll(Iterable<EleCoordinate> newConnectors) {
        if (locked) {
            logger.error("addAll:already locked");
        }
        if (id == 344) {
            int h = 9;
        }
        for (EleCoordinate c : newConnectors) {
            eleConnectors.add(c);
            c.setGroup(this);
            cmapput(c.coordinate, this);
        }

    }

    private void cmapput(Coordinate c, EleConnectorGroup o) {
        //25.4.19: Keine schöne Prüfung, aber es sollen keine verfrühten Coordinates registriert werden.
        if (initCompleted && !Phase.POLYGONSREADY.reached()) {
            throw new RuntimeException("invalid phase");
        }
        cmap.put(c, o);
    }

    public Double getElevation() {
        return elevation;
    }

    /**
     * 12.7.19: TODO: Geht das nicht im Constructor? Wahrscheinlich schon ueber mapnode.
     *
     * @param location
     */
    @Deprecated
    public void fixLocation(Location location) {
        if (mapNode != null && mapNode.getOsmId() == 256588788) {
            int h = 9;
        }
        if (elevation != null && elevation < 0.1) {
            int h = 9;
        }
        if (this.gridlocation != null) {
            logger.warn("overriding fixed location?");
        }
        this.gridlocation = location;
    }

    public void fixElevation(Double elevation) {
        if (gridlocation == null) {
            //might happen for bridge gap
            logger.warn("unknown grid location of ele group. possible bridge base? mapNode=" + mapNode);
            SceneryContext.getInstance().warnings.add("unknown grid location of ele group");
        }
        if (mapNode != null && mapNode.getOsmId() == 256588788) {
            int h = 9;
        }
        if (elevation != null && elevation < 0.1) {
            int h = 9;
        }
        if (isFixed()) {
            logger.warn("overriding fixed elevation?");
            if (elevation.floatValue() != this.elevation.floatValue()) {
                logger.error("severe?");
            }
        }
        this.elevation = elevation;
    }

    public boolean hasElevation() {
        return elevation != null;
    }

    /*public EleConnector getConnector(VectorXZ pos) {
        //TODO review this method (parameters sufficient? necessary at all?)

        for (EleConnector eleConnector : eleConnectors) {
            if (eleConnector.pos.equals(pos)) {
                return eleConnector;
            }
        }

        return null;
        //TODO maybe ... throw new IllegalArgumentException();

    }*/

    /*public List<EleConnector> getConnectors(Iterable<VectorXZ> positions) {

        List<EleConnector> connectors = new ArrayList<EleConnector>();

        for (VectorXZ pos : positions) {
            EleConnector connector = getConnector(pos);
            connectors.add(connector);
            if (connector == null) {
                throw new IllegalArgumentException();
            }
        }

        return connectors;

    }*/

   /* public VectorXYZ getPosXYZ(VectorXZ pos) {

        EleConnector c = getConnector(pos);

        if (c != null) {

            return c.getPosXYZ();

        } else {

            return pos.xyz(0);
            //TODO maybe ... throw new IllegalArgumentException();

        }

    }*/

  /*  public List<VectorXYZ> getPosXYZ(Collection<VectorXZ> positions) {

        List<VectorXYZ> result = new ArrayList<VectorXYZ>(positions.size());

        for (VectorXZ pos : positions) {
            result.add(getPosXYZ(pos));
        }

        return result;

    }*/

   /* public PolygonXYZ getPosXYZ(SimplePolygonXZ polygon) {
        return new PolygonXYZ(getPosXYZ(polygon.getVertexLoop()));
    }

    public Collection<TriangleXYZ> getTriangulationXYZ(
            Collection<? extends TriangleXZ> trianglesXZ) {

        Collection<TriangleXYZ> trianglesXYZ =
                new ArrayList<TriangleXYZ>(trianglesXZ.size());

        for (TriangleXZ triangleXZ : trianglesXZ) {

            VectorXYZ v1 = getPosXYZ(triangleXZ.v1);
            VectorXYZ v2 = getPosXYZ(triangleXZ.v2);
            VectorXYZ v3 = getPosXYZ(triangleXZ.v3);

            if (triangleXZ.isClockwise()) { //TODO: ccw test should not be in here, but maybe in triangulation util
                trianglesXYZ.add(new TriangleXYZ(v3, v2, v1));
            } else {
                trianglesXYZ.add(new TriangleXYZ(v1, v2, v3));
            }

        }

        return trianglesXYZ;

    }*/

    @Override
    public Iterator<EleCoordinate> iterator() {
        return eleConnectors.iterator();
    }
    
	/*public static final EleConnectorGroup EMPTY = new EleConnectorGroup(
            Collections.<EleConnector>emptyList());*/

    public void setGroundState(GroundState groundState) {
        this.groundState = groundState;
    }

    public boolean isFixed() {
        return elevation != null;
    }

    /**
     * 22.8.18: Auch noch nicht so ganz das wahre. Nee, es kann ja mehrere geben!
     * 31.8.18: wenn es über die mapnode geht, ginge es.
     *
     * @return
     */
    public Coordinate getCoordinate() {
        if (mapNode != null) {
            return JTSConversionUtil.vectorXZToJTSCoordinate(mapNode.getPos());
        }
        return eleConnectors.get(0).coordinate;
    }

    public boolean atGridNode() {
        return mapNode != null && mapNode.getOsmId() < 0;
    }

    /**
     * Never returns null.
     * 28.9.18: Doch, wenn es ein TriangulationVertex ist schon.
     * 16.6.19: Oder durch freie BG Coordinates nach LazyCut. Ob das Runden so gut ist?
     * 19.7.19: Auch durch Decorations.
     *
     * @param coor
     * @return
     */
    static public EleConnectorGroup getGroup(Coordinate coor, boolean isPossibleUnknownVertex, String label, boolean silently, TerrainMesh tm) {
        EleConnectorGroup e = getGroup(coor);
        if (e == null) {
            // Der Sache auf den Grund gehen
            boolean containskey = cmap.containsKey(coor);
            List<MeshLine> meshLines = tm.findLines(null, coor);
            MeshLine meshLine = (meshLines.size() > 0) ? meshLines.get(0) : null;
            //Der cut ist dafuer die häufigste Ursache. Erstmal nicht mehr loggen, weil es zu oft vorkommt.
            //5.9.18: Der cut wird jetzt gehandelt. Darum wieder log. Und mal checken, ob ein Rundungsfehler die Ursache sein kann.
            //28.9.18: Ansonsten ist ja noch Triangulation ein klare moegliche Ursache. Dann zwar auch Rundungsfehler checken,
            //weiter aber nicht loggen.
            boolean possibleroundingproblem = false;
            List<Coordinate> allc = new ArrayList<>(cmap.keySet());
            int index = JtsUtil.findClosestVertexIndex(coor, allc);
            if (index != -1) {
                if (allc.get(index).distance(coor) < 0.01f) {
                    possibleroundingproblem = true;
                    e = cmap.get(allc.get(index));
                }
            }
            // Es kann wohl zumindest beim BG roundings problem geben
            if (!isPossibleUnknownVertex && !possibleroundingproblem) {
                if (!silently) {
                    logger.warn("no ele group found (" + label + ") for coordinate " + coor + ",possibleroundingproblem=" + possibleroundingproblem + ",containskey=" + containskey + ",meshLine=" + meshLine);

                    SceneryContext.getInstance().warnings.add("no eleconnector found for coordinate " + coor + ",possibleroundingproblem=" + possibleroundingproblem);
                    SceneryContext.getInstance().unknowncoordinates.add(coor);
                }
            }
        }
        return e;
    }

    static public Collection<Coordinate> getKnownCoordinates() {
        return Collections.unmodifiableCollection(cmap.keySet());
    }

    static public EleConnectorGroup getGroup(Coordinate coor) {
        return cmap.get(coor);
    }

    /**
     * Pruefen, ob eine Coordinate schon zu einer Group registriert wurde.
     *
     * @param coor
     * @return
     */
    static public boolean hasGroup(Coordinate coor) {
        return cmap.containsKey(coor);
    }

    public static void clear() {
        cmap.clear();
        elegroups.clear();
        EleConnectorGroup.gridCellBounds = null;
    }

    /**
     * only for testing
     *
     * @return
     */
    public static Collection<EleConnectorGroup> getAllGroupsDistinct() {
        List<EleConnectorGroup> l = new ArrayList(new HashSet(cmap.values()));
        for (EleConnectorGroup g : l) {
            //logger.debug("group:"+g.mapNode);
        }
        return l;
    }

    public static enum Location {
        //vielleicht auch ein ONGRID??
        INSIDEGRID, OUTSIDEGRID, GRIDNODE;
    }
}
