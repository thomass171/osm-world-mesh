package de.yard.owm.controller;

import de.yard.owm.dto.MeshBuildResponse;
import de.yard.owm.dto.MeshResponse;
import de.yard.owm.dto.PolygonResponse;
import de.yard.owm.misc.GeneralOwmException;
import de.yard.owm.services.OsmDataService;
import de.yard.owm.services.mesh.MeshService;
import de.yard.owm.services.osm.OsmService;
import de.yard.owm.services.util.WellKnownMesh;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.MainGrid;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.OsmXmlParser;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.OSMData;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@AllArgsConstructor
@RestController
@Slf4j
public class MeshController {

    private final OsmService osmService;

    private final MeshService meshService;

    private final OsmDataService osmDataService;

    /**
     * For now restricted to "well known meshes".
     * Rely on ControllerAdvice for exception handling (at least most?)
     */
    @CrossOrigin
    @PostMapping(value = "/owm/mesh")
    public ResponseEntity<MeshResponse> createMesh(@RequestParam(value = "meshName", required = true) String meshName) throws MeshInconsistencyException, GeneralOwmException {

        //try {
        TerrainMesh terrainMesh = meshService.loadMesh(meshName);
        if (terrainMesh != null) {
            MeshResponse response = null;
            response = new MeshResponse();
            response.setError("Mesh already exists: " + meshName);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        WellKnownMesh wellKnownMesh = WellKnownMesh.of(meshName);
        // calc gridCellBounds from osm?? or existing mesh??
        meshService.createMesh(meshName, wellKnownMesh.getGridCellBounds().getBoundary());
        terrainMesh = meshService.loadMesh(meshName);
        MeshResponse response = new MeshResponse(meshName,
                MeshResponse.buildFromPolygonList(JtsUtil.buildGeoPolygonList(terrainMesh.polygons)),
                MeshResponse.buildFromFailureList(terrainMesh.failures));
       /* } catch (Exception e) {
            log.error("populateMesh() failed", e);
            return ResponseEntity.internalServerError().build();
        }*/
        return ResponseEntity.ok(response);
    }

    /**
     * Add OSM data to a mesh.
     * For now restricted to "well known meshes".
     * Returns the full mesh for now (not just newly created polygons and the failures of this build).
     */
    @CrossOrigin
    @PutMapping(value = "/owm/mesh"/*might be XML or a file name, consumes = MediaType.APPLICATION_XML_VALUE*/)
    public ResponseEntity<MeshResponse> put(@RequestParam(value = "meshName", required = true) String meshName, @RequestBody String osmXmlOrFileName) {

        try {
            WellKnownMesh wellKnownMesh = WellKnownMesh.valueOf(meshName);

            String osmXml;
            if (osmXmlOrFileName.length() < 512 && osmXmlOrFileName.endsWith(".xml")) {
                // assume it is a file name
                osmXml = osmDataService.loadXml(wellKnownMesh, osmXmlOrFileName);
            } else {
                osmXml = osmXmlOrFileName;
            }
            OsmXmlParser parser = new OsmXmlParser(osmXml);
            OSMData osmData = parser.getData();

            //TODO calc gridCellBounds from osm?? or existing mesh??
            GridCellBounds gridCellBounds = MainGrid.buildDesdorf();
            // meshService.createMesh("Desdorf", gridCellBounds.getBoundary());
            //temeshService.loadMesh("Desdorf", gridCellBounds.getBoundary());


            //TerrainMesh.meshFactoryInstance = new PersistedMeshFactory("Desdorf", gridCellBounds.getProjection().getBaseProjection(), terrainMeshManager);

            OSMToSceneryDataConverter converter = new OSMToSceneryDataConverter(gridCellBounds.getProjection(), gridCellBounds);
            MapData mapData = converter.createMapData(osmData);
            osmService.populateMesh(meshName, mapData);
            // just return full mesh as response for now, not just what was populated
            TerrainMesh terrainMesh = meshService.loadMesh(meshName);
            MeshResponse response = new MeshResponse(meshName,
                    MeshResponse.buildFromPolygonList(JtsUtil.buildGeoPolygonList(terrainMesh.polygons)),
                    MeshResponse.buildFromFailureList(terrainMesh.failures));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("populateMesh() failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @CrossOrigin
    @GetMapping(value = "/owm/mesh")
    public ResponseEntity<MeshResponse> getMesh(@RequestParam(value = "meshName", required = true) String meshName) {

        MeshResponse response = null;
        try {
            TerrainMesh terrainMesh = meshService.loadMesh(meshName);
            if (terrainMesh == null) {
                //Hmm, should not return 200 or 404, so use 400
                response = new MeshResponse();
                response.setError("Mesh not found: " + meshName);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            response = new MeshResponse(meshName,
                    MeshResponse.buildFromPolygonList(JtsUtil.buildGeoPolygonList(terrainMesh.polygons)),
                    MeshResponse.buildFromFailureList(terrainMesh.failures));
        } catch (Exception e) {
            log.error("getMesh() failed", e);
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(response);
    }

    @CrossOrigin
    @DeleteMapping(value = "/owm/mesh")
    public ResponseEntity<MeshResponse> deleteMesh(@RequestParam(value = "meshName", required = true) String meshName) {

        MeshResponse response = null;
        try {
            boolean found = meshService.deleteMesh(meshName);
            if (!found) {
                //Hmm, should not return 200 or 404, so use 400
                response = new MeshResponse();
                response.setError("Mesh not found: " + meshName);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            response = new MeshResponse(meshName, Collections.emptyList(),Collections.EMPTY_LIST);
        } catch (Exception e) {
            log.error("deleteMesh() failed", e);
            return ResponseEntity.internalServerError().build();
        }
        // Does REST or someone define 'NOCONTENT' to be returned? Anywhy, OK seems also OK
        return ResponseEntity.ok(response);
    }

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