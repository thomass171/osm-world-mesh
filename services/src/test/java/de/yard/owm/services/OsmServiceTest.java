package de.yard.owm.services;

import de.yard.threed.AbstractSceneryTest;
import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.osm2mesh.DesdorfExpectations;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.owm.services.mesh.MeshService;
import de.yard.owm.services.osm.OsmService;
import de.yard.owm.services.persistence.*;
import de.yard.owm.testutils.TestServices;
import de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon;
import de.yard.threed.osm2mesh.testutils.ValidatorServiceFacade;
import de.yard.threed.osm2scenery.polygon20.MeshPolygonType;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import static de.yard.threed.osm2mesh.DesdorfExpectations.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@SpringBootTest(classes = TestServices.class)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@Slf4j
public class OsmServiceTest extends AbstractSceneryTest {

    @Autowired
    private OsmService osmService;

    @Autowired
    private MeshService meshService;

    @Autowired
    TerrainMeshManager terrainMeshManager;

    @Autowired
    private MeshNodeRepository meshNodeRepository;

    @Autowired
    private MeshLineRepository meshLineRepository;

    @Autowired
    private MeshPolygonRepository meshPolygonRepository;

    @Autowired
    private MeshPolygonNodeRepository meshPolygonNodeRepository;

    @Autowired
    private TestServices testServices;

    @Autowired
    private PlatformService platformService;
    private DesdorfTestData desdorfTestData;

    @BeforeEach
    void setUp() throws Exception {
        testServices.cleanup();
        desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());
    }


    @Test
    //Using maingrid appears easier finally @Sql({"classpath:meshDesdorf.sql"})
    public void testDesdorfK41Segment() throws Exception {

        OsmService.Results results = osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 24927839L);
        assertEquals(1, results.getProcessCounter());
        assertEquals(0, results.getFailureCounter());

        TerrainMesh terrainMesh = meshService.loadMesh("Desdorf");

        testServices.validateMesh(terrainMesh,
                desdorfTestData.expectedBoundary,
                expectedConnector255563538,
                expected24927839LowerK41);

        terrainMesh.writeToSvg();

        assertNull(terrainMesh.isValid());
    }

    /**
     * K43 has many segments
     */
    @Test
    public void testDesdorf107468171K43() throws Exception {

        OsmService.Results results = osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 107468171L);
        assertEquals(1, results.getProcessCounter());
        assertEquals(0, results.getFailureCounter());

        TerrainMesh terrainMesh = meshService.loadMesh("Desdorf");

        terrainMesh.writeToSvg();

        testServices.validateMesh(terrainMesh,
                desdorfTestData.expectedBoundary,
                expectedConnector445410497,
                expectedConnector270353278,
                expectedK43[0],
                expectedConnector445409643,
                expectedK43[1],
                expectedConnector387409890,
                expectedK43[2],
                expectedConnector255563537,
                expectedK43[3],
                expectedConnector255563538,
                expectedK43[4]
        );

        // try again with the same data. It should fail because the polygon already exists.
        results = osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 107468171L);
        assertEquals(0, results.getProcessCounter());
        assertEquals(1, results.getFailureCounter());
        assertEquals(1, results.getFailures().size());

        // delete and retry. Should work now.
        meshService.deletePolygon(107468171L);
        results = osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 107468171L);
        assertEquals(1, results.getProcessCounter());
        assertEquals(0, results.getFailureCounter());
        assertEquals(0, results.getFailures().size());

    }

    @Test
    public void testDesdorfStepByStep() throws Exception {

        // lower K41 (full map data is needed for connector, so no filterWay(24927839)
        OsmService.Results results = osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 24927839L);
        assertEquals(1, results.getProcessCounter());
        assertEquals(0, results.getFailureCounter());

        TerrainMesh terrainMesh = meshService.loadMesh("Desdorf");

        testServices.validateMesh(terrainMesh,
                desdorfTestData.expectedBoundary,
                expectedConnector255563538,
                expected24927839LowerK41);

        terrainMesh.writeToSvg();

        // upper K41
        results = osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 182152619L);
        assertEquals(1, results.getProcessCounter());
        assertEquals(0, results.getFailureCounter());

        terrainMesh = meshService.loadMesh("Desdorf");

        testServices.validateMesh(terrainMesh,
                desdorfTestData.expectedBoundary,
                expectedConnector255563538,
                expected24927839LowerK41,
                expectedConnector251517906,
                expectedUpperK41s0,
                expectedUpperK41s1);

        // left branch
        osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 107468171L);

        terrainMesh = meshService.loadMesh("Desdorf");

        testServices.validateMesh(terrainMesh,
                desdorfTestData.expectedBoundary,
                DesdorfExpectations.expectedConnector255563538,
                expected24927839LowerK41,
                expectedConnector251517906,
                expectedUpperK41s0,
                expectedUpperK41s1,
                expectedConnector445410497,
                expectedConnector270353278,
                expectedK43[0],
                expectedConnector445409643,
                expectedK43[1],
                expectedConnector387409890,
                expectedK43[2],
                expectedConnector255563537,
                expectedK43[3],
                expectedK43[4]
        );

        // forest
        osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 322751236L);
        terrainMesh = meshService.loadMesh("Desdorf");
        testServices.validateMesh(terrainMesh,
                desdorfTestData.expectedBoundary,
                ExpectedMeshPolygon.expectedArea(322751236L, MeshPolygonType.AREA, 20));

        // building
        osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 322751224L);
        terrainMesh = meshService.loadMesh("Desdorf");
        testServices.validateMesh(terrainMesh,
                desdorfTestData.expectedBoundary,
                ExpectedMeshPolygon.expectedArea(322751224L, MeshPolygonType.AREA, 7));

        terrainMesh.writeToSvg();
    }

    /**
     * Takes appx. 1min on my machine. The test is not really needed, but it is a good check that the whole process works.
     */
    @Test
    public void testDesdorfFull() throws Exception {

        OsmService.Results results = osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, null);

        TerrainMesh terrainMesh = meshService.loadMesh("Desdorf");

        testServices.validateMesh(terrainMesh,
                desdorfTestData.expectedBoundary,
                DesdorfExpectations.expectedConnector255563538,
                expected24927839LowerK41,
                expectedConnector251517906,
                expectedUpperK41s0,
                expectedUpperK41s1,
                expectedConnector445410497,
                expectedConnector270353278,
                expectedK43[0],
                expectedConnector445409643,
                expectedK43[1],
                expectedConnector387409890,
                expectedK43[2],
                expectedConnector255563537,
                expectedK43[3],
                expectedK43[4]
        );

        terrainMesh.writeToSvg();
    }

    @Test
    public void testDesdorfAreaSplit() throws Exception {

        OsmService.Results results = osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 107468171L);
        assertEquals(1, results.getProcessCounter());
        assertEquals(0, results.getFailureCounter());

        TerrainMesh terrainMesh = meshService.loadMesh("Desdorf");

        testServices.validateMesh(terrainMesh,
                desdorfTestData.expectedBoundary,
                DesdorfExpectations.expectedConnector255563538,
                expectedK43[0],
                expectedConnector445409643,
                expectedK43[1],
                expectedConnector387409890,
                expectedK43[2],
                expectedConnector255563537,
                expectedK43[3],
                expectedK43[4]
        );

        // farmland 'below' K43. It needs to be split into 2 areas.
        results = osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 87822834L);
        assertEquals(1, results.getProcessCounter());
        assertEquals(0, results.getFailureCounter());

        terrainMesh = meshService.loadMesh("Desdorf");
        testServices.validateMesh(terrainMesh,
                desdorfTestData.expectedBoundary,
                ExpectedMeshPolygon.expectedArea(87822834L, MeshPolygonType.AREA, 38/*appears to be correct)*/));

        // farmland 87822834L 'below' K43 needs to be split into 2 areas by way 37935545.
        results = osmService.populateMesh("Desdorf", desdorfTestData.fullMapData, 37935545L);
        /*TODO assertEquals(1, results.getProcessCounter());
        assertEquals(0, results.getFailureCounter());*/

        terrainMesh.writeToSvg();
    }

    /*17.5.26 public static DesdorfTestData setupForDesdorf(MeshService meshService, TestServices testServices, TerrainMeshManager terrainMeshManager) throws Exception {
        // The 'lower' segment. Full data will provide one connector
        //OSMData osmData = TestUtil.loadOsmDataFromXmlClasspath("K41-segment.osm.xml");
        OSMData osmData = TestUtil.loadOsmDataFromXmlClasspath("Desdorf.osm.xml");

        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider");
        customconfig.setProperty("modules.HighwayModule.tagfilter", "highway=secondary");

        GridCellBounds gridCellBounds = MainGrid.buildDesdorf();
        meshService.createMesh("Desdorf", gridCellBounds.getBoundary());
        //temeshService.loadMesh("Desdorf", gridCellBounds.getBoundary());

        TerrainMesh terrainMesh = meshService.loadMesh("Desdorf");
        ExpectedMeshPolygon expectedBoundary = expectedBoundary(3);
        testServices.validateMesh(terrainMesh,
                expectedBoundary);

        TerrainMesh.meshFactoryInstance = new PersistedMeshFactory("Desdorf", gridCellBounds.getProjection().getBaseProjection(), terrainMeshManager);

        OSMToSceneryDataConverter converter = new OSMToSceneryDataConverter(gridCellBounds.getProjection(), gridCellBounds);
        MapData mapData = converter.createMapData(osmData);

        ExpectedMeshPolygon[] expectedK43 = new ExpectedMeshPolygon[]{
                expectedWay(107468171L, 0, 10),
                expectedWay(107468171L, 1, 6),
                expectedWay(107468171L, 2, 4),
                expectedWay(107468171L, 3, 4),
                expectedWay(107468171L, 4, 4)
        };

        return new DesdorfTestdata(expectedBoundary, mapData, expectedK43);
    }

    /*17.5.26 public static DesdorfTestData setupForDesdorf(MeshService meshService, TestServices testServices, TerrainMeshManager terrainMeshManager) throws Exception {
        // The 'lower' segment. Full data will provide one connector
        //OSMData osmData = TestUtil.loadOsmDataFromXmlClasspath("K41-segment.osm.xml");
        OSMData osmData = TestUtil.loadOsmDataFromXmlClasspath("Desdorf.osm.xml");

        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider");
        customconfig.setProperty("modules.HighwayModule.tagfilter", "highway=secondary");

        GridCellBounds gridCellBounds = MainGrid.buildDesdorf();
        meshService.createMesh("Desdorf", gridCellBounds.getBoundary());
        //temeshService.loadMesh("Desdorf", gridCellBounds.getBoundary());

        TerrainMesh terrainMesh = meshService.loadMesh("Desdorf");
        ExpectedMeshPolygon expectedBoundary = expectedBoundary(3);
        testServices.validateMesh(terrainMesh,
                expectedBoundary);

        TerrainMesh.meshFactoryInstance = new PersistedMeshFactory("Desdorf", gridCellBounds.getProjection().getBaseProjection(), terrainMeshManager);

        OSMToSceneryDataConverter converter = new OSMToSceneryDataConverter(gridCellBounds.getProjection(), gridCellBounds);
        MapData mapData = converter.createMapData(osmData);

        ExpectedMeshPolygon[] expectedK43 = new ExpectedMeshPolygon[]{
                expectedWay(107468171L, 0, 10),
                expectedWay(107468171L, 1, 6),
                expectedWay(107468171L, 2, 4),
                expectedWay(107468171L, 3, 4),
                expectedWay(107468171L, 4, 4)
        };

        return new DesdorfTestdata(expectedBoundary, mapData, expectedK43);
    }*/

    /*14.5.26 @Data
    @AllArgsConstructor
    public static class DesdorfTestdata {
        ExpectedMeshPolygon expectedBoundary;
        public MapData fullMapData;
        public ExpectedMeshPolygon[] expectedK43;

    }*/
    @Override
    protected MeshServiceFactory getMeshServiceFactory() {
        return gridCellBounds -> MeshService.buildMeshServiceFacade();
    }

    @Override
    protected ValidatorServiceFactory getValidatorServiceFactory() {
        return new ValidatorServiceFactory() {
            @Override
            public ValidatorServiceFacade createService() {
                return testServices;
            }
        };
    }

}