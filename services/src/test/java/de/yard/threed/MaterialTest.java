package de.yard.threed;

import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.Materials;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

import static de.yard.threed.osm2graph.SceneryBuilder.loadConfig;
import static de.yard.threed.osm2graph.SceneryBuilder.loadMaterialConfig;



/**
 * 11.4.19: Auch um das Material Konzept zu ergruenden
 */
public class MaterialTest {
    //EngineHelper platform = PlatformHomeBrew.init(new HashMap<String, String>());
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(MaterialTest.class);

    /**
     * @throws IOException
     */
    @Test
    public void testMaterial() throws IOException {
        Config.reinit(Processor.defaultconfigfile, loadMaterialConfig(Config.MATERIAL_FLIGHT), loadConfig("superdetailed"), null);
        Materials.configureMaterials(Config.getCurrentConfiguration());
        Material material = Materials.RAIL_DEFAULT;
    }

}
