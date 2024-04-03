package de.yard.threed.osm2world;

import org.apache.commons.configuration2.Configuration;


/**
 * simple superclass for {@link WorldModule}s that stores a configuration set by
 *
 */
public abstract class ConfigurableWorldModule implements WorldModule {
    protected Configuration config;
	
	@Override
	public void setConfiguration(Configuration config) {
		this.config = config;
	}
	
	
}
