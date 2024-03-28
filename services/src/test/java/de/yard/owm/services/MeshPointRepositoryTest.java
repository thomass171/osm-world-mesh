package de.yard.owm.services;

import static de.yard.owm.testutils.TestUtils.loadFileFromClasspath;
import static de.yard.owm.testutils.TestUtils.validateAlmostNow;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import de.yard.owm.services.maze.Maze;
import de.yard.owm.services.maze.MazeRepository;
import de.yard.owm.services.persistence.MeshPoint;
import de.yard.owm.services.persistence.MeshPointRepository;
import de.yard.owm.testutils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.ZonedDateTime;

import static de.yard.owm.services.util.Util.buildList;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public class MeshPointRepositoryTest {

    public static final String ENDPOINT_MAZES = "/mazes/mazes";

    private MockMvc mockMvc;

    @Autowired
    private MeshPointRepository meshPointRepository;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JsonService jsonService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void teardown() {
        meshPointRepository.deleteAll();
    }

    /**
     *
     */
    @Test
    //@Sql({"classpath:testGrids.sql"})
    public void test1() {

        //assertEquals(2, mazeRepository.count());

        MeshPoint meshPoint = new MeshPoint();
        meshPoint.setLat(2.0);
        meshPoint.setLon(5.0);
        meshPoint = meshPointRepository.save(meshPoint);

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
