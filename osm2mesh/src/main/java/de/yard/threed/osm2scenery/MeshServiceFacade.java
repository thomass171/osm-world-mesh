package de.yard.threed.osm2scenery;

import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2scenery.polygon20.*;
import de.yard.threed.osm2scenery.scenery.LandClass;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MapWaySegmentAtConnector;

import java.util.List;
import java.util.Map;

/**
 * Only temporary until we resolve package dependency issues
 * No projected coordinates here, only unprojected geocoordinates.
 */
public interface MeshServiceFacade {

    void createMesh(String meshName, List<GeoCoordinate> boundary);

    /**
     * @param areasToAdjust the AREA polygons the way overlaps (see findAreaIntersections()). They need to be adjusted
     *                      so that there is no overlap with the new way. Empty if there are none, never null.
     */
    TerrainMesh addWay(String meshName, long osmWayId, Pair<GeoCoordinate, GeoCoordinate> fromConnector, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine, Pair<GeoCoordinate, GeoCoordinate> toConnector, int lanes, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException;

    /**
     * @param meshName
     * @param osmNodeId
     * @param polygon         should be closed
     * @param wayAttachPoints
     * @param landClass       optional, might be null
     * @param areasToAdjust   like in addWay()
     * @return
     * @throws MeshInconsistencyException
     */
    TerrainMesh addConnector(String meshName, long osmNodeId, List<GeoCoordinate> polygon, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException;

    /**
     * Connector at the end of a bridge (polygon type BRIDGE_CONNECTOR). Parameter like addConnector().
     * For now the implementation is the same as for addConnector(), but this might change in the future.
     */
    TerrainMesh addBridgeConnector(String meshName, long osmNodeId, List<GeoCoordinate> polygon, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException;

    TerrainMesh loadMesh(String meshName) throws MeshInconsistencyException;

    /**
     * Instead of just saving a specific polygon it seems more flexible and helpful to also save a SVG
     */
    MeshFailure addFailure(String meshName, String message, String sourceref, GeoPolygon polygon, String svg);

    /**
     * Returns null if not found
     */
    /*MeshWayConnector*/ MeshPolygon getConnector(long osmNodeId);

    /**
     * The most generic use case
     * 16.11.26: Adds a list of polygons instead of a single polygon, because an area might be split into several polygons
     * by clipping it to existing ways (see findWayIntersectionsOfArea()). Each polygon is stored as a separate
     * polygon with the same osmId.
     *
     * @param areasToAdjust like in addWay(). They are adjusted to all of the polygons.
     */
    TerrainMesh addArea(String meshName, long osmId, MeshPolygonType type, List<List<GeoCoordinate>> polygons, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException;

    /**
     * Returns the polygons of type AREA that overlap the polygon a way with these outlines would get in addWay().
     * Areas only touching the way are not included. The mesh isn't modified.
     */
    List<MeshPolygon> findAreaIntersectionsOfWay(String meshName, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine) throws MeshInconsistencyException;

    /**
     * Like findAreaIntersections() for a way, but for an area (closed, like in addConnector()).
     */
    List<MeshPolygon> findAreaIntersectionsOfArea(String meshName, List<GeoCoordinate> polygon) throws MeshInconsistencyException;

    /**
     * Like findAreaIntersectionsOfArea(), but returns the polygons of type WAY and CONNECTOR. Unlike areas these
     * must not be split, so a new area has to be clipped to them instead.
     */
    List<MeshPolygon> findWayIntersectionsOfArea(String meshName, List<GeoCoordinate> polygon) throws MeshInconsistencyException;

}