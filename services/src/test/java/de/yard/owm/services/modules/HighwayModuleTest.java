package de.yard.owm.services.modules;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.owm.services.osm.OsmElementService;
import de.yard.owm.services.persistence.PersistedMeshFactory;
import de.yard.owm.services.persistence.TerrainMeshManager;
import de.yard.owm.services.util.OsmXmlParser;
import de.yard.owm.testutils.TestData;
import de.yard.owm.testutils.TestUtils;
import de.yard.threed.TestUtil;
import de.yard.threed.core.Pair;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.geometry.SimpleGeometry;
import de.yard.threed.core.loader.PortableModelDefinition;
import de.yard.threed.core.loader.PortableModelList;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.graph.Graph;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.MapDataHelper;

import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.scenery.components.WayArea;
import de.yard.threed.osm2scenery.util.Dumper;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.OSMData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.yard.owm.testutils.TestUtils.loadFileFromClasspath;
import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.ROAD;
import static de.yard.threed.osm2world.Config.MATERIAL_FLIGHT;
import static org.junit.jupiter.api.Assertions.*;


/**
 * 11.4.19: Tests on module (OsmElementService) level (no Processor und ConversionFacade)
 * 3.4.24: Derived from traditional HighwayModuleTest
 */
@SpringBootTest
@Slf4j
public class HighwayModuleTest {
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(HighwayModuleTest.class);

    @Autowired
    OsmElementService osmElementService;

    @Autowired
    TerrainMeshManager terrainMeshManager;

    /**
     * Den clip() hier nicht testen, da haengt zu viel dran? Connector clippen jetzt aber teilweise in createPolygon()?
     *
     * @throws IOException
     */
    /*3.4.24 @Test
    public void testDesdorf() throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/Desdorf.osm.xml", "Desdorf", "superdetailed");

        HighwayModule roadModule = new HighwayModule();
        SceneryObjectList roads = roadModule.applyTo(SceneryTestUtil.mapData);
        roadModule.classify(SceneryTestUtil.mapData);

        List<SceneryObject> knownobjects = new ArrayList<>();

        // List<RoadModule.Road> roads = roadModule.getRoads();
        //19.4.19: 12->20 wegen Connector
        assertEquals(20, roads.size(), "roads");
        assertEquals( /*keine ausserhalb mehr73* /28, SceneryContext.getInstance().getGraph(SceneryObject.Category.ROAD).getNodeCount(), "roads.nodes");
        HighwayModule.Highway k41vonunten;// = SceneryContext.getInstance().getRoadByOsmId(24927839));
        k41vonunten = (HighwayModule.Highway) roads.findObjectByOsmId(24927839);
        assertNotNull( k41vonunten,"K41");
        knownobjects.add(k41vonunten);

        //Polygon gibt es noch nicht
        assertNull(k41vonunten.getArea(), "K41.polygon");
        //ist n54286220->n255563538 mit 6 Nodes, 54286220 als erste im Sueden und die vierte ist DummyNode für Grid
        MapWay mapway = k41vonunten.mapWay;
        MapNode griddummynode = mapway.getMapNodes().get(3);
        assertTrue(MapDataHelper.isDummyNode(griddummynode), "dummy node");

        // rearrangeForWayCut() is done later, so here we cannot create TerrainMesh
        TerrainMesh tm = null;

        k41vonunten.buildEleGroups();
        k41vonunten.createPolygon(null, null, tm);
        assertEquals(1, k41vonunten.getArea().length, "K41.polygons");
        //double polygonarea = k41vonunten.getArea().poly.uncutPolygon.getArea();
        //logger.debug("vor cut: polygonarea=" + k41vonunten.getArea().poly.getArea());


        SceneryWayObject k41upper = (HighwayModule.Highway) roads.findObjectByOsmId(182152619);
        assertNotNull( k41upper,"k41upper");
        knownobjects.add(k41upper);
        k41upper.buildEleGroups();
        k41upper.createPolygon(null, null, tm);

        //Die K43 (107468171) beginnt an 445410497 im Westen
        HighwayModule.Highway k43 = (HighwayModule.Highway) roads.findObjectByOsmId(107468171);
        assertNotNull(k43,"k43");
        k43.buildEleGroups();
        k43.createPolygon(null, null, tm);
        assertEquals(4, k43.innerConnector.size(), "k43.innerConnectorIndex.size");
        knownobjects.add(k43);

        //Der Weg südlich Richtung Haus ("Gut Desdorf") schneidet durch das Farmland.
        SceneryWayObject gutdesdorf = (SceneryWayObject) roads.findObjectByOsmId(37935545);
        gutdesdorf.buildEleGroups();
        gutdesdorf.createPolygon(null, null, tm);
        assertEquals(4, gutdesdorf.getEleConnectorGroups().size(), "gutdesdorf.elegroups.size");

        knownobjects.add(gutdesdorf);

        //Der Weg Richtung Norden mit einem Dead end.
        SceneryWayObject gutdesdorfnorth = (SceneryWayObject) roads.findObjectByOsmId(33817499);
        gutdesdorfnorth.buildEleGroups();
        gutdesdorfnorth.createPolygon(null, null, tm);
        knownobjects.add(gutdesdorfnorth);

        //k43 ist von West nach Ost
        assertEquals(SceneryWayObject.WayOuterMode.GRIDBOUNDARY, k43.startMode, "k43.startMode==CONNECTOR");
        assertTrue(k43.endMode == SceneryWayObject.WayOuterMode.CONNECTOR, "k43.endMode==CONNECTOR");
        assertTrue(k41vonunten.startMode == SceneryWayObject.WayOuterMode.GRIDBOUNDARY, "k41lower.startMode==GRIDBOUNDARY");
        assertTrue(k41vonunten.endMode == SceneryWayObject.WayOuterMode.CONNECTOR, "k41lower.endMode==CONNECTOR");
        assertTrue(k41vonunten.endConnector == k43.endConnector, "k41lower.endMode.wayConnector== k43.endMode.wayConnector");
        assertTrue(k41upper.startMode == SceneryWayObject.WayOuterMode.CONNECTOR, "k41upper.startMode==CONNECTOR");
        assertTrue(gutdesdorf.startMode == SceneryWayObject.WayOuterMode.CONNECTOR, "gutdesdorf.startMode==CONNECTOR");
        assertEquals(0, gutdesdorf.startNode, "gutdesdorf.startNode");
        assertTrue(gutdesdorf.endMode == SceneryWayObject.WayOuterMode.GRIDBOUNDARY, "gutdesdorf.endMode==GRIDBOUNDARY");
        assertEquals(3, gutdesdorf.endNode, "gutdesdorf.endNode");

        //assertTrue("k41upper.endMode==GRIDBOUNDARY", k41upper.endMode == SceneryWayObject.WayOuterMode.GRIDBOUNDARY);
        //assertTrue("k41upper.endMode.wayConnector== k43.endMode.wayConnector", k41upper.endMode.wayConnector == k41lower.endMode.wayConnector);

        //GutDesdorf(nach Sueden)/Desdorfer Strasse
        SceneryWayConnector connectorK43GutDesdorf = SceneryContext.getInstance().wayMap.getConnector(ROAD, Long.valueOf(445409643));
        assertNotNull( connectorK43GutDesdorf,"connectorK43GutDesdorf");
        assertEquals(2, connectorK43GutDesdorf.getWaysCount(), "connectorK43GutDesdorf.ways.size");
        assertEquals(SceneryWayConnector.WayConnectorType.SIMPLE_INNER_SINGLE_JUNCTION, connectorK43GutDesdorf.getType(), "connectorK43GutDesdorf.type==MIDWAY_JUNCTIOM");

        WayArea k43area = (WayArea) k43.getArea()[0];
        //assertEquals("k43.size",0,k43area.LeftOutline().size());
        for (long osmid : new long[]{225794271, 24879711}) {
            SceneryWayObject swo = (SceneryWayObject) roads.findObjectByOsmId(osmid);
            knownobjects.add(swo);
            swo.buildEleGroups();
            swo.createPolygon(null, null, tm);
        }

        // Connector und clip

        SceneryWayConnector k41k43connector = (SceneryWayConnector) roads.findObjectByOsmId(255563538);
        assertEquals(k43.getOsmIdsAsString(), k41k43connector.getWay(k41k43connector.minorway).getOsmIdsAsString(), "k41k43connector.minorway");
        assertEquals(k41upper.getOsmIdsAsString(), k41k43connector.getMajor0().getOsmIdsAsString(), "k41k43connector.major0");
        assertEquals(k41vonunten.getOsmIdsAsString(), k41k43connector.getMajor1().getOsmIdsAsString(), "k41k43connector.major1");

        k41k43connector.createPolygon(null, null, tm);
        assertNotNull(k41k43connector.getArea(),"k41k43connector.area");
        assertEquals(5, k41k43connector.getArea()[0].getPolygon(tm).getCoordinates().length, "k41k43connector.polygon.size");

        //way[0] ist von unten, way[1] nach Westen und way[2] upper. Die angle sind unabhängig von major/minor
        assertEquals(4.693446904686727, k41k43connector.angles[1], 0.0001, "k41k43connector.angles[1]");
        assertEquals(3.086679996710109, k41k43connector.angles[2], 0.0001, "k41k43connector.angles[2]");
        assertEquals(2, k41k43connector.angleorder[1], "k41k43connector.angleorder[1]");
        assertEquals(1, k41k43connector.angleorder[2], "k41k43connector.angleorder[2]");

        // die noch fehlenden Connector,
        for (long osmid : new long[]{251517906, 255563537, 270353278}) {
            SceneryWayConnector swo = (SceneryWayConnector) roads.findObjectByOsmId(osmid);
            knownobjects.add(swo);
            swo.buildEleGroups();
            swo.createPolygon(null, null, tm);
        }


        k41vonunten.clip(tm);
        k41upper.clip(tm);
        k43.clip(tm);
        k41k43connector.clip(tm);

        //Coordinate "2" pruefen, auch nach einem cut
        Pair<Coordinate, Coordinate> pair0 = ((WayArea) k41vonunten.getArea()[0]).getEndPair()[0];
        Pair<Coordinate, Coordinate> pair1 = ((WayArea) k43.getArea()[0]).getEndPair()[0];
        TestUtil.assertCoordinate("coordinate2", pair0.getSecond(), pair1.getFirst());
        List<SceneryWayConnector> k43innerconnector = k43.getInnerConnector();
        assertEquals(4, k43innerconnector.size(), "k43.innerConnector.size");

        connectorK43GutDesdorf.createPolygon(null, null, tm);
        connectorK43GutDesdorf.clip(tm);
        gutdesdorf.clip(tm);

        pair0 = gutdesdorf.getWayArea().getStartPair(tm)[0];
        pair1 = connectorK43GutDesdorf.getAttachCoordinates(gutdesdorf.mapWay);
        TestUtil.assertCoordinate("gutdesdorf.connectorattach", pair0.getFirst(), pair1.getFirst());

        // 33817500 und 33817501 (der north ans grid grenzende) muessen durch SIMPLE_CONNECTOR verbunden sein
        SceneryWayObject w33817500 = (SceneryWayObject) roads.findObjectByOsmId(33817500);
        SceneryWayObject w33817501 = (SceneryWayObject) roads.findObjectByOsmId(33817501);
        assertEquals(SceneryWayConnector.WayConnectorType.SIMPLE_CONNECTOR, w33817501.getStartConnector().getType(), "w33817501.startconnector.type==SIMPLE_CONNECTOR");
        knownobjects.add(w33817500);
        knownobjects.add(w33817501);
        w33817500.buildEleGroups();
        w33817501.buildEleGroups();
        w33817500.createPolygon(null, null, tm);
        w33817501.createPolygon(null, null, tm);
        w33817501.getStartConnector().createPolygon(null, null, tm);
        gutdesdorfnorth.innerConnector.get(0).createPolygon(null, null, tm);
        gutdesdorfnorth.getStartConnector().createPolygon(null, null, tm);
        gutdesdorfnorth.clip(tm);
        w33817500.clip(tm);
        w33817501.clip(tm);
        pair0 = w33817500.getWayArea().getEndPair()[0];
        pair1 = w33817501.getWayArea().getStartPair(tm)[0];
        TestUtil.assertCoordinate("33817500/33817501 connectorattach", pair0.getFirst(), pair1.getFirst());
        gutdesdorfnorth.innerConnector.get(0).clip(tm);
        gutdesdorfnorth.getStartConnector().clip(tm);

        assertEquals(5, k43.getWayArea().getSegmentCount(), "k43.segments");

        // cut

        k41vonunten.cut(SceneryTestUtil.gridCellBounds);
        //logger.debug("nach cut: polygonarea=" + k41vonunten.getArea().poly.getArea());
        k43.cut(SceneryTestUtil.gridCellBounds);
        assertEquals(12, k43area.getLeftOutline().size(), "k43.leftoutline.size");
        k41upper.cut(SceneryTestUtil.gridCellBounds);
        gutdesdorf.cut(SceneryTestUtil.gridCellBounds);
        gutdesdorfnorth.cut(SceneryTestUtil.gridCellBounds);
        k41k43connector.cut(SceneryTestUtil.gridCellBounds);

        //Coordinate "2" pruefen
        pair0 = ((WayArea) k41vonunten.getArea()[0]).getEndPair()[0];
        pair1 = ((WayArea) k43.getArea()[0]).getEndPair()[0];
        TestUtil.assertCoordinate("coordinate2", pair0.getSecond(), pair1.getFirst());

        w33817500.cut(SceneryTestUtil.gridCellBounds);
        w33817501.cut(SceneryTestUtil.gridCellBounds);
        gutdesdorfnorth.innerConnector.get(0).cut(SceneryTestUtil.gridCellBounds);
        gutdesdorfnorth.getStartConnector().cut(SceneryTestUtil.gridCellBounds);

        //erst die noch fehlenden Connector, dann die Ways
        for (long osmid : new long[]{251517906, 255563537, 270353278,/*EOC* /225794271, 24879711}) {
            SceneryFlatObject swo = (SceneryFlatObject) roads.findObjectByOsmId(osmid);
            swo.clip(tm);
            swo.cut(SceneryTestUtil.gridCellBounds);
        }

        // TerrainMesh
        knownobjects.add(k41k43connector);
        knownobjects.add(gutdesdorfnorth.getStartConnector());
        knownobjects.add(gutdesdorfnorth.innerConnector.get(0));
        //dead end; isType null knownobjects.add(gutdesdorfnorth.getEndConnector());

        SceneryTestUtil.gridCellBounds.rearrangeForWayCut(knownobjects, null);
        tm = TerrainMesh.init(SceneryTestUtil.gridCellBounds);

        //11 statt 12, weil nicht alle Ways processed wurden
        assertEquals(13, SceneryTestUtil.gridCellBounds.getPolygon().getCoordinates().length, "gridCellBounds.coordinates.size");
        List<GridCellBounds.LazyCutObject> lazyCuts = SceneryTestUtil.gridCellBounds.getLazyCuts();
        assertEquals(6, lazyCuts.size(), "lazyCuts.size");


        for (SceneryObject ob : knownobjects) {
            ((SceneryFlatObject) ob).addToTerrainMesh(tm);
        }
        List<MeshLine> leftlines = k43.getWayArea().getLeftLines(tm);
        List<MeshLine> rightlines = k43.getWayArea().getRightLines(tm);
        assertEquals(4, leftlines.size(), "k43.leftLines.size");
        assertEquals(3, leftlines.get(0).length(), "k43.leftLine[0].size");
        assertEquals(5, leftlines.get(1).length(), "k43.leftLine[1].size");
        assertEquals(2, leftlines.get(2).length(), "k43.leftLine[2].size");
        assertEquals(2, leftlines.get(3).length(), "k43.leftLine[3].size");
        assertEquals(2, rightlines.size(), "k43.rightLines.size");
        assertEquals(6, rightlines.get(0).length(), "k43.rightLine[0].size");
        assertEquals(6, rightlines.get(1).length(), "k43.rightLine[1].size");


        //line4 isType 33817501 lazycut boundary
        MeshLine line4 = tm.lines.get(4);
        assertNotNull( line4.getLeft(),"33817501.lazycut.left");

        Coordinate cleft = w33817500.getWayArea().getStartPair(tm)[0].left();
        List<MeshLine> linesatcleft = tm.getMeshNode(cleft).getLines();
        assertEquals(3, linesatcleft.size(), "linesatcleft.size");
        MeshPolygon meshPolygon = tm.traversePolygon(gutdesdorfnorth.getWayArea().getLeftLines(tm).get(0), gutdesdorfnorth.getArea()[0], false);
        assertNotNull( meshPolygon,"gutdesdorfnorth.meshPolygon");

        //traverse BG.
        //line3 isType boundary at upper right between two lazycut
        //line5 isType boundary at upper between two lazycut
        Map<Integer, Integer> expectedEdgesPerBoundaryLine = new HashMap<>();
        expectedEdgesPerBoundaryLine.put(1, 4);
        expectedEdgesPerBoundaryLine.put(3, 7);
        expectedEdgesPerBoundaryLine.put(5, 8);
        expectedEdgesPerBoundaryLine.put(7, 3);
        expectedEdgesPerBoundaryLine.put(9, 3);
        expectedEdgesPerBoundaryLine.put(11, 4);

        for (int lineindex : expectedEdgesPerBoundaryLine.keySet()) {
            MeshLine meshLine = tm.lines.get(lineindex);
            if (lineindex == 1) {
                int h = 9;
            }
            meshPolygon = tm.traversePolygon(meshLine, null, true);
            assertNotNull( meshPolygon,"meshPolygon");
            assertEquals((int) expectedEdgesPerBoundaryLine.get(lineindex), meshPolygon.lines.size(), "meshPolygon.size");
        }


        // VertexData

        k43.triangulateAndTexturize(tm);
        VertexData vertexData = k43.getVertexData();
        Dumper.dumpVertexData(logger, vertexData);
        //2 von den 10 Nodes sind OUTSIDE
        assertEquals(16 + k43innerconnector.size() * 2, vertexData.vertices.size(), "vertices");

        TestUtils.assertVector2( new Vector2(0, 0.875), vertexData.getUV(0),"uv[0]");
        TestUtils.assertVector2( new Vector2(0, 0.8125), vertexData.getUV(1),"uv[1]");
        TestUtils.assertVector2( new Vector2(0.48913448892358063, 0.875), vertexData.getUV(2),"uv[2]");
        TestUtils.assertVector2(new Vector2(0.48913448892358063, 0.8125), vertexData.getUV(3),"uv[3]");

        Assertions.assertEquals( -210, Math.round(vertexData.vertices.get(0).x),"v.x[0]");
        Assertions.assertEquals(-208, Math.round(vertexData.vertices.get(1).x),"v.x[1]");
        Assertions.assertEquals( -183, Math.round(vertexData.vertices.get(2).x),"v.x[2]");
        Assertions.assertEquals( -181, Math.round(vertexData.vertices.get(3).x),"v.x[3]");

        assertEquals(0, SceneryContext.getInstance().warnings.size(), "warnings");
    }*/

    /**
     * @throws IOException
     */
    /*3.4.24 @Test
    public void testB55B477Small() throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/B55-B477-small.osm.xml", "B55-B477-small", "superdetailed");

        HighwayModule roadModule = new HighwayModule();
        SceneryObjectList roads = roadModule.applyTo(SceneryTestUtil.mapData);
        //Way 363500734 galt mal fälschlicherweise als Problem
        HighwayModule.Highway b477_363500734 = (HighwayModule.Highway) roads.findObjectByOsmId(363500734);
        Assertions.assertNotNull( b477_363500734,"K41");

        Assertions.assertEquals( -61.657, b477_363500734.startCoord.x);
        Assertions.assertEquals( -15.173, b477_363500734.startCoord.z);

        if (SceneryBuilder.FTR_SMARTGRID) {
            SceneryTestUtil.gridCellBounds.rearrangeForWayCut(roads.objects, null);
        }
        TerrainMesh tm = TerrainMesh.init(SceneryTestUtil.gridCellBounds);

        b477_363500734.createPolygon(null, null, tm);
        b477_363500734.triangulateAndTexturize(tm);
        VertexData vertexData = b477_363500734.getVertexData();
        Dumper.dumpVertexData(logger, vertexData);
        assertEquals(4, vertexData.vertices.size(), "vertices");

        TestUtils.assertVector2( new Vector2(0, 0.875), vertexData.getUV(0),"uv[0]");
        TestUtils.assertVector2(new Vector2(0, 0.8125), vertexData.getUV(1),"uv[1]");
        TestUtils.assertVector2( new Vector2(0.7757179836359042, 0.875), vertexData.getUV(2),"uv[2]");
        TestUtils.assertVector2( new Vector2(0.7757179836359042, 0.8125), vertexData.getUV(3),"uv[3]");
    }*/

    /*3.4.24 @Test
    public void testB55B477() throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/B55-B477.osm.xml", "B55-B477", "superdetailed");

        HighwayModule roadModule = new HighwayModule();
        SceneryObjectList roads = roadModule.applyTo(SceneryTestUtil.mapData);

        //der Kreisverkehr. Der Split ist ganz am Ende
        HighwayModule.Highway circle26927466 = (HighwayModule.Highway) roads.findObjectByOsmId(26927466);
        assertEquals(295055704, circle26927466.mapWay.getStartNode().getOsmId(), "roadP38809414.mapway.startnode");
        assertEquals(255665018, circle26927466.mapWay.getEndNode().getOsmId(), "roadP38809414.mapway.endnode");
        SceneryWayConnector c = (SceneryWayConnector) circle26927466.getEndConnector();
        SceneryWayObject splittedWay = (c.getWay(0) == circle26927466) ? c.getWay(1) : c.getWay(0);
        assertEquals(255665018, splittedWay.mapWay.getStartNode().getOsmId(), "rsplittedWay.startnode");
        assertEquals(295055704, splittedWay.mapWay.getEndNode().getOsmId(), "rsplittedWay.endnode");


    }*/

    /*3.4.24 @Test
    public void testZieverichSued() throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/Zieverich-Sued.osm.xml", "Zieverich-Sued", "superdetailed");

        HighwayModule roadModule = new HighwayModule();
        SceneryObjectList roads = roadModule.applyTo(SceneryTestUtil.mapData);

        //P-Shaped way 38809414 wurde an 275038674 (index 4 des 38809414) gesplittet.
        HighwayModule.Highway roadP38809414 = (HighwayModule.Highway) roads.findObjectByOsmId(38809414);
        SceneryWayConnector c275038674 = (SceneryWayConnector) roads.findObjectByOsmId(275038674);
        assertEquals(275038674, roadP38809414.mapWay.getEndNode().getOsmId(), "roadP38809414.mapway.endnode");
        SceneryWayObject splittedWay = (c275038674.getWay(0) == roadP38809414) ? c275038674.getWay(1) : c275038674.getWay(0);
        assertEquals(275038672, splittedWay.mapWay.getEndNode().getOsmId(), "rsplittedWay.endnode");

        SceneryWayConnector c1379039502 = (SceneryWayConnector) roads.findObjectByOsmId(1379039502);
        assertEquals(3, c1379039502.getWaysCount(), "c1379039502.ways");


    }*/

    /**
     * Der 221158694 ist durch shift/clip schon mal kaputt gegangen.
     *
     * @throws IOException
     */
    /*3.4.24 @Test
    public void testEDDKSmall() throws IOException {

        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/EDDK-Small.osm.xml", "EDDK-Small", "superdetailed");

        HighwayModule roadModule = new HighwayModule();
        SceneryObjectList roads = roadModule.applyTo(SceneryTestUtil.mapData);
        roadModule.classify(SceneryTestUtil.mapData);

        if (SceneryBuilder.FTR_SMARTGRID) {
            SceneryTestUtil.gridCellBounds.rearrangeForWayCut(roads.objects, null);
        }
        TerrainMesh tm = TerrainMesh.init(SceneryTestUtil.gridCellBounds);

        SceneryWayConnector crossing388796251 = (SceneryWayConnector) roads.findObjectByOsmId(388796251);
        //12.9.19: Es gibt keine eigentstaendiges type crossing mehr
        assertEquals(true/*SceneryWayConnector.WayConnectorType.CROSSING* /, crossing388796251.isCrossing, "crossing388796251.type==CROSSING");

        //Way 221158694 war mal ein Problem
        HighwayModule.Highway w221158694 = (HighwayModule.Highway) roads.findObjectByOsmId(221158694);
        w221158694.createPolygon(null, null, tm);
        Polygon p = w221158694.getArea()[0].getPolygon(tm);
        assertTrue(p.isValid(), "polygon.valid");
        w221158694.clip(tm);

        p = w221158694.getArea()[0].getPolygon(tm);
        assertTrue(p.isValid(), "polygon.valid");
    }*/

    /**
     * Test not existing in traditional
     */
    @Test
    public void testDesdorfK41SegmentGrid2DE() throws Exception {
        Configuration customconfig = new BaseConfiguration();
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        customconfig.setProperty("modules.HighwayModule.tagfilter", "highway=secondary");
        dotestDesdorfK41SegmentGrid(customconfig, true, "poc");
    }

    private void dotestDesdorfK41SegmentGrid(Configuration customconfig, boolean elevated, String configsuffix) throws Exception {
        String xml = loadFileFromClasspath("K41-segment.osm.xml");
        OsmXmlParser parser = new OsmXmlParser(xml);
        OSMData osmData = parser.getData();

        GridCellBounds gridCellBounds = GridCellBounds.buildFromOsmData(osmData);
        TerrainMesh.meshFactoryInstance = new PersistedMeshFactory(gridCellBounds.getProjection().getBaseProjection(), terrainMeshManager);

        OSMToSceneryDataConverter converter = new OSMToSceneryDataConverter(gridCellBounds.getProjection(), gridCellBounds);
        MapData mapData = converter.createMapData(osmData);

        SceneryBuilder.FTR_SMARTGRID = true;
        SceneryBuilder.FTR_SMARTBG = true;

        SceneryContext sceneryContext = new SceneryContext();

        MapWay k41 = mapData.findMapWays(24927839).get(0);
        TerrainMesh tm = TerrainMesh.init(gridCellBounds);
        TestUtils.addTerrainMeshBoundary(tm, gridCellBounds.getOrigin().getLatDeg().getDegree(), gridCellBounds.getOrigin().getLonDeg().getDegree(),
                gridCellBounds.degwidth, gridCellBounds.degheight, gridCellBounds.getProjection().getBaseProjection(), 0.01);

        //Processor processor = sb.execute(desdorfk41, configsuffix, "Desdorf", false, customconfig, MATERIAL_FLIGHT).processor;
        HighwayModule roadModule = new HighwayModule();
        List<SceneryObject> sceneryObjects = osmElementService.process(k41, List.of(roadModule), tm, sceneryContext);

        TestUtils.writeTmpSvg(tm.toSvg());

        // 5 echte und eine durch gridnode. 13.8.19 jetzt nur innerhalb
        //3.4.24 assertEquals(2 + 1, SceneryContext.getInstance().getGraph(SceneryObject.Category.ROAD).getNodeCount(), "edges");

        //SceneryMesh sceneryMesh = processor.getResults().sceneryresults.sceneryMesh;
        // only K41 und einen Background.
        assertEquals(1, sceneryContext.highways.size(), "sceneryContext.highways");
        assertEquals(1, sceneryObjects.size(), "scenery.areas");

        assertEquals(4 + 1 + 2 * 4 + 2, tm.lines.size(), "TerrainMesh.lines");


        /*3.4.24 if (SceneryBuilder.FTR_SMARTBG) {
            assertEquals(1, sceneryMesh.getBackground().bgfillersize(), "scenery.backgrounds");
            assertEquals(0, sceneryMesh.getBackground().background.size(), "scenery.backgrounds");
        } else {
            assertEquals(0, sceneryMesh.getBackground().bgfillersize(), "scenery.backgrounds");
            assertEquals( /*13.8.19 ??? 1* /2, sceneryMesh.getBackground().background.size(), "scenery.backgrounds");
        }

        assertEquals(6, SceneryContext.getInstance().wayMap.getMapForCategory(ROAD).size(), "scenery.waymap.size");

        SceneryFlatObject road = (SceneryFlatObject) sceneryMesh.sceneryObjects.objects.get(0);
        // durch den cut wohl nur noch 8. 12.4.19 8->7
        assertEquals(7/*8/*11* /, road.getArea()[0].getPolygon(sceneryMesh.terrainMesh).getCoordinates().length, "road.coordinates");
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
        assertEquals(6/*12* /, roadgeo.getVertices().size(), "road.vertices");
        assertEquals(6/*12* /, roadgeo.getUvs().size(), "road.uvs");
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
        assertTrue(sceneryMesh.terrainMesh.isValid(true), "TerrainMesh.valid");*/

    }

    /**
     * Test not existing in traditional
     */
    @Test
    public void testTestData2024() throws Exception {

        TestData testData = TestData.build2024(terrainMeshManager);

        GridCellBounds gridCellBounds = testData.terrainMesh.getGridCellBounds();
        TerrainMesh.meshFactoryInstance = new PersistedMeshFactory(gridCellBounds.getProjection().getBaseProjection(), terrainMeshManager);

        TerrainMesh tm = TerrainMesh.init(gridCellBounds);
        TestUtils.addTerrainMeshBoundary(tm, gridCellBounds.getOrigin().getLatDeg().getDegree(), gridCellBounds.getOrigin().getLonDeg().getDegree(),
                gridCellBounds.degwidth, gridCellBounds.degheight, gridCellBounds.getProjection().getBaseProjection(), 0.01);

        OSMToSceneryDataConverter converter = new OSMToSceneryDataConverter(gridCellBounds.getProjection(), gridCellBounds);
        MapData mapData = converter.createMapData(testData.osmData);

        MapWay uWay = mapData.getMapWays().get(0);

        SceneryContext sceneryContext = new SceneryContext();

        HighwayModule roadModule = new HighwayModule();
        List<SceneryObject> sceneryObjects = osmElementService.process(uWay, List.of(roadModule), tm, sceneryContext);

        TestUtils.writeTmpSvg(tm.toSvg());

        // only way and Background.
        assertEquals(1, sceneryContext.highways.size(), "sceneryContext.highways");
        assertEquals(1, sceneryObjects.size(), "scenery.areas");

        assertEquals(4 + 2 * 3 + 2, tm.lines.size(), "TerrainMesh.lines");
    }
}
