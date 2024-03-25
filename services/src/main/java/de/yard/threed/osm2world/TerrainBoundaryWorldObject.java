package de.yard.threed.osm2world;

/**
 * area covered by representations with this interface will not be
 * covered by terrain if the representation is on the ground
 * (according to {@link WorldObject#getGroundState()}).
 */
public interface TerrainBoundaryWorldObject
	extends WorldObjectWithOutline, IntersectionTestObject {
	
	//TODO: multipolygon support -> requires retrieving the inner polygons
	// (preferably, the supertype WorldObjectWithOutline should be modified)
	
	/**
	 * returns the axis aligned bounding box that contains the entire object
	 */
	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ();
	
}
