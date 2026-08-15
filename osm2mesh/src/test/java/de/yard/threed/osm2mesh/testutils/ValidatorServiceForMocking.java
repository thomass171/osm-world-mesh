package de.yard.threed.osm2mesh.testutils;

import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;

import java.util.List;

public class ValidatorServiceForMocking implements ValidatorServiceFacade {

    @Override
    public void validateMesh(TerrainMesh terrainMesh, ExpectedMeshPolygon[] expectedBoundary) throws MeshInconsistencyException {

    }
}
