package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2world.VectorXZ;

import java.awt.*;
import java.awt.geom.Area;

public interface TileProjection {
    Point vectorxzToPoint(VectorXZ v);

    Point coordinateToPoint(Coordinate v);

    /**
     * AWT Polygon kann keine Holes, darum Area.
     */
    Area toArea(com.vividsolutions.jts.geom.Polygon polygon);

    double getScale();
}
