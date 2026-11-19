package de.yard.threed.modules;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.osm2mesh.testutils.DefaultMockingSceneryTest;
import de.yard.threed.osm2mesh.testutils.MeshTestData;
import de.yard.threed.osm2scenery.modules.Bridge;
import de.yard.threed.osm2world.MapWay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

}
