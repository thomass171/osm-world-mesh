package de.yard.threed.osm2world;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import de.yard.threed.osm2graph.osm.SceneryProjection;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OSM2World's abstraction of {@link OSMData}, consists of {@link MapElement}s.
 * Initially contains only a slightly altered representation of OSM
 * map data. During later conversion steps, additional information is
 * added to the {@link MapElement}s.
 */
public class MapData {

    final List<MapNode> mapNodes;
    final List<MapWaySegment> mapWaySegments;
    final List<MapArea> mapAreas;
    List<MapWay> mapWays;
    public SceneryProjection projection;
    AxisAlignedBoundingBoxXZ fileBoundary;
    AxisAlignedBoundingBoxXZ dataBoundary;

    public MapData(List<MapNode> mapNodes, List<MapWaySegment> mapWaySegments,
                   List<MapArea> mapAreas, List<MapWay> mapWays, AxisAlignedBoundingBoxXZ fileBoundary,
                   SceneryProjection projection) {

        this.mapNodes = mapNodes;
        this.mapWaySegments = mapWaySegments;
        this.mapAreas = mapAreas;
        this.mapWays = mapWays;
        this.fileBoundary = fileBoundary;
        this.projection = projection;
        calculateDataBoundary();

    }

    /**
     * used for cloning
     */
    private MapData(int dummy, List<MapNode> mapNodes, List<MapWaySegment> mapWaySegments,
                    List<MapArea> mapAreas, List<MapWay> mapWays, AxisAlignedBoundingBoxXZ dataBoundary, SceneryProjection projection) {

        this.mapNodes = mapNodes;
        this.mapWaySegments = mapWaySegments;
        this.mapAreas = mapAreas;
        this.mapWays = mapWays;
        this.dataBoundary = dataBoundary;
        this.projection = projection;
    }

    private void calculateDataBoundary() {

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        if (fileBoundary != null) {
            // use the file boundary as the minimum extent of the data boundary
            minX = fileBoundary.minX;
            maxX = fileBoundary.maxX;
            minZ = fileBoundary.minZ;
            maxZ = fileBoundary.maxZ;
        }

        for (MapNode node : mapNodes) {
            final double nodeX = node.getPos().x;
            final double nodeZ = node.getPos().z;
            if (nodeX < minX) {
                minX = nodeX;
            }
            if (nodeX > maxX) {
                maxX = nodeX;
            }
            if (nodeZ < minZ) {
                minZ = nodeZ;
            }
            if (nodeZ > maxZ) {
                maxZ = nodeZ;
            }
        }

        dataBoundary = new AxisAlignedBoundingBoxXZ(minX, minZ, maxX, maxZ);

    }

    public Iterable<MapElement> getMapElements() {
        return Iterables.concat(mapNodes, mapWaySegments, mapAreas);
    }

    public Collection<MapArea> getMapAreas() {
        return mapAreas;
    }

    public Collection<MapWaySegment> getMapWaySegments() {
        return mapWaySegments;
    }

    public Collection<MapNode> getMapNodes() {
        return mapNodes;
    }

    /**
     * returns a rectangular boundary polygon from the minimum/maximum of
     * coordinates in the map data
     */
    public AxisAlignedBoundingBoxXZ getDataBoundary() {
        return dataBoundary;
    }

    /**
     * returns a boundary based on the bounds in the input file if available,
     * otherwise returns the same as {@link #getDataBoundary()}
     */
    public AxisAlignedBoundingBoxXZ getBoundary() {
        if (fileBoundary != null) {
            return fileBoundary;
        } else {
            return dataBoundary;
        }
    }

    /**
     * calculates the center from the {@link MapNode}s' positions
     */
    public VectorXZ getCenter() {

        int nodeCount = getMapNodes().size();

        double avgX = 0, avgZ = 0;
        for (MapNode node : getMapNodes()) {
            avgX += node.getPos().x / nodeCount; // need to divide before
            // numbers get too large
            avgZ += node.getPos().z / nodeCount;
        }

        return new VectorXZ(avgX, avgZ);

    }

    /**
     * returns all {@link WorldObject}s from elements in this data set.
     */
    public Iterable<WorldObject> getWorldObjects() {

        return Iterables.concat(
                Iterables.transform(getMapElements(),
                        new Function<MapElement, Iterable<? extends WorldObject>>() {
                            @Override
                            public Iterable<? extends WorldObject> apply(MapElement e) {
                                return e.getRepresentations();
                            }
                        }));

    }

    /**
     * returns all {@link WorldObject}s from elements in this data set
     * that are instances of a certain type.
     * Can be used, for example, to access all
     * { TerrainBoundaryWorldObject}s in the grid.
     */
    public <T> Iterable<T> getWorldObjects(Class<T> type) {
        return Iterables.filter(getWorldObjects(), type);
    }

    public List<MapWay> getMapWays() {
        return mapWays;
    }

    public List<MapWay> findMapWays(long osmId) {
        return mapWays.stream().filter(m -> m.getOsmWay().id == osmId).collect(Collectors.toList());
    }

    public MapWay findMapWay(long osmId) {
        List<MapWay> l = mapWays.stream().filter(m -> m.getOsmWay().id == osmId).collect(Collectors.toList());
        if (l.size() != 1) {
            throw new RuntimeException("not exactly one way found: inconsistent data");
        }
        return l.get(0);
    }

    public List<MapNode> findMapNodes(long osmId) {
        return mapNodes.stream().filter(n -> n.getOsmId() == osmId).collect(Collectors.toList());
    }

    public MapNode findMapNode(long osmId) {
        List<MapNode> l = mapNodes.stream().filter(n -> n.getOsmId() == osmId).collect(Collectors.toList());
        if (l.size() != 1) {
            throw new RuntimeException("not exactly one node found: inconsistent data");
        }
        return l.get(0);
    }

    public List<MapWaySegment2> findMapWaySegmentsAtNode(long nodeOsmId) {
        List<MapWaySegment2> result = new ArrayList<>();
        mapWays.forEach(w -> {
                    result.addAll(w.segment2s.stream().filter(s -> s.getStartNode().getOsmId() == nodeOsmId || s.getEndNode().getOsmId() == nodeOsmId).collect(Collectors.toList()));
                }
        );

        return result;
    }

    public MapData filterWay(long wayId) {
        return new MapData(9, mapNodes, mapWaySegments,
                mapAreas,
                mapWays.stream().filter(w -> w.getOsmId() == wayId).collect(Collectors.toUnmodifiableList()),
                dataBoundary, projection);
    }
}
