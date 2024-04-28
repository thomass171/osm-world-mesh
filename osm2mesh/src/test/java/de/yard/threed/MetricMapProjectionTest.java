package de.yard.threed;


import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Degree;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2world.MetricMapProjection;
import de.yard.threed.traffic.geodesy.GeoCoordinate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Also for MercatorProjection
 */
public class MetricMapProjectionTest {

    /**
     * Sketch 3??
     */
    @Test
    void testProjection() {
        LatLon origin = new LatLon(new Degree(51.0).toRad(), new Degree(7.0).toRad());

        MetricMapProjection projection = new MetricMapProjection(origin);

        Degree bottom = new Degree(51.0 - (0.001 / 2.0) + 0.0001);
        Degree left = new Degree(7.0 - (0.001 / 2.0) + 0.0001);
        Coordinate c = projection.project(new GeoCoordinate(bottom, left));

        assertEquals(-28.022, c.x, 0.0001);
        assertEquals(-44.528, c.y, 0.0001);

        GeoCoordinate reprojected = projection.unproject(c);
        assertEquals(bottom.getDegree(), reprojected.getLatDeg().getDegree(), 0.01);
        assertEquals(left.getDegree(), reprojected.getLonDeg().getDegree(), 0.01);
    }
}
