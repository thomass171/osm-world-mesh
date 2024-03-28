package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.elevation.ElevationCalculator;
import de.yard.threed.osm2scenery.modules.AerowayModule;
import de.yard.owm.services.persistence.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshLineSplitCandidate;
import de.yard.owm.services.persistence.MeshNode;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.WayArea;
import de.yard.threed.osm2scenery.util.CoordinatePair;
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
 *
 * Wird in drei Stufen aufgebaut:
 * 1) Boundary
 * 2) Ways+Connector
 * 3) Areas (die koennen dann an die Ways anbinden)
 * 4) Supplements
 */
public class TerrainMesh {
    //static TerrainMesh instance;
    static Logger logger = Logger.getLogger(TerrainMesh.class);
    public int errorCounter = 0;
    GridCellBounds gridCellBounds;
    public List<MeshNode> points = new ArrayList();
    public List<MeshLine> lines = new ArrayList();
    //public Map<Integer, List<Integer>> linesOfPoint = new HashMap();
    List<Integer> knowntwoedger = new ArrayList<>();
    //geht nicht, weil durch split noch welche dazukommen private int lastboundaryindex;
    int step = 0;
    //so einer kann nicht mehr valid sein
    private boolean hasDuplicates = false;

    /**
     * gridCellBounds are the outer boundaries of the (sub)mesh.
     */
    private TerrainMesh(GridCellBounds gridCellBounds) {
        this.gridCellBounds = gridCellBounds;
        Polygon boundary = gridCellBounds.getPolygon();
        Coordinate[] coors = boundary.getCoordinates();
        for (int i = 0; i < coors.length; i++) {
            if (i < coors.length - 1) {
                registerPoint(coors[i]);
            }
            if (i > 0) {
                //sfo might be null if there is no lazy cut. 27.3.24: Only pre DB has cuts
                if (gridCellBounds.isPreDbStyle()) {
                    SceneryFlatObject sfo = gridCellBounds.getLazyCutObjectOfCoordinate(coors[i - 1], coors[i]);
                    MeshLine meshLine = registerLine(JtsUtil.toList(coors[i - 1], coors[i]), (sfo != null) ? sfo.getArea()[0] : null, null, true, true);

                    meshLine.isBoundary = true;
                } else {
                    MeshLine meshLine = registerLine(JtsUtil.toList(coors[i - 1], coors[i]), null, null, true, true);

                    meshLine.isBoundary = true;
                }
                //lines.add(meshLine);
            }
            //bei Boundary kann es immr mal bei nur zwei bleiben
            knowntwoedger.add(i);
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
        throw new RuntimeException("inconsistent mesh polygon");
    }

    /**
     * Seit split einer area zwi AbstractArea erzeugt, dürfte das hier eindeutig sein.
     *
     * @param area
     * @return
     */
    public MeshPolygon getPolygon(AbstractArea/*SceneryFlatObject*/ area) {
        for (MeshLine startline : lines) {
            if (startline.getLeft() == area || startline.getRight() == area) {
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
    public MeshPolygon getPolygon(MeshLine startline, AbstractArea/*SceneryFlatObject*/ area) {
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
        MeshPolygon polygon = traversePolygon(startline, area, left);
        return polygon;
    }

    public MeshPolygon traversePolygon(MeshLine startline, AbstractArea/*SceneryFlatObject*/ area, boolean left) {
        int abortcounter = 0;
        MeshLine line = startline;
        MeshNode next = line.getTo();
        List<MeshLine> result = new ArrayList<>();

        do {
            result.add(line);
            if (line.getTo() == line.getFrom()) {
                // Sonderfall closed line
                break;
            }
            MeshLine nextline = getSuccessor(next, area, left, line);
            if (nextline == null) {
                logger.error("traversePolygon: inconsistency?");
                return null;
            }
            next = getOpposite(nextline, next);
            line = nextline;
            if (next.coordinate.distance(new Coordinate(-199, -149)) < 3) {
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
     * Eine line der Area area an einem point liefern.
     * Mit origin laesst sich ein moeglicher Origin ausschliessen um nicht im Kreis zu laufen.
     * 9.9.19: Das ist doch bei Ways so nicht eindeutig wegen innerer Querverbindungen? Oder gibt es sowas nicht?
     */
    public MeshLine getSuccessor(MeshNode meshNode, AbstractArea area, boolean left, MeshLine origin) {
        List<MeshLine> candidates = new ArrayList();

        for (MeshLine line : meshNode.getLines()) {
            if (line != origin) {
                boolean skipLine = false;
                AbstractArea areaToCheck = null;
                if (line.getFrom() == meshNode) {
                    if (left) {
                        areaToCheck = line.getLeft();
                    } else {
                        areaToCheck = line.getRight();
                        //bei der Suche nach Leerflächen keine BoundaryLine beachten mit left != null, denn right ist immer 0
                        if (line.isBoundary && area == null) {
                            skipLine = true;
                        }
                    }
                } else {
                    if (left) {
                        areaToCheck = line.getRight();
                        //bei der Suche nach Leerflächen keine BoundaryLine beachten mit left != null, denn right ist immer 0
                        if (line.isBoundary && area == null) {
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
        LineString originline = (origin == null) ? null : origin.line;
        if (candidates.size() == 0) {
            logger.error("no successor at point " + meshNode + " for origin " + originline);
            errorCounter++;
            return null;
        }
        if (candidates.size() > 1) {
            //das deutet doch auf ein grundsätzliches Konsistenzproblem. Ausser bei BG (area==null). Dann kann so etwas wirklich mal vorkommen.
            //Tja, das bekommt man aber wohl nicht gelöst. Für den Sonderfall start auf boundary waehle ich eine nicht boundary. Das hilft beim Desdorf Farmland,
            //aber obs allgemeingültig ist??
            if (origin != null && origin.isBoundary) {
                if (candidates.get(0).isBoundary && !candidates.get(1).isBoundary) {
                    return candidates.get(1);
                }
                if (!candidates.get(0).isBoundary && candidates.get(1).isBoundary) {
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
     * 5.8.19:Auch sonstige Areas.NeeNee.
     */
    public void addWays(SceneryObjectList sceneryObjects) {
        if (step != 1) {
            throw new RuntimeException("invalid step");
        }
        for (SceneryObject obj : sceneryObjects.objects) {
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
        if (!isValid(true)) {
            logger.error("invalid after adding ways and way connector");
        }
    }

    public void addAreas(SceneryObjectList sceneryObjects) {
        if (step != 2) {
            throw new RuntimeException("invalid step");
        }
        for (SceneryObject obj : sceneryObjects.objects) {
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
        if (!isValid(true)) {
            logger.error("invalid after adding areas");
        }
    }

    /**
     * GapFiller sind hier noch nicht dabei. Die registrieren sich spaeter selber.
     * Die Liste enthält nur Supplements.
     */
    public void addSupplements(List<SceneryObject> supplements) {
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


    public MeshLine registerLine(LineString line, AbstractArea/*SceneryFlatObject*/ left, AbstractArea/*SceneryFlatObject*/ right) {
        return registerLine(JtsUtil.toList(line.getCoordinates()), left, right, false, false);
    }

    public MeshLine registerLine(List<Coordinate> line, AbstractArea/*SceneryFlatObject*/ left, AbstractArea/*SceneryFlatObject*/ right,
                                 boolean startOnGrid, boolean endOnGrid) {
        //logger.debug("new line with " + line.size() + " coordinates");
        MeshLine meshLine = buildMeshLineFromList(line);
        int startPoint, endPoint;
        if (startOnGrid) {
            MeshNode p = getMeshNode(line.get(0));
        }
        if (endOnGrid) {
            MeshNode p = getMeshNode(line.get(line.size() - 1));
        }
        meshLine.setLeft(left);
        meshLine.setRight(right);
        //return /*lines.get(*/registerLine(meshLine/*, startPoint, endPoint*/);
        registerLine(meshLine);
        return meshLine;
    }

    public MeshLine registerLine(MeshNode p0, MeshNode p1, AbstractArea/*SceneryFlatObject*/ left, AbstractArea/*SceneryFlatObject*/ right) {
        MeshLine meshLine = MeshLine.buildMeshLine(new Coordinate[]{p0.coordinate, p1.coordinate});
        if (meshLine == null) {
            return null;
        }
        meshLine.setFrom(p0);
        meshLine.setTo(p1);
        meshLine.setLeft(left);
        meshLine.setRight(right);
        registerLine(meshLine);
        return meshLine;
    }

    public boolean isValid() {
        return isValid(false);
    }

    /**
     * Ob points mir nur zwei lines wirklich invalid sind?? eigentlich doch nicht. Für manche Tests ist es aber brauchbar.
     *
     * @param ignoretwoliner
     * @return
     */
    public boolean isValid(boolean ignoretwoliner) {
        boolean valid = true;
        for (int i = 0; i < points.size(); i++) {
            MeshNode point = points.get(i);

            if (point.getLineCount() == 0) {
                logger.warn("no line at point " + points.get(i).coordinate + "(" + i + "): ");
                valid = false;
            } else {
                if (point.getLineCount() < 2) {
                    if (!point.getLines().get(0).isClosed()) {
                        logger.warn("only one line at point " + points.get(i).coordinate + "(" + i + "): ");
                        valid = false;
                    }
                } else {
                    if (point.getLineCount() == 2 && !ignoretwoliner && !knowntwoedger.contains(i)) {
                        logger.warn("too few lines at point " + points.get(i).coordinate + "(" + i + "): " + point.getLineCount());
                        valid = false;
                    } else {
                        //bei Way junctions gibt es auch mal 4, darum > 4.
                        if (point.getLineCount() > 4) {
                            //21.8.19:aber da ist doch was faul
                            logger.warn("too many lines at point " + points.get(i).coordinate + "(" + i + "): " + point.getLineCount());
                            valid = false;
                        }
                    }
                }
            }
            // each point covering a line needs to be part of that line
            for (MeshLine meshLine : lines) {
                if (meshLine.getCoveringSegment(point.coordinate) != -1) {
                    if (!meshLine.contains(point.coordinate)) {
                        logger.error("validation: point covers line but not part of it:" + point);
                        valid = false;
                    }
                }
            }

        }

        // fuer jede Area muss es einen konsistenten Polygon geben.
        Map<AbstractArea, Void> areas = new HashMap<>();
        for (MeshLine line : lines) {
            if (line.getLeft() != null) {
                areas.put(line.getLeft(), null);
            }
            if (line.getRight() != null) {
                areas.put(line.getRight(), null);
            }
        }
        for (AbstractArea abstractArea : areas.keySet()) {
            MeshPolygon meshPolygon = getPolygon(abstractArea);
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
        return valid;
    }

    private MeshLine buildMeshLineFromList(List<Coordinate> line) {
        MeshLine meshLine = MeshLine.buildMeshLine((Coordinate[]) line.toArray(new Coordinate[0]));
        if (meshLine == null) {
            return null;
        }
        MeshNode p;
        if ((p = getMeshNode(line.get(0))) == null) {
            p = registerPoint(line.get(0));
        }
        meshLine.setFrom(p);
        if ((p = getMeshNode(line.get(line.size() - 1))) == null) {
            p = registerPoint(line.get(line.size() - 1));
        }
        meshLine.setTo(p);
        return meshLine;
    }

    private MeshNode registerPoint(Coordinate coordinate) {
        int index;
        // check for consistency with large tolerance. 4.9.19: 1->0.2 weil z.B. 161036756 am circle sehr schmal wird.
        if ((index = getPoint(coordinate, 0.2)) != -1) {
            MeshNode existingFound = points.get(index);
            logger.error("duplicate point registration for " + coordinate + ". Nearby existing isType " + existingFound.coordinate);
            hasDuplicates = true;
        }
        points.add(new MeshNode(coordinate));
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
            if (p.coordinate.equals2D(coordinate, 0.00001)) {
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
            if (meshLine.isBoundary) {
                if (meshLine.getLeft() == null) {
                    lineisopen = true;
                }
            } else {
                if (meshLine.getLeft() == null || meshLine.getRight() == null) {
                    lineisopen = true;
                }
            }
            if (lineisopen) {
                if (meshLine.isBoundary && (boundaryflag == 0 || boundaryflag == 1)) {
                    return meshLine;
                }
                if (!meshLine.isBoundary && (boundaryflag == 0 || boundaryflag == 2)) {
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
            linenewcoors.add(p0.coordinate);
            linenewcoors.addAll(JtsUtil.sublist(line.getCoordinates(), meshLineSplit.from + 1, line.length() - 1));
            MeshLine lastline = buildMeshLineFromList(linenewcoors);
            if (lastline == null) {
                return null;
            }
            //cut existing line before registering new last line
            line.getTo().removeLine(line);
            registerLine(lastline);
            lastline.setLeft(line.getLeft());
            lastline.setRight(line.getRight());

            linenewcoors = JtsUtil.sublist(line.getCoordinates(), 0, meshLineSplit.from - ((meshLineSplit.fromIsCoordinate) ? 1 : 0));
            linenewcoors.add(p0.coordinate);
            line.setCoordinatesAndTo(JtsUtil.toArray(linenewcoors), lastline.getFrom());
            //line.setTo(lastline.getFrom());
            lastline.getFrom().addLine(line);
            if (line.isBoundary) {
                lastline.isBoundary = true;
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
        linenewcoors.add(p1.coordinate);
        linenewcoors.addAll(JtsUtil.sublist(line.getCoordinates(), meshLineSplit.to + 1, line.length() - 1));
        MeshLine lastline = buildMeshLineFromList(linenewcoors);
        //wenn die line closed ist, nicht entfernen, sonst hängt der Anfang in der Luft.
        if (!toWasFrom) {
            line.getTo().removeLine(line);
        }
        //cut existing line before registering new last line
        registerLine(lastline);
        line.getTo().addLine(lastline);

        linenewcoors = JtsUtil.sublist(line.getCoordinates(), 0, meshLineSplit.from - ((meshLineSplit.fromIsCoordinate) ? 1 : 0));
        linenewcoors.add(p0.coordinate);
        //line.coordinates = JtsUtil.toArray(linenewcoors);
        MeshLine midline = buildMeshLineFromList(meshLineSplit.newcoors);
        registerLine(midline);
        midline.getFrom().addLine(line);
        line.setCoordinatesAndTo(JtsUtil.toArray(linenewcoors), midline.getFrom());
        //line.setTo(midline.getFrom());
        if (line.isBoundary) {
            midline.isBoundary = true;
            lastline.isBoundary = true;
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
            if (points.get(i).coordinate.equals2D(coordinate, tolerance)) {
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
        return lines.stream().filter(o -> o.isBoundary && o.getLeft() != null).collect(Collectors.toList());
    }

    public List<MeshLine> getBoundaries() {
        //List<MeshLine> result = new ArrayList();
        return lines.stream().filter(o -> o.isBoundary).collect(Collectors.toList());
    }

    public List<MeshLine> getNonBoundaries() {
        //List<MeshLine> result = new ArrayList();
        return lines.stream().filter(o -> !o.isBoundary).collect(Collectors.toList());
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
        if (meshLine.isBoundary) {
            if (meshLine.getLeft() != null) {
                if (meshLine.getLeft() != area) {
                    logger.error("left already set");
                } else {
                    logger.debug("Overriding with same value.why?");
                }
            }
            meshLine.setLeft(area);
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
            meshLine.setLeft(area);
        } else {
            if (meshLine.getRight() == null) {
                meshLine.setRight(area);
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
                if (!StringUtils.contains(meshLine.getLeft().parentInfo, "BG") && !StringUtils.contains(meshLine.getRight().parentInfo, "BG")) {
                    result.add(meshLine);
                }
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
            line = getSuccessor(point, wayArea, left, line);
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
    public void createMeshPolygon(List<LineString> segments, List<MeshLine> existingShares, Polygon polygonOfArea, AbstractArea abstractArea) {
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
        abstractArea.isPartOfMesh = true;
        //MeshPolygon meshPolygon = new MeshPolygon(lines);
        //((Area) flatComponent[index]).setMeshPolygon(/*index,*/meshPolygon);
        //index++;

        //Gegenprobe
        if (getPolygon(abstractArea) == null) {
            logger.error("Gegenprobe failed");
        }
    }

    public MeshLine addSegmentToTerrainMesh(LineString segment, Polygon polygon, AbstractArea abstractArea) {
        Boolean areaIsLeft = JtsUtil.isPolygonLeft(segment, polygon);
        if (areaIsLeft == null) {
            logger.error("doing kappes");
            areaIsLeft = true;
        }
        MeshLine meshLine = registerLine(segment, (areaIsLeft) ? abstractArea : null, (areaIsLeft) ? null : abstractArea);
        return meshLine;
    }
}


