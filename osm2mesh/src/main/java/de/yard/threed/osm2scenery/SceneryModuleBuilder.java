package de.yard.threed.osm2scenery;

import de.yard.threed.osm2scenery.modules.SceneryModule;

@FunctionalInterface
public interface SceneryModuleBuilder {
    SceneryModule buildModule(MeshServiceFacade meshServiceFacade);
}
