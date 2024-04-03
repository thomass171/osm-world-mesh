package de.yard.threed.osm2graph.osm;


import de.yard.threed.osm2scenery.SceneryConversionFacade;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMDataReader;
import de.yard.threed.osm2world.OSMFileReader;
import de.yard.threed.osm2world.Tag;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created on 22.05.18.
 */
public class MainGrid {
    static Logger logger = Logger.getLogger(MainGrid.class.getName());
    public WaySet roads, rivers;
    public Connector connector;

    MainGrid(WaySet roads, WaySet rivers) {
        this.roads = roads;
        this.rivers = rivers;
        connector = new Connector();
    }

    /**
     * Das OSM Netz muss aber schon vorgefiltert hier rein kommen, oder? Noch nicht ganz klar.
     * 9.4.19: Abgelöst von gelöschtne teilen von Modul Osm2World
     *
     * @return
     * @throws IOException
     */
    public static MainGrid build() throws IOException {
        if (true) {
            //9.4.19: Das mit dem MainGrid muss nochmal durchdacht. Dafuer doch nicht die ganze MAschinerie anwerfen.
            return null;
        }

        File inputfile = new File("/Users/thomas/osmdata/maingrid.osm.xml");
        //OSM2World osm2World = OSM2World.buildInstance(inputfile, null);
        OSMDataReader dataReader = new OSMFileReader(inputfile);
        OSMData osmData = dataReader.getData();
        SceneryConversionFacade cf = new SceneryConversionFacade(osmData);

        //Das ueber die Represntations zu machen, ist wohl/vielleicht oversized.
        //Die Representations brauche ich hier doch nicht?

        Config.getInstance().enableModules(new String[]{"RoadModule", "WaterModule"});
        SceneryConversionFacade.Results results = cf.createRepresentations(null, null);


        HighwayModule roadModule = (HighwayModule) cf.getModule("RoadModule");
        // WaterModule riverModule = (WaterModule)cf.getModule("WaterModule");

        //OSMData osmData = osm2World.getData();
        WaySet allways = new WaySet(cf.getMapData().getMapWaySegments());
        WaySet motorways = allways.extractWaysByTag(new Tag("highway", "motorway"));
        System.out.println("" + motorways.size() + " motorways found total");
        WaySet rivers = allways.extractWaysByTag(new Tag("waterway", "river"));
        System.out.println("" + rivers.size() + " rivers found total");
        return new MainGrid(motorways, rivers);

    }

    /**
     * erstmal als einfaches Dreieck.
     */
    /*16.5.19 gibts doch Datei für public static GridCellBounds buildDesdorf() {
        List<Long> ids = new ArrayList<>();
        List<LatLon> coords = new ArrayList<>();
        //Suedost
        ids.add(1829065191L);
        coords.add(OsmUtil.toLatLon(new SGGeod(new Degree(6.5936041f), new Degree(50.9479214f), 0)));
        //Nord
        ids.add(1829058473L);
        coords.add(OsmUtil.toLatLon(new SGGeod(new Degree(6.5913940f), new Degree(50.9502612f), 0)));
        //West
        ids.add(2377084113L);
        coords.add(OsmUtil.toLatLon(new SGGeod(new Degree(6.5874427f), new Degree(50.9475747f), 0)));
        GridCellBounds simpleTargetBounds = new GridCellBounds(coords);

        return simpleTargetBounds;
    }*/
    public static GridCellBounds buildA4Poll() {
        //A$ Poll 370524/370513, ha falsch, das sind ja Endpunkte, ich brauch Zwischenpunkte,
        //besser 370512/1664879703, die sind aber nicht direkt auf gleicher Höhe.
        return null;
    }


}
