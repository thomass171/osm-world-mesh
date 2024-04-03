package de.yard.threed.osm2world;

public interface WaySegmentWorldObject extends WorldObject {

	@Override
	public MapWaySegment getPrimaryMapElement();
	
	/**
	 * returns the start position.
	 * Might be different from {@link MapWaySegment}'s start position;
	 * as node features such as crossings require space, too.
	 */
	public VectorXZ getStartPosition();
	
	/**
	 * returns the end position.
	 * See {@link #getStartPosition()} for details.
	 */
	public VectorXZ getEndPosition();
		
}
