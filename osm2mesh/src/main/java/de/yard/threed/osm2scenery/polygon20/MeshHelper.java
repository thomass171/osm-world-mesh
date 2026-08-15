package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Pair;
import de.yard.threed.core.Util;
import de.yard.threed.engine.Mesh;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import de.yard.threed.osm2scenery.util.GeoCoordinatePair;

import java.util.ArrayList;
import java.util.List;

public class MeshHelper {

    public static CoordinatePair projectCoordinatePair(GeoCoordinatePair geoCoordinatePair, SceneryProjection projection) {
        return new CoordinatePair(projection.project(geoCoordinatePair.getFirst()), projection.project(geoCoordinatePair.getSecond()));
    }

    public static CoordinatePair projectNodePair(Pair<MeshNode, MeshNode> nodePair, SceneryProjection projection) {
        return new CoordinatePair(projection.project(nodePair.getFirst().getGeoCoordinate()), projection.project(nodePair.getSecond().getGeoCoordinate()));
    }

    public static Coordinate projectNode(MeshNode mn, SceneryProjection projection) {
        return projection.project(mn.getGeoCoordinate());
    }

    public static Polygon projectPolygon(MeshPolygon mp, SceneryProjection projection) {
        List<Coordinate> coordinateList = new ArrayList<>();
        for (MeshNode n : mp.getNodesSortedByIndex()) {
            coordinateList.add(projectNode(n, projection));
        }
        return JtsUtil.createPolygon(coordinateList);
    }

}
