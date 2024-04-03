package de.yard.threed.osm2world;


import java.util.List;

/**
 * enforcer implementation that simply passes the interpolated terrain
 * elevations through, and does not actually enforce constraints.
 */
public class NoneEleConstraintEnforcer implements EleConstraintEnforcer {
	
	@Override
	public void addConnectors(Iterable<O2WEleConnector> connectors) {}
	
	@Override
	public void requireSameEle(O2WEleConnector c1, O2WEleConnector c2) {}
	
	@Override
	public void requireSameEle(Iterable<O2WEleConnector> cs) {}
	
	@Override
	public void requireVerticalDistance(ConstraintType type, double distance,
										O2WEleConnector upper, O2WEleConnector lower) {}
	
	@Override
	public void requireVerticalDistance(ConstraintType type, double distance,
										O2WEleConnector upper, O2WEleConnector base1, O2WEleConnector base2) {}
	
	@Override
	public void requireIncline(ConstraintType type, double incline,
			List<O2WEleConnector> cs) {}
	
	@Override
	public void requireSmoothness(
			O2WEleConnector from, O2WEleConnector via, O2WEleConnector to) {}
	
	@Override
	public void enforceConstraints() {}
	
}
