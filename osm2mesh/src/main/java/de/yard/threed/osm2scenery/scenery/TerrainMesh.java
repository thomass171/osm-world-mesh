package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Degree;
import de.yard.threed.core.Pair;
import de.yard.threed.core.Util;
import de.yard.threed.core.Vector2;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.CoordinateList;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.WayMap;
import de.yard.threed.osm2scenery.elevation.ElevationCalculator;
import de.yard.threed.osm2scenery.modules.AerowayModule;
import de.yard.threed.osm2scenery.polygon20.MeshArea;
import de.yard.threed.osm2scenery.polygon20.MeshFactory;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshLineSplitCandidate;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.polygon20.OsmWay;
import de.yard.threed.osm2scenery.polygon20.Sector;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.WayArea;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.traffic.geodesy.ElevationProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A mesh of {@link MeshNode}s and {@link MeshLine}s. Usually its just a submesh of the full world mesh.
 * 28.3.24:
 * <p>
 * Wird in drei Stufen aufgebaut:
 * 1) Boundary
 * 2) Ways+Connector
 * 3) Areas (die koennen dann an die Ways anbinden)
 * 4) Supplements
 */
public class TerrainMesh {
    public static MeshFactory meshFactoryInstance = null;
    //static TerrainMesh instance;
    static Logger logger = Logger.getLogger(TerrainMesh.class);
    public int errorCounter = 0;
    GridCellBounds gridCellBounds;
    public List<MeshNode> points = new ArrayList();
    public List<MeshLine> lines = new ArrayList();
    public List<MeshArea> areas = new ArrayList();
    //public Map<Integer, List<Integer>> linesOfPoint = new HashMap();
    List<Integer> knowntwoedger = new ArrayList<>();
    //geht nicht, weil durch split noch welche dazukommen private int lastboundaryindex;
    int step = 0;
    //so einer kann nicht mehr valid sein
    // 2.5.24 aren't hasDuplicates and warnings deprecated here?
    private boolean hasDuplicates = false;
    public List<String> warnings = new ArrayList<>();

    /**
     * gridCellBounds are the outer boundaries of the (sub)mesh.
     */
    private TerrainMesh(GridCellBounds gridCellBounds) {
        this.gridCellBounds = gridCellBounds;
        Polygon boundary = gridCellBounds.getPolygon();
        // 27.3.24: In DB boundary does not belong to mesh
        if (gridCellBounds.isPreDbStyle()) {
            Coordinate[] coors = boundary.getCoordinates();
            for (int i = 0; i < coors.length; i++) {
                if (i < coors.length - 1) {
                    registerPoint(coors[i]);
                }
                if (i > 0) {
                    //sfo might be null if there is no lazy cut.
                    SceneryFlatObject sfo = gridCellBounds.getLazyCutObjectOfCoordinate(coors[i - 1], coors[i]);
                    MeshLine meshLine = registerLine(JtsUtil.toList(coors[i - 1], coors[i]), (sfo != null) ? sfo.getArea()[0] : null, null, true, true);
                    meshLine.setBoundary(true);

                    //lines.add(meshLine);
                }
                //bei Boundary kann es immr mal bei nur zwei bleiben
                knowntwoedger.add(i);
            }
        }
        //lastboundaryindex = lines.size() - 1;
        step = 1;
    }

    public static TerrainMesh init(GridCellBounds gridCellBounds) {
        TerrainMesh instance = new TerrainMesh(gridCellBounds);
        //may no longer change
        gridCellBounds.lock();
        //isValid() not usable because lazy cuts are only fragments.
       /* if (!instance.isValid(true)) {
            logger.error("not valid");
        }*/
        for (MeshNode p : instance.points) {
            if (p.getLineCount() != 2) {
                logger.error("not valid");
            }
        }
        return instance;
    }

    /*public static TerrainMesh getInstance() {
        return instance;
    }*/

    public static List<MeshLine> sort(List<MeshLine> lines) {
        List<MeshLine> newlines = new ArrayList();
        if (lines.size() == 0) {
            return newlines;
        }
        MeshLine line = lines.get(0);
        newlines.add(line);
        MeshNode point = line.getTo();
        for (int i = 1; i < lines.size(); i++) {
            line = getSuccessorInPolygon(point, lines, newlines);
            newlines.add(line);
            point = getOpposite(line, point);
        }
        return newlines;
    }

    private static MeshLine getSuccessorInPolygon(MeshNode point, List<MeshLine> lines, List<MeshLine> except) {
        for (int i = 0; i < lines.size(); i++) {
            MeshLine line = lines.get(i);
            if (point == line.getTo() || point == line.getFrom()) {
                if (!except.contains(line)) {
                    return line;
                }
            }
        }
        if (point.getClass().getName().contains("ersisted")) {
            throw new RuntimeException("inconsistent mesh polygon");
        }
        return null;
    }

    /**
     * Seit split einer area zwi AbstractArea erzeugt, dürfte das hier eindeutig sein.
     *
     * @param area
     * @return
     */
    public MeshPolygon getPolygon(MeshArea /*AbstractArea/*SceneryFlatObject*/ area) throws MeshInconsistencyException {
        for (MeshLine startline : lines) {
            if (area.equals(startline.getLeft()) || area.equals(startline.getRight())) {
                return getPolygon(startline, area);
            }
        }
        logger.error("no polygon startline found for area " + area);
        return null;
    }

    /**
     * Ein Helder für Analyse von Zwischenschritten, wenn getPolygon() noch nicht geht.
     */
    public List<MeshLine> getLinesOfArea(AbstractArea area) {
        List<MeshLine> result = new ArrayList<>();
        for (MeshLine line : lines) {
            if (line.getLeft() == area || line.getRight() == area) {
                result.add(line);
            }
        }
        return result;
    }

    /**
     * Geht das mit Boundary? Wohl nicht.
     *
     * @param startline
     * @param area
     * @return
     */
    public MeshPolygon getPolygon(MeshLine startline, MeshArea/*AbstractArea/*SceneryFlatObject*/ area) throws MeshInconsistencyException {
        if (area == null) {
            logger.error("invalid use");
            return null;
        }
        boolean left;
        if (startline.getLeft() == area) {
            left = true;
        } else {
            if (startline.getRight() == area) {
                left = false;
            } else {
                logger.error("invalid area");
                return null;
            }
        }
        // 2.5.24 by direction instead of area
        MeshPolygon polygon = traversePolygon(startline, /*area*/null, left);
        return polygon;
    }

    /**
     * Build a polygon by traversing lines of the mesh. If we know the area, its easier to traverse by that area.
     * If we don't know it we traverse by line angles (left/right).
     * 19.4.24: Returns null if no such polygon exists. MeshInconsistencyException is only thrown when there really was an inconsistency(?? is that possible?).
     * a pure outer polygon might be the 'wrong' result of going (C)CW. But difficult to detect.
     */
    public MeshPolygon traversePolygon(MeshLine startline, MeshArea/*AbstractArea/*SceneryFlatObject*/ area, boolean leftOrCCW) throws MeshInconsistencyException {
        int abortcounter = 0;
        MeshLine line = startline;
        MeshNode next = line.getTo();
        List<MeshLine> result = new ArrayList<>();

        do {
            result.add(line);
            if (line.getTo() == line.getFrom()) {
                // special case closed line
                break;
            }
            MeshLine nextline = getSuccessor(next, area, leftOrCCW, line);
            if (nextline == null) {
                logger.error("traversePolygon: no successor. inconsistency?");
                return null;
            }
            next = getOpposite(nextline, next);
            line = nextline;
            if (next.getCoordinate().distance(new Coordinate(-199, -149)) < 3) {
                int h = 9;
            }
            /*if (startline.from==next ){
                logger.debug("xyz");
                result.add(line);
                break;
            }*/
        } while (line != startline && abortcounter++ < 100);
        if (abortcounter >= 100) {
            logger.error("abort");
            return null;
        }
        // logger.debug("found lines:" + result.size());
        return new MeshPolygon(result);
    }

    /**
     * Find the line of area that connects at "meshNode" to "origin".
     * Mit origin laesst sich ein moeglicher Origin ausschliessen um nicht im Kreis zu laufen.
     * 9.9.19: Das ist doch bei Ways so nicht eindeutig wegen innerer Querverbindungen? Oder gibt es sowas nicht?
     * 18.4.24: If we know the area, its easier to find successor by that area. Otherwise we traverse by line angles (left/right).
     */
    public MeshLine getSuccessor(MeshNode meshNode, MeshArea/*AbstractArea*/ area, boolean left, MeshLine origin) throws MeshInconsistencyException {
        List<MeshLine> candidates = new ArrayList();

        if (area == null) {
            // find by angles
            MeshNodeDetails details = new MeshNodeDetails(meshNode);
            candidates.add(details.getNeighborLine(origin, left));
        } else {
            for (MeshLine line : meshNode.getLines()) {
                if (line != origin) {
                    boolean skipLine = false;
                    MeshArea areaToCheck = null;
                    if (line.getFrom() == meshNode) {
                        if (left) {
                            areaToCheck = line.getLeft();
                        } else {
                            areaToCheck = line.getRight();
                            //bei der Suche nach Leerflächen keine BoundaryLine beachten mit left != null, denn right ist immer 0
                            if (line.isBoundary() && area == null) {
                                skipLine = true;
                            }
                        }
                    } else {
                        if (left) {
                            areaToCheck = line.getRight();
                            //bei der Suche nach Leerflächen keine BoundaryLine beachten mit left != null, denn right ist immer 0
                            if (line.isBoundary() && area == null) {
                                skipLine = true;
                            }
                        } else {
                            areaToCheck = line.getLeft();
                        }
                    }
                    if (!skipLine) {
                        if (left && areaToCheck == area) {
                            candidates.add(line);
                        }
                        if (!left && areaToCheck == area) {
                            candidates.add(line);
                        }
                    }
                }
            }
        }
        LineString originline = (origin == null) ? null : origin.getLine();
        if (candidates.size() == 0) {
            logger.error("no successor at point " + meshNode + " for origin " + originline);
            errorCounter++;
            return null;
        }
        if (candidates.size() > 1) {
            //das deutet doch auf ein grundsätzliches Konsistenzproblem. Ausser bei BG (area==null). Dann kann so etwas wirklich mal vorkommen.
            //Tja, das bekommt man aber wohl nicht gelöst. Für den Sonderfall start auf boundary waehle ich eine nicht boundary. Das hilft beim Desdorf Farmland,
            //aber obs allgemeingültig ist??
            if (origin != null && origin.isBoundary()) {
                if (candidates.get(0).isBoundary() && !candidates.get(1).isBoundary()) {
                    return candidates.get(1);
                }
                if (!candidates.get(0).isBoundary() && candidates.get(1).isBoundary()) {
                    return candidates.get(0);
                }
            }
            logger.warn("multiple successor at point " + meshNode + " for origin " + originline);
            SceneryContext.getInstance().warnings.add("multiple successor at point " + meshNode + " for origin " + originline);
            //5.9.19 lieber null um Fehlerkaschierung zu vermeiden
            return null;
        }
        return candidates.get(0);
    }

    private static MeshNode getOpposite(MeshLine line, MeshNode coordinate) {
        if (line.getFrom() == coordinate) {
            return line.getTo();//coordinates[line.coordinates.length - 1];
        }
        return line.getFrom();//coordinates[0];
    }

    /**
     * Von den Way Starts ausgehend die Ways entlanglaufen. Werden unterwegs Line20 gefunden, ok.
     * Ansonsten eine Anlegen mit BG auf einer Seite.
     */
    private void t() {

    }

    /**
     * Ways and WayConnector.
     * 5.8.19: Not for areas.
     */
    public void addWays(List<SceneryObject> sceneryObjects) throws OsmProcessException {
        if (isPreDbStyle()) {
            if (step != 1) {
                throw new RuntimeException("invalid step");
            }
        } else {
            if (sceneryObjects.size() != 1) {
                // not sure this is an exception. A single OSM way might result in several mesh ways??
                //throw new RuntimeException("should only add one way in one call/cycle");
            }
        }
        for (SceneryObject obj : sceneryObjects) {
           /*doofe pruefung  if (!obj.isCut || !obj.isClipped) {
                throw new RuntimeException("neither cut or clipped:");
            }*/

            if (obj.getOsmIdsAsString().contains("23696494")) {
                int h = 9;
            }
            //skip eg. bridges
            if (obj.isTerrainProvider) {
                if (obj instanceof SceneryWayObject) {
                    SceneryWayObject way = (SceneryWayObject) obj;
                    way.addToTerrainMesh(this);

                }
                if (obj instanceof SceneryWayConnector) {
                    SceneryWayConnector way = (SceneryWayConnector) obj;
                    way.addToTerrainMesh(this);

                }
            }
        }
        step = 2;
        try {
            if (!isValid(true)) {
                logger.error("invalid after adding ways and way connector");
            }
        } catch (MeshInconsistencyException e) {
            logger.error("invalid after adding ways and way connector");
        }
    }

    public void addAreas(List<SceneryObject> sceneryObjects) throws OsmProcessException {
        if (step != 2) {
            throw new RuntimeException("invalid step");
        }
        for (SceneryObject obj : sceneryObjects) {
           /*doofe pruefung  if (!obj.isCut || !obj.isClipped) {
                throw new RuntimeException("neither cut or clipped:");
            }*/
            if (obj.isTerrainProvider) {

                if (obj instanceof SceneryAreaObject) {
                    SceneryAreaObject way = (SceneryAreaObject) obj;
                    way.addToTerrainMesh(this);

                }
                if (obj instanceof AerowayModule.Runway) {
                    //der instanceof Runway ist ja nun auch wieder doof
                    ((AerowayModule.Runway) obj).addToTerrainMesh(this);

                }
            }
        }
        step = 3;
        try {
            if (!isValid(true)) {
                logger.error("invalid after adding areas");
            }
        } catch (MeshInconsistencyException e) {
            logger.error("invalid after adding areas");
        }
    }

    /**
     * GapFiller sind hier noch nicht dabei. Die registrieren sich spaeter selber.
     * Die Liste enthält nur Supplements.
     */
    public void addSupplements(List<SceneryObject> supplements) throws OsmProcessException {
        if (step != 3) {
            throw new RuntimeException("invalid step");
        }
        for (SceneryObject obj : supplements) {
            if (obj.isTerrainProvider) {

                //if (obj instanceof ScenerySupplementAreaObject) {
                ScenerySupplementAreaObject way = (ScenerySupplementAreaObject) obj;
                way.addToTerrainMesh(this);

                //}

            }
        }
        step = 4;
    }

    public static void extractBackground() {

    }

    /**
     * 5.4.24: Deprecated in favor of registerArea
     */
    @Deprecated
    public MeshLine registerLine(LineString line, AbstractArea/*SceneryFlatObject*/ left, AbstractArea/*SceneryFlatObject*/ right) {
        return registerLine(JtsUtil.toList(line.getCoordinates()), left, right, false, false);
    }

    /**
     * 5.4.24: Deprecated in favor of registerArea
     * 8.4.24: registerArea/Way uses below method
     */
    @Deprecated
    public MeshLine registerLine(List<Coordinate> line, AbstractArea/*SceneryFlatObject*/ left, AbstractArea/*SceneryFlatObject*/ right,
                                 boolean startOnGrid, boolean endOnGrid) {
        //logger.debug("new line with " + line.size() + " coordinates");
        MeshLine meshLine = buildMeshLinesFromList(line).get(0);
        int startPoint, endPoint;
        if (startOnGrid) {
            MeshNode p = getMeshNode(line.get(0));
        }
        if (endOnGrid) {
            MeshNode p = getMeshNode(line.get(line.size() - 1));
        }
        /*2.5.24 no longer fits meshLine.setLeft(left);
        meshLine.setRight(right);*/
        //return /*lines.get(*/registerLine(meshLine/*, startPoint, endPoint*/);
        registerLine(meshLine);
        return meshLine;
    }

    /**
     * 5.4.24: Deprecated in favor of registerArea
     */
    @Deprecated
    public MeshLine registerLine(MeshNode p0, MeshNode p1, AbstractArea/*SceneryFlatObject*/ left, AbstractArea/*SceneryFlatObject*/ right) {
        MeshLine meshLine = MeshFactory.buildMeshLines(new Coordinate[]{p0.getCoordinate(), p1.getCoordinate()}).get(0);
        if (meshLine == null) {
            return null;
        }
        meshLine.setFrom(p0);
        TerrainMesh.validateMeshLine(meshLine, warnings);
        meshLine.setTo(p1);
        TerrainMesh.validateMeshLine(meshLine, warnings);
        /*2.5.24 no longer fits meshLine.setLeft(left);
        meshLine.setRight(right);*/
        registerLine(meshLine);
        return meshLine;
    }

    /**
     * 5.4.24: Three additional register instead of registerLine, which isn't ready for polygons.
     * lanes is used leter to detect ways for triangulateAndTexturize
     * connector might be null, otherwise the connecting part must have been created before.
     * 6.4.24: Well, maybe its easier to keep existing registerLine for a while?
     *
     * @return
     */
    public MeshPolygon registerWay(OsmWay osmWay, Pair<Coordinate, Coordinate> fromConnector, List<Coordinate> leftLine, List<Coordinate> rightLine, Pair<Coordinate, Coordinate> toConnector, int lanes) throws OsmProcessException {

        Polygon polygon = JtsUtil.createPolygonFromWayOutlines(new CoordinateList(rightLine), new CoordinateList(leftLine));

        List<MeshLine> linesToDelete = new ArrayList<>();
        for (MeshLine line : lines) {
            if (crosses(line, polygon)) {
                // intersection found. If it is not a BG line, this is a failure. Either the way overlaps some existing area or the (sub)mesh is too small.
                if (!MeshLine.isBackgroundTriangulation(line.getType())) {
                    throw new OsmProcessException("polygon crosses unremovable line " + line);
                }
                linesToDelete.add(line);
            }
        }
        linesToDelete.forEach(l -> deleteLineFromMesh(l));

        AbstractArea/*SceneryFlatObject*/ leftArea = null;
        AbstractArea/*SceneryFlatObject*/ rightArea = null;

        MeshArea meshArea = addArea();
        meshArea.setOsmWay(osmWay);

        List<MeshLine> newLines = new ArrayList<>();
        MeshNode n = meshFactoryInstance.buildMeshNode(leftLine.get(0));
        points.add(n);
        MeshLine l;
        for (int i = 1; i < leftLine.size(); i++) {
            l = addLine(n, leftLine.get(i));
            n = l.getTo();
            newLines.add(l);
            l.setRight(meshArea);
        }
        l = addLine(n, rightLine.get(rightLine.size() - 1));
        n = l.getTo();
        newLines.add(l);

        for (int i = rightLine.size() - 2; i >= 0; i--) {
            l = addLine(n, rightLine.get(i));
            n = l.getTo();
            newLines.add(l);
            // polygon continues, so 'right' is correct.
            l.setRight(meshArea);
        }

        // lines.addAll(registerLineNonPreDB(JtsUtil.toList(leftLine.get(0), rightLine.get(0)), null, null));
        l = meshFactoryInstance.buildMeshLine(n, newLines.get(0).getFrom());
        newLines.add(l);
        lines.addAll(newLines);

        MeshPolygon newArea = null;
        try {
            newArea = new MeshPolygon(newLines);

            // the new area has no connection to the mesh yet.
            MeshLine someLine = findSomeEnclosingLine(newArea);
            if (someLine == null) {
                throw new OsmProcessException("no enclosing line");
            }
            MeshPolygon enclosingPolygon = traversePolygon(someLine, null, true);
            if (enclosingPolygon == null) {
                throw new OsmProcessException("no enclosingPolygon");
            }

            // connect ot background mesh
            MeshLine startConnectingLine = newLines.get(newLines.size() - 1);
            connectAreaNodeToPolygon(newLines.get(0), newLines.get(0).getFrom(), newArea, enclosingPolygon);
            MeshLine endConnectingLine = newLines.get(leftLine.size() - 1);
            connectAreaNodeToPolygon(endConnectingLine, endConnectingLine.getFrom(), newArea, enclosingPolygon);
            connectAreaNodeToPolygon(endConnectingLine, endConnectingLine.getTo(), newArea, enclosingPolygon);
            connectAreaNodeToPolygon(startConnectingLine, startConnectingLine.getFrom(), newArea, enclosingPolygon);
            // the caller should call validate before persist. Don't remember the reason.
            return newArea;
        } catch (MeshInconsistencyException e) {
            throw new OsmProcessException(e);
        }
    }

    private void connectAreaNodeToPolygon(MeshLine rline, MeshNode connectNode, MeshPolygon newArea, MeshPolygon enclosingPolygon) throws MeshInconsistencyException {
        MeshNodeDetails details = new MeshNodeDetails(connectNode);
        Sector sector = details.getNeighborSector(rline, rline.getFrom().equals(connectNode));
        // ometimes, 90 is too small.
        sector = sector.reduce(new Degree(150/*90*/));
        MeshLine connectingLine = connectNodeToPolygon(connectNode, sector, newArea, enclosingPolygon);
        if (connectingLine != null) {
            lines.add(connectingLine);
        }
    }

    private MeshLine addLine(MeshNode from, Coordinate to) {
        MeshNode existingNode = null;//TODO find
        if (existingNode == null) {
            existingNode = meshFactoryInstance.buildMeshNode(to);
            points.add(existingNode);
        }
        return meshFactoryInstance.buildMeshLine(from, existingNode);
    }

    private MeshArea addArea() {
        MeshArea meshArea = meshFactoryInstance.buildMeshArea();
        areas.add(meshArea);
        return meshArea;
    }

    /**
     * Find a line that is part of a/the polygon that encloses newArea.
     * Done just by probing.
     */
    private MeshLine findSomeEnclosingLine(MeshPolygon newArea) {
        // probe from just the first for now
        LineString probingLine = getProbingLineFromNode(newArea, 0, 100000);
        if (probingLine != null) {
            for (MeshLine line : lines) {
                if (JtsUtil.isIntersectingLine(probingLine, List.of(line.getLine()))) {
                    return line;
                }
            }
        }
        logger.warn("no enclosing line");
        return null;
    }

    /**
     * Build a probing line pointing from a area point to outside.
     */
    private LineString getProbingLineFromNode(MeshPolygon area, int lineIndex, double length) {
        MeshNode node = area.lines.get(lineIndex).getFrom();
        if (node.getLineCount() != 2) {
            Util.notyet();
        }
        List<Vector2> lineDirections = new MeshNodeDetails(node).directions;
        Degree angle = Degree.buildFromRadians(Vector2.getAngleBetween(lineDirections.get(0), lineDirections.get(1)));
        // Not sure this calc is correct. Needs unit testing. TODO
        Vector2 probingDir = lineDirections.get(0).rotate(angle).normalize();

        LineString line = JtsUtil.createLine(node.getCoordinate(), JtsUtil.add(node.getCoordinate(), probingDir.multiply(length)));
        if (!JtsUtil.isIntersecting(line, List.of(area.getPolygon()))) {
            return line;
        }
        // try opposite direction
        probingDir = probingDir.rotate(new Degree(180)).normalize();
        line = JtsUtil.createLineInDirection(node.getCoordinate(), probingDir, length);
        if (!JtsUtil.isIntersecting(line, List.of(area.getPolygon()))) {
            return line;
        }
        logger.warn("no probing line");
        return null;
    }

    public void registerConnector() {

    }

    public void registerArea() {

    }

    public void validate() throws MeshInconsistencyException {
        if (!isValid(true)) {
            throw new MeshInconsistencyException("not valid");
        }
    }

    public boolean isValid() {
        try {
            return isValid(false);
        } catch (MeshInconsistencyException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    /**
     * Ob points mir nur zwei lines wirklich invalid sind?? eigentlich doch nicht. Für manche Tests ist es aber brauchbar.
     * 24.4.24 Until full triangulation there might be nodes with only two lines.
     * 25.4.24: Might also throw MeshInconsistencyException
     *
     * @param ignoretwoliner
     * @return
     */
    public boolean isValid(boolean ignoretwoliner) throws MeshInconsistencyException {
        boolean valid = true;
        for (int i = 0; i < points.size(); i++) {
            MeshNode point = points.get(i);

            if (point.getLineCount() == 0) {
                logger.warn("no line at point " + points.get(i).getCoordinate() + "(" + i + "): ");
                valid = false;
            } else {
                if (point.getLineCount() < 2) {
                    if (!point.getLines().get(0).isClosed()) {
                        logger.warn("only one line at point " + points.get(i).getCoordinate() + "(" + i + "): ");
                        valid = false;
                    }
                } else {
                    if (point.getLineCount() == 2 && !ignoretwoliner && !knowntwoedger.contains(i)) {
                        logger.warn("too few lines at point " + points.get(i).getCoordinate() + "(" + i + "): " + point.getLineCount());
                        valid = false;
                    } else {
                        //bei Way junctions gibt es auch mal 4, darum > 4.
                        if (point.getLineCount() > 4) {
                            //21.8.19:aber da ist doch was faul
                            // 26.4.24: Should be nor problem anymore
                            //logger.warn("too many lines at point " + points.get(i).getCoordinate() + "(" + i + "): " + point.getLineCount());
                            //valid = false;
                        }
                    }
                }
            }
            // each point covering a line needs to be part of that line
            for (MeshLine meshLine : lines) {
                if (meshLine.getCoveringSegment(point.getCoordinate()) != -1) {
                    if (!meshLine.contains(point.getCoordinate())) {
                        logger.error("validation: point covers line but not part of it:" + point);
                        valid = false;
                    }
                }
            }

        }

        // fuer jede Area muss es einen konsistenten Polygon geben.
        Map<MeshArea, Void> areas = new HashMap<>();
        for (MeshLine line : lines) {
            if (line.getLeft() != null) {
                areas.put(line.getLeft(), null);
            }
            if (line.getRight() != null) {
                areas.put(line.getRight(), null);
            }
        }
        for (MeshArea abstractArea : areas.keySet()) {
            MeshPolygon meshPolygon = null;
            try {
                meshPolygon = getPolygon(abstractArea);
            } catch (MeshInconsistencyException e) {
                logger.error("polygon exception");
                return false;
            }
            if (meshPolygon == null) {
                logger.error("polygon not found");
                return false;
            }
        }

        if (hasDuplicates) {
            logger.error("not valid due to duplicates");
            return false;
        }
        if (errorCounter != 0) {
            logger.error("errorCounter=" + errorCounter);
            return false;
        }
        // No line might intersect any other line.
        if (!isPreDbStyle()) {
            for (MeshLine l0 : lines) {
                for (MeshLine l1 : lines) {
                    if (!l0.equals(l1)) {
                        if (JtsUtil.isReallyIntersectingLine(l0.getLineSegment(), l1.getLineSegment())) {
                            throw new MeshInconsistencyException("line " + l0 + " intersects " + l1);
                            //logger.error("line " + l0 + " intersects " + l1);
                        }
                    }
                }
            }
        }
        return valid;
    }

    /**
     * The LineSegment might not be a derival from an existing line!
     */
    private boolean isReallyIntersectingAnyLine(LineSegment lineSegment) {
        for (MeshLine l : lines) {
            if (JtsUtil.isReallyIntersectingLine(l.getLineSegment(), lineSegment)) {
                return true;
            }
        }
        return false;
    }

    public GridCellBounds getGridCellBounds() {
        return gridCellBounds;
    }

    private List<MeshLine> buildMeshLinesFromList(List<Coordinate> line) {
        List<MeshLine> meshLines = MeshFactory.buildMeshLines((Coordinate[]) line.toArray(new Coordinate[0]));
        if (isPreDbStyle()) {
            MeshLine meshLine = meshLines.get(0);
            if (meshLine == null) {
                return null;
            }
            MeshNode p;
            if ((p = getMeshNode(line.get(0))) == null) {
                p = registerPoint(line.get(0));
            }
            meshLine.setFrom(p);
            TerrainMesh.validateMeshLine(meshLine, warnings);
            if ((p = getMeshNode(line.get(line.size() - 1))) == null) {
                p = registerPoint(line.get(line.size() - 1));
            }
            meshLine.setTo(p);
            TerrainMesh.validateMeshLine(meshLine, warnings);

            return List.of(meshLine);
        }
        // DB style
        return meshLines;
    }

    private MeshNode registerPoint(Coordinate coordinate) {
        int index;
        // check for consistency with large tolerance. 4.9.19: 1->0.2 weil z.B. 161036756 am circle sehr schmal wird.
        if ((index = getPoint(coordinate, 0.2)) != -1) {
            MeshNode existingFound = points.get(index);
            logger.error("duplicate point registration for " + coordinate + ". Nearby existing isType " + existingFound.getCoordinate());
            hasDuplicates = true;
        }
        points.add(meshFactoryInstance.buildMeshNode(coordinate));
        return points.get(points.size() - 1);
    }

    /*private int registerLine(MeshLine line) {
        int startPoint, endPoint;
        if ((startPoint = getPoint(line.get(0))) == -1) {
            startPoint = registerPoint(line.get(0));
        }
        if ((endPoint = getPoint(line.get(line.size() - 1))) == -1) {
            endPoint = registerPoint(line.get(line.size() - 1));
        }
        return registerLine(line, startPoint, endPoint);
    }*/


    public MeshNode getMeshNode(Coordinate coordinate) {
        for (MeshNode p : points) {
            if (p.getCoordinate().equals2D(coordinate, 0.00001)) {
                return p;
            }
        }
        //kein warning, weil das auch zur Pruefung verwendet wird.
        //logger.warn("Meshpoint not found:" + coordinate);
        return null;
    }

    public int getPoint(Coordinate coordinate) {
        return getPoint(coordinate, 0.00001);
    }


    public void addKnownTwoEdger(Coordinate coordinate) {
        int index = getPoint(coordinate);
        if (index == -1) {
            logger.warn("unknown");
            return;
        }
        knowntwoedger.add(index);
    }

    /**
     * Find first occurence of a line with a missing area.
     * optionally boundary or but no boundary.
     * boundaryflag=0 -> all lines
     * boundaryflag=1 -> only boundaries
     * boundaryflag=2 -> no boundaries
     *
     * @return
     */
    public MeshLine findOpenLine(int boundaryflag) {
        for (MeshLine meshLine : lines) {
            boolean lineisopen = false;
            if (meshLine.isBoundary()) {
                if (meshLine.getLeft() == null) {
                    lineisopen = true;
                }
            } else {
                if (meshLine.getLeft() == null || meshLine.getRight() == null) {
                    lineisopen = true;
                }
            }
            if (lineisopen) {
                if (meshLine.isBoundary() && (boundaryflag == 0 || boundaryflag == 1)) {
                    return meshLine;
                }
                if (!meshLine.isBoundary() && (boundaryflag == 0 || boundaryflag == 2)) {
                    return meshLine;
                }
            }
        }
        return null;
    }

    /**
     * Wenn Kanten des Polygon sich mit Boundary (oder andere) Kanten des Mesh decken,
     * müssen bestehende Kanten gesplittet werden. Tritt typischerweise beim Cut auf.
     * <p>
     * Es gibt auch Sonderfaelle, bei denen nur an einem Punkt eine Edgeberührung ist und gesplittet werden muss (z.B.Desdorf Farmalnd).
     * Und es können -vor allem bei wiederholtem Aufruf- auch nur schon Splitpunkte gefunden werden.
     * <p>
     * 7.8.19: Nur für Boundary. Andere Fälle (z.B. adjacent area) sollen vorher erledigt sein.
     * <p>
     * Returns null, wenn keine Edge auf der Boundary liegt, bzw. wenn ein Split nicht erforderlich ist.
     * <p>
     * 20.8.19: Das mit adjacent ist aber auch problematisch (z.B. Runway), darum hier optional Boundaries oder inner.
     *
     * @param polygon
     * @return
     */
    public Collection<MeshLineSplitCandidate> findCommon(LineString polygon, boolean boundaries) {
        Coordinate[] coors = polygon.getCoordinates();
        List<MeshLine> boundaryset = getBoundaries();
        if (!boundaries) {
            boundaryset = getNonBoundaries();
        }
        //TODO das muss auch gesetzt werden
        boolean durchgehend = false;

        //Einfach die Coordinates durchgehen und die erten Treffer verwenden ist zu schlicht, wenn mehrere Lines in Frage kommen. Darum
        //erst Candidates zusammenstellen und dann daraus den most promising verwenden.
        Map<MeshLine, MeshLineSplitCandidate> candidates = new HashMap();
        //Input ist mal polygon, mal linestring, darum nicht immer -1
        boolean isClosed = coors[0].equals2D(coors[coors.length - 1]);
        for (int i = 0; i < coors.length - ((isClosed) ? 1 : 0); i++) {
            List<PointPosition> pointPositions = findPointPositions(boundaryset, coors[i]);
            //die gefundenen koennen unterschiedlich geeignet sein, den second zu fuellen; auch mehrfach!
            //PointPosition pointPosition = findBestSuitedPointPosition(pointPositions,candidates);
            for (PointPosition pointPosition : pointPositions) {
                if (pointPosition != null) {
                    MeshLineSplitCandidate split = candidates.get(pointPosition.meshLine);
                    //common.add(i);
                    if (split == null) {
                        split = new MeshLineSplitCandidate(pointPosition.meshLine);
                        candidates.put(pointPosition.meshLine, split);
                        split.from/*firstposition*/ = pointPosition.index;
                        split.fromIsCoordinate = pointPosition.isCoordinate;
                        split.result.add(coors[i]);
                        split.commonfirst = i;

                    } else {
                        //Zweiter Punkt auf einer anderen line geht so nicht. Wenn die zweite Line
                        //Fortsetzung der ersten ist, den Vertex der beiden als zweiten Punkt nehmen.
                        //Scheint plausibel, aber ob das immer so gilt.
                        //Nee, ich slippe den einfach, denn es muesste dann ja noch einen auf der line geben.
                        //Auch das aber nicht immer, sonst bleibt man beim zweiten Aufruf immer wieder am ersten Point haengen.
                        //dann ersten durch zweiten ersetzen.
                   /*nicht mehr relevant  if (split.meshLineToSplit != pointPosition.meshLine) {
                        /*MeshNode vertex=null;
                        if (split.meshLineToSplit.getFrom().getLines().contains(meshLine.meshLine)){
                            vertex=split.meshLineToSplit.getFrom();
                        }
                        if (split.meshLineToSplit.getTo().getLines().contains(meshLine.meshLine)){
                            vertex=split.meshLineToSplit.getTo();
                        }
                        if (vertex==null){
                            logger.error("second common point on unknown line not supported");
                            return null;
                        }* /
                        //bei wiederholten Aufrufen passiert das immer! Wenn beide Punkte auf einem known Point liegen, gehe ich hier einfach raus.
                       if (firstposition.isCoordinate && pointPosition.isCoordinate) {
                            logger.debug("both points are coordinates. -> no split");
                            return null;
                        }
                        logger.debug("multiple line split? second point " + pointPosition);
                    } else {*/
                        if (split.to != -1/*secondposition != null*/) {
                            //was ist das?? mehr als zwei?? Da duerfte noch was unfertig sein.
                            logger.warn("third common point found? Replacing second.");
                        }
                        split.to/*secondposition*/ = pointPosition.index;
                        split.toIsCoordinate = pointPosition.isCoordinate;
                        split.result.add(coors[i]);
                        split.commonsecond = i;
                    }
                }
            }
        }
        //if (split == null) {
        //  return null;
        //}
        List<MeshLineSplitCandidate> finalcandidates = new ArrayList<>();
        for (MeshLineSplitCandidate split : candidates.values()) {

            // Die Positionen des gefundenen common lineSegment oder der Coordinate, je nach dem.
            PointPosition firstposition = null, secondposition = null;
            firstposition = new PointPosition(split.meshLineToSplit, split.from, split.fromIsCoordinate);
            if (split.to != -1) {
                secondposition = new PointPosition(split.meshLineToSplit, split.to, split.toIsCoordinate);
            }
            if (split.commonfirst == -1 || split.commonsecond == -1) {
                //das kann passieren bei cut areas, die nachher noch verkleinert werden, Z.B. durch waytoarea filler. Stimmt das Beispiel wirklich? Eher Desdorf farmland.
                //darum error->debug
                //Da muss nur ein einzelner neuer Point in die Boundary.
                //Bei wiederholten Aufrufen kann das aber auch passieren. Dann wird ja immer ein gemeinsamer Point gefunden.
                //Darum hier nur eine Splitinfo liefern, wenn der Point keine Coordinate ist.
                /*if (firstposition.isCoordinate) {
                    logger.debug("single common point isType known. ->no split");
                    return null;
                }*/
                //Tja,warn oder debug?
                logger.debug("only one common point found. no edge on boundary?");
                split.remaining = JtsUtil.toList(polygon.getCoordinates());
                split.from = firstposition.index;
                split.to = -1;
                split.newcoors = new ArrayList<>(1);
                split.newcoors.add(coors[split.commonfirst]);
                split.commonsegment = -1;
                //return split;
            } else {
                //die Reihenfolge fromto ist problematisch. GroesserKleiner bringt nichts. Mal annahmen, das der Abstand immer 1 ist.
                int[] fromto;
                if (split.commonfirst == split.commonsecond - 1) {
                    fromto = new int[]{split.commonfirst, split.commonsecond};
                } else {
                    fromto = new int[]{split.commonsecond, split.commonfirst};
                }
                LineString[] res = JtsUtil.removeCoordinatesFromLine(polygon, fromto);
                if (res.length != 1) {
                    logger.warn("unhandled result?");
                }
                List<Coordinate> remaining = JtsUtil.toList(res[0].getCoordinates());
                if (firstposition != null && secondposition == null) {
                    logger.error("only firstindex found. strange. Will cause inconsistent mesh");
                    return null;
                }
        /*20.8.19 falsche Logik if (firstindex != secondindex) {
            //mehrere lines splitten
            logger.error("not yet. Will cause inconsistent mesh");
            return null;
        }*/
        /*Mischmasch ibt es aber, z.B. Runway if (firstindex.isCoordinate != secondindex.isCoordinate) {
            logger.error("unknown mischmasch");
            return null;
        }*/
                // die Logik koennte fuer line und coordinate Positionen stimmen; Aber Mischmasch? Wohl auch.
                if (PointPosition.isFirstBehindSecond(split.meshLineToSplit, firstposition, secondposition, split.result)) {
                    split.to = firstposition.index;
                    split.toIsCoordinate = firstposition.isCoordinate;
                    split.from = secondposition.index;
                    split.fromIsCoordinate = secondposition.isCoordinate;
                    Collections.reverse(split.result);
                    split.newcoors = split.result;
                } else {
                    split.from = firstposition.index;
                    split.fromIsCoordinate = firstposition.isCoordinate;
                    split.to = secondposition.index;
                    split.toIsCoordinate = secondposition.isCoordinate;
                    JtsUtil.sortByDistance(split.result, split.meshLineToSplit.getCoordinates()[firstposition.index]);
                    split.newcoors = split.result;
                }


                //der Default bei drei Segmenten
                split.commonsegment = 1;
                split.remaining = remaining;
                if (split.toIsCoordinate) {
                    //remove to if its at the end resulting in two line split result
                    if (split.to == split.meshLineToSplit.length() - 1) {
                        split.to = -1;
                        Coordinate c = split.newcoors.get(0/*22.8.19 1*/);
                        split.newcoors = new ArrayList<>();
                        split.newcoors.add(c);
                    }
                }
                if (split.fromIsCoordinate && split.to != -1) {
                    //create single point split by removing from and moving "to" to from.
                    if (split.from == 0) {
                        split.from = split.to;
                        split.fromIsCoordinate = split.toIsCoordinate;
                        split.to = -1;
                        if (split.newcoors.size() != 2) {
                            logger.error("inconsistent split?");
                            return new ArrayList<>();
                        }
                        Coordinate c = split.newcoors.get(1);
                        split.newcoors = new ArrayList<>();
                        split.newcoors.add(c);
                        split.commonsegment = 0;
                    }
                }
            }
            if (split.from == 0) {
                int h = 9;
            }
            boolean ignoreSplit = false;
            // nicht immer wieder an einem einzelnen common point haengenbleiben (z.B. Desdorf Farmland Sonderfall)
            // oder generell: Ein Split an known coordinates ist kein Candidate mehr. Den entferne ich.
            if (split.fromIsCoordinate && getMeshNode(split.meshLineToSplit.get(split.from)) != null) {
                if (split.to == -1) {
                    if (SceneryBuilder.TerrainMeshDebugLog) {
                        logger.debug("Ignoring single point split candiate at known coordinate");
                    }
                    ignoreSplit = true;
                } else {
                    if (split.toIsCoordinate && getMeshNode(split.meshLineToSplit.get(split.to)) != null) {
                        if (SceneryBuilder.TerrainMeshDebugLog) {
                            logger.debug("Ignoring split candidate at known coordinates");
                        }
                        ignoreSplit = true;
                    }
                }
            }
            if (!ignoreSplit) {
                finalcandidates.add(split);
            }
        }
        if (SceneryBuilder.TerrainMeshDebugLog) {
            logger.debug("found " + finalcandidates.size() + " split candidates");
        }
        return finalcandidates;
    }

    /**
     * am besten ist die, die ein fehlendes second liefert.
     *
     * @param pointPositions
     * @param candidates
     * @return
     */
    private PointPosition findBestSuitedPointPosition(List<PointPosition> pointPositions, Map<MeshLine, MeshLineSplitCandidate> candidates) {
        for (PointPosition pp : pointPositions) {
            if (candidates.get(pp) != null && candidates.get(pp).commonsecond == -1) {
                return pp;
            }
        }
        //dann kann es irgendeine sein.
        if (pointPositions.size() > 0) {
            return pointPositions.get(0);
        }
        return null;
    }

    public Collection<MeshLineSplitCandidate> findBoundaryCommon(LineString polygon) {
        return findCommon(polygon, true);
    }

    /**
     * @param candidates
     * @return
     */
    public static MeshLineSplitCandidate findBestSplitCandidate(Collection<MeshLineSplitCandidate> candidates) {

        MeshLineSplitCandidate best = null;
        for (MeshLineSplitCandidate candidate : candidates) {
            if (best == null) {
                best = candidate;
            }
            if (candidate.to != -1) {
                if (best.to == -1) {
                    //dann ist der auf jeden Fall besser
                    best = candidate;
                } else {
                    if (best.fromIsCoordinate && best.toIsCoordinate && (!candidate.fromIsCoordinate || !candidate.toIsCoordinate)) {
                        //mit einem auf line auch
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Universelle Split Methode.
     * Split "auf freier Strecke" (zwischen bekannten Coordinates) oder an bekannten Coordinates.
     * Returns three created new lines.
     * Bei einem Sonderfall Split kommen nur zwei new lines.
     * <p>
     * Returns null on error.
     * <p>
     * isValdid() prueft zu umfangreich.
     */
    public MeshLine[] split(MeshLineSplitCandidate meshLineSplit) {

        if (!isPreDbStyle()) {
            Util.notyet();
        }

        /*if (!isValid(true)) {
            logger.error("already invalid");
        }*/
        MeshLine line = meshLineSplit.meshLineToSplit;
        MeshNode p0 = buildPoint(meshLineSplit, meshLineSplit.from, meshLineSplit.fromIsCoordinate, meshLineSplit.newcoors.get(0));
        // also consider special case when "to" isType the end. Then there will be no mid line.
        if (meshLineSplit.isPointSplit()) {
            if (meshLineSplit.newcoors.size() != 1) {
                logger.warn("unexpected newcoord size");
            }
            List<Coordinate> linenewcoors = new ArrayList<>();
            // start with last line
            linenewcoors.add(p0.getCoordinate());
            linenewcoors.addAll(JtsUtil.sublist(line.getCoordinates(), meshLineSplit.from + 1, line.length() - 1));
            MeshLine lastline = buildMeshLinesFromList(linenewcoors).get(0);
            if (lastline == null) {
                return null;
            }
            //cut existing line before registering new last line
            line.getTo().removeLine(line);
            registerLine(lastline);
            lastline.setLeft(line.getLeft());
            lastline.setRight(line.getRight());

            linenewcoors = JtsUtil.sublist(line.getCoordinates(), 0, meshLineSplit.from - ((meshLineSplit.fromIsCoordinate) ? 1 : 0));
            linenewcoors.add(p0.getCoordinate());
            line.setCoordinatesAndTo(JtsUtil.toArray(linenewcoors), lastline.getFrom());
            TerrainMesh.validateMeshLine(line, warnings);
            //line.setTo(lastline.getFrom());
            lastline.getFrom().addLine(line);
            if (line.isBoundary()) {
                lastline.setBoundary(true);
            }
           /* if (!isValid(true)) {
                logger.error("no longer valid");
            }*/
            return new MeshLine[]{line, lastline};
        }
        boolean toWasFrom = line.getFrom() == line.getTo();
        // start with last line
        MeshNode p1 = buildPoint(meshLineSplit, meshLineSplit.to, meshLineSplit.toIsCoordinate, meshLineSplit.newcoors.get(1));

        List<Coordinate> linenewcoors = new ArrayList<>();
        linenewcoors.add(p1.getCoordinate());
        linenewcoors.addAll(JtsUtil.sublist(line.getCoordinates(), meshLineSplit.to + 1, line.length() - 1));
        MeshLine lastline = buildMeshLinesFromList(linenewcoors).get(0);
        //wenn die line closed ist, nicht entfernen, sonst hängt der Anfang in der Luft.
        if (!toWasFrom) {
            line.getTo().removeLine(line);
        }
        //cut existing line before registering new last line
        registerLine(lastline);
        line.getTo().addLine(lastline);

        linenewcoors = JtsUtil.sublist(line.getCoordinates(), 0, meshLineSplit.from - ((meshLineSplit.fromIsCoordinate) ? 1 : 0));
        linenewcoors.add(p0.getCoordinate());
        //line.coordinates = JtsUtil.toArray(linenewcoors);
        MeshLine midline = buildMeshLinesFromList(meshLineSplit.newcoors).get(0);
        registerLine(midline);
        midline.getFrom().addLine(line);
        line.setCoordinatesAndTo(JtsUtil.toArray(linenewcoors), midline.getFrom());
        TerrainMesh.validateMeshLine(line, warnings);

        //line.setTo(midline.getFrom());
        if (line.isBoundary()) {
            midline.setBoundary(true);
            lastline.setBoundary(true);
        }
        midline.setLeft(line.getLeft());
        midline.setRight(line.getRight());
        lastline.setLeft(line.getLeft());
        lastline.setRight(line.getRight());
        /*if (!isValid(true)) {
            logger.error("no longer valid");
        }*/
        return new MeshLine[]{line, midline, lastline};
    }

    private List<PointPosition> findPointPositions(List<MeshLine> lineset, Coordinate coor) {
        List<PointPosition> result = new ArrayList<>();
        if (coor.distance(new Coordinate(154.51, -92.784)) < 0.1) {
            int h = 9;
        }
        for (MeshLine line : lineset) {
            int lineIndex;
            if ((lineIndex = JtsUtil.findVertexIndex(coor, line.getCoordinates())) != -1) {
                result.add(new PointPosition(line, lineIndex, true));
            } else {
                if ((lineIndex = line.getCoveringSegment(coor)) != -1) {
                    result.add(new PointPosition(line, lineIndex, false));
                }
            }
        }
        return result;
    }

    private int registerLine(MeshLine meshLine) {
        lines.add(meshLine);
        int index = lines.size() - 1;
        meshLine.getFrom().addLine(meshLine);
        meshLine.getTo().addLine(meshLine);
        return lines.size() - 1;
    }

    public int getPoint(Coordinate coordinate, double tolerance) {
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).getCoordinate().equals2D(coordinate, tolerance)) {
                return i;
            }
        }
        return -1;
    }

    private MeshNode buildPoint(MeshLineSplitCandidate meshLineSplit, int index, boolean isCoordinate, Coordinate c) {
        if (isCoordinate) {
            int i = getPoint(meshLineSplit.meshLineToSplit.getCoordinates()[index]);
            if (i == -1) {
                return registerPoint(meshLineSplit.meshLineToSplit.getCoordinates()[index]);
            }
            return points.get(i);
        }
        return registerPoint(c);
    }

    /**
     * Split an existierender Coordinate.
     * Returns two resulting lines. Meistens.
     */
    public MeshLine[] split(MeshLine line, int coorindex) {
        if (coorindex == 0 || coorindex == line.length() - 1) {
            return new MeshLine[]{line};
        }
        MeshLineSplitCandidate msc = new MeshLineSplitCandidate(line);
        msc.fromIsCoordinate = true;
        msc.from = coorindex;
        msc.toIsCoordinate = true;
        msc.to = -1;
        msc.newcoors = new ArrayList<>();
        msc.newcoors.add(line.getCoordinates()[coorindex]);
        MeshLine[] result = split(msc);
        return result;
    }

    /**
     * Split an zwei existierenden Coordinates.
     * Returns three resulting lines, except...
     */
    public MeshLine[] split(MeshLine line, int coorindex0, int coorindex1) {
        //erst hinten splitten. CCW
        MeshLine[] secondsplit = split(line, coorindex1);
        MeshLine[] firstsplit = split(line, coorindex0);
        if (firstsplit == null || secondsplit == null) {
            logger.error("failure");
            return null;
        }
        return new MeshLine[]{firstsplit[0], firstsplit[1], secondsplit[1]};
    }

    /**
     * Returns boundaries that are shared.
     *
     * @return
     */
    public List<MeshLine> getSharedBoundaries() {
        //List<MeshLine> result = new ArrayList();
        return lines.stream().filter(o -> o.isBoundary() && o.getLeft() != null).collect(Collectors.toList());
    }

    public List<MeshLine> getBoundaries() {
        //List<MeshLine> result = new ArrayList();
        return lines.stream().filter(o -> o.isBoundary()).collect(Collectors.toList());
    }

    public List<MeshLine> getNonBoundaries() {
        //List<MeshLine> result = new ArrayList();
        return lines.stream().filter(o -> !o.isBoundary()).collect(Collectors.toList());
    }

    /**
     * Ob das immer zuverlaessig geht. Hmm.
     *
     * @param meshLine
     * @param area
     */
    public void completeLine(MeshLine meshLine, AbstractArea/*SceneryFlatObject*/ area) {
        if (area.toString().contains("23696494")) {
            int h = 9;
        }
        if (meshLine == null) {
            logger.error("inconsistent call");
            return;
        }
        if (meshLine.isBoundary()) {
            if (meshLine.getLeft() != null) {
                if (meshLine.getLeft() != area) {
                    logger.error("left already set");
                } else {
                    logger.debug("Overriding with same value.why?");
                }
            }
            //2.5.24 meshLine.setLeft(area);
            return;
        }
        if (meshLine.getLeft() == null && meshLine.getRight() == null) {
            logger.error("both empty??");
        }
        if (meshLine.getLeft() == area || meshLine.getRight() == area) {
            //logger.debug("already set");
            return;
        }

        if (meshLine.getLeft() == null) {
            //2.5.24 meshLine.setLeft(area);
        } else {
            if (meshLine.getRight() == null) {
                //2.5.24 meshLine.setRight(area);
            }
        }

    }

    public int getStep() {
        return step;
    }

    public boolean hasUnFixedElevation() {
        for (MeshNode meshNode : points) {
            /*if (meshPoint.group==null){
                logger.error("no group");
                return false;
            }*/
        }
        return false;
    }

    public void calculateElevations(ElevationProvider elevationProvider) {
        //Alle Mesh Points muessen jetzt ueber ihre EGR eine fixe Elevation haben.
        if (/*TerrainMesh.getInstance().*/hasUnFixedElevation()) {
            throw new RuntimeException("no fixed elevation");
        }
        Set<Coordinate> coorset = new HashSet();
        for (MeshLine meshLine : lines) {
            coorset.addAll(JtsUtil.toList(meshLine.getCoordinates()));
        }
        ElevationCalculator.calculateElevationsForCoordinates((Coordinate[]) coorset.toArray(new Coordinate[0]), "TerrainMesh", this);
        //ElevationCalculator.calculateElevationsForVertexCoordinates()
    }

    /**
     * Nicht Covering, sondern exakt eine Coordinate.
     * <p>
     * 4.9.19: Das ist ja nun nicht eindeutig. Otpional mit area und mehrdeutig loggen.
     * 5.9.19: Tortzdem ist das bei coor==meshnode nie eindeutig. Da wird es immer mehr als eine geben. Darum kommt die
     * ganze Liste zurueck. Soll der Aufrufer doch sehen.
     *
     * @param coor
     * @return
     */
    public List<MeshLine> findLines(AbstractArea area, Coordinate coor) {
        List<MeshLine> result = new ArrayList<>();
        for (MeshLine ml : lines) {
            if (ml.contains(coor) && (area == null || (ml.getLeft() == area || ml.getRight() == area))) {
                /*if (result == null) {
                    result = ml;
                } else {
                    logger.error("findLine: non unique result for coordinate " + coor + ". Returning " + result);
                }*/
                result.add(ml);
            }
        }
        return result;
    }

    /**
     * Die Line muss existieren.
     *
     * @param pair
     * @return
     */
    public MeshLine findLineBetweenExistingPoints(CoordinatePair pair) {
        MeshNode p0 = getMeshNode(pair.getFirst());
        if (p0 == null) {
            logger.error("no point");
            return null;
        }
        MeshNode p1 = getMeshNode(pair.getSecond());
        if (p1 == null) {
            logger.error("no point");
            return null;
        }
        for (MeshLine meshLine : p0.getLines()) {
            if (getOpposite(meshLine, p0) == p1) {
                return meshLine;
            }
        }
        logger.error("no line found");
        return null;
    }

    /**
     * Returns edges that are shared (non boundaries).
     *
     * @return
     */
    public List<MeshLine> getShared() {
        //List<MeshLine> result = new ArrayList();
        return lines.stream().filter(o -> o.getRight() != null && o.getLeft() != null).collect(Collectors.toList());
    }

    /**
     * Liefert alle Lines die an zwei "normale" Areas grenzen, ie. kein BG.
     *
     * @return
     */
    public List<MeshLine> getSharedLines() {
        List<MeshLine> result = new ArrayList<>();
        for (MeshLine meshLine : lines) {
            if (meshLine.getLeft() != null && meshLine.getRight() != null) {
                /*2.5.24 if (!StringUtils.contains(meshLine.getLeft().parentInfo, "BG") && !StringUtils.contains(meshLine.getRight().parentInfo, "BG")) {
                    result.add(meshLine);
                }*/
            }
        }
        return result;
    }

    public List<MeshLine> getShared(AbstractArea a1, AbstractArea a2) {
        return lines.stream().filter(o -> (o.getRight() == a1 && o.getLeft() == a2) || (o.getRight() == a2 && o.getLeft() == a1)).collect(Collectors.toList());
    }

    public MeshLine findClosestLine(Coordinate c) {
        double bestdistance = Double.MAX_VALUE;
        MeshLine best = null;
        for (MeshLine line : lines) {
            double distance = line.getDistance(c);
            if (distance < bestdistance) {
                bestdistance = distance;
                best = line;
            }
        }
        return best;
    }

    /**
     * die left/right lines eines Way sind ja nicht zu jedem Zeitpunkt (fehlende Connector) fortlaufend.
     * Was will denn der Aufrufer?
     * Das ist zum Fuellen der durch split entstandenen Fortläufigkeit. Naja, das muss sich noch bewähren, weil es nicht zwingend eindeutig ist (z.B. Querverbindungen).
     */
    public List<MeshLine> findLineOfWay(MeshLine from, MeshNode to, WayArea wayArea, boolean left) {
        List<MeshLine> lines = new ArrayList();
        if (from == null) {
            logger.error("invalid usage");
            return lines;
        }

        MeshNode point = from.getTo();
        lines.add(from);
        int cntr = 0;
        MeshLine line = null;
        while (point != to && cntr++ < 100) {
            try {
                line = getSuccessor(point,null /*2.5.24wayArea*/, left, line);
            } catch (MeshInconsistencyException e) {
                throw new RuntimeException(e);
            }
            if (line == null) {
                logger.error("inconsistent way?");
                return lines;
            }
            lines.add(line);
            point = getOpposite(line, point);
        }

        if (cntr >= 100) {
            logger.error("cntr overflow");
        }
        return lines;

    }

    public static void validateMeshLine(MeshLine meshLine, List<String> warnings) {
        MeshNode from = meshLine.getFrom();
        MeshNode to = meshLine.getTo();
        if (from != null && to != null && from == to) {
            //warum sollte from nicht gleich to sein? Wenns doch closed ist.
            //logger.error("from==to");
            //SceneryContext.getInstance().warnings.add("invalid mesh line found");
        }

        //Konsistenzcheck auf doppelte
        boolean isClosed = meshLine.isClosed();
        Coordinate[] coordinates = meshLine.getCoordinates();
        for (int i = 0; i < coordinates.length - ((isClosed) ? 1 : 0); i++) {
            if (JtsUtil.findVertexIndex(coordinates[i], coordinates) != i) {
                logger.error("duplicate coordinate?");
                warnings.add("invalid mesh line found");
            }
        }
        if (from != null && !from.getCoordinate().equals2D(coordinates[0])) {
            logger.error("from not first coordinate");
            warnings.add("invalid mesh line found");
        }
        if (to != null && !to.getCoordinate().equals2D(coordinates[meshLine.length() - 1])) {
            logger.error("to not last coordinate");
            warnings.add("invalid mesh line found");
        }
    }

    public static class PointPosition {
        // Der Index des Point auf der Line. NeeNee, nicht unbedingt. Der Punkt muss nicht eine Coordinate sein, sondern kann auch "auf freier Strecke" sein.
        // Dann ist das ist der Index des LineSegment, auf dem der Punkt liegt.
        public final int index;
        public final MeshLine meshLine;
        public boolean isCoordinate;

        public PointPosition(MeshLine line, int position, boolean isCoordinate) {
            this.meshLine = line;
            this.index = position;
            this.isCoordinate = isCoordinate;
        }

        @Override
        public String toString() {
            return "on line " + meshLine + ",isCoordinate=" + isCoordinate;
        }

        public static boolean isFirstBehindSecond(MeshLine line, PointPosition first, PointPosition second, List<Coordinate> newcoors) {
            if (first.isCoordinate) {
                if (second.isCoordinate) {
                    return first.index > second.index;
                } else {
                    //dann liegt second auf der line und damit behind
                    return first.index > second.index;
                }
            } else {
                if (second.isCoordinate) {
                    if (first.index == second.index) {
                        //dann ist second als isCoordinate naeher und damit first hinter second
                        return true;
                    }
                    return first.index > second.index;
                } else {
                    if (first.index == second.index) {
                        //geht nur mit distance
                        Coordinate start = line.getCoordinates()[first.index];
                        if (start.distance(newcoors.get(0)) > start.distance(newcoors.get(1))) {
                            return true;
                        }
                        return false;
                    }
                    return first.index > second.index;
                }
            }
        }
    }

    /**
     * Die segments werden neu registriert, die existingShares bekommen nur left/right.
     */
    public void createMeshPolygon(List<LineString> segments, List<MeshLine> existingShares, Polygon polygonOfArea, AbstractArea abstractArea) throws MeshInconsistencyException {
        List<MeshLine> lines = new ArrayList<>();
        for (LineString segment : segments) {
            if (segment == null) {
                logger.error("inconsistent poly?");
            } else {
                MeshLine meshLine = addSegmentToTerrainMesh(segment, polygonOfArea, abstractArea);
                lines.add(meshLine);
            }
        }
        // Wegen Konsistenz erst jetzt die areas der shares setzen
        for (MeshLine es : existingShares) {
            completeLine(es, abstractArea);
        }

        //lines sind nicht sortiert. Das macht der Konstruktor. Und es fehlen noch die shared Segmente, die gab es ja
        //schon vorher.
        lines.addAll(existingShares);
        //2.5.24 abstractArea.isPartOfMesh = true;
        //MeshPolygon meshPolygon = new MeshPolygon(lines);
        //((Area) flatComponent[index]).setMeshPolygon(/*index,*/meshPolygon);
        //index++;

        //Gegenprobe
        /*2.5.24 if (getPolygon(abstractArea) == null) {
            logger.error("Gegenprobe failed");
        }*/
    }

    public MeshLine addSegmentToTerrainMesh(LineString segment, Polygon polygon, AbstractArea abstractArea) {
        Boolean areaIsLeft = JtsUtil.isPolygonLeft(segment, polygon);
        if (areaIsLeft == null) {
            logger.error("doing kappes");
            areaIsLeft = true;
        }
        MeshLine meshLine = null;//2.5.24 registerLine(segment, (areaIsLeft) ? abstractArea : null, (areaIsLeft) ? null : abstractArea);
        return meshLine;
    }

    public boolean isPreDbStyle() {
        return gridCellBounds.isPreDbStyle();
    }

    public String toSvg() {

        // should have same sizes to make scaling successful in both span cases?
        int width = 800;
        int height = 800;
        String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" baseProfile=\"full\" width=\"" + width + "px\" height=\"" + height + "px\" viewBox=\"0 0 " + width + " " + height + "\">\n";

        svg += " <rect x=\"0\" y=\"0\" width=\"" + width + "\" height=\"" + height +
                "\" stroke=\"green\" stroke-width=\"1px\" fill=\"white\"/>\n";

        svg += "<g transform=\"translate(" + width / 2 + "," + height / 2 + ")\">";

        VectorXZ bottomLeft = gridCellBounds.getProjection().getBaseProjection().calcPos(gridCellBounds.getBottomLeft());
        VectorXZ topRight = gridCellBounds.getProjection().getBaseProjection().calcPos(gridCellBounds.getTopRight());

        // maxextension in gridCellBounds can be used instead?
        double spanX = topRight.x - bottomLeft.x;
        double spanY = topRight.z - bottomLeft.z;
        double scale;
        if (spanX > spanY) {
            scale = (double) width / spanX;
        } else {
            scale = (double) height / spanY;
        }

        String fontSize10px = "10px";
        String fontSize6px = "6px";
        String fontSize4px = "4px";

        for (MeshLine line : lines) {
            int x1 = (int) (line.getFrom().getCoordinate().x * scale);
            int y1 = -(int) (line.getFrom().getCoordinate().y * scale);
            int x2 = (int) (line.getTo().getCoordinate().x * scale);
            int y2 = -(int) (line.getTo().getCoordinate().y * scale);
            svg += " <line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" stroke=\"black\"/>\n";
            int tx = x1 + (x2 - x1) / 2;
            int ty = y1 + (y2 - y1) / 2;
            // line label
            svg += svgText(tx, ty, line.getLabel(), fontSize10px);
        }
        for (MeshNode node : points) {
            int x = (int) (node.getCoordinate().x * scale);
            int y = -(int) (node.getCoordinate().y * scale);
            svg += svgText(x, y, node.getLabel(), fontSize6px);
        }
        svg += "</g>";
        svg += "</svg>";
        return svg;
    }

    private String svgText(int x, int y, String text, String fontSize) {
        // text scale also applies to position
        return " <text x=\"" + x + "\" y=\"" + y + "\" font-size=\"" + fontSize + "\" fill=\"" + "black" + "\" transform=\"" + "scale(1.0)" + "\">"
                + text + "</text>\n";

    }

    private boolean crosses(MeshLine line, Polygon polygon) {
        LineString lineString = JtsUtil.createLine(line.getFrom().getCoordinate(), line.getTo().getCoordinate());
        return lineString.crosses(polygon);
    }

    /**
     * Remove line from all nodes an finally delete it.
     */
    void deleteLineFromMesh(MeshLine line) {
        line.getFrom().removeLine(line);
        line.getTo().removeLine(line);
        meshFactoryInstance.deleteMeshLine(line);
        lines.remove(line);
    }

    /**
     * Connect a node to a polygon. The node must reside inside the polygon, (not even on the outline?).
     * Returns null when no connection cannot be build.
     */
    public MeshLine connectNodeToPolygon(MeshNode node, Sector sector, MeshPolygon origin, MeshPolygon polygonToConnectTo) {

        // MeshNodeDetails details = new MeshNodeDetails(node);
        //Sector sector = details.getNeighborSector(nod);

        for (MeshNode n : sector.getNodesOfPolygonInSector(polygonToConnectTo)) {
            @Deprecated // should use LineSegment
            LineString line = JtsUtil.createLine(node.getCoordinate(), n.getCoordinate());
            LineSegment lineSegment = JtsUtil.createLineSegment(node.getCoordinate(), n.getCoordinate());
            // TODO check its completely in polygon
            // should neither interect the origin polygon, ...
            if (!JtsUtil.isIntersecting(line, List.of(origin.getPolygon()))) {
                // ... nor any other line except 'polygonToConnectTo'
                if (!isReallyIntersectingAnyLine(lineSegment)) {
                    return meshFactoryInstance.buildMeshLine(node, n);
                }
            }
        }
        return null;
    }


}


