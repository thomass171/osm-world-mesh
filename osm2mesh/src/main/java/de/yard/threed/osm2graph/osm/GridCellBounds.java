package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.*;

import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.LatLon;
import de.yard.threed.core.Util;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.*;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.components.WayArea;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.openstreetmap.osmosis.core.domain.v0_6.Bound;


import java.io.File;
import java.io.IOException;
import java.util.*;

/*17.3.26 we no longer care about import static de.yard.threed.osm2scenery.scenery.SceneryWayObject.WayOuterMode.GRIDBOUNDARY;

/**
 * The definition of a grid. This is both a boundary/outline polygon and the smallest rectangle that fully covers the outline.
 * <p>
 * Es ist doof, im Constructor schon projected MapNodes reinzustecken, denn dafuer muss man vorab schon mal alles verarbeiten. Besser
 * on the fly bei Nutzung.
 * 13.6.18: MapNodes sind doof wegen freier Koordinaten. Mal ohne versuchen. Einfach mit Koordinaten.
 * 30.8.18: Da es fuer Grid enter ohnehin dummy osm nodes gibt, verwende ich die hier auch.
 * <p>
 * Liefert jetzt die Projection. Der Origin ergibt sich nicht mehr aus den OSM Daten, sondern dem Grid.
 * Origin bzw. wird weiterverwnedet statt links/oben/unten, damit Verzerrungen gleichmässig nach aussen gehen und damit die draw() Funktionen
 * weiterverwendet werden können. Verzerrungen beachte ich erstmal nicht, evtl. müsste man mal was wie OrthographicAzimuthalMapProjection machen.
 * Erstmal mach ich aber eine schnöde simple lineare Projection.
 * Metric ist die Projection nicht mehr, sondern pixelbasiert
 * auf eine Textur in Standardgroesse (2er Potenz).
 * Die äusseren Grideckpunkte liegen exakt auf der Textureaussenkant. Entweder vertikal oder horizontal
 * gibt es dann Verschnitt.
 * <p>
 * 11.4.19: Lineare Projection ist Unsinn, weil die Tiles Richtung Pole schmaler werden, das verzerrt total.
 * Es wird doch eine MetricProjection verwendet, damit die Koordinaten in 2D intuitiv sind.
 * Die müssen dann noch evtl. auf eine Supertextur getappt werden. Vorerst wird es auch Supertexturen mit krummen Größen geben.
 *
 * <p>
 * Das mit der Ableitung ist gar nicht so schön, aber Naja.
 * <p>
 * Skizze 51
 * <p>
 * 26.7.19 Durch SMARTGRID kann sich der genaue Verlauf ändern.
 * 13.2.26: Also DB style uses a boundary now.
 * <p>
 * Created on 02.06.18.
 */
@Slf4j
public class GridCellBounds /*implements TargetBounds*/ {
    //16.5.19: Erstmal nur so
    public List<Long> tripnodes;
    //public SimplePolygonXZ simplePolygonXZ
    private Polygon projectedBoundaryPolygon;
    //The boundary CCW?
    public List<GeoCoordinate/*13.2.26 LatLon*/> coords;
    // die dummy MapNodes zu den definierten Eckpunkten. Ergeben sich aus der Projection.
    // die spielen nach dem Rearrange keine Rolle mehr.
    public List<BoundaryNode> basicnodes = new ArrayList<>();
    public List<EleConnectorGroup> elegroups = new ArrayList<>();
    // additional gridnodes, z.B. for ways. for now here.
    public List<MapNode> additionalGridnodes = new ArrayList<>();
    SceneryProjection projection;
    //size of tile in degrees
    double degsize;
    int texturesize = 512;
    LatLon origin;
    // in degrees
    double bottom, top, left, right;
    VectorXZ bottomleft, topright;
    double maxextension;
    public static GridCellBounds instance;
    private List<BoundaryLine> boundaryLines = new ArrayList<>();
    //For analysis
    public Polygon polygon345, polygon345a;
    private boolean locked = false;
    private Map<Integer, List<LazyCutObject>> mapOfCuts;

    public double degwidth, degheight;

    /**
     * 26.3.24: Deprecated because of new gridless DB approach.
     * 13.2.26: Even in DB we have multiple meshes/tiles. So not deprecated.
     */
    public GridCellBounds(List<GeoCoordinate/*LatLon*/> coords) throws MeshInconsistencyException {
        instance = this;
        this.coords = coords;
        top = Collections.max(coords, (o1, o2) -> (o1.getLatDeg().getDegree() < o2.getLatDeg().getDegree()) ? -1 : 1).getLatDeg().getDegree();
        bottom = Collections.min(coords, (o1, o2) -> (o1.getLatDeg().getDegree() < o2.getLatDeg().getDegree()) ? -1 : 1).getLatDeg().getDegree();
        left = Collections.min(coords, (o1, o2) -> (o1.getLonDeg().getDegree() < o2.getLonDeg().getDegree()) ? -1 : 1).getLonDeg().getDegree();
        right = Collections.max(coords, (o1, o2) -> (o1.getLonDeg().getDegree() < o2.getLonDeg().getDegree()) ? -1 : 1).getLonDeg().getDegree();

        double degwidth = right - left;
        double degheight = top - bottom;
        // der groessere Wert bestimmt den factor, in die andere Richtung wird Verschnitt entstehen.
        degsize = Math.max(degheight, degwidth);

        origin = LatLon.fromDegrees(
                (top + bottom) / 2,
                (left + right) / 2);
        projection = new MetricSceneryProjection(this);
        /*projection.setOrigin();
         */
        bottomleft = projection.project(LatLon.fromDegrees(bottom, left));
        topright = projection.project(LatLon.fromDegrees(top, right));
        maxextension = topright.x - bottomleft.x;
        if (topright.z - bottomleft.z > maxextension) {
            maxextension = topright.z - bottomleft.z;
        }
        // log.debug("maxextension=" + maxextension);
        buildBoundaryPolygon(coords);
    }

    /**
     * 26.3.24: New gridless DB constructor
     * 13.2.26: Boundary is equals to outer...
     */
    public GridCellBounds(double top, double bottom, double left, double right) throws MeshInconsistencyException {
        instance = this;
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;

        degwidth = right - left;
        degheight = top - bottom;
        // der groessere Wert bestimmt den factor, in die andere Richtung wird Verschnitt entstehen.
        degsize = Math.max(degheight, degwidth);

        origin = LatLon.fromDegrees(
                (top + bottom) / 2,
                (left + right) / 2);
        projection = new MetricSceneryProjection(this);

        bottomleft = projection.project(LatLon.fromDegrees(bottom, left));
        topright = projection.project(LatLon.fromDegrees(top, right));
        maxextension = topright.x - bottomleft.x;
        if (topright.z - bottomleft.z > maxextension) {
            maxextension = topright.z - bottomleft.z;
        }
        buildBoundaryPolygon(List.of(
                LatLon.fromDegrees(top, left),
                LatLon.fromDegrees(top, right),
                LatLon.fromDegrees(bottom, right),
                LatLon.fromDegrees(bottom, left)
        ));
    }

    private void buildBoundaryPolygon(List<? extends LatLon> boundary) throws MeshInconsistencyException {
        // copied from init
        de.yard.threed.osm2graph.osm.CoordinateList vl = new de.yard.threed.osm2graph.osm.CoordinateList();
        boundary.forEach(b-> vl.add(JTSConversionUtil.vectorXZToJTSCoordinate(OsmUtil.project(projection,b))));
        //close polygon
        vl.add(vl.get(0));
        projectedBoundaryPolygon = JtsUtil.createPolygonFromCoordinateList(vl, false);
        if (!projectedBoundaryPolygon.isValid()) {
            throw new RuntimeException("invalid polygon");
        }
    }

    //@Override
    public void init(SceneryProjection projection) throws MeshInconsistencyException {

        de.yard.threed.osm2graph.osm.CoordinateList vl = new CoordinateList();

        int index = 0;
        for (LatLon coor : coords) {
            OSMNode gridosmnode = OsmUtil.buildDummyNode(coor.getLatDeg().getDegree(), coor.getLonDeg().getDegree());
            VectorXZ xz = OsmUtil.project(projection, coor);
            MapNode gridnode = new MapNode(xz, gridosmnode/*17.3.26, MapNode.Location.GRIDNODE*/);

            //vl.add(JTSConversionUtil.vectorXZToJTSCoordinate(xz));
            BoundaryNode boundaryNode = new BoundaryNode(gridnode, JTSConversionUtil.vectorXZToJTSCoordinate(xz));
            basicnodes.add(boundaryNode);
            vl.add(boundaryNode.coordinate);
        }
        //close polygon
        vl.add(vl.get(0));
        projectedBoundaryPolygon = JtsUtil.createPolygonFromCoordinateList(vl, false);
        if (!projectedBoundaryPolygon.isValid()) {
            throw new RuntimeException("invalid polygon");
        }
        for (int i = 0; i < basicnodes.size() - 1; i++) {
            boundaryLines.add(new BoundaryLine(basicnodes.get(i), basicnodes.get(i + 1)));
        }
    }

    public boolean isInside(MapNode mapNode) {
        return isInside(mapNode.getPos());
    }

    public boolean isInside(VectorXZ vectorXZ) {
        if (projectedBoundaryPolygon.contains(JtsUtil.GF.createPoint(JTSConversionUtil.vectorXZToJTSCoordinate(vectorXZ)))) {
            return true;
        }
        return false;
    }

    public boolean isBoundaryNode(VectorXZ mapNode) {
        for (BoundaryNode n : basicnodes) {
            if (mapNode/*.getPos().*/.distanceTo(n.mapNode.getPos()) < 0.000001) {
                return true;
            }
        }
        return false;
    }

    public boolean isInside(Coordinate c) {
        if (projectedBoundaryPolygon.contains(JtsUtil.GF.createPoint(c))) {
            return true;
        }
        /*for (MapNode n : boundNodes) {
            if (n.getOsmNode().id == mapNode.getOsmNode().id) {
                return true;
            }
        }*/

        return false;
    }

    //@Override
    public Polygon getProjectedBoundaryPolygon() {
        return projectedBoundaryPolygon;
    }

    public SceneryProjection getProjection() {
        return projection;
    }

    public double getTop() {
        return top;
    }

    public double getBottom() {
        return bottom;
    }

    public double getLeft() {
        return left;
    }

    public double getRight() {
        return right;
    }

    public LatLon getOrigin() {
        return origin;
    }

    public LatLon getBottomLeft() {
        return LatLon.fromDegrees(getBottom(), getLeft());
    }

    public LatLon getTopLeft() {
        return LatLon.fromDegrees(getTop(), getLeft());
    }

    public LatLon getTopRight() {
        return LatLon.fromDegrees(getTop(), getRight());
    }

    public double getScale(int imagesize) {
        // etwas konservativ, damits wirklich reinpasst
        return (imagesize - 10) / maxextension;
    }

    /**
     * Die nodes aus dem Grid muessen nicht in den MapData vorkommen.
     * Das wäre ungünstig, wenn ich freie Koordinaten als Connector nehme.
     *
     * @param gridfile
     * @return
     * @throws IOException
     */
    public static GridCellBounds buildGridFromFile(File gridfile) throws IOException, MeshInconsistencyException {
        List<Long> tripnodes = new ArrayList<>();
        List<GeoCoordinate/*LatLon*/> ids = new ArrayList();
        Scanner scanner = new Scanner(gridfile);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!StringUtils.isEmpty(line) && !line.startsWith("#")) {
                if (line.contains("=")) {
                    String[] parts = line.split("=");
                    tripnodes.add(Long.parseLong(parts[1]));
                } else {
                    String[] parts = line.split(",");
                    if (parts.length != 2) {
                        throw new RuntimeException("invalid grid line: " + line);
                    }
                    ids.add(GeoCoordinate.fromLatLon(LatLon.fromDegrees(Double.parseDouble(parts[0]),
                            Double.parseDouble(parts[1]))));
                }
            }
        }
        scanner.close();
        GridCellBounds targetBounds = new GridCellBounds(ids);
        targetBounds.tripnodes = tripnodes;
        return targetBounds;
    }

    /**
     * 26.3.24: Deprecated because of new gridless DB approach.
     */
    @Deprecated
    public static GridCellBounds buildGrid(String s, MapData mapData) {
        String gridfilename = SceneryBuilder.osmdatadir + "/" + s + ".grid.txt";

        if (s.equals("Wayland")) {
            gridfilename = "src/main/resources/Wayland.grid.txt";
        }
        try {
            return buildGridFromFile(new File(gridfilename));//buildDesdorf();
        } catch (IOException e) {
            //e.printStackTrace();
            log.warn("Could not read grid file " + gridfilename + ":" + e.getMessage() + ". Ignoring");
        } catch (MeshInconsistencyException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static GridCellBounds buildFromOsmData(OSMData osmData) throws MeshInconsistencyException {
        if (osmData.getBounds() != null && !osmData.getBounds().isEmpty()) {

            Bound firstBound = osmData.getBounds().iterator().next();

            return buildFromGeos(
                    firstBound.getTop(),
                    firstBound.getBottom(),
                    firstBound.getLeft(),
                    firstBound.getRight()
            );

        } else {

            if (osmData.getNodes().isEmpty()) {
                throw new IllegalArgumentException(
                        "OSM data must contain bounds or nodes");
            }

            OSMNode firstNode = osmData.getNodes().iterator().next();
            Util.notyet();
            return null;
        }

    }

    public static GridCellBounds buildFromGeos(double top, double bottom, double left, double right) throws MeshInconsistencyException {

        // Surrounding is needed for ...?? 16.4.24: No longer use it. Always causes confusion
        double offset = 0.0;//0.01;
        top = top + offset;
        bottom = bottom - offset;
        left = left - offset;
        right = right + offset;
        return new GridCellBounds(top, bottom, left, right);
    }

    /**
     * Returns false, when extensiuon was completely outside.
     *
     * @param extension
     */
    public boolean extend(Polygon extension) {
        //int intersection=JtsUtil.intersection(polygon,extension);
        if (!projectedBoundaryPolygon.intersects(extension)) {
            return false;
        }
        Geometry newpoly = projectedBoundaryPolygon.union(extension);
        if (newpoly instanceof MultiPolygon) {
            int h = 9;
        }
        projectedBoundaryPolygon = (Polygon) newpoly;
        return true;
    }

    /**
     * Betrachtet nur Ways. Alles andere ist Fehler, obwohl es nicht erkannt wird.
     * 27.3.24: Cannot get TerrainMesh, whose constructor calls this!
     *
     * @param objects
     */
    public void rearrangeForWayCut(List<SceneryObject> objects, TerrainMesh tm) throws MeshInconsistencyException {
        if (locked) {
            throw new RuntimeException("locked");
        }
        // bis jetzt besteht der Polygon nur aus den basic nodes.
        if (projectedBoundaryPolygon.getCoordinates().length - 1 != basicnodes.size()) {
            throw new RuntimeException("inconsistent grid");
        }
        //Die cut nodes gruppieren nach der BoundaryLine, auf der sie liegen
        //kann aber auch eine basicnode sein.
        mapOfCuts = getCuts(objects, tm);

        //really tricky. Das ganze mit der Polygonerzeugung ist ein Vabanquespiel und kann immer
        //mal wieder scheitern (invalid polygons).
        //LineString newboundary = null;
        int[] nodewidth = new int[basicnodes.size()];
        int index = 0;
        LineSegment[] nodeline = new LineSegment[basicnodes.size()];
        for (int i = 0; i < basicnodes.size(); i++) {
            if (mapOfCuts.containsKey(-i - 1)) {
                List<LazyCutObject> pairsatnode = mapOfCuts.get(-i - 1);
                if (pairsatnode.size() != 1) {
                    throw new RuntimeException("inconsisten");
                }

                projectedBoundaryPolygon = JtsUtil.replace(projectedBoundaryPolygon, basicnodes.get(i).coordinate, pairsatnode.get(0).coordinatePair.getFirst(), pairsatnode.get(0).coordinatePair.getSecond());
                Coordinate[] coors = projectedBoundaryPolygon.getCoordinates();
                nodeline[i] = new LineSegment(coors[index], coors[index + 1]);
                nodewidth[i] = 2;
            } else {
                LazyCutObject lco;
                // Ein Pair sehr nahe an einer basicnode soll auch die Node ersetzen (Sonderfall evtl. nur in Desdorf)
                if ((lco = getNearCut(basicnodes.get(i), mapOfCuts)) != null) {
                    CoordinatePair pair = lco.coordinatePair;

                    projectedBoundaryPolygon = JtsUtil.replace(projectedBoundaryPolygon, basicnodes.get(i).coordinate, pair.getFirst(), pair.getSecond());
                    Coordinate[] coors = projectedBoundaryPolygon.getCoordinates();
                    nodeline[i] = new LineSegment(coors[index], coors[index + 1]);
                    nodewidth[i] = 2;
                    lco.replacedBasicNode = true;
                } else {
                    nodewidth[i] = 1;
                    nodeline[i] = null;
                }
            }

            index += nodewidth[i];
        }
        polygon345 = projectedBoundaryPolygon;

        int beforeindex = 0;
        for (int i = 0; i < basicnodes.size(); i++) {
            beforeindex += nodewidth[i];
            List<LazyCutObject> pairsonline = mapOfCuts.get(i);
            if (pairsonline != null) {
                for (int j = 0; j < pairsonline.size(); j++) {
                    LazyCutObject lco = pairsonline.get(j);
                    if (!lco.replacedBasicNode) {
                        Coordinate beforevertex;
                        int nextindex;
                        if (i < basicnodes.size() - 1) {
                            nextindex = i + 1;
                        } else {
                            nextindex = 0;
                        }

                        if (nodeline[nextindex] == null) {
                            beforevertex = basicnodes.get(nextindex).coordinate;
                        } else {
                            beforevertex = nodeline[nextindex].p0;
                        }

                        Polygon newpolygon = JtsUtil.insert(projectedBoundaryPolygon, beforevertex, lco.coordinatePair.getFirst(), lco.coordinatePair.getSecond());
                        if (newpolygon == null) {
                            //was nun??
                            log.error("unexpected failure");
                            return;
                        }
                        projectedBoundaryPolygon = newpolygon;
                    }
                }
            }
        }
        polygon345a = projectedBoundaryPolygon;
        //damit niemenad mehr dran rumfummelt und die Coordinates registriert werden können.
        locked = true;
    }

    private LazyCutObject getNearCut(BoundaryNode boundaryNode, Map<Integer, List<LazyCutObject>> cuts) {
        for (int i = 0; i < basicnodes.size(); i++) {
            List<LazyCutObject> pairsonline = cuts.get(i);
            if (pairsonline != null) {
                for (int j = 0; j < pairsonline.size(); j++) {
                    LazyCutObject lco = pairsonline.get(j);
                    CoordinatePair p = lco.coordinatePair;
                    if (boundaryNode.coordinate.distance(p.getFirst()) < 15 ||
                            boundaryNode.coordinate.distance(p.getSecond()) < 15) {
                        //5.8.19:der wird noch gebraucht pairsonline.remove(j);
                        return lco;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Die Grid (Lazy)Cuts von ways ermitteln und den Boundary edges zuordnen.
     */
    public Map<Integer, List<LazyCutObject>> getCuts(List<SceneryObject> ways, TerrainMesh tm) {
        Map<Integer, List<LazyCutObject>> cuts = new HashMap();

        SceneryObjectList.iterateWays(ways, (way) -> {
            if (way.isEmpty(tm)) {
                //possible invalid polygon ways
                return;
            }
            WayArea wayArea = way.getWayArea();
            if (wayArea == null) {
                log.error("unexpected?");
                return;
            }
            if (way.getOsmIdsAsString().contains("37935545")) {
                int h = 9;
            }
            /*17.3.26 we no longer care about if (way.startMode == GRIDBOUNDARY) {
                MapNode mapNode = way.getStartNode();
                groupMapNode(way.getStartNode(), cuts, wayArea.getStartPair(tm)[0], way);
            }*/
            /*17.3.26 we no longer care about if (way.endMode == GRIDBOUNDARY) {
                MapNode mapNode = way.getEndNode();
                groupMapNode(way.getEndNode(), cuts, wayArea.getEndPair()[0], way);
            }*/

        });
        return cuts;
    }

    private int groupMapNode(MapNode mapNode, Map<Integer, List<LazyCutObject>> cuts, CoordinatePair pair, SceneryFlatObject sfo) {
        int key;
        if ((key = findBasicnode(mapNode)) != -1) {
            //kann auch sein. -1, weil die 0 mehrdeutig ist.
            key = -key - 1;
            if (cuts.containsKey(key)) {
                throw new RuntimeException("duplicate grid cut " + mapNode.getOsmId());
            }
            cuts.put(key, new ArrayList<>());
            cuts.get(key).add(new LazyCutObject(pair, sfo));
            return -7777;
        }
        if (!additionalGridnodes.contains(mapNode)) {
            throw new RuntimeException("unknown gridnode " + mapNode.getOsmId());
        }
        Coordinate c = JtsUtil.toCoordinate(mapNode.getPos());
        int lineindex = JtsUtil.getCoveringLine(c, projectedBoundaryPolygon);
        final Coordinate endOfLine = projectedBoundaryPolygon.getCoordinates()[lineindex + 1];
        if (!cuts.containsKey(lineindex)) {
            cuts.put(lineindex, new ArrayList<>());
        }
        cuts.get(lineindex).add(new LazyCutObject(pair, sfo));
        Collections.sort(cuts.get(lineindex), new Comparator<LazyCutObject>() {
            @Override
            public int compare(LazyCutObject o1, LazyCutObject o2) {
                return endOfLine.distance(o1.coordinatePair.left()) > endOfLine.distance(o2.coordinatePair.left()) ? -1 : 1;
            }
        });
        return 0;
    }

    public SceneryFlatObject getLazyCutObjectOfCoordinate(Coordinate c0, Coordinate c1) {
        for (LazyCutObject lco : getLazyCuts()) {
            if (lco.coordinatePair.left() == c0 && lco.coordinatePair.right() == c1) {
                return lco.sfo;
            }
            if (lco.coordinatePair.left() == c1 && lco.coordinatePair.right() == c0) {
                return lco.sfo;
            }
        }

        //its OK not to find a pair when its not a lazy cut.
        //log.error("coordinate not found on grid:"+c0+c1);
        return null;
    }

    public List<LazyCutObject> getLazyCuts() {
        List<LazyCutObject> result = new ArrayList<>();
        for (List<LazyCutObject> lcos : mapOfCuts.values()) {
            result.addAll(lcos);
        }
        return result;
    }

    /**
     * normale mapnodes koennen auch basic nodes, ohne dass man sie dirket erkennt. Darum ueber position suchen.
     *
     * @param mapNode
     * @return
     */
    private int findBasicnode(MapNode mapNode) {
        for (int i = 0; i < basicnodes.size(); i++) {
            if (basicnodes.get(i).mapNode.getPos().distanceTo(mapNode.getPos()) < 0.0000001) {
                return i;
            }
        }
        return -1;
    }

    private void insertWayCut() {
        Coordinate[] coors = projectedBoundaryPolygon.getCoordinates();
    }

    public void lock() {
        locked = true;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean onBoundary(Coordinate coor) {
        return JtsUtil.onBoundary(coor, projectedBoundaryPolygon);
    }

    public boolean isPreDbStyle() {
        //13.2.26 no longer a criteria return coords != null;
        return false;
    }

    /**
     * 29.7.19:gleich deprecated wegen Stilbruch. Für mapnode->coordinates gibts doch einen map/registry?
     */
    @Deprecated
    public static class BoundaryNode {
        public MapNode mapNode;
        public Coordinate coordinate;

        BoundaryNode(MapNode node, Coordinate coordinate) {
            this.mapNode = node;
            this.coordinate = coordinate;
        }
    }

    public static class LazyCutObject {
        public CoordinatePair coordinatePair;
        public SceneryFlatObject sfo;
        public boolean replacedBasicNode;

        LazyCutObject(CoordinatePair coordinatePair, SceneryFlatObject sfo) {
            this.coordinatePair = coordinatePair;
            this.sfo = sfo;
        }
    }

    public List<GeoCoordinate> getBoundary() {
        return coords;
    }
}

class BoundaryLine {
    BoundaryLine(GridCellBounds.BoundaryNode start, GridCellBounds.BoundaryNode end) {

    }
}


/**
 * Doch eine im Grunde metrische Projection.
 * Linear auf Textur verzerrt viel zu stark Richtung Pole.
 * S.o.
 */
class MetricSceneryProjection implements SceneryProjection {
    GridCellBounds gridCellBounds;
    MetricMapProjection metricMapProjection;

    public MetricSceneryProjection(GridCellBounds gridCellBounds) {
        this.gridCellBounds = gridCellBounds;
        metricMapProjection = new MetricMapProjection(gridCellBounds.getOrigin());
        //metricMapProjection.setOrigin(gridCellBounds.getOrigin());
    }

    @Override
    public VectorXZ project(LatLon latlon) {
        /*double x = (latlon.getLongitudeDeg().getDegree() - gridCellBounds.origin.lon - gridCellBounds.degsize / 2);
        x = x / gridCellBounds.degsize;
        x = x * (double) 512 + 256;
        double y = (latlon.getLatitudeDeg().getDegree() - gridCellBounds.origin.lat - gridCellBounds.degsize / 2);
        y = y / gridCellBounds.degsize;
        y = y * (double) 512 + 256;
        return new Vector2(x, y);*/
        VectorXZ pos = metricMapProjection.calcPos(latlon);
        return pos;
    }

    @Override
    public LatLon unproject(VectorXZ loc) {
        /*double lon =  gridCellBounds.origin.lon ;
        lon += ((double)loc.x / 256) *  (gridCellBounds.degsize / 2);
        double lat =  gridCellBounds.origin.lat ;
        lat += ((double)loc.y / 256) *  (gridCellBounds.degsize / 2);
        return SGGeod.fromDeg(lon,lat);*/
        return LatLon.fromDegrees(metricMapProjection.calcLat(loc), metricMapProjection.calcLon(loc));
    }

    @Override
    public MetricMapProjection getBaseProjection() {
        return metricMapProjection;
    }

    //@Override
    public LatLon/*SGGeod*/ getOrigin() {
        //22.12.21 return SGGeod.fromDeg(gridCellBounds.origin.getLonDeg().getDegree(), gridCellBounds.origin.getLatDeg().getDegree());
        return new LatLon(gridCellBounds.origin.getLatDeg().getDegree(), gridCellBounds.origin.getLonDeg().getDegree());
    }

    @Override
    public Coordinate project(GeoCoordinate geoCoordinate) {
        return metricMapProjection.project(geoCoordinate);
    }
}
