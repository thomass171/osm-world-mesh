package de.yard.threed;

import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.util.TagHelper;
import de.yard.threed.osm2scenery.util.TagMap;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.TagGroup;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

import static de.yard.threed.osm2graph.SceneryBuilder.loadConfig;
import static de.yard.threed.osm2graph.SceneryBuilder.loadMaterialConfig;
import static de.yard.threed.osm2world.Config.MATERIAL_FLIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * 17.4.19: auch für TagFilter, TagMap
 */
public class ConfigTest {
    //EngineHelper platform = PlatformHomeBrew.init(new HashMap<String, String>());
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(ConfigTest.class);

    /**
     * @throws IOException
     */
    @Test
    public void testTagMap() throws IOException {

        Config.reinit(Processor.defaultconfigfile, loadMaterialConfig(MATERIAL_FLIGHT), loadConfig("superdetailed"), null);
        HighwayModule highwayModule = new HighwayModule();
        TagMap materialmap = highwayModule.getTagMap("materialmap");
        assertEquals(5, materialmap.getSize(), "materialmap");

        TagGroup tg = TagHelper.buildTagGroup("highway", "secondary");

        assertEquals("ROAD", materialmap.getValue(tg), "material");
        assertNull(materialmap.getValue(TagHelper.buildTagGroup("highway", "Xsecondary")), "material");
    }

}
