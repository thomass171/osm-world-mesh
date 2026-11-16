package de.yard.threed.osm2scenery.modules;

import de.yard.threed.osm2scenery.scenery.LandClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * There is no test data containing waterways yet, so the land class of WaterModule can only be checked here.
 */
public class WayModuleLandClassTest {

    @Test
    public void testLandClass() {
        assertEquals(LandClass.Asphalt, new HighwayModule(null, null, "test").getLandClass());
        assertEquals(LandClass.RIVER, new WaterModule(null, null, "test").getLandClass());
    }
}
