package de.yard.owm.services.osm;

import com.vividsolutions.jts.triangulate.ConstraintEnforcementException;
import de.yard.owm.services.persistence.OsmWayRepository;
import de.yard.owm.services.persistence.TerrainMeshManager;
import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.Phase;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryConversionFacade;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.elevation.ElevationMap;
import de.yard.threed.osm2scenery.modules.SceneryModule;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.InvalidGeometryException;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.O2WEleConnector;
import de.yard.threed.osm2world.OSMData;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Server based counterpart of SceneryConversionFacade
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

        SceneryContext sceneryContext = SceneryContext.buildFromDatabase(terrainMeshManager.findOsmWays());

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

        TerrainMesh terrainMesh = terrainMeshManager.loadTerrainMesh(targetBounds);

        //handle posible old instances.

        // Das braucht schon eine besondere Reihenfolge:
        //Evtl. MapData ergänzen
        for (SceneryModule module : worldModules) {
            module.extendMapData(osmData.source, mapData, converter);
        }
        // step by step approach istead of previous "all-in-one".
        for (MapWay mapWay : mapData.getMapWays()) {
            // 1 Scenery Objekte erstellen. WayConnector werden hier auch schon erstellt.
            try {
                sceneryMesh.sceneryObjects.objects.addAll(osmElementService.process(mapWay,
                        SceneryModule.getRelevant(worldModules, mapWay), terrainMesh, sceneryContext));
            } catch (OsmProcessException | MeshInconsistencyException e) {
                log.error("Adding way failed",e);
            }


        }

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
            sceneryMesh.calculateElevations(elevationProvider, sceneryContext);
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

