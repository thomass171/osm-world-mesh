package de.yard.threed;


import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.osm.Aerodrome;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.PortableModelTarget;
import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.osm2graph.osm.WaySet;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapWaySegment;
import de.yard.threed.osm2world.WaySegmentWorldObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static de.yard.threed.osm2graph.SceneryBuilder.loadConfig;
import static de.yard.threed.osm2graph.SceneryBuilder.loadMaterialConfig;
import static de.yard.threed.osm2world.Config.MATERIAL_FLIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Einfache Tests der OSM Daten. Ohne Processor und SceneryBuilding.
 * 18.4.19: Hat das überhaupt seine Berechtigung?
 * <p>
 * Created on 17.05.18.
 */
public class OsmTest {
    //EngineHelper platform = PlatformHomeBrew.init(new HashMap<String, String>());
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));

    //9.4.19 Das mit dem Maingrid/serviceways  muss redesigned werden @Test
    public void testEDDK() {
        try {
            Aerodrome aerodrome = Aerodrome.serviceWays();

            WaySet serviceways = aerodrome.serviceways;
            System.out.println("" + serviceways.size() + " service ways found");
            //sind evtl. mehr geworden durch Verwendung von WaySegments
            //26.7.18 4360 durch neues EDDK osm file?
            assertEquals(4360/*6673/*1759*/, serviceways.size(), "serviceways.total");
            assertEquals(2166/*1916/*355*/, aerodrome.servicewayseddk.size(), "serviceways.areas");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Einfache Tests mit K41 xml, obwohl da die Bounds ja gar nicht drin sind.
     *
     * @throws IOException
     */
    @Test
    public void testK41() throws IOException {
        Config.reinit(Processor.defaultconfigfile, loadMaterialConfig(MATERIAL_FLIGHT), loadConfig("poc"), null);
        //9.4.19: Ohne Grid ist doch völlig witzlos
        GridCellBounds gridCellBounds = null;
        gridCellBounds = GridCellBounds.buildGrid("Desdorf", null);

        String inputfile = "/Users/thomas/osmdata/K41-segment.osm.xml";
        Processor processor = new Processor(new File(inputfile));
        processor.process(gridCellBounds);
        //ConversionFacade.Results results =processor.results;
        MapData mapData = processor.getMapData();
        Collection<MapWaySegment> mws = mapData.getMapWaySegments();
        assertEquals( /*4 wegen grid? */5, mws.size(), "MapWaySegments");
        MapWaySegment mws0 = (MapWaySegment) mws.iterator().next();
        List<WaySegmentWorldObject> reps = mws0.getRepresentations();
        // die Representation ist eine Road (die Klasse) mit 3 Lanes (LEFT, DASHED_LINE, RIGHT)
        //9.4.19 assertEquals("Representations", 1, reps.size());
        //9.4.19 WaySegmentWorldObject wswo0 = reps.get(0);

        PortableModelTarget pmt = new PortableModelTarget();
        SceneryMesh sceneryMesh = processor.getResults().sceneryresults.sceneryMesh;
        sceneryMesh.render(pmt);

    }


}
