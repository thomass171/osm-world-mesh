package de.yard.threed.osm2mesh.testutils;

import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.MeshServiceFacade;
import de.yard.threed.osm2scenery.polygon20.*;
import de.yard.threed.osm2scenery.scenery.LandClass;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MapWaySegmentAtConnector;

import java.util.ArrayList;
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
    public TerrainMesh addWay(String meshName, long osmWayId, Pair<GeoCoordinate, GeoCoordinate> fromConnector, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine, Pair<GeoCoordinate, GeoCoordinate> toConnector, int lanes, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException {

        List<GeoCoordinate> polyList = new ArrayList<>();

        JtsUtil.processWayOutlines(leftLine, rightLine, (geoCoordinate) -> {
            polyList.add(geoCoordinate);
        });
        polyList.add(polyList.get(0)); // close polygon

        polygons.add(new MeshPolygonMock(osmWayId, MeshPolygonType.WAY, landClass, polyList));
        return TerrainMesh.init(gridCellBounds, points, polygons);
    }

    @Override
    public List<MeshPolygon> findAreaIntersectionsOfWay(String meshName, List<GeoCoordinate> leftLine, List<GeoCoordinate> rightLine) throws MeshInconsistencyException {
        List<GeoCoordinate> outline = new ArrayList<>();
        JtsUtil.processWayOutlines(leftLine, rightLine, outline::add);
        outline.add(outline.get(0)); // close polygon
        return findAreaIntersectionsOfArea(new GeoPolygon(outline));
    }

    @Override
    public List<MeshPolygon> findAreaIntersectionsOfArea(String meshName, List<GeoCoordinate> polygon) throws MeshInconsistencyException {
        return findAreaIntersectionsOfArea(new GeoPolygon(polygon));
    }

    @Override
    public List<MeshPolygon> findWayIntersectionsOfArea(String meshName, List<GeoCoordinate> polygon) throws MeshInconsistencyException {
        return findIntersections(new GeoPolygon(polygon), List.of(MeshPolygonType.WAY, MeshPolygonType.CONNECTOR));
    }

    private List<MeshPolygon> findAreaIntersectionsOfArea(GeoPolygon wayPolygon) {
        return findIntersections(wayPolygon, List.of(MeshPolygonType.AREA));
    }

    private List<MeshPolygon> findIntersections(GeoPolygon wayPolygon, List<MeshPolygonType> types) {
        List<MeshPolygon> result = new ArrayList<>();
        for (MeshPolygon p : polygons) {
            if (types.contains(p.getType()) && Boolean.TRUE.equals(JtsUtil.overlaps(wayPolygon, p.getGeoPolygon()))) {
                result.add(p);
            }
        }
        return result;
    }

    @Override
    public TerrainMesh addConnector(String meshName, long osmNodeId, List<GeoCoordinate> polygon, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException {

        polygons.add(new MeshPolygonMock(osmNodeId, MeshPolygonType.CONNECTOR, landClass, polygon, wayAttachPoints));
        return TerrainMesh.init(gridCellBounds, points, polygons);
    }

    @Override
    public TerrainMesh loadMesh(String meshName) {
        // ignore name for now

        TerrainMesh terrainMesh = TerrainMesh.init(gridCellBounds, points, polygons);
        //20.8.26 terrainMesh.meshService = this;
        terrainMesh.meshName = meshName;
        return terrainMesh;
    }

    @Override
    public MeshFailure addFailure(String meshName, String message, String sourceref, GeoPolygon polygon, String svg) {
        return null;
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

    @Override
    public TerrainMesh addArea(String meshName, long osmId, MeshPolygonType type, List<List<GeoCoordinate>> polygonList, LandClass landClass, List<MeshPolygon> areasToAdjust) throws MeshInconsistencyException {
        for (List<GeoCoordinate> geoCoordinates : polygonList) {
            List<GeoCoordinate> polyList = new ArrayList<>(geoCoordinates);
            polyList.add(polyList.get(0)); // close polygon

            polygons.add(new MeshPolygonMock(osmId, type, landClass, polyList));
        }
        return TerrainMesh.init(gridCellBounds, points, polygons);
    }
}
