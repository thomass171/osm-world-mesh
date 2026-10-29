package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JtsUtilTest {

    Polygon square = polygon("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");

    @Test
    public void testSubtractPolygonsCrossing() {
        // strip crossing from south to north splits into west and east part
        List<Polygon> result = JtsUtil.subtractPolygons(square, polygon("POLYGON((4 -1, 6 -1, 6 11, 4 11, 4 -1))"));
        assertEquals(2, result.size());
        assertNoHolesAndArea(80, result);
    }

    @Test
    public void testSubtractPolygonsNoOverlap() {
        assertNull(JtsUtil.subtractPolygons(square, polygon("POLYGON((20 0, 30 0, 30 10, 20 10, 20 0))")));
        // only touching
        assertNull(JtsUtil.subtractPolygons(square, polygon("POLYGON((10 0, 20 0, 20 10, 10 10, 10 0))")));
    }

    @Test
    public void testSubtractPolygonsInside() {
        // completely inside would be a hole, so it is split through the hole
        List<Polygon> result = JtsUtil.subtractPolygons(square, polygon("POLYGON((4 4, 6 4, 6 6, 4 6, 4 4))"));
        assertEquals(2, result.size());
        assertNoHolesAndArea(96, result);
    }

    @Test
    public void testSplitAtHolesTwoHoles() {
        // wide rectangle with two holes side by side
        Polygon wide = polygon("POLYGON((0 0, 20 0, 20 4, 0 4, 0 0), (2 1, 4 1, 4 3, 2 3, 2 1), (14 1, 16 1, 16 3, 14 3, 14 1))");
        List<Polygon> result = JtsUtil.splitAtHoles(wide);
        assertTrue(result.size() >= 3, "" + result.size());
        assertNoHolesAndArea(72, result);
    }

    @Test
    public void testSubtractPolygonsGeoCoordinates() {
        // degree sized polygons need a smaller minAreaChange
        Polygon area = polygon("POLYGON((7.0001 51.0001, 7.0004 51.0001, 7.0004 51.0004, 7.0001 51.0004, 7.0001 51.0001))");
        Polygon connector = polygon("POLYGON((7.0002 51.0002, 7.0003 51.0002, 7.0003 51.0003, 7.0002 51.0003, 7.0002 51.0002))");
        assertNull(JtsUtil.subtractPolygons(area, connector));
        List<Polygon> result = JtsUtil.subtractPolygons(area, connector, 0.000000001);
        assertEquals(2, result.size());
        result.forEach(p -> assertEquals(0, p.getNumInteriorRing()));
        assertEquals(area.getArea() - connector.getArea(), result.stream().mapToDouble(Polygon::getArea).sum(), 1e-15);
    }

    private void assertNoHolesAndArea(double expectedArea, List<Polygon> polygons) {
        double area = 0;
        for (Polygon p : polygons) {
            assertEquals(0, p.getNumInteriorRing());
            assertTrue(p.isValid());
            assertFalse(p.isEmpty());
            area += p.getArea();
        }
        assertEquals(expectedArea, area, 0.000001);
    }

    private static Polygon polygon(String wkt) {
        try {
            return (Polygon) new WKTReader().read(wkt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
