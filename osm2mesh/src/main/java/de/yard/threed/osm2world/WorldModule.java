package de.yard.threed.osm2world;

import org.apache.commons.configuration2.Configuration;

public interface WorldModule {
	
	/**
	 * provides a {@link Configuration} that can be used to control aspects
	 * of a WorldModule's behavior.
	 * 
	 * This is guaranteed to be called before {@link #applyTo(MapData)},
	 * but not all parameters might be explicitly set in the configuration,
	 * so defaults need to be available.
	 */
	public void setConfiguration(Configuration config);
	
	/**
	 * adds {@link WorldObject}s to {@link MapElement}s
	 */
	public void applyTo(MapData mapData);
	
}
