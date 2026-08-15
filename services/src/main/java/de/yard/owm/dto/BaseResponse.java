package de.yard.owm.dto;

import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse {
    // Typically set in combination with HTTP 400
    String error;

}
