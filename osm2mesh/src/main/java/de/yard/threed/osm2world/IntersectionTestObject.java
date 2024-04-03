package de.yard.threed.osm2world;

/**
 * object which can be inserted into data structures
 * that speed up intersection tests
 */
public interface IntersectionTestObject {

	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ();
	
}
