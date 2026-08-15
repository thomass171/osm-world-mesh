package de.yard.owm.services;

import de.yard.threed.AbstractSceneryTest;
import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.osm2mesh.Expectations;
import de.yard.threed.osm2mesh.testutils.ExpectedMeshNodePair;
import de.yard.threed.TestUtil;
import de.yard.threed.core.Degree;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.owm.services.mesh.MeshService;
import de.yard.owm.services.osm.OsmService;
import de.yard.owm.services.persistence.*;
import de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon;
import de.yard.owm.testutils.TestServices;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.MainGrid;
import de.yard.threed.osm2mesh.testutils.ValidatorServiceFacade;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.SceneryWayConnectorTest;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.OSMData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import static de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon.*;
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

    @BeforeEach
    void setUp() {
        testServices.cleanup();
    }


    @Test
    //Using maingrid appears easier finally @Sql({"classpath:meshDesdorf.sql"})
    public void testDesdorfK41Segment() throws Exception {

        //DesdorfTestData desdorfTestdata = setupForDesdorf(meshService, testServices, terrainMeshManager);
        DesdorfTestData desdorfTestdata = new DesdorfTestData(getMeshServiceFactory(),getValidatorServiceFactory());

        OsmService.Results results = osmService.populateMesh("Desdorf", desdorfTestdata.fullMapData, 24927839L);
        assertNotNull(results.sceneryMesh);

        TerrainMesh terrainMesh = meshService.loadMesh("Desdorf");

        ExpectedMeshPolygon expectedConnector = expectedConnector(255563538L, 4);

        // osmway has 5 nodes
        ExpectedMeshPolygon expectedLowerK41 = expectedWay(24927839L, 0, 10);
        testServices.validateMesh(terrainMesh,
                desdorfTestdata.expectedBoundary,
                expectedConnector,
                expectedLowerK41);

        terrainMesh.writeToSvg();
    }

    /**
     * K43 has many segments
     */
    @Test
    public void testDesdorf107468171K43() throws Exception {

        //DesdorfTestData desdorfTestdata = setupForDesdorf(meshService, testServices, terrainMeshManager);
        DesdorfTestData desdorfTestdata = new DesdorfTestData(getMeshServiceFactory(),getValidatorServiceFactory());

        OsmService.Results results = osmService.populateMesh("Desdorf", desdorfTestdata.fullMapData, 107468171L);
        assertNotNull(results.sceneryMesh);

        TerrainMesh terrainMesh = meshService.loadMesh("Desdorf");

        terrainMesh.writeToSvg();

        testServices.validateMesh(terrainMesh,
                desdorfTestdata.expectedBoundary,
                Expectations.expectedConnector445410497,
                Expectations.expectedConnector270353278,
                desdorfTestdata.expectedK43[0],
                expectedConnector(445409643, 6/*4*/),
                desdorfTestdata.expectedK43[1],
                expectedConnector(387409890, 6),
                desdorfTestdata.expectedK43[2]
                //??Expectations.expectedConnector255563537,
                //??desdorfTestdata.expectedK43[3],
                //??Expectations.expectedConnector255563538,
                //??desdorfTestdata.expectedK43[4]

                //expectedConnector255563538,
                //expectedLowerK41,
                //expectedConnector251517906,
                //expectedUpperK41s0,
                //expectedUpperK41s1,
                //expectedLConnector445410497,
        );


    }

    @Test
    public void testDesdorfStepByStep() throws Exception {

        //DesdorfTestData desdorfTestdata = setupForDesdorf(meshService, testServices, terrainMeshManager);
        DesdorfTestData desdorfTestdata = new DesdorfTestData(getMeshServiceFactory(),getValidatorServiceFactory());

        // lower K41 (full map data is needed for connector, so no .filterWay(24927839)
        OsmService.Results results = osmService.populateMesh("Desdorf", desdorfTestdata.fullMapData, 24927839L);
        assertNotNull(results.sceneryMesh);

        TerrainMesh terrainMesh = meshService.loadMesh("Desdorf");
        // osmway has 5 nodes
        ExpectedMeshPolygon expectedLowerK41 = expectedWay(24927839L, 0, 10);

        testServices.validateMesh(terrainMesh,
                desdorfTestdata.expectedBoundary,
                Expectations.expectedConnector255563538,
                expectedLowerK41);

        terrainMesh.writeToSvg();

        // upper K41
        results = osmService.populateMesh("Desdorf", desdorfTestdata.fullMapData, 182152619L);
        assertNotNull(results.sceneryMesh);

        terrainMesh = meshService.loadMesh("Desdorf");
        // osmway has 6 nodes
        ExpectedMeshPolygon expectedUpperK41s0 = expectedWay(182152619L, 0, 4);
        ExpectedMeshPolygon expectedUpperK41s1 = expectedWay(182152619L, 0, 4/*10.8.26 10*/);
        ExpectedMeshPolygon expectedConnector251517906 = expectedConnector(251517906L, 4/*no longer has type GENERIC??*/);

        testServices.validateMesh(terrainMesh,
                desdorfTestdata.expectedBoundary,
                Expectations.expectedConnector255563538,
                expectedLowerK41,
                expectedConnector251517906,
                expectedUpperK41s0,
                expectedUpperK41s1);

        // left branch
        osmService.populateMesh("Desdorf", desdorfTestdata.fullMapData, 107468171L);

        terrainMesh = meshService.loadMesh("Desdorf");
        ExpectedMeshPolygon expectedLConnector445410497 = expectedConnector(445410497L, 4);
        // osmway 107468171 has 11 nodes
        ExpectedMeshPolygon expectedK43s4 = expectedWay(107468171L, 0, 20);

        testServices.validateMesh(terrainMesh,
                desdorfTestdata.expectedBoundary,
                Expectations.expectedConnector255563538,
                expectedLowerK41,
                expectedConnector251517906,
                expectedUpperK41s0,
                expectedUpperK41s1,
                expectedLConnector445410497,
                expectedConnector(270353278, 4),
                desdorfTestdata.expectedK43[0],
                //expectedConnector(445409643, 4 or 6??),
                desdorfTestdata.expectedK43[1],
                expectedConnector(387409890, 6),
                desdorfTestdata.expectedK43[2],
                expectedConnector(255563537, 4/*??6*/)
                //??desdorfTestdata.expectedK43[3],
                //??desdorfTestdata.expectedK43[4]
        );

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