package de.yard.threed.osm2mesh.testutils;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.TestUtil;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.LatLon;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon.expectedBoundary;
import static de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon.expectedWay;

@Data
@AllArgsConstructor
public class MeshTestData {
    // just four intuitive coordinates near "Desdorf"
    public static GeoCoordinate SW = GeoCoordinate.fromLatLon(LatLon.fromDegrees(50.94, 6.6), 0);
    public static GeoCoordinate NW = GeoCoordinate.fromLatLon(LatLon.fromDegrees(50.95, 6.6), 0);
    public static GeoCoordinate NE = GeoCoordinate.fromLatLon(LatLon.fromDegrees(50.95, 6.61), 0);
    public static GeoCoordinate SE = GeoCoordinate.fromLatLon(LatLon.fromDegrees(50.94, 6.61), 0);

    // OSM input files of 'Desdorf'
    public static final String DESDORF = "Desdorf/Desdorf.osm.xml";
    public static final String DESDORF_BRIDGE_K41_A61 = "Desdorf/bridge-K41-A61.xml";

    // the selected input, initially the first input file
    public OSMData osmData;
    public TerrainMesh terrainMesh;
    public MapData fullMapData;
    public GridCellBounds gridCellBounds;
    public ExpectedMeshPolygon expectedBoundary;
    public MeshServiceFacade meshService;
    // all inputs by file name. Each OSM file is kept separately, they are not merged.
    public Map<String, OsmInput> inputs;

    /**
     * The classic 'Desdorf' triangle. Selected by default is DESDORF.
     * The OSM files are test resources of module 'services'.
     */
    public static MeshTestData desdorf(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception {
        return new MeshTestData("Desdorf", List.of(DESDORF, DESDORF_BRIDGE_K41_A61), MainGrid.buildDesdorf(), 3, meshServiceFactory, validatorServiceFactory);
    }

    /**
     * Rectangle of 'Kenten-South'. The OSM file is a test resource of module 'services'.
     */
    public static MeshTestData kenten(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception {
        return new MeshTestData("Kenten", List.of("Kenten/Kenten-South.osm.xml"), MainGrid.buildKenten(), 4, meshServiceFactory, validatorServiceFactory);
    }

    /**
     * @param osmFilenames classpath resources, each converted to its own OSMData/MapData. The first one is selected.
     * @param expectedBoundaryNodes number of nodes of the grid boundary (3 for the Desdorf triangle, 4 for a rectangle)
     */
    public MeshTestData(String meshName, List<String> osmFilenames, GridCellBounds gridCellBounds, int expectedBoundaryNodes,
                           MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception {
        this.gridCellBounds = gridCellBounds;

        inputs = new LinkedHashMap<>();
        for (String osmFilename : osmFilenames) {
            OSMData data = TestUtil.loadOsmDataFromXmlClasspath(osmFilename);
            OSMToSceneryDataConverter converter = new OSMToSceneryDataConverter(gridCellBounds.getProjection(), gridCellBounds);
            inputs.put(osmFilename, new OsmInput(data, converter.createMapData(data)));
        }
        select(osmFilenames.get(0));

        meshService = meshServiceFactory.createMeshService(gridCellBounds);//new MeshServiceMock(gridCellBounds);
        ValidatorServiceFacade validatorService = validatorServiceFactory.createService();

        //15.5.26 terrainMesh = TerrainMesh.init(gridCellBounds);
        meshService.createMesh(meshName, gridCellBounds.getBoundary());

        terrainMesh = meshService.loadMesh(meshName);
        expectedBoundary = expectedBoundary(expectedBoundaryNodes);
        validatorService.validateMesh(terrainMesh, expectedBoundary);

        //15.5.26?? TerrainMesh.meshFactoryInstance = new PersistedMeshFactory("Desdorf", gridCellBounds.getProjection().getBaseProjection(), terrainMeshManager);
        //terrainMesh.meshService = meshService;


    }

    /**
     * Make the input from one OSM file the current 'osmData'/'fullMapData' (and SceneryContext).
     */
    public MeshTestData select(String osmFilename) {
        OsmInput input = inputs.get(osmFilename);
        if (input == null) {
            throw new IllegalArgumentException("unknown input " + osmFilename);
        }
        osmData = input.osmData;
        fullMapData = input.mapData;
        SceneryContext.init(fullMapData);
        return this;
    }

    @AllArgsConstructor
    public static class OsmInput {
        public OSMData osmData;
        public MapData mapData;
    }
}
