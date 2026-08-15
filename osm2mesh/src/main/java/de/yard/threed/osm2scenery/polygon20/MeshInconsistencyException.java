package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;

public class MeshInconsistencyException extends Exception {
    public Polygon invalidPolygon;
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
        MeshInconsistencyException e = new MeshInconsistencyException(msg + " for " + osmId + ": " + invalidPolygon);
        e.invalidPolygon = invalidPolygon;
        e.osmId = osmId;
        return e;
    }
}
