package de.yard.threed.osm2mesh.testutils;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.TestUtil;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.MainGrid;
import de.yard.threed.osm2scenery.MeshServiceFacade;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.OSMData;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;

import static de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon.expectedBoundary;
import static de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon.expectedWay;

@Data
@AllArgsConstructor
public class DesdorfTestData {
    public OSMData osmData;
    public TerrainMesh terrainMesh;
    public MapData fullMapData;
    public GridCellBounds gridCellBounds;
    public ExpectedMeshPolygon expectedBoundary;

    public DesdorfTestData(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception {
        osmData = TestUtil.loadOsmDataFromXmlClasspath("Desdorf.osm.xml");

        gridCellBounds = MainGrid.buildDesdorf();

        OSMToSceneryDataConverter converter = new OSMToSceneryDataConverter(gridCellBounds.getProjection(), gridCellBounds);
        fullMapData = converter.createMapData(osmData);

        SceneryContext.init(fullMapData);

        MeshServiceFacade meshService = meshServiceFactory.createMeshService(gridCellBounds);//new MeshServiceMock(gridCellBounds);
        ValidatorServiceFacade validatorService = validatorServiceFactory.createService();

        //15.5.26 terrainMesh = TerrainMesh.init(gridCellBounds);
        meshService.createMesh("Desdorf", gridCellBounds.getBoundary());

        terrainMesh = meshService.loadMesh("Desdorf");
        expectedBoundary = expectedBoundary(3);
        validatorService.validateMesh(terrainMesh,  expectedBoundary);

        //15.5.26?? TerrainMesh.meshFactoryInstance = new PersistedMeshFactory("Desdorf", gridCellBounds.getProjection().getBaseProjection(), terrainMeshManager);
        terrainMesh.meshService = meshService;



    }
}
