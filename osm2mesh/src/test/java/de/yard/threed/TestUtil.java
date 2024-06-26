package de.yard.threed;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Pair;
import de.yard.threed.core.Util;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.testutil.TestUtils;
import de.yard.threed.graph.Graph;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static de.yard.threed.osm2graph.osm.OsmUtil.toVector2;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        TestUtils.assertVector2( new Vector2(uvfrom.x, uvto.y), vertexData.getUV(0),"uv[0]");
        TestUtils.assertVector2(new Vector2(uvfrom.x, uvfrom.y), vertexData.getUV(1),"uv[1]");
        TestUtils.assertVector2( new Vector2(uvto.x, uvto.y), vertexData.getUV(2),"uv[2]");
        TestUtils.assertVector2(new Vector2(uvto.x, uvfrom.y), vertexData.getUV(3),"uv[3]");
    }

    public static void assertUVs(VertexData vertexData, Vector2 uv0, Vector2 uv1, Vector2 uv2, Vector2 uv3) {
        List<Coordinate> v = vertexData.vertices;

        TestUtils.assertVector2(uv0, vertexData.getUV(0),"uv[0]");
        TestUtils.assertVector2( uv1, vertexData.getUV(1),"uv[1]");
        TestUtils.assertVector2( uv2, vertexData.getUV(2),"uv[2]");
        TestUtils.assertVector2( uv3, vertexData.getUV(3),"uv[3]");
    }

    public static void assertPair(String msg, Pair<Coordinate, Coordinate> expected, Pair<Coordinate, Coordinate> actual) {
        TestUtils.assertVector2(toVector2(expected.getFirst()), toVector2(actual.getFirst()),"getFirst");
        TestUtils.assertVector2( toVector2(expected.getSecond()), toVector2(actual.getSecond()),"getSecond");
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
        if (connector.majorway0 != -1) {
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
        }

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

    public static void validateConnector(long osmid, SceneryObjectList sceneryObjects, SceneryWayConnector.WayConnectorType expectedType, Boolean expectedminorHitsLeft, TerrainMesh tm) throws MeshInconsistencyException {
        SceneryWayConnector swc = (SceneryWayConnector) sceneryObjects.findObjectByOsmId(osmid);
        validateConnector(swc, expectedType, expectedminorHitsLeft, tm);
    }

    public static void validateConnector(SceneryWayConnector swc, SceneryWayConnector.WayConnectorType expectedType, Boolean expectedminorHitsLeft, TerrainMesh tm) throws MeshInconsistencyException {
        assertNotNull(swc);
        assertEquals(expectedType, swc.getType(), swc.getOsmIdsAsString() + ".type==" + expectedType);
        if (expectedminorHitsLeft != null) {
            assertEquals(expectedminorHitsLeft.booleanValue(), swc.minorHitsLeft(swc.minorway, tm), swc.getOsmIdsAsString() + ".minorHitsLeft");
        } else {
            assertEquals(-1, swc.minorway, swc.getOsmIdsAsString() + ".minor");
        }
        TestUtil.assertNoOverlap("", swc);

        for (int i = 0; i < swc.getWaysCount(); i++) {
            SceneryWayObject way = swc.getWay(i);
            //might be eg. a bridge
            if (way.isTerrainProvider()) {
                MeshPolygon mp = null;//2.5.24tm.getPolygon(way.getArea()[0]);
                if (mp == null) {
                    int h = 9;
                }
                assertNotNull(mp, "MeshPolygon for way isType null: " + way.mapWay.getOsmId());
            }
        }
        switch (swc.getType()) {
            case SIMPLE_INNER_SINGLE_JUNCTION:
                break;
            case STANDARD_TRI_JUNCTION:
                break;
            case MOTORWAY_ENTRY_JUNCTION:
                MeshPolygon mp = null;//2.5.24tm.getPolygon(swc.getArea()[0]);
                assertEquals(6, mp.lines.size(), "connector.meshpolygon.size");
                break;
            case SIMPLE_CONNECTOR:
                if (swc.getMajor0().isClosed()) {
                    //TODO
                } else {
                    CoordinatePair expected = swc.getWayStartEndPair(swc.majorway0, tm);
                    assertPair("", expected, swc.getAttachCoordinates(swc.getMajor0().mapWay));
                    expected = swc.getWayStartEndPair(swc.majorway1, tm);
                    assertPair("", expected, swc.getAttachCoordinates(swc.getMajor1().mapWay));
                    assertPair("", swc.getWayStartEndPairInNodeOrientation(swc.majorway0, tm), swc.getWayStartEndPairInNodeOrientation(swc.majorway1, tm).swap());
                }
                break;
            case SIMPLE_SINGLE_JUNCTION:
                break;
            default:

        }


    }

    public static void validateResult(SceneryMesh sceneryMesh, Logger logger, int toleratedWarnings, int expectedBgFiller, TerrainMesh tm) throws MeshInconsistencyException {
        assertTrue(tm.isValid(true), "TerrainMesh.valid");
        assertEquals(0, SceneryContext.getInstance().unresolvedoverlaps, "unresolvedoverlaps");
        assertEquals(0, SceneryContext.getInstance().overlappingways, "overlappingways");
        assertEquals(0, SceneryContext.getInstance().overlappingterrain, "overlappingterrain");
        assertEquals(0, SceneryContext.getInstance().overlappingTerrainWithSupplements, "overlappingTerrainWithSupplements");
        assertEquals(toleratedWarnings, SceneryContext.getInstance().warnings.size(), "warnings");
        int cnt = sceneryMesh.checkForOverlappingAreas(true);
        logger.debug("" + cnt + " overlapping areas");
        assertEquals(0, cnt, "overlaps cnt");
        assertEquals(0, ScenerySupplementAreaObject.deprecatedusage, "deprecatedusage");
        assertEquals(0,tm.errorCounter, "TerrainMesh.errorCounter");
        assertEquals(0, SceneryContext.getInstance().errorCounter, "SceneryContext.errorCounter");
        assertEquals(expectedBgFiller, sceneryMesh.getBackground().bgfillersize(), "scenery.background.bgfiller");

    }

    public static void validateSupplement(String label, ScenerySupplementAreaObject supplement, TerrainMesh tm) throws MeshInconsistencyException {

        if (!supplement.isEmpty(tm)) {
            if (supplement.isTerrainProvider()) {
                MeshPolygon mp = null;//2.5.24tm.getPolygon(supplement.getArea()[0]);
                assertNotNull(mp, "MeshPolygon for supplement isType null: " + supplement.getOsmIdsAsString());
            }
        }

    }

}
