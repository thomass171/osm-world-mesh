package de.yard.threed.osm2mesh.testutils;

import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2scenery.polygon20.GeoPolygon;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MapWaySegmentAtConnector;

import java.util.List;
import java.util.Map;

/**
 * Only temporary until we resolve package dependency issues
 *
 */
public interface ValidatorServiceFacade {

    void validateMesh(TerrainMesh terrainMesh, ExpectedMeshPolygon... expectedBoundary) throws MeshInconsistencyException ;
}
