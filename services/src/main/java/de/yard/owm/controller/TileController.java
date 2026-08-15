package de.yard.owm.controller;

import de.yard.owm.services.TileService;
import de.yard.threed.core.Util;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.stream.Collectors;

@AllArgsConstructor
//it's no REST @RestController
@Controller
@Slf4j
public class TileController {

    private final TileService tileService;

    /**
     * Currently no filter, so return all
     */
    /*@CrossOrigin
    @GetMapping("/owm/tile/search/findByFilter")
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