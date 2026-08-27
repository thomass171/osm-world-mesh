package de.yard.owm.controller;

import de.yard.owm.configuration.LoggingFilter;
import de.yard.owm.services.JsonService;
import de.yard.owm.testutils.TestServices;
import de.yard.owm.testutils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static de.yard.owm.controller.MeshControllerTest.validateResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestServices.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class OsmControllerTest {

    public static String ENDPOINT_OSM = "/worldmesh/osm";

    @Value(value = "${local.server.port}")
    private int port;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JsonService jsonService;

    @Autowired
    private LoggingFilter loggingFilter;

    @Autowired
    private TestServices testServices;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(loggingFilter)
                .build();
        testServices.cleanup();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    public void testQueryDatasets() throws Exception {

        MvcResult result = TestUtils.doGet(mockMvc, ENDPOINT_OSM + "?meshName=Desdorf");
        String response = validateResponse(result,HttpStatus.OK);
        assertTrue(response.contains("K41-segment.osm.xml"));
        assertTrue(response.contains("Desdorf.osm.xml"));

    }

    @Test
    public void testUnknownMesh() throws Exception {

        MvcResult result = TestUtils.doGet(mockMvc, ENDPOINT_OSM + "?meshName=xyz");
        String response = validateResponse(result,HttpStatus.BAD_REQUEST);
        assertTrue(response.contains("No well known mesh: xyz"));
    }

}