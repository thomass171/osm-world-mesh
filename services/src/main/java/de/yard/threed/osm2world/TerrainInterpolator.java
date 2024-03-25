package de.yard.threed.osm2world;

import java.util.Collection;

/**
 * strategy for elevation interpolation from a set of known points
 */
public interface TerrainInterpolator {

	void setKnownSites(Collection<VectorXYZ> sites);
	
	VectorXYZ interpolateEle(VectorXZ pos);
	
}
