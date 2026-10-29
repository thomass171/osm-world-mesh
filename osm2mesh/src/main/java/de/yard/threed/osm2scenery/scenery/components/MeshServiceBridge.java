package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.MeshServiceFacade;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.polygon20.MeshPolygonType;
import de.yard.threed.osm2scenery.scenery.*;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import de.yard.threed.osm2world.MapWaySegmentAtConnector;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 28.10.26: Was TerrainMeshAdder once. Now it is the bridge between projection based scenery objects
 * and the unprojected mesh service.
 */
@Slf4j
public class MeshServiceBridge implements SceneryObjectComponent {
    // 19.8.26 Moved here from TerrainMesh
    String meshName;
    public MeshServiceFacade meshService;
    public SceneryProjection projection;
    // Now disabled for 2026 DB persist
    // public void addToTerrainMesh(AbstractArea[] areas, TerrainMesh tm) throws OsmProcessException, MeshInconsistencyException;
    //void addToTerrainMesh(MeshServiceFacade meshService) throws OsmProcessException, MeshInconsistencyException;

    public MeshServiceBridge(String meshName, MeshServiceFacade meshService, SceneryProjection projection) {
        this.meshName = meshName;
        this.meshService = meshService;
        this.projection = projection;
    }

    /**
     * 5.4.24: Three additional register instead of registerLine, which isn't ready for polygons.
     * lanes is used leter to detect ways for triangulateAndTexturize
     * connector might be null, otherwise the connecting part must have been created before.
     * 6.4.24: Well, maybe it's easier to keep existing registerLine for a while?
     * 24.8.26: No longer returns a value to avoid confusion.
     * 28.10.26: areasToAdjust are the areas the way overlaps (see findAreaIntersections()). They need to be adjusted
     * for avoiding overlaps.
     *
     * @return
     */
    public void registerWay(long osmWayId, Pair<Coordinate, Coordinate> fromConnector, List<Coordinate> leftLine, List<Coordinate> rightLine, Pair<Coordinate, Coordinate> toConnector, int lanes, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException {

        meshService.addWay(meshName, osmWayId, JtsUtil.unproject(fromConnector, projection), JtsUtil.unproject(leftLine, projection), JtsUtil.unproject(rightLine, projection), JtsUtil.unproject(toConnector, projection), lanes, landClass, areasToAdjust);

    }

    /**
     * Analog to registerWay. Returns the AREA polygons a way with these outlines would overlap.
     */
    public List<MeshPolygon> findAreaIntersections(List<Coordinate> leftLine, List<Coordinate> rightLine) throws MeshInconsistencyException {
        return meshService.findAreaIntersectionsOfWay(meshName, JtsUtil.unproject(leftLine, projection), JtsUtil.unproject(rightLine, projection));
    }

    /**
     * Analog to registerConnector (so GeoCoordinate instead of Coordinate). Returns the AREA polygons a connector
     * with this polygon would overlap.
     */
    public List<MeshPolygon> findAreaIntersections(List<Pair<GeoCoordinate, Long>> line) throws MeshInconsistencyException {
        return meshService.findAreaIntersectionsOfArea(meshName, line);
    }

    /**
     * Analog to registerWay. "pair.second" is attached way id
     * 24.8.26: No longer returns a value to avoid confusion.
     * 1.9.26 TODO should expect Coordinate like registerWay and registerArea, not GeoCoordinate. But for now it is easier to keep it like this.
     * 28.10.26: areasToAdjust like in registerWay().
     */
    public void/*MeshWayConnector*/ registerConnector(long osmNodeId, List<Pair<GeoCoordinate, Long>> line, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException {
        TerrainMesh tm = meshService.addConnector(meshName, osmNodeId, line, wayAttachPoints, landClass, areasToAdjust);
        //24.8.26 return tm.getConnector(osmNodeId);

    }

    /**
     * Analog to registerWay.
     */
    public void/*MeshWayConnector*/ registerArea(long osmNodeId, MeshPolygonType type, List<Coordinate> coordinates, LandClass landClass) throws MeshInconsistencyException {
        TerrainMesh tm = meshService.addArea(meshName, osmNodeId, type, JtsUtil.unproject(coordinates, projection), landClass);

    }

    /**
     * 28.10.26: Moved here from WayTerrainMeshAdder.
     * areasToAdjust: see registerWay().
     */
    public void persistWay(long osmWayId, Pair<List<Coordinate>, List<Coordinate>> outlines, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException {



        registerWay(osmWayId, null, outlines.getFirst(), outlines.getSecond(), null, 2, null, areasToAdjust);


        // Also consider dead end. Connect left and right lines from above.
        /*19.8.26 if (tm.isPreDbStyle()) {
            Util.nomore();
                    /*26.2.26 if (sceneryWayObject.startMode == SceneryWayObject.WayOuterMode.DEADEND) {
                CoordinatePair p = wayArea.getStartPair(tm)[0];
                tm.registerLine(JtsUtil.toList(p.left(), p.right()), areas[0], null, false, false);
                tm.addKnownTwoEdger(p.left());
                tm.addKnownTwoEdger(p.right());
            }
            if (sceneryWayObject.endMode == SceneryWayObject.WayOuterMode.DEADEND) {
                CoordinatePair p = wayArea.getEndPair()[0];
                tm.registerLine(JtsUtil.toList(p.left(), p.right()), null, areas[0], false, false);
                tm.addKnownTwoEdger(p.left());
                tm.addKnownTwoEdger(p.right());
            }* /
        }*/
        //  lazy cut isType already registerd in GridBounds, but needs left/right

        /*kann man hier nicht pruefen, weil Connector noch fehlen if (!TerrainMesh.getInstance().isValid(true)){
            log.error("invalid after adding way");
        }*/
    }


}
