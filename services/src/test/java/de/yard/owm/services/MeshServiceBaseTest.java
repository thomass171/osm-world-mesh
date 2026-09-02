package de.yard.owm.services;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.owm.services.mesh.MeshService;
import de.yard.owm.services.persistence.*;
import de.yard.owm.testutils.TestServices;
import de.yard.owm.testutils.TestData;
import de.yard.owm.testutils.TestUtils;
import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.LatLon;
import de.yard.threed.modules.AbstractWayModuleTest;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.threed.osm2mesh.testutils.ValidatorServiceFacade;
import de.yard.threed.osm2scenery.AbstractSceneryWayConnectorTest;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MetricMapProjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Also for MeshNode, MeshLine, MeshArea, OsmNode, OsmWay and repositories
 */
@SpringBootTest(classes = {TestServices.class})
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public class MeshServiceBaseTest {

    @Autowired
    private MeshNodeRepository meshNodeRepository;

    @Autowired
    private MeshLineRepository meshLineRepository;

    @Autowired
    private MeshPolygonRepository meshPolygonRepository;

    @Autowired
    private MeshPolygonNodeRepository meshPolygonNodeRepository;

    @Autowired
    private MeshAreaRepository meshAreaRepository;

    @Autowired
    private OsmWayRepository osmWayRepository;

    @Autowired
    private OsmNodeRepository osmNodeRepository;

    @Autowired
    private OsmWayNodeRepository osmWayNodeRepository;

    @Autowired
    private TestServices testServices;

    @Autowired
    private TerrainMeshManager manager;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    MeshService meshService;

    @Autowired
    TerrainMeshManager terrainMeshManager;

    MetricMapProjection projectionDESDORF_SW = new MetricMapProjection(TestUtils.DESDORF_SW);

    @BeforeEach
    void setUp() {
        testServices.cleanup();
    }

    @AfterEach
    void teardown() {

    }

    /**
     *
     */
    @Test
    //@Sql({"classpath:meshDesdorf.sql"})
    public void testAddNode() throws Exception {

        // OsmServiceTest.setupForDesdorf(meshService, testServices, terrainMeshManager);
        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(),getValidatorServiceFactory());

        // SQL script already deletes. Setting this here might be fatal as projection might change
        TerrainMesh.meshFactoryInstance = new PersistedMeshFactory("Desdorf", projectionDESDORF_SW, manager);

        assertEquals(3, meshNodeRepository.count());

        Coordinate c = projectionDESDORF_SW.project(TestUtils.DESDORF_SW);
        PersistedMeshNode meshNode = (PersistedMeshNode) TerrainMesh.meshFactoryInstance.buildMeshNode(c);
        //meshNode.setLat(2.0);
        //meshNode.setLon(5.0);
        meshNode = meshNodeRepository.save(meshNode);
        assertEquals(4, meshNodeRepository.count());
    }

    @Test
    //@Sql({"classpath:meshDesdorf.sql"})
    public void testDesdorf() throws Exception {

        //OsmServiceTest.setupForDesdorf(meshService, testServices, terrainMeshManager);
        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(),getValidatorServiceFactory());

        // SQL script already deletes. Setting this here might be fatal as projection might change
        TerrainMesh.meshFactoryInstance = new PersistedMeshFactory("Desdorf", projectionDESDORF_SW, manager);

        TerrainMesh terrainMesh = TestData.loadDesdorf(meshService, manager);
        assertNotNull(terrainMesh);
        assertEquals(3, terrainMesh.points.size());
        assertEquals(1, terrainMesh.polygons.size());

        terrainMesh.writeToSvg();
    }

    /**
     *
     */
    /*10.2.26 @Test
    public void testOsmNodesSorted() {

        testServices.cleanup();

        int nodeCount = 7;
        int osmId = 100;
        int[] indexes = new int[]{3, 6, 2, 5, 1, 4, 0};

        PersistedOsmWay  osmWay = new PersistedOsmWay();
        osmWay.setOsmId(osmId++);
        osmWayRepository.save(osmWay);

        List<PersistedOsmNode> osmNodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            PersistedOsmNode osmNode = new PersistedOsmNode();
            osmNode.setOsmId(osmId++);
            osmNodeRepository.save(osmNode);
            osmNodes.add(osmNode);

            PersistedOsmWayNode osmWayNode = new PersistedOsmWayNode();
            osmWayNode.setId(new PersistedOsmWayNodeKey());
            osmWayNode.getId().setOsmWayId(osmWay.getId());
            osmWayNode.getId().setOsmNodeId(osmNode.getId());
            osmWayNode.setIndex(indexes[i]);
            osmWayNodeRepository.save(osmWayNode);
            //osmWay.getOsmNodes().add(osmNode);
            //osmWayRepository.save(osmWay);
        }
        assertEquals(7, osmWayNodeRepository.count());
        PersistedOsmWayNode h = osmWayNodeRepository.findAll().get(0);
        h.getOsmWay().getOsmWayNodes();

        osmWay = testServices.loadOsmWay();
        assertEquals(7, osmWay.getOsmWayNodes().size());
        for (int i = 0; i < nodeCount; i++) {
            assertEquals(i, osmWay.getOsmWayNodes().get(i).getIndex());
        }


    }*/

    /**
     * Sketch 3
     *
     * @throws OsmProcessException
     * @throws MeshInconsistencyException
     */
    @Test
    public void testSimpleRegisterWay() throws OsmProcessException, MeshInconsistencyException {

         buildSmall2024();

        // create way not intersecting any line
        List<GeoCoordinate> leftLine = new ArrayList<>();
        leftLine.add(GeoCoordinate.fromLatLon(LatLon.fromDegrees(51.0003, 7.0001)));
        leftLine.add(GeoCoordinate.fromLatLon(LatLon.fromDegrees(51.0003, 7.0003)));
        List<GeoCoordinate> rightLine = new ArrayList<>();
        rightLine.add(GeoCoordinate.fromLatLon(LatLon.fromDegrees(51.00025, 7.0001)));
        rightLine.add(GeoCoordinate.fromLatLon(LatLon.fromDegrees(51.00025, 7.0003)));
        TerrainMesh terrainMesh = meshService.addWay("small2024", -1/*200, List.of(100L, 101L)*/, null, leftLine, rightLine, null, 1);

        terrainMesh.writeToSvg();

        assertEquals(4/*boundary*/ + 4/*way*/, terrainMesh.points.size(), "points");
        assertEquals(1 + 1, terrainMesh.polygons.size(), "polygons");
        assertEquals(5, terrainMesh.polygons.get(0).getNodesSortedByIndex().size(), "polygon size");
        assertEquals(5, terrainMesh.polygons.get(1).getNodesSortedByIndex().size(), "polygon size");

        assertEquals(4 + 4, meshNodeRepository.count());
        assertEquals(2, meshPolygonRepository.count());
        assertEquals(5+5, meshPolygonNodeRepository.count());

       // terrainMesh = manager.loadTerrainMesh(terrainMesh.getGridCellBounds());
        terrainMesh = meshService.loadMesh("small2024"/*, terrainMesh.getGridCellBounds()*/);
        assertEquals(4 + 4, terrainMesh.points.size(), "points");
        assertTrue(terrainMesh.isValid());
    }

    @Test
    public void testTestData2024() throws MeshInconsistencyException {

        testServices.cleanup();

        TestData.buildLarge2024(meshService);
        assertEquals(4, meshNodeRepository.count());
        assertEquals(1, meshPolygonRepository.count());

        TerrainMesh terrainMesh = meshService.loadMesh("large2024");
        terrainMesh.writeToSvg();


    }

    /**
     * Test test data?
     * Sketch 3
     *
     * @throws OsmProcessException
     * @throws MeshInconsistencyException
     */
    private void buildSmall2024() throws MeshInconsistencyException {

        double centerLat = (51);
        double centerLon = (7.0);
        double widthInDegrees = 0.001;
        double heightInDegrees = 0.001;
        TestData.build2024(meshService, "small2024", centerLat, centerLon, widthInDegrees, heightInDegrees, 0.0001, false);



      //11.2.26   ((PersistedMeshFactory) TerrainMesh.meshFactoryInstance).projection = testData.terrainMesh.getGridCellBounds().getProjection().getBaseProjection();

       // return testData.terrainMesh;

    }


    private MeshServiceFactory getMeshServiceFactory() {
        return gridCellBounds -> MeshService.buildMeshServiceFacade();
    }

    private ValidatorServiceFactory getValidatorServiceFactory() {
        return new ValidatorServiceFactory() {
            @Override
            public ValidatorServiceFacade createService() {
                return testServices;
            }
        };
    }

}
