package de.yard.threed.osm2world;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;

/**
 * superclass for {@link NodeWorldObject}s that do have an outline
 * and are not just treated as an infinitely small point.
 * 
 * @see
 */
public abstract class OutlineNodeWorldObject implements NodeWorldObject,
		IntersectionTestObject, WorldObjectWithOutline {

	protected final MapNode node;

	private O2WEleConnectorGroup connectors = null;
	
	protected OutlineNodeWorldObject(MapNode node) {
		this.node = node;
	}
	
	@Override
	public abstract SimplePolygonXZ getOutlinePolygonXZ();
	
	@Override
	public final MapNode getPrimaryMapElement() {
		return node;
	}
	
	@Override
	public O2WEleConnectorGroup getEleConnectors() {
		
		if (connectors == null) {
			
			SimplePolygonXZ outlinePolygonXZ = getOutlinePolygonXZ();
			
			if (outlinePolygonXZ == null) {
				
				connectors = O2WEleConnectorGroup.EMPTY;
				
			} else {
				
				connectors = new O2WEleConnectorGroup();
				connectors.addConnectorsFor(outlinePolygonXZ.getVertices(),
						node, getGroundState());
				
			}
			
		}
		
		return connectors;
		
	}
	
	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) {}
	
	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		if (getOutlinePolygonXZ() != null) {
			return new AxisAlignedBoundingBoxXZ(
					getOutlinePolygonXZ().getVertexCollection());
		} else {
			return new AxisAlignedBoundingBoxXZ(
					node.getPos().x, node.getPos().z,
					node.getPos().x, node.getPos().z);
		}
	}
	
	@Override
	public PolygonXYZ getOutlinePolygon() {
		return connectors.getPosXYZ(getOutlinePolygonXZ());
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + node + ")";
	}
	
	/**
	 * @return  a triangulation of the area covered by this junction
	 */
	protected Collection<TriangleXYZ> getTriangulation() {
		
		if (getOutlinePolygonXZ() == null) return emptyList();
		
		Collection<TriangleXZ> trianglesXZ = TriangulationUtil.triangulate(
				getOutlinePolygonXZ(),
				Collections.<SimplePolygonXZ>emptyList());
		
		Collection<TriangleXYZ> trianglesXYZ =
			new ArrayList<TriangleXYZ>(trianglesXZ.size());
		
		for (TriangleXZ triangleXZ : trianglesXZ) {
			VectorXYZ v1 = connectors.getPosXYZ(triangleXZ.v1);
			VectorXYZ v2 = connectors.getPosXYZ(triangleXZ.v2);
			VectorXYZ v3 = connectors.getPosXYZ(triangleXZ.v3);
			if (triangleXZ.isClockwise()) {
				trianglesXYZ.add(new TriangleXYZ(v3, v2, v1));
			} else  {
				trianglesXYZ.add(new TriangleXYZ(v1, v2, v3));
			}
		}
		
		return trianglesXYZ;
		
	}
	
}
