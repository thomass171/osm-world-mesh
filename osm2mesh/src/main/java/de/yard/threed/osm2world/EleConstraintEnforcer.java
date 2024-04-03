package de.yard.threed.osm2world;


import java.util.List;


public interface EleConstraintEnforcer {
	
	/** whether a constraint requires a minimum, maximum or exact value */
	public static enum ConstraintType {
		MIN, MAX, EXACT
	}
	
	/**
	 * makes connectors known to this enforcer. Only these connectors can be
	 * used in constraints later on, and only they will be affected by
	 * {@link #enforceConstraints()}.
	 * 
	 * @param connectors  connectors with elevation values initially set to
	 *  terrain elevation at their xz position
	 */
	void addConnectors(Iterable<O2WEleConnector> connectors);
	
	/**
	 * requires two connectors to be at the same elevation
	 */
	public void requireSameEle(O2WEleConnector c1, O2WEleConnector c2);
	
	/**
	 * requires a number of connectors to be at the same elevation
	 */
	public void requireSameEle(Iterable<O2WEleConnector> cs);
	
	/**
	 * requires two connectors' elevations to differ by a given distance
	 */
	void requireVerticalDistance(ConstraintType type, double distance,
								 O2WEleConnector upper, O2WEleConnector lower);
	
	/**
	 * requires a connector to be a give distance above a line segment
	 * defined by two other connectors.
	 * 
	 * @param distance  distance, may be negative if 'upper' is actually below
	 */
	void requireVerticalDistance(ConstraintType type, double distance,
								 O2WEleConnector upper, O2WEleConnector base1, O2WEleConnector base2);
	
	/**
	 * requires an incline along a sequence of connectors.
	 * 
	 * @param incline  incline value,
	 *  negative values are inclines in opposite direction
	 */
	void requireIncline(ConstraintType type, double incline,
			List<O2WEleConnector> cs);
	
	/**
	 * requires that there is a "smooth" transition between two line segments
	 */
	void requireSmoothness(O2WEleConnector from, O2WEleConnector via, O2WEleConnector to);
	
	/**
	 * tries to enforce the previously added constraints
	 * on elevations of connectors that have been added using
	 * {@link #addConnectors(Iterable)}
	 */
	void enforceConstraints();

}
