package de.yard.threed.osm2mesh.testutils;

import de.yard.threed.core.Degree;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2scenery.polygon20.*;
import de.yard.threed.osm2world.MapWaySegmentAtConnector;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MeshPolygonMock implements MeshPolygon {
    long osmId;
    MeshPolygonType meshPolygonType;
    List<Pair<GeoCoordinate, Long>> polygon;

    public MeshPolygonMock(long osmId, MeshPolygonType meshPolygonType, List<Pair<GeoCoordinate, Long>> polygon) {
        this.osmId = osmId;
        this.meshPolygonType = meshPolygonType;
        if (polygon == null) {
            throw new IllegalArgumentException("polygon must not be null");
        }
        this.polygon = polygon;
    }

    @Override
    public List<? extends MeshPolygonNode> getPolygonNodes() {
        List<MeshPolygonNode> result = new ArrayList<>();
        int index = 0;
        for (Pair<GeoCoordinate, Long> p : polygon) {
            MeshPolygonNode pn = new MeshPolygonNodeMock(index++, new MeshNodeMock(p.getFirst()));
            result.add(pn);
        }
        return result;
    }

    @Override
    public GeoPolygon getGeoPolygon() {
        return null;
    }

    @Override
    public Long getOsmId() {
        return osmId;
    }

    @Override
    public MeshPolygonType getType() {
        return meshPolygonType;
    }

    //private Map<Long, Pair<MeshNodeMock, MeshNodeMock>> wayAttachPoints = new HashMap<>();
    public Map<String, Pair<Integer, Integer>> rawWayAttachPoints = new HashMap<>();

    // from MeshWayConnectorMock
    public MeshPolygonMock(long osmId, MeshPolygonType meshPolygonType, List<Pair<GeoCoordinate, Long>> polygon, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints) {
        this(osmId, meshPolygonType, polygon);
/*        for (Pair<Long, Pair<Integer, Integer>> wap : wayAttachPoints) {
            this.wayAttachPoints.put(wap.getFirst(), wap.getSecond());
        }*/
        wayAttachPoints.forEach((k, v) -> {
            this.rawWayAttachPoints.put("" + k.getWayOsmId() + "-" + (int) k.getHeadingAtconnector().getDegree(), v);
        });


        //this.rawWayAttachPoints=wayAttachPoints;
    }

    /*11.8.26 now in interface @Override
    public Pair<? extends MeshNode, ? extends MeshNode> getAttachCoordinates(long wayOsmId, Degree heading/*MapWaySegmentKey key* /) {
        Pair<Integer, Integer> pairOfWay = getAttachPointsByDegree(wayOsmId, heading);
        return new Pair(this.getNodesSortedByIndex().get(pairOfWay.getFirst()), getNodesSortedByIndex().get(pairOfWay.getSecond()));
    }*/

    @Override
    public Pair<Integer, Integer> getAttachIndices(long wayOsmId, Degree heading) {
        Pair<Integer, Integer> pairOfWay = getAttachPointsByDegree(wayOsmId, heading);
        return pairOfWay;
    }

    private Pair<Integer, Integer> getAttachPointsByDegree(long wayOsmId, Degree heading) {
        Pair<Integer, Integer> result = rawWayAttachPoints.get("" + wayOsmId + "-" + (int) heading.getDegree());
        if (result == null) {
            log.warn("no attach points found for way " + wayOsmId + " and heading " + heading.getDegree() + ". Available: " + rawWayAttachPoints.keySet());
        }
        return result;
    }
}
