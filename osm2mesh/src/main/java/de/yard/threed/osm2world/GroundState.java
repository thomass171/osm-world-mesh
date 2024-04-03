package de.yard.threed.osm2world;

public enum GroundState {
	
	ON, ABOVE, BELOW;
	
	public boolean isHigherThan(GroundState other) {
		return this == ABOVE && other != ABOVE
			|| this == ON && other == BELOW;
	}
	
}