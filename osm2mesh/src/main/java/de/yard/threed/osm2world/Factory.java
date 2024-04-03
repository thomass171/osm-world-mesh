package de.yard.threed.osm2world;

/**
 * Creates instances of another object type.
 *
 * @param <T>  the type of objects created by this factory
 */
public interface Factory<T> {
	
	/** creates a new instance */
	T make();
	
}
