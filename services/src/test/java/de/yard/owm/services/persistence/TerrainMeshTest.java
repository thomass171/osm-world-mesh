package de.yard.owm.services.persistence;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import de.yard.owm.services.JsonService;
import de.yard.threed.core.LatLon;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {

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

        MeshNode meshNode = new MeshNode();
        meshNode.setLat(2.0);
        meshNode.setLon(5.0);
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
    //@Sql({"classpath:testGrids.sql"})
    public void testFindByName() throws Exception {
      /*  this.mockMvc.perform(get("/mazes/mazes/search/findByName?name=Sokoban Wikipedia")).andDo(print())
                .andExpect(content().string(containsString("##")));*/
    }
}
