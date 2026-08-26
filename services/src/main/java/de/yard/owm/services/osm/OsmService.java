package de.yard.owm.services.osm;

import com.vividsolutions.jts.triangulate.ConstraintEnforcementException;
import de.yard.owm.services.mesh.MeshService;
import de.yard.owm.services.persistence.TerrainMeshManager;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.*;
import de.yard.threed.osm2scenery.elevation.ElevationMap;
import de.yard.threed.osm2scenery.modules.SceneryModule;
import de.yard.threed.osm2scenery.modules.SurfaceAreaModule;
import de.yard.threed.osm2scenery.modules.common.WayModule;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.*;
import de.yard.threed.trafficcore.ElevationProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * Server based counterpart of SceneryConversionFacade.
 * Top level service for converting OSM data to scenery objects and terrain mesh.
 */
@Service
@Slf4j
public class OsmService {

    @Value(value = "${modules}")
    String[] modules;

    @Autowired
    OsmElementService osmElementService;

    @Autowired
    TerrainMeshManager terrainMeshManager;

    @Autowired
    MeshService meshService;

    LoggerFactory f;

    /**
     * Add 'mapData' to an existing mesh. Done by applying modules
     * in logical order (decreasing relevance for relevation
     * - river/lakes (water in general)
     * - railways
     * - roads (highways down to path)
     * What happens if an element already exists?
     * 13.2.26 We separate the mesh building part and the visual representation (model, image, aso).
     * Renamed from createRepresentations() to populateMesh()
     * Passing a single osmwayid is intended for test purposes only.
     *
     * @throws SceneryConversionFacade.BoundingBoxSizeException for oversized bounding boxes
     */
    public OsmService.Results populateMesh(String meshName, MapData mapData, Long osmwayid)
            throws IOException, SceneryConversionFacade.BoundingBoxSizeException, MeshInconsistencyException {


        /* create map data from OSM data */
        Phase.updatePhase(Phase.MAP_DATA);

       /* 16.2.26OSMToSceneryDataConverter converter = new OSMToSceneryDataConverter(mapProjection, targetBounds);
        MapData mapData = converter.createMapData(osmData);*/

        SceneryContext sceneryContext = SceneryContext.buildFromDatabase(terrainMeshManager.findOsmWays(), mapData);

        Phase.updatePhase(Phase.OBJECTS);

        /*24.8.26 not needed/used?? List<SceneryModule> worldModules = null;
        worldModules = new ArrayList<>();
        for (String modulename : modules) {

            try {
                String classname = "de.yard.threed.osm2scenery.modules." + modulename;
                Class clazz = Class.forName(classname);
                SceneryModule instance = (SceneryModule) clazz.newInstance();
                worldModules.add(instance);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                log.error("Failure loading module " + modulename);
                e.printStackTrace();
            }
        }*/

        //26.3.24 Materials.configureMaterials(compositeConfiguration);
        //this will cause problems if multiple conversions are run
        //at the same time, because global variables are being modified

        SceneryMesh sceneryMesh = new SceneryMesh();

        // TerrainMesh is still needed temporarily for having Gridcellbounds, projection and meshservicefacade
        //TerrainMesh terrainMesh = terrainMeshManager.loadTerrainMesh(targetBounds);
        TerrainMesh terrainMesh = meshService.loadMesh(meshName);

        //handle posible old instances.

        // Das braucht schon eine besondere Reihenfolge:
        //Evtl. MapData ergänzen
         /* 16.2.26 what is this? for (SceneryModule module : worldModules) {
            module.extendMapData(osmData.source, mapData, converter);
        }*/

        // step by step approach istead of previous "all-in-one".
        //  for (MapWay mapWay : mapData.getMapWays()) {
        // 1 Scenery Objekte erstellen. WayConnector werden hier auch schon erstellt.
        //     if (mapWay.getOsmId() == osmwayid) {
        //try {
        /*sceneryMesh.sceneryObjects.objects.addAll*/
        //20.3.26 osmElementService.process(mapWay,                           SceneryModule.getRelevant(worldModules, mapWay), terrainMesh, sceneryContext, OsmClassifier.LOD_BASIC);
        processMapData(mapData, meshName, osmwayid, MeshService.buildMeshServiceFacade());

        /* 26.3.24 no longer here
        //4.8.18 mal vor der Elevation, weil scheinbar bei der Trianglation die z-Coordinaten durcheinander kommen können. Schon skuril!
        //24.4.19: Und weil bei der Triangulation noch Coordinates entstehen.
        Phase.updatePhase(Phase.TRIANGULATION);
        sceneryMesh.triangulateAndTexturize();*/

        //24.4.19: Der ganze Elekram erst jetzt, wenn alle Polygone final sind. Elegroups gibt es aber schon lange.
        Phase.updatePhase(Phase.ELEVATION);
        SceneryMesh.connectElevationGroups(sceneryMesh.sceneryObjects.objects, terrainMesh);

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

         /* 16.2.26 not sure when it should be done if (elevationProvidername != null && targetBounds != null) {
            log.info("Elevation Provider isType " + elevationProvidername);
            String classname = elevationProvidername;// + ".class";
            try {
                Class clazz = Class.forName(classname);
                elevationProvider = (ElevationProvider) clazz.newInstance();

                ElevationMap.init(elevationProvider, (GridCellBounds) targetBounds, mapProjection);
                //fixElevationGroups(mapData, /*eleData* /elevationProvider, compositeConfiguration);
                //later sceneryMesh.fixElevationGroups(elevationProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.info("No Elevation provider or nor grid. Elevation not calculated");
        }*/


        if (ElevationMap.hasInstance()) {
            //26.9.18: einfach alles cathen ist doch wohl doof.
            //try {
            sceneryMesh.calculateElevations(elevationProvider, sceneryContext);
            //} catch (Exception e) {
            //    e.printStackTrace();
            //}
        }

        /* create terrain */
        //updatePhase(Phase.TERRAIN); //TODO this phase may be obsolete

        /* supply results to targets and caller */
        //updatePhase(Phase.FINISHED);


        return new OsmService.Results(null/*mapProjection*/, mapData, null/*eleData*/, sceneryMesh);

    }

    public OsmService.Results populateMesh(String meshName, MapData mapData) throws IOException, SceneryConversionFacade.BoundingBoxSizeException, MeshInconsistencyException {
        return populateMesh(meshName, mapData, null);
    }

    /**
     * Apply defined modules to the map data and create scenery objects.
     * Passing a single osmwayid is intended for test purposes only.
     */
    private void processMapData(MapData mapData, String meshName, Long osmwayid, MeshServiceFacade meshService) throws MeshInconsistencyException {
        WayModule wayModule = new WayModule(meshService, mapData.projection, meshName);
        SurfaceAreaModule lakeModule = new SurfaceAreaModule(meshService, mapData.projection, meshName);
        WayModule highwayModule = new WayModule(meshService, mapData.projection, meshName);

        List<MapWay> effectiveWays = mapData.getMapWays();
        if (osmwayid != null) {
            effectiveWays = effectiveWays.stream().filter(w -> w.getOsmId() == osmwayid).collect(Collectors.toUnmodifiableList());
        }
        for (MapWay mapWay : effectiveWays) {
            for (MapWaySegment2 segment : mapWay.segment2s) {
                wayModule.applyTo(segment, SceneryContext.getInstance());
            }
        }
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


    /**
     * uses OSM data and an terrain elevation data (usually from an external
     * source) to calculate elevations for all { EleConnector}s of the
     * {@link WorldObject}s
     */
    private void calculateElevations(MapData mapData,
                                     TerrainElevationData eleData, Configuration config) {

        final TerrainInterpolator interpolator = null;
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

