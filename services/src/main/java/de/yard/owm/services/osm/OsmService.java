package de.yard.owm.services.osm;

import com.vividsolutions.jts.triangulate.ConstraintEnforcementException;
import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.Phase;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryConversionFacade;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.ElevationMap;
import de.yard.threed.osm2scenery.modules.SceneryModule;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.InvalidGeometryException;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.O2WEleConnector;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMWay;
import de.yard.threed.osm2world.Renderable;
import de.yard.threed.osm2world.TerrainElevationData;
import de.yard.threed.osm2world.TerrainInterpolator;
import de.yard.threed.osm2world.VectorXYZ;
import de.yard.threed.osm2world.WorldObject;
import de.yard.threed.osm2world.ZeroInterpolator;
import de.yard.threed.traffic.geodesy.ElevationProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static de.yard.threed.osm2scenery.scenery.SceneryObject.Cycle.*;
import static de.yard.threed.osm2scenery.scenery.SceneryObject.Cycle.SUPPLEMENT;
import static java.util.Collections.emptyList;

/**
 * Server based counterpart of SceneryConversionFacade
 */
@Service
@Slf4j
public class OsmService {

    @Value(value = "${modules}")
    String[] modules;

    /**
     * Use this when all data isType already
     * in memory, for example with editor applications.
     * To obtain the data, you can use an { OSMDataReader}.
     * Can be run multiple times with different configurations. modulelist isType derived from configuration.
     * Rendering to targets extracted.
     * <p>
     * targetBounds kann auch null sein.
     *
     * @throws SceneryConversionFacade.BoundingBoxSizeException for oversized bounding boxes
     */
    public OsmService.Results createRepresentations(GridCellBounds targetBounds, SceneryProjection mapProjection, OSMData osmData)
            throws IOException, SceneryConversionFacade.BoundingBoxSizeException {


        /* create map data from OSM data */
        Phase.updatePhase(Phase.MAP_DATA);

        OSMToSceneryDataConverter converter = new OSMToSceneryDataConverter(mapProjection, targetBounds);
        MapData mapData = converter.createMapData(osmData);
        SceneryContext.init(mapData);

        Phase.updatePhase(Phase.OBJECTS);

        List<SceneryModule> worldModules = null;
        worldModules = new ArrayList<>();
        for (String modulename : modules) {

            try {
                String classname = "de.yard.threed.osm2scenery.modules." + modulename;
                Class clazz = Class.forName(classname);
                SceneryModule instance = (SceneryModule) clazz.newInstance();
                worldModules.add(instance);
            } catch (Exception e) {
                log.error("Failure loading module " + modulename);
                e.printStackTrace();
            }
        }

        //26.3.24 Materials.configureMaterials(compositeConfiguration);
        //this will cause problems if multiple conversions are run
        //at the same time, because global variables are being modified

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
        // step by step approach istead of previous "all-in-one".
        for (MapWay mapWay: mapData.getMapWays()) {
            // 1 Scenery Objekte erstellen. WayConnector werden hier auch schon erstellt.
            for (SceneryModule module : SceneryModule.getRelevant(worldModules, mapWay)) {
                SceneryObjectList areas = module.applyTo(mapWay);
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
            sceneryMesh.createElevationGroups();

            //TODO 23.5.19 buildBridgeApproaches besser in Phasen abstrahieren.
            sceneryMesh.buildBridgeApproaches();


            //erst dann, wenn alle Objekte und Verbindungen bekannt sind, die Polygone dazu erstellen
            Phase.updatePhase(Phase.WAYS);
            processCycle(sceneryMesh, WAY);

            log.info("Resolving way overlaps");
            sceneryMesh.resolveWaysAndConnectorOverlaps();
            SceneryContext.getInstance().overlappingways = sceneryMesh.checkForOverlappingAreas(true);
            log.debug("After resolving still " + SceneryContext.getInstance().overlappingways + " overlapping terrain provider way areas");

            //Ermitteln, was unter den Bridges ist und die Approaches der Bridges. Dazu werden die Polygone gebraucht.
            //13.7.19 nicht mehr sceneryMesh.completeBridgeRelations();
            //bridge gap soll eigentlich eine normale Area sein, braucht aber Polygon. Und "below".
            //sceneryMesh.closeBridgeGaps();

            if (SceneryBuilder.FTR_SMARTGRID) {
                Phase.updatePhase(Phase.GRIDREARRANGE);

                // 26.3.24 no longer cut targetBounds.rearrangeForWayCut(sceneryMesh.sceneryObjects.objects);
            }

            //die Areas brauchen für den Cut das finale Grid mit LazyCuts
            Phase.updatePhase(Phase.BUILDINGSANDAREAS);
            for (SceneryObject.Cycle cycle : new SceneryObject.Cycle[]{SceneryObject.Cycle.BUILDING, GENERICAREA, UNKNOWN}) {
                processCycle(sceneryMesh, cycle);
            }
            SceneryContext.getInstance().overlappingterrain = sceneryMesh.checkForOverlappingAreas(true);
            log.debug(SceneryContext.getInstance().overlappingterrain + " overlapping terrain areas");

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
            log.info("Initializing terrain mesh with " + sceneryMesh.sceneryObjects.size() + " scenery objects (ways and areas).");
            /*26.3.24 TODO if (SceneryBuilder.FTR_SMARTGRID) {
                TerrainMesh.init(targetBounds);
                //sonst geht waytoarea filler nicht if (SceneryBuilder.FTR_SMARTBG) {
                //erst die Ways, danach areas, um Komplkationen zu vermeiden.
                log.info("adding ways to terrain mesh");
                TerrainMesh.getInstance().addWays(sceneryMesh.sceneryObjects);
                log.info("adding areas to terrain mesh");
                TerrainMesh.getInstance().addAreas(sceneryMesh.sceneryObjects);
                //}
            }

            //Supplements anlegen und verarbeiten
            Phase.updatePhase(Phase.SUPPLEMENTS);
            log.info("Creating supplements for " + sceneryMesh.sceneryObjects.size() + " scenery objects.");

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
            log.info("Resolving supplement overlaps");
            sceneryMesh.resolveSupplementOverlaps();
            // wenn durch Supplements overlaps entstanden sind, wird das mit Sicherheit zu Problemen im TerrainMesh führen.
            // SceneryContext.getInstance().unresolvedoverlaps ist aber nur der Zaehler fuer versuchte und gescheiterte!
            // Darum neu zählen.
            String comment = "no terrain overlaps";
            SceneryContext.getInstance().overlappingTerrainWithSupplements = sceneryMesh.checkForOverlappingAreas(true);
            if (SceneryContext.getInstance().overlappingTerrainWithSupplements > 0) {
                comment = "" + SceneryContext.getInstance().overlappingTerrainWithSupplements + " terrain overlaps";
            }
            log.info("Created " + sceneryMesh.sceneryObjects.findObjectsByCycle(SUPPLEMENT).size() + " supplements. Now " + sceneryMesh.sceneryObjects.size() + " scenery objects (" +
                    comment + ").Start adding to mesh.");

            // Supplements muessen auch ins TerrainMesh
            TerrainMesh.getInstance().addSupplements(sceneryMesh.sceneryObjects.findObjectsByCycle(SUPPLEMENT));
            boolean meshValid = TerrainMesh.getInstance().isValid(true);
            //gap filler sind zwar auch supplements. Aber die haengen sich schon selber ins mesh.
            log.info("Supplements added to terrain mesh (mesh " + ((meshValid) ? "valid" : "invalid") + "). Creating gap filler");
            int cnt = sceneryMesh.createWayToAreaFiller();
            log.info("Created " + cnt + " gap filler");

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
            end of TODO */

        }

        /* 26.3.24 no longer here
        //4.8.18 mal vor der Elevation, weil scheinbar bei der Trianglation die z-Coordinaten durcheinander kommen können. Schon skuril!
        //24.4.19: Und weil bei der Triangulation noch Coordinates entstehen.
        Phase.updatePhase(Phase.TRIANGULATION);
        sceneryMesh.triangulateAndTexturize();*/

        //24.4.19: Der ganze Elekram erst jetzt, wenn alle Polygone final sind. Elegroups gibt es aber schon lange.
        Phase.updatePhase(Phase.ELEVATION);
        sceneryMesh.connectElevationGroups();

        // 28.8.18: Vorab Elevation vorbereiten, damit die Groups angelegt werden koennen.
        // Die Property ElevationProvider legt nicht nur den Provider fest, sondern
        // ist auch der Schalter fuer (de)aktieiveren der Elevation Berechnung.
        // Ohne gibt es nur die GroundStates, die dann ein Relief liefern koennen.
        String elevationProvidername = null;//26.3.24 compositeConfiguration.getString("ElevationProvider", null);
        /*String srtmDir = compositeConfiguration.getString("srtmDir", null);
        TerrainElevationData eleData = null;

        if (srtmDir != null) {
            eleData = new SRTMData(new File(srtmDir), mapProjection);
        }*/

        ElevationMap.drop();
        ElevationProvider elevationProvider = null;

        if (elevationProvidername != null && targetBounds != null) {
            log.info("Elevation Provider isType " + elevationProvidername);
            String classname = elevationProvidername;// + ".class";
            try {
                Class clazz = Class.forName(classname);
                elevationProvider = (ElevationProvider) clazz.newInstance();

                ElevationMap.init(elevationProvider, (GridCellBounds) targetBounds, mapProjection);
                //fixElevationGroups(mapData, /*eleData*/elevationProvider, compositeConfiguration);
                //later sceneryMesh.fixElevationGroups(elevationProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.info("No Elevation provider or nor grid. Elevation not calculated");
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


        return new OsmService.Results(mapProjection, mapData, null/*eleData*/, sceneryMesh);

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

    private void processCycle(SceneryMesh sceneryMesh, SceneryObject.Cycle cycle) {
        //Phase.updatePhase(Phase.POLYGONS);
        //sceneryMesh.createNonWaysPolygons();
        List<ScenerySupplementAreaObject> supplements = sceneryMesh.createPolygons(cycle);
        if (SceneryBuilder.FTR_OVERLAPCAUSESSUPPLEMENT) {
            //supplements verarbeiten fehlt.
            Util.notyet();
        }

        //Phase.updatePhase(Phase.CLIP);
        //sceneryMesh.clipNonWays();
        sceneryMesh.clip(cycle);

        // und aus dem Background ausschneiden und selber zuschneiden.
        //Phase.updatePhase(Phase.CUT);
        sceneryMesh.insertSceneryObjectsIntoBackgroundAndCut(cycle);
    }

    /**
     * uses OSM data and an terrain elevation data (usually from an external
     * source) to calculate elevations for all { EleConnector}s of the
     * {@link WorldObject}s
     */
    private void calculateElevations(MapData mapData,
                                     TerrainElevationData eleData, Configuration config) {

        final TerrainInterpolator interpolator =null;
                /*26.3.24 (eleData != null)
                        ? terrainEleInterpolatorFactory.make()
                        : new ZeroInterpolator();*/

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

        //Better log each exception with causing OSM element

        for (WorldObject worldObject : mapData.getWorldObjects()) {
            try {
                for (O2WEleConnector conn : worldObject.getEleConnectors()) {
                    conn.setPosXYZ(interpolator.interpolateEle(conn.pos));
                    connectors.add(conn);
                }
            } catch (ConstraintEnforcementException enforcementException) {
                // just warning because it just happens
                log.warn("Caught ConstraintEnforcementException");
            } catch (InvalidGeometryException invalidGeometryException) {
                // just warning because it just happens
                log.warn("Caught InvalidGeometryException");
            } catch (Exception exception) {
                // just warning because it just happens
                log.warn("Caught general Exception", exception);
            }
        }

        System.out.println("time terrain interpolation: " + stopWatch);
        stopWatch.reset();
        stopWatch.start();

        /* enforce constraints defined by WorldObjects */

        boolean debugConstraints = config.getBoolean("debugConstraints", false);

        /*26.3.24 final EleConstraintEnforcer enforcer = debugConstraints
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
         */

        System.out.println("time enforce constraints: " + stopWatch);
        stopWatch.reset();
        stopWatch.start();

    }

    /*26.3.24 public SceneryModule getModule(String name) {
        for (SceneryModule m : worldModules) {
            String n = m.getClass().getSimpleName();
            if (n.equals(name)) {
                return m;
            }
        }
        return null;
    }*/

     /*26.3.24public MapData getMapData() {
        return mapData;
    }*/

    private void init(Configuration compositeConfiguration) {

        /*26.3.24 String interpolatorType = compositeConfiguration.getString("terrainInterpolator");
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

        }*/

    }
}

