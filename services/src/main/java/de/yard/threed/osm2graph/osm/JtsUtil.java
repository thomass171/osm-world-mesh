package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.math.Vector2D;
import com.vividsolutions.jts.triangulate.ConformingDelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.ConstraintEnforcementException;
import de.yard.threed.core.*;
import de.yard.threed.core.geometry.Shape;
import de.yard.threed.osm2world.*;
import org.apache.log4j.Logger;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static de.yard.threed.osm2graph.osm.OsmUtil.toVector2;

/**
 * Created on 12.07.18.
 */
public class JtsUtil {
    static Logger logger = Logger.getLogger(JtsUtil.class.getName());
    public static GeometryFactory GF = new GeometryFactory();

    /**
     * von links unten CCW
     * z.B. fuer Tests
     */
    public static Polygon buildRectangle(double x, double y, double w, double h) {
        Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(x - w / 2, y - h / 2);
        coordinates[1] = new Coordinate(x + w / 2, y - h / 2);
        coordinates[2] = new Coordinate(x + w / 2, y + h / 2);
        coordinates[3] = new Coordinate(x - w / 2, y + h / 2);
        coordinates[4] = coordinates[0];
        GeometryFactory geometryFactory = new GeometryFactory();
        Polygon polygonFromCoordinates = geometryFactory.createPolygon(coordinates);
        return polygonFromCoordinates;
    }


    public static Polygon buildPolygon(SimplePolygonXZ polygon) {
        return JTSConversionUtil.polygonXZToJTSPolygon(polygon);
    }

    /**
     * 14.8.19: Warum überhaupt eine Toleranz? Undwenn, dann doch kleiner als 0.00001. TODO
     *
     * @param c
     * @param coordlist
     * @return
     */
    public static int findVertexIndex(Coordinate c, Coordinate[] coordlist) {
        for (int i = 0; i < coordlist.length; i++) {
            if (coordlist[i].equals2D(c, 0.00001)) {
                return i;
            }
        }
        return -1;
    }

    /*public static int findCoordinate(Coordinate[] coordlist, double x, double y) {
        for (int i=0;i<coordlist.length;i++){
            Coordinate c = coordlist[i];
            if (coordlist[i].equals2D(c,0.00001)){
                return i;
            }
        }
        return -1;
    }*/

    public static int findVertexIndex(Coordinate c, List<Coordinate> coordlist) {
        for (int i = 0; i < coordlist.size(); i++) {
            if (coordlist.get(i).equals2D(c, 0.00001)) {
                return i;
            }
        }
        return -1;
    }

    public static Coordinate findClosest(Coordinate c, Coordinate[] coordlist) {
        return coordlist[findClosestVertexIndex(c, coordlist)];
    }

    public static int findClosestVertexIndex(Coordinate c, Coordinate[] coordlist) {
        return findClosestVertexIndex(c, Arrays.asList(coordlist));
    }

    public static int findClosestVertexIndex(Coordinate c, List<Coordinate> coordlist) {
        double bestdistance = Double.MAX_VALUE;
        int best = -1;
        for (int i = 0; i < coordlist.size(); i++) {
            double distance = coordlist.get(i).distance(c);
            if (distance < bestdistance) {
                bestdistance = distance;
                best = i;
            }
        }
        return best;
    }

    public static int findClosestWithinDistance(Coordinate coordinate, Coordinate[] coors, double maxdistance) {
        int index = JtsUtil.findClosestVertexIndex(coordinate, coors);
        if (coordinate.distance(coors[index]) < maxdistance) {
            return index;
        }
        return -1;
    }

    /**
     * Die Range ermitteln, wo partcandidate auf line liegen. Das kann auch end/start
     * uebergreifend sein, muss aber fortlaufend sein.
     * Beide Richtungen von coors.
     * Exakt, nicht appx.
     * partcandidate muss komplett auf line liegen. Ansonsten gibt es noch getSeam().
     * <p>
     * Returns [from,to];
     * <p>
     * Nicht fertig, vor allem "rundlauf".
     *
     * @return
     */
    public static int[] findCommon(LineString line, LineString partcandidate) {
        int[] result = findCommonForward(line, partcandidate);
        if (result != null) {
            return result;
        }
        //avoid reverse in itself
        List<Coordinate> l = toList(partcandidate.getCoordinates().clone());
        Collections.reverse(l);
        result = findCommonForward(line, createLineFromCoordinates(l));
        return result;
    }

    private static int[] findCommonForward(LineString line, LineString partcandidate) {
        Coordinate[] coors = partcandidate.getCoordinates();
        Coordinate[] lc = line.getCoordinates();
        int startpos;
        boolean started = false, reverse = false;
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < lc.length; i++) {

            if (lc[i].equals2D(coors[0])) {
                started = true;
                startpos = i;
                int remaininglc = lc.length - 1 - i;
                int remaining2 = coors.length - 1;
                int rem = Math.min(remaining2, remaininglc);
                for (int j = 1; j <= rem; j++) {
                    if (!lc[i + j].equals2D(coors[j])) {
                        return null;
                    }
                }
                int endpos = startpos + rem;
                if (remaining2 > remaininglc) {

                    // vorne weitermachen
                    for (int j = 0; j < remaining2 - rem; j++) {
                        if (!lc[j].equals2D(coors[remaining2 - rem + j])) {
                            return null;
                        }
                    }
                    endpos = remaining2 - rem - 1;
                }
                /*for (int j = 1; j < coors.length; j++) {
                    if (lc.length > i + j && !lc[i + j].equals2D(coors[j])) {
                        //over/under flow??. Go back on line
                        int linepos = -1;
                        for (int k = 0; k < coors.length; k++) {
                            linepos = i - k;
                            if (linepos < 0) {
                                linepos = lc.length + linepos;
                            }
                            if (!lc[linepos].equals2D(coors[k])) {
                                //sollte vielleicht doch gueltig sein? Naja?? nicht mehr loggen, weil
                                //die Ausgabe bei Reverse irritiert.Andererseits its das unexpected.
                                logger.warn("findCommon: partcandidate" + partcandidate + " not completely on line " + line);
                                return null;
                            }
                        }
                        return new int[]{linepos, startpos};
                    }
                }*/
                return new int[]{startpos, endpos/*i + coors.length - 1*/};
            }
            /*ausgelagert if (lc[i].equals2D(coors[coors.length - 1])) {
                started = true;
                reverse = true;
                startpos = i;
                for (int j = 1; j < coors.length; j++) {
                    if (!lc[i + j].equals2D(coors[coors.length - 1 - j])) {
                        //over/under flow??. Go back on line
                        int linepos = -1;
                        for (int k = 0; k < coors.length; k++) {
                            linepos = i - k;
                            if (linepos < 0) {
                                linepos = lc.length + linepos;
                            }
                            if (!lc[linepos].equals2D(coors[coors.length - 1 - k])) {
                                logger.error("findCommon no idea:line=" + line + ",partcandidate" + partcandidate);
                                //Util.notyet();
                                return null;
                            }
                        }
                        return new int[]{linepos, startpos};
                    }
                }
                return new int[]{startpos, i + coors.length - 1};
            }*/


        }
        return null;
    }

    /**
     * Exakt? Ja, erstmal. Nee, etwas Toleranz muss schon sein.
     * TODO das ist doch das gleiche wie oben
     *
     * @param c
     * @param coordlist
     * @return
     */
    public static int findCoordinate(Coordinate c, Coordinate[] coordlist) {
        double bestdistance = Double.MAX_VALUE;
        int best = -1;
        for (int i = 0; i < coordlist.length; i++) {
            double distance = coordlist[i].distance(c);
            //Toleranz einfach mal so.
            if (distance < 0.000001) {
                bestdistance = distance;
                best = i;
                return i;
            }
        }
        return best;
    }

    public static Geometry buildFromWKT(String wktString) {
        WKTReader reader = new WKTReader();
        try {
            Geometry geom = reader.read(wktString);
            if (!geom.isValid()) {
                logger.error("invalid WKT Geometry");
            }
            return geom;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Vector3 muss in der z0 Ebene sein.
     *
     * @param v
     * @return
     */
    public static Coordinate buildCoordinateFromZ0(Vector3 v) {
        return new Coordinate(v.getX(), v.getY());
    }

    public static Coordinate buildCoordinateFromZ0(Vector2 v) {
        return new Coordinate(v.getX(), v.getY());
    }

    public static Vector3 buildVector3(Coordinate c) {
        return new Vector3((float) c.x, (float) c.y, (float) c.z);
    }

    public static Coordinate createCoordinate(Vector2 v) {
        return new Coordinate(v.getX(), v.getY());
    }

    public static Vector2 toVector2(Coordinate c) {
        return new Vector2((float) c.x, (float) c.y);
    }

    public static Coordinate toCoordinate(Vector2 v) {
        return new Coordinate(v.x, v.y);
    }

    public static Coordinate toCoordinate(VectorXZ v) {
        return new Coordinate(v.x, v.z);
    }

    public static double getCrossProduct(Vector2D v0, Vector2D v1) {
        return v0.getX() * v1.getY() - v1.getX() * v0.getY();
    }

    /**
     * Return list of triangles or null in case of error (already logged)
     *
     * @param polygon
     * @return
     */
    public static List<Polygon> triangulatePolygonByDelaunay(Polygon polygon, boolean silently) {
        // die Benutzung und die Ergebnisse sind unklar  
        // Trianguliert werden wohl die Points. Der Polygon ist nur als Constraint.

        ConformingDelaunayTriangulationBuilder triangulationBuilder =
                new ConformingDelaunayTriangulationBuilder();

        List<Geometry> constraints =
                new ArrayList<Geometry>(/*1 + holes.size() + segments.size()*/);

        constraints.add(/*polygonXZToJTSPolygon(*/polygon);

        List<Polygon> holes = new ArrayList<>();
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            Polygon hole = GF.createPolygon(polygon.getInteriorRingN(i).getCoordinates());
            constraints.add(hole);
            holes.add(hole);
        }

            /*for (LineSegmentXZ segment : segments) {
                constraints.add(lineSegmentXZToJTSLineString(segment));
            }*/

        ArrayList<Point> jtsPoints = new ArrayList<Point>();
            /*for (VectorXZ p : points) {
                CoordinateSequence coordinateSequence =
                        new CoordinateArraySequence(new Coordinate[] {
                                vectorXZToJTSCoordinate(p)});
                jtsPoints.add(new Point(coordinateSequence, GF));
            }*/
        for (Coordinate p : polygon.getExteriorRing().getCoordinates()) {
            CoordinateSequence coordinateSequence =
                    new CoordinateArraySequence(new Coordinate[]{
                            (p)});
            jtsPoints.add(new Point(coordinateSequence, GF));
        }
        triangulationBuilder.setSites(
                new GeometryCollection(jtsPoints.toArray(new Geometry[0]), GF));
        triangulationBuilder.setConstraints(
                new GeometryCollection(constraints.toArray(new Geometry[0]), GF));
        triangulationBuilder.setTolerance(0.01);
        //es gibt offenbar hier und da ein Rundungsproblem?
        //triangulationBuilder.setTolerance(0.3);

        // run triangulation
        Geometry triangulationResults = null;
        try {
            triangulationResults = triangulationBuilder.getTriangles(GF);
        } catch (ConstraintEnforcementException e) {
            int holecnt = polygon.getNumInteriorRing();
            if (!silently) {
                String details = "" + holecnt + " holes," + polygon.getExteriorRing().getCoordinates().length + " points";
                logger.error("getTriangles() failed(" + details + "):" + e);
                logger.error("Polygon: " + toWKT(polygon));
            }
            return null;
        }
        // interpret the resulting polygons as triangles, filter out those which are outside the polygon or in a hole 

            /*Collection<PolygonWithHolesXZ> trianglesAsPolygons =
                    polygonsXZFromJTSGeometry(triangulationResult);

            List<TriangleXZ> triangles = new ArrayList<TriangleXZ>();

            for (PolygonWithHolesXZ triangleAsPolygon : trianglesAsPolygons) {

                boolean triangleInHole = false;
                for (Polygon hole : holes) {
                    if (hole.contains(triangleAsPolygon.getOuter().getCenter())) {
                        triangleInHole = true;
                        break;
                    }
                }

                if (!triangleInHole && polygon.contains(
                        triangleAsPolygon.getOuter().getCenter())) { //TODO: create single method for this query within PolygonWithHoles

                    triangles.add(triangleAsPolygon.asTriangleXZ());

                }

            }*/


        List<Polygon> triangles = new ArrayList<Polygon>();
        int trianglecnt = triangulationResults.getNumGeometries();
        for (int i = 0; i < trianglecnt; i++) {
            Geometry geo = triangulationResults.getGeometryN(i);
            triangles.add((Polygon) geo);
        }
        triangles = removeTrianglesInHolesAndOutside(polygon, holes, triangles);
        return triangles;
    }

    public static String toWKT(Polygon polygon) {
        WKTWriter w = new WKTWriter();
        return w.write(polygon);
    }

    public static Polygon createTriangle(Coordinate c0, Coordinate c1, Coordinate c2) {
        return GF.createPolygon(new Coordinate[]{c0, c1, c2, c0});
    }

    public static LineString createLine(Coordinate c0, Coordinate c1) {
        return createLine(new Coordinate[]{c0, c1});
    }

    /**
     * Returns null on error.
     */
    public static LineString createLine(Coordinate[] c) {
        try {
            return GF.createLineString(c);
        } catch (Exception e) {
            logger.error("createLine failed:" + e.getMessage());
        }
        return null;
    }

    /**
     * Returns null on error.
     */
    public static LineString createLine(List<Vector2> vlist) {
        Coordinate[] c = new Coordinate[vlist.size()];
        int i = 0;
        for (Vector2 v : vlist) {
            c[i++] = (buildCoordinateFromZ0(v));
        }
        return createLine(c);
    }

    /**
     * Returns null on error.
     *
     * @param vlist
     * @return
     */
    public static LineString createLineFromCoordinates(List<Coordinate> vlist) {
        Coordinate[] c = new Coordinate[vlist.size()];
        int i = 0;
        for (Coordinate v : vlist) {
            c[i++] = v;
        }
        try {
            return GF.createLineString(c);
        } catch (Exception e) {
            logger.error("createLineFromCoordinates failed:" + e.getMessage());
        }
        return null;
    }


    public static List<Vector2> createLine(LineString line) {
        Coordinate[] c = line.getCoordinates();
        List<Vector2> vlist = new ArrayList<>();
        int i = 0;
        for (Coordinate v : c) {
            vlist.add(toVector2(v));
        }
        return vlist;
    }

    public static LineSegment createLineSegment(Coordinate c0, Coordinate c1) {
        return new LineSegment(c0, c1);
    }

    /**
     * negative extension extends at p0, positive at c1.
     *
     * @param line
     * @param extension
     * @return
     */
    public static LineSegment extendLineSegment(LineSegment line, double extension) {
        //per vector solution appears easier
        Vector2 p0 = toVector2(line.p0);
        Vector2 p1 = toVector2(line.p1);
        Vector2 v = p1.subtract(p0);
        Vector2 n = v.normalize();
        if (extension > 0) {
            v = v.add(n.scale((float) extension));
            return new LineSegment(line.p0, createCoordinate(p0.add(v)));
        } else {
            v = v.negate().add(n.scale((float) extension));
            return new LineSegment(createCoordinate(p1.add(v)), line.p1);
        }
    }

    /**
     * extends at both ends
     *
     * @param line
     * @param extension
     * @return
     */
    public static LineSegment extendLineSegment2(LineSegment line, double extension) {
        line = extendLineSegment(line, -extension);
        line = extendLineSegment(line, extension);
        return line;
    }

    public static List<LineSegment> buildLineSegmentList(Coordinate[] coors) {
        List<LineSegment> lines = new ArrayList<>();
        for (int i = 1; i < coors.length; i++) {
            lines.add(new LineSegment(coors[i - 1], coors[i]));
        }
        return lines;
    }

    /**
     * Holes are getFirst integrated into polygon. Smart.
     * Das kann aber seeehr langsam sein, wenn viele Holes aufzulösen sind.
     *
     * @param polygon
     * @return
     */
    public static List<Polygon> triangulatePolygonByEarClippingRespectingHoles(Polygon polygon) {
        SimplePolygonXZ outerPolygon;
        Collection<SimplePolygonXZ> holes = new ArrayList();

        List<Polygon> triangles = new ArrayList<Polygon>();

        try {
            outerPolygon = JTSConversionUtil.polygonXZFromLineString(polygon.getExteriorRing());
            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                holes.add(JTSConversionUtil.polygonXZFromLineString(polygon.getInteriorRingN(i)));
            }
            List<TriangleXZ> trianglesXZ = EarClippingTriangulationUtil.triangulate(outerPolygon, holes);
            for (int i = 0; i < trianglesXZ.size(); i++) {
                TriangleXZ tri = trianglesXZ.get(i);
                triangles.add(JTSConversionUtil.polygonXZToJTSPolygon(new SimplePolygonXZ(tri.getVertexList())));
            }
        } catch (Exception e) {
            logger.error("triangluation failed: " + e.getMessage());
            logger.error("Polygon: " + toWKT(polygon));
            return null;
        }

        List<Polygon> pholes = new ArrayList<>();
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            Polygon hole = GF.createPolygon(polygon.getInteriorRingN(i).getCoordinates());
            pholes.add(hole);
        }
        triangles = removeTrianglesInHolesAndOutside(polygon, pholes, triangles);
        return triangles;
    }


    public static boolean isIntersecting(LineString line, List<Polygon> polygons) {
        for (Polygon p : polygons) {
            //doc recommends covers over contains esspecially for boundaries
            //geht trotzdem nicht
            if (line.crosses(p)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIntersectingLine(LineString line, List<LineString> lines) {
        for (LineString p : lines) {
            //doc recommends covers over contains esspecially for boundaries
            //geht trotzdem nicht
            if (line.crosses(p)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIntersectingLine(LineSegment line, List<LineSegment> lines) {
        for (LineSegment p : lines) {
            if (line.intersection(p) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Geht nur mit exakt einer Intersection, sonst kommt null.
     *
     * @return
     */
    /*nicht fertig public static Coordinate getSingleIntersection(LineString line, Polygon polygon) {
            Geometry intersection = line.intersection(polygon.getExteriorRing());

        return false;
    }*/
    public static List<Polygon> removeTrianglesInHolesAndOutside(Polygon polygon, List<Polygon> holes, List<Polygon> alltriangles) {
        List<Polygon> triangles = new ArrayList<Polygon>();
        int trianglecnt = alltriangles.size();
        for (int i = 0; i < trianglecnt; i++) {
            Polygon geo = alltriangles.get(i);
            boolean triangleInHole = false;
            for (Polygon hole : holes) {
                if (hole.contains(geo.getCentroid())) {
                    triangleInHole = true;
                    break;
                }
            }

            if (!triangleInHole && polygon.contains(
                    geo.getCentroid())) {

                triangles.add(geo);

            }

        }
        return triangles;
    }

    /**
     * aufsteigend
     *
     * @param lines
     */
    public static void sortByLength(List<LineSegment> lines) {
        Collections.sort(lines, new Comparator<LineSegment>() {
            @Override
            // 19.4.19: Einfach
            public int compare(LineSegment o1, LineSegment o2) {
                return (int) (o1.getLength() - o2.getLength());
            }
        });
    }

    /**
     * aufsteigend, d.h. Element 0 ist am naechsten
     */
    public static void sortByDistance(List<Coordinate> coors, Coordinate ref) {
        Collections.sort(coors, new Comparator<Coordinate>() {
            @Override
            // 19.4.19: Einfach return (int) (o1.distance(ref) - o2.distance(ref)) läuft hier und da auf Fehler
            public int compare(Coordinate o1, Coordinate o2) {
                return (o1.distance(ref) < o2.distance(ref)) ? -1 : 1;
            }
        });
    }

    /**
     * 10.8.18: Ein Hole durch zerschneiden des Polygon in zwei Teile erzeugen.
     * <p>
     * Neu: Kürzeste Verbindungen etwa gegenüber, dann zwei neue bilden.
     * <p>
     * Returns two new polygons or null in case of error.
     *
     * @param polygon
     * @return
     */
    public static Polygon[] removeHoleFromPolygonBySplitting(Polygon polygon) {
        /*keine Ahnung wofuer das war Coordinate[] coor = polygon.getCoordinates();
        Coordinate p0 = coor[0];

        for (int index = 2; index < coor.length - 2; index++) {

            Coordinate p1 = coor[index];


            LineString g = GF.createLineString(new Coordinate[]{p0, p1});
            if (polygon.contains(g)) {
                Coordinate[] poly0c = new Coordinate[index + 2];
                for (int i = 0; i <= index; i++) {
                    poly0c[i] = coor[i];
                }
                poly0c[index + 1] = coor[0];

                Coordinate[] poly1c = new Coordinate[coor.length - index + 1];
                for (int i = 0; i < coor.length - index; i++) {
                    poly1c[i] = coor[index + i];
                }
                poly1c[poly1c.length - 1] = coor[index];
                return new Polygon[]{GF.createPolygon(poly0c), GF.createPolygon(poly1c)};
            }
        }
        return null;*/
        List<LineSegment> conns0 = getOutConnections(polygon, 0);
        sortByLength(conns0);
        // mal annehmen das das ungefaehr gegenueber ist
        List<LineSegment> connsopposite = getOutConnections(polygon, polygon.getInteriorRingN(0).getCoordinates().length / 2);
        sortByLength(connsopposite);
        CoordinateList pouter = new CoordinateList(polygon.getExteriorRing().getCoordinates());
        CoordinateList chole = new CoordinateList(polygon.getInteriorRingN(0).getCoordinates());
        //choose two lines not intersecting 
        LineSegment line0 = null;
        LineSegment line1 = null;

        boolean foundpair = false;
        for (int i = 0; i < conns0.size(); i++) {
            line0 = conns0.get(i);
            for (int j = 0; j < connsopposite.size(); j++) {
                line1 = connsopposite.get(j);
                if (line0.intersection(line1) == null) {
                    i = conns0.size();
                    foundpair = true;
                    break;
                }
            }
        }
        if (!foundpair) {
            logger.warn("no pair found");
            return null;
        }
        //pouter.removeLast();
        // an den Punkten der beiden kuerzesten aufteilen.
        //p0 are expected to be outer, p1 hole coordinates
        CoordinateList[] outerparts = pouter.splitByPoints(line0.p0, line1.p0);
        CoordinateList[] innerparts = chole.splitByPoints(line0.p1, line1.p1);
        // Jetzt ausprobieren
        Polygon p0 = createPolygonFromCoordinateLists(outerparts[0], innerparts[0]);
        Polygon p1 = createPolygonFromCoordinateLists(outerparts[1], innerparts[1]);
        if (p0 == null || p1 == null) {
            logger.warn("Couldn't split polygon:" + toWKT(polygon));
            return null;
        }
        if (p0.crosses(p1)) {
            // dann muss es anders kombiniert werden
            p0 = createPolygonFromCoordinateLists(outerparts[0], innerparts[1]);
            p1 = createPolygonFromCoordinateLists(outerparts[1], innerparts[0]);

        }
        return new Polygon[]{p0, p1};
    }

    public static List<LineSegment> getPossibleSplitConnections(Polygon polygon) {
        List<LineSegment> conns = new ArrayList<>();
        // Ich gehe alle Punkte durch
        Coordinate[] coors = polygon.getExteriorRing().getCoordinates();
        // last coor isType dup of getFirst
        for (int i = 0; i < coors.length - 1; i++) {
            conns.addAll(getPossibleConnections(polygon, coors[i]));
        }
        return conns;
    }

    /**
     * 18.8.18: Ein Polygon durch zerschneiden des Polygon in zwei Teile zerlegen.
     * Holes bleiben erhalten, entweder in der einen oder anderen Haelfte.
     * <p>
     * 20.8.18: Deprecated, weil die Nutzung zur Vereinfachung von entarteten Polygonen zu sehr auf dem Zufall basiert.
     * <p>
     * Returns two new polygons or null in case of error.
     *
     * @param polygon
     * @return
     */
    @Deprecated
    public static Polygon[] splitPolygon(Polygon polygon) {
        List<LineSegment> conns = getPossibleSplitConnections(polygon);
        sortByLength(conns);
        CoordinateList pouter = new CoordinateList(polygon.getExteriorRing().getCoordinates());
        LineSegment splitline = conns.get(0);
        //pouter.removeLast();
        // an den Punkten der beiden kuerzesten aufteilen.
        //p0 are expected to be outer, p1 hole coordinates
        CoordinateList[] outerparts = pouter.splitByPoints(splitline.p0, splitline.p1);
        // Jetzt ausprobieren
        outerparts[0].add(outerparts[0].get(0));
        outerparts[1].add(outerparts[1].get(0));
        Polygon p0 = createPolygonFromCoordinateList(outerparts[0], true);
        Polygon p1 = createPolygonFromCoordinateList(outerparts[1], true);
        if (p0 == null || p1 == null) {
            logger.warn("Couldn't split polygon:" + toWKT(polygon));
            return null;
        }
        if (p0.crosses(p1)) {
            logger.warn("crossing?");
        }
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            //logger.error("difference for holes not working");
            //if (true) return null;
            LineString hole = polygon.getInteriorRingN(i);
            if (p0.contains(hole)) {
                Polygon phole = GF.createPolygon(hole.getCoordinates());
                p0 = (Polygon) p0.difference(phole);
            } else if (p1.contains(hole)) {
                p1 = (Polygon) p1.difference(GF.createPolygon(hole.getCoordinates()));
            } else {
                logger.warn("hole neither in p0 nor p1");
            }
        }

        return new Polygon[]{p0, p1};
    }

    public static Polygon createPolygon(Shape shape) {
        if (!shape.isClosed()) {
            logger.warn("shape not closed. Ignoring.");
            return null;
        }
        Coordinate[] c = new Coordinate[shape.getPoints().size() + 1];
        int i = 0;
        for (Vector2 v : shape.getPoints()) {
            c[i++] = (buildCoordinateFromZ0(v));
        }
        c[i] = buildCoordinateFromZ0(shape.getPoints().get(0));
        return GF.createPolygon(c);
    }

    /**
     * Aus ein oder zwei Coordinateketten(Listen) durch verbinden der Eckpunkte ein Polygon erstellen.
     * Bei zweien einfach die beiden Kombinationen probieren, einen gültigen Polygon zu bekommen.
     *
     * @param part0
     * @param part1
     * @return
     */
    public static Polygon createPolygonFromCoordinateLists(CoordinateList part0, CoordinateList part1) {
        CoordinateList l;
        Polygon p;


        l = part0.join(part1);
        l.add(l.get(0));
        p = createPolygonFromCoordinateList(l, true);
        if (p != null && p.isValid()) {
            return p;
        }
        Collections.reverse(part1.coorlist);
        l = part0.join(part1);
        l.add(l.get(0));
        p = createPolygonFromCoordinateList(l, true);
        if (p != null && p.isValid()) {
            return p;
        }
        logger.warn("cannot build valid polygon");
        return null;
    }

    /**
     * Aus ein oder zwei LineSegments durch verbinden der Eckpunkte ein Polygon erstellen.
     * Bei zweien einfach die beiden Kombinationen probieren, einen gültigen Polygon zu bekommen.
     */
    public static Polygon createPolygonFromLines(LineSegment s0, LineSegment s1) {
        return createPolygonFromCoordinateLists(new CoordinateList(s0), new CoordinateList(s1));
    }

    /**
     * Used for probing also, so option silently
     *
     * @param l
     * @param silently
     * @return
     */
    public static Polygon createPolygonFromCoordinateList(CoordinateList l, boolean silently) {
        //29.7.19: Das ist doch Driss und kaschiert Probleme l.ensureClosed();
        try {
            LinearRing linearRing = GF.createLinearRing(l.toArray());
            Polygon p = GF.createPolygon(linearRing);
            if (!p.isValid()) {
                // such a polygon isType useless
                if (!silently) {
                    logger.warn("created polygon isType not valid. Discarding: LinearRing=" + linearRing);
                }
                return null;
            }
            return p;
        } catch (IllegalArgumentException e) {
            logger.debug("createPolygonFromCoordinateList failed: " + e.getMessage());
            return null;
        }
    }

    public static Polygon createPolygonFromWayOutlines(CoordinateList rightline, CoordinateList leftline) {
        try {
            boolean isClosed = rightline.get(0).equals2D(rightline.get(rightline.size() - 1));
            if (isClosed) {
                Polygon polygon = createPolygon(rightline.coorlist);
                Polygon hole = createPolygon(leftline.coorlist);
                polygon = (Polygon) polygon.difference(hole);
                return polygon;
            }
            List<Coordinate> total = new ArrayList<>();
            total.addAll(rightline.coorlist);
            total.addAll(leftline.reverse().coorlist);
            total.add(total.get(0));
            Polygon polygon = createPolygon(total);
            return polygon;

        } catch (Exception e) {
            //Das wird schon gelegentlich passieren, z.B. beim Anlegen der inner connector
            logger.debug("createPolygonFromCoordinateList failed: " + e.getMessage());
            return null;
        }
    }

    public static Polygon createEmptyPolygon() {
        return GF.createPolygon(new Coordinate[]{});
    }

    /**
     * Fuer Polygone mit genau einem Hole.
     * Liefert eine Liste der moeglichen  Lines von einem Punkt des Hole in den Exterior.
     * Die Menge der Lines liegt komplett im Polygon und nicht im Hole.
     * Die Lines sind immer von exterior Richtung hole.
     *
     * @param polygon
     */
    public static List<LineSegment> getOutConnections(Polygon polygon, int holecoordinateindex) {
        List<LineSegment> lines = new ArrayList<>();

        //Coordinate[] coors = polygon.getExteriorRing().getCoordinates();
        Coordinate startpoint = polygon.getInteriorRingN(0).getCoordinates()[holecoordinateindex];
        lines = getPossibleConnections(polygon, startpoint);
        return lines;
    }

    /**
     * Liefert eine Liste aller moeglichen Verbindungen der Coordinates eines Polygon zu irgendeiner
     * bestimmten Coorinate.
     * Stellt sicher, dass es keine Line ausserhalb, keine auf der Boundary
     * und keine ueber ein Hole ist. Somit muss destination
     * zumindest auf der Boundary des Polygon liegen, damit was gefunden wird.
     *
     * @return
     */
    public static List<LineSegment> getPossibleConnections(Polygon polygon, Coordinate destination) {
        List<LineSegment> lines = new ArrayList<>();

        Coordinate[] coors = polygon.getExteriorRing().getCoordinates();
        Coordinate before = coors[coors.length - 2], after = null;
        // last coor isType dup of getFirst
        for (int i = 0; i < coors.length - 1; i++) {
            Coordinate c = coors[i];
            after = coors[i + 1];
//LineSegment line = new LineSegment(startpoint,c);
            if (!destination.equals(c) && !destination.equals(before) && !destination.equals(after)) {
                LineString line = createLine(c, destination);
                // coveredby stellt sicher, dass es keine Line ausserhalb und keine ueber ein Hole ist.
                if (line.coveredBy(polygon)) {
                    lines.add(new LineSegment(c, destination));
                }
            }
            before = c;
        }
        return lines;
    }

    /**
     * Ein Hole durch umfliessen durch das Gesamtpolygon zu entfernmen versuchen.
     * Skizze 61
     * <p>
     * Das wird einen  Self-Touching Rings to for geben, der wohl nicht valid ist.
     * TODO unfertig geht nicht
     *
     * @param polygon
     * @return
     */
    public static Polygon removeHoleFromPolygonUnfertig(Polygon polygon) {
        Coordinate[] coor = polygon.getExteriorRing().getCoordinates();

        LineString hole = polygon.getInteriorRingN(0);
        Coordinate start = coor[coor.length - 1];
        Coordinate[] holecoor = hole.getCoordinates();
        int closest = findClosestVertexIndex(start, holecoor);
        //CoordinateArraySequence cc = new CoordinateArraySequence(coor);
        //cc.LinearRing ring = new LinearRing(coor);
        List<Coordinate> clist = new ArrayList(Arrays.asList(coor));
        for (int i = closest; i < holecoor.length; i++) {
            clist.add(holecoor[i]);
        }
        for (int i = 0; i < closest; i++) {
            clist.add(holecoor[i]);
        }
        clist.add(coor[0]);
        return createPolygon(clist);
    }

    /**
     * Ohne jede Pruefung.
     * 9.8.19: Doch mit Prüfung, sonst gibt es eine IllegalArgumentException. Über das Log koennen wir noch diskutieren.
     *
     * @param clist
     * @return
     */
    public static Polygon createPolygon(List<Coordinate> clist) {
        try {
            return GF.createPolygon(clist.toArray(new Coordinate[0]));
        } catch (IllegalArgumentException e) {
            logger.error("createPolygon failed" + e.getMessage());
        }
        return null;
    }

    /**
     * Returns null if polygon isType invalid.
     */
    public static Polygon createPolygon(Coordinate[] clist) {
        Polygon p = GF.createPolygon(clist);
        if (!p.isValid()) {
            return null;
        }
        return p;
    }

    /**
     * irgendwie tricky. Mit coveredBy geht es nicht zuverlaessig.
     *
     * @param c
     * @param polygon
     * @return
     */
    public static boolean onBoundary(Coordinate c, Polygon polygon) {
        /*Point p = GF.createPoint(c);
        if (p.coveredBy(polygon.getExteriorRing())) {
            return true;
        }
        return false;*/
        return getBoundaryLine(c, polygon) != null;
    }

    /**
     * @param c
     * @return
     */
    public static boolean onLine(Coordinate c, List<LineSegment> lines) {
        return getCoveringLine(c, lines) != -1;
    }

    /**
     * Die Kanten eines Polygon ermitteln, auf der eine Coordinate liegt.
     * irgendwie tricky. Mit coveredBy geht es nicht zuverlaessig.
     * Bei Knotenpunkten können es auch zwei Kanten sein. Die Reihenfolge ist dann "von from nach to", mit coordinate am ende des ersten und beginn des zweiten.
     *
     * @param c
     * @param polygon
     * @return
     */
    public static LineSegment[] getBoundaryLine(Coordinate c, Polygon polygon) {
        LineSegment firstresult = null;
        //Point p = GF.createPoint(c);
        LineSegment[] segs = toLineSegments(polygon);
        if (segs == null) {
            //already logged
            return null;
        }
        for (LineSegment s : segs) {
            if (onLine(c, s)) {
                if (firstresult != null) {
                    return new LineSegment[]{firstresult, s};
                }
                firstresult = s;
            }
        }
        if (firstresult != null) {
            return new LineSegment[]{firstresult};
        }
        return null;
    }

    /**
     * irgendwie tricky. Mit coveredBy geht es nicht zuverlaessig.
     * Bei Knotenpunkten können es auch zwei Kanten sein. Es wird aber nur die erste gefundene geliefert.
     */
    public static int getCoveringLine(Coordinate c, List<LineSegment> segs) {
        for (int i = 0; i < segs.size(); i++) {
            LineSegment s = segs.get(i);
            if (onLine(c, s)) {
                return i;
            }
        }
        return -1;
    }

    public static int getCoveringLine(Coordinate c, Polygon polygon) {
        LineSegment[] segs = toLineSegments(polygon);
        if (segs == null) {
            //already logged
            return -1;
        }
        return getCoveringLine(c, Arrays.asList(segs));
    }

    public static boolean onLine(Coordinate c, LineSegment line) {
        //TODO Toleranz verkleinern?
        return line.distance(c) < 0.0001;
    }

    public static LineSegment[] toLineSegments(Polygon polygon) {
        return toLineSegments(polygon.getExteriorRing());
    }

    public static LineSegment[] toLineSegments(LineString polygon) {
        Coordinate[] coors = polygon.getCoordinates();
        return toLineSegments(coors);
    }

    public static LineSegment[] toLineSegments(Coordinate[] coors) {
        if (coors.length < 2) {
            logger.warn("inconsistent polygon with " + coors.length + " coordinates?");
            return null;
        }
        LineSegment[] segs = new LineSegment[coors.length - 1];
        for (int i = 0; i < coors.length - 1; i++) {
            segs[i] = createLineSegment(coors[i], coors[i + 1]);
        }
        return segs;
    }

    /**
     * Check whether s1 lies on s0.
     *
     * @param s0
     * @param s1
     * @return
     */
    public static boolean covers(LineSegment s0, LineSegment s1) {
        if (!onLine(s1.p0, s0)) {
            return false;
        }
        if (!onLine(s1.p1, s0)) {
            return false;
        }
        return true;
    }

    /**
     * Inwiefern das Points on Boundary abdeckt, ist unsicher.
     * Wahrscheinlich gilt aber auf der Boundary auch als Covered und damit als Teil des Polygon.
     *
     * @return
     */
    public static boolean isPartOfPolygon(Coordinate c, Polygon polygon) {
        Point p = GF.createPoint(c);
        //getExteriorRing und getBoundary kann man nicht einfach direkt nehmen
        Polygon ohneholes = GF.createPolygon(polygon.getExteriorRing().getCoordinates());
        if (p.coveredBy(ohneholes)) {
            return true;
        }
        return false;
    }

    /**
     * Liefert p0 ohne die Bereiche von p1, also p0-p1.
     * Wenn nichts von p1 ausserhalb von p0 liegt, passiert das evtl. "einfach" durch einfügen eines Hole.
     * <p>
     * Konvertiert die möglichen verschiedenen Resultate einheitlich in eine Polygon List.
     * <p>
     * Wenn p1 komplett ausserhalb liegt (kein Overlap) wird entgegen mathematischen Regeln null returned.
     * <p>
     * 25.04.19
     *
     * @return
     */
    public static List<PolygonSubtractResult> subtractPolygons(Polygon p0, Polygon p1) {
        List<PolygonSubtractResult> result = new ArrayList<>();
        Geometry diff = JtsUtil.difference(p0, p1);
        if (diff != null) {
            if (diff instanceof Polygon) {
                //4.9.19:Die Ermittlung der
                List<Coordinate> seam = getSeam((Polygon) diff, p1);
                if (Math.abs(diff.getArea() - p0.getArea()) < 0.0001) {
                    return null;
                }
                result.add(new PolygonSubtractResult((Polygon) diff, seam));
            } else if (diff instanceof MultiPolygon) {
                MultiPolygon mp = (MultiPolygon) diff;
                for (int i = 0; i < mp.getNumGeometries(); i++) {
                    Polygon diffpart = (Polygon) mp.getGeometryN(i);
                    List<Coordinate> seam = getSeam((Polygon) diffpart, p1);

                    result.add(new PolygonSubtractResult(diffpart, seam));
                }
            } else if (diff instanceof GeometryCollection) {
                //18.4.19: sowas gibts auch. Dann kann da ein LineString drinsein.
                //Bestimmt weil etwas exakt uebreinander liegt. Reichlich doof. Was heisst das wohl?
                GeometryCollection gc = (GeometryCollection) diff;
                for (int i = 0; i < gc.getNumGeometries(); i++) {
                    Geometry sg = gc.getGeometryN(i);
                    if (sg instanceof Polygon) {
                        result.add(new PolygonSubtractResult((Polygon) sg));
                    } else {
                        logger.warn("Ignoring difference polygon " + sg);
                    }
                }
            } else {
                logger.error("unknown background difference type. skipped.: " + diff.getClass().getName());
            }
        }
        return result;
    }

    /**
     * Die Verbindungs Line(s) zwischen den beiden Polygonen ermitteln. Bzw. den Linstring in p0, den es auch in p1 gibt.
     * per intersection können dabei auch sehr(1) schmale Polygone rauskommen. Unguenstig. Darum per Logik.
     * Diese Methode dürfte nur funktionieren, wenn die PArameter Resultat eines difference sind.
     * <p>
     * Es werden die exakten Coordinates von p0 (diff result) auf Lage auf p1 geprueft.
     * 4.9.19: Wegen der Komplexitaet eigentlich keine Seam(LineString) liefern, sondern alle Punkte "mit Kontakt".
     *
     * @return
     */
    public static List<Coordinate>/*LineString[]*/ getSeam(Polygon p0, Polygon p1) {
        //return intersection(diffpart,p1);
        List<Coordinate> common = new ArrayList<>();
        Coordinate[] coors0 = p0.getCoordinates();
        Coordinate[] coors1 = p1.getCoordinates();
        List<LineString> result = new ArrayList<>();

        int startedAt = -1;
        for (int i = 0; i < coors0.length; i++) {
            //der muss ja nicht unbedingt als Coordinate existieren, sondern kann auch nur auf Boundary liegen.
            if (onBoundary(coors0[i], p1)) {
               /* if (startedAt == -1) {
                    startedAt = i;
                }else{
                    if (i > startedAt+1){

                        result.add(createLineFromCoordinates(common));
                    }
                }*/
                common.add(coors0[i]);
            }
        }
        /*if (common.size() < 2) {
            return null;
        }*/
        return common;//createLineFromCoordinates(common);
    }

    private static List<LineString> toLineStringList(Geometry seam) {
        if (seam == null) {
            return null;

        }
        List<LineString> result = new ArrayList<>();
        if (seam instanceof MultiLineString) {
            MultiLineString mls = (MultiLineString) seam;
            for (int i = 0; i < mls.getNumGeometries(); i++) {
                result.add((LineString) mls.getGeometryN(i));
            }
            return result;
        }
        logger.error("unknown geometry");
        return null;
    }

    /**
     * Liefert p0 ohne die Bereiche von p1, also
     * p0-p1.
     * <p>
     * Es gibt einfach mal Fehler bei difference.
     * <p>
     * 02.04.19
     *
     * @return
     */
    public static Geometry difference(Geometry p0, Geometry p1) {
        try {
            return p0.difference(p1);
        } catch (TopologyException e) {
            logger.error("TopologyException in difference: p0=" + p0 + ", p1=" + p1);
            return null;
        }
    }


    /**
     * Der uebergebene Way Polygon ist counterclockwise. Begonnen wird "links/oben", weil
     * die TexCoordFunction so davon ausgeht.
     * 11.4.19
     * <p>
     * Ob es hier gut liegt? TextureUtil ist aber schlechter.
     * In VertexData gibts was aehnliches.
     *
     * @param coord
     * @return
     */
    public static VertexData createTriangleStripForPolygon(Coordinate[] coord, CoordinateList rightline, CoordinateList leftline) {
        List<Coordinate> vertices = new ArrayList<>();
        int trianglecnt;
        if (coord != null) {
            if (coord.length < 4 || coord.length % 2 == 0) {
                logger.error("not a triangle strip. length=" + coord.length);
                return null;
            }
            int len = coord.length / 2;
            trianglecnt = (len - 1) * 2;
            // List<Polygon> triangles = new ArrayList<>();

            //29.4.19: Nicht ueber JtsUtil.createTriangle() und TextureUtil.buildIndexes(), denn die ordnen
            //die Vertices evtl. um, so dass das UV Mapping nicht mehr passt. Die CCW Order duerfte auch
            //hier erreicht werden.
            for (int i = 0; i < len; i++) {
                vertices.add(coord[2 * len - i - 1]);
                vertices.add(coord[i]);
            }
        } else {
            if (leftline.size() != rightline.size() || rightline.size() < 2) {
                logger.error("not a triangle strip. length=");
                return null;
            }
            trianglecnt = (leftline.size() - 1) * 2;
            for (int i = 0; i < leftline.size(); i++) {
                vertices.add(leftline.get(i));
                vertices.add(rightline.get(i));
            }
        }

        //for (int i = 0; i < (trianglecnt / 2); i++) {
        //    triangles.add(JtsUtil.createTriangle(coord[2 * len - i - 1], coord[i], coord[i + 1]));
        //    triangles.add(JtsUtil.createTriangle(coord[2 * len - i - 2], coord[2 * len - i - 1], coord[i + 1]));
        //}

        int[] indices = new int[trianglecnt * 3];
        //List<Coordinate> vertices = new ArrayList<>();

        //indices = TextureUtil.buildIndexes(triangles, vertices);
        int offset = 0;
        for (int i = 0; i < (trianglecnt / 2) * 6; i += 6) {
            indices[i] = offset + 0;
            indices[i + 1] = offset + 1;
            indices[i + 2] = offset + 2;
            indices[i + 3] = offset + 2;
            indices[i + 4] = offset + 1;
            indices[i + 5] = offset + 3;
            //offset 2 ist richtig, weil die Borgaenger ja wiederverwendet werden.
            offset += 2;
        }
        if (vertices.size() % 2 == 1) {
            logger.error("not a triangle strip ");
            return null;
        }
        return new VertexData(vertices, indices, null);
    }

    public static boolean contains(Geometry geometry, Vector2 point) {
        Coordinate c = toCoordinate(point);
        return geometry.contains(GF.createPoint(c));
    }

    /**
     * Fuer Analysezwecke.
     * 4.6.19: z mit precision 2 wegen overlay.
     *
     * @return
     */
    public static String toRoundedString(Coordinate c) {
        String s = "(";
        s += String.format("%4.1f", c.x).replaceAll(",", ".") + ",";
        s += String.format("%4.1f", c.y).replaceAll(",", ".") + ",";
        s += String.format("%4.2f", c.z).replaceAll(",", ".") + ")";
        return s;
    }

    /**
     * Implementierung eines etwas weniger formalen aber praktikablen overlaps. Naja.
     * <p>
     * zu intersects,touches und overlap: http://docs.geotools.org/stable/userguide/library/jts/relate.html
     * Die JTS Logik ist etwas speziell. Und tricky wegen Rundungen?
     *
     * @return
     */
    public static Boolean overlaps(Polygon p1, Polygon p2) {
        //exceptions might happen
        Geometry intersection = intersection(p1, p2);
        if (intersection == null) {
            //doesn't know better
            return null;
        }
        //Welcher Grenzwert guenstig ist, ist unklar. "1" ist jedenfalls zu gross.
        return intersection.getArea() > 0.00001;
        /*
        if (p1.within(p2)) {
            return true;
        }
        if (p2.within(p1)) {
            return true;
        }

        if (p1.overlaps(p2) && !p1.touches(p2)) {
            return true;
        }
        return false;*/
    }

    public static Geometry intersection(Geometry/*Polygon*/ p0, Geometry/*Polygon*/ p1) {
        try {
            //exceptions just might happen
            return p0.intersection(p1);
        } catch (TopologyException e) {
            logger.error("TopologyException in difference: p0=" + p0 + ", p1=" + p1);
            return null;
        }

    }

    public static Vector2 getDirection(Coordinate from, Coordinate to) {
        return getVector2(from, to).normalize();
    }

    public static Vector2 getVector2(Coordinate from, Coordinate to) {
        return toVector2(to).subtract(toVector2(from));
    }

    public static Coordinate add(Coordinate c, Vector2 v) {
        return new Coordinate(c.x + v.x, c.y + v.y, c.z);
    }

    /**
     * Einzelne Coordinates eines Polygon nach innen/aussen verschieben und damit den Polygon
     * teilweise vergrößern oder verkleinern.
     * <p>
     * Returns null if resulting polygon isType invalid.
     */
    public static Polygon createResizedPolygon(Polygon polygon, List<Coordinate> coordinates, double offset) {
        Vector2[] offsets = new Vector2[coordinates.size()];
        Vector2[] normals = new Vector2[coordinates.size()];
        for (int j = 0; j < coordinates.size(); j++) {
            // Normal points outside, so negate.
            if ((normals[j] = JtsUtil.getNormalAtCoordinate(polygon, coordinates.get(j))) == null) {
                return null;
            }
            offsets[j] = normals[j].negate().multiply(offset);

        }
        Polygon newp = JtsUtil.createResizedPolygon(polygon, coordinates, offsets);
        return newp;
    }

    /**
     * Returns null if polygon isType invalid.
     */
    public static Polygon createResizedPolygon(Polygon polygon, List<Coordinate> coordinates, Vector2[] offsets) {
        Coordinate[] coors = polygon.getCoordinates();
        for (int i = 0; i < coordinates.size(); i++) {
            int index = findCoordinate(coordinates.get(i), coors);
            if (index != -1) {
                coors[index] = moveCoordinate(coors[index], offsets[i]);
            }
        }
        return createPolygon(coors);
    }

    public static Coordinate moveCoordinate(Coordinate c, Vector2 offset) {
        return new Coordinate(c.x + offset.x, c.y + offset.y, c.z);

    }

    public static Coordinate getOutlinePointFromDirections(LineSegment s0, LineSegment s1, double offset) {
        Vector2 dir0 = getDirection(s0.p0, s0.p1);
        Vector2 dir1 = getDirection(s1.p0, s1.p1);
        Vector2 node = toVector2(s0.p1);
        Coordinate c = toCoordinate(OutlineBuilder.getOutlinePointFromDirections(dir0, node, dir1, offset));
        return c;

    }

    public static Vector2 getEffectiveDirection(LineSegment s0, LineSegment s1) {
        Vector2 dir0 = getDirection(s0.p0, s0.p1);
        Vector2 dir1 = getDirection(s1.p0, s1.p1);
        Vector2 effectivedir;
        effectivedir = dir0.add(dir1).normalize();
        return effectivedir;

    }

    /**
     * Die Normale Richtung aussen liefern am Punkt.
     * Returns null if coordinate isType not part of the boundary.
     *
     * @return
     */
    public static Vector2 getNormalAtCoordinate(Polygon p, Coordinate coordinate) {
        if (JtsUtil.findCoordinate(coordinate, p.getCoordinates()) != -1) {
            LineSegment[] lines = JtsUtil.getBoundaryLine(coordinate, p);
            if (lines != null && lines.length == 2) {
                Vector2 dir = JtsUtil.getEffectiveDirection(lines[0], lines[1]);
                Vector2 normal = JtsUtil.getNormalAtCoordinate(p, coordinate, dir);
                return normal;
            } else {
                logger.error("not exactly two lines");
            }
        } else {
            // coordinate on boundary?
            LineSegment[] lines = JtsUtil.getBoundaryLine(coordinate, p);
            if (lines != null) {
                LineSegment line = lines[0];
                Vector2 dir = JtsUtil.getDirection(line.p0, line.p1);
                Vector2 normal = JtsUtil.getNormalAtCoordinate(p, coordinate, dir);
                return normal;
            }
        }
        return null;
    }

    /**
     * Die Normale Richtung aussen liefern.
     *
     * @return
     */
    public static Vector2 getNormalAtCoordinate(Polygon p, Coordinate coordinate, Vector2 dirOfTangente) {
        Vector2 normal = dirOfTangente.rotate(new Degree(90));
        //Probing inside/outside
        Vector2 offset = normal.multiply(0.1);
        Coordinate probe = moveCoordinate(coordinate, offset);
        if (!isPartOfPolygon(probe, p)) {
            return normal;
        }
        normal = normal.negate();
        offset = normal.multiply(0.1);
        probe = moveCoordinate(coordinate, offset);
        if (!isPartOfPolygon(probe, p)) {
            return normal;
        }
        logger.warn("No normal pointing outside found");
        return new Vector2(1, 0);
    }

    /**
     * nur fuer einfache Faelle.
     *
     * @param pwithHoleOnEdge
     * @return
     */
    public static Polygon removeHoleOnEdge(Polygon pwithHoleOnEdge) {
        if (pwithHoleOnEdge.getNumInteriorRing() != 1) {
            return null;
        }
        LineSegment[] hole = toLineSegments(pwithHoleOnEdge.getInteriorRingN(0));
        LineSegment[] outer = toLineSegments(pwithHoleOnEdge.getExteriorRing());
        //Ermitteln welche hole edge auf einer outer edge liegt
        //so geht es nicht Geometry intersection = outer.intersection(hole);
        for (int i = 0; i < hole.length; i++) {
            for (int j = 0; j < outer.length; j++) {
                if (covers(outer[j], hole[i])) {
                    List<Coordinate> l = new ArrayList<>();
                    for (int k = 0; k <= j; k++) {
                        l.add(outer[k].p0);
                    }

                    int closest = findClosestVertexIndex(outer[j].p0, new Coordinate[]{hole[i].p0, hole[i].p1});
                    if (closest == 0) {
                        l.add(hole[i].p0);
                        for (int k = i + 1; k < hole.length; k++) {
                            l.add(hole[k].p0);
                        }
                        for (int k = 0; k < i; k++) {
                            l.add(hole[k].p0);
                        }
                    } else {
                        // hole has reverse order
                        l.add(hole[i].p1);
                        for (int k = i + 1; k < hole.length - 1; k++) {
                            l.add(hole[k].p1);
                        }
                        for (int k = 0; k < i; k++) {
                            l.add(hole[k].p1);
                        }
                        l.add(hole[i].p0);
                    }
                    for (int k = j + 1; k < outer.length; k++) {
                        l.add(outer[k].p0);
                    }
                    l.add(l.get(0));
                    Polygon newpoly = createPolygon(l);
                    if (!newpoly.isValid()) {
                        logger.error("invalid polygon created:" + newpoly);
                        return null;
                    }
                    //TODO check for multiple solutions?
                    return newpoly;
                }
            }
        }
        return null;
    }

    /**
     * including start AND end.
     * end might be lower than start!
     * start==end returns one element
     *
     * @return
     */
    public static List<Coordinate> sublist(Coordinate[] coors, int start, int end) {
        List<Coordinate> l = new ArrayList();

        if (start < 0 || end < 0) {
            logger.error("start or end < 0");
            return l;
        }
        if (start > coors.length - 1 || end > coors.length - 1) {
            logger.error("start or end > coors.length-1");
            return l;
        }
        if (start < end) {
            for (int i = start; i <= end; i++) {
                l.add(coors[i]);
            }
        } else {
            for (int i = start; i >= end; i--) {
                l.add(coors[i]);
            }
        }
        return l;
    }

    /**
     * Eine Coordinate so durch zwei andere ersetzen, dass es ein valid polygon bleibt.
     *
     * @param polygon
     * @param coordinate
     * @param c0
     * @param c1
     * @return
     */
    public static Polygon replace(Polygon polygon, Coordinate coordinate, Coordinate c0, Coordinate c1) {
        if (!polygon.isValid()) {
            logger.error("invalid");
            return null;
        }
        Coordinate[] coors = polygon.getCoordinates();
        List<Coordinate> clist = new ArrayList(Arrays.asList(coors));
        int index = findClosestVertexIndex(coordinate, clist);
        clist.set(index, c0);
        clist.add(index + 1, c1);
        if (index == 0) {
            clist.set(clist.size() - 1, c0);
        }
        Polygon newpoly = createPolygonFromCoordinateList(new CoordinateList(clist), true);
        if (newpoly != null && newpoly.isValid()) {
            return newpoly;
        }
        clist = new ArrayList(Arrays.asList(coors));
        clist.set(index, c1);
        clist.add(index + 1, c0);
        if (index == 0) {
            clist.set(clist.size() - 1, c1);
        }
        newpoly = createPolygonFromCoordinateList(new CoordinateList(clist), true);
        if (newpoly != null && newpoly.isValid()) {
            return newpoly;
        }
        logger.error("replace: resulting polygon invalid:" + newpoly);
        return null;
    }

    /**
     * Moeglichst geschmeidig ohne ZickZack einfuegen.
     *
     * @return
     */
    public static Polygon insert(Polygon polygon, Coordinate beforevertex, Coordinate c0, Coordinate c1) {
        if (!polygon.isValid()) {
            logger.error("invalid");
            return null;
        }
        Coordinate[] coors = polygon.getCoordinates();
        List<Coordinate> clist = new ArrayList(Arrays.asList(coors));
        int beforeindex = findClosestVertexIndex(beforevertex, clist);
        if (beforeindex == 0) {
            // den gibt dann ja hinten nochmal. Darum den verwenden. Das ist evtl. eine Desdorf Grid Sonderlocke
            beforeindex = clist.size() - 1;
        }
        if (c0.distance(coors[beforeindex]) < c1.distance(coors[beforeindex])) {
            clist.add(beforeindex, c0);
            clist.add(beforeindex, c1);
        } else {
            clist.add(beforeindex, c1);
            clist.add(beforeindex, c0);
        }
        Polygon newpoly = createPolygonFromCoordinateList(new CoordinateList(clist), false);
        if (newpoly != null && newpoly.isValid()) {
            return newpoly;
        }
        logger.error("insert: resulting polygon invalid:" + newpoly);
        return null;
    }

    /**
     *
     */
    public static List<Coordinate> toList(Coordinate c0, Coordinate c1) {
        List<Coordinate> l = new ArrayList<>();
        l.add(c0);
        l.add(c1);
        return l;
    }

    public static List<Coordinate> toList(Coordinate[] c) {
        return Arrays.asList(c);
    }

    public static Coordinate[] toArray(List<Coordinate> sublist) {
        return (Coordinate[]) sublist.toArray(new Coordinate[0]);
    }

    /**
     * Aus einem (closed oder open) LineString eine fortlaufende(!) Reihe von Coordinates rausnehmen und das
     * Resultat liefern. Wenn der LineString geteilt wird, kommen zwei zurück.
     * <p>
     * fromto ist in/exklusive(?). Die bleiben selber erhalten, aber nicht die line zwischen ihnen.
     */
    public static LineString[] removeCoordinatesFromLine(LineString line, int[] fromto/*List<Integer> toBeRemoved*/) {
        List<Coordinate> result = new ArrayList<>();
        List<Coordinate> result2 = null;
        Coordinate[] coors = line.getCoordinates();
        int from = fromto[0];
        int to = fromto[1];
        boolean isClosed = coors[0].equals2D(coors[coors.length - 1]);
        //if (toBeRemoved.contains(0) && toBeRemoved.contains(coors.length - 2)) {
        if (to < from) {
            boolean started = false;
            /*for (int i = 0; i < coors.length - 1; i++) {
                if (i == fromto[0]) {
                    if (started) {
                        result.add(coors[i]);
                        break;
                    }
                } else {
                    if (result.size() == 0) {
                        result.add(coors[i - 1]);
                    }
                    result.add(coors[i]);
                    started = true;
                }
            }*/
            for (int i = to; i <= from; i++) {
                result.add(coors[i]);
            }
            //for (int i = 0; i <= to; i++) {
            //  result.add(coors[i]);
            //}
        } else {
            for (int i = 0; i <= from && from > 0; i++) {
                result.add(coors[i]);
            }
            if (isClosed) {
                //dann weiter result fuellen (aber von vorne!) und nur eine line liefern.
                int pos = 0;
                for (int i = to; i < coors.length - 1; i++) {
                    result.add(pos, coors[i]);
                    pos++;
                }
            } else {
                result2 = new ArrayList<>();
                for (int i = to; i < coors.length; i++) {
                    result2.add(coors[i]);
                }
                if (result2.size() < 2) {
                    result2 = null;
                }
            }
        }
        if (result.size() == 0) {
            return new LineString[]{createLineFromCoordinates(result2)};
        }
        if (result2 == null) {
            return new LineString[]{createLineFromCoordinates(result)};
        }
        return new LineString[]{createLineFromCoordinates(result), createLineFromCoordinates(result2)};
    }

    /**
     * Knifflig, denn das boundarysegment kann eine andere Orientierung haben.
     * Ob das immer so zuverlaessig ist? Bei ganz spitzen Winkeln wird das scheitern.
     * Nein. das Scheitert bei allem < 90Grad! Besser von der Mitte pruefen und mit
     * Gegenprobe. Evtl. dann noch iterativ bei undecided?
     * Evtl. hilft https://stackoverflow.com/questions/1165647/how-to-determine-if-a-list-of-polygon-points-are-in-clockwise-order
     */
    public static Boolean isPolygonLeft(LineString boundarysegment, Polygon polygon) {
        Coordinate[] c = boundarysegment.getCoordinates();
        Vector2 c0 = toVector2(c[0]);
        Vector2 dir = toVector2(c[1]).subtract(c0);
        Vector2 origin = c0.add(dir.multiply(0.5));
        Vector2 normal = dir.rotate(new Degree(90)).normalize();

        // zu klein darf die auch nicht sein. 5.9.19: 0.00001->0.01
        double probeDistance = 0.01;
        Coordinate probe = toCoordinate(origin.add(normal.multiply(probeDistance)));
        if (isPartOfPolygon(probe, polygon)) {
            return true;
        }
        probe = toCoordinate(origin.add(normal.negate().multiply(probeDistance)));
        if (isPartOfPolygon(probe, polygon)) {
            return false;
        }
        logger.error("isPolygonLeft undecided");
        return null;
    }
}
