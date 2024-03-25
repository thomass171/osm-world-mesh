package de.yard.threed.osm2world;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static de.yard.threed.osm2world.NormalCalculationUtil.calculateTriangleFanNormals;
import static de.yard.threed.osm2world.NormalCalculationUtil.calculateTriangleNormals;
import static de.yard.threed.osm2world.Primitive.Type.*;


/**
 * superclass for targets that are based on OpenGL primitives.
 * These targets will treat different primitives similarly:
 * They convert them all to a list of vertices
 * and represent the primitive type using an enum or flags.
 */
public abstract class PrimitiveTarget<R extends Renderable>
		extends AbstractTarget<R> {

	/**
	 * @param vs       vertices that form the primitive
	 * @param normals  normal vector for each vertex; same size as vs
	 * @param texCoordLists  texture coordinates for each texture layer,
	 *                       each list has the same size as vs
	 */
	abstract protected void drawPrimitive(Primitive.Type type, Material material,
			List<VectorXYZ> vs, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists);
	
	@Override
	public void drawTriangleStrip(Material material, VectorXYZList vs,
			List<List<VectorXZ>> texCoordLists, OsmOrigin rawRenderData) {
		boolean smooth = (material.getInterpolation() == Material.Interpolation.SMOOTH);
		drawPrimitive(TRIANGLE_STRIP, material, vs.vs,
				NormalCalculationUtil.calculateTriangleStripNormals(vs, smooth).vs,
				texCoordLists);
	}

	@Override
	public void drawTriangleFan(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {
		boolean smooth = (material.getInterpolation() == Material.Interpolation.SMOOTH);
		drawPrimitive(TRIANGLE_FAN, material, vs,
				calculateTriangleFanNormals(new VectorXYZList(vs), smooth).vs,
				texCoordLists);
	}
	
	@Override
	public void drawTriangles(Material material,
			Collection<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> texCoordLists, OsmOrigin rawRenderData) {
		
		List<VectorXYZ> vectors = new ArrayList<VectorXYZ>(triangles.size()*3);
		
		for (TriangleXYZ triangle : triangles) {
			vectors.add(triangle.v1);
			vectors.add(triangle.v2);
			vectors.add(triangle.v3);
		}
		
		drawPrimitive(TRIANGLES, material, vectors,
				calculateTriangleNormals(vectors,
						material.getInterpolation() == Material.Interpolation.SMOOTH),
						texCoordLists);
		
	}
	
	@Override
	public void drawTrianglesWithNormals(Material material,
			Collection<? extends TriangleXYZWithNormals> triangles,
			List<List<VectorXZ>> texCoordLists) {

		List<VectorXYZ> vectors = new ArrayList<VectorXYZ>(triangles.size()*3);
		List<VectorXYZ> normals = new ArrayList<VectorXYZ>(triangles.size()*3);
				
		for (TriangleXYZWithNormals triangle : triangles) {
			vectors.add(triangle.v1);
			vectors.add(triangle.v2);
			vectors.add(triangle.v3);
			normals.add(triangle.n1);
			normals.add(triangle.n2);
			normals.add(triangle.n3);
		}
		
		drawPrimitive(TRIANGLES, material, vectors, normals, texCoordLists);
		
	}
	
}
