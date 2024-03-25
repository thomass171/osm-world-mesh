package de.yard.threed.osm2world;

import java.util.Collection;

/**
 * a closed shape, covering a non-zero area, that is not self-intersecting.
 * For this kind of shape, the vertices describe the area's boundary.
 */
public interface SimpleClosedShapeXZ extends ShapeXZ {
	
	/**
	 * returns a decomposition of the shape into triangles.
	 * For some shapes (e.g. circles), this may be an approximation.
	 */
	public Collection<TriangleXZ> getTriangulation();
	
}
