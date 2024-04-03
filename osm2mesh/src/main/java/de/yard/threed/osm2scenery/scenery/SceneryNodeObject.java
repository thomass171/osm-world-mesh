package de.yard.threed.osm2scenery.scenery;

import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2world.*;


import java.util.List;

/**
 * Aus OSM2World OutlineNodeWorldObject.
 * <p>
 * Object mit area/polygon, aber kein Way, but only ONE node relation. Typischerweise Connection/Junction between ways.
 * For now polygon free. bis ich sowas wie Templates habe.
 * <p>
 * Gegenstück zu SceneryAreaObject.
 *
 * @see NoOutlineNodeWorldObject
 */
public abstract class SceneryNodeObject extends SceneryFlatObject /*implements NodeWorldObject,
		IntersectionTestObject, WorldObjectWithOutline*/ {

    public MapNode node;

    //11.11.21??private EleConnectorGroup connectors = null;

    public SceneryNodeObject(String creatortag, MapNode node, Material material, Category category) {
        super(creatortag, material, category, new Area(null,material));
        this.node = node;
        osmIds.add(node.getOsmId());

    }



    @Override
    final public void buildEleGroups() {
        de.yard.threed.osm2scenery.elevation.EleConnectorGroup egr = getEleConnectorGroup(node);
        elevations = new EleConnectorGroupSet();
        getEleConnectorGroups().eleconnectorgroups.add(egr);
    }

    @Override
    protected void/*EleConnectorGroupSet*/ registerCoordinatesToElegroups(TerrainMesh tm) {
        //return null;
    }

    @Override
    public List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds, TerrainMesh tm, SceneryContext sceneryContext) {
        flatComponent = new AbstractArea[]{AbstractArea.EMPTYAREA};
        return null;
    }

    @Override
    public boolean isPartOfMesh(TerrainMesh tm) {
        //TODO irgendwie erkennen
        return false;
    }

	/*protected OutlineNodeSceneryObject(MapNode node) {
		this.node = node;
	}
	
	@Override
	public abstract SimplePolygonXZ getOutlinePolygonXZ();
	
	@Override
	public final MapNode getPrimaryMapElement() {
		return node;
	}
	
	@Override
	public EleConnectorGroup getEleConnectors() {
		
		if (connectors == null) {
			
			SimplePolygonXZ outlinePolygonXZ = getOutlinePolygonXZ();
			
			if (outlinePolygonXZ == null) {
				
				connectors = EleConnectorGroup.EMPTY;
				
			} else {
				
				connectors = new EleConnectorGroup();
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
	*/
    /**
     * @return a triangulation of the area covered by this junction
     */
	/*protected Collection<TriangleXYZ> getTriangulation() {
		
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
		
	}*/

}
