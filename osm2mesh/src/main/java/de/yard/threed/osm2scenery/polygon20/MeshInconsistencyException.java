package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;

import java.util.ArrayList;
import java.util.List;

public class MeshInconsistencyException extends Exception {
    // 14.10.26 Depending on where the exception occured polygons are projected of unprojected here
    private List<GeoPolygon> invalidGeoPolygons = new ArrayList<>();
    private List<Polygon> invalidPolygons = new ArrayList<>();
    public TerrainMesh terrainMesh;
    public Long osmId;

    public MeshInconsistencyException(String msg) {
        super(msg);
    }

    public MeshInconsistencyException(TerrainMesh terrainMesh, MeshInconsistencyException e) {
        super(e);
        this.terrainMesh = terrainMesh;
    }

    public static MeshInconsistencyException forInvalidPolygon(String msg, Long osmId, Polygon invalidPolygon) {
        return forInvalidPolygon(msg, osmId, List.of(invalidPolygon));
    }

    public static MeshInconsistencyException forInvalidPolygon(String msg, Long osmId, List<Polygon> invalidPolygons) {
        MeshInconsistencyException e = new MeshInconsistencyException(msg + " for " + osmId + ": " + invalidPolygons);
        e.invalidPolygons = invalidPolygons;
        e.osmId = osmId;
        return e;
    }

    public static MeshInconsistencyException forInvalidGeoPolygons(String msg, Long osmId, List<GeoPolygon> invalidPolygons) {
        MeshInconsistencyException e = new MeshInconsistencyException(msg + " for " + osmId + ": " + invalidPolygons);
        e.invalidGeoPolygons = invalidPolygons;
        e.osmId = osmId;
        return e;
    }

    public List<Polygon> getInvalidPolygons(SceneryProjection projection) throws MeshInconsistencyException {
        List<Polygon> result = new ArrayList<>();
        result.addAll(invalidPolygons);
        for (GeoPolygon gp:invalidGeoPolygons){
            result.add(JtsUtil.project(gp, projection));
        }
        return result;
    }
}
