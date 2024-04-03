package de.yard.threed.osm2graph.backup;


import org.apache.commons.configuration2.Configuration;

import java.util.List;

/**
 * provides an easy way to call all steps of the conversion process
 * in the correct order
 * <p>
 * Hat den lgeichen Namen und Zweck wie in OSM2WOrld, arbeitet aber anders.
 */
@Deprecated
public class ConversionFacade {

 

    /**
     * all results of a conversion run
     */
    public static final class Results {

  /*      private final MapProjection mapProjection;
        private final MapData mapData;
        public final MyMapData mymapData;
        private final TerrainElevationData eleData;
        public RenderData renderdata;

        public Results(MapProjection mapProjection, MapData mapData, TerrainElevationData eleData, List<GridModule.Road> roads) {
            this.mapProjection = mapProjection;
            this.mapData = mapData;
            this.eleData = eleData;
            this.mymapData = null;
            if (roads != null){
                renderdata=new RenderData();
                renderdata.roads=roads;
            }
        }

        public Results(MapProjection mapProjection, MyMapData mymapData, TerrainElevationData eleData) {
            this.mapProjection = mapProjection;
            this.mymapData = mymapData;
            this.mapData = null;
            this.eleData = eleData;
        }

        public MapProjection getMapProjection() {
            return mapProjection;
        }

        public MapData getMapData() {
            return mapData;
        }

        public TerrainElevationData getEleData() {
            return eleData;
        }
*/
        /**
         * collects and returns all representations that implement a
         * renderableType, including terrain.
         * Convenience method.
         */
        /*public <R extends Renderable> Collection<R> getRenderables(Class<R> renderableType) {
            return getRenderables(renderableType, true, true);
        }*/

        /**
         * @see #getRenderables(Class)
         */
        /*public <R extends Renderable> Collection<R> getRenderables(
                Class<R> renderableType, boolean includeGrid, boolean includeTerrain) {

            //TODO make use of or drop includeTerrain

            Collection<R> representations = new ArrayList<R>();

            if (includeGrid) {
                for (R r : mapData.getWorldObjects(renderableType)) {
                    representations.add(r);
                }
            }

            return representations;

        }*/

    }

   /* private List<WorldModule> worldModules;
    private MapData mapData;
    public MetricMapProjection prj;
    Configuration config;
    /*public ConversionFacade(OSMData osmData, Configuration conf) throws IOException {
        config=conf;

        Double maxBoundingBoxDegrees = config.getDouble("maxBoundingBoxDegrees", null);
        if (maxBoundingBoxDegrees != null) {
            for (Bound bound : osmData.getBounds()) {
                if (bound.getTop() - bound.getBottom() > maxBoundingBoxDegrees
                        || bound.getRight() - bound.getLeft() > maxBoundingBoxDegrees) {
                    throw new BoundingBoxSizeException(bound);
                }
            }
        }

        prj = new MetricMapProjection();//mapProjectionFactory.make();
        prj.setOrigin(osmData);
        
        OSMToMapDataConverter converter = new OSMToMapDataConverter(prj, conf);
        mapData = converter.createMapData(osmData);

        //worldModules=createDefaultModuleList();
        
    
        // aus Ways Roads machen
        //mapData = buildRoads(mapData);
    }*/

    /*private MetaData buildRoads(MetaData mapData) {
        for (WaySegmentSet way : mapData.roads.ways) {
            
            MyMapGraph waygraph = new MyMapGraph(way.way);
            GraphNode previousNode = null;
            for (OSMNode node : way.nodes) {
                MapNode nodemap = nodeMap.get(node);
                GraphNode n = waygraph.addNode(null, new Vector3((float) nodemap.getPos().getX(), 0, (float) nodemap.getPos().getZ()));
                if (previousNode != null) {
                    waygraph.connectNodes(previousNode, n);
                }
                previousNode = n;
            }
            mapWaySegs.add(waygraph);
        }
    }*/

    /**
     * generates a default list of modules for the conversion
     */
    /*private static final List<WorldModule> createDefaultModuleList() {

        return Arrays.asList((WorldModule)
                        new RoadModule(),
                // new RiverModule()
                /*new RailwayModule(),
                new BuildingModule(),
                new ParkingModule(),
                new TreeModule(),
                new StreetFurnitureModule(),
                new TrafficSignModule(),* /
                new WaterModule(),
                new GridModule()
                /*new PoolModule(),
                new GolfModule(),
                new CliffModule(),
                new BarrierModule(),
                new PowerModule(),
                new BridgeModule(),
                new TunnelModule(),
                new SurfaceAreaModule(),
                new InvisibleModule(),
                new SimpleForestModule()* /
        );

    }

    private Factory<? extends OriginMapProjection> mapProjectionFactory =
            new DefaultFactory<MetricMapProjection>(MetricMapProjection.class);

    private Factory<? extends TerrainInterpolator> terrainEleInterpolatorFactory =
            new DefaultFactory<LeastSquaresInterpolator>(LeastSquaresInterpolator.class);

    private Factory<? extends EleConstraintEnforcer> eleConstraintEnforcerFactory =
            new DefaultFactory<NoneEleConstraintEnforcer>(NoneEleConstraintEnforcer.class);
*/

    /**
     * sets the factory that will make {@link MapProjection}
     * instances during subsequent calls to
     * {@link #createRepresentations(OSMData, List, Configuration, List)}.
     *
     * @see DefaultFactory
     */
   /* public void setMapProjectionFactory(
            Factory<? extends OriginMapProjection> mapProjectionFactory) {
        this.mapProjectionFactory = mapProjectionFactory;
    }*/

    /**
     * sets the factory that will make {@link EleConstraintEnforcer}
     * instances during subsequent calls to
     * {@link #createRepresentations(OSMData, List, Configuration, List)}.
     *
     * @see DefaultFactory
     */
    /*public void setEleConstraintEnforcerFactory(
            Factory<? extends EleConstraintEnforcer> interpolatorFactory) {
        this.eleConstraintEnforcerFactory = interpolatorFactory;
    }*/

    /**
  

    

    
/*    public Results createRepresentations(/*OSMData osmData,
                                         List<WorldModule> worldModules, Configuration config,
                                         List/*<Target<?>>* / targets* /boolean buildgrid)
            /*throws IOException, BoundingBoxSizeException* / {
        
	
        updatePhase(Phase.MAP_DATA);

        //jetzt im Constructor
        //MetricMapProjection mapProjection = new MetricMapProjection();//mapProjectionFactory.make();
        //mapProjection.setOrigin(osmData);
        //prj = mapProjection;
        //OSMToMapDataConverter converter = new OSMToMapDataConverter(mapProjection, config);
        //MapData mapData = converter.createMapData(osmData);
        
	    updatePhase(Phase.REPRESENTATION);

        
        //24.5.18 immer neu
        //if (worldModules == null) {
            worldModules = createDefaultModuleList();
        //}

        Materials.configureMaterials(config);
        //this will cause problems if multiple conversions are run
        //at the same time, because global variables are being modified

        WorldCreator moduleManager =
                new WorldCreator(config, worldModules);
        moduleManager.addRepresentationsTo(mapData);
		
	    updatePhase(Phase.ELEVATION);

        String srtmDir = config.getString("srtmDir", null);
        TerrainElevationData eleData = null;

        if (srtmDir != null) {
            eleData = null;//new SRTMData(new File(srtmDir), mapProjection);
        }

        fixElevationGroups(mapData, eleData, config);
		
        
	    updatePhase(Phase.TERRAIN); //TODO this phase may be obsolete
				
	   updatePhase(Phase.FINISHED);

        boolean underground = config.getBoolean("renderUnderground", true);

     
       List<GridModule.Road> roads = null;
       if (buildgrid){
           roads = ((GridModule)getModule(GridModule.class)).roads;
       }
        return new Results(prj, mapData, null/*eleData* /,roads);

    }*/

   
    
    public static enum Phase {
        MAP_DATA,
        REPRESENTATION,
        ELEVATION,
        TERRAIN,
        FINISHED
    }


   

}
