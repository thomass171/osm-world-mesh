package de.yard.owm.services;


import de.yard.owm.services.osm.OsmService;
import de.yard.owm.services.util.OsmXmlParser;
import de.yard.owm.testutils.TestUtils;
import de.yard.threed.core.geometry.SimpleGeometry;
import de.yard.threed.core.loader.PortableModelDefinition;
import de.yard.threed.core.loader.PortableModelList;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.graph.Graph;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.OSMData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.HashMap;

import static de.yard.owm.testutils.TestUtils.loadFileFromClasspath;
import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.ROAD;
import static de.yard.threed.osm2world.Config.MATERIAL_FLIGHT;
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

        OsmService.Results results = osmService.createRepresentations(gridCellBounds, gridCellBounds.getProjection() ,osmData);
        assertNotNull( results.sceneryMesh);

    }


}