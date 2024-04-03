package de.yard.threed.osm2world;

import de.yard.threed.core.LatLon;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;


/**
 * abstract map projection superclass with configurable coordinate origin
 */
public abstract class O2WOriginMapProjection implements O2WMapProjection {
	
	/**
	 * the origin.
	 * 
	 * TODO make this final when future Java versions offer a replacement for
	 * current factories i
	 */
	protected LatLon origin;
	
	@Override
	public LatLon getOrigin() {
		return origin;
	}
	
	/**
	 * sets a new origin.
	 * 
	 * Calling {@link #calcLat(org.osm2world.core.math.VectorXZ)},
	 * {@link #calcLon(org.osm2world.core.math.VectorXZ)} or
	 * {@link #calcPos(LatLon)} before the origin has been set
	 * will result in an {@link IllegalStateException}.
	 */
	public void setOrigin(LatLon origin) {
		this.origin = origin;
	}
	
	/**
	 * sets a new origin. It is placed at the center of the bounds,
	 * or else at the first node's coordinates.
	 * 
	 * @see #setOrigin(LatLon)
	 */
	public void setOrigin(OSMData osmData) {
		
		if (osmData.getBounds() != null && !osmData.getBounds().isEmpty()) {
			
			Bound firstBound = osmData.getBounds().iterator().next();
			
			setOrigin(LatLon.fromDegrees(
					(firstBound.getTop() + firstBound.getBottom()) / 2,
					(firstBound.getLeft() + firstBound.getRight()) / 2));
			
		} else {
			
			if (osmData.getNodes().isEmpty()) {
				throw new IllegalArgumentException(
						"OSM data must contain bounds or nodes");
			}
			
			OSMNode firstNode = osmData.getNodes().iterator().next();
			setOrigin(LatLon.fromDegrees(firstNode.lat, firstNode.lon));
			
		}
		
	}
	
}
