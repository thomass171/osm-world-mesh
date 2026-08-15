package de.yard.threed.osm2scenery;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.core.Degree;
import de.yard.threed.core.Pair;
import de.yard.threed.core.platform.Platform;
import de.yard.threed.core.testutil.CoreTestFactory;
import de.yard.threed.core.testutil.PlatformFactoryTestingCore;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.threed.osm2mesh.testutils.MeshServiceMock;
import de.yard.threed.osm2mesh.testutils.ValidatorServiceForMocking;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2world.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SceneryWayConnectorTest extends AbstractSceneryWayConnectorTest {

    Platform platform = CoreTestFactory.initPlatformForTest(new PlatformFactoryTestingCore(), null);



    /*What for @Test
    public void testConnector387409895() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData();
        MapWay way107468169 = desdorfTestData.fullMapData.findMapWay(107468169);

        SceneryWayConnector wayConnector = new SceneryWayConnector("RoadConnector", desdorfTestData.fullMapData.findMapNode(387409895), ASPHALT, ROAD);
        wayConnector.add(desdorfTestData.fullMapData.findMapWay(33817500).getFirstSegment());
        wayConnector.add(desdorfTestData.fullMapData.findMapWay(33817501).getFirstSegment());
        assertEquals(387409895, wayConnector.getOsmIds().get(0));

        wayConnector.classify();
        assertEquals(SceneryWayConnector.WayConnectorType.SIMPLE_CONNECTOR, wayConnector.getType());
        assertEquals(0, wayConnector.majorway0);
        assertEquals(1, wayConnector.majorway1);
        assertEquals(-1, wayConnector.minorway);
        assertEquals(-1, wayConnector.secondminor);

        // DB persist
        wayConnector.cca(desdorfTestData.terrainMesh, SceneryContext.getInstance());
        assertEquals(4 + 1, wayConnector.getPolygonLine().size());
        SvgWriter.build()
                .addPolygon(wayConnector.getPolygon(desdorfTestData.gridCellBounds.getProjection()), SvgWriter.LabelMode.NODEBYINDEX)
                .writeTmpFile();

        MeshServiceMock meshServiceMock = (MeshServiceMock) desdorfTestData.terrainMesh.meshService;
        MeshWayConnectorMock meshWayConnector = (MeshWayConnectorMock) meshServiceMock.getConnector(387409895);
        assertNotNull(meshWayConnector);
        validateAttachPoint(meshWayConnector, new MapWaySegmentKey(33817500L, 0, 0), 1, 0);
        validateAttachPoint(meshWayConnector, new MapWaySegmentKey(33817501L, 0, 0), 2, 3);
    }*/


    /*9.6.26 public static void validateAttachPoint(MeshWayConnector meshWayConnector, MapWaySegmentAtConnector key, int expectedRight, int expectedLeft) throws MeshInconsistencyException {
        Pair<Integer, Integer> ac = meshWayConnector.getAttachIndices(key.getWayOsmId(), key.getHeadingAtconnector());
        assertNotNull(ac);
        assertEquals(expectedRight, ac.getFirst(), "right");
        assertEquals(expectedLeft, ac.getSecond(), "left");
    }*/

    public static void validateAttachPoint(/*MeshWayConnector*/MeshPolygon meshWayConnector, MapWaySegmentAtConnector key, int expectedLeft, int expectedRight) throws MeshInconsistencyException {
        validateAttachPoint(meshWayConnector, key.getWayOsmId(), key.getHeadingAtconnector(), expectedLeft, expectedRight);
    }

    public static void validateAttachPoint(/*MeshWayConnector*/MeshPolygon meshWayConnector, long wayOsmId, Degree heading, int expectedLeft, int expectedRight) throws MeshInconsistencyException {
        Pair<Integer, Integer> ac = meshWayConnector.getAttachIndices(wayOsmId, heading);
        assertNotNull(ac,"attachpoints for way "+wayOsmId+" heading "+heading+" in wayconnector "+meshWayConnector);
        assertEquals(expectedLeft, ac.getSecond(), "left");
        assertEquals(expectedRight, ac.getFirst(), "right");
    }

    /*9.6.26 @Deprecated
    public static void validateAttachPoint(MeshWayConnector meshWayConnector, MapWaySegment2 segment, int expectedRight, int expectedLeft) {
        //TODO validateAttachPoint(meshWayConnector, segment.getKey(), expectedRight, expectedLeft);
    }

    @Deprecated
    public static void validateAttachPoint(MeshWayConnector meshWayConnector, MapWaySegment2 segment, int expectedLeft, int expectedRight) {
        //TODO validateAttachPointC(meshWayConnector, segment.getKey(true), expectedLeft, expectedRight);
    }*/


    @Override
    protected DesdorfTestData setupForDesdorf(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception {
        return new DesdorfTestData(meshServiceFactory, validatorServiceFactory);
    }

    @Override
    protected MeshServiceFactory getMeshServiceFactory() {
        return gridCellBounds -> new MeshServiceMock(gridCellBounds);
    }

    @Override
    protected ValidatorServiceFactory getValidatorServiceFactory() {
        return () -> new ValidatorServiceForMocking();
    }
}
