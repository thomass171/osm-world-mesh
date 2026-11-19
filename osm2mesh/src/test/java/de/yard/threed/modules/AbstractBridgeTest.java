package de.yard.threed.modules;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.osm2mesh.testutils.DefaultMockingSceneryTest;
import de.yard.threed.osm2mesh.testutils.MeshTestData;
import de.yard.threed.osm2scenery.modules.Bridge;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.MapWaySegment2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractBridgeTest extends DefaultMockingSceneryTest {

    protected abstract MeshTestData setupForDesdorf(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception;

    MeshTestData desdorfTestData;

    @BeforeEach
    public void setUp() throws Exception {
        desdorfTestData = setupForDesdorf(getMeshServiceFactory(), getValidatorServiceFactory());
    }

    /**
     * K41 bridge over the A61 (from 'Desdorf/bridge-K41-A61.xml').
     */
    @Test
    public void testBridge23245301() throws Exception {

        MapWay way = desdorfTestData.findMapWay(MeshTestData.DESDORF_BRIDGE_K41_A61, 23245301);
        assertNotNull(way);
        assertTrue(Bridge.isBridge(way.getTags()));
    }

    /**
     * The K41 bridge 23245301 crosses both carriageways of the A61.
     */
    @Test
    public void testFindWaysBelow23245301() throws Exception {

        MapData mapData = desdorfTestData.getMapData(MeshTestData.DESDORF_BRIDGE_K41_A61);

        assertEquals(List.of(33816682L, 34442466L), findOsmIdsOfWaysBelow(mapData.findMapWay(23245301), mapData));
        // The A61 is below the bridge, so nothing is below the A61.
        assertEquals(List.of(), findOsmIdsOfWaysBelow(mapData.findMapWay(33816682), mapData));
    }

    /**
     * Sorted OSM ids of the ways below all segments of a way.
     */
    private List<Long> findOsmIdsOfWaysBelow(MapWay way, MapData mapData) {
        Set<Long> osmIds = new TreeSet<>();
        for (MapWaySegment2 segment : way.segment2s) {
            for (MapWay below : Bridge.findWaysBelow(segment, mapData)) {
                osmIds.add(below.getOsmId());
            }
        }
        return new ArrayList<>(osmIds);
    }
}
