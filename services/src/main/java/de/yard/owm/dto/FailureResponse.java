package de.yard.owm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FailureResponse extends BaseResponse {
    /**
     * Persistence id of the failure, used by the frontend to request the related SVG
     * from MeshController. null if the failure is not DB-backed.
     */
    Long id;
    String message;
    String sourceRef;
    PolygonResponse polygon;

}
