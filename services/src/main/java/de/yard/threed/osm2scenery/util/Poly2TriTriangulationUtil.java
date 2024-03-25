package de.yard.threed.osm2scenery.util;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2world.LineSegmentXZ;
import de.yard.threed.osm2world.SimplePolygonXZ;
import de.yard.threed.osm2world.TriangleXZ;
import de.yard.threed.osm2world.TriangulationException;
import de.yard.threed.osm2world.VectorXZ;
import org.apache.log4j.Logger;
import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.point.TPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.util.Collections.disjoint;

/**
 * Aus OSM2World. Hat eigenes Copyright TODO CREDITS (http://sites-final.uclouvain.be/mema/Poly2Tri/)
 * <p>
 * uses the poly2tri library for triangulation.
 * Creates a Constrained Delaunay Triangulation, not true Delaunay!
 */
public final class Poly2TriTriangulationUtil {
    static Logger logger = Logger.getLogger(Poly2TriTriangulationUtil.class);

    private Poly2TriTriangulationUtil() {
    }

    /**
     * triangulates of a polygon with holes.
     * <p>
     * Accepts some unconnected points within the polygon area
     * and will create triangle vertices at these points.
     * It will also accept line segments as edges that must be integrated
     * into the resulting triangulation.
     *
     * @throws TriangulationException if triangulation fails
     */
    public static final List<TriangleXZ> triangulate(
            SimplePolygonXZ outerPolygon,
            Collection<SimplePolygonXZ> holes,
            Collection<LineSegmentXZ> segments,
            Collection<VectorXZ> points) throws TriangulationException {
        
		/* remove any problematic data (duplicate points) from the input */

        Set<VectorXZ> knownVectors =
                new HashSet<VectorXZ>(outerPolygon.getVertexCollection());

        List<SimplePolygonXZ> filteredHoles = new ArrayList<SimplePolygonXZ>();

        for (SimplePolygonXZ hole : holes) {

            if (disjoint(hole.getVertexCollection(), knownVectors)) {
                filteredHoles.add(hole);
                knownVectors.addAll(hole.getVertices());
            }

        }

        //TODO filter segments

        Set<VectorXZ> filteredPoints = new HashSet<VectorXZ>(points);
        filteredPoints.removeAll(knownVectors);

        // remove points that are *almost* the same as a known vector
        Iterator<VectorXZ> filteredPointsIterator = filteredPoints.iterator();
        while (filteredPointsIterator.hasNext()) {
            VectorXZ filteredPoint = filteredPointsIterator.next();
            for (VectorXZ knownVector : knownVectors) {
                if (knownVector.distanceTo(filteredPoint) < 0.2) {
                    filteredPointsIterator.remove();
                    break;
                }
            }
        }
		
		/* run the actual triangulation */

        return triangulateFast(outerPolygon, filteredHoles, segments, filteredPoints);

    }

    /**
     * variant of {@link #triangulate(SimplePolygonXZ, Collection, Collection, Collection)}
     * that does not validate the input. This isType obviously faster,
     * but the caller needs to make sure that there are no problems.
     *
     * @throws TriangulationException if triangulation fails
     */
    public static final List<TriangleXZ> triangulateFast(
            SimplePolygonXZ outerPolygon,
            Collection<SimplePolygonXZ> holes,
            Collection<LineSegmentXZ> segments,
            Collection<VectorXZ> points) throws TriangulationException {
		
		/* prepare data for triangulation */

        Polygon triangulationPolygon = toPolygon(outerPolygon);

        for (SimplePolygonXZ hole : holes) {
            triangulationPolygon.addHole(toPolygon(hole));
        }

        //TODO collect points and constraints from segments

        for (VectorXZ p : points) {
            triangulationPolygon.addSteinerPoint(toTPoint(p));
        }

        try {
			
			/* run triangulation */

            Poly2Tri.triangulate(triangulationPolygon);
			
			/* convert the result to the desired format */

            List<DelaunayTriangle> triangles = triangulationPolygon.getTriangles();

            List<TriangleXZ> result = new ArrayList<TriangleXZ>(triangles.size());

            for (DelaunayTriangle triangle : triangles) {
                result.add(toTriangleXZ(triangle));
            }

            return result;

        } catch (Exception e) {
            throw new TriangulationException(e);
        } catch (StackOverflowError e) {
            throw new TriangulationException(e);
        }

    }

    public static final List<com.vividsolutions.jts.geom.Polygon> triangulate(
            com.vividsolutions.jts.geom.Polygon polygon) {


        Polygon triangulationPolygon = toPolygon(polygon.getExteriorRing().getCoordinates());

        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            triangulationPolygon.addHole(toPolygon(polygon.getInteriorRingN(i).getCoordinates()));
        }

        //TODO collect points and constraints from segments

        for (Coordinate p : polygon.getCoordinates()) {
            //triangulationPolygon.addSteinerPoint(toTPoint(p));
        }

        try {
			
			/* run triangulation */

            Poly2Tri.triangulate(triangulationPolygon);
			
			/* convert the result to the desired format */

            List<DelaunayTriangle> triangles = triangulationPolygon.getTriangles();

            List<com.vividsolutions.jts.geom.Polygon> result = new ArrayList<com.vividsolutions.jts.geom.Polygon>(triangles.size());

            for (DelaunayTriangle triangle : triangles) {
                result.add(toTriangle(triangle));
            }

            return result;

        } catch (Exception e) {
            //e.printStackTrace();
            //18.4.19; Kommt schon mal vor, z.B. NPE
            logger.error("triangulate failed:"+e.getClass().getName()+":"+e.getMessage());
        } catch (StackOverflowError e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final TPoint toTPoint(VectorXZ v) {
        return new TPoint(v.x, v.z);
    }

    private static final TPoint toTPoint(Coordinate v) {
        return new TPoint(v.x, v.y);
    }

    private static final VectorXZ toVectorXZ(TriangulationPoint points) {
        return new VectorXZ(points.getX(), points.getY());
    }

    private static final Coordinate toCoordinate(TriangulationPoint points) {
        return new Coordinate(points.getX(), points.getY(),0);
    }

    private static final Polygon toPolygon(SimplePolygonXZ polygon) {

        List<PolygonPoint> points = new ArrayList<PolygonPoint>(polygon.size());

        for (VectorXZ v : polygon.getVertices()) {
            points.add(new PolygonPoint(v.x, v.z));
        }

        return new Polygon(points);

    }

    private static final Polygon toPolygon(Coordinate[] coors) {

        List<PolygonPoint> points = new ArrayList<PolygonPoint>();
        for (Coordinate v : coors) {
            points.add(new PolygonPoint(v.x, v.y));
        }
        // Poly2Tri doesn't expect closed Polygons
        points.remove(points.size()-1);
        return new Polygon(points);

    }

    private static final TriangleXZ toTriangleXZ(DelaunayTriangle triangle) {

        return new TriangleXZ(
                toVectorXZ(triangle.points[0]),
                toVectorXZ(triangle.points[1]),
                toVectorXZ(triangle.points[2]));

    }

    private static final com.vividsolutions.jts.geom.Polygon toTriangle(DelaunayTriangle triangle) {

        return JtsUtil.createTriangle(
                toCoordinate(triangle.points[0]),
                toCoordinate(triangle.points[1]),
                toCoordinate(triangle.points[2]));

    }
}