package de.yard.owm.services.persistence;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.owm.testutils.TestData;
import de.yard.owm.testutils.TestUtils;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MetricMapProjection;
import de.yard.threed.osm2world.O2WOriginMapProjection;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.traffic.geodesy.GeoCoordinate;
import de.yard.threed.traffic.geodesy.MapProjection;
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
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Also for MeshNode, MeshLine and repositories
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public class TerrainMeshTest {

    @Autowired
    private MeshNodeRepository meshNodeRepository;

    @Autowired
    private MeshLineRepository meshLineRepository;

    @Autowired
    private TerrainMeshManager manager;

    @Autowired
    private WebApplicationContext context;

    MetricMapProjection projection = new MetricMapProjection(TestUtils.DESDORF_SW);

    @BeforeEach
    void setUp() {
        // SQL script already deletes
        TerrainMesh.meshFactoryInstance = new PersistedMeshFactory(projection, manager);
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

        Coordinate c = projection.project(TestUtils.DESDORF_SW);
        PersistedMeshNode meshNode = (PersistedMeshNode) TerrainMesh.meshFactoryInstance.buildMeshNode(c);
        //meshNode.setLat(2.0);
        //meshNode.setLon(5.0);
        meshNode = meshNodeRepository.save(meshNode);

     /*
        Maze foundMaze = mazeRepository.findById(maze1.getId()).get();
        assertEquals("name", foundMaze.getName());
        assertEquals("aa", foundMaze.getGrid());
        assertEquals("sec", foundMaze.getSecret());
        assertEquals("bb", foundMaze.getDescription());
        assertEquals("P", foundMaze.getType());*/
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

    @Test
    //@Sql({"classpath:testGrids.sql"})
    public void testFindByName() throws Exception {
      /*  this.mockMvc.perform(get("/mazes/mazes/search/findByName?name=Sokoban Wikipedia")).andDo(print())
                .andExpect(content().string(containsString("##")));*/
    }

    /**
     * Scetch 3??
     *
     * @throws OsmProcessException
     * @throws MeshInconsistencyException
     */
    @Test
    public void testSimpleRegisterWay() throws OsmProcessException, MeshInconsistencyException {
        meshLineRepository.deleteAll();
        meshNodeRepository.deleteAll();

        double centerLat = (51);
        double centerLon = (7.0);
        double widthInDegrees = 0.001;
        double heightInDegrees = 0.001;
        TestData testData = TestData.build2024(manager, centerLat, centerLon, widthInDegrees, heightInDegrees, 0.0001, false);

        TerrainMesh terrainMesh = testData.terrainMesh;
        assertNotNull(terrainMesh);
        assertEquals(4, terrainMesh.points.size());
        assertEquals(5, terrainMesh.lines.size());

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
        terrainMesh.registerWay(null, leftLine, rightLine, null, 1);

        String svg = terrainMesh.toSvg();

        TestUtils.writeTmpSvg(svg);

        assertEquals(4 + 4, terrainMesh.points.size(), "points");
        assertEquals(5 + 4, terrainMesh.lines.size(), "lines");

        manager.persist(terrainMesh);
        terrainMesh = manager.loadTerrainMesh(terrainMesh.getGridCellBounds());
        assertEquals(4 + 4, terrainMesh.points.size(), "points");
        assertEquals(5 + 4, terrainMesh.lines.size(), "lines");

    }

    @Test
    public void testTestData2024() {

        meshLineRepository.deleteAll();
        meshNodeRepository.deleteAll();

        TestData testData = TestData.build2024(manager);

        String svg = testData.terrainMesh.toSvg();

        TestUtils.writeTmpSvg(svg);
    }
}
