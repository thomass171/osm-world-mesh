package de.yard.threed.osm2scenery.util;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Pair;

/**
 * For Simplification.
 * On ways left/right apply in direction of way.
 * 7.3.26 Derived from CoordinatePair
 */
public class GeoCoordinatePair extends Pair<GeoCoordinate, GeoCoordinate> {
    public GeoCoordinatePair(GeoCoordinate first, GeoCoordinate second) {
        super(first, second);
    }

    public GeoCoordinatePair swap() {
        return new GeoCoordinatePair(this.getSecond(), this.getFirst());
    }

    public GeoCoordinate right() {
        return this.getFirst();
    }


    public GeoCoordinate left() {
        return this.getSecond();
    }
}
