package de.yard.threed.osm2world;



public interface WorldObject {
	
	/**
	 * returns the "primary" {@link MapElement} for this WorldObject;
	 * i.e. the one it is most strongly associated with.
	 * Can be null if there is no (clear) primary element for this feature.
	 */
	public MapElement getPrimaryMapElement();
	
	/**
	 * returns whether this feature is on, above or below the ground.
	 * This is relevant for elevation calculations,
	 * because the elevation of features o.t.g. is directly
	 * determined by terrain elevation data.
	 * Elevation of features above/below t.g. depends on elevation of
	 * features o.t.g. as well as other features above/below t.g.
	 */
	public GroundState getGroundState();
	
	/**
	 * returns all { EleConnector}s used by this WorldObject
	 */
	Iterable<O2WEleConnector> getEleConnectors();
	
	/**
	 * lets this object add constraints for the relative elevations of its
	 * { EleConnector}s. Called after {@link #getEleConnectors()}.
	 */
	public void defineEleConstraints(EleConstraintEnforcer enforcer);
	
}