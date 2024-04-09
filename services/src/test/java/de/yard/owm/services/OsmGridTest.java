package de.yard.owm.services;


import com.vividsolutions.jts.geom.Coordinate;
import de.yard.owm.services.persistence.PersistedMeshFactory;
import de.yard.owm.services.persistence.TerrainMeshManager;
import de.yard.owm.testutils.TestUtils;
import de.yard.threed.TestUtil;
import de.yard.threed.core.Color;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.geometry.SimpleGeometry;
import de.yard.threed.core.loader.InvalidDataException;
import de.yard.threed.core.loader.LoaderGLTF;
import de.yard.threed.core.loader.PortableMaterial;
import de.yard.threed.core.loader.PortableModelDefinition;
import de.yard.threed.core.loader.PortableModelList;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.core.resource.BundleResource;
import de.yard.threed.core.testutil.InMemoryBundle;
import de.yard.threed.graph.Graph;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.ProcessResults;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GeoJson;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.MainGrid;
import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.WayMap;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.modules.AerowayModule;
import de.yard.threed.osm2scenery.modules.BridgeModule;
import de.yard.threed.osm2scenery.modules.BuildingModule;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.modules.RailwayModule;
import de.yard.threed.osm2scenery.modules.WaterModule;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.Background;
import de.yard.threed.osm2scenery.scenery.SceneryAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.ConfigUtil;
import de.yard.threed.tools.GltfBuilder;
import de.yard.threed.tools.GltfBuilderResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import static de.yard.owm.testutils.TestUtils.loadFileFromClasspath;
import static de.yard.threed.osm2graph.SceneryBuilder.loadConfig;
import static de.yard.threed.osm2graph.SceneryBuilder.loadMaterialConfig;
import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.ROAD;
import static de.yard.threed.osm2world.Config.MATERIAL_FLIGHT;
import static de.yard.threed.osm2world.Config.MATERIAL_MODEL;
import static de.yard.threed.osm2world.Materials.FARMLAND;
import static de.yard.threed.osm2world.Materials.TERRAIN_DEFAULT;
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

    @Autowired
    TerrainMeshManager terrainMeshManager;

    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(OsmGridTest.class);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        TerrainMesh.meshFactoryInstance = new PersistedMeshFactory(null, terrainMeshManager);
    }

    @Test
    public void testPostXML() throws Exception {

        String xml = loadFileFromClasspath("K41-segment.osm.xml");
        MvcResult result = TestUtils.doPostXml(mockMvc, ENDPOINT_OSM, xml);
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());

    }


    //9.4.19 Das mit dem Maingrid muss redesigned werden @Test
    public void testMainGrid() {
        //Altlasten loswerden
        Config.init(Processor.defaultconfigfile);
        try {
            MainGrid maingrid = MainGrid.build();
            String result = GeoJson.export(maingrid.roads, maingrid.rivers);
            Files.write(Paths.get("/Users/thomas/tmp/maingrid.json"), result.getBytes());
            // wegen der Segment ist das ja viel zu viel. Nach Umabu wieder weniger. Einfach uebernommen weil plausibel. Doch mehr weil Segmente
            assertEquals(3193/*1053*/, maingrid.roads.size(), "roads.total");
            assertEquals(977/*22*/, maingrid.rivers.size(), "rivers.total");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Geht nicht mit K41 xml, weil da die Bounds ja gar nicht drin sind. 8.6.18: Scheinen aber doch welche drin zu sein.
     *
     * @throws IOException
     */
    /*28.8.18 @Test
    public void testDesdorfGrid() throws IOException {
        Config.reinit(loadConfig(), null);
        String desdorf = "/Users/thomass/osmdata/Desdorf.osm.xml";
        Processor processor = new Processor(new File(desdorf));
        processor.process(null);
        //assertEquals("",3,st.getGlobalCount(StatisticsTarget.Stat.TOTAL_TRIANGLE_COUNT));

        GridCellBounds desdorfGrid = MainGrid.buildDesdorf();
        processor = new Processor(new File(desdorf));
        processor.process(desdorfGrid);
        ConversionFacade.Results results = processor.results;

        GraphModule graphModule = (GraphModule) processor.cf.getModule("GraphModule");
        assertNotNull("GraphModule", graphModule);
        //15.6.18: Warum 28? Wegen erneutem Osmdata lesen?
        assertEquals("edges", 28/*25* /, graphModule.graph.getNodeCount());

        // TODO der export nach osmscenery ist erstmal nur testhalber hier.
        File f = new File(SceneryBuilder.outdir + "/graph/tt.xml");
        PrintStream pw = new PrintStream(f);
        GraphExporter.exportToXML(graphModule.graph, pw, true);
        pw.close();
        GraphExporter.exportToXML(graphModule.graph, System.out, true);

        PortableModelTarget pmt = new PortableModelTarget();
        MapData mapData = processor.cf.getMapData();
        TargetUtil.renderWorldObjects(pmt, mapData, false);

        GltfBuilder gltfBuilder = new GltfBuilder();
        String gltfstring = gltfBuilder.process(pmt.pml);
        System.out.println(gltfstring);

        GltfProcessor.writeGltfOutput(SceneryBuilder.outdir + "/tiles", "desdorf", gltfstring, gltfBuilder.getBin().getBuffer());
    }*/
    @Test
    public void testPOC() throws IOException {
        Config.reinit(Processor.defaultconfigfile, loadMaterialConfig(MATERIAL_FLIGHT), loadConfig("poc"), null);
        Configuration configuration = Config.getCurrentConfiguration();
        assertFalse(configuration.getBoolean("modules.TunnelModule.enabled"));
    }

    //9.4.19: Ohne Grid ist doch völlig witzlos @Test
    public void testMunster() throws IOException {
        new SceneryBuilder().execute(SceneryBuilder.osmdatadir + "/Munster-K33.osm.xml", "poc", null, /*SceneryBuilder.outdir, "Munster-K33",*/ false, null, MATERIAL_FLIGHT);
    }

    @Test
    @Disabled // 2.4.24
    public void testDesdorfGridsuperdetailed() throws IOException {
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider");
        dotestDesdorfGrid(customconfig, "superdetailed", true);
    }

    private void dotestDesdorfGrid(Configuration customconfig, String configsuffix, boolean smartgrid) throws IOException {
        String desdorfk41 = SceneryBuilder.osmdatadir + "/Desdorf.osm.xml";

        SceneryBuilder sb = new SceneryBuilder();
        //SceneryBuilder.FTR_SMARTGRID=true;//smartgrid;
        if (smartgrid) {
            SceneryBuilder.FTR_SMARTBG = true;
        }
        Processor processor = sb.execute(desdorfk41, configsuffix, "Desdorf", false, customconfig, MATERIAL_FLIGHT).processor;
        HighwayModule roadModule = (HighwayModule) processor.scf.getModule("HighwayModule");
        assertNotNull(roadModule, "HighwayModule");
        //de.yard.threed.osm2scenery.modules.GraphModule graphModule = (de.yard.threed.osm2scenery.modules.GraphModule) processor.scf.getModule("GraphModule");
        //assertNotNull("GraphModule", graphModule);

        //73 einfach uebernommen. Durch PreCut nur noch 28
        assertEquals(28, SceneryContext.getInstance().getGraph(SceneryObject.Category.ROAD).getNodeCount(), "edges");

        SceneryMesh sceneryMesh = processor.getResults().sceneryresults.sceneryMesh;

        BuildingModule buildingModule = (BuildingModule) processor.scf.getModule("BuildingModule");
        assertNotNull(buildingModule, "BuildingModule");
        List<SceneryObject> buildings = sceneryMesh.sceneryObjects.findObjectsByCreatorTag("Building");
        //23 corresponds to OSM data
        assertEquals(23, buildings.size(), "roads");

        //Der Weg südlich Richtung Haus ("Gut Desdorf") schneidet durch das Farmland.
        SceneryWayObject gutDesdorf = (SceneryWayObject) sceneryMesh.sceneryObjects.findObjectByOsmId(37935545);

        List<SceneryWayConnector> connectors = SceneryContext.getInstance().wayMap.getConnectors(ROAD);
        assertEquals(8, connectors.size(), "connectors");

        SceneryWayConnector k41k43connector = (SceneryWayConnector) sceneryMesh.sceneryObjects.findObjectByOsmId(255563538);
        assertNotNull(k41k43connector.getArea(), "k41k43connector.area");
        assertEquals(5, k41k43connector.getArea()[0].getPolygon(sceneryMesh.terrainMesh).getCoordinates().length, "k41k43connector.polygon.size");
        TestUtil.validateConnector(255563538, sceneryMesh.sceneryObjects, SceneryWayConnector.WayConnectorType.STANDARD_TRI_JUNCTION, Boolean.TRUE, sceneryMesh.terrainMesh);

        SceneryAreaObject splitfarmland = (SceneryAreaObject) sceneryMesh.sceneryObjects.findObjectByOsmId(87822834);
        assertEquals(2, splitfarmland.getArea().length, "splitfarmland.polygone.size");
        assertEquals(2, splitfarmland.polydiffs.size(), "splitfarmland.polydiffs");
        assertEquals(4, splitfarmland.polydiffs.get(0).seam.size(), "splitfarmland.polydiff[0].seam.size");
        assertEquals((SceneryBuilder.FTR_SMARTGRID) ? 5 : 6, splitfarmland.polydiffs.get(1).seam.size(), "splitfarmland.polydiff[1].seam.size");
        assertEquals(1, splitfarmland.getAdjacent().size(), "splitfarmland.adjacent.size");
        MeshPolygon splitfarmlandEast = splitfarmland.getArea()[1].getMeshPolygon(sceneryMesh.terrainMesh);

        SceneryAreaObject smallScrubarea = (SceneryAreaObject) sceneryMesh.sceneryObjects.findObjectByOsmId(225794276);
        //es muss eine shared line geben. Oder sogar 2?
        assertEquals(2/*26.8.19 3*/, smallScrubarea.getArea()[0].getMeshPolygon(sceneryMesh.terrainMesh).lines.size(), "smallScrubarea.MeshPolygon.size");
        assertEquals(smallScrubarea, splitfarmland.getAdjacent().keySet().iterator().next(), "smallScrubarea==splitfarmland.adjacent");
        assertEquals(1, sceneryMesh.terrainMesh.getShared(splitfarmland.getArea()[1], smallScrubarea.getArea()[0]).size(), "smallScrubarea,splitfarmland.sharedLines.size");

        List<SceneryObject> wayToAreaFiller = sceneryMesh.sceneryObjects.findObjectsByCreatorTag("WayToAreaFiller");
        assertEquals(1, wayToAreaFiller.size(), "wayToAreaFiller.size");

        TestUtil.validateResult(sceneryMesh, logger, 0, 9, sceneryMesh.terrainMesh);

        //getCuts kann man nicht mehr aufrufenMap<Integer, List<GridCellBounds.LazyCutObject>> cuts = processor.gridCellBoundsused.getCuts(sceneryMesh.sceneryObjects.objects);
        //2 nodes + 4 lazy cut auf 3 Edges.->5
        //getCuts kann man nicht mehr aufrufen.assertEquals("cuts.size", 3+1, cuts.size());
        //6 LazyCuts
        assertEquals(12 + 1, processor.gridCellBoundsused.getPolygon().getCoordinates().length, "gridCellBounds.coordinates.size");
        //Zwischenstufe des Boundary Polygon visuell getest und hier hinterlegt. Die Boundary im TerrainMesh ist noch weiter gesplittet.
        assertEquals("POLYGON ((205.60063218280519 -93.23774501728838, 211.2937464793042 -88.99528720927412, 71.44657661407406 151.0014835023132, 64.98341606170719 148.06251185901493, 48.06776390698129 126.56772547560094, 45.151512521241365 125.86382359666469, -143.32618865755532 -79.42480938115578, -146.28690948697593 -81.29139240107078, -209.97803471204543 -147.78372405767405, -207.82396724107957 -151.27227569818533, 26.540499353209718 -117.4358168330573, 29.332551336487548 -115.32526104780207, 205.60063218280519 -93.23774501728838))", processor.gridCellBoundsused.polygon345a.toString(), "new grid boundary");
        TerrainMesh tm = sceneryMesh.terrainMesh;
        //3 normale Edges, 6 LazyCuts, 3 Areas. Aber 12 ist doch etwas wenig! 14 besser? 19.8.19: 14->16. Ganz plausibel.
        assertEquals(16, tm.getBoundaries().size(), "TerrainMesh.Bounds.size");
        assertTrue(tm.isValid(true), "TerrainMesh.valid");
        List<MeshLine> sharedLines = tm.getSharedLines();
        //13 ist plausibel
        assertEquals(13, sharedLines.size(), "TerrainMesh.sharedLines.size");

        assertNotNull(gutDesdorf.getWayArea().getVertexData(), "gutDesdorf.vertexData");
    }

    @Test
    @Disabled // 2.4.24
    public void testDesdorfK41SegmentGrid2D() throws IOException {
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider");
        customconfig.setProperty("modules.HighwayModule.tagfilter", "highway=secondary");
        dotestDesdorfK41SegmentGrid(customconfig, false, "poc");
    }

    @Test
    @Disabled // 2.4.24
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
        assertEquals(7/*8/*11*/, road.getArea()[0].getPolygon(sceneryMesh.terrainMesh).getCoordinates().length, "road.coordinates");
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
            TestUtil.assertEleConnectorGroup("K41elevations", road.getEleConnectorGroups(), new float[]{68, 68, 68, 68, 68, 68});

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
            TestUtil.assertBasicGraph(roadgraph, expectedelevation);

        }
        assertTrue(sceneryMesh.terrainMesh.isValid(true), "TerrainMesh.valid");

    }

    /**
     * Etwas groesser mit genau einer Bridge.
     *
     * @throws IOException
     */
    @Test
    @Disabled // 2.4.24
    public void testB55B477smallGrid() throws IOException {
        // 25.7.18: es wird nicht mehr gemerged, darum 6->12 (11 wegen gapfiller)+2scrub+4 farmland+4 Ramps
        //22.4.19: plus 9 Connector
        //13.8.19: smallcut nicht mehr, weil der auch den Way zerlegt, was z.Z. nicht handlebar ist.Der 225794273 wird jetzt per Sonderlocke rausgenommen, darum -2(?)
        doB55B477small("B55-B477-small"/*cut"*/, 11 + 2 + 4 + 4 + 9 - 2);
    }

    //9.4.19: Ohne Grid ist doch völlig witzlos @Test
    /*public void testB55B477smallNoGrid() throws IOException {
        //das sind 6 durch ungünstiges Merging.
        // 25.7.18: es wird nicht mehr gemerged, darum 6->14
        // 12 Roads(keine Feldwege, inkl BrückenRoadsegment) + + 1 Gapfiller +2 scrub+4 farmland+4 Ramps
        doB55B477small(null, 12 + 1 + 2 + 4 + 4);
    }*/

    /**
     * Zwei Ways rechts am Grid bleiben erstmal overlapped.
     */
    public void doB55B477small(String gridname, int expectedobjects) throws IOException {
        String b55b477small = SceneryBuilder.osmdatadir + "/B55-B477-small.osm.xml";
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        //per detailed customconfig.setProperty("modules.RoadModule.tagfilter","highway=*");
        SceneryBuilder sb = new SceneryBuilder();
        SceneryBuilder.FTR_SMARTGRID = true;
        SceneryBuilder.FTR_SMARTBG = true;

        Processor processor = sb.execute(b55b477small, "detailed", gridname, false, customconfig, MATERIAL_FLIGHT).processor;

        assertEquals(12/*smartgrid 4 */, processor.gridCellBoundsused.getPolygon().getCoordinates().length - 1, "b55b477smallgrid.nodes");
        //Warum 7? Viewer zeigt nur 5. 13.8.19: 5 (wie hier) bei detailed, 7 bei superdetailed.Der 225794273 wird jetzt per Sonderlocke rausgenommen, darum -1
        assertEquals(5 - 1, processor.gridCellBoundsused.additionalGridnodes.size(), "b55b477smallgrid.additionalGridnodes");

        HighwayModule roadModule = (HighwayModule) processor.scf.getModule("HighwayModule");
        assertNotNull(roadModule, "HighwayModule");

        //14 scheint plausible. Das sind 11 normale Roads (keine Feldwege) und einmal ueber die Brucke. Der 225794273 wird jetzt per Sonderlocke rausgenommen, darum -1
        assertEquals(11 + 1 - 1, roadModule.getRoads().size(), "HighwayModule.roads");
        SceneryMesh sceneryMesh = processor.getResults().sceneryresults.sceneryMesh;
        TerrainMesh tm = sceneryMesh.terrainMesh;
        assertEquals(expectedobjects, sceneryMesh.sceneryObjects.objects.size(), "scenery.areas");

        // wie spielen denn die Bridges rein? Normale Connector. Aber warum sind manche Outer da und manche nicht?
        WayMap wayMap = SceneryContext.getInstance().wayMap;
        //Der 225794273 wird jetzt per Sonderlocke rausgenommen, darum -1
        assertEquals(7 - 1, wayMap.getConnectorMapForCategory(ROAD).size(), "connector.size");

        TestUtil.validateConnector(2345486254L, sceneryMesh.sceneryObjects, SceneryWayConnector.WayConnectorType.SIMPLE_SINGLE_JUNCTION, Boolean.FALSE, tm);
        TestUtil.validateConnector(54286220, sceneryMesh.sceneryObjects, SceneryWayConnector.WayConnectorType.STANDARD_TRI_JUNCTION, Boolean.FALSE, tm);
        TestUtil.validateConnector(54286227, sceneryMesh.sceneryObjects, SceneryWayConnector.WayConnectorType.STANDARD_TRI_JUNCTION, Boolean.TRUE, tm);

        validateEleGroups(7093390, sceneryMesh.sceneryObjects);

        //Der eine main way am connector 2345486254 muss wegen SIMPLE_SINGLE_JUNCTION ein Pair dazubekommen haben.
        SceneryWayConnector c2345486254 = (SceneryWayConnector) sceneryMesh.sceneryObjects.findObjectByOsmId(2345486254L);
        assertEquals(3, c2345486254.getMajor0().getWayArea().getRawLength(), "2345486254.main0.rawlength");
        assertEquals(2, c2345486254.getMajor0().getWayArea().getLength(), "2345486254.main0.rawlength");
        assertEquals(2, c2345486254.getMajor0().getWayArea().getStartPair(tm).length, "2345486254.main0.rawlength");
        assertEquals(1, c2345486254.getMajor0().getWayArea().getEndPair().length, "2345486254.main0.rawlength");
        assertEquals(3, c2345486254.getMajor0().getWayArea().getPairsOfSegment(0).length, "2345486254.main0.PairsOfSegment(0)");
        //main0 hat am minor split
        assertEquals(4 + 1, tm.getPolygon(c2345486254.getMajor0().getWayArea()).lines.size(), "2345486254.main0.meshpolygon.size");
        assertEquals(4 + 2/*ramps*/, tm.getPolygon(c2345486254.getMajor1().getWayArea()).lines.size(), "2345486254.main1.meshpolygon.size");
        assertEquals(4, tm.getPolygon(c2345486254.getWay(c2345486254.minorway).getWayArea()).lines.size(), "2345486254.minor.meshpolygon.size");

        //die Runterfahrt
        HighwayModule.Highway road7093390 = (HighwayModule.Highway) sceneryMesh.sceneryObjects.findObjectByOsmId(7093390);
        MeshPolygon road7093390mp = tm.getPolygon(road7093390.getWayArea());
        //6 sind sichtbar, aber der Abzweig ist unsichtbar, hat aber seine Attachpoints. Der 225794273 wird jetzt per Sonderlocke rausgenommen, darum doch 6
        assertEquals(6, road7093390mp.lines.size(), "road7093390.meshpolygon.size");
        //der Connector unten
        SceneryWayConnector c54286227 = (SceneryWayConnector) sceneryMesh.sceneryObjects.findObjectByOsmId(54286227);
        MeshPolygon c54286227mp = tm.getPolygon(c54286227.getArea()[0]);
        assertEquals(4, c54286227mp.lines.size(), "c54286227.meshpolygon.size");
        ScenerySupplementAreaObject bridgegroundfiller = (ScenerySupplementAreaObject) sceneryMesh.sceneryObjects.findObjectsByCreatorTag("BridgeGroundFiller").get(0);
        assertEquals(2, bridgegroundfiller.getArea().length, "bridgegroundfiller.size");
        MeshPolygon bridgegroundfillermp = tm.getPolygon(bridgegroundfiller.getArea()[0]);
        assertEquals(4, bridgegroundfillermp.lines.size(), "bridgegroundfiller.meshpolygon.size");

        //die Area SceneryAreaObject schneidet zwei verschiedene Grid Lines. Das sind dann drei Mesh lines, zwei davon boundary
        //die Reihenfolge ist hier natuerlich irgendwie Zufall.
        SceneryAreaObject area87818511 = (SceneryAreaObject) sceneryMesh.sceneryObjects.findObjectByOsmId(87818511);
        MeshPolygon area87818511mp = tm.getPolygon(area87818511.getArea()[0]);
        assertEquals(3, area87818511mp.lines.size(), "area87818511.meshpolygon.size");
        assertTrue(area87818511mp.lines.get(0).isBoundary(), "area87818511.meshpolygon[0].isBoundary");
        assertFalse(area87818511mp.lines.get(1).isBoundary(), "area87818511.meshpolygon[1].isBoundary");
        assertTrue(area87818511mp.lines.get(2).isBoundary(), "area87818511.meshpolygon[2].isBoundary");

        // Bridges
        // von Sueden ist cut, darum erstmal von Norden
        HighwayModule.Highway roadToBridgeFromNorth = (HighwayModule.Highway) sceneryMesh.sceneryObjects.findObjectByOsmId(8033747);
        validateBridgeApproach(roadToBridgeFromNorth, false, sceneryMesh.terrainMesh);

        //Die Bruecke selber, aber auch das Roadsegment da drin muss hoeher liegen
        BridgeModule.Bridge bridge = (BridgeModule.Bridge) sceneryMesh.sceneryObjects.findObjectsByCreatorTag("Bridge").get(0);
        validateBridge(bridge, bridgegroundfiller, roadToBridgeFromNorth, sceneryMesh.terrainMesh);

        //Background
        // wegen Bruecke 7 statt 9. Und mit background Holebereinigung wieder 9 (obs stimmt? viewer zeigt 8. Mit zaehlen sinds auch 8, obwohls logisch
        // nur 6 sein duerften.). Mit Surface sinds 10? Brücke dazu. 28.8.18: wieder 8? oder 9. 21.11.18: durch float->double wieder 8? 13.8.19: wieder 7
        //28.8.19: Die BridgeGaps schliessen z.Z.nicht richtig, darum 5 statt 7
        TestUtil.validateResult(sceneryMesh, logger, 0, 5, tm);

        //Elevation
        float expectedMinimumElevation = 68;

        //Warum 3? 28.9.18: jetzt 4, wohl wegen river? 5.4.19: 4->8 wegen Mapnode als Boundarynode(??)
        //8->4 wegen gridcell projection? 11.6.19: 4->5(??)
        //8.11.21 jetzt sind es 6. Mal weglassen. assertEquals("ElevationArea.history.size", 5, ElevationArea.history.size());

        assertMinimumElevation(sceneryMesh.sceneryObjects.objects, expectedMinimumElevation);

        //PML
        PortableModelList pml = processor.pml;

        //Roads und Terrain + gapfgiller + surface(scrub+farmland)+decoration 11.7.19:ROadMarking, GRASS? 17.7.19 schwankt immer mal.TODO  Tests verbessern!
        //wegen schwanken TODO assertEquals("pml.materials", 1 + 1 + 1 + 2 + 1 + 1, pml.materials.size());
        //wegen schwanken TODO assertEquals("pml.objects", 2 + 1 + 2 + 1 + 1, pml.objects.size());

        assertMinimumElevation(pml, expectedMinimumElevation);
        SimpleGeometry terraingeo = pml.findObject(TERRAIN_DEFAULT.getName()).geolist.get(0);

        //Terrain elevation. Manche nahe der Brücke sind erhöht. Darum (erstmal?) nur auf Minimum pruefen.
        for (int i = 0; i < terraingeo.getVertices().size(); i++) {
            assertTrue(expectedMinimumElevation <= terraingeo.getVertices().getElement(i).getZ(), "terrain.vertex.z[" + i + "]");
        }
        SimpleGeometry farmlandgeo = pml.findObject(FARMLAND.getName()).geolist.get(0);
        //die werden doch auch irgendwann mal abweichende Elevation haben
        for (int i = 0; i < farmlandgeo.getVertices().size(); i++) {
            assertEquals(expectedMinimumElevation, farmlandgeo.getVertices().getElement(i).getZ(), "farmland.vertex.z[" + i + "]");
        }

        //ASPHALT+GRASS+SCRUB+FARMLAND?
        //TODO assertEquals("pml.materials", 1 + 1 + 1 + 1, pml.materials.size());
        //ROADS+Gapfiller+surface(scrub+farmland)
        //TODO assertEquals("pml.objects", 1 + 1 + 1 + 1, pml.objects.size());
        //TODO assertEquals("pmt.ASPHALT", 12, processor.pmt.getSceneryObjectCount(Materials.ASPHALT.getName()));

        //noch nicht solange es keine echten Connector gibt assertEquals("warnings",0,SceneryContext.getInstance().warnings.size());

        //da sind drei Ref Decos dran
        //HighwayModule.Highway road7093390 = (HighwayModule.Highway) sceneryMesh.sceneryObjects.findObjectByOsmId(7093390);
        assertEquals(3, road7093390.getDecorations().size(), "decorations.size");
        assertEquals(expectedMinimumElevation + AbstractArea.OVERLAYOFFSET, road7093390.getDecorations().get(0).getVertexData().vertices.get(0).z, "decorations.vertex.z[0");
    }

    /**
     * Das normale mit Rails, Roundabout,...
     *
     * @throws IOException
     */
    @Test
    @Disabled // 2.4.24
    public void testB55B477Grid() throws IOException {
        // 25.7.18: es wird nicht mehr gemerged, darum 6->12 (11 wegeen gapfiller)+2scrub+4 farmland
        doB55B477("B55-B477", 17);
    }

    public void doB55B477(String gridname, int expectedobjects) throws IOException {
        String b55b477small = SceneryBuilder.osmdatadir + "/B55-B477.osm.xml";
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        SceneryBuilder sb = new SceneryBuilder();
        SceneryBuilder.FTR_SMARTBG = true;

        Processor processor = sb.execute(b55b477small, "detailed", gridname, false, customconfig, MATERIAL_FLIGHT).processor;

        RailwayModule railwayModule = (RailwayModule) processor.scf.getModule("RailwayModule");
        SceneryObjectList sceneryObjectList = processor.getResults().sceneryresults.sceneryMesh.sceneryObjects;
        SceneryMesh sceneryMesh = processor.getResults().sceneryresults.sceneryMesh;
        TerrainMesh tm = sceneryMesh.terrainMesh;

        List<SceneryWayObject> railways = sceneryObjectList.findWaysByCategory(SceneryObject.Category.RAILWAY);
        //2 ist sagen wir mal plausibel
        assertEquals(2, railways.size(), "railways");
        // 28.8.18: 24->26, wohl wegen Grid nodes
        TestUtil.assertEleConnectorGroup("railways", railways.get(0).getEleConnectorGroups(), new float[]{68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68});

        List<SceneryObject> bridges = sceneryObjectList.findBridges();
        //9 ist sagen wir mal plausibel. Eigentlich sinds aber nur 6. Die A61 Abfahrten haben drei eigene Bridges, macht 9.
        assertEquals(9, bridges.size(), "bridges");

        //der Kreisverkehr
        HighwayModule.Highway circle26927466 = (HighwayModule.Highway) sceneryObjectList.findObjectByOsmId(26927466);
        assertFalse(circle26927466.isClosed(), "circle26927466");
        assertFalse(circle26927466.getWayArea().isClosed(), "circle26927466.isClosed");
        assertEquals(17/*anderer split?? 18/*nicht mehr closed 19*/, circle26927466.getWayArea().getLength(), "circle26927466.length");
        MeshPolygon mp = tm.getPolygon(circle26927466.getArea()[0]);
        //warum nur 9? Das passt. Das ist die outerline.
        assertEquals(10/*anderer split??12/*nicht mehr closed  9*/, mp.lines.size(), "circle26927466.meshpolygon.size");

        //Connector des Kreisverkehr
        SceneryWayConnector c295055704 = (SceneryWayConnector) sceneryObjectList.findObjectByOsmId(295055704);
        TestUtil.validateConnector(c295055704, SceneryWayConnector.WayConnectorType.SIMPLE_CONNECTOR, null, tm);

        //two Connector at circle at 173190487
        SceneryWayConnector c1840257467 = (SceneryWayConnector) sceneryObjectList.findObjectByOsmId(1840257467);
        TestUtil.validateConnector(c1840257467, SceneryWayConnector.WayConnectorType.SIMPLE_SINGLE_JUNCTION, Boolean.TRUE, tm);
        SceneryWayConnector c1840257469 = (SceneryWayConnector) sceneryObjectList.findObjectByOsmId(1840257469);
        TestUtil.validateConnector(c1840257469, SceneryWayConnector.WayConnectorType.SIMPLE_SINGLE_JUNCTION, Boolean.TRUE, tm);

        //Auffahrt A61
        SceneryWayConnector c1353883890 = (SceneryWayConnector) sceneryObjectList.findObjectByOsmId(1353883890);
        TestUtil.validateConnector(c1353883890, SceneryWayConnector.WayConnectorType.MOTORWAY_ENTRY_JUNCTION, Boolean.FALSE, tm);

        //Zweite Auffahrt A61
        SceneryWayConnector c255574409 = (SceneryWayConnector) sceneryObjectList.findObjectByOsmId(255574409);
        TestUtil.validateConnector(c255574409, SceneryWayConnector.WayConnectorType.MOTORWAY_ENTRY_JUNCTION, Boolean.TRUE, tm);

        //Connector an der Abfahrt. Bekommt overlap reduce
        TestUtil.validateConnector((SceneryWayConnector) sceneryObjectList.findObjectByOsmId(2345486588L), SceneryWayConnector.WayConnectorType.SIMPLE_CONNECTOR, null, tm);

        //Der 225794249 liegt zwischen zwei bridges. Da gabs mal einen nicht sauberen Split.
        HighwayModule.Highway w225794249 = (HighwayModule.Highway) sceneryObjectList.findObjectByOsmId(225794249);
        List<MeshLine> ll = w225794249.getWayArea().getLeftLines(sceneryMesh.terrainMesh);
        assertEquals(3, ll.size(), "225794249.leftLines.length");

        //Ob 28 richtig sind? Auf jeden Fall plausibler als 9. NeeNee, die sind sehr groß. 9 würde schon gut passen. Oder? Gezählt komm ich schon auf 15
        //wahrscheinlich ein Hole Problem.
        //TODO assertEquals("scenery.backgrounds", 9/*10*/, sceneryMesh.getBackground().bgfiller.size());
        TestUtil.validateResult(sceneryMesh, logger, 0, 28, tm);

        //PML
        PortableModelList pml = processor.pml;
        assertMinimumElevation(pml, 68);
        TestUtil.assertBasicGraph(SceneryContext.getInstance().getGraph(SceneryObject.Category.ROAD), 68);

        //30.8.18: 55->59, warum auch immer? Irgendwie wegen gapfiller.59->67 wegen Ramps (so wenig?)
        //2.4.19: Wegen TopologyException (seit float->double?) beim Cut: 67->61
        //5.4.19: 61->62, 9.4.19: 62->68, 11.4.19: 68->5??(difference Fehler), 18.4.19: 5->59, durch Aeroway->58? 3.6.19 58->59, 23.7.19: area mit sofortcut: 59->44?13.8.19->61,->60 wegen Sonderlocke
        //27.8.19: 60->76->74->4??->0 wegen SmartBG
        assertBackground(pml, processor.getResults().sceneryresults.sceneryMesh.getBackground(), 0);
    }

    /**
     * Mit River, Lake
     *
     * @throws IOException
     */
    @Test
    @Disabled // 2.4.24
    public void testZieverichSuedGrid() throws IOException {
        doZieverichSued("Zieverich-Sued", 17);
    }

    public void doZieverichSued(String gridname, int expectedobjects) throws IOException {
        String zieverichsued = SceneryBuilder.osmdatadir + "/Zieverich-Sued.osm.xml";
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        //26.9.18: Nur mit superdetailed tritt eine TopologyException: found non-noded intersection between LINESTRING auf
        SceneryBuilder sb = new SceneryBuilder();
        SceneryBuilder.FTR_SMARTBG = true;

        Processor processor = sb.execute(zieverichsued, "superdetailed", gridname, false, customconfig, MATERIAL_FLIGHT).processor;
        SceneryMesh sceneryMesh = processor.getResults().sceneryresults.sceneryMesh;

        //P-Shaped way 38809414 wurde an 275038674 (index 4 des 38809414) gesplittet.
        HighwayModule.Highway roadP38809414 = (HighwayModule.Highway) sceneryMesh.sceneryObjects.findObjectByOsmId(38809414);
        SceneryWayConnector c275038674 = (SceneryWayConnector) sceneryMesh.sceneryObjects.findObjectByOsmId(275038674);
        assertEquals(roadP38809414.getEndConnector(), c275038674, "roadP38809414.endMode.wayConnector== c275038674");


        SceneryWayConnector c320122390 = (SceneryWayConnector) sceneryMesh.sceneryObjects.findObjectByOsmId(320122390);
        //TODO SceneryTestUtil.validateConnector(c320122390, SceneryWayConnector.WayConnectorType.SIMPLE_INNER_DOUBLE_JUNCTION, Boolean.TRUE);

        //das ist Minimum
        float expectedelevation = 68;
        assertMinimumElevation(sceneryMesh.sceneryObjects.objects, expectedelevation);
        //TODO 22.8.19 assertMinimumElevation(pml, expectedelevation);

        WaterModule.Waterway erft = (WaterModule.Waterway) sceneryMesh.sceneryObjects.findObjectByOsmId(7778157);

        PortableModelList pml = processor.pml;

        assertEquals(5.0, erft.getWidth(), "erft.width");


    }

    /**
     * Das was in maingrid steckt. Eigentlich ein bischen gross für einen Unittest, darum nur optional.
     * 7.8.18: Das mit dem maingrid.xml muss eh mal überarbeitet werden, oder auch nicht? Wegen fehlender Teile
     * muss es aber mal neu gemacht werden. Dafuer gibt es jetzt A4A3A1.osm.xml
     *
     * @throws IOException
     */
    //@Test
    public void testA4A3A1WithElevation68() throws IOException {
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        dotestA4A3A1(customconfig);
    }

    //@Test
    public void testA4A3A1NoElevation() throws IOException {
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", null);
        dotestA4A3A1(customconfig);
    }

    private void dotestA4A3A1(Configuration customconfig) throws IOException {
        String a4a3a1 = SceneryBuilder.osmdatadir + "/A4A3A1.osm.xml";

        Processor processor = new SceneryBuilder().execute(a4a3a1, "poc", "A4A3A1", false, customconfig, MATERIAL_FLIGHT).processor;
        HighwayModule roadModule = (HighwayModule) processor.scf.getModule("HighwayModule");
        assertNotNull(roadModule, "HighwayModule");
        //de.yard.threed.osm2scenery.modules.GraphModule graphModule = (de.yard.threed.osm2scenery.modules.GraphModule) processor.scf.getModule("GraphModule");
        //assertNotNull("GraphModule", graphModule);


        SceneryMesh sceneryMesh = processor.getResults().sceneryresults.sceneryMesh;
        // 798 Roads. Backgrounds? eigentlich 8, aber zwei sind extrem kompley/gross und scheitern. 7.8.18: Ich brauche bessere Rohdaten.
        // 14.8.18: Das mit den Holes ist jetzt durch gapfiller besser
        //Die Referenzwerte sind alle im Fluss
        //TODO und wo ist der Rhein geblieben? jetzt mit gaps 960
        //assertEquals("scenery.areas", 960, sceneryMesh.sceneryObjects.size());
        //1->40
        //assertEquals("scenery.backgrounds", 40, sceneryMesh.getBackground().background.size());

        SceneryFlatObject road = (SceneryFlatObject) sceneryMesh.sceneryObjects.objects.get(0);
        // durch den cut wohl nur noch 8

        PortableModelList pml = processor.pml;

    }

    @Test
    @Disabled // 2.4.24
    public void testTestData() throws IOException {
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        String testdataOSM = SceneryBuilder.osmdatadir + "/TestData-Simplified.osm.xml";
        SceneryBuilder sb = new SceneryBuilder();
        SceneryBuilder.FTR_SMARTGRID = true;
        SceneryBuilder.FTR_SMARTBG = true;
        //Simple wegen sonst ueberhaengendem DeadEnd Zipfel
        Processor processor = sb.execute(testdataOSM, "superdetailed", "TestData", false, customconfig, MATERIAL_FLIGHT).processor;

        SceneryMesh sceneryMesh = processor.getResults().sceneryresults.sceneryMesh;
        AerowayModule.Runway runway = (AerowayModule.Runway) sceneryMesh.sceneryObjects.findObjectsByCreatorTag("Runway").get(0);
        //Die zwei Runway Segments  4 und 5  liegen links cut auf der Boundary
        AbstractArea segmentonboundary4 = runway.getArea()[4];
        AbstractArea segmentonboundary5 = runway.getArea()[5];
        //Es sind zwei dazugekommen. Wiki Skizze 67. Eigentlich ist es nur eine, aber beim FTR_TRACKEDBPCOORS
        //wird die rechte faelschlicherweise/verfrueht mit registriert.12.8.19:einer wniger. 15.8.19: Jetzt nur noch 2! TODO pruefen.
        //16.8.19 TODO assertEquals("", 4 + ((SceneryBuilder.FTR_TRACKEDBPCOORS) ? 2 : 1) - 3, segmentonboundary4.getEleConnectorGroups().get(0).eleConnectors.size());

        SceneryAreaObject building = (SceneryAreaObject) sceneryMesh.sceneryObjects.findObjectByOsmId(109);
        assertTrue(building.getArea()[0].isOverlay, "building.overlay");
        assertTrue(building.isCut, "building.isCut");
        //assertFalse("building.assCut", building.getArea()[0].wasCut);

        //1 LazyCuts in "Simplified"
        assertEquals(4 + 2 + 1, processor.gridCellBoundsused.getPolygon().getCoordinates().length, "gridCellBounds.coordinates.size");
        assertEquals(0, SceneryContext.getInstance().warnings.size(), "warnings");

        TerrainMesh tm = sceneryMesh.terrainMesh;
        //22 ist plausibel
        assertEquals(22, tm.getSharedLines().size(), "TerrainMesh.sharedLines.size");
        assertTrue(tm.isValid(true), "TerrainMesh.valid");

        PortableModelList pml = processor.pml;
        SimpleGeometry runwaygeo = pml.findObject("RUNWAY").geolist.get(0);

        //Dumper.dumpVertexData(logger, runwaygeo.getVertices(), runwaygeo.getUvs(), runwaygeo.getIndices());

        //das ist Minimum
        float expectedelevation = 68;
        assertMinimumElevation(sceneryMesh.sceneryObjects.objects, expectedelevation);
        assertMinimumElevation(pml, expectedelevation);

    }

    @Test
    @Disabled // 2.4.24
    public void testWayland() throws IOException, InvalidDataException {
        String zieverichsued = /*SceneryBuilder.osmdatadir*/"src/main/resources/Wayland.osm.xml";
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        //26.9.18: Nur mit superdetailed tritt eine TopologyException: found non-noded intersection between LINESTRING auf
        SceneryBuilder sb = new SceneryBuilder();
        SceneryBuilder.FTR_SMARTBG = true;

        ProcessResults processResults = sb.execute(zieverichsued, "superdetailed", "Wayland", false, customconfig, MATERIAL_MODEL);
        SceneryMesh sceneryMesh = processResults.results.sceneryresults.sceneryMesh;
        assertMinimumElevation(processResults.processor.pml, 68);

        assertNotNull(processResults.railwayGraph, "railwayGraph");
        assertNotNull(processResults.roadGraph, "roadGraph");

        TestUtil.assertBasicGraph(processResults.roadGraph, 68);
        TestUtil.assertBasicGraph(processResults.railwayGraph, 68);

        PortableModelList pml = processResults.getPortableModelList();
        assertWaylandPml(pml);

        GltfBuilder gltfBuilder = new GltfBuilder();
        GltfBuilderResult builderResult = gltfBuilder.process(pml);
        System.out.println(builderResult.gltfstring);

        BundleResource gltfbr = new BundleResource(new InMemoryBundle("wayland", builderResult.gltfstring, builderResult.bin), "wayland.gltf");
        try {
            LoaderGLTF lf1 = LoaderGLTF.buildLoader(gltfbr, null);
            PortableModelList reloadedPml = lf1.doload();
            assertWaylandPml(reloadedPml);
        } catch (InvalidDataException e) {
            throw e;
        }
    }

    private void assertWaylandPml(PortableModelList pml) {
        //check material. As of 15.11.21 there are seven different
        int expected = 7;
        assertEquals(expected, pml.materials.size(), "pml.materials");
        assertEquals(expected, pml.getObjectCount(), "pml.objects");
        PortableModelDefinition roadobject = pml.findObject("ROAD");

        assertPortableMaterial(pml.findMaterial("WATER"), null, ConfigUtil.parseColor("#0000FF"));
        assertPortableMaterial(pml.findMaterial("ROAD"), null, ConfigUtil.parseColor("#555555"));
        //gibts nicht? assertPortableMaterial(pml.findMaterial("TERRAIN_DEFAULT"), null, ConfigUtil.parseColor("#005500"));
    }

    private void assertPortableMaterial(PortableMaterial mat, String expectedTexture, java.awt.Color expectedColor) {
        assertNotNull(mat, "material");
        assertNull(mat.texture, "texture");
        if (expectedColor != null) {
            assertColor(expectedColor, mat.color);
        } else {
            assertNull(mat.color);
        }
        if (expectedTexture != null) {
            assertEquals(expectedTexture, mat.texture);
        } else {
            assertNull(mat.texture);
        }
    }

    /**
     * 18.4.19: Jetzt mit schnellerer CPU kann man auch EDDK mal wieder reinnehmen.
     * Z.Z. primär um irgendwelche exotischen Ausnahmen (Exceptions) zu entdecken.
     * Aber doch nur optional zuschalten.
     * "EDDK-Small" geht aber immer.
     *
     * @throws IOException
     */
    @Test
    @Disabled // 2.4.24
    public void testEDDKSmallsuperdetailed() throws IOException {
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        dotestEDDKSmall(customconfig, "superdetailed");
    }

    private void dotestEDDKSmall(Configuration customconfig, String configsuffix) throws IOException {
        String desdorfk41 = SceneryBuilder.osmdatadir + "/EDDK-Small.osm.xml";

        Processor processor = new SceneryBuilder().execute(desdorfk41, configsuffix, "EDDK-Small", false, customconfig, MATERIAL_FLIGHT).processor;
        HighwayModule roadModule = (HighwayModule) processor.scf.getModule("HighwayModule");
        assertNotNull(roadModule, "HighwayModule");

        SceneryMesh sceneryMesh = processor.getResults().sceneryresults.sceneryMesh;

        List<SceneryObject> taxiwayareas = sceneryMesh.sceneryObjects.findObjectsByCreatorTag("TaxiwayArea");
        logger.debug("found " + taxiwayareas.size() + " taxiwayareas");
        //wieviele auch immer das sind, auf jeden Fall eine Menge.
        //TODO 12.8.19 assertTrue("taxiwayareas.size>80", taxiwayareas.size() > 80);

        //assertTrue("runway.cutIntoBackground", runway.cutIntoBackground);
        PortableModelList pml = processor.pml;


        SceneryFlatObject apronE = (SceneryFlatObject) sceneryMesh.sceneryObjects.findObjectByOsmId(217349177);
        //TODO apronE ist durch Taxiway in drei Teile zerschnitten (aber noch nicht, ist noch geklebt)

        SceneryFlatObject starC = (SceneryFlatObject) sceneryMesh.sceneryObjects.findObjectByOsmId(218058763);

        PortableMaterial starCpmlMaterial = pml.findMaterial("OSM218058763");
        assertNotNull(starCpmlMaterial, "starCpmlMaterial=OSM218058763");

        //das ist Minimum
        float expectedelevation = 68;
        assertMinimumElevation(sceneryMesh.sceneryObjects.objects, expectedelevation);
        //TODO 19.8.19 assertMinimumElevation(pml, expectedelevation);
    }

    /**
     * see comment EDDKSmallTest
     *
     * @throws IOException
     */
    //@Test
    public void testEDDKsuperdetailed() throws IOException {
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        dotestEDDK(customconfig, "superdetailed");
    }

    private void dotestEDDK(Configuration customconfig, String configsuffix) throws IOException {
        String desdorfk41 = SceneryBuilder.osmdatadir + "/EDDK-Complete-Large.osm.xml";

        Processor processor = new SceneryBuilder().execute(desdorfk41, configsuffix, "EDDK-Complete-Large", false, customconfig, MATERIAL_FLIGHT).processor;
        HighwayModule roadModule = (HighwayModule) processor.scf.getModule("HighwayModule");
        assertNotNull(roadModule, "HighwayModule");


    }


    private void validateBridgeApproach(HighwayModule.Highway roadToBridgeFromNorth, boolean superdetailed, TerrainMesh tm) {
        assertFalse(roadToBridgeFromNorth.getArea()[0].isOverlay, "overlayway");
        //just detailed->ASPHALT
        assertEquals("ASPHALT", roadToBridgeFromNorth.getMaterial().getName(), "roadToBridgeFromNorth.material");
        //Warum 7 und 3?
        assertEquals(7/*5*/, roadToBridgeFromNorth.getArea()[0].getPolygon(tm).getCoordinates().length, "scenery.roadToBridgeFromNorth.coordinates");
        assertEquals(3/*2*/, roadToBridgeFromNorth.getEleConnectorGroups().eleconnectorgroups.size(), "scenery.roadToBridgeFromNorth.coordinates");

        TestUtil.assertEleConnectorGroup("scenery.roadToBridgeFromNorth.elevation", roadToBridgeFromNorth.getEleConnectorGroups(), new float[]{68f + 6f, 68f + 3f, 68f});
        //assertFloat("scenery.roadToBridgeFromNorth.elevation", 68f + 6f, roadToBridgeFromNorth.getEleConnectorGroups().eleconnectorgroups.get(0).getElevation().floatValue());
        //assertFloat("scenery.roadToBridgeFromNorth.elevation", 68f + 3f, roadToBridgeFromNorth.getEleConnectorGroups().eleconnectorgroups.get(1).getElevation().floatValue());
        //assertFloat("scenery.roadToBridgeFromNorth.elevation", 68f, roadToBridgeFromNorth.getEleConnectorGroups().eleconnectorgroups.get(2).getElevation().floatValue());


        // 6 ist plausibel
        VertexData vertexData = roadToBridgeFromNorth.getArea()[0].getVertexData();
        assertEquals(6, vertexData.vertices.size(), "road.vertices");
        //16.4.19: durch Standardway hat sich die Reihenfolge geändert. Hmmm. 20.5.19: Ist aber plausibel.
        assertEquals(74, (float) vertexData.vertices.get(0).z, "road.vertex0.z");
        assertEquals(74, (float) vertexData.vertices.get(1).z, "road.vertex1.z");
        assertEquals(71, (float) vertexData.vertices.get(2).z, "road.vertex2.z");
        assertEquals(71, (float) vertexData.vertices.get(3).z, "road.vertex3.z");
        assertEquals(68, (float) vertexData.vertices.get(4).z, "road.vertex4.z");
        assertEquals(68, (float) vertexData.vertices.get(5).z, "road.vertex5.z");

        //just detailed->ASPHALT
        if (superdetailed) {
            de.yard.threed.core.testutil.TestUtils.assertVector2(new Vector2(0, 0.375), vertexData.getUV(0), "uv[0]");
            de.yard.threed.core.testutil.TestUtils.assertVector2(new Vector2(0, 0.25), vertexData.getUV(1), "uv[1]");
            de.yard.threed.core.testutil.TestUtils.assertVector2(new Vector2(3.30973006351319, 0.375), vertexData.getUV(2), "uv[2]");
            de.yard.threed.core.testutil.TestUtils.assertVector2(new Vector2(3.30973006351319, 0.25), vertexData.getUV(3), "uv[3]");
        }
    }

    /**
     * Ist das generisch für alle Bridges? Eher nicht.
     */
    private void validateBridge(BridgeModule.Bridge bridge, ScenerySupplementAreaObject bridgegroundfillerfromobjectlist, HighwayModule.Highway roadToBridgeFromNorth, TerrainMesh tm) {
        assertFalse(bridge.isTerrainProvider(), "isTerrainProvider");
        assertTrue(bridge.isClipped(), "bridge.clipped");
        assertEquals("Bridge", bridge.getCreatorTag(), "creatortag");
        assertTrue(bridge.getGroundFiller() == bridgegroundfillerfromobjectlist, "BridgeGroundFiller");
        assertEquals("BridgeGroundFiller", bridge.getGroundFiller().getCreatorTag(), "GroundFiller.creatortag");

        SceneryWayConnector startConnector = bridge.getStartConnector();
        TestUtil.validateConnector(startConnector, SceneryWayConnector.WayConnectorType.SIMPLE_CONNECTOR, null, tm);

        SceneryWayConnector endConnector = bridge.getEndConnector();
        TestUtil.validateConnector(endConnector, SceneryWayConnector.WayConnectorType.SIMPLE_CONNECTOR, null, tm);


        assertEquals(68f + 6f, bridge.getEleConnectorGroups().eleconnectorgroups.get(0).getElevation().floatValue(), "scenery.bridge.elevation");
        assertEquals(68f + 6f, bridge./*.roadorrailway*/getEleConnectorGroups().eleconnectorgroups.get(0).getElevation().floatValue(), "scenery.bridge.roadorrailway.elevation");
        assertEquals(68f + 6f, (float) ((SceneryFlatObject) bridge/*.roadorrailway*/).getArea()[0].getPolygon(tm).getCoordinates()[0].z, "scenery.bridge.roadorrailway.elevation");
        //assert elevations of the northern ramp. 17.7.19: Ist ramp2 nicht north right?
        ScenerySupplementAreaObject rampnorthleft = bridge.endHead.ramp0;
        Coordinate[] rampcoors = rampnorthleft.getArea()[0].poly.polygon.getCoordinates();
        //Validation bezieht sich speziell auf north left. Flag ist jetzt wasCut
        assertTrue(rampnorthleft.isCut, "Ramp was cut");
        assertFalse(rampnorthleft.getArea()[0].wasCut, "Ramp was cut");
        //start of north road isType at bridge. egrnorth ist am Brückenkopf.
        EleConnectorGroup egrnorth = roadToBridgeFromNorth.getEleConnectorGroups().eleconnectorgroups.get(0);
        TestUtil.assertCoordinate("egrnorth.coordinate", JtsUtil.toCoordinate(roadToBridgeFromNorth.mapWay.getMapNodes().get(0).getPos()), egrnorth.getCoordinate());
        assertTrue(EleConnectorGroup.getGroup(rampcoors[0], false, "for test", false, tm) == roadToBridgeFromNorth.getEleConnectorGroups().eleconnectorgroups.get(1), "Ramp.elegrooup[0]");
        assertTrue(EleConnectorGroup.getGroup(rampcoors[1], false, "for test", false, tm) == roadToBridgeFromNorth.getEleConnectorGroups().eleconnectorgroups.get(0), "Ramp.elegrooup[1]");
        assertTrue(EleConnectorGroup.getGroup(rampcoors[2], false, "for test", false, tm) == bridge.gap.getEleConnectorGroups().eleconnectorgroups.get(0), "Ramp.elegrooup[2]");
        //TODO 24.4.19 assertFloat("bridge.rampnorthleft.elevation", 68f + 3f, rampnorthleft.getEleConnectorGroups().eleconnectorgroups.get(0).getElevation().floatValue());
        //TODO 24.4.19 assertFloat("bridge.rampnorthleft.elevation", 68f + 6f, rampnorthleft.getEleConnectorGroups().eleconnectorgroups.get(1).getElevation().floatValue());

        assertEquals(3, bridge.startHead.ramp0.getArea()[0].getPolygon(tm).getCoordinates().length - 1, "Ramp0.polygon.size");
        //6 statt 4 wegen mesh ist plausibel
        assertEquals( /*4*/6, bridge.gap.getArea()[0].getPolygon(tm).getCoordinates().length - 1, "Gapfiller.polygon.size");
        //genau eine Group mit 14 Eles (4 vom Polygon + 4 sideramp + 6(?) background, scheint plausibel)
        //TODO 5.9.19 assertEquals("Gapfiller.elegroups", 1, bridge.gap.getEleConnectorGroups().eleconnectorgroups.size());
        //4 vom gap+ 4 von Ramps, 1 vom cut einer ramp, 10 vom BG=19, war mal 14. Jetzt wieder. 12.6.19:evtl. 14->8
        //TODO 27.8.19 assertEquals("Gapfiller.elegroups.size", ((SceneryBuilder.FTR_TRACKEDBPCOORS) ? 8 : 14), bridge.gap.getEleConnectorGroups().eleconnectorgroups.get(0).eleConnectors.size());
        //Der Triangulator ist da sehr eigenwillig, wieviele Triangle er baut? Eigentlich ist die Ramp ja eins. Vor allem aber wohl wegen des cut an ramp1.
        //5.4.19 5->23, warum auch immer. 9.4.19 23->3 und 5->9, 11.4.19: 3->5,9->5,5->3
        //26.8.19: Wegen Earclipping jetzt wieder deutlich weniger.
        assertEquals(3, bridge.startHead.ramp0.getArea()[0].getVertexData().vertices.size(), "Ramp0.vertices");
        assertEquals(3/*80*/, bridge.startHead.ramp1.getArea()[0].getVertexData().vertices.size(), "Ramp1.vertices");
        assertEquals(3/*9/*5*/, bridge.endHead.ramp0.getArea()[0].getVertexData().vertices.size(), "Ramp2.vertices");
        //die Wert sind von der Triangulation abhängig, je nach dem wo die zwei zusätzlichen Vertices liegen.
        //auf jeden Fall brauchts einmal
        assertInterpolatedElevation("Ramp2", bridge.endHead.ramp0.getArea()[0].getVertexData().vertices, new float[]{68, 71, 74});
        assertEquals(3/*7/*5*/, bridge.endHead.ramp1.getArea()[0].getVertexData().vertices.size(), "Ramp3.vertices");
        //6 statt 4 wegen mesh ist plausibel
        assertEquals( /*4*/6, bridge.gap.getArea()[0].getVertexData().vertices.size(), "Gapfiller.vertices");
        TestUtil.validateSupplement("", bridge.startHead.ramp0, tm);
        TestUtil.validateSupplement("", bridge.startHead.ramp1, tm);
        TestUtil.validateSupplement("", bridge.endHead.ramp0, tm);
        TestUtil.validateSupplement("", bridge.endHead.ramp1, tm);
    }


    public static void validateEleGroups(long osmid, SceneryObjectList sceneryObjects) {
        SceneryFlatObject sfo = (SceneryFlatObject) sceneryObjects.findObjectByOsmId(osmid);
        Coordinate[] coors = sfo.getArea()[0].poly.polygon.getCoordinates();
        for (Coordinate c : coors) {
            EleConnectorGroup egr = EleConnectorGroup.getGroup(c);
            assertNotNull(egr, "EleGroup of " + c);
        }
    }


    /**
     * Assert minimum elavation for all vertices.
     */
    private void assertMinimumElevation(PortableModelList pml, float minelevationexpected) {
        for (int k = 0; k < pml.getObjectCount(); k++) {
            PortableModelDefinition object = pml.getObject(k);
            for (int j = 0; j < object.geolist.size(); j++) {
                SimpleGeometry geo = object.geolist.get(j);

                for (int i = 0; i < geo.getVertices().size(); i++) {
                    // kleine Toleranz (wegen Rundungsfehler?)
                    assertTrue(
                            minelevationexpected <= geo.getVertices().getElement(i).getZ() + 0.1f, "no minelevation: vertex.z[" + i + "] in object " + object.name + "(material=" + object.geolistmaterial.get(j) + "), minelevationexpected=" + minelevationexpected +
                                    ",found=" + geo.getVertices().getElement(i).getZ());
                }
            }
        }
    }

    /**
     * Assert minimum elavation for all scenery objects
     */
    private void assertMinimumElevation(List<SceneryObject> objlist, float minelevationexpected) {
        for (SceneryObject object : objlist) {
            for (int j = 0; j < object.getEleConnectorGroups().size(); j++) {
                EleConnectorGroup g = object.getEleConnectorGroups().get(j);
                // kleine Toleranz (wegen Rundungsfehler?)
                boolean tooLow = minelevationexpected > g.getElevation() + 0.1f;
                if (tooLow) {
                    int h = 9;
                }
                assertTrue(!tooLow, "elegroup.elevation too low in " + object.creatortag + ", isType " + g.getElevation() + ", expected " + minelevationexpected);
            }
        }
    }

    /**
     * Assert interpolated elevation of vertices. Important for additional vertices by triangulation.
     */
    private void assertInterpolatedElevation(String label, List<Coordinate> vertices, float[] expectedexactlyonce) {
        for (int j = 0; j < expectedexactlyonce.length; j++) {
            int found = 0;

            for (int i = 0; i < vertices.size(); i++) {
                float z = (float) vertices.get(i).z;
                if (z == expectedexactlyonce[j]) {
                    found++;
                }
            }
            if (found != 1) {
                fail("assertInterpolatedElevation: z " + expectedexactlyonce[j] + " found " + found + " times for " + label);
            }

        }
    }


    private void assertBackground(PortableModelList pml, Background background, int areasexpected) {
        SimpleGeometry terraingeo = pml.findObject(TERRAIN_DEFAULT.getName()).geolist.get(0);
        int tris = terraingeo.getIndices().length / 3;
        assertEquals(areasexpected, background.background.size(), "background.size");
        if (!SceneryBuilder.FTR_SMARTBG) {
            int expectedtriangles = 0;
            for (int i = 0; i < background.background.size(); i++) {
                expectedtriangles += background.background.get(i).vertexData.indices.length / 3;
            }
            assertEquals(expectedtriangles, tris, "background.triangles");
        }

    }

    private void assertColor(java.awt.Color expected, Color actual) {
        assertEquals(expected.getRed(), actual.getRasInt());
        assertEquals(expected.getGreen(), actual.getGasInt());
        assertEquals(expected.getBlue(), actual.getBasInt());
        assertEquals(expected.getAlpha(), actual.getAlphaasInt());
        //assertEquals(expected.toString(),actual.toString());
    }
}