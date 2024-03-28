package de.yard.owm.services;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import de.yard.owm.services.persistence.MeshNode;
import de.yard.owm.services.persistence.MeshNodeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public class MeshNodeRepositoryTest {

    public static final String ENDPOINT_MAZES = "/mazes/mazes";

    private MockMvc mockMvc;

    @Autowired
    private MeshNodeRepository meshNodeRepository;

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
        meshNodeRepository.deleteAll();
    }

    /**
     *
     */
    @Test
    //@Sql({"classpath:testGrids.sql"})
    public void test1() {

        //assertEquals(2, mazeRepository.count());

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
