package de.yard.owm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FailureResponse extends BaseResponse {
    String message;
    String sourceRef;
    PolygonResponse polygon;

}
