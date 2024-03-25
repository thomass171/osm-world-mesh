package de.yard.threed.osm2scenery.util;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Pair;

/**
 * For Simplification
 */
public class CoordinatePair extends Pair<Coordinate, Coordinate> {
    public CoordinatePair(Coordinate first, Coordinate second) {
        super(first, second);
    }

    public CoordinatePair swap() {
        return new CoordinatePair(this.getSecond(), this.getFirst());
    }

    public Coordinate right() {
        return this.getFirst();
    }


    public Coordinate left() {
        return this.getSecond();
    }
}
