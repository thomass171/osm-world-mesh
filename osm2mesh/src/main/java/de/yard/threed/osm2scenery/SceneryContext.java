package de.yard.threed.osm2scenery;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.graph.Graph;
import de.yard.threed.graph.GraphOrientation;
import de.yard.threed.osm2scenery.modules.BridgeModule;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.polygon20.OsmWay;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2world.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.ROAD;

/**
 * Soll nur Datencontainer sein, ohne Logik(?).
 * Manches koennte auch in Config (wie GridCell). Hier ist aber auch ganz sinnig.
 * Die Klasse könnte auch SceneryData heissen. Aber wie grenzt sich das zu SceneryMesh ab?
 *
 * 12.7.19: Dies hier ist Singleton und static overall available, z.B. der WarningCounter
 * 1.5.24: By 2024 design SceneryContext is still used as a container for high level terrainmesh wrapper. These
 * are retrieved from TerrainMesh. "warnings","unknowncoordinates","overlapping*" however are deprecated(?)
 * <p>
 * Created on 26.07.18.
 */
public class SceneryContext {
    public static SceneryContext instance;
    // Registry for warnings that shouldn't occur and should be avoided (might differ from log level warning).
    public List<String> warnings = new ArrayList<>();
    public List<Coordinate> unknowncoordinates = new ArrayList<>();
    public int overlappingways;
    public int overlappingterrain = 0;
    public int overlappingTerrainWithSupplements = 0;
    //das ist aber nur der Zaehler fuer versuchte und gescheiterte!
    public int unresolvedoverlaps = 0;
    public int errorCounter=0;

    /**
     * 3.4.24 Non singleton constructor.
     * Needs to be filled from DB.
     */
    public SceneryContext() {
        roadgraph = new Graph(GraphOrientation.buildForZ0());
        taxiwaygraph = new Graph(GraphOrientation.buildForZ0());
        railwaygraph = new Graph(GraphOrientation.buildForZ0());
        rivergraph = new Graph(GraphOrientation.buildForZ0());
    }

    public static void init(MapData mapdata) {
        instance = new SceneryContext();
        instance.mapdata=mapdata;
        //21.7.18: Graph ist von vornherein in z0
        instance.roadgraph = new Graph(GraphOrientation.buildForZ0());
        instance.taxiwaygraph = new Graph(GraphOrientation.buildForZ0());
        instance.railwaygraph = new Graph(GraphOrientation.buildForZ0());
        instance.rivergraph = new Graph(GraphOrientation.buildForZ0());
    }

    /**
     * For now read it from osm ways in DB
     */
    public static SceneryContext buildFromDatabase(List<OsmWay> osmWays) {
        SceneryContext sceneryContext = new SceneryContext();
        //21.7.18: Graph in z0
        sceneryContext.roadgraph = new Graph(GraphOrientation.buildForZ0());
        sceneryContext.taxiwaygraph = new Graph(GraphOrientation.buildForZ0());
        sceneryContext.railwaygraph = new Graph(GraphOrientation.buildForZ0());
        sceneryContext.rivergraph = new Graph(GraphOrientation.buildForZ0());
        for (OsmWay osmWay:osmWays){
           sceneryContext.highways.put(osmWay.getOsmId(), new SceneryWayObject(osmWay));
        }
        return sceneryContext;
    }

    private MapData mapdata;
    //21.7.18: Graph ist von vornherein in z0
    //taxiwaygraph enthaelt auch runway
    private Graph roadgraph, taxiwaygraph, railwaygraph, rivergraph;
    //osmWay->Bridge
    public Map<Long, BridgeModule.Bridge> bridges = new HashMap<>();
    //Enthaelt auch die Road Bestandteile einer Bridge
    //umbenannt roads->highways. Key is osm way id
    //7.5.24: Should we really use HighwayModule.Highway? Give SceneryWayObject a chance
    public Map<Long, SceneryWayObject/*HighwayModule.Highway*/> highways = new HashMap<>();
    //MapNode->alle Ways dazu. Von einem Way werden aber nur Start/End registriert.
    //24.8.18: die logische Trennung hatte was statt alles in einer map, evtl. ueber Category?
    public WayMap wayMap = new WayMap();
    //public Map<Long, List<SceneryWayObject>> riverMap = new HashMap<>();
    //public Map<Long, List<SceneryWayObject>> railwayMap = new HashMap<>();


    public static SceneryContext getInstance() {
        return instance;
    }

    public static void clear() {
        instance=null;
    }

    public Graph getGraph(SceneryObject.Category category) {
        if (category == SceneryObject.Category.ROAD) {
            return roadgraph;
        } else if (category == SceneryObject.Category.TAXIWAY || category == SceneryObject.Category.RUNWAY) {
            return taxiwaygraph;
        } else if (category == SceneryObject.Category.RAILWAY) {
            return railwaygraph;
        } else if (category == SceneryObject.Category.RIVER) {
            return rivergraph;
        }
        throw new RuntimeException("no graph for " + category);

    }

    public MapData getMapdata(){
        return mapdata;
    }
}
