package de.yard.owm.dto;

import de.yard.threed.core.LatLon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebLatLon {
    double lat;
    double lon;

    public static WebLatLon buildFromLatLon(LatLon latLon){
        return new WebLatLon(latLon.getLatDeg().getDegree(), latLon.getLonDeg().getDegree());
    }
}
