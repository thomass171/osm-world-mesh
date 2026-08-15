package de.yard.threed.osm2scenery.util;

import de.yard.threed.osm2scenery.modules.OsmClassifier;

@FunctionalInterface
public interface Selector<T> {

    boolean matches(T element);


}
