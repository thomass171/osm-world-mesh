package de.yard.owm.dto;

import de.yard.threed.core.LatLon;
import de.yard.threed.core.StringUtils;
import de.yard.threed.core.geometry.Polygon;
import de.yard.threed.osm2scenery.polygon20.GeoPolygon;
import de.yard.threed.osm2scenery.polygon20.MeshFailure;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeshResponse extends BaseResponse {
    String name;
    List<PolygonResponse> polygons;
    List<FailureResponse> failures;

    public static List<PolygonResponse> buildFromPolygonList(List<GeoPolygon> polygons) {
        List<PolygonResponse> response = new ArrayList<>();

        for (int i = 0; i < polygons.size(); i++) {
            response.add(PolygonResponse.buildFromPolygon(polygons.get(i)));
        }
        return response;
    }

    public static List<FailureResponse> buildFromFailureList(List<MeshFailure> failures) {
        List<FailureResponse> response = new ArrayList<>();

        if (failures != null) {
            for (MeshFailure failure : failures) {
                String msg;
                if (failure.getMessage().length() > 100) {
                    msg = StringUtils.substring(failure.getMessage(), 0, 100) + "...";
                } else {
                    msg = failure.getMessage();
                }
                response.add(new FailureResponse(failure.getId(), msg, failure.getSourceRef(), PolygonResponse.buildFromPolygon(failure.getGeoPolygon())));
            }
        }
        return response;
    }
}
