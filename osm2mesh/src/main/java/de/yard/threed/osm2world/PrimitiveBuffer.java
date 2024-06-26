package de.yard.threed.osm2world;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Copied from OSM2World.jogl
 *
 * Storage for low-level rendering information (vertex and primitive data)
 * that can be displayed using graphics APIs, e.g. OpenGL.
 * Higher-level information, such as object coherence, OSM attributes
 * or representations, isn't present in a PrimitiveBuffer.
 */
public class PrimitiveBuffer extends
		PrimitiveTarget<RenderableToPrimitiveTarget> {

	@Override
	public Class<RenderableToPrimitiveTarget> getRenderableType() {
		return RenderableToPrimitiveTarget.class;
	}
	
	@Override
	public void render(RenderableToPrimitiveTarget renderable) {
		renderable.renderTo(this);
	}
	
	private Multimap<Material, Primitive> primitiveMap = HashMultimap.create();
	
	@Override
	protected void drawPrimitive(Primitive.Type type, Material material,
								 List<VectorXYZ> vertices, List<VectorXYZ> normals,
								 List<List<VectorXZ>> texCoordLists) {
		primitiveMap.put(material,
				new Primitive(type, vertices, normals, texCoordLists));
	}
	
	/**
	 * returns all materials used in the buffer
	 */
	public Set<Material> getMaterials() {
		return primitiveMap.keySet();
	}
	
	/**
	 * returns all primitives that use a given material
	 */
	public Collection<Primitive> getPrimitives(Material material) {
		return primitiveMap.get(material);
	}
	
}
