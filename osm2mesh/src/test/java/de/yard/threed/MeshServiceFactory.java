package de.yard.threed;

import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.MeshServiceFacade;

@FunctionalInterface
public interface MeshServiceFactory {
    MeshServiceFacade createMeshService(GridCellBounds gridCellBounds);
}
