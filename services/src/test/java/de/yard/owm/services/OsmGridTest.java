package de.yard.owm.services;


import de.yard.owm.testutils.TestUtils;
import de.yard.threed.core.geometry.SimpleGeometry;
import de.yard.threed.core.loader.PortableModelDefinition;
import de.yard.threed.core.loader.PortableModelList;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.graph.Graph;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
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
public class OsmGridTest {

    public static final String ENDPOINT_OSM = "/api/osm";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(de.yard.threed.OsmGridTest.class);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void testPostXML() throws Exception {

        String xml = loadFileFromClasspath("K41-segment.osm.xml");
        MvcResult result = TestUtils.doPostXml(mockMvc, ENDPOINT_OSM, xml);
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());

    }

    @Test
    public void testDesdorfK41SegmentGrid2D() throws IOException {
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider");
        customconfig.setProperty("modules.HighwayModule.tagfilter", "highway=secondary");
        dotestDesdorfK41SegmentGrid(customconfig, false, "poc");
    }

    @Test
    public void testDesdorfK41SegmentGrid2DE() throws IOException {
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        customconfig.setProperty("modules.HighwayModule.tagfilter", "highway=secondary");
        dotestDesdorfK41SegmentGrid(customconfig, true, "poc");
    }

    private void dotestDesdorfK41SegmentGrid(Configuration customconfig, boolean elevated, String configsuffix) throws IOException {
        String desdorfk41 = SceneryBuilder.osmdatadir + "/K41-segment.osm.xml";
        SceneryBuilder sb = new SceneryBuilder();
        SceneryBuilder.FTR_SMARTGRID = true;
        SceneryBuilder.FTR_SMARTBG = true;

        Processor processor = sb.execute(desdorfk41, configsuffix, "Desdorf", false, customconfig, MATERIAL_FLIGHT).processor;
        HighwayModule roadModule = (HighwayModule) processor.scf.getModule("HighwayModule");
        assertNotNull(roadModule, "HighwayModule");
        //de.yard.threed.osm2scenery.modules.GraphModule graphModule = (de.yard.threed.osm2scenery.modules.GraphModule) processor.scf.getModule("GraphModule");
        //assertNotNull("GraphModule", graphModule);

        // 5 echte und eine durch gridnode. 13.8.19 jetzt nur innerhalb
        assertEquals(2 + 1, SceneryContext.getInstance().getGraph(SceneryObject.Category.ROAD).getNodeCount(), "edges");

        SceneryMesh sceneryMesh = processor.getResults().sceneryresults.sceneryMesh;
        //nur die K41 und einen Background.
        assertEquals(1, sceneryMesh.sceneryObjects.objects.size(), "scenery.areas");
        if (SceneryBuilder.FTR_SMARTBG) {
            assertEquals(1, sceneryMesh.getBackground().bgfillersize(), "scenery.backgrounds");
            assertEquals(0, sceneryMesh.getBackground().background.size(), "scenery.backgrounds");
        } else {
            assertEquals(0, sceneryMesh.getBackground().bgfillersize(), "scenery.backgrounds");
            assertEquals( /*13.8.19 ??? 1*/2, sceneryMesh.getBackground().background.size(), "scenery.backgrounds");
        }

        assertEquals(6, SceneryContext.getInstance().wayMap.getMapForCategory(ROAD).size(), "scenery.waymap.size");

        SceneryFlatObject road = (SceneryFlatObject) sceneryMesh.sceneryObjects.objects.get(0);
        // durch den cut wohl nur noch 8. 12.4.19 8->7
        assertEquals(7/*8/*11*/, road.getArea()[0].getPolygon().getCoordinates().length, "road.coordinates");
        assertEquals(3, road.getEleConnectorGroups().size(), "road.elegroups");
        //eigentlich sind es nur 4+4, aber unten rechts hatg ein Lazycut eine Gridnode "ersetzt", oder auch nicht, auf jeden Fall ist das nicht sauber.Klärt sich vielleicht noch mal.
        // und dann noch 5(?) durch BG Triangulation. 19.8.19: Durch earclipping wider 5 weniger?
        assertEquals(4 + 4 + 1 + 5 - 5, EleConnectorGroup.getKnownCoordinates().size(), "EleConnectorGroup.coordinates");
        assertEquals(3, processor.gridCellBoundsused.elegroups.size(), "gridcellbounds.elegroups.size");
        assertEquals(0, SceneryContext.getInstance().warnings.size(), "warnings");

        PortableModelList pml = processor.pml;
        assertEquals(2, pml.materials.size(), "pml.materials");
        assertEquals(2, pml.getObjectCount(), "pml.objects");
        PortableModelDefinition roadobject = pml.findObject("ROAD");
        SimpleGeometry roadgeo = roadobject.geolist.get(0);
        SimpleGeometry terraingeo = pml.findObject("TERRAIN_DEFAULT").geolist.get(0);
        //2.4.18: einfach mal eine Normale pruefen. Die dürften eh alle gleich sein.
        //2.5.19 Vector3 normal = pml.objects.get(0).geolist.get(0).getNormals().getElement(3);
        //2.5.19 TestUtil.assertVector3("normal", new Vector3(0, 0, 1), normal);

        // warum eigentlich 12 und nicht 10? 12.4.19: 12->6, 16.4.19: 6->51 (wegen no strip??). wieder 6; gut
        assertEquals(6/*12*/, roadgeo.getVertices().size(), "road.vertices");
        assertEquals(6/*12*/, roadgeo.getUvs().size(), "road.uvs");
        //uv einfach mal übernommen
        //16.4.19 TODO TestUtil.assertVector2("road.uv[0]", new Vector2(96.155586,36.590164), roadgeo.getUvs().getElement(0));
        if (!elevated) {
            assertEquals(0, roadgeo.getVertices().getElement(0).getZ(), "road.vertex0.z");
            assertEquals(0, terraingeo.getVertices().getElement(0).getZ(), "terrain.vertex0.z");
            assertEquals(0, SceneryContext.getInstance().getGraph(SceneryObject.Category.ROAD).getNode(0).getLocation().getZ(), "graph.node0.z");
        } else {
            float expectedelevation = 68;
            // Eles sind noch nicht cut. 5 echte und eine durch gridnode und 3 vom Grid. 13.8.19 jetzt nur innerhalb
            assertEquals(2 + 1, road.getEleConnectorGroups().eleconnectorgroups.size(), "road.elevationsgropus");
            assertEquals(2 + 1 + 3, EleConnectorGroup.elegroups.size(), "ElevationMap.elevationsgropus");
            //12.6.19: Der distinct liefert keine mehr ausserhalb (-2)? 12.8.19 einer weniger. 14.8.19: 6 scheint plausibel. Rechts unten ist aber unklar.
            assertEquals(6, EleConnectorGroup.getAllGroupsDistinct().size(), "ElevationGroup.cmap.elevationsgropus");
            // 28.8.18: nur zwei nodes liegen innerhalb des Grid. Stimmt das wirklich? Das passt wohl, zwei innerhalb und eine GridNode, drei dann ausserhalb
            de.yard.threed.OsmGridTest.assertEleConnectorGroup("K41elevations", road.getEleConnectorGroups(), new float[]{68, 68, 68, 68, 68, 68});

            for (int i = 0; i < roadgeo.getVertices().size(); i++) {
                assertEquals(expectedelevation, roadgeo.getVertices().getElement(i).getZ(), "road.vertex.z[" + i + "]");
            }
            for (int i = 0; i < terraingeo.getVertices().size(); i++) {
                assertEquals(expectedelevation, terraingeo.getVertices().getElement(i).getZ(), "terrain.vertex.z[" + i + "]");
            }
            Graph roadgraph = SceneryContext.getInstance().getGraph(SceneryObject.Category.ROAD);
            for (int i = 0; i < roadgraph.getNodeCount(); i++) {
                assertEquals(expectedelevation, roadgraph.getNode(i).getLocation().getZ(), "graph.node.z");
            }
            de.yard.threed.OsmGridTest.assertBasicGraph(roadgraph, expectedelevation);

        }
        assertTrue(TerrainMesh.getInstance().isValid(true), "TerrainMesh.valid");

    }
}