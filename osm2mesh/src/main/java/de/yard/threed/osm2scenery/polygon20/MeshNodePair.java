package de.yard.threed.osm2scenery.polygon20;

import de.yard.threed.core.Degree;
import de.yard.threed.osm2scenery.util.GeoCoordinatePair;

/**
 * Typically two way nodes, eg. at a connector
 */
public interface MeshNodePair {
    GeoCoordinatePair getCoordinates();
    long getOsmWayId();
    Degree getHeading();
}
