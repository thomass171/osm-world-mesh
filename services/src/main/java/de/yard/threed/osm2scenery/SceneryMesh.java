package de.yard.threed.osm2scenery;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.IntHolder;
import de.yard.threed.core.Vector2;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.TextureUtil;
import de.yard.threed.osm2scenery.elevation.ElevationCalculator;
import de.yard.threed.osm2scenery.elevation.SimpleEleConnectorGroupFinder;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.polygon20.MeshFillCandidate;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.Background;
import de.yard.threed.osm2scenery.scenery.BackgroundElement;
import de.yard.threed.osm2scenery.scenery.OverlapResolver;
import de.yard.threed.osm2scenery.scenery.SceneryAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryObjectFactory;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2scenery.scenery.components.WayArea;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import de.yard.threed.osm2world.Materials;
import de.yard.threed.osm2world.OsmOrigin;
import de.yard.threed.traffic.geodesy.ElevationProvider;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 11.07.18.
 */
public class SceneryMesh {
    Logger logger = Logger.getLogger(SceneryMesh.class);

    //16.8.18: jetzt drei Listen. Aaach. ich weiss nicht
    //public List<SceneryAreaObject/*Area*/> sceneryAreaObjects = new ArrayList<>();
    //public List<SceneryWayObject/*Area*/> sceneryWayObjects = new ArrayList<>();
    //public List<SceneryObject/*Area*/> sceneryObjects = new ArrayList<>();
    public SceneryObjectList sceneryObjects = new SceneryObjectList();
    // public List<SceneryWayObject/*Area*/> sceneryWayObjects = new ArrayList<>();

    // Der background kann ja in viele Teile zerschnitten werden.
    // Einen Background gibt es nur, wenn es ein begrenzendes Grid gibt.
    private Background background = null;
    /*Polygon*/ GridCellBounds gridbounds;


    SceneryMesh() {
        //mal so'ne kleine Sandbox
        //areas.add(new SceneryArea(buildSample1()));
        //areas.get(0).merge(new SceneryArea(buildSample2()));
        //areas.add(new SceneryArea(buildSample2()));
        //areas.add(new SceneryArea(buildSample3()));

        Polygon s1 = buildSample1();
        // bei dem diff entstehen zwei Polygone.
        MultiPolygon diff = (MultiPolygon) s1.difference(buildRectangle3());
        //areas.add(new SceneryArea((Polygon) diff.getGeometryN(0)));
        //areas.add(new SceneryArea((Polygon) diff.getGeometryN(1)));

        //TextureUtil.triangulate(s1.getCoordinates(),s1);

        // der ist so mal bei Roads entstanden. Da ist beim Union ein Hole entstanden.
        Polygon pwithhole = (Polygon) JtsUtil.buildFromWKT("POLYGON ((49.972 -30.167, 48.12887487632708 -30.94345977261266, 33.0748317683746 4.7912136322257135, 4.337197425440958 72.12000304319542, -2.5515357935065426 83.0079020176959, -10.202367976668196 91.59259706000324, -18.205262458259348 96.86708474811385, -25.842766510414897 100.82346745286253, -36.90287126131779 102.82009355148364, -48.44508871945548 102.51587517039499, -60.64442668805475 98.63798905508973, -70.6691338832814 92.40816251415829, -80.79630565079619 82.74213577725818, -102.16725314155842 57.927328210975055, -122.63925314155841 37.555328210975055, -124.05 38.973, -126.5524324136154 36.454998475629424, -197.39043241361543 106.85499847562943, -244.37481412914067 161.48355916999287, -299.6215566716621 235.60677045644246, -349.5172850237618 318.64818274283436, -343.43071497623816 322.30381725716563, -293.92844332833783 239.84922954355756, -238.9911858708593 166.11244083000713, -192.38556758638458 111.89100152437058, -122.95220813131472 42.88695700537062, -104.98874685844159 60.76267178902494, -83.82769434920381 85.35186422274181, -73.4328661167186 95.2998374858417, -62.767573311945256 102.02801094491026, -49.67491128054453 106.322124829605, -37.03512873868221 106.81790644851637, -25.147233489585105 104.76253254713748, -16.400737541740654 100.43691525188615, -8.005632023331803 94.93540293999675, 0.4195357935065422 85.68609798230409, 7.712802574559043 74.26599695680457, 36.7531682316254 6.362786367774286, 36.85573141867386 6.119326958783388, 47.64598836010592 -4.53076035524815, 54.23171262031565 -8.290748911436406, 61.40009189356124 -10.3847754762109, 69.21522075283465 -10.66638244394461, 81.35358664560277 -8.12000890535213, 99.12185434326392 -5.551051403348532, 105.14656455638077 -3.0060537061844967, 186.05731589635795 37.165662900741765, 252.73170820737343 75.18597626991966, 315.21140722610056 114.67790578183288, 371.35100310301465 156.2424135291909, 375.5749968969854 150.5355864708091, 319.00459277389945 108.67609421816714, 256.2482917926266 69.01802373008034, 189.21468410364204 30.80633709925823, 108.20668410364203 -9.41366290074177, 106.628 -6.234, 108.00941785496035 -9.504196381173825, 51.35341785496034 -33.43719638117383, 49.972 -30.167), (86.39393922007629 -10.92766098826892, 81.85441335439722 -11.583991094647871, 69.93477924716535 -14.09161755605539, 61.29590810643876 -13.883224523789101, 53.27428737968435 -11.657251088563594, 45.930011639894076 -7.58123964475185, 40.334019827420974 -2.1372616346789215, 50.43614616349587 -26.11718833264797, 86.39393922007629 -10.92766098826892))");
        //areas.add(new SceneryArea(pwithhole));
        //logger.debug("pwithhole.interiorNum=" + pwithhole.getNumInteriorRing());


    }

    /**
     * Aus background werden zusaetzliche Areas rausgeschnitten und gridbounds dient dazu die zusaetzlichen zu cutten.
     *
     * @param bounds
     */
    public void setBackgroundMesh(GridCellBounds bounds) {
        //this.bounds = bounds;
        background = new Background(bounds.getPolygon());
        //background.add((bounds.getPolygon()));
        gridbounds = bounds;//(bounds.getPolygon());
    }


    /**
     * 16.8.18: ohne cut weil Polygonen noch nicht da sind.
     *
     * @param area
     */
    public void add(SceneryFlatObject area) {
        //empty kgeht nicht weil kein Polygon?
        //if (!area.isEmpty()) {
        //sceneryObjects.add(area);
        //if (area instanceof SceneryWayObject){
        //  sceneryWayObjects.add((SceneryWayObject) area);
        //}
        //}
    }

    /**
     * Das ist kein Bestandteil des Terrain und muss auch nicht in den Background eingeschnitten werden.
     *
     */
    /*3.6.19 public void add(SceneryVolumeOverlayObject vvo) {
        sceneryObjects.add(vvo);
    }*/

    /**
     * Fuer die Objects Elevation echt ermitteln. Der Background kann sich sein dann aus der
     * ElevationMap suchen.
     */
    public void calculateElevations(ElevationProvider elevationProvider) {
        //Zuerst die Elevation der EleGroups setzen
        ElevationCalculator.fixElevationGroups(sceneryObjects, elevationProvider);
        //13.8.19: Die Berechnung im TerrainMesh ersetzt eigentlich die im Objekt, aber nicht das Anheben fuer Overlays z.B., auch nicht die VertexData.
        TerrainMesh tm = TerrainMesh.getInstance();
        tm.calculateElevations(elevationProvider);

        ElevationCalculator.calculateElevations(sceneryObjects, elevationProvider);
        if (background != null) {
            if (!SceneryBuilder.FTR_SMARTBG) {
                ElevationCalculator.registerBackgroundElevations(background.background, sceneryObjects);
            }
            ElevationCalculator.calculateBackgroundElevations(background);
        }
    }

    /**
     * Dabei werden auch die Vertices final festgelegt, damit die Zuordnung passt.
     * Es entstehen neue Coordinates durch die Triangulation.
     * 14.8.19: Darum lieber erst die regulaeren Areas, dann der BG. Dann kann der BG von den neuen profitieren. War mal andersrum.
     */
    public void triangulateAndTexturize() {
        for (SceneryObject area : sceneryObjects.objects) {
            area.triangulateAndTexturize();
        }
        if (background != null) {
            //backgroundvd = new ArrayList<>();
            for (BackgroundElement be : background.background) {
                be.vertexData = TextureUtil.triangulateAndTexturizePolygon(be.polygon, Materials.TERRAIN_DEFAULT);
                if (be.vertexData == null) {
                    logger.error("Triangulation failed for background area. valid= " + be.polygon.isValid());
                    be.trifailed = true;
                }
            }
            for (Area be : background.bgfiller) {
                if (be.isEmpty()) {
                    logger.warn("empty BG filler?");
                } else {
                    be.triangulateAndTexturize(new SimpleEleConnectorGroupFinder(gridbounds.elegroups));
                    if (be.getVertexData() == null) {
                        Polygon p = be.getPolygon();
                        if (p == null) {
                            logger.error("Triangulation failed for background filler area. polygon=null ");
                        } else {
                            logger.error("Triangulation failed for background filler area. valid= " + p.isValid());
                            //TODO be.trifailed = true;
                        }
                    }
                }
            }
        }
        /*for (SceneryObject area : sceneryObjects.objects) {
            area.triangulateAndTexturize();
        }*/
    }

    /**
     * @param sceneryRenderer
     * @return
     */
    public Map<Integer, RenderedObject> render(SceneryRenderer sceneryRenderer) {
        sceneryRenderer.beginRendering();
        Map<Integer, RenderedObject> rendermap = new HashMap<>();

        // als letztes den Background oben drüber, um zu sehen ob falsch ausgeschnitten wurde.
        // 25.4.19: Na, ob das wirklich eine Hilfe ist?
        // 17.7.19:Kommt jetzt zuerst, um Overlays (z.B. Buildings) in 2D zu sehen. Und dann sieht man auch die durch clip
        // verbreiterten Ways.
        if (background != null) {
            int i = 0;
            if (background.background.size() != 0 && background.bgfiller.size() != 0) {
                throw new RuntimeException("entweder oder");
            }
            for (BackgroundElement be : background.background) {
                Polygon p = be.polygon;
                // 2.8.18: TODO hier kann es doch auch eine trifailed geben, oder?
                OsmOrigin osmOrigin = new OsmOrigin("GridCell/Background");
                osmOrigin.trifailed = be.trifailed;
                sceneryRenderer.drawArea("background", background.material, p, be.vertexData, osmOrigin, null);
                //(be.eleConnectorGroupSet != null) ? be.eleConnectorGroupSet : null);
                i++;
            }
            i = 0;
            for (Area be : background.bgfiller) {
                Polygon p = be.getPolygon();
                // 2.8.18: TODO hier kann es doch auch eine trifailed geben, oder?
                OsmOrigin osmOrigin = new OsmOrigin("Background Area");
                osmOrigin.trifailed = false;//TODO be.trifailed;
                sceneryRenderer.drawArea("bgfiller " + i, background.material, p, be.getVertexData(), osmOrigin, null);
                //(be.eleConnectorGroupSet != null) ? be.eleConnectorGroupSet : null);
                i++;
            }
        }

        //ORange fuer mit gescheiterten triangluate.10.6.19: Asbach?
        //25.4.19: Zweistufig. Die Docorations erst im zweiten Schritt, damit sie in 2D nicht verdeckt werden.
        //10.6.19:Nicht mehr nötig, weil die Teil des SO sind. die kommen auch in 2D drüber.
        //Aber Ways könnten unter Areas verschwinden (in 2D). Darum Areas zuerst.
        for (SceneryObject/*Area*/ obj : sceneryObjects.objects) {
            //SceneryAreaObject area = ((SceneryAreaObject) obj);//).getSceneryArea();
            //4.6.19  if (!obj.isDecoration) {
            if (!(obj instanceof SceneryWayObject)) {
                RenderedObject ro = obj.render(sceneryRenderer);
                if (ro != null) {
                    rendermap.put(obj.id, ro);
                }
            }
            // }
            //sceneryRenderer.drawArea(area.creatortag, area.material, area.getPolygon(), area.vertexData, new OsmOrigin(area.creatortag, 
            //        area.getOsmIds()), (area.trifailed) ? highlightcolor : null,area.getEleConnectorGroups());
        }
        for (SceneryObject obj : sceneryObjects.objects) {
            if (obj instanceof SceneryWayObject) {
                RenderedObject ro = obj.render(sceneryRenderer);
                if (ro != null) {
                    rendermap.put(obj.id, ro);
                }
            }
        }

        sceneryRenderer.endRendering();
        return rendermap;
    }

    private Polygon buildSample1() {
        return JtsUtil.buildRectangle(40, 80, 30, 15);
    }

    private Polygon buildSample2() {
        return JtsUtil.buildRectangle(-60, 80, 30, 15);
    }

    public static Polygon buildRectangle3() {
        return JtsUtil.buildRectangle(40, 80, 10, 60);
    }

    public static Polygon buildRectangle4inside3() {
        return JtsUtil.buildRectangle(40, 80, 5, 30);
    }


    public Background getBackground() {
        return background;
    }

    public static Polygon buildRectangleWithHole() {
        Polygon polygon = JtsUtil.buildRectangle(0, 40, 120, 60);
        LinearRing[] holes = new LinearRing[1];
        holes[0] = JtsUtil.GF.createLinearRing(JtsUtil.buildRectangle(0, 40, 40, 20).getCoordinates());
        polygon = JtsUtil.GF.createPolygon(JtsUtil.GF.createLinearRing(polygon.getCoordinates()), holes);
        return polygon;
    }

    public void createDecorations() {
        for (SceneryObject obj : sceneryObjects.objects) {
            if (obj instanceof SceneryFlatObject) {
                ((SceneryFlatObject) obj).createDecorations();
            }
        }
    }

    /**
     * <p>
     * Das passirt in zwei Schritten, erst die "primary" Objekte, dann die "secondary", die auf den Polygonen der
     * "primary" basieren. 23.5.19: Gibts das immer noch? 10.7.19: Irgendwie doof ist es aber. Für Connector aber auch wieder
     * erforderlich. 16.7.19: Immer noch?
     * Macht jetzt alles Polygone eines Cycle. Nicht mehr way spezifisch.
     * Supplements nach den anderen
     */
    public List<ScenerySupplementAreaObject> createPolygons(SceneryObject.Cycle cycle) {
       /* 12.7.19 for (SceneryObject obj : sceneryObjects.objects) {
            //16.8.18: Auch auf Volume, z.B. Bridge
            //TODO 12.7.19: Was ist das hier? Das ist doch die Asbach Idee, erst Flächen und dann Ways?
            if (!(obj instanceof ScenerySupplementAreaObject) && !(obj instanceof SceneryWayConnector)) {
                //AbstractSceneryFlatObject asf = (SceneryObject) obj;
                //BG surfaces haben schon polygon
                //if (asf.poly==null) {
                obj.createPolygon();
            }
        }*/
        List<ScenerySupplementAreaObject> supplements = new ArrayList<>();
        for (SceneryObject obj : sceneryObjects.objects) {
            //Connector brauchen die Ways
            if (/*obj instanceof SceneryWayObject*/obj.cycle == cycle && !(obj instanceof ScenerySupplementAreaObject)) {
                List<ScenerySupplementAreaObject> l = obj.createPolygon(Collections.unmodifiableList(sceneryObjects.objects), gridbounds);
                if (l != null) {
                    supplements.addAll(l);
                }
            }
        }
        for (SceneryObject obj : sceneryObjects.objects) {
            //Connector brauchen die Ways
            if (/*obj instanceof SceneryWayObject*/obj.cycle == cycle && (obj instanceof ScenerySupplementAreaObject)) {
                List<ScenerySupplementAreaObject> l = obj.createPolygon(Collections.unmodifiableList(sceneryObjects.objects), gridbounds);
                if (l != null) {
                    supplements.addAll(l);
                }
            }
        }
        return supplements;

    }

    /**
     * Supplements nach den anderen
     */
   /* public void createNonWaysPolygons() {
        for (SceneryObject obj : sceneryObjects.objects) {
            if (!(obj instanceof SceneryWayConnector) && !(obj instanceof SceneryWayObject) && !(obj instanceof ScenerySupplementAreaObject)) {
                obj.createPolygon();
                //26.9.18: immer, weil das auch connector sind

            }
        }

        for (SceneryObject obj : sceneryObjects.objects) {
            if (!(obj instanceof SceneryWayConnector) && !(obj instanceof SceneryWayObject) && obj instanceof ScenerySupplementAreaObject) {
                obj.createPolygon();
                //26.9.18: immer, weil das auch connector sind

            }
        }
    }*/

    /**
     * Macht jetzt alles Polygone eines Cycle. Nicht mehr way spezifisch.
     */
    public void clip(SceneryObject.Cycle cycle) {
        for (SceneryObject obj : sceneryObjects.objects) {
            if (obj.cycle == cycle/*obj instanceof SceneryWayObject*/) {
                /* ((SceneryWayObject) obj)*/
                obj.clip();
            }
        }
        /*for (SceneryObject obj : sceneryObjects.objects) {
            if (obj instanceof SceneryWayConnector) {
                ((SceneryWayConnector) obj).clip();
            }
        }*/
    }

   /* public void clipNonWays() {
        for (SceneryObject obj : sceneryObjects.objects) {
            if (!(obj instanceof SceneryWayConnector) && !(obj instanceof SceneryWayObject) && obj instanceof SceneryFlatObject) {
                ((SceneryFlatObject) obj).clip();
            }
        }
    }*/

    /**
     * Die {@link de.yard.threed.osm2scenery.elevation.EleConnectorGroup}s bestehen zwar schon,
     * aber die Polygonpunkte werden angehangen.
     * <p>
     */
    public void createElevationGroups() {
        for (SceneryObject obj : sceneryObjects.objects) {
            //16.8.18: Auch auf Volume, z.B. Bridge
            if (!(obj instanceof ScenerySupplementAreaObject)) {
                //AbstractSceneryFlatObject asf = (SceneryObject) obj;
                //BG surfaces haben schon polygon
                //if (asf.poly==null) {
                //26.9.18: immer, weil das auch connector sind
                //if (ElevationMap.hasInstance()) {
                obj.prepareElevationGroups();
                //}
            }
        }
        for (SceneryObject obj : sceneryObjects.objects) {
            if (obj instanceof ScenerySupplementAreaObject) {
                //26.9.18: immer, weil das auch connector sind
                //if (ElevationMap.hasInstance()) {
                obj.prepareElevationGroups();
                //}
            }
        }
    }

    public void connectElevationGroups() {
        for (SceneryObject obj : sceneryObjects.objects) {
            //16.8.18: Auch auf Volume, z.B. Bridge
            if (!(obj instanceof ScenerySupplementAreaObject)) {
                //AbstractSceneryFlatObject asf = (SceneryObject) obj;
                //BG surfaces haben schon polygon
                //if (asf.poly==null) {
                //26.9.18: immer, weil das auch connector sind
                //if (ElevationMap.hasInstance()) {
                obj.connectElevationGroups();
                //}
            }
        }
        for (SceneryObject obj : sceneryObjects.objects) {
            if (obj instanceof ScenerySupplementAreaObject) {
                //26.9.18: immer, weil das auch connector sind
                //if (ElevationMap.hasInstance()) {
                obj.connectElevationGroups();
                //}
            }
        }
        /*das bringts nichtz for (SceneryObject obj : sceneryObjects.objects) {
            if (!obj.isRegistered) {
                logger.error("objects coordinates not registered in object "+obj);
            }
        }*/
    }

    public void buildBridgeApproaches() {
        HighwayModule.buildBridgeApproaches(sceneryObjects.objects);
    }

    /**
     * Einfuegen/schneiden aller Scenery Objects in das BackgroundMesh, wenn das Object zum Background beitraegt (SurfaceProvider).
     * <p>
     * Hierbei werden komplett ausserhalb liegende Areas nicht mehr (19.4.19) aus der Objektliste gelöscht, weil das Analysen
     * schwieriger macht, z.B. bei Connectoren.
     * TODO: Aus dem Graphen sollten die outside Edges aber doch raus.
     */
    public void insertSceneryObjectsIntoBackgroundAndCut(SceneryObject.Cycle cycle) {
        List<SceneryObject> outside = new ArrayList<>();
        for (SceneryObject obj : sceneryObjects.objects) {
            if (obj instanceof SceneryFlatObject && obj.cycle == cycle) {
                // Als erstes aus dem Background schneiden und dann erst zuschneiden. Sonst entstehen an den Aussenkanten des Background sehr leicht
                // quasi flächenlose Spitzen.

                SceneryFlatObject asf = (SceneryFlatObject) obj;
                if (asf.creatortag.contains("WayToAreaFiller")) {
                    int h = 9;
                }
                //17.7.19:Overlays kommen nicht mehr in den BG
                if (background != null && !SceneryBuilder.FTR_SMARTBG && !obj.isOverlay()) {
                    background.insert(asf, true);
                }
                if (gridbounds != null) {
                    // area zurechtschneiden. Auch wenn das aufgrufen wird, kann das SO selber entscheiden, ob wirklich ein cut gemacht wird.
                    //30.7.19: Bei Precut wird z.B. kein Cut mehr gemacht.
                    asf.cut(gridbounds);
                }
                if (asf.isEmpty()) {
                    //19.4.19 outside.add(asf);
                }
            }
        }
        for (SceneryObject so : outside) {
            // Die Edges aus dem Graph nehmen. TODO nicht nur Roads
            SceneryContext.getInstance().getGraph(SceneryObject.Category.ROAD).removeLayer(so.id);
            sceneryObjects.objects.remove(so);
        }
    }

    public void createBackground() {
        //Asbach TerrainMesh.extractBackground();

        if (SceneryBuilder.FTR_SMARTBG) {
            if (background.background.size() > 0) {
                throw new RuntimeException("inconsistent background size " + background.background.size());
            }
            TerrainMesh tm = TerrainMesh.getInstance();
            if (!tm.isValid(true)) {
                logger.error("Terrain mesh not valid. Not building BG filler");
                tm.errorCounter++;
                return;
            }
            //19.8.19: Wenn man nicht von Boundaries ausgeht, sondern von innen, kann man auf nicht entscheidbare
            //Successorfragen stossen (z.B. am Desdorf farmland). Darum erstmal an den Boundaries beginnen.
            MeshLine meshLine = tm.findOpenLine(1);
            int cntr = 0;
            while (meshLine != null && cntr++ <= 100) {
                if (!createBGFillerFromLine(meshLine, true)) {
                    logger.error("createBackground: create BG filler from boundary failed. Aborting");
                    tm.errorCounter++;
                    return;
                }
                meshLine = tm.findOpenLine(1);
            }
            // und dann von innen
            meshLine = tm.findOpenLine(2);
            while (meshLine != null && cntr++ <= 100) {
                if (!createBGFillerFromLine(meshLine, false)) {
                    logger.error("createBackground: create BG filler from inner failed. Aborting");
                    tm.errorCounter++;
                    return;
                }
                meshLine = tm.findOpenLine(2);
            }
            if (cntr >= 100) {
                logger.error("abort counter reached");
                tm.errorCounter++;
            }
            logger.info("" + cntr + " BG polygons created");
            return;
        }
        //6.8.19 mal nicht mehr background.createFromRemaining(sceneryObjects);
    }

    /**
     * Return true on success.
     *
     * @param meshLine
     * @param fromBoundary
     * @return
     */
    private boolean createBGFillerFromLine(MeshLine meshLine, boolean fromBoundary) {
        //logger.debug("found open mesh line " + meshLine);
        TerrainMesh tm = TerrainMesh.getInstance();
        boolean left = meshLine.getLeft() == null;
        MeshPolygon meshPolygon = tm.traversePolygon(meshLine, null, left);
        if (meshPolygon == null || meshPolygon.lines.size() == 0) {
            logger.error("createBackground: MeshPolygon not found. Aborting");
            return false;
        }
        //Polygon polygon = meshPolygon.getPolygon();
        //Ein Supplement daraus zu machen passt eigentlich nicht in die Verarbeitung. Aber nur so koennen left/right im Mesh gesetzt
        //werden. 12.8.19: wuerde jetzt auch nur mit Area gehen.
        //ScenerySupplementAreaObject bgFiller = SceneryObjectFactory.createBackgroundFiller(meshPolygon);
        //sceneryObjects.add(bgFiller);
        //bgFiller.addToTerrainMesh();
        logger.debug("Creating BG filler from open (" + ((fromBoundary) ? "boundary" : "non boundary") + ") line " + meshPolygon.getPolygon());
        Area bgfiller = new Area(meshPolygon, Materials.TERRAIN_DEFAULT, true);
        bgfiller.parentInfo = "BG filler";
        for (MeshLine ml : meshPolygon.lines) {
            tm.completeLine(ml, bgfiller);
        }
        background.addFiller(bgfiller);
        //logger.debug("resolved open mesh line to" + meshPolygon);
        if (!tm.isValid(true)) {
            logger.error("not valid");
            return false;
        }
        return true;
    }

    /**
     * Die Verbindungen ziwschen Ways hinterlegen(TODO).
     * 24.8.18: Daran laesst sich dann auch gut eine {@link de.yard.threed.osm2scenery.elevation.EleConnectorGroup} festmachen.
     * 4.4.19: Das wurde doch schon gemahct mit z.B. RoadConnector. ODer. mal raus
     */
    /*public void createConnections() {

    }*/

    /**
     * Ermitteln, was unter den Bridges ist. Dazu werden die Polygone gebraucht.
     * Dann die Approaches. 31.8.18: Nee, die werden schon vor Polygonen gebraucht.
     * //13.7.19 Warum sollte eine Brücke den Untergrund kennen? Führt nur zu doofen
     * // Abhaengigkeiten
     */
    public void completeBridgeRelations() {

        /*13.7.19 for (BridgeModule.Bridge bridge : SceneryContext.getInstance().bridges.values()) {
            SmartPolygon above = bridge./*.roadorrailway* /getArea().poly;
            // unter einer Bridge koennen ja auch areas liegen, also alles durchgehen (aber keine Volumes und sich selbst).
            for (SceneryObject obj : sceneryObjects.objects) {
                if (obj instanceof SceneryAreaObject) {
                    SceneryFlatObject area = (SceneryFlatObject) obj;
                    SmartPolygon polygon = area.getArea().poly;
                    if (polygon.polygon.length > 1 || above.polygon.length > 1) {
                        logger.warn("many polygons TODO");
                    }
                    if (polygon.polygon[0].intersects(above.polygon[0])) {
                        bridge.addBelow(area);
                    }
                }
            }
        }*/
        //buildBridgeApproaches();

    }

    /**
     * Overlaps einschneiden/einbetten oder als Overlay markieren und später leicht erhöhen.
     * Betrifft z.B. Decorations. Aber nicht mehr  overlapping SOs.
     */
    public void processOverlaps() {
        /*23.7.19if (!SceneryBuilder.FTR_OVERLAPS) {
        //23.7.19return;
        //23.7.19}
        for (SceneryObject obj : sceneryObjects.objects) {
            //Nur Ways, Decorations sind schon overlay. Und nicht mit Ways, sonst finden sich z.Z. auch Junctions
            if (obj instanceof SceneryWayObject) {
                SceneryWayObject way = (SceneryWayObject) obj;
                //But don't mark bridges/tunnel, these shouldn't be overlays, but overlap filler. TODO use something like isPossibleOverlay()?
                if (!(way instanceof BridgeOrTunnel)) {
                    List<SceneryFlatObject> overlaps = SceneryObjectList.getOverlaps(sceneryObjects.objects,way);
                    boolean foundoverlap = false;
                    for (SceneryFlatObject overlap : overlaps) {
                        if (!(overlap instanceof SceneryWayObject)) {
                            //Test gerade biegen für roadToBridgeFromNorth. TODO
                            if (!way.getOsmIdsAsString().contains("8033747")) {
                                way.getArea().isOverlay = true;
                            }
                        }
                    }
                }
            }
        }*/
        boolean einschneiden = false;
        if (einschneiden) {
            //TODO Bridges aber nicht!
        } else {
            // Dann alle Decorations als Overlay markieren
            for (SceneryObject obj : sceneryObjects.objects) {
                for (AbstractArea deco : obj.getDecorations()) {
                    deco.isOverlay = true;
                }
            }
        }
    }

    /**
     * Eigentlich sollen sich keine Polygone overlappen (im Sinne von UniqueCoor und MeshPolygon).
     * BG ist hier nicht einbezogen, denn der wird evtl. erst später ermittelt.
     * Es werden nur TerrainProvider verglichen, denn Bridges/Tunnel overlappen sich ja immer.
     * Man kann aber trotzdem mit BG pruefen. Wenns noch keinen gibt wird halt nichts gefunden.
     *
     * @return
     */
    public int checkForOverlappingAreas(boolean verbose) {
        IntHolder cnt = new IntHolder(0);
        nestedTerrainProviderLoop((sfo, sfo1) -> {
            if (sfo.getOsmIdsAsString().contains("33817500") && sfo1.getOsmIdsAsString().contains("33817501")) {
                int h = 9;
            }
            if (sfo.overlaps(sfo1)) {
                if (verbose) {
                    logger.debug("area " + sfo.getOsmIdsAsString() + " overlaps area " + sfo1.toString());
                }
                cnt.v++;
            }
        });


        return cnt.v;
    }

    public int overlapsWithAnyTerrainProvider(Polygon polygon) {
        int cnt = 0;
        for (SceneryObject obj : sceneryObjects.objects) {
            if (obj instanceof SceneryFlatObject) {
                SceneryFlatObject sfo = (SceneryFlatObject) obj;

                if (sfo.isTerrainProvider()) {
                    if (((SceneryFlatObject) sfo).overlaps(polygon)) {
                        logger.debug("area " + sfo.getOsmIdsAsString() + " overlaps polygon ");
                        cnt++;
                    }
                }
            }
        }
        return cnt;
    }

    /**
     * Nach dem Clip sollten sich keine Ways mehr overlappen. There are situations, where the minor way
     * isType too close to a main way. (eg. 120831068,225794270,120831071).
     * Und auch sonst können Ways immer mal ungünstig nebeneinander verlaufen.
     *
     * Erst die Connector, denn es gibt z.B. die Fälle, wo der minor explizit betroffen ist und eine Änderung des main unangemessen wäre.
     * Das erledigt vielleicht schon viele Overlaps.
     *
     * Das mit dem nested ist trotzdem fragwürdig. Vielleicht gezielt bei minor ways anfangen? Andererseits brauchts eh eine logische Lösung für das Overlap Problem.
     * Das hier ist dann ja eher eine Notlösung.
     */
    public void resolveWaysAndConnectorOverlaps() {
        sceneryObjects.iterateWayConnectors(sceneryObjects.objects, (connector) -> {
            //"logical" resolve
            connector.resolveOverlaps();
        });

        nestedTerrainProviderLoop((sfo, sfo1) -> {
            if (sfo instanceof SceneryWayObject && sfo.overlaps(sfo1)) {
                AbstractArea[] aa = sfo1.getArea();
                if (aa != null) {
                    if (aa.length != 1) {
                        throw new RuntimeException("invalid usage");
                    }
                    ((SceneryWayObject)sfo).resolveWayOverlaps(aa[0]);
                }
            }
        });
    }

    public void resolveSupplementOverlaps() {
        sceneryObjects.iterateSupplements(sceneryObjects.objects, (supplement) -> {
            List<SceneryFlatObject> overlaps = SceneryObjectList.getTerrainOverlaps(supplement, sceneryObjects.objects);
            if (supplement.isTerrainProvider() && overlaps.size() > 0) {
                supplement.resolveSupplementOverlaps(overlaps);
                //check again
                OverlapResolver.reCheck(supplement,  sceneryObjects.objects);
            }
        });
    }

    private void nestedTerrainProviderLoop(NestedLoopHandler nlh) {
        for (SceneryObject obj : sceneryObjects.objects) {
            if (obj instanceof SceneryFlatObject && obj.isTerrainProvider()) {
                SceneryFlatObject sfo = (SceneryFlatObject) obj;
                for (SceneryObject obj1 : sceneryObjects.objects) {
                    if (obj1 instanceof SceneryFlatObject) {
                        SceneryFlatObject sfo1 = (SceneryFlatObject) obj1;
                        if (sfo != sfo1 && sfo1.isTerrainProvider()) {
                            nlh.handle(sfo, sfo1);
                        }
                    }
                }
            }
        }
    }

    @FunctionalInterface
    public static interface NestedLoopHandler {
        void handle(SceneryFlatObject sfo, SceneryFlatObject sfo1);
    }

    /**
     * BackgroundElemente an den WayConnector durchgehen.
     * Mal sehen, ob man nicht ohne BG klarkommt.
     */
    public int createWayToAreaFiller() {
        int cnt=0;
        TerrainMesh terrainMesh = TerrainMesh.getInstance();
        if (terrainMesh.getStep() != 4) {
            //areas muessen schon drin sein. 28.8.19: Und andere non gapfiller supplements auch.
            throw new RuntimeException("invalid step");
        }
        int len = sceneryObjects.objects.size();
        for (int i = 0; i < len; i++) {
            SceneryObject obj = sceneryObjects.objects.get(i);
            if (obj instanceof SceneryWayObject) {
                SceneryWayObject way = (SceneryWayObject) obj;
                MeshFillCandidate[] candidates;
                if ((candidates = createWayToAreaCandidates(way)) != null) {
                    logger.debug("found wayToArea candidate for way " + way.getOsmIdsAsString());
                    for (MeshFillCandidate candidate : candidates) {
                        //sicherheitshalber auf overlap pruefen, weil das einen Riesen Kudddelmuddel verursachen würde.
                        //und im Zweifel auf das Supplement verzichten.
                        if (overlapsWithAnyTerrainProvider(candidate.polygon) == 0) {
                            logger.debug("Creating new wayToArea Filler");
                            //weitgehend registrieren, um Inkonsistenzen bei Fehlern gering zu halten.
                            candidate.register();
                            if (candidate.lines == null) {
                                logger.error("register failed");
                            } else {
                                ScenerySupplementAreaObject filler = SceneryObjectFactory.createWayToAreaFiller(way, candidate);
                                sceneryObjects.add(filler);
                                cnt++;
                            }
                        } else {
                            logger.warn("found overlaps for candidate");
                        }
                    }
                }
            }
        }
        return cnt;
    }

    /**
     * Erstmal nur rechts versuchen
     * Ways und normals Areas liegen im TerrainMesh schon vor.
     *
     * @param way
     * @return
     */
    public MeshFillCandidate[] createWayToAreaCandidates(SceneryWayObject way) {

        WayArea wayArea = way.getWayArea();
        if (wayArea == null) {
            return null;
        }
        SceneryAreaObject possibletarget = null;
        CoordinatePair pair0;

        if (wayArea == null || wayArea.isEmpty()) {
            return null;
        }
        List<MeshLine> leftlines = wayArea.getLeftLines();
        List<MeshLine> rightlines = wayArea.getRightLines();
        if (leftlines == null || rightlines == null) {
            if (SceneryBuilder.WayToAreaFillerDebugLog) {
                logger.debug("no left/right lines");
            }
            return null;
        }
        //Coordinate[] coors = wayArea.getPolygons()[0].getCoordinates();
        if (way.getOsmIdsAsString().contains("37935545")) {
            int h = 9;
        }


        //TODO nicht immer erste line. Und waere nicht ein Zugriff auf Segments besser. Na, wohl nicht??
        MeshLine rightline = rightlines.get(0);
        //Die inneren Points sollen eine andere Area treffen. Start/End Pair können dann schraeg verlaufen
        List<Coordinate> candidate =new ArrayList<>();
        for (int i = 1; i < rightline.size()/*wayArea.getLength()*/ - 1; i++) {
            /*CoordinatePair pair = wayArea.getPair(i);
            if (pair == null) {
                return null;
            }*/
            Coordinate c = rightline.get(i);
            Vector2 normal = wayArea.getNormalAtCoordinate(c/*pair.getFirst()*/);
            //mal in 5 Metern versuchen
            Coordinate destination = JtsUtil.moveCoordinate(c/*pair.getFirst()*/, normal.multiply(5));
            List<SceneryAreaObject> result = sceneryObjects.findAreasByCoordinate(destination);
            if (result.size() > 0) {
                //logger.debug("possible candidate for " + way.getOsmIdsAsString() + ":" + result.get(0).getOsmIdsAsString());
                if (possibletarget == null) {
                    possibletarget = result.get(0);
                } else {
                    if (result.get(0) != possibletarget) {
                        return null;
                    }
                }

            }
            candidate.add(c);
        }
        if (possibletarget == null) {
            return null;
        }
        if (SceneryBuilder.WayToAreaFillerDebugLog) {
            logger.debug("inner points all fit for line " + rightline + " in way " + way.getOsmIdsAsString() + ":" + possibletarget.getOsmIdsAsString());
        }
        if (!possibletarget.isPartOfMesh()) {
            logger.error("not part of mesh: " + possibletarget.getOsmIdsAsString());
            return null;
        }
        Polygon polygon = possibletarget.getArea()[0].getPolygon();
        if (polygon == null) {
            logger.error("no polygon: " + possibletarget.getOsmIdsAsString());
            return null;
        }
        Coordinate[] polycoors = polygon.getCoordinates();
        //pair0 = wayArea.getPair(0);

        int c0;
        double maxdistance = 13;
        MeshLine targetLine;
        if ((c0 = JtsUtil.findClosestWithinDistance(rightline.getFrom().coordinate, polycoors, maxdistance)) == -1) {
            return null;
        }
        int c1;
        if ((c1 = JtsUtil.findClosestWithinDistance(rightline.getTo().coordinate, polycoors, maxdistance)) == -1) {
            return null;
        }
        if ((targetLine = possibletarget.getArea()[0].findMeshLineWithCoordinates(polycoors[c0], polycoors[c1])) == null) {
            return null;
        }

        //einfach die reche line nehmen. TODO viel zu simple.
        //9.9.19: so nicht. oben mitlisten. List<Coordinate> candidate = wayArea.getRightOutline();
        //aber der erste durefte noch fehlen. TODO das ist alles noch unrund. die hinteren fehlen evt. auch
        candidate=new ArrayList(Arrays.asList(rightline.getCoordinates()));
        //candidate.add(0,rightline.get(0));
        candidate.addAll(JtsUtil.sublist(polycoors, c1, c0));
        candidate.add(candidate.get(0));
        Polygon cp = JtsUtil.createPolygon(candidate);
        if (!cp.isValid()) {
            logger.warn("invalid");
        }
        return new MeshFillCandidate[]{new MeshFillCandidate(cp, rightline, targetLine, c0, c1, true)};
    }

    public void connectWayWithArea(SceneryWayObject way, int pairindex, SceneryAreaObject area, boolean right) {
        WayArea wayArea = way.getWayArea();
        CoordinatePair pair = wayArea.getPair(0);


        Coordinate[] wcoors = way.getArea()[0].getPolygon().getCoordinates();

        Coordinate[] acoors = area.getArea()[0].getPolygon().getCoordinates();

    }

    /**
     * Areas sollen adjacent areas (gemaess OSM) kennen.
     * static zum Decoupling und Test
     *
     * @param objects
     */
    public static void connectAreas(List<SceneryObject> objects) {
        SceneryObjectList.iterateAreas(objects, (area1) -> {
            SceneryObjectList.iterateAreas(objects, (area2) -> {
                if (area1 != area2) {
                    SceneryAreaObject.registerAdjacentAreas(area1, area2);
                }
            });
        });
    }


    /**
     * Decoupled from approachbuilding.
     * Trotzdem unrund. Läuft vor Polygonbuilding, muesste aber below kennen. Das ist ein doofer Konflikt.
     * Eigentlich nicht. Denn er braucht Polygon und läuft daher später.
     */
    /*public void closeBridgeGaps(){
        for (BridgeModule.Bridge bridge : SceneryContext.getInstance().bridges.values()) {
            
            sceneryObjects.objects.add(bridge.closeBridgeGap(/*sroads.get(0), eroads.get(0))* /));

        }
    }*/
    /*public void removeEmptyAreas(){
        
        Iterator<SceneryArea> iter = areas.iterator();
        while (iter.hasNext()){
            SceneryArea a=iter.next();
            if (a.getPolygon().isEmpty()){
                areas.remove(a);
            }
        }
    }*/

}
