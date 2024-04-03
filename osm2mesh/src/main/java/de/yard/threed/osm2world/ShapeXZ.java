package de.yard.threed.osm2world;

import java.util.List;

/**
 * two-dimensional, immutable shape. The shape is not required to be closed.
 * This has a variety of uses, including creating geometries by extrusion.
 */
public interface ShapeXZ {
	
	/**
	 * returns the shape's vertices.
	 * For closed shapes, the first and last vertex are identical.
	 *
	 * TODO support accuracy-dependent vertex collections (e.g. for circles)
	 * 
	 * @return list of vertices, not empty, not null
	 */
	public List<VectorXZ> getVertexList();
	
}
