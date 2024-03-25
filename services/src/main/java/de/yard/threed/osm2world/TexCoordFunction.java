package de.yard.threed.osm2world;


import java.util.List;

/**
 * the function used to calculate texture coordinates for each vertex from
 * a collection. Some implementations only make sense for certain geometries
 * (e.g. vertices forming triangle strips).
 */
public interface TexCoordFunction {
	
	/**
	 * calculates a texture coordinate for each vertex.
	 * 12.7.19: Returns null in the case of error instead of throwing an exception.
	 *
	 */
	public List<VectorXZ> apply(
			List<VectorXYZ> vs, TextureData textureData);
	
}
