package de.yard.threed.osm2world;



import java.util.ArrayList;
import java.util.List;

import static de.yard.threed.osm2world.EleConstraintEnforcer.ConstraintType.EXACT;
import static de.yard.threed.osm2world.GroundState.ON;
import static de.yard.threed.osm2world.WorldModuleGeometryUtil.createTriangleStripBetween;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;


/**
 * adds tunnels to the world.
 * 
 * Needs to be applied <em>after</em> all the modules that generate
 * whatever runs through the tunnels.
 */
public class TunnelModule extends AbstractModule {

	public static final boolean isTunnel(TagGroup tags) {
		return tags.containsKey("tunnel")
				&& !"no".equals(tags.getValue("tunnel"))
				&& !"building_passage".equals(tags.getValue("tunnel"));
	}

	public static final boolean isTunnel(MapSegment segment) {
		if (segment instanceof MapWaySegment) {
			return isTunnel(((MapWaySegment)segment).getTags());
		} else {
			return isTunnel(((MapAreaSegment)segment).getArea().getTags());
		}
	}
	
	@Override
	protected void applyToWaySegment(MapWaySegment segment) {
		
		WaySegmentWorldObject primaryRepresentation =
			segment.getPrimaryRepresentation();
		
		if (primaryRepresentation instanceof AbstractNetworkWaySegmentWorldObject
				&& isTunnel(segment)) {
			
			segment.addRepresentation(new Tunnel(segment,
					(AbstractNetworkWaySegmentWorldObject) primaryRepresentation));
			
		}
		
	}
	
	@Override
	protected void applyToNode(MapNode node) {
		
		/* entrances */
		
		if (node.getConnectedWaySegments().size() == 2) {
			
			MapWaySegment segmentA = node.getConnectedWaySegments().get(0);
			MapWaySegment segmentB = node.getConnectedWaySegments().get(1);
			
			if (isTunnel(segmentA) && !isTunnel(segmentB)
					&& segmentA.getPrimaryRepresentation() instanceof AbstractNetworkWaySegmentWorldObject) {
				
				node.addRepresentation(new TunnelEntrance(node,
						(AbstractNetworkWaySegmentWorldObject)
						segmentA.getPrimaryRepresentation()));
				
			} else if (isTunnel(segmentB) && !isTunnel(segmentA)
					&& segmentB.getPrimaryRepresentation() instanceof AbstractNetworkWaySegmentWorldObject) {
				
				node.addRepresentation(new TunnelEntrance(node,
						(AbstractNetworkWaySegmentWorldObject)
						segmentB.getPrimaryRepresentation()));
				
			}
			
		}
		
		/* tunnel nodes and junctions */
		
		boolean onlyTunnelConnected = true;
		for (MapWaySegment segment : node.getConnectedWaySegments()) {
			if (!isTunnel(segment)) {
				onlyTunnelConnected = false;
				break;
			}
		}
			
		if (onlyTunnelConnected) {

			if (node.getPrimaryRepresentation() instanceof VisibleConnectorNodeWorldObject) {
				//TODO: TunnelConnector
			} else if (node.getPrimaryRepresentation() instanceof JunctionNodeWorldObject) {
				node.addRepresentation(new TunnelJunction(node,
						(JunctionNodeWorldObject) node.getPrimaryRepresentation()));
			}
			
		}

		
	}
	
	public static class Tunnel extends BridgeOrTunnel
			implements RenderableToAllTargets {
		
		public Tunnel(MapWaySegment segment,
				AbstractNetworkWaySegmentWorldObject primaryWO) {
			super(segment, primaryWO);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.BELOW;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			List<VectorXYZ> leftOutline = primaryRep.getOutline(false);
			List<VectorXYZ> rightOutline = primaryRep.getOutline(true);
			
			List<VectorXYZ> aboveLeftOutline =
				new ArrayList<VectorXYZ>(leftOutline.size());
			List<VectorXYZ> aboveRightOutline =
				new ArrayList<VectorXYZ>(rightOutline.size());
			
			for (int i=0; i < leftOutline.size(); i++) {
			
				VectorXYZ clearingOffset = VectorXYZ.Y_UNIT.mult(
						10); //TODO restore clearing
//						primaryRep.getClearingAbove(leftOutline.get(i).xz()));
				
				aboveLeftOutline.add(leftOutline.get(i).add(clearingOffset));
				aboveRightOutline.add(rightOutline.get(i).add(clearingOffset));
				
			}

			VectorXYZList strip1 = createTriangleStripBetween(
					rightOutline, aboveRightOutline);
			VectorXYZList strip2 = createTriangleStripBetween(
					aboveRightOutline, aboveLeftOutline);
			VectorXYZList strip3 = createTriangleStripBetween(
					aboveLeftOutline, leftOutline);
			
			target.drawTriangleStrip(Materials.TUNNEL_DEFAULT, strip1, null,null);
			target.drawTriangleStrip(Materials.TUNNEL_DEFAULT, strip2, null,null);
			target.drawTriangleStrip(Materials.TUNNEL_DEFAULT, strip3, null,null);
					
		}
		
	}
	
	public static class TunnelEntrance implements NodeWorldObject,
		TerrainBoundaryWorldObject {
		
		private final MapNode node;
		private final AbstractNetworkWaySegmentWorldObject tunnelContent;

		/**
		 * counterclockwise outline composed of
		 * {@link #lowerLeft}, {@link #lowerCenter}, {@link #lowerRight},
		 * {@link #upperRight}, {@link #upperCenter} and {@link #upperLeft}.
		 */
		private SimplePolygonXZ outline;
		
		private VectorXZ lowerLeft;
		private VectorXZ lowerCenter;
		private VectorXZ lowerRight;
		private VectorXZ upperLeft;
		private VectorXZ upperCenter;
		private VectorXZ upperRight;
		
		private O2WEleConnectorGroup connectors;
		
		public TunnelEntrance(MapNode node,
				AbstractNetworkWaySegmentWorldObject tunnelContent) {
			
			this.node = node;
			this.tunnelContent = tunnelContent;
			
		}
		
		/**
		 * creates outline as "ring" around the entrance
		 */
		private void calculateOutlineIfNecessary() {
			
			if (outline != null) return;

			lowerCenter = node.getPos();
			
			VectorXZ toRight = tunnelContent.getStartCutVector()
					.mult(tunnelContent.getWidth() * 0.5f);
			
			lowerLeft = lowerCenter.subtract(toRight);
			lowerRight = lowerCenter.add(toRight);
			
			VectorXZ toBack = tunnelContent.segment.getDirection().mult(0.1);
			if (tunnelContent.segment.getEndNode() == node) {
				toBack = toBack.invert();
			}
			
			upperLeft = lowerLeft.add(toBack);
			upperCenter = lowerCenter.add(toBack);
			upperRight = lowerRight.add(toBack);
			
			outline = new SimplePolygonXZ(asList(
					lowerLeft, lowerCenter, lowerRight,
					upperRight, upperCenter, upperLeft,
					lowerLeft));
			
		}
		
		@Override
		public MapNode getPrimaryMapElement() {
			return node;
		}

		@Override
		public GroundState getGroundState() {
			return ON;
		}
		
		@Override
		public Iterable<O2WEleConnector> getEleConnectors() {
			
			calculateOutlineIfNecessary();
			
			if (connectors == null) {
				connectors = new O2WEleConnectorGroup();
				connectors.add(new O2WEleConnector(lowerLeft, node, ON));
				connectors.add(new O2WEleConnector(lowerCenter, node, ON));
				connectors.add(new O2WEleConnector(lowerRight, node, ON));
				connectors.add(new O2WEleConnector(upperLeft, null, ON));
				connectors.add(new O2WEleConnector(upperCenter, null, ON));
				connectors.add(new O2WEleConnector(upperRight, null, ON));
			}
			
			return connectors;
			
		}
		
		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {
			
			enforcer.requireVerticalDistance(
					EXACT,
					10,
					connectors.getConnector(upperLeft), connectors.getConnector(lowerLeft));
			
			enforcer.requireVerticalDistance(
					EXACT,
					10,
					connectors.getConnector(upperCenter), connectors.getConnector(lowerCenter));

			enforcer.requireVerticalDistance(
					EXACT,
					10,
					connectors.getConnector(upperRight), connectors.getConnector(lowerRight));
			
			//TODO restore original clearing
			//tunnelPrimaryRep.getClearingAbove(node.getPos()));
			
		}
		
		@Override
		public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
			
			calculateOutlineIfNecessary();
			
			return new AxisAlignedBoundingBoxXZ(getOutlinePolygon().getVertices());
			
		}
		
		@Override
		public SimplePolygonXZ getOutlinePolygonXZ() {
			
			calculateOutlineIfNecessary();
			
			return outline;
			
		}
		
		@Override
		public PolygonXYZ getOutlinePolygon() {
			
			calculateOutlineIfNecessary();
			
			return connectors.getPosXYZ(getOutlinePolygonXZ());
			
		}
		
	}
	
	public static class TunnelJunction implements NodeWorldObject,
		RenderableToAllTargets {
	
		private final MapNode node;
		private final JunctionNodeWorldObject primaryRep;
		
		public TunnelJunction(MapNode node, JunctionNodeWorldObject primaryRep) {
			this.node = node;
			this.primaryRep = primaryRep;
		}
		
		@Override
		public MapNode getPrimaryMapElement() {
			return node;
		}
	
		@Override
		public GroundState getGroundState() {
			return GroundState.BELOW;
		}
		
		@Override
		public Iterable<O2WEleConnector> getEleConnectors() {
			// TODO EleConnectors for tunnels
			return emptyList();
		}
		
		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {}
		
		@Override
		public void renderTo(Target<?> target) {
			
			//TODO port to new elevation model
			
//			List<VectorXYZ> topOutline = new ArrayList<VectorXYZ>();
//
//			int segCount = node.getConnectedSegments().size();
//			for (int i=0; i<segCount; i++) {
//
//				List<VectorXYZ> line = primaryRep.getOutline((i+1)%segCount, i);
//
//				List<VectorXYZ> lineTop =
//					new ArrayList<VectorXYZ>(line.size());
//
//				for (VectorXYZ lineV : line) {
//
//					double clearing;
//
//					if (line.indexOf(lineV) == 0) {
//						MapSegment segment = node.getConnectedSegments().get((i+1)%segCount);
//						clearing = 10; //TODO clearingAboveMapSegment(lineV, segment);
//					} else {
//						MapSegment segment = node.getConnectedSegments().get(i);
//						clearing = 10; //TODO clearingAboveMapSegment(lineV, segment);
//					}
//
//					lineTop.add(lineV.y(lineV.y + clearing));
//
//				}
//
//				// draw wall
//
//				target.drawTriangleStrip(Materials.TUNNEL_DEFAULT,
//						createTriangleStripBetween(line, lineTop), null);
//
//				//collect nodes for top outline
//
//				topOutline.addAll(lineTop);
//
//			}
//
//			// draw top
//
//			target.drawConvexPolygon(Materials.TUNNEL_DEFAULT, topOutline,
//					globalTexCoordLists(
//							topOutline, Materials.TUNNEL_DEFAULT, false));
//
		}

		//TODO update or delete
//		private static double clearingAboveMapSegment(VectorXYZ lineV, MapSegment segment) {
//
//			WorldObject segmentRep;
//			if (segment instanceof MapWaySegment) {
//				segmentRep = ((MapWaySegment)segment)
//					.getPrimaryRepresentation();
//			} else {
//				segmentRep = ((MapAreaSegment)segment)
//					.getArea().getPrimaryRepresentation();
//			}
//
//			if (segmentRep != null) {
//				return segmentRep.getClearingAbove(lineV.xz());
//			} else {
//				return 0;
//			}
//
//		}
		
	}
	
}
