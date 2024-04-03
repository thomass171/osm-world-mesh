package de.yard.threed.jts;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Created by thomass on 30.01.17.
 */
public class Sample {
    Sample() {
        
    }
    public static Geometry buildSample() throws ParseException {
        // read a geometry from a WKT string (using the default geometry factory)
        
        Geometry g1 = new WKTReader().read("LINESTRING (0 0, 10 10, 20 20)");
        System.out.println("Geometry 1: " + g1);

        // create a geometry by specifying the coordinates directly
        Coordinate[] coordinates = new Coordinate[]{new Coordinate(0, 0),
                new Coordinate(10, 10), new Coordinate(20, 20)};
        // use the default factory, which gives full double-precision
        Geometry g2 = new GeometryFactory().createLineString(coordinates);
        System.out.println("Geometry 2: " + g2);

        // compute the intersection of the two geometries
        Geometry g3 = g1.intersection(g2);
        System.out.println("G1 intersection G2: " + g3);
        return g3;
    }

    /**
     * 
     * @return
     * @throws ParseException
     */
    public static Geometry buildSampleA() {
        try {
            Geometry g = new WKTReader().read("POLYGON ((50 300, 220 300, 220 100, 50 100, 50 300))");
            return g;
        }
        catch (Exception e ){
            throw new RuntimeException(e);
        }
    }

    public static Geometry buildSampleB() {
        try {
            Geometry g = new WKTReader().read("POLYGON ((310 330, 210 330, 140 270, 130 190, 185 88, 270 70, 355 119, 380 210, 360 280, 310 330))");
            return g;
        }
        catch (Exception e ){
            throw new RuntimeException(e);
        }
    }
}
