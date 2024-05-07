package de.yard.owm.services.persistence;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.owm.testutils.TestServices;
import de.yard.owm.testutils.TestData;
import de.yard.owm.testutils.TestUtils;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
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
public class TerrainMeshTest {

    @Autowired
    private MeshNodeRepository meshNodeRepository;

    @Autowired
    private MeshLineRepository meshLineRepository;

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

    MetricMapProjection projectionDESDORF_SW = new MetricMapProjection(TestUtils.DESDORF_SW);

    @BeforeEach
    void setUp() {
        // SQL script already deletes. Setting this here might be fatal as projection might change
        TerrainMesh.meshFactoryInstance = new PersistedMeshFactory(projectionDESDORF_SW, manager);
    }

    @AfterEach
    void teardown() {

    }

    /**
     *
     */
    @Test
    @Sql({"classpath:meshDesdorf.sql"})
    public void test1() {

        assertEquals(3, meshNodeRepository.count());

        Coordinate c = projectionDESDORF_SW.project(TestUtils.DESDORF_SW);
        PersistedMeshNode meshNode = (PersistedMeshNode) TerrainMesh.meshFactoryInstance.buildMeshNode(c);
        //meshNode.setLat(2.0);
        //meshNode.setLon(5.0);
        meshNode = meshNodeRepository.save(meshNode);

    }

    @Test
    @Sql({"classpath:meshDesdorf.sql"})
    public void testDesdorf() throws MeshInconsistencyException {

        TerrainMesh terrainMesh = TestData.prepareDesdorf(manager);
        assertNotNull(terrainMesh);
        assertEquals(3, terrainMesh.points.size());
        assertEquals(3, terrainMesh.lines.size());

        TestUtils.writeTmpSvg(terrainMesh.toSvg());

        // test some Basic Operations
        // a pure outer polygon might be the 'wrong' result of going (C)CW. But difficult to detect
        for (int i = 0; i < 3; i++) {
            MeshPolygon meshPolygon = terrainMesh.traversePolygon(terrainMesh.lines.get(i), null, true);
            assertNotNull(meshPolygon);
            assertEquals(3, meshPolygon.lines.size());
            meshPolygon = terrainMesh.traversePolygon(terrainMesh.lines.get(i), null, false);
            assertNotNull(meshPolygon);
            assertEquals(3, meshPolygon.lines.size());
        }
    }

    /**
     *
     */
    @Test
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


    }

    /**
     * Sketch 3??
     *
     * @throws OsmProcessException
     * @throws MeshInconsistencyException
     */
    @Test
    public void testSimpleRegisterWay() throws OsmProcessException, MeshInconsistencyException {

        testServices.cleanup();

        TerrainMesh terrainMesh = buildSimpleTestMesh();

        // test some Basic Operations
        // a pure outer polygon might be the 'wrong' result of going (C)CW. But difficult to detect
        MeshPolygon meshPolygon = terrainMesh.traversePolygon(terrainMesh.lines.get(0), null, true);
        assertNotNull(meshPolygon);
        assertEquals(4, meshPolygon.lines.size());
        meshPolygon = terrainMesh.traversePolygon(terrainMesh.lines.get(0), null, false);
        assertNotNull(meshPolygon);
        assertEquals(3, meshPolygon.lines.size());

        // create way not intersecting triangulation line
        List<Coordinate> leftLine = new ArrayList<>();
        leftLine.add(new Coordinate(10, 10));
        leftLine.add(new Coordinate(20, 10));
        List<Coordinate> rightLine = new ArrayList<>();
        rightLine.add(new Coordinate(10, 9));
        rightLine.add(new Coordinate(20, 9));
        MeshPolygon wayPolygon = terrainMesh.registerWay(null/*200, List.of(100L, 101L)*/, null, leftLine, rightLine, null, 1);

        TestUtils.writeTmpSvg(terrainMesh.toSvg());

        // Has 4 connector instead of 3 with sector angle 150 instead of 90
        int connector = 4;
        assertEquals(4 + 4, terrainMesh.points.size(), "points");
        assertEquals(5 + 4 + connector, terrainMesh.lines.size(), "lines");
        manager.persist(terrainMesh);

        assertEquals(1, meshAreaRepository.count());
        assertEquals(4 + 4, meshNodeRepository.count());
        assertEquals(5 + 4 + connector, meshLineRepository.count());
        //SceneryWayObject

        terrainMesh = manager.loadTerrainMesh(terrainMesh.getGridCellBounds());
        assertEquals(4 + 4, terrainMesh.points.size(), "points");
        assertEquals(5 + 4 + connector, terrainMesh.lines.size(), "lines");
        terrainMesh.validate();
        assertTrue(terrainMesh.isValid(true));
    }

    @Test
    public void testTestData2024() throws MeshInconsistencyException {

        testServices.cleanup();

        TestData testData = TestData.build2024(manager);

        String svg = testData.terrainMesh.toSvg();

        TestUtils.writeTmpSvg(svg);
    }

    /**
     * Test test data?
     * Sketch 3??
     *
     * @throws OsmProcessException
     * @throws MeshInconsistencyException
     */
    private TerrainMesh buildSimpleTestMesh() throws MeshInconsistencyException {

        double centerLat = (51);
        double centerLon = (7.0);
        double widthInDegrees = 0.001;
        double heightInDegrees = 0.001;
        TestData testData = TestData.build2024(manager, centerLat, centerLon, widthInDegrees, heightInDegrees, 0.0001, false);

        assertNotNull(testData.terrainMesh);
        assertEquals(4, testData.terrainMesh.points.size());
        assertEquals(5, testData.terrainMesh.lines.size());

        ((PersistedMeshFactory) TerrainMesh.meshFactoryInstance).projection = testData.terrainMesh.getGridCellBounds().getProjection().getBaseProjection();

        return testData.terrainMesh;

    }

}
