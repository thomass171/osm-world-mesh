package de.yard.owm.controller;

import de.yard.owm.configuration.LoggingFilter;
import de.yard.owm.dto.FailureResponse;
import de.yard.owm.dto.MeshBuildResponse;
import de.yard.owm.dto.MeshResponse;
import de.yard.owm.dto.WebLatLon;
import de.yard.owm.services.JsonService;
import de.yard.owm.testutils.TestServices;
import de.yard.owm.testutils.TestUtils;
import de.yard.threed.TestUtil;
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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestServices.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class MeshControllerTest {

    public static String ENDPOINT_MESH = "/worldmesh/mesh";

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

    /**
     * Takes appx. 1min on my machine. The test is not really needed, but it is a good check that the whole process works.
     */
    @Test
    public void testDesdorfFull() throws Exception {

        MvcResult result = TestUtils.doPost(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf", "");
        String response = validateResponse(result, HttpStatus.OK);

        result = TestUtils.doPutXml(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf",
                TestUtil.loadFileFromClasspath("Desdorf.osm.xml"));
        response = validateResponse(result, HttpStatus.OK);
        MeshResponse meshBuildResponse = jsonService.jsonToModel(response, MeshResponse.class);
        // currently we have 5 failures, but that might change over time. Now 3.
        // Now 89
        validateFailures(89, meshBuildResponse.getFailures());

        result = TestUtils.doGet(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf");
        response = validateResponse(result, HttpStatus.OK);
    }

    @Test
    public void testDesdorfK41WithDataFile() throws Exception {

        MvcResult result = TestUtils.doPost(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf", "");
        String response = validateResponse(result, HttpStatus.OK);

        result = TestUtils.doPost(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf", "");
        response = validateResponse(result, HttpStatus.BAD_REQUEST);
        assertTrue(response.contains("already exists"));

        result = TestUtils.doPutXml(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf", "K41-segment.osm.xml");
        response = validateResponse(result, HttpStatus.OK);
        MeshBuildResponse meshBuildResponse = jsonService.jsonToModel(response, MeshBuildResponse.class);
        assertEquals(2, meshBuildResponse.getPolygons().size());

        result = TestUtils.doGet(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf");
        response = validateResponse(result, HttpStatus.OK);
    }

    @Test
    public void testLoadNotExistingMesh() throws Exception {

        MvcResult result = TestUtils.doGet(mockMvc, ENDPOINT_MESH + "?meshName=xyz");
        String response = validateResponse(result, HttpStatus.BAD_REQUEST);
        assertTrue(response.contains("not found"));

    }

    @Test
    public void testUndefinedMesh() throws Exception {

        MvcResult result = TestUtils.doPost(mockMvc, ENDPOINT_MESH + "?meshName=xyz", "");
        String response = validateResponse(result, HttpStatus.BAD_REQUEST);
        assertTrue(response.contains("No well known mesh: xyz"));

    }

    @Test
    public void testDeleteMesh() throws Exception {

        MvcResult result = TestUtils.doPost(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf", "");
        String response = validateResponse(result, HttpStatus.OK);

        result = TestUtils.doPutXml(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf", "K41-segment.osm.xml");
        response = validateResponse(result, HttpStatus.OK);

        result = TestUtils.doGet(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf");
        response = validateResponse(result, HttpStatus.OK);

        result = TestUtils.doDelete(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf");
        response = validateResponse(result, HttpStatus.OK);

        result = TestUtils.doGet(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf");
        response = validateResponse(result, HttpStatus.BAD_REQUEST);
        assertTrue(response.contains("not found"));
    }

    public static String validateResponse(MvcResult result, HttpStatus expectedHttpStatus) throws UnsupportedEncodingException {
        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        log.debug("response={}", response);
        assertEquals(expectedHttpStatus.value(), result.getResponse().getStatus());
        return response;
    }

    private void validateFailures(int expectedCount, List<FailureResponse> failures) {
        assertEquals(expectedCount, failures.size());
        for (FailureResponse r : failures) {
            if (r.getPolygon() != null) {
                //Hmm, anything to test? Just one random test for making sure it is a geo coordinate
                WebLatLon webLatLon = r.getPolygon().getPoints().get(0);
                assertTrue(webLatLon.getLat() > 50.9 && webLatLon.getLat() < 51.0, "" + webLatLon.getLat());
            }
        }
    }


}