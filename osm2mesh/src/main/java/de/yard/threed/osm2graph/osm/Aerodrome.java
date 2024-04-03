package de.yard.threed.osm2graph.osm;


import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2scenery.SceneryConversionFacade;
import de.yard.threed.osm2world.*;


import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 18.4.19: Was ist denn mittlerweile der Zweck dieser Klasse? Brauche ich Serviceways eines Airport?
 * Das kann man in OSM doch eh nicht zuverlässig ermitteln. Alles irgendwie fraglich?
 *
 * Created on 22.05.18.
 */
public class Aerodrome {
    public WaySet serviceways = null, servicewayseddk;
    // im kleinen ist das Areodrome nicht komplett drin.
    public static String EDDKFILE = "/Users/thomas/osmdata/EDDK-Complete-Large.osm.xml";


    public Aerodrome(WaySet serviceways, WaySet servicewayseddk) {
        this.serviceways = serviceways;
        this.servicewayseddk = servicewayseddk;
    }

    public static Aerodrome serviceWays() throws IOException {

        WaySet servicewayseddk = null;
        WaySet serviceways = null;
        // im kleinen ist das Areodrome nicht komplett drin.
        //das grosse pbf dauert doch zu lange
        //new File("/Users/thomass/Downloads/koeln-regbez-latest.osm.pbf"));         
        File inputfile = new File(EDDKFILE);
        OSMDataReader dataReader = new OSMFileReader(inputfile);
        OSMData osmData = dataReader.getData();
        SceneryConversionFacade cf = new SceneryConversionFacade(osmData);
        cf.createRepresentations(null,null);
        //O//SM2World osm2World = OSM2World.buildInstance(inputfile, null);


        // OSMData osmData = osm2World.getData();
        //Configuration conf = Osm2Graph.loadConfig();
        //ConversionFacade cf = osm2World.getConversionFacade();
        //Das ueber die Represntations zu machen, ist wohl/vielleicht oversized.
        //ConversionFacade.Results results = cf.createRepresentations();

        // RoadModule roadModule = (RoadModule) cf.getModule(RoadModule.class);
        // WaterModule riverModule = (WaterModule)cf.getModule(WaterModule.class);

        WaySet allways = new WaySet(cf.getMapData().getMapWaySegments());

        serviceways = allways.extractWaysByTag(new Tag("highway", "service"));
        System.out.println("" + serviceways.size() + " service ways found total");
        List<MapArea> eddk = SceneryBuilder.findByTag(cf.getMapData().getMapAreas(), new Tag("aeroway", "aerodrome"));
        System.out.println("" + eddk.size() + " aerodromes found");
        servicewayseddk = serviceways.inArea(eddk.get(0));
        System.out.println("" + servicewayseddk.size() + " service ways in EDDK found");

        return new Aerodrome(serviceways, servicewayseddk);
    }
}
