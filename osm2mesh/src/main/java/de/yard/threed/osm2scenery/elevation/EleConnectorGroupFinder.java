package de.yard.threed.osm2scenery.elevation;

import com.vividsolutions.jts.geom.Coordinate;

public interface EleConnectorGroupFinder {
    EleConnectorGroup findGroupForCoordinate(Coordinate coordinate);
}
