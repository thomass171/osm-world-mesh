package de.yard.owm.testutils;

import de.yard.threed.osm2scenery.util.OsmXmlParser;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2world.MetricMapProjection;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.core.GeoCoordinate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.MultiValueMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class TestUtils {

    // from K41 segment. elevation just guessed
    public static GeoCoordinate DESDORF_SW = GeoCoordinate.fromLatLon(LatLon.fromDegrees(50.9455, 6.59), 65.0);
    public static GeoCoordinate DESDORF_NE = GeoCoordinate.fromLatLon(LatLon.fromDegrees(50.950, 6.596), 65.0);

    public static MvcResult doGet(MockMvc mockMvc, String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                //.andDo(print()).andReturn();
                .andReturn();
        return result;
    }

    public static MvcResult doGet(MockMvc mockMvc, String url, MultiValueMap<String, String> params) throws Exception {
        MvcResult result = mockMvc.perform(get(url).params(params))
                .andDo(print()).andReturn();
        return result;
    }

    public static MvcResult doPost(MockMvc mockMvc, String url, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(body))
                .andDo(print()).andReturn();
        return result;
    }

    public static MvcResult doPostXml(MockMvc mockMvc, String url, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_XML_VALUE)
                        .content(body))
                .andDo(print()).andReturn();
        return result;
    }

    public static MvcResult doPatch(MockMvc mockMvc, String url, String body) throws Exception {
        MvcResult result = mockMvc.perform(patch(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(body))
                .andDo(print()).andReturn();
        return result;
    }

    public static MvcResult doPatchWithKey(MockMvc mockMvc, String url, String body, String key) throws Exception {
        MvcResult result = mockMvc.perform(patch(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .header("Maze-Key", key)
                        .content(body))
                .andDo(print()).andReturn();
        return result;
    }

    public static MvcResult doPut(MockMvc mockMvc, String url, String body) throws Exception {
        MvcResult result = mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(body))
                .andDo(print()).andReturn();
        return result;
    }

    public static MvcResult doPutXml(MockMvc mockMvc, String url, String body) throws Exception {
        MvcResult result = mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_XML_VALUE)
                        .content(body))
                //.andDo(print()).andReturn();
                .andReturn();
        return result;
    }

    public static MvcResult doDelete(MockMvc mockMvc, String url) throws Exception {
        MvcResult result = mockMvc.perform(delete(url))
                //.andDo(print()).andReturn();
                .andReturn();
        return result;
    }

    public static void validateAlmostNow(ZonedDateTime dateTime) {

        Duration duration = Duration.between(dateTime, ZonedDateTime.now());
        assertTrue(duration.abs().getSeconds() < 10);
    }



    /**
     * Creates a rectangluar boundary for the mesh.
     */
    public static List<GeoCoordinate> getRectangularMeshBoundary(double centerLat, double centerLon, double widthInDegrees,
                                                                 double heightInDegrees, MetricMapProjection baseProjection, double marginInDegrees) {
        // mesh boundary
        double margin = marginInDegrees;//0.01;
        GeoCoordinate topLeft = GeoCoordinate.fromLatLon(
                LatLon.fromDegrees(centerLat + heightInDegrees / 2 - margin, centerLon - widthInDegrees / 2 + margin), 0);
        GeoCoordinate topRight = GeoCoordinate.fromLatLon(
                LatLon.fromDegrees(centerLat + heightInDegrees / 2 - margin, centerLon + widthInDegrees / 2 - margin), 0);
        GeoCoordinate bottomRight = GeoCoordinate.fromLatLon(
                LatLon.fromDegrees(centerLat - heightInDegrees / 2 + margin, centerLon + widthInDegrees / 2 - margin), 0);
        GeoCoordinate bottomLeft = GeoCoordinate.fromLatLon(
                LatLon.fromDegrees(centerLat - heightInDegrees / 2 + margin, centerLon - widthInDegrees / 2 + margin), 0);
        if (topLeft.getLonDeg().getDegree() > topRight.getLonDeg().getDegree()) {
            throw new RuntimeException("left > right");
        }
        if (bottomLeft.getLatDeg().getDegree() > topRight.getLatDeg().getDegree()) {
            throw new RuntimeException("left > right");
        }
       /* terrainMesh.points.add(topLeft);
        terrainMesh.points.add(topRight);
        terrainMesh.points.add(bottomRight);
        terrainMesh.points.add(bottomLeft);*/

        /*terrainMesh.lines.add(new PersistedMeshLine(topLeft, topRight));
        terrainMesh.lines.add(new PersistedMeshLine(topRight, bottomRight));
        terrainMesh.lines.add(new PersistedMeshLine(bottomRight, bottomLeft));
        terrainMesh.lines.add(new PersistedMeshLine(bottomLeft, topLeft));*/

        // for now do a manual triangulation
        //List<TriangleXZ> trianglesXZ = EarClippingTriangulationUtil.triangulate(outerPolygon, holes);
        // MeshLine triLine = new PersistedMeshLine(bottomRight, topLeft);
        //triLine.setType(2);

        //terrainMesh.lines.add(triLine);
        return List.of(topLeft, topRight, bottomRight, bottomLeft);
    }


}
