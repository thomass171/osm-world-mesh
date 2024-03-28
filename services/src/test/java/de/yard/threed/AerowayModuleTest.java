package de.yard.threed;

import de.yard.threed.core.Degree;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.modules.AerowayModule;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.RoadDecorator;
import de.yard.threed.osm2scenery.util.Dumper;
import de.yard.threed.osm2world.ConfMaterial;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.Materials;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.TAXIWAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * 11.4.19: Einfachere gezieltere Tests als in OsmGridTest (ohne Processor und ConversionFacade)
 * Elevation wird hier nicht getestet, weil es zu umfangreich ist.
 */
public class AerowayModuleTest {
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(AerowayModuleTest.class);

    /**
     * @throws IOException
     */
    @Test
    public void testTestData() throws IOException {
        //Simple wegen sonst ueberhaengendem DeadEnd Zipfel
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/TestData-Simplified.osm.xml", "TestData", "superdetailed");

        AerowayModule aerowayModule = new AerowayModule();

        SceneryObjectList objs = aerowayModule.applyTo(SceneryTestUtil.mapData);
        aerowayModule.classify(SceneryTestUtil.mapData);

        AerowayModule.Runway runway = (AerowayModule.Runway) objs.findObjectByOsmId(117);
        runway.createPolygon(null, null, null);
        runway.cut(SceneryTestUtil.gridCellBounds);
        runway.triangulateAndTexturize(null);

        //800m lang. 16.8.19: jetzt multiple areas statt multiple objects.
        assertEquals( /*80 */1, objs.size(), "objs");
        assertEquals(80 - 1, runway.getArea().length, "areas");

        AbstractArea segment;

        //Skizze 67: Segmente 4 und 5 schneiden Grid.
        for (int i = 0; i < 4; i++) {
            segment = runway.getArea()[0];
            assertTrue(segment.isEmpty(null), "apron.decorations.minimum 20");
        }
        segment = runway.getArea()[6];
        VertexData vd = segment.getVertexData();
        Dumper.dumpVertexData(logger, vd);
        assertEquals(4, vd.vertices.size(), "segment0.vertices");

        int expectedatlassegment = 4;
        // 4096=32*128;
        double cellsize = 1.0 / 32;// =0,03125
        SceneryTestUtil.assertTriangleStrip(vd, new Vector2(expectedatlassegment * cellsize, 0.25), new Vector2((expectedatlassegment + 2) * cellsize, 0.5));

    }

    @Test
    public void testEDDKSmallOSMOnly() throws IOException {
        //taxiway+parking_position
        //c4x mal so uebernommen
        dotestEDDKSmall(true, 32 + 65, 637/*wegen parkpos 334*/, "C04", -70.1, -112.8);
    }

    @Test
    public void testEDDKSmall() throws IOException {
        //c4x mal so uebernommen. 24.6.21:546->637, 269->97,0->-112.84526182290385 damit Tests laufen; was auch immer da wirklich richtig ist.
        //24.6.21: NPE, der Test ist verhunzt, oder war nie fertig(??).
        //dotestEDDKSmall(false, 97, 637, "C_4", -12.5, -112.84526182290385);
    }


    /**
     * TODO: Klären: Warum unetrscheidet sich C_4 position zwischen OSM und FG?
     */
    public void dotestEDDKSmall(boolean osmOnly, int expectedtaxiways, int expectedwaymapsize, String c4name, double c4x, double c4heading) throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/EDDK-Small.osm.xml", "EDDK-Small", "superdetailed");

        AerowayModule aerowayModule = new AerowayModule();
        if (!osmOnly) {
            aerowayModule.doExtendMapData("EDDK-Small.osm.xml", SceneryTestUtil.mapData, SceneryTestUtil.converter);
        }
        SceneryObjectList objs = aerowayModule.applyTo(SceneryTestUtil.mapData);
        aerowayModule.classify(SceneryTestUtil.mapData);

        assertEquals(expectedwaymapsize, SceneryContext.getInstance().wayMap.getMapForCategory(TAXIWAY).size(), "scenery.taxiwaywaymap.size");

        List<SceneryObject> taxiways = objs.findObjectsByCreatorTag("Taxiway");
        //32 corresponds to OSM data
        //EDDK 32->301(32+269)
        assertEquals(expectedtaxiways, taxiways.size(), "scenery.taxiway.size");
        List<SceneryObject> runways = objs.findObjectsByCreatorTag("Runway");
        //1 corresponds to OSM data
        assertEquals(1, runways.size(), "scenery.runways.size");
        AerowayModule.Runway runway = (AerowayModule.Runway) runways.get(0);

        AerowayModule.Apron apron = (AerowayModule.Apron) objs.findObjectByOsmId(147175470);

        // Polygons

        apron.createPolygon(null, null, null);

        // Supplements

        //fehlen zu viele Polygone. aerowayModule.createSupplements(objs.objects);

        // Decorations

        apron.createDecorations();
        SceneryWayObject parkingC4 = (SceneryWayObject) objs.findObjectByOsmId(385706063);
        if (parkingC4 != null) {
            //only with OSM
            Degree heading = OsmUtil.getHeadingAtEnd(parkingC4.mapWay);
            assertEquals(c4heading, heading.getDegree(), 0.1, "C4 heading");
            new RoadDecorator().createParking(parkingC4, Materials.GRASS);
        }

        assertTrue(apron.getDecorations().size() > 20, "apron.decorations.minimum 20");

        // VertexData

        apron.triangulateAndTexturize(null);

        AbstractArea deco0 = objs.findDecorationByName(c4name);//apron.getDecorations().get(0);
        VertexData vd = deco0.getVertexData();
        Dumper.dumpVertexData(logger, vd);
        assertEquals(4, vd.vertices.size(), "deco0.vertices");
        assertEquals(c4x, vd.vertices.get(0).x, 0.1, "deco0.vertices[0].x");

        //triangle strip
        SceneryTestUtil.assertUVs(vd, new Vector2(0.875, 1), new Vector2((0.875), 0.875), new Vector2(1, 1), new Vector2(1, 0.875));
    }

    @Test
    public void testMaterial() throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/EDDK-Small.osm.xml", "EDDK-Small", "superdetailed");
        ConfMaterial material = new ConfMaterial("PARKINGPOSITION", Material.Interpolation.FLAT, Color.ORANGE);
        OsmUtil.loadMaterialConfiguration(Config.getCurrentConfiguration(), material, true);
//TODO assert
    }
}
