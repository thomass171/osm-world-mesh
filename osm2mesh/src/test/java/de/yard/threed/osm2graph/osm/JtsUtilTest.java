package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.LatLon;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JtsUtilTest {

    Polygon square = polygon("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");

    @Test
    public void testIsSameLocation() {
        LatLon location = LatLon.fromDegrees(51.0, 7.0);
        assertTrue(JtsUtil.isSameLocation(location, LatLon.fromDegrees(51.0, 7.0)));
        // elevation is ignored
        assertTrue(JtsUtil.isSameLocation(GeoCoordinate.fromLatLon(location, 10), GeoCoordinate.fromLatLon(location, 20)));

        // rounding noise
        LatLon noise = LatLon.fromDegrees(51.0, 7.0);
        noise.setLongitudeRad(noise.getLonRad() + 1e-15);
        assertTrue(JtsUtil.isSameLocation(location, noise));

        // about 0.1 m away
        assertFalse(JtsUtil.isSameLocation(location, LatLon.fromDegrees(51.000001, 7.0)));
        assertFalse(JtsUtil.isSameLocation(location, LatLon.fromDegrees(51.0, 7.000001)));
    }

    @Test
    public void testSubtractPolygonsCrossing() {
        // strip crossing from south to north splits into west and east part
        List<Polygon> result = JtsUtil.subtractPolygons(square, polygon("POLYGON((4 -1, 6 -1, 6 11, 4 11, 4 -1))"));
        assertEquals(2, result.size());
        assertNoHolesAndArea(80, result);
    }

    @Test
    public void testSubtractPolygonsNoOverlap() {
        // returns the polygon itself
        assertSame(square, JtsUtil.subtractPolygons(square, polygon("POLYGON((20 0, 30 0, 30 10, 20 10, 20 0))")).get(0));
        // only touching
        assertEquals(List.of(square), JtsUtil.subtractPolygons(square, polygon("POLYGON((10 0, 20 0, 20 10, 10 10, 10 0))")));
    }

    @Test
    public void testSubtractPolygonsCovered() {
        assertEquals(0, JtsUtil.subtractPolygons(square, polygon("POLYGON((-1 -1, 11 -1, 11 11, -1 11, -1 -1))")).size());
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
        // Real geo coordinates (lon/lat near 7/51) on purpose instead of readable integers: Polygons of a few meters
        // have tiny areas in degree² (here about 1e-8), which is what the relative minimum overlap
        // (MIN_OVERLAP_AREA_RATIO) has to cope with. An absolute limit suitable for projected coordinates wouldn't work here.
        Polygon area = polygon("POLYGON((7.0001 51.0001, 7.0004 51.0001, 7.0004 51.0004, 7.0001 51.0004, 7.0001 51.0001))");
        Polygon connector = polygon("POLYGON((7.0002 51.0002, 7.0003 51.0002, 7.0003 51.0003, 7.0002 51.0003, 7.0002 51.0002))");
        List<Polygon> result = JtsUtil.subtractPolygons(area, connector);
        assertEquals(2, result.size());
        result.forEach(p -> assertEquals(0, p.getNumInteriorRing()));
        assertEquals(area.getArea() - connector.getArea(), result.stream().mapToDouble(Polygon::getArea).sum(), 1e-15);

        // only touching (sharing the east edge)
        Polygon neighbour = polygon("POLYGON((7.0004 51.0001, 7.0005 51.0001, 7.0005 51.0004, 7.0004 51.0004, 7.0004 51.0001))");
        assertSame(area, JtsUtil.subtractPolygons(area, neighbour).get(0));
    }

    @Test
    public void testSubtractAllPolygons() {
        // a north/south and a west/east strip crossing each other split into 4 parts
        List<Polygon> result = JtsUtil.subtractAllPolygons(square, List.of(
                polygon("POLYGON((4 -1, 6 -1, 6 11, 4 11, 4 -1))"),
                polygon("POLYGON((-1 4, 11 4, 11 6, -1 6, -1 4))")));
        assertEquals(4, result.size());
        assertNoHolesAndArea(64, result);

        // no overlap at all returns the polygon itself
        assertEquals(List.of(square), JtsUtil.subtractAllPolygons(square, List.of(polygon("POLYGON((20 0, 30 0, 30 10, 20 10, 20 0))"))));
        assertSame(square, JtsUtil.subtractAllPolygons(square, List.of()).get(0));

        // only the second one overlaps
        result = JtsUtil.subtractAllPolygons(square, List.of(
                polygon("POLYGON((20 0, 30 0, 30 10, 20 10, 20 0))"),
                polygon("POLYGON((5 -1, 11 -1, 11 11, 5 11, 5 -1))")));
        assertEquals(1, result.size());
        assertNoHolesAndArea(50, result);

        // completely covered
        assertEquals(0, JtsUtil.subtractAllPolygons(square, List.of(polygon("POLYGON((-1 -1, 11 -1, 11 11, -1 11, -1 -1))"))).size());
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
