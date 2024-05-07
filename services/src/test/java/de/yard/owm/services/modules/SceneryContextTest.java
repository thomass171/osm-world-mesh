package de.yard.owm.services.modules;

import de.yard.owm.services.osm.OsmElementService;
import de.yard.owm.services.persistence.PersistedMeshFactory;
import de.yard.owm.services.persistence.TerrainMeshManager;
import de.yard.owm.testutils.DesdorfTestUtil;
import de.yard.owm.testutils.ServicesTestUtil;
import de.yard.owm.testutils.TestData;
import de.yard.owm.testutils.TestServices;
import de.yard.owm.testutils.TestUtils;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapWay;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 *
 */
@SpringBootTest(classes = {TestServices.class})
@Slf4j
public class SceneryContextTest {
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(SceneryContextTest.class);

    @Autowired
    OsmElementService osmElementService;

    @Autowired
    TerrainMeshManager terrainMeshManager;

    @Autowired
    TestServices testServices;

    @BeforeEach
    void setup(){
        testServices.cleanup();
    }

    /**
     */
    @Test
    public void testSceneryContextFromDB() throws Exception {
        DesdorfTestUtil desdorfTestUtil = new DesdorfTestUtil(terrainMeshManager, osmElementService);

        desdorfTestUtil.processK41Low();

        SceneryContext newSceneryContext = SceneryContext.buildFromDatabase(terrainMeshManager.findOsmWays());
        assertEquals(1, newSceneryContext.highways.size(), "sceneryContext.highways");
        assertTrue( newSceneryContext.highways.containsKey(desdorfTestUtil.k41Low.getOsmId()), "sceneryContext.highways.osmid");

    }
}
