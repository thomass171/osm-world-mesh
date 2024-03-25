package de.yard.threed.osm2scenery.modules;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.engine.XmlDocument;
import de.yard.threed.graph.GraphEdge;
import de.yard.threed.graph.GraphNode;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.MapDataHelper;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2graph.osm.TextureUtil;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupFinder;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.scenery.CustomData;
import de.yard.threed.osm2scenery.scenery.SceneryAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryObjectFactory;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2scenery.scenery.components.DefaultTerrainMeshAdder;
import de.yard.threed.osm2scenery.scenery.components.RoadDecorator;
import de.yard.threed.osm2scenery.scenery.components.WayArea;
import de.yard.threed.osm2scenery.util.TagFilter;
import de.yard.threed.osm2scenery.util.TagHelper;
import de.yard.threed.osm2scenery.util.TagMap;
import de.yard.threed.osm2world.ConfMaterial;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.MapArea;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.Materials;
import de.yard.threed.osm2world.NamedTexCoordFunction;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.OSMWay;
import de.yard.threed.osm2world.TagGroup;
import de.yard.threed.osm2world.TextureData;
import de.yard.threed.osm2world.ValueStringParser;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.traffic.NodeCoord;
import de.yard.threed.traffic.geodesy.GeoCoordinate;
import de.yard.threed.trafficfg.flight.GroundNet;
import de.yard.threed.trafficfg.flight.Parking;
import de.yard.threed.trafficfg.flight.TaxiwayNode;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * Das ist das Module fuer alles, was in OSM den key "aeroway" hat.
 * Z.B. Aerodrome, apron, Runway
 * <p>
 * Taxiways kommen aus OSM oder FG, Parkpos aus...
 * OSM hat "gate" und aeroway=parking_position; aber obs die immer gibt?
 * Bei EDDK fehlen z.B. parkpos. 16.7.19 Wirklich? Die sind doch da.
 * Hier entsteht ein Groundnet Graph für den Airport. Darum
 * ist ein Airport immer ein Tile, wie FG.
 * road graph entsteht hier nicht, den macht HighwayModule.
 * created 20.4.19
 */
public class AerowayModule extends SceneryModule {
    static Logger logger = Logger.getLogger(AerowayModule.class);
    boolean foundrunway = false;
    //soll eigentlich 0.3 sein, 1.8 hat zum Tests aber bessere Erkennbarkeit.
    public static double GROUNDNETMARKERWIDTH = 1.8f;
    GroundNet groundNet = null;
    //Taxiways taxiways = null;
    /*20.4.19 public AerowayModule(RenderData renderdata) {
        super();
        this.renderdata=renderdata;
    }*/
    //Die gemergte Flaeche aller Taxiways.
    Geometry taxiwayarea = null;
    SceneryObjectList aerowayobjects = new SceneryObjectList();

    @Override
    public void extendMapData(String osmDatasetName, MapData mapData, OSMToSceneryDataConverter converter) {
        //11.6.19: Weil in EDDK das OSM ganz gut ist, kein FG mehr. Evtl. mal per Config.
        //doExtendMapData(osmDatasetName,mapData,converter);
    }

    /**
     * extracted for testing
     */
    public void doExtendMapData(String osmDatasetName, MapData mapData, OSMToSceneryDataConverter converter) {
        if (osmDatasetName.contains("EDDK")) {
            loadGroundnet(converter.getProjection());
        }
        if (groundNet != null) {
            GroundNetToMapDataConverter.convert(groundNet, mapData, converter);
        }
    }

    @Override
    public SceneryObjectList applyTo(MapData mapData) {
        if (groundNet != null) {

        }
        Apron apron = null;
        //logger.debug("apply " + grid);

        //noch nicht AerowayModule.TaxiwayArea ta = new AerowayModule.TaxiwayArea();
        //aerowayobjects.add(ta);

        //taxiways = new Taxiways();
        TagFilter tagfilter = getTagFilter("tagfilter");
        TagMap materialmap = getTagMap("materialmap");
        for (MapWay mapway : mapData.getMapWays()) {
            if (mapway.getOsmId() == 8610418) {
                int osmid = 6;
            }
            if (isRunway(mapway.getTags()) && tagfilter.isAccepted(mapway.getTags())) {
                long osmid = mapway.getOsmId();
                logger.debug("found runway ");
                foundrunway = true;

                Runway runway = new Runway(mapway, materialmap/*, mapway.getTags(), mapway.getOsmId()*/);
                //SceneryContext.getInstance().highways.put(osmid, road);
                //Polygone werden gleich schon berbaucht zum Ausscvhneiden. 21.5.19:Gibt es jetzt erstmal nicht.
                //mal weglassen runway.createPolygon();
                //SceneryArea area = new SceneryArea("Road", p, ASPHALT, mapway.getOsmId());
                aerowayobjects.add(runway/*area*/);
                /*for (SceneryFlatObject a : runway.segments) {
                    aerowayobjects.add(a);
                }*/
                //runway.addToWayMap(SceneryObject.Category.RUNWAY);
            } else {
                //don't mix OSM taxiways with dedicated groundnet
                if ((groundNet != null && mapway.getOsmId() < 0) || groundNet == null && mapway.getOsmId() > 0) {
                    if (isTaxiway(mapway.getTags()) || isParkingTaxiway(mapway.getTags())) {
                        addTaxiway(mapway, materialmap);
                    }
                }
            }
        }

        for (MapArea area : mapData.getMapAreas()) {
            // taxiways und runways sind offenbar keine areas, was ja auch Sinn macht. Darum hier nicht behandeln
            // Apron sind als Way ("aeroway" halt) codiert, sollten aber als MapArea gemappt werden.
            if (isApron(area.getTags())) {
                apron = new Apron(area, aerowayobjects);
                aerowayobjects.add(apron);
            }
        }


        return aerowayobjects;
    }

    @Override
    public List<ScenerySupplementAreaObject> createSupplements(List<SceneryObject> objects) {
        List<ScenerySupplementAreaObject> supplements = new ArrayList<>();
        //Die Taxiwayarea wird unabhaengig vom OSM Apron gehandhabt. Obwohl die sicher overlappen.
        createAreaWithoutTaxiwaysAndRunways(supplements);

        return supplements;
    }

    /**
     * 20.5.19: Es scheint günstiger, das ganze Areodrome auf ASPHALT zu setzen, sonst hat man nachher zu viele grüne Flecken, die unpassend wirken.
     * <p>
     * Aber nur, wenn es einen Aerodrome gibt.
     *
     * @return
     */
    /*Quatsch public Material getBackgroundMaterial(){
        if (foundrunway) {
            return ASPHALT;
        }
        return null;
    }*/
    private static boolean isRunway(TagGroup tags) {
        if (tags.contains("aeroway", "runway")
                /*&& !tags.contains("highway", "construction")
                && !tags.contains("highway", "proposed")*/) {
            return true;
        } /*else {
            return tags.contains("railway", "platform")
                    || tags.contains("leisure", "track");
        }*/
        return false;
    }

    private static boolean isTaxiway(TagGroup tags) {
        if (tags.contains("aeroway", "taxiway")) {
            return true;
        }
        return false;
    }

    private static boolean isParkingTaxiway(TagGroup tags) {
        if (tags.contains("aeroway", "parking_position")) {
            return true;
        }
        return false;
    }

    private static boolean isApron(TagGroup tags) {
        //Apron auch erstmal."aerodrome" ist das ganze Airportgelände und damit zu groß.
        if (tags.contains("aeroway", "apron")) {
            return true;
        }
        return false;
    }

    private void loadGroundnet(SceneryProjection projection) {
        //TODO config??
        String src = "/Users/thomas/Projekte/Granada/data/resources/flusi/EDDK.groundnet.xml";
        try {
            String groundnetdefinition = FileUtils.readFileToString(new File(src));
            XmlDocument groundnetxml = XmlDocument.buildXmlDocument(groundnetdefinition);
            groundNet = new GroundNet(null, null, groundnetxml, null);
        } catch (Exception e) {
            logger.error("loading groundnet failed");
        }
    }

    /**
     * Die Menge der Taxyways ist ein Sonderfall, weil es nicht nur die Ways für einen Graph, sondern durch ihre Ausdehnung auch eine Fläche ergeben, die
     * zum Teil definiertes Apron ist, zum Teil Apron aber auch vergrößert.
     */
    private void addTaxiway(MapWay mapway, TagMap materialmap) {
        //logger.debug("found taxiway");
        TaxiWayCustomData taxiWayCustomData = new TaxiWayCustomData(mapway);
        SceneryWayObject taxiway = SceneryObjectFactory.createTaxiway(mapway, materialmap, taxiWayCustomData);
        //aerowayobjects.add(taxiway/*area*/);
        taxiway.addToWayMap(SceneryObject.Category.TAXIWAY);
        aerowayobjects.add(taxiway/*area*/);

        //Polygone werden gleich schon berbaucht zum Ausscvhneiden.17.7.19:nicht mehr
        //taxiway.createPolygon(null);

        // Fuer die Taxiways noch breite Grundflächen bauen, die alle dann zu einer Apronerweiterung gemerged werden.
        // So ähnlich scheint das auch die OSM Map zu machen.
        //die Breite eines Taxiway ist nicht herleitbar? 50 ist zumindest in EEDK zu breit; optisch und schneidet in Grünflächen.
        double width = 30;
        AbstractArea ta = WayArea.buildOutlinePolygonFromCenterLine(taxiway.getGraphComponent().getCenterLine(), taxiway.effectiveNodes/*mapway.getMapNodes()*/, width, this, Materials.ASPHALT);
        if (ta != null) {
            //26.8.19: Das ist hier wohl nicht mehr ganz koscher.
            if (taxiwayarea == null) {
                if (!ta.isEmpty()) {
                    taxiwayarea = ta.poly.uncutPolygon;
                }
            } else {
                if (!ta.isEmpty()) {
                    taxiwayarea = taxiwayarea.union(ta.poly.uncutPolygon);
                }
            }
        } else {
            logger.error("taxiway area segment failed");
        }

    }

    /**
     * Die Taxiways und Runways selber aus der taxiwayarea ausschneiden, denn die haben ja eine eigene Darstellung.
     * Dann bleibt als taxiwayarea nur noch das "Drumherum", das dann eine "ganz normale" Area wird.
     * <p>
     * TODO Überschneidung mit Apron?
     *
     * @param supplements
     */
    private void createAreaWithoutTaxiwaysAndRunways(List<ScenerySupplementAreaObject> supplements) {
        if (taxiwayarea != null) {
            //
            for (SceneryObject so : aerowayobjects.objects) {
                Geometry geo = null;
                if (so instanceof SceneryWayObject) {
                    geo = ((SceneryFlatObject) so).getArea()[0].poly.uncutPolygon;
                }
                if (geo != null) {
                    //difference might fail.
                    Geometry diff = JtsUtil.difference(taxiwayarea, geo);
                    if (diff != null) {
                        taxiwayarea = diff;
                    }
                }
            }

            //Was übrigbleibt wird eine Area
            if (taxiwayarea instanceof MultiPolygon) {
                MultiPolygon mp = (MultiPolygon) taxiwayarea;
                for (int i = 0; i < mp.getNumGeometries(); i++) {
                    TaxiwayArea ta = new TaxiwayArea((Polygon) mp.getGeometryN(i));
                    supplements.add(ta);
                }
            } else {
                TaxiwayArea ta = new TaxiwayArea((Polygon) taxiwayarea);
                supplements.add(ta);
            }
        }
    }

    /**
     * * ist einer Road schon aehnlich. Naja.
     * 20.4.19: Ob wirklich als Way, ist noch unklar. Besser nicht.
     * Die Texturierung per TriangleStrip wird nicht gehen, weil unterschiedliche
     * Atlas Segments genutzt werden.
     * Wegen der Texturieurng besteht die Runway aus vielen Areas.
     * Am einfachsten wären pro Textursegment ein Sceneryobject.
     * Runway selber ist ein leeres Objekt.
     * 16.8.19: Warum nicht ein AreaObject mit n areas, wie ein gesplittetes Farmland?
     * Und wie bei einem Way erfolgt der LazyCut an einer DummyNode am Grid? Quatsch, nicht noch eine
     * Sonderlocke. Es werden ganz standardkonform die Areas ausserhalb entfernt bzw cut.
     */
    public static class Runway extends SceneryFlatObject {
        final private TagGroup tags;
        private final MapWay mapWay;
        public String[] naming = null;
        //fatal zu verwahren wegen cut public AbstractArea[] segments;
        VectorXZ startCoord, endCoord;

        public Runway(MapWay mapWay, TagMap materialmap) {

            //super(line);
            super("Runway", null, Category.RUNWAY, null);
            osmIds.add(mapWay.getOsmId());
            logger = Logger.getLogger(Runway.class.getName());

            this.tags = mapWay.getTags();//tags;
            String ref = tags.getValue("ref");
            if (ref != null) {
                naming = ref.split("/");
            }

            startCoord = mapWay.getStartNode().getPos();
            endCoord = mapWay.getEndNode().getPos();
            this.mapWay = mapWay;
            terrainMeshAdder=new DefaultTerrainMeshAdder(this);
        }

        private void adjustMaterialForSegment(ConfMaterial material, int i) {
            // 4096=32*128;
            double cellsize = 1.0 / 32;// =0,03125
            TextureData textureData = material.getTextureDataList().get(0);
            switch (i) {
                case 0:
                    //pa_threshold
                    calcFromToForCell(textureData, 10, 8, 2, 8, cellsize);
                    break;
                case 1:
                    //'L'
                    calcFromToForCell(textureData, 10, 24, 1, 8, cellsize);
                    break;
                case 2:
                    //'22' weils einfach ist ohne centerline vertex
                    calcFromToForCell(textureData, 2, 16, 1, 8, cellsize);
                    break;
                default:
                    //rest, sieht aus wie centerline
                    calcFromToForCell(textureData, 4, 8, 2, 8, cellsize);
                    break;
            }
        }

        /**
         * y von unten
         */
        private void calcFromToForCell(TextureData textureData, int x, int y, int w, int h, double cellsize) {
            textureData.from = new VectorXZ(x * cellsize, y * cellsize);
            textureData.to = new VectorXZ((x + w) * cellsize, (y + h) * cellsize);
        }


        /*@Override
        public void triangulateAndTexturize() {
            //nothing to do
        }

        @Override
        public void calculateElevations() {
            //nothing to do
        }*/

        @Override
        protected void registerCoordinatesToElegroups() {
            if (flatComponent != null) {

                for (AbstractArea area : flatComponent) {
                    area.registerCoordinatesToElegroups(elevations);
                }

            }
        }

        @Override
        public void buildEleGroups() {
            EleConnectorGroupSet areaElevation = new EleConnectorGroupSet();
            areaElevation.eleconnectorgroups = new ArrayList<>();
            //nur eine group fuer startnode ist natuerlich problematisch
            areaElevation.add(new EleConnectorGroup(mapWay.getStartNode()));
            elevations = areaElevation;
        }

        /*@Override
        public RenderedObject render(SceneryRenderer sceneryRenderer) {
            //nothing to do
            return null;
        }*/

        /**
         * Erstmal alle Segmente anlegen. Beim cut werden die ausserhalb liegenden entfernt.
         *
         * @param objects
         * @param gridbounds
         * @return
         */
        @Override
        public List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds) {
            double width = 60;
            String s = tags.getValue("width");
            if (s != null) {
                width = ValueStringParser.parseMeasure(s);
            }
            double len = endCoord.distanceTo(startCoord);

            //Anzahl 10m Segmente
            int segment10cnt = (int) (len / 10);
            VectorXZ dir = endCoord.subtract(startCoord).normalize();
            VectorXZ from = startCoord, to;
            AbstractArea[] segments = new AbstractArea/*SceneryFlatObject*/[segment10cnt];
            //kein SceneryWayObject, weil der keine centerline Vertices hat.
            //Durch den Atlas geht kein TextureRepeat, darum viele Segmente. Doof.
            for (int i = 0; i < segment10cnt; i++) {
                //Orange um zu erkenn, das was nicht stimmt
                ConfMaterial material = new ConfMaterial("RUNWAY", Material.Interpolation.FLAT, Color.ORANGE);
                OsmUtil.loadMaterialConfiguration(Config.getCurrentConfiguration(), material, true);
                adjustMaterialForSegment(material, i);
                double seglen = 10;
                switch (i) {
                    case 0:
                        break;
                }
                to = from.add(dir.mult(seglen));
                //Ueberall mal die startnode verwenden. Ob das gut ist? Erstmal besser als nichts.
                //segments[i] = new SceneryAreaObject/*SceneryFlatObject*/(mapWay.getStartNode(), "RunwaySegment", null, Category.RUNWAY, new RunwayArea(from, to, width, material));
                Polygon polygon = MapDataHelper.getOutlinePolygon(from, to, width);

                segments[i] = new RunwayArea(polygon, width, material);
                from = to;
            }
            flatComponent = segments;
            return null;
        }

        @Override
        public void cut(GridCellBounds gridbounds) {
            if (getOsmIdsAsString().contains("87822834")) {
                int h = 9;
            }
            if (isCut) {
                return;
            }
            super.cut(gridbounds);
        }

        /*28.8.19 nur noch superklasse @Override
        public void addToTerrainMesh() {
            super.addToTerrainMesh();
            TerrainMesh tm = TerrainMesh.getInstance();
            for (int i = 0; i < flatComponent.length; i++) {
                AbstractArea aa = flatComponent[i];
                if (!aa.isEmpty()) {
                    List<AreaSeam> areaSeams = new ArrayList<>();


                    Map<SceneryAreaObject, AreaSeam> adjacentareas;
                    Area.addAreaToTerrainMesh((Area) aa, this, null);

                } else {

                }
            }

        }*/

    }

    public static class TaxiWayCustomData implements CustomData {
        final private TagGroup tags;
        public String naming = null;

        TaxiWayCustomData(MapWay/*Segment*/ line) {
            this.tags = line.getTags();//tags;
            // Taxiways also know a "ref" tag.
            String ref = tags.getValue("ref");
            if (ref != null) {
                naming = ref;
            }

        }
    }


    public static class Apron extends SceneryAreaObject {
        SceneryObjectList aerowayobjects;

        /**
         * Hier die Liste reinstecken ist irgendwie doof.
         *
         * @param area
         * @param aerowayobjects
         */
        public Apron(MapArea area, SceneryObjectList aerowayobjects) {
            super(area, "Apron", Materials.ASPHALT, Category.APRON);
            this.aerowayobjects = aerowayobjects;
        }

        @Override
        public void createDecorations() {
            // Parking parkposC4 = groundNet.getParkPos("C_4");

            //nur mal so ne Test Decoration
            ConfMaterial material = new ConfMaterial("PARKINGPOSITION", Material.Interpolation.FLAT, Color.ORANGE);
            OsmUtil.loadMaterialConfiguration(Config.getCurrentConfiguration(), material, true);

            for (SceneryObject so : aerowayobjects.objects) {
                if (so instanceof SceneryWayObject) {
                    SceneryWayObject way = (SceneryWayObject) so;
                    //13.8.19 TODO centerline < 1 sollte es gar nicht geben
                    if (isParkingTaxiway(way.mapWay.getTags()) && way.getCenterLine().size() > 1) {
                        RoadDecorator roadDecorator = new RoadDecorator();
                        AbstractArea marking = roadDecorator.createParking(way, material);
                        addDecoration(marking);
                    }
                }
            }
        }
    }

    public static class TaxiwayArea extends ScenerySupplementAreaObject {
        TaxiwayArea() {
            super("TaxiwayArea", Materials.ASPHALT, null);
        }

        TaxiwayArea(Polygon taxiwayarea) {
            super("TaxiwayArea", taxiwayarea, Materials.ASPHALT);
        }

        @Override
        public void buildEleGroups() {
            //?? mal eine??
        }

        /**
         * TODO in super bzw. nicht erforderlich?
         */
        @Override
        public List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds) {
            return null;
        }
    }
}

/**
 * Einen cut der Runway will ich eigentlich durch das Grid vermeiden. Wenn doch, erfolgt hier normal der cut ueber Area.
 */
class RunwayArea extends Area {
    static Logger logger = Logger.getLogger(RunwayArea.class);

    public VectorXZ startCoord, endCoord;

    RunwayArea(Polygon polygon, double width, Material material) {
        super(polygon, material);

    }

    /*@Override
    public CutResult cut(Geometry gridbounds, SceneryFlatObject abstractSceneryFlatObject, EleConnectorGroupSet elevations) {
        //
        super.cut()
    }*/

    /**
     * Fuer leere wird das gar nicht aufgerufen.
     *
     * @param eleConnectorGroupFinder
     * @return
     */
    @Override
    public boolean triangulateAndTexturize(EleConnectorGroupFinder eleConnectorGroupFinder) {
        if (getPolygon().getCoordinates().length == 5) {
            //Standardsegment
            vertexData = JtsUtil.createTriangleStripForPolygon(getPolygon().getCoordinates(),null,null);
            if (vertexData == null) {
                //already logged.
                poly.trifailed = true;
                return false;
            }
            vertexData.uvs = TextureUtil.texturizeVertices(vertexData.vertices, material, NamedTexCoordFunction.STRIP_FIT);
            //texturizer = "way Texturizer";
            return true;
        }
        // duerften die cut Segment bleiben. Ueber die Superklasse wird falsch sein TODO und das mit coordfunction
        material.getTextureDataList().get(0).coordFunction = NamedTexCoordFunction.GLOBAL_X_Y;
        return super.triangulateAndTexturize(eleConnectorGroupFinder);
    }
}


class Taxiways {
    Logger logger = Logger.getLogger(Taxiways.class);


}

class GroundNetToMapDataConverter {
    static Logger logger = Logger.getLogger(GroundNetToMapDataConverter.class);

    public static void convert(GroundNet groundNet, MapData mapData, OSMToSceneryDataConverter converter) {
        for (int i = 0; i < groundNet.groundnetgraph.getBaseGraph().getEdgeCount(); i++) {
            //TODO werden das nicht zu viele Nodes?
            GraphEdge graphEdge = groundNet.groundnetgraph.getBaseGraph().getEdge(i);
            GraphNode from = graphEdge.getFrom();
            GraphNode to = graphEdge.getTo();
            List<OSMNode> nodes = new ArrayList<>(2);
            nodes.add(buildOSMNode(from));
            nodes.add(buildOSMNode(to));
            Map<String, String> tags = new HashMap();
            tags.put("aeroway", "taxiway");

            //https://wiki.openstreetmap.org/wiki/Tag:aeroway%3Dparking_position
            if (from.customdata instanceof Parking) {
                Parking parking = (Parking) from.customdata;
                //overwrites taxiway
                tags.put("aeroway", "parking_position");
                tags.put("ref", parking.name);
            }
            if (to.customdata instanceof Parking) {
                Parking parking = (Parking) to.customdata;
                //overwrites taxiway
                tags.put("aeroway", "parking_position");
                tags.put("ref", parking.name);
                //flip from/to
                nodes.add(nodes.remove(0));
            }
            OSMWay way = OsmUtil.buildDummyWay(TagHelper.buildTagGroup(tags), nodes);
            converter.addWayToMapData(way, mapData);

        }
    }

    private static OSMNode buildOSMNode(GraphNode graphNode) {
        GeoCoordinate coor = null;
        if (graphNode.customdata instanceof Parking) {
            Parking parking = (Parking) graphNode.customdata;
            coor = parking.coor;

        } else if (graphNode.customdata instanceof NodeCoord) {
            NodeCoord nodeCoord = (NodeCoord) graphNode.customdata;
            coor = nodeCoord.coor;
        } else if (graphNode.customdata instanceof TaxiwayNode) {
            TaxiwayNode taxiwayNode = (TaxiwayNode) graphNode.customdata;
            coor = taxiwayNode.coor;
        }
        if (coor == null) {
            logger.error("unknown groundnet node " + graphNode.customdata);
            return null;
        }
        OSMNode osmNode = OsmUtil.buildDummyNode(coor.getLatDeg().getDegree(), coor.getLonDeg().getDegree());
        return osmNode;
    }
}