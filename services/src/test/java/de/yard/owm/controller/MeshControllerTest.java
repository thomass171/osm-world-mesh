package de.yard.owm.controller;

import de.yard.owm.configuration.LoggingFilter;
import de.yard.owm.dto.FailureResponse;
import de.yard.owm.dto.MeshBuildResponse;
import de.yard.owm.dto.MeshResponse;
import de.yard.owm.dto.PolygonResponse;
import de.yard.owm.dto.WebLatLon;
import de.yard.owm.services.JsonService;
import de.yard.owm.testutils.TestServices;
import de.yard.owm.testutils.TestUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import de.yard.threed.TestUtil;
import de.yard.threed.osm2world.JTSConversionUtil;
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
     * Takes appx. 1min on my machine. The test is not really needed, but it is a good check that the whole process works,
     * eg. PUT for the retry of failed ways.
     */
    @Test
    public void testDesdorfFull() throws Exception {

        MvcResult result = TestUtils.doPost(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf", "");
        String response = validateResponse(result, HttpStatus.OK);

        result = TestUtils.doPutXml(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf",
                TestUtil.loadFileFromClasspath("Desdorf.osm.xml"));
        response = validateResponse(result, HttpStatus.OK);
        MeshResponse meshBuildResponse = jsonService.jsonToModel(response, MeshResponse.class);
        // currently we have 5 failures, but that might change over time. Now 3. Now 17.
        validateFailures(14, meshBuildResponse.getFailures());

        // the SVG is not part of the response, but retrievable one at a time via a dedicated endpoint
        Long failureId = meshBuildResponse.getFailures().get(0).getId();
        result = TestUtils.doGet(mockMvc, ENDPOINT_MESH + "/failure/" + failureId + "/svg");
        String svg = validateResponse(result, HttpStatus.OK);
        assertTrue(svg.contains("<svg"), svg);

        result = TestUtils.doGet(mockMvc, ENDPOINT_MESH + "/failure/99999999/svg");
        validateResponse(result, HttpStatus.NOT_FOUND);

        // retry processing of a single failed way via the optional osmwayid parameter
        String failedSourceRef = meshBuildResponse.getFailures().get(0).getSourceRef();
        String osmwayid = failedSourceRef.substring(failedSourceRef.lastIndexOf('/') + 1);
        result = TestUtils.doPutXml(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf&osmwayid=" + osmwayid,
                TestUtil.loadFileFromClasspath("Desdorf.osm.xml"));
        validateResponse(result, HttpStatus.OK);

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
    public void testPolygonsAt() throws Exception {

        // create mesh ...
        MvcResult result = TestUtils.doPost(mockMvc, ENDPOINT_MESH + "?meshName=K41", "");
        validateResponse(result, HttpStatus.OK);
        // ... and populate it
        result = TestUtils.doPutXml(mockMvc, ENDPOINT_MESH + "?meshName=K41", "K41-segment.osm.xml");
        String response = validateResponse(result, HttpStatus.OK);
        MeshResponse meshResponse = jsonService.jsonToModel(response, MeshResponse.class);

        // An interior point of each polygon must return that polygon. Only the boundary polygon is never returned.
        int found = 0;
        for (PolygonResponse polygon : meshResponse.getPolygons()) {
            Coordinate[] coordinates = polygon.getPoints().stream().map(p -> new Coordinate(p.getLon(), p.getLat())).toArray(Coordinate[]::new);
            Point interiorPoint = JTSConversionUtil.GF.createPolygon(coordinates).getInteriorPoint();
            result = TestUtils.doGet(mockMvc, ENDPOINT_MESH + "/polygon?meshName=K41&lat=" + interiorPoint.getY() + "&lon=" + interiorPoint.getX());
            response = validateResponse(result, HttpStatus.OK);
            MeshResponse polygonsAt = jsonService.jsonToModel(response, MeshResponse.class);
            polygonsAt.getPolygons().forEach(p -> assertNotEquals("BOUNDARY", p.getType()));
            if (polygonsAt.getPolygons().stream().anyMatch(p -> p.getPoints().equals(polygon.getPoints()))) {
                found++;
            }
        }
        assertEquals(meshResponse.getPolygons().size() - 1, found);

        // far away from the mesh
        result = TestUtils.doGet(mockMvc, ENDPOINT_MESH + "/polygon?meshName=K41&lat=10.0&lon=10.0");
        response = validateResponse(result, HttpStatus.OK);
        assertEquals(0, jsonService.jsonToModel(response, MeshResponse.class).getPolygons().size());

        // unknown mesh
        result = TestUtils.doGet(mockMvc, ENDPOINT_MESH + "/polygon?meshName=xyz&lat=10.0&lon=10.0");
        response = validateResponse(result, HttpStatus.BAD_REQUEST);
        assertTrue(response.contains("not found"));
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

    @Test
    public void testPutWithInvalidBody() throws Exception {

        // Just empty body.
        MvcResult result  = TestUtils.doPutXml(mockMvc, ENDPOINT_MESH + "?meshName=Desdorf&osmwayid=55",    "");
        validateResponse(result, HttpStatus.BAD_REQUEST);

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
            assertNotNull(r.getId(), "failure id");
            if (r.getPolygon() != null) {
                //Hmm, anything to test? Just one random test for making sure it is a geo coordinate
                WebLatLon webLatLon = r.getPolygon().getPoints().get(0);
                assertTrue(webLatLon.getLat() > 50.9 && webLatLon.getLat() < 51.0, "" + webLatLon.getLat());
            }
        }
    }


}