package de.yard.owm.controller;

import de.yard.owm.dto.OsmResponse;
import de.yard.owm.misc.GeneralOwmException;
import de.yard.owm.services.OsmDataService;
import de.yard.owm.services.util.WellKnownMesh;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@AllArgsConstructor
@RestController
@Slf4j
public class OsmController {

    private final OsmDataService osmDataService;

    /**
     * Rely on ControllerAdvice for exception handling (at least most?)
     */
    @CrossOrigin
    @GetMapping(value = "/worldmesh/osm")
    public ResponseEntity<OsmResponse> getDatasetlist(@RequestParam(value = "meshName", required = true) String meshName) throws GeneralOwmException {

        OsmResponse response = null;
        WellKnownMesh wellKnownMesh = WellKnownMesh.of(meshName);

        List<String> names = osmDataService.findDatasetsByWellKnownMesh(wellKnownMesh);
        response = new OsmResponse(names);
        return ResponseEntity.ok(response);
    }
}