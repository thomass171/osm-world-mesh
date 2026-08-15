package de.yard.threed.osm2mesh.testutils;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2graph.osm.CoordinateList;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.MeshServiceFacade;
import de.yard.threed.osm2scenery.polygon20.*;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MapWaySegmentAtConnector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * No full mock, which is too complex.
 */
public class MeshServiceMock implements MeshServiceFacade {
    GridCellBounds gridCellBounds;
    List<MeshNode> points = new ArrayList<>();
    public List<MeshPolygon> polygons = new ArrayList<>();

    public MeshServiceMock(GridCellBounds gridCellBounds) {
        this.gridCellBounds = gridCellBounds;
    }

    @Override
    public void createMesh(String meshName, List<GeoCoordinate> boundary) {

    }

    @Override
    public TerrainMesh addWay(String meshName, long osmWayId, Pair<GeoCoordinate, GeoCoordinate> fromConnector, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine, Pair<GeoCoordinate, GeoCoordinate> toConnector, int lanes) throws OsmProcessException, MeshInconsistencyException {

        List<Pair<GeoCoordinate, Long>> polyList = new ArrayList<>();

        JtsUtil.processWayOutlines(leftLine, rightLine, (geoCoordinate) -> {
            polyList.add(new Pair<>(geoCoordinate, 0L));
        });
        polyList.add(polyList.get(0)); // close polygon

        polygons.add(new MeshPolygonMock(osmWayId, MeshPolygonType.WAY, polyList));
        return TerrainMesh.init(gridCellBounds, points, polygons);
    }

    @Override
    public TerrainMesh addConnector(String meshName, long osmNodeId, List<Pair<GeoCoordinate, Long>> polygon, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints) throws MeshInconsistencyException {

        polygons.add(new MeshPolygonMock(osmNodeId, MeshPolygonType.CONNECTOR, polygon, wayAttachPoints));
        return TerrainMesh.init(gridCellBounds, points, polygons);
    }

    @Override
    public TerrainMesh loadMesh(String meshName) {
        // ignore name for now

        TerrainMesh terrainMesh = TerrainMesh.init(gridCellBounds, points, polygons);
        terrainMesh.meshService = this;
        terrainMesh.meshName = meshName;
        return terrainMesh;
    }

    @Override
    public void addFailure(String meshName, String message, String sourceref, GeoPolygon polygon) {

    }

    @Override
    public MeshPolygon getConnector(long osmNodeId) {
        for (MeshPolygon p : polygons) {
            //if (p instanceof MeshWayConnector meshWayConnector) {
                if (p.getOsmId() == osmNodeId) {
                    return p;
                }
           // }
        }
        return null;
    }
}
