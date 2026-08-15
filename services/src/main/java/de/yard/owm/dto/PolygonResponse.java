package de.yard.owm.dto;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.geometry.Polygon;
import de.yard.threed.osm2scenery.polygon20.GeoPolygon;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PolygonResponse {
    final List<WebLatLon> points = new ArrayList();
    private boolean closed = false;

    public static PolygonResponse buildFromPolygon(GeoPolygon polygon) {
        if (polygon == null) {
            return null;
        }
        PolygonResponse response = new PolygonResponse();
        // skip closing point
        for (Coordinate c : polygon.getCoordinates()) {
            response.points.add(new WebLatLon(c.y, c.x));
        }
        response.setClosed(true);
        return response;
    }
}
