package de.yard.owm.services;


import de.yard.owm.services.osm.OsmService;
import de.yard.owm.services.persistence.PersistedMeshFactory;
import de.yard.owm.services.persistence.TerrainMeshManager;
import de.yard.owm.services.util.OsmXmlParser;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.OSMData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.HashMap;

import static de.yard.owm.testutils.TestUtils.loadFileFromClasspath;
import static org.junit.jupiter.api.Assertions.*;


/**
 *
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@Slf4j
public class OsmServiceTest {

    @Autowired
    private OsmService osmService;

    @Autowired
    TerrainMeshManager terrainMeshManager;

    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));


    @Test
    public void testDesdorfK41Segment() throws Exception {

        String xml = loadFileFromClasspath("K41-segment.osm.xml");
        OsmXmlParser parser = new OsmXmlParser(xml);
        OSMData osmData = parser.getData();

        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider");
        customconfig.setProperty("modules.HighwayModule.tagfilter", "highway=secondary");

        GridCellBounds gridCellBounds = GridCellBounds.buildFromOsmData(osmData);
        TerrainMesh.meshFactoryInstance = new PersistedMeshFactory(gridCellBounds.getProjection().getBaseProjection(), terrainMeshManager);

        OsmService.Results results = osmService.createRepresentations(gridCellBounds, gridCellBounds.getProjection() ,osmData);
        assertNotNull( results.sceneryMesh);

    }


}