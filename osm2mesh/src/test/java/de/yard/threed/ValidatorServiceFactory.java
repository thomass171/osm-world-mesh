package de.yard.threed;

import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2mesh.testutils.ValidatorServiceFacade;
import de.yard.threed.osm2scenery.MeshServiceFacade;

@FunctionalInterface
public interface ValidatorServiceFactory {
    ValidatorServiceFacade createService();
}
