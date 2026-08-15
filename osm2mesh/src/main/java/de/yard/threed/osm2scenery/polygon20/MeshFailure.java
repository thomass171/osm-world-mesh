package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.GeoCoordinate;

import java.util.List;

/**
 * 12.2.26: No setter to keep consistent modification workflow via MeshService
 */
public interface MeshFailure {

    String getMessage();
    String getSourceRef();
    // Should not need projection
    GeoPolygon getGeoPolygon();
}
