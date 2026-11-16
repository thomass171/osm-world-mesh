package de.yard.threed.osm2scenery.scenery;

import de.yard.threed.osm2world.MapBasedTagGroup;
import de.yard.threed.osm2world.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LandClassTest {

    @Test
    public void testOsmTagsToLandClass() {
        assertEquals(LandClass.FARMLAND, LandClass.osmTagsToLandClass(new MapBasedTagGroup(new Tag("landuse", "farmland"))));
        assertNull(LandClass.osmTagsToLandClass(new MapBasedTagGroup(new Tag("landuse", "forest"))));
        assertNull(LandClass.osmTagsToLandClass(new MapBasedTagGroup()));
        assertNull(LandClass.osmTagsToLandClass(null));
    }

    @Test
    public void testDbValue() {
        assertEquals(LandClass.FARMLAND, LandClass.fromDbValue(LandClass.FARMLAND.getCode()));
    }
}
