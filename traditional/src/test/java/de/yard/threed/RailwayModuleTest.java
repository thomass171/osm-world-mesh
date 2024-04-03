package de.yard.threed;

import de.yard.threed.core.Vector2;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.core.testutil.TestUtils;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.modules.RailwayModule;
import de.yard.threed.osm2scenery.polygon20.MeshFactory;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.Dumper;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * 11.4.19: Einfachere gezieltere Tests als in OsmGridTest (ohne Processor und ConversionFacade)
 */
public class RailwayModuleTest {
    //EngineHelper platform = PlatformHomeBrew.init(new HashMap<String, String>());
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(RailwayModuleTest.class);

    @BeforeAll
    public static void setup(){
        TerrainMesh.meshFactoryInstance = new TraditionalMeshFactory();
    }
    /**
     * Skizze 44
     *
     * @throws IOException
     */
    @Test
    public void testHambachbahn() throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/B55-B477.osm.xml", "B55-B477", "superdetailed");

        RailwayModule railwayModule = new RailwayModule();
        SceneryObjectList railways = railwayModule.applyTo(SceneryTestUtil.mapData);
        // 2 ways weil zwei Gleise
        assertEquals(2, railways.size(), "railways");
        RailwayModule.Rail hambachbahn = (RailwayModule.Rail) railways.findObjectByOsmId(241235604);
        //24 Nodes in OSM + 2 Grid Dummy Nodes
        assertEquals(24 + 1 + 1, hambachbahn.mapWay.getMapNodes().size(), "mapway.nodes");
        //assertNotNull("K41", k41vonunten);

        if (SceneryBuilder.FTR_SMARTGRID) {
            SceneryTestUtil.gridCellBounds.rearrangeForWayCut(railways.objects, null);
        }
        TerrainMesh tm = TerrainMesh.init(SceneryTestUtil.gridCellBounds);

        hambachbahn.buildEleGroups();
        hambachbahn.createPolygon(null, null, tm, SceneryContext.getInstance());

        hambachbahn.cut(SceneryTestUtil.gridCellBounds);

        Dumper.dumpPolygon(logger, hambachbahn.getArea()[0].getPolygon(tm).getCoordinates());

        assertNull(hambachbahn.getVertexData(), "vertexdata");
        hambachbahn.triangulateAndTexturize(tm);

        VertexData vertexData = hambachbahn.getVertexData();
        Dumper.dumpVertexData(logger, vertexData);
        assertEquals(12, vertexData.vertices.size(), "vertices");

        //Egal wo der Way beginnt, (0,1) soll am ersten Vertex (links/oben) sein
        TestUtils.assertVector2(new Vector2(0, 0.125), vertexData.getUV(0),"uv[0]");
        TestUtils.assertVector2( new Vector2(0, 0), vertexData.getUV(1),"uv[1]");
        //die weiteren x Werte sind von der config der Texturlänge abhängig, damit unguenstig pruefbar. TODO aber machbar!
        TestUtils.assertVector2( new Vector2(4.153010609423403, 0.125), vertexData.getUV(2),"uv[2]");
        TestUtils.assertVector2( new Vector2(4.153010609423403, 0), vertexData.getUV(3),"uv[3]");

    }

}
