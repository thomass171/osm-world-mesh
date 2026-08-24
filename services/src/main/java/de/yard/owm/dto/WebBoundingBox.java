package de.yard.owm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bounding box (rectangular area on a map).
 * In Europe min is southwest while max is northeast.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebBoundingBox {
    WebLatLon min;
    WebLatLon max;
}
