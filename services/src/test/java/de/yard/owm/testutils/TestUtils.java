package de.yard.owm.testutils;

import de.yard.owm.services.persistence.PersistedMeshFactory;
import de.yard.owm.services.persistence.PersistedMeshLine;
import de.yard.owm.services.persistence.PersistedMeshNode;
import de.yard.owm.services.persistence.TerrainMeshManager;
import de.yard.owm.services.util.OsmXmlParser;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.EarClippingTriangulationUtil;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MetricMapProjection;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.TriangleXZ;
import de.yard.threed.traffic.geodesy.GeoCoordinate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class TestUtils {

    // from K41 segment. elevation just guessed
    public static GeoCoordinate DESDORF_SW = GeoCoordinate.fromLatLon(LatLon.fromDegrees(50.9455, 6.59), 65.0);
    public static GeoCoordinate DESDORF_NE = GeoCoordinate.fromLatLon(LatLon.fromDegrees(50.950, 6.596), 65.0);

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

    public static String loadFileFromClasspath(String fileName) throws Exception {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        return textBuilder.toString();
    }

    public static void validateAlmostNow(ZonedDateTime dateTime) {

        Duration duration = Duration.between(dateTime, ZonedDateTime.now());
        assertTrue(duration.abs().getSeconds() < 10);
    }

    public static void writeTmpSvg(String svg) {

        // string -> bytes
        try {
            Files.write(Paths.get("/Users/thomas/tmp/tmp.svg"), svg.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void addTerrainMeshBoundary(TerrainMesh terrainMesh, double centerLat, double centerLon, double widthInDegrees,
                                              double heightInDegrees, MetricMapProjection baseProjection, double marginInDegrees) {
        // mesh boundary
        double margin = marginInDegrees;//0.01;
        PersistedMeshNode topLeft = PersistedMeshNode.build(GeoCoordinate.fromLatLon(
                LatLon.fromDegrees(centerLat + heightInDegrees / 2 - margin, centerLon - widthInDegrees / 2 + margin), 0),
                baseProjection);
        PersistedMeshNode topRight = PersistedMeshNode.build(GeoCoordinate.fromLatLon(
                LatLon.fromDegrees(centerLat + heightInDegrees / 2 - margin, centerLon + widthInDegrees / 2 - margin), 0),
                baseProjection);
        PersistedMeshNode bottomRight = PersistedMeshNode.build(GeoCoordinate.fromLatLon(
                LatLon.fromDegrees(centerLat - heightInDegrees / 2 + margin, centerLon + widthInDegrees / 2 - margin), 0),
                baseProjection);
        PersistedMeshNode bottomLeft = PersistedMeshNode.build(GeoCoordinate.fromLatLon(
                LatLon.fromDegrees(centerLat - heightInDegrees / 2 + margin, centerLon - widthInDegrees / 2 + margin), 0),
                baseProjection);
        if (topLeft.getLon() > topRight.getLon()){
            throw new RuntimeException("left > right");
        }
        if (bottomLeft.getLat() > topRight.getLat()){
            throw new RuntimeException("left > right");
        }
        terrainMesh.points.add(topLeft);
        terrainMesh.points.add(topRight);
        terrainMesh.points.add(bottomRight);
        terrainMesh.points.add(bottomLeft);
        terrainMesh.lines.add(new PersistedMeshLine(topLeft, topRight));
        terrainMesh.lines.add(new PersistedMeshLine(topRight, bottomRight));
        terrainMesh.lines.add(new PersistedMeshLine(bottomRight, bottomLeft));
        terrainMesh.lines.add(new PersistedMeshLine(bottomLeft, topLeft));

        // for now do a manual triangulation
        //List<TriangleXZ> trianglesXZ = EarClippingTriangulationUtil.triangulate(outerPolygon, holes);
        MeshLine triLine = new PersistedMeshLine(bottomRight, topLeft);
        triLine.setType(2);
        terrainMesh.lines.add(triLine);
    }
}
