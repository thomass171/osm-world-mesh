package de.yard.threed.osm2scenery;

import com.vividsolutions.jts.triangulate.ConstraintEnforcementException;
import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.ElevationMap;
import de.yard.threed.osm2scenery.modules.SceneryModule;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.DefaultFactory;
import de.yard.threed.osm2world.EleConstraintEnforcer;
import de.yard.threed.osm2world.EleConstraintValidator;
import de.yard.threed.osm2world.Factory;
import de.yard.threed.osm2world.FaultTolerantIterationUtil;
import de.yard.threed.osm2world.InvalidGeometryException;
import de.yard.threed.osm2world.LeastSquaresInterpolator;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.Materials;
import de.yard.threed.osm2world.NaturalNeighborInterpolator;
import de.yard.threed.osm2world.NoneEleConstraintEnforcer;
import de.yard.threed.osm2world.O2WEleConnector;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.Renderable;
import de.yard.threed.osm2world.SimpleEleConstraintEnforcer;
import de.yard.threed.osm2world.TerrainElevationData;
import de.yard.threed.osm2world.TerrainInterpolator;
import de.yard.threed.osm2world.VectorXYZ;
import de.yard.threed.osm2world.WorldObject;
import de.yard.threed.osm2world.ZeroInterpolator;
import de.yard.threed.traffic.geodesy.ElevationProvider;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static de.yard.threed.osm2scenery.scenery.SceneryObject.Cycle.*;
import static java.util.Collections.emptyList;

/**
 * provides an easy way to call all steps of the conversion process
 * in the correct order.
 * Instanciated by build() method.
 * <p>
 * Abgeleitet vom OSM2World ConversionFacade.java
 */
public class SceneryConversionFacade {
    Logger logger = Logger.getLogger(SceneryConversionFacade.class.getName());
    OSMData osmData = null;
    MapData mapData = null;
    List<SceneryModule> worldModules = null;
    //9.4.19 OriginMapProjection mapProjection = null;

    /**
     *
     */
    public SceneryConversionFacade(OSMData osmData) {
        if (osmData == null) {
            throw new IllegalArgumentException("osmData must not be null");
        }

        this.osmData = osmData;
        // moved here from below
        //mapProjection = mapProjectionFactory.make();
        //mapProjection.setOrigin(osmData);
    }

    /**
     * all results of a conversion run
     */
    public static final class Results {

        private final SceneryProjection mapProjection;
        private final MapData mapData;
        private final TerrainElevationData eleData;
        public SceneryMesh sceneryMesh;

        private Results(SceneryProjection mapProjection, MapData mapData, TerrainElevationData eleData, SceneryMesh sceneryMesh) {
            this.mapProjection = mapProjection;
            this.mapData = mapData;
            this.eleData = eleData;
            this.sceneryMesh = sceneryMesh;
        }

        /*public SceneryProjection getMapProjection() {
            return mapProjection;
        }*/

        public MapData getMapData() {
            return mapData;
        }

        public TerrainElevationData getEleData() {
            return eleData;
        }

        /**
         * collects and returns all representations that implement a
         * renderableType, including terrain.
         * Convenience method.
         */
        public <R extends Renderable> Collection<R> getRenderables(Class<R> renderableType) {
            return getRenderables(renderableType, true, true);
        }

        /**
         * @see #getRenderables(Class)
         */
        public <R extends Renderable> Collection<R> getRenderables(
                Class<R> renderableType, boolean includeGrid, boolean includeTerrain) {

            //TODO make use of or drop includeTerrain

            Collection<R> representations = new ArrayList<R>();

            if (includeGrid) {
                for (R r : mapData.getWorldObjects(renderableType)) {
                    representations.add(r);
                }
            }

            return representations;

        }

    }

    /*private Factory<? extends OriginMapProjection> mapProjectionFactory =
            new DefaultFactory<MetricMapProjection>(MetricMapProjection.class);*/

    private Factory<? extends TerrainInterpolator> terrainEleInterpolatorFactory =
            new DefaultFactory<LeastSquaresInterpolator>(LeastSquaresInterpolator.class);

    private Factory<? extends EleConstraintEnforcer> eleConstraintEnforcerFactory =
            new DefaultFactory<NoneEleConstraintEnforcer>(NoneEleConstraintEnforcer.class);


    /**
     * sets the factory that will make {@link EleConstraintEnforcer}
     * instances during subsequent calls to
     *
     * @see DefaultFactory
     */
    public void setEleConstraintEnforcerFactory(
            Factory<? extends EleConstraintEnforcer> interpolatorFactory) {
        this.eleConstraintEnforcerFactory = interpolatorFactory;
    }

    /**
     * sets the factory that will make {@link TerrainInterpolator}
     * instances during subsequent calls to
     *
     * @see DefaultFactory
     */
    public void setTerrainEleInterpolatorFactory(
            Factory<? extends TerrainInterpolator> enforcerFactory) {
        this.terrainEleInterpolatorFactory = enforcerFactory;
    }

    /**
     * Extracted from createRepresentations.
     */
    /*26.4.19public void render(SceneryConversionFacade.Results results, List<Target<?>> targets) {
        boolean underground = Config.getCurrentConfiguration().getBoolean("renderUnderground", true);

        if (targets != null) {
            for (Target<?> target : targets) {
                TargetUtil.renderWorldObjects(target, results.getMapData(), underground);
                target.finish();
            }
        }
    }*/

    /**
     * Use this when all data isType already
     * in memory, for example with editor applications.
     * To obtain the data, you can use an { OSMDataReader}.
     * Can be run multiple times with different configurations. modulelist isType derived from configuration.
     * Rendering to targets extracted.
     * <p>
     * targetBounds kann auch null sein.
     *
     * @throws BoundingBoxSizeException for oversized bounding boxes
     */
    public Results createRepresentations(GridCellBounds targetBounds, SceneryProjection mapProjection)
            throws IOException, BoundingBoxSizeException {

        Configuration compositeConfiguration = Config.getCurrentConfiguration();
        init(compositeConfiguration);

        /*Double maxBoundingBoxDegrees = compositeConfiguration.getDouble("maxBoundingBoxDegrees", null);
        if (maxBoundingBoxDegrees != null) {
            for (Bound bound : osmData.getBounds()) {
                if (bound.getTop() - bound.getBottom() > maxBoundingBoxDegrees
                        || bound.getRight() - bound.getLeft() > maxBoundingBoxDegrees) {
                    throw new BoundingBoxSizeException(bound);
                }
            }
        }*/

        /* create map data from OSM data */
        Phase.updatePhase(Phase.MAP_DATA);

        //in constrctor now: mapProjection = mapProjectionFactory.make();
        //mapProjection.setOrigin(osmData);


        //GridCellBounds targetBounds = null;//(GridCellBounds) Config.getInstance().getTargetBounds();


        OSMToSceneryDataConverter converter = new OSMToSceneryDataConverter(mapProjection, targetBounds);
        mapData = converter.createMapData(osmData);
        SceneryContext.init(mapData);

        Phase.updatePhase(Phase.OBJECTS);

        worldModules = new ArrayList<>();
        String[] modulelist = Config.getInstance().getModules();
        for (String modulename : modulelist) {
            //String modulename = (String) modconfig.getString("name");
            if (Config.getCurrentConfiguration().getBoolean("modules." + modulename + ".enabled")) {
                //logger.debug("Building module " + modulename);
                try {
                    String classname = Config.getCurrentConfiguration().getString("modules." + modulename + ".class");
                    //9.4.19 classname = classname.replaceAll(".*\\.modules", "de.yard.threed.osm2scenery.modules");
                    Class clazz = Class.forName(classname);
                    SceneryModule instance = (SceneryModule) clazz.newInstance();
                    worldModules.add(instance);
                } catch (Exception e) {
                    logger.error("Failure loading module " + modulename);
                    e.printStackTrace();
                }
            }
        }


        Materials.configureMaterials(compositeConfiguration);
        //this will cause problems if multiple conversions are run
        //at the same time, because global variables are being modified

        /*SceneryCreator moduleManager =
                new SceneryCreator(compositeConfiguration, worldModules);
        if (targetBounds != null) {
            moduleManager.sceneryMesh.setBackgroundMesh(targetBounds);
        }
        SceneryMesh sceneryMesh=  SceneryCreator.buildSceneryObjects(mapData,targetBounds,worldModules);*/
        SceneryMesh sceneryMesh = new SceneryMesh();
        if (targetBounds != null) {
            sceneryMesh.setBackgroundMesh(targetBounds);
        }


        //handle posible old instances.

        // Das braucht schon eine besondere Reihenfolge:
        //Evtl. MapData ergänzen
        for (SceneryModule module : worldModules) {
            module.extendMapData(osmData.source, mapData, converter);
        }
        // 1 Scenery Objekte erstellen. WayConnector werden hier auch schon erstellt.
        for (SceneryModule module : worldModules) {
            SceneryObjectList areas = module.applyTo(mapData);
            sceneryMesh.sceneryObjects.objects.addAll(areas.objects);
        }

        // Eine halbwegs schlüssige Klassifizierung (z.B. Garage) der Objekte geht erst jetzt, wenn der Kontext bekannt ist.
        Phase.updatePhase(Phase.CLASSIFY);
        for (SceneryModule module : worldModules) {
            module.classify(mapData);
        }
        //20.8.19: ist doch zu frueh sceneryMesh.connectAreas(sceneryMesh.sceneryObjects.objects);

        Phase.updatePhase(Phase.ELEGROUPS);

        //4.4.19 schon gemacht? sceneryMesh.createConnections();
        //Bridgeapproaches werden fuer Polygon gebraucht.
        //24.4.19: Und die brauchen Elegroups für die Groundstates.
        EleConnectorGroup.clear();
        EleConnectorGroup.init((GridCellBounds) targetBounds, mapProjection);
        sceneryMesh.createElevationGroups(sceneryMesh.sceneryObjects.objects);

        //TODO 23.5.19 buildBridgeApproaches besser in Phasen abstrahieren.
        sceneryMesh.buildBridgeApproaches(sceneryMesh.sceneryObjects.objects, SceneryContext.getInstance());


        //erst dann, wenn alle Objekte und Verbindungen bekannt sind, die Polygone dazu erstellen
        Phase.updatePhase(Phase.WAYS);
        processCycle(sceneryMesh, WAY);

        logger.info("Resolving way overlaps");
        sceneryMesh.resolveWaysAndConnectorOverlaps();
        SceneryContext.getInstance().overlappingways = sceneryMesh.checkForOverlappingAreas(true);
        logger.debug("After resolving still " + SceneryContext.getInstance().overlappingways + " overlapping terrain provider way areas");

        //Ermitteln, was unter den Bridges ist und die Approaches der Bridges. Dazu werden die Polygone gebraucht.
        //13.7.19 nicht mehr sceneryMesh.completeBridgeRelations();
        //bridge gap soll eigentlich eine normale Area sein, braucht aber Polygon. Und "below".
        //sceneryMesh.closeBridgeGaps();

        if (SceneryBuilder.FTR_SMARTGRID) {
            Phase.updatePhase(Phase.GRIDREARRANGE);

            targetBounds.rearrangeForWayCut(sceneryMesh.sceneryObjects.objects, sceneryMesh.terrainMesh);
            //jetzt noch pruefe, ob es aus dem Grid ragende Wayteile gibt (z.B. bei TestData) TODO
        }

        //die Areas brauchen für den Cut das finale Grid mit LazyCuts
        Phase.updatePhase(Phase.BUILDINGSANDAREAS);
        for (SceneryObject.Cycle cycle : new SceneryObject.Cycle[]{SceneryObject.Cycle.BUILDING, GENERICAREA, UNKNOWN}) {
            processCycle(sceneryMesh, cycle);
        }
        SceneryContext.getInstance().overlappingterrain = sceneryMesh.checkForOverlappingAreas(true);
        logger.debug(SceneryContext.getInstance().overlappingterrain + " overlapping terrain areas");

        //erst wenn alle Polygone/Areas da sind, können adjacent areas ermittelt werden.
        sceneryMesh.connectAreas(sceneryMesh.sceneryObjects.objects);

        //Konsistenzcheck. OSM Objekte sind jetzt alle angelegt. Supplements darf noch nicht geben.
        List<SceneryObject> supples = sceneryMesh.sceneryObjects.findObjectsByCycle(SUPPLEMENT);
        if (supples.size() > 0) {
            throw new RuntimeException("supplements not yet expected");
        }

        // Vor den Supplements das Mesh erstellen. Die Supplements koennen dann daran anschliessen, muessen aber nicht (oder auch teilweise).
        // Ohne Smartgrid kann das nicht konsistent werden.
        Phase.updatePhase(Phase.TERRAINMESH);
        logger.info("Initializing terrain mesh with " + sceneryMesh.sceneryObjects.size() + " scenery objects (ways and areas).");
        if (SceneryBuilder.FTR_SMARTGRID) {
            sceneryMesh.terrainMesh = TerrainMesh.init(targetBounds);
            //sonst geht waytoarea filler nicht if (SceneryBuilder.FTR_SMARTBG) {
            //erst die Ways, danach areas, um Komplkationen zu vermeiden.
            logger.info("adding ways to terrain mesh");
            sceneryMesh.terrainMesh.addWays(sceneryMesh.sceneryObjects);
            logger.info("adding areas to terrain mesh");
            sceneryMesh.terrainMesh.addAreas(sceneryMesh.sceneryObjects);
            //}
        }

        //Supplements anlegen und verarbeiten
        Phase.updatePhase(Phase.SUPPLEMENTS);
        logger.info("Creating supplements for " + sceneryMesh.sceneryObjects.size() + " scenery objects.");

        for (SceneryModule module : worldModules) {
            //Das durefen definitionsgemaess nur Supplements mit Cycle SUPPLEMENT sein.
            List<ScenerySupplementAreaObject> supplements = module.createSupplements(Collections.unmodifiableList(sceneryMesh.sceneryObjects.objects));
            if (supplements != null) {
                for (ScenerySupplementAreaObject s : supplements) {
                    //Supplement haben vielleicht gar keine eigenen
                    s.prepareElevationGroups();
                }
                sceneryMesh.sceneryObjects.objects.addAll(supplements);
            }
        }
        processCycle(sceneryMesh, SUPPLEMENT);
        logger.info("Resolving supplement overlaps");
        sceneryMesh.resolveSupplementOverlaps(sceneryMesh.terrainMesh);
        // wenn durch Supplements overlaps entstanden sind, wird das mit Sicherheit zu Problemen im TerrainMesh führen.
        // SceneryContext.getInstance().unresolvedoverlaps ist aber nur der Zaehler fuer versuchte und gescheiterte!
        // Darum neu zählen.
        String comment = "no terrain overlaps";
        SceneryContext.getInstance().overlappingTerrainWithSupplements = sceneryMesh.checkForOverlappingAreas(true);
        if (SceneryContext.getInstance().overlappingTerrainWithSupplements > 0) {
            comment = "" + SceneryContext.getInstance().overlappingTerrainWithSupplements + " terrain overlaps";
        }
        logger.info("Created " + sceneryMesh.sceneryObjects.findObjectsByCycle(SUPPLEMENT).size() + " supplements. Now " + sceneryMesh.sceneryObjects.size() + " scenery objects (" +
                comment + ").Start adding to mesh.");

        // Supplements muessen auch ins TerrainMesh
        sceneryMesh.terrainMesh.addSupplements(sceneryMesh.sceneryObjects.findObjectsByCycle(SUPPLEMENT));
        boolean meshValid = sceneryMesh.terrainMesh.isValid(true);
        //gap filler sind zwar auch supplements. Aber die haengen sich schon selber ins mesh.
        logger.info("Supplements added to terrain mesh (mesh " + ((meshValid) ? "valid" : "invalid") + "). Creating gap filler");
        int cnt = sceneryMesh.createWayToAreaFiller();
        logger.info("Created " + cnt + " gap filler");

        Phase.updatePhase(Phase.BACKGROUND);
        sceneryMesh.createBackground();

        //lieber erst nach bridgeaaproaches dekorieren. 24.5.19 braucht doch Polygone. Die Aussenpolygone ändern sich nicht,
        //darum ist nach "cut" OK.
        Phase.updatePhase(Phase.DECORATION);
        sceneryMesh.createDecorations();

        Phase.updatePhase(Phase.OVERLAPS);
        sceneryMesh.processOverlaps();

        //25.4.19: Gefällt mir besser: Erst nach dem cut die Polygone in die Elegroups connecten. Dann spart man sich den
        //Huddle mit den Änderungen durch den cut. Und die Elegroups sind halbwegs frei von alten Coordinates.
        //Sogar erst nach Triangulation. Dann hat man wirklich alle Coordinates.
        Phase.updatePhase(Phase.POLYGONSREADY);
        //sceneryMesh.connectElevationGroups();

        //4.8.18 mal vor der Elevation, weil scheinbar bei der Trianglation die z-Coordinaten durcheinander kommen können. Schon skuril!
        //24.4.19: Und weil bei der Triangulation noch Coordinates entstehen.
        Phase.updatePhase(Phase.TRIANGULATION);
        sceneryMesh.triangulateAndTexturize();

        //24.4.19: Der ganze Elekram erst jetzt, wenn alle Polygone final sind. Elegroups gibt es aber schon lange.
        Phase.updatePhase(Phase.ELEVATION);
        SceneryMesh.connectElevationGroups(sceneryMesh.sceneryObjects.objects, sceneryMesh.terrainMesh);

        // 28.8.18: Vorab Elevation vorbereiten, damit die Groups angelegt werden koennen.
        // Die Property ElevationProvider legt nicht nur den Provider fest, sondern
        // ist auch der Schalter fuer (de)aktieiveren der Elevation Berechnung.
        // Ohne gibt es nur die GroundStates, die dann ein Relief liefern koennen.
        String elevationProvidername = compositeConfiguration.getString("ElevationProvider", null);
        /*String srtmDir = compositeConfiguration.getString("srtmDir", null);
        TerrainElevationData eleData = null;

        if (srtmDir != null) {
            eleData = new SRTMData(new File(srtmDir), mapProjection);
        }*/

        ElevationMap.drop();
        ElevationProvider elevationProvider = null;

        if (elevationProvidername != null && targetBounds != null) {
            logger.info("Elevation Provider isType " + elevationProvidername);
            String classname = elevationProvidername;// + ".class";
            try {
                Class clazz = Class.forName(classname);
                elevationProvider = (ElevationProvider) clazz.newInstance();

               /* if (elevationProvidername.equals("zero")) {
                    elevationProvider = new FixedElevationProvider(0);
                    defaultelevation = 0;
                } else {
                    if (elevationProvidername.equals("fixed")) {
                        elevationProvider = new FixedElevationProvider(defaultelevation);
                    } else {

                        elevationProvider = new FixedElevationProvider(defaultelevation);
                        //elevationProvider = ElevationCache.buildInstance(new ElevationProxy());
                    }
                }*/
                ElevationMap.init(elevationProvider, (GridCellBounds) targetBounds, mapProjection);
                //fixElevationGroups(mapData, /*eleData*/elevationProvider, compositeConfiguration);
                //later sceneryMesh.fixElevationGroups(elevationProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            logger.info("No Elevation provider or nor grid. Elevation not calculated");
        }


        if (ElevationMap.hasInstance()) {
            //26.9.18: einfach alles cathen ist doch wohl doof.
            //try {
            sceneryMesh.calculateElevations(elevationProvider);
            //} catch (Exception e) {
            //    e.printStackTrace();
            //}
        }

        /* create terrain */
        //updatePhase(Phase.TERRAIN); //TODO this phase may be obsolete

        /* supply results to targets and caller */
        //updatePhase(Phase.FINISHED);


        return new Results(mapProjection, mapData, null/*eleData*/, sceneryMesh);

    }

    private void processCycle(SceneryMesh sceneryMesh, SceneryObject.Cycle cycle) {
        //Phase.updatePhase(Phase.POLYGONS);
        //sceneryMesh.createNonWaysPolygons();
        List<ScenerySupplementAreaObject> supplements = sceneryMesh.createPolygons(cycle, sceneryMesh.sceneryObjects.objects, sceneryMesh.gridbounds, sceneryMesh.terrainMesh, SceneryContext.getInstance());
        if (SceneryBuilder.FTR_OVERLAPCAUSESSUPPLEMENT) {
            //supplements verarbeiten fehlt.
            Util.notyet();
        }

        //Phase.updatePhase(Phase.CLIP);
        //sceneryMesh.clipNonWays();
        sceneryMesh.clip(cycle);

        // und aus dem Background ausschneiden und selber zuschneiden.
        //Phase.updatePhase(Phase.CUT);
        sceneryMesh.insertSceneryObjectsIntoBackgroundAndCut(cycle, sceneryMesh.terrainMesh);
    }

    /**
     * uses OSM data and an terrain elevation data (usually from an external
     * source) to calculate elevations for all { EleConnector}s of the
     * {@link WorldObject}s
     */
    private void calculateElevations(MapData mapData,
                                     TerrainElevationData eleData, Configuration config) {

        final TerrainInterpolator interpolator =
                (eleData != null)
                        ? terrainEleInterpolatorFactory.make()
                        : new ZeroInterpolator();

        /* provide known elevations from eleData to the interpolator */

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        if (!(interpolator instanceof ZeroInterpolator)) {

            Collection<VectorXYZ> sites = emptyList();

            try {

                sites = eleData.getSites(mapData);

                System.out.println("time getSites: " + stopWatch);
                stopWatch.reset();
                stopWatch.start();

            } catch (IOException e) {
                e.printStackTrace();
            }

            interpolator.setKnownSites(sites);

            System.out.println("time setKnownSites: " + stopWatch);
            stopWatch.reset();
            stopWatch.start();

        }

        /* interpolate connectors' elevations */

        final List<O2WEleConnector> connectors = new ArrayList<O2WEleConnector>();

        //Better log each exception with causing OSM element FaultTolerantIterationUtil.iterate(mapData.getWorldObjects(),
        //new Operation<WorldObject>() {
        // @Override
        //public void perform(WorldObject worldObject) {

        for (WorldObject worldObject : mapData.getWorldObjects()) {
            try {
                for (O2WEleConnector conn : worldObject.getEleConnectors()) {
                    conn.setPosXYZ(interpolator.interpolateEle(conn.pos));
                    connectors.add(conn);
                }
            } catch (ConstraintEnforcementException enforcementException) {
                // just warning because it just happens
                logger.warn("Caught ConstraintEnforcementException");
            } catch (InvalidGeometryException invalidGeometryException) {
                // just warning because it just happens
                logger.warn("Caught InvalidGeometryException");
            } catch (Exception exception) {
                // just warning because it just happens
                logger.warn("Caught general Exception", exception);
            }
        }

        System.out.println("time terrain interpolation: " + stopWatch);
        stopWatch.reset();
        stopWatch.start();

        /* enforce constraints defined by WorldObjects */

        boolean debugConstraints = config.getBoolean("debugConstraints", false);

        final EleConstraintEnforcer enforcer = debugConstraints
                ? new EleConstraintValidator(mapData,
                eleConstraintEnforcerFactory.make())
                : eleConstraintEnforcerFactory.make();

        enforcer.addConnectors(connectors);

        if (!(enforcer instanceof NoneEleConstraintEnforcer)) {

            FaultTolerantIterationUtil.iterate(mapData.getWorldObjects(),
                    new FaultTolerantIterationUtil.Operation<WorldObject>() {
                        @Override
                        public void perform(WorldObject worldObject) {

                            worldObject.defineEleConstraints(enforcer);

                        }
                    });

        }

        System.out.println("time add constraints: " + stopWatch);
        stopWatch.reset();
        stopWatch.start();

        enforcer.enforceConstraints();

        System.out.println("time enforce constraints: " + stopWatch);
        stopWatch.reset();
        stopWatch.start();

    }

    public SceneryModule getModule(String name) {
        for (SceneryModule m : worldModules) {
            String n = m.getClass().getSimpleName();
            if (n.equals(name)) {
                return m;
            }
        }
        return null;
    }

    public MapData getMapData() {
        return mapData;
    }

    /*9.4.19 public MapProjection getProjection() {
        return mapProjection;
    }*/

    /**
     * implemented by classes that want to be informed about
     * a conversion run's progress
     */
    public static interface ProgressListener {

        /**
         * announces the start of a new phase
         */
        public void updatePhase(Phase newPhase);

//		/** announces the fraction of the current phase that isType completed */
//		public void updatePhaseProgress(float phaseProgress);

    }

    private List<ProgressListener> listeners = new ArrayList<ProgressListener>();

    public void addProgressListener(ProgressListener listener) {
        listeners.add(listener);
    }

    /**
     * exception to be thrown if the OSM input data covers an area
     * larger than the maxBoundingBoxDegrees config property
     */
    public static class BoundingBoxSizeException extends RuntimeException {

        private static final long serialVersionUID = 2841146365929523046L; //generated VersionID
        public final Bound bound;

        private BoundingBoxSizeException(Bound bound) {
            this.bound = bound;
        }

        @Override
        public String toString() {
            return "oversized bounding box: " + bound;
        }

    }

    private void init(Configuration compositeConfiguration) {

        String interpolatorType = compositeConfiguration.getString("terrainInterpolator");
        if ("ZeroInterpolator".equals(interpolatorType)) {
            setTerrainEleInterpolatorFactory(
                    new DefaultFactory<TerrainInterpolator>(ZeroInterpolator.class));
        } else if ("LeastSquaresInterpolator".equals(interpolatorType)) {
            setTerrainEleInterpolatorFactory(
                    new DefaultFactory<TerrainInterpolator>(LeastSquaresInterpolator.class));
        } else if ("NaturalNeighborInterpolator".equals(interpolatorType)) {
            setTerrainEleInterpolatorFactory(
                    new DefaultFactory<TerrainInterpolator>(NaturalNeighborInterpolator.class));
        }

        String enforcerType = compositeConfiguration.getString("eleConstraintEnforcer");
        if ("NoneEleConstraintEnforcer".equals(enforcerType)) {
            setEleConstraintEnforcerFactory(
                    new DefaultFactory<EleConstraintEnforcer>(NoneEleConstraintEnforcer.class));
        } else if ("SimpleEleConstraintEnforcer".equals(enforcerType)) {
            setEleConstraintEnforcerFactory(
                    new DefaultFactory<EleConstraintEnforcer>(SimpleEleConstraintEnforcer.class));
        } else if ("LPEleConstraintEnforcer".equals(enforcerType)) {
            throw new RuntimeException("LPEleConstraintEnforcer not available");
            //cf.setEleConstraintEnforcerFactory(
            //		new DefaultFactory<EleConstraintEnforcer>(LPEleConstraintEnforcer.class));
        }

    }
}
