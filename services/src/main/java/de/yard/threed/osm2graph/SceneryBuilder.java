package de.yard.threed.osm2graph;

import de.yard.threed.graph.GraphExporter;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.javanative.ConfigurationHelper;
import de.yard.threed.osm2graph.osm.Aerodrome;
import de.yard.threed.osm2graph.osm.GeoJson;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.PortableModelTarget;
import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.MapArea;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.O2WMapProjection;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.Tag;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.tools.GltfBuilder;
import de.yard.threed.tools.GltfBuilderResult;
import de.yard.threed.tools.GltfProcessor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.yard.threed.javanative.ConfigurationHelper.loadSingleConfig;


/**
 * Zum Einstieg aus dem EDDK Complete die Service Roads raussuchen. Das Resultat ist in FG 3D Vektoren.
 * <p>
 * Jetzt eher ganz allgemein wird das eine Serviceschnittstelle zu OSM mit Resultat Graph oder Image der sonstiges.
 * <p>
 * Verwendbar auch aus Servlet.
 * Created on 05.05.18.
 */
public class SceneryBuilder {

    static Logger logger = Logger.getLogger(SceneryBuilder.class.getName());
    public static final String osmdatadir = "/Users/thomas/osmdata";
    public static final int BUILDMODE_2D = 0;// 0 = 2D projected without elevation. Relief durch z.B. Bruecken gibt es aber schon. Ansonsten ist Elevation aber 0.
    public static final int BUILDMODE_2DE = 1;   // 1 = 2D projected with elevation
    public static final int BUILDMODE_3D = 2;// 2 = 3D 

    public static boolean FTR_LAZYCUT = true;
    public static boolean FTR_SMARTBG = false;
    public static boolean FTR_TRACKEDBPCOORS = false;
    public static boolean FTR_UNIQUECOOR = false;
    //aka TerrainMesh
    //public static boolean FTR_POLYGON20 = false;
    //23.7.19public static boolean FTR_OVERLAPS = true;
    public static boolean FTR_BUILDINGASOVERLAY = true;
    public static boolean FTR_OVERLAPCAUSESSUPPLEMENT = false;
    public static boolean FTR_DYNAMICGRID = false;
    //28.8.19 jetzt immer true
    public static boolean FTR_SMARTGRID = true;
    //mit PreCut gibt es StackOverFlow bei Triangulation des BG)
    public static boolean FTR_PRECUT = false;

    public static boolean OverlapResolverDebugLog = false;
    public static boolean TerrainMeshDebugLog = false;
    public static boolean WayToAreaFillerDebugLog = false;

    public SceneryBuilder() {
        //Initial Defaultwerte
        FTR_LAZYCUT = true;
        FTR_SMARTBG = false;
        FTR_TRACKEDBPCOORS = false;
        FTR_UNIQUECOOR = false;
        //FTR_POLYGON20 = false;
        //23.7.19FTR_OVERLAPS = true;
        FTR_BUILDINGASOVERLAY = true;
        FTR_OVERLAPCAUSESSUPPLEMENT = false;
        FTR_DYNAMICGRID = false;
        FTR_SMARTGRID = true;
    }

    public static void main(String[] args) {

        try {
            //Vector3 und logging braucht Platform.
            //PlatformHomeBrew.init(new HashMap<String, String>());
            SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));

            //firstgjson();
            //13.6.18 erstmal so auf die schnelle
            //buildSamples();
            Options options = new Options();
            //TODO geht pbf?)
            //3.11.21: input now is complete file name (relative,absolute,with suffix)
            options.addOption("i", "input", true, "OSM input file (relative or absolute,with suffix eg. osm.xml or osm.pbf)");
            options.addOption("g", "grid", true, "grid file");
            options.addOption("p", "projected", false, "create projected 2D output. Otherwise 3D world coordinates will be created.");
            options.addOption("P", "projected with elevation", false, "create projected 2D output in z0 layer with elevation.");

            options.addOption("c", "config", true, "configuration file suffix, eg. poc");
            options.addOption("m", "material", true, "material configuration file suffix, eg. flight");
            options.addOption("o", "output", true, "output directory (must have tiles and graph sub dirs)");
            options.addOption("d", "destination", true, "destination file name without suffix and path");

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            String inputfile = cmd.getOptionValue("i");
            String lodconfigsuffix = cmd.getOptionValue("c");
            String materialconfigsuffix = cmd.getOptionValue("m");
            String outdir = cmd.getOptionValue("o");
            String gridfile = cmd.getOptionValue("g");
            String destinationfilename = cmd.getOptionValue("d");
            boolean projected = cmd.hasOption("p");
            boolean projectedwithelevation = cmd.hasOption("P");
            Configuration customconfig = new BaseConfiguration();
            int buildmode = BUILDMODE_3D;
            if (projectedwithelevation) {
                buildmode = BUILDMODE_2DE;
                customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
            } else if (projected) {
                // No elevationProvider, so no elevation will be calculated (z will be 0).
                buildmode = BUILDMODE_2D;
            }
            boolean emptyterrain = false;
            //7.8.18: ISt doch doof, dafuer hab ich doch die Config.
            //SceneryBuilder.configureForBuildMode(customconfig,buildmode);


            SceneryBuilder sb = new SceneryBuilder();

            ProcessResults processResults = sb.execute(inputfile/*osmdatadir + "/" + inputfile + ".osm.xml"*/, lodconfigsuffix, gridfile, /*outdir, inputfile,*/ false, customconfig,materialconfigsuffix);

            writeOutput(processResults, outdir, destinationfilename);


        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private static void writeOutput(ProcessResults processResults, String outputdir, String destinationfilename) throws IOException {
        logger.debug("Writing output to " + outputdir + " with name " + destinationfilename);
        if (outputdir != null) {
            new GltfProcessor().writeGltfOutput(outputdir + "/tiles", destinationfilename, processResults.gltfstring.gltfstring, processResults.gltfstring.bin);
            // 2.8.18: z im graph nicht negieren. Why?
            if (processResults.roadGraph != null) {
                File f = new File(outputdir + "/graph/" + destinationfilename + "-road.xml");
                PrintStream pw = new PrintStream(f);
                pw.println(GraphExporter.exportToXML(processResults.roadGraph, false, processResults.gridCellBounds.tripnodes));
                pw.close();
            }
            if (processResults.railwayGraph != null) {
                File f = new File(outputdir + "/graph/" + destinationfilename + "-railway.xml");
                PrintStream pw = new PrintStream(f);
                pw.println(GraphExporter.exportToXML(processResults.railwayGraph, false, processResults.gridCellBounds.tripnodes));
                pw.close();
            }
        } else {
            // eigentlich nur fuer Tests
            //doof wegen viel output System.out.println(gltfstring);
        }
    }



    /**
     * 17.7.19:Nicht mehr static, um Features selektiv setzen zu koennen.
     * 3.11.21: No longer returns Processor but ProcessResults for better decoupling.
     *
     * @return
     * @throws IOException
     */
    public /*static*/ ProcessResults execute(String inputfile, String lodconfigsuffix, String gridname, /*String outputdir,String destinationfilename ,*/ boolean useosm2world,
                                             Configuration customconfig, String materialconfigsuffix) throws IOException {
        GridCellBounds gridCellBounds = null;
        if (gridname != null) {
            // Bei Fehler wird null geliefert und ohne weitergemacht
            gridCellBounds = GridCellBounds.buildGrid(gridname, null);
        }

        //Config.reinit(loadConfig(configsuffix));
        // 10.7.18: empty terrain und gridcell widersprechen sich aber doch.
        //9.4.19 weiss ghar nicht mehr was das soll Config.getCurrentConfiguration().setProperty("createTerrain", new Boolean(emptyterrain));
        Processor processor = new Processor(new File(inputfile));
        processor.reinitConfig(materialconfigsuffix, lodconfigsuffix, customconfig);
        processor.process(gridCellBounds);
        //ConversionFacade.Results results = processor.results;

        ProcessResults processResults = new ProcessResults();

        processResults.roadGraph = SceneryContext.getInstance().getGraph(SceneryObject.Category.ROAD);//sgraphModule.graph;
        processResults.railwayGraph = SceneryContext.getInstance().getGraph(SceneryObject.Category.RAILWAY);//sgraphModule.graph;


        PortableModelTarget pmt = new PortableModelTarget();

        SceneryMesh sceneryMesh = processor.getResults().sceneryresults.sceneryMesh;
        // render to GLTF?
        sceneryMesh.render(pmt);

        processor.pml = pmt.pml;
        processor.pmt = pmt;

        GltfBuilder gltfBuilder = new GltfBuilder();
        GltfBuilderResult gltfstring = gltfBuilder.process(pmt.pml);

        logger.info("Building for " + inputfile + " took xxx");
        processResults.processor = processor;
        processResults.results = processor.getResults();
        processResults.gltfstring = gltfstring;
        processResults.gridCellBounds = gridCellBounds;
        return processResults;
    }

    public static List<MapArea> findByTag(Collection<MapArea> mapAreas, Tag tag) {
        List<MapArea> l = new ArrayList<MapArea>();
        for (MapArea a : mapAreas) {
            if (a.getTags().contains(tag)) {
                l.add(a);
            }
        }
        return l;
    }

    static void firstgjson() throws IOException {
        Aerodrome aerodrome = Aerodrome.serviceWays();
        GeoJson.exportWaySet(aerodrome.serviceways);
    }

    public static Configuration loadMaterialConfig(String configfilesuffix) {
        return loadConfig("material-"+configfilesuffix);
    }

    public static Configuration loadConfig(String configfilesuffix) {

        Configuration baseconfig = loadConfig();
        if (configfilesuffix == null) {
            return baseconfig;
        }
        return ConfigurationHelper.addConfiguration(ConfigurationHelper.loadSingleConfigFromClasspath("config/configuration-" + configfilesuffix + ".properties"),
                baseconfig);
    }

    public static Configuration loadConfig() {
        String configfilename = "config/configuration-base.properties";
        return ConfigurationHelper.loadSingleConfigFromClasspath(configfilename);
    }

    public static Configuration loadExtensionConfig(long osmId) {
        String filename = SceneryBuilder.osmdatadir + "/extensions/configuration-" + osmId + ".properties";
        File file = new File(filename);
        if (file.exists() && file.canRead()) {
            try {
                Configuration configuration = loadSingleConfig(new FileInputStream(file));
                return configuration;
            } catch (FileNotFoundException e) {
                //silently ignore
                return null;
            }
        }
        return null;
    }


    /**
     * Das ist doch doof. Dafuer habe ich doch die Configs.
     */
    /*public static void configureForBuildMode(Configuration customconfig, int buildmode) {
        //Der Default Provider, der OOTB immer 68 liefert.
        //Das ist doch doof. 
        customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        switch (buildmode) {
            case BUILDMODE_2D:
                customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider");
                break;
            /*not required case BUILDMODE_2DE:
                customconfig.setProperty("ElevationProvider", "zero");
                break;* /
        }

    }*/
    public static Map<OSMNode, MapNode> buildNodeMap(OSMData osmData, List<MapNode> mapNodes, O2WMapProjection mapProjection) {
        final Map<OSMNode, MapNode> nodeMap = new HashMap<OSMNode, MapNode>();

        for (OSMNode node : osmData.getNodes()) {
            VectorXZ nodePos = mapProjection.calcPos(node.lat, node.lon);
            MapNode mapNode = new MapNode(nodePos, node, null);
            if (mapNodes != null) {
                mapNodes.add(mapNode);
            }
            nodeMap.put(node, mapNode);
        }
        return nodeMap;
    }

    public static Bound getBounds(OSMData osmData) {
        //16.4.19: Bounds no more allowed?
        if (osmData.getBounds().size() != 1) {
            throw new RuntimeException("inv para");
        }
        Bound osmbound = osmData.getBounds().iterator().next();

        if (osmData.getBounds() != null && !osmData.getBounds().isEmpty()) {

            Bound firstBound = osmData.getBounds().iterator().next();

            return osmbound;

        } else {
            if (osmData.getNodes().isEmpty()) {
                throw new IllegalArgumentException(
                        "OSM data must contain bounds or nodes");
            }

            OSMNode firstNode = osmData.getNodes().iterator().next();
            return null;

        }
    }
}
