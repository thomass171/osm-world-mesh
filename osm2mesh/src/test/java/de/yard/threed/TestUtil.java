package de.yard.threed;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Pair;
import de.yard.threed.core.Util;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.testutil.TestUtils;
import de.yard.threed.graph.Graph;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2mesh.testutils.ExpectedMeshNodePair;
import de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.polygon20.MeshPolygonOld;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.util.OsmXmlParser;
import de.yard.threed.osm2world.OSMData;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static de.yard.threed.osm2graph.osm.OsmUtil.toVector2;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class TestUtil {

    public static void assertEleConnectorGroup(String label, EleConnectorGroupSet eleConnectorGroupSet, float[] expected) {
        int i = 0;
        for (EleConnectorGroup eg : eleConnectorGroupSet.eleconnectorgroups) {
            assertEquals(expected[i++], eg.getElevation().floatValue(), label);
        }
    }

    /**
     * Assert minimum elavation for all graph nodes.
     * Nur nodes mit edges dran pruefen, weil es auch verwaiste Nodes ausserhalb des Grid ohne elevation geben kann.
     */
    public static void assertBasicGraph(Graph graph, float minelevationexpected) {
        for (int j = 0; j < graph.getNodeCount(); j++) {
            if (graph.getNode(j).getEdgeCount() > 0) {
                if (minelevationexpected > graph.getNode(j).getLocation().getZ()) {
                    int i = 99;
                }
                assertTrue(minelevationexpected <= graph.getNode(j).getLocation().getZ(), "graph.vertex.z[" + j + "]");
            }
        }
    }

    public static void assertTriangleStrip(VertexData vertexData, Vector2 uvfrom, Vector2 uvto) {
        List<Coordinate> v = vertexData.vertices;
        if (v.size() % 2 != 0) {
            fail("no triangle strip");
        }
        if (vertexData.vertices.size() != 4) {
            Util.notyet();
            //TODO andere als 4!
        }
        TestUtils.assertIndices("segment0.indices[]", new int[]{0, 1, 2, 2, 1, 3}, vertexData.indices);

        /*tricky for (int i=0;i<v.size();i++){
            Vector2 expected;
            if (i%2==0){
              expected=new Vector2(uvfrom.x)
            }
            TestUtil.assertVector2("uv",expected, vertexData.getUV(i));
        }*/
        TestUtils.assertVector2(new Vector2(uvfrom.x, uvto.y), vertexData.getUV(0), "uv[0]");
        TestUtils.assertVector2(new Vector2(uvfrom.x, uvfrom.y), vertexData.getUV(1), "uv[1]");
        TestUtils.assertVector2(new Vector2(uvto.x, uvto.y), vertexData.getUV(2), "uv[2]");
        TestUtils.assertVector2(new Vector2(uvto.x, uvfrom.y), vertexData.getUV(3), "uv[3]");
    }

    public static void assertUVs(VertexData vertexData, Vector2 uv0, Vector2 uv1, Vector2 uv2, Vector2 uv3) {
        List<Coordinate> v = vertexData.vertices;

        TestUtils.assertVector2(uv0, vertexData.getUV(0), "uv[0]");
        TestUtils.assertVector2(uv1, vertexData.getUV(1), "uv[1]");
        TestUtils.assertVector2(uv2, vertexData.getUV(2), "uv[2]");
        TestUtils.assertVector2(uv3, vertexData.getUV(3), "uv[3]");
    }

    public static void assertPair(String msg, Pair<Coordinate, Coordinate> expected, Pair<Coordinate, Coordinate> actual) {
        TestUtils.assertVector2(toVector2(expected.getFirst()), toVector2(actual.getFirst()), "getFirst");
        TestUtils.assertVector2(toVector2(expected.getSecond()), toVector2(actual.getSecond()), "getSecond");
    }

    /**
     * ohne z!
     */
    public static void assertCoordinate(String label, Coordinate expected, Coordinate actual) {
        assertEquals(expected.x, actual.x, label + ".x");
        assertEquals(expected.y, actual.y, label + ".y");
    }

    public static void assertCoordinate(String label, Coordinate expected, Coordinate actual, double tolerance) {
        assertEquals(expected.x, actual.x, tolerance, label + ".x");
        assertEquals(expected.y, actual.y, tolerance, label + ".y");
    }

    public static void assertNoOverlap(String label, SceneryWayConnector connector) {
        List<AbstractArea> areas = new ArrayList();
        areas.add(connector.getArea()[0]);
        boolean hasMajor0 = false;
       /*23.2.26 TODO? if (connector.majorway0 != -1) {
            areas.add(connector.getMajor0().getArea()[0]);
            hasMajor0 = true;
        }
        if (connector.majorway1 != -1) {
            //for closed ways both main areas are the same, so don't check for overlap
            if (!hasMajor0 || connector.getMajor0() != connector.getMajor1()) {
                areas.add(connector.getMajor1().getArea()[0]);
            }
        }
        if (connector.minorway != -1) {
            areas.add(connector.getWay(connector.minorway).getArea()[0]);
        }*/

        for (int i = 0; i < areas.size(); i++) {
            for (int j = 0; j < areas.size(); j++) {
                if (i != j) {
                    if (areas.get(i).overlaps(areas.get(j))) {
                        fail(label + ": overlap in connector " + connector.getOsmIdsAsString());
                    }
                }
            }
        }
    }

    @Deprecated
    public static void validateConnector(long osmid, SceneryObjectList sceneryObjects, SceneryWayConnector.WayConnectorType expectedType, Boolean expectedminorHitsLeft, TerrainMesh tm) throws MeshInconsistencyException {
        SceneryWayConnector swc = (SceneryWayConnector) sceneryObjects.findObjectByOsmId(osmid);
        validateConnector(swc, expectedType, expectedminorHitsLeft);
    }

    public static void validateConnector(SceneryWayConnector swc, SceneryWayConnector.WayConnectorType expectedType, Boolean expectedminorHitsLeft/*, TerrainMesh tm*/) throws MeshInconsistencyException {
        assertNotNull(swc);
        assertEquals(expectedType, swc.getType(), swc.getOsmIdsAsString() + ".type==" + expectedType);
        /*TODO 9.6.26 how is this defined? Still needed? if (expectedminorHitsLeft != null) {
            assertEquals(expectedminorHitsLeft.booleanValue(), swc.minorHitsLeft(swc.minorway0), swc.getOsmIdsAsString() + ".minorHitsLeft");
        } else {
            assertEquals(-1, swc.minorway0, swc.getOsmIdsAsString() + ".minor");
        }*/
        TestUtil.assertNoOverlap("", swc);

        for (int i = 0; i < swc.getWaysCount(); i++) {
          /*23.2.26 TODO?  SceneryWayObject way = swc.getWay(i);
            //might be eg. a bridge
            if (way.isTerrainProvider()) {
                MeshPolygonOld mp = null;//2.5.24tm.getPolygon(way.getArea()[0]);
                if (mp == null) {
                    int h = 9;
                }
                assertNotNull(mp, "MeshPolygon for way isType null: " + way.mapWay.getOsmId());
            }*/
        }
        switch (swc.getType()) {
            /*19.3.26 case SIMPLE_INNER_SINGLE_JUNCTION:
                break;*/
            case STANDARD_JUNCTION:
                break;
            case MOTORWAY_ENTRY_JUNCTION:
                MeshPolygonOld mp = null;//2.5.24tm.getPolygon(swc.getArea()[0]);
                assertEquals(6, mp.lines.size(), "connector.meshpolygon.size");
                break;
            case SIMPLE_CONNECTOR:
               /*23.2.26 TODO if (swc.getMajor0().isClosed()) {
                    //TODO
                } else {
                    CoordinatePair expected = swc.getWayStartEndPair(swc.majorway0, tm);
                    assertPair("", expected, swc.getAttachCoordinates(swc.getMajor0().mapWay));
                    expected = swc.getWayStartEndPair(swc.majorway1, tm);
                    assertPair("", expected, swc.getAttachCoordinates(swc.getMajor1().mapWay));
                    assertPair("", swc.getWayStartEndPairInNodeOrientation(swc.majorway0, tm), swc.getWayStartEndPairInNodeOrientation(swc.majorway1, tm).swap());
                }*/
                break;
            case SIMPLE_JUNCTION:
                break;
            default:

        }


    }

    public static void validateResult(SceneryMesh sceneryMesh, int toleratedWarnings, int expectedBgFiller, TerrainMesh tm) throws MeshInconsistencyException {
        assertTrue(tm.isValid(), "TerrainMesh.valid");
        assertEquals(0, SceneryContext.getInstance().unresolvedoverlaps, "unresolvedoverlaps");
        assertEquals(0, SceneryContext.getInstance().overlappingways, "overlappingways");
        assertEquals(0, SceneryContext.getInstance().overlappingterrain, "overlappingterrain");
        assertEquals(0, SceneryContext.getInstance().overlappingTerrainWithSupplements, "overlappingTerrainWithSupplements");
        assertEquals(toleratedWarnings, SceneryContext.getInstance().warnings.size(), "warnings");
        int cnt = sceneryMesh.checkForOverlappingAreas(true);
        log.debug("" + cnt + " overlapping areas");
        assertEquals(0, cnt, "overlaps cnt");
        assertEquals(0, ScenerySupplementAreaObject.deprecatedusage, "deprecatedusage");
        assertEquals(0, tm.errorCounter, "TerrainMesh.errorCounter");
        assertEquals(0, SceneryContext.getInstance().errorCounter, "SceneryContext.errorCounter");
        assertEquals(expectedBgFiller, sceneryMesh.getBackground().bgfillersize(), "scenery.background.bgfiller");

    }

    public static void validateSupplement(String label, ScenerySupplementAreaObject supplement, TerrainMesh tm) throws MeshInconsistencyException {

        if (!supplement.isEmpty(tm)) {
            if (supplement.isTerrainProvider()) {
                MeshPolygonOld mp = null;//2.5.24tm.getPolygon(supplement.getArea()[0]);
                assertNotNull(mp, "MeshPolygon for supplement isType null: " + supplement.getOsmIdsAsString());
            }
        }

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

    public static OSMData loadOsmDataFromXmlClasspath(String resource) throws Exception {
        String xml = loadFileFromClasspath(resource);
        OsmXmlParser parser = new OsmXmlParser(xml);
        OSMData osmData = parser.getData();
        return osmData;
    }

    public static void writeTmpSvg(String svg) {

        // string -> bytes
        try {
            Files.write(Paths.get("/Users/thomas/tmp/tmp.svg"), svg.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void assertDesdorfK41K43connector(SceneryWayConnector k41k43connector) throws MeshInconsistencyException {

        assertNotNull(k41k43connector.getArea(), "k41k43connector.area");
        //TODO assertEquals(5, k41k43connector.getArea()[0].getPolygon(sceneryMesh.terrainMesh).getCoordinates().length, "k41k43connector.polygon.size");
        validateConnector(k41k43connector, SceneryWayConnector.WayConnectorType.STANDARD_JUNCTION, Boolean.TRUE);

    }

    public static void validateMeshPolygon(ExpectedMeshPolygon ep, MeshPolygon p) throws MeshInconsistencyException {
        assertNotNull(p);
        assertEquals(ep.type, p.getType());
        assertEquals(ep.nodes, p.getNodesSortedByIndex().size() - 1, "polygon nodes for " +
                ep.osmId + "[" + ep.segmentIndex + "]");

        for (ExpectedMeshNodePair emnp : ep.expectedMeshNodePairs) {
            Pair<Integer, Integer> pair = p.getAttachIndices(emnp.osmWayId, emnp.heading);
            assertNotNull(pair, "pair for " + emnp + " in polygon " + p);
            // first=right
            assertEquals(emnp.expectedLeft, pair.getSecond(), "left of " + emnp);
            assertEquals(emnp.expectedRight, pair.getFirst(), "right of " + emnp);
        }
        //TODO assertEquals(0, p.getNodePairs().size());
    }
}
