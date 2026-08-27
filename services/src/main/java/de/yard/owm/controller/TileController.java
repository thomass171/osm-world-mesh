package de.yard.owm.controller;

import de.yard.owm.dto.WebBoundingBox;
import de.yard.owm.services.TileService;
import de.yard.threed.core.LatLon;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@AllArgsConstructor
//it's no REST @RestController
@Controller
@Slf4j
public class TileController {

    private final TileService tileService;

    /**
     * Returns an image for the given bounding box (rectangular area selected on the map).
     * For now just a placeholder image, not a real tile rendering.
     * Bounds are bound from query params min.lat, min.lon, max.lat, max.lon.
     * In Europe min is southwest while max is northeast.
     */
    @CrossOrigin
    @GetMapping(value = "/worldmesh/tile/image", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getImage(@ModelAttribute WebBoundingBox request) {
        try {
            LatLon min = new LatLon(request.getMin().getLat(), request.getMin().getLon());
            LatLon max = new LatLon(request.getMax().getLat(), request.getMax().getLon());
            byte[] image = tileService.buildExampleImage(min, max);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
        } catch (IOException e) {
            log.error("buildExampleImage() failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Currently no filter, so return all
     */
    /*@CrossOrigin
    @GetMapping("/worldmesh/tile/search/findByFilter")
    public ResponseEntity<TileSearchResponse> findByFilter() {

        TileSearchResponse response = null;
        try {
            response = new TileSearchResponse();
            response.setTiles(tileService.getTiles().stream().map(t -> TileResponse.buildFromTile(t)).collect(Collectors.toUnmodifiableList()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(response);
    }*/

    /*@CrossOrigin
    @GetMapping("/traffic/tile/{index}")
    public ResponseEntity<TileResponse> findTile(@PathVariable("index") int index) {

        Util.notyet();
        return (ResponseEntity<TileResponse>) ResponseEntity.noContent();
    }*/
}