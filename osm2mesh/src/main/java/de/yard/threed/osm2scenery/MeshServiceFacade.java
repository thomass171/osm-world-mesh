package de.yard.threed.osm2scenery;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.LatLon;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2scenery.polygon20.GeoPolygon;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MapWaySegmentAtConnector;

import java.util.List;
import java.util.Map;

/**
 * Only temporary until we resolve package dependency issues
 */
public interface MeshServiceFacade {

    void createMesh(String meshName, List<GeoCoordinate> boundary);

    //   * from TerrainMesh.registerWay()
    TerrainMesh addWay(String meshName, long osmWayId, Pair<GeoCoordinate, GeoCoordinate> fromConnector, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine, Pair<GeoCoordinate, GeoCoordinate> toConnector, int lanes) throws OsmProcessException, MeshInconsistencyException;

    /**
     * @param meshName
     * @param osmNodeId
     * @param polygon         should be closed
     * @param wayAttachPoints
     * @return
     * @throws MeshInconsistencyException
     */
    TerrainMesh addConnector(String meshName, long osmNodeId, List<Pair<GeoCoordinate, Long>> polygon, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints) throws MeshInconsistencyException;

    TerrainMesh loadMesh(String meshName) throws MeshInconsistencyException;

    /**
     * Instead of just saving a specific polygon it seems more flexible and helpful to also save a SVG
     */
    void addFailure(String meshName, String message, String sourceref, GeoPolygon polygon, String svg);

    /**
     * Returns null if not found
     */
    /*MeshWayConnector*/ MeshPolygon getConnector(long osmNodeId);
}
