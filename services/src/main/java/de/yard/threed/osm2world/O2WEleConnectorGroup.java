package de.yard.threed.osm2world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * TODO document
 * 
 * This only exists to make it easier for WorldObjects to manage a large
 * number of EleConnectors.
 */
public class O2WEleConnectorGroup implements Iterable<O2WEleConnector> {

	//TODO make private
	public final List<O2WEleConnector> eleConnectors;

	public O2WEleConnectorGroup() {
		this(new ArrayList<O2WEleConnector>());
	}

	private O2WEleConnectorGroup(List<O2WEleConnector> eleConnectors) {
		this.eleConnectors = eleConnectors;
	}
	
	public void addConnectorsFor(Iterable<VectorXZ> positions,
			Object reference, GroundState groundState) {
		
		for (VectorXZ pos : positions) {
			eleConnectors.add(new O2WEleConnector(pos, reference, groundState));
		}
		
	}
	
	public void addConnectorsFor(PolygonWithHolesXZ polygon,
			Object reference, GroundState groundState) {
		
		addConnectorsFor(polygon.getOuter().getVertices(), reference, groundState);
		
		for (SimplePolygonXZ hole : polygon.getHoles()) {
			addConnectorsFor(hole.getVertices(), reference, groundState);
		}
		
	}

	public void addConnectorsForTriangulation(Iterable<TriangleXZ> triangles,
			Object reference, GroundState groundState) {
		//TODO check later whether this method is still necessary
		
		Set<VectorXZ> positions = new HashSet<VectorXZ>();
		
		for (TriangleXZ t : triangles) {
			positions.add(t.v1);
			positions.add(t.v2);
			positions.add(t.v3);
		}
		
		addConnectorsFor(positions, null, groundState);
		
	}

	public void add(O2WEleConnector newConnector) {
		
		eleConnectors.add(newConnector);
				
	}
	
	public void addAll(Iterable<O2WEleConnector> newConnectors) {
		
		for (O2WEleConnector c : newConnectors) {
			eleConnectors.add(c);
		}
		
	}
	
	public O2WEleConnector getConnector(VectorXZ pos) {
		//TODO review this method (parameters sufficient? necessary at all?)
		
		for (O2WEleConnector eleConnector : eleConnectors) {
			if (eleConnector.pos.equals(pos)) {
				return eleConnector;
			}
		}
		
		return null;
		//TODO maybe ... throw new IllegalArgumentException();
		
	}
	
	public List<O2WEleConnector> getConnectors(Iterable<VectorXZ> positions) {
		
		List<O2WEleConnector> connectors = new ArrayList<O2WEleConnector>();
		
		for (VectorXZ pos : positions) {
			O2WEleConnector connector = getConnector(pos);
			connectors.add(connector);
			if (connector == null) {
				throw new IllegalArgumentException();
			}
		}
		
		return connectors;
		
	}

	public VectorXYZ getPosXYZ(VectorXZ pos) {

		O2WEleConnector c = getConnector(pos);
		
		if (c != null) {
			
			return c.getPosXYZ();
			
		} else {
			
			return pos.xyz(0);
			//TODO maybe ... throw new IllegalArgumentException();
			
		}
		
	}

	public List<VectorXYZ> getPosXYZ(Collection<VectorXZ> positions) {
		
		List<VectorXYZ> result = new ArrayList<VectorXYZ>(positions.size());
		
		for (VectorXZ pos : positions) {
			result.add(getPosXYZ(pos));
		}
		
		return result;
		
	}

	public PolygonXYZ getPosXYZ(SimplePolygonXZ polygon) {
		return new PolygonXYZ(getPosXYZ(polygon.getVertexLoop()));
	}
	
	public Collection<TriangleXYZ> getTriangulationXYZ(
			Collection<? extends TriangleXZ> trianglesXZ) {
		
		Collection<TriangleXYZ> trianglesXYZ =
				new ArrayList<TriangleXYZ>(trianglesXZ.size());
		
		for (TriangleXZ triangleXZ : trianglesXZ) {
			
			VectorXYZ v1 = getPosXYZ(triangleXZ.v1);
			VectorXYZ v2 = getPosXYZ(triangleXZ.v2);
			VectorXYZ v3 = getPosXYZ(triangleXZ.v3);
			
			if (triangleXZ.isClockwise()) { //TODO: ccw test should not be in here, but maybe in triangulation util
				trianglesXYZ.add(new TriangleXYZ(v3, v2, v1));
			} else  {
				trianglesXYZ.add(new TriangleXYZ(v1, v2, v3));
			}
			
		}
		
		return trianglesXYZ;
		
	}
	
	@Override
	public Iterator<O2WEleConnector> iterator() {
		return eleConnectors.iterator();
	}
	
	public static final O2WEleConnectorGroup EMPTY = new O2WEleConnectorGroup(
			Collections.<O2WEleConnector>emptyList());
	
}
