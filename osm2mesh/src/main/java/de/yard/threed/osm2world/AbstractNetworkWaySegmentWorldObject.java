package de.yard.threed.osm2world;



import de.yard.threed.osm2scenery.modules.BridgeModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static de.yard.threed.osm2world.EleConstraintEnforcer.ConstraintType.MAX;
import static de.yard.threed.osm2world.EleConstraintEnforcer.ConstraintType.MIN;
import static de.yard.threed.osm2world.GeometryUtil.interpolateBetween;
import static de.yard.threed.osm2world.GroundState.ABOVE;
import static de.yard.threed.osm2world.GroundState.ON;
import static de.yard.threed.osm2world.ValueStringParser.parseIncline;
import static de.yard.threed.osm2world.VectorXZ.distanceSquared;
import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.util.Collections.emptyList;


public abstract class AbstractNetworkWaySegmentWorldObject
	implements NetworkWaySegmentWorldObject, WaySegmentWorldObject,
	           IntersectionTestObject, WorldObjectWithOutline {

	public final MapWaySegment segment;
	
	private VectorXZ startCutVector = null;
	private VectorXZ endCutVector = null;
	
	private VectorXZ startOffset = VectorXZ.NULL_VECTOR;
	private VectorXZ endOffset = VectorXZ.NULL_VECTOR;
	
	protected O2WEleConnectorGroup connectors;
	
	private List<VectorXZ> centerlineXZ = null;
	
	private List<VectorXZ> leftOutlineXZ = null;
	private List<VectorXZ> rightOutlineXZ = null;
	
	private SimplePolygonXZ outlinePolygonXZ = null;
	
	private Boolean broken = null;
	
	protected AbstractNetworkWaySegmentWorldObject(MapWaySegment segment) {
		this.segment = segment;
	}
	
	@Override
	public final MapWaySegment getPrimaryMapElement() {
		return segment;
	}
	
	@Override
	public void setStartCutVector(VectorXZ cutVector) {
		this.startCutVector = cutVector;
	}
	
	@Override
	public void setEndCutVector(VectorXZ cutVector) {
		this.endCutVector = cutVector;
	}

	@Override
	public VectorXZ getStartCutVector() {
		return startCutVector;
	}

	@Override
	public VectorXZ getEndCutVector() {
		return endCutVector;
	}
	
	public VectorXZ getCutVectorAt(MapNode node) {
		if (node == segment.getStartNode()) {
			return getStartCutVector();
		} else if (node == segment.getEndNode()) {
			return getEndCutVector();
		} else {
			throw new IllegalArgumentException("node is not part of the line");
		}
	}
	
	@Override
	public void setStartOffset(VectorXZ offsetVector) {
		this.startOffset = offsetVector;
	}
	
	@Override
	public void setEndOffset(VectorXZ offsetVector) {
		this.endOffset = offsetVector;
	}
	
	protected VectorXZ getStartWithOffset() {
		return segment.getStartNode().getPos().add(startOffset); //SUGGEST (performance): cache? [also getEnd*]
	}
	
	protected VectorXZ getEndWithOffset() {
		return segment.getEndNode().getPos().add(endOffset);
	}
	
	/**
	 * calculates centerline and outlines, along with their connectors
	 * TODO: perform before construction, to simplify the object and avoid {@link #broken}
	 */
	private void calculateXZGeometry() {
	
		if (startCutVector == null || endCutVector == null) {
			throw new IllegalStateException("cannot calculate outlines before cut vectors");
		}
		
		connectors = new O2WEleConnectorGroup();
		
		{ /* calculate centerline */
						
			centerlineXZ = new ArrayList<VectorXZ>();
			
			final VectorXZ start = getStartWithOffset();
			final VectorXZ end = getEndWithOffset();
			
			centerlineXZ.add(start);
			
			connectors.add(new O2WEleConnector(start,
					segment.getStartNode(), getGroundState(segment.getStartNode())));
			
			// add intersections along the centerline
			
			for (MapOverlap<?,?> overlap : segment.getOverlaps()) {
				
				if (overlap.getOther(segment).getPrimaryRepresentation() == null)
					continue;
				
				if (overlap instanceof MapIntersectionWW) {
					
					MapIntersectionWW intersection = (MapIntersectionWW) overlap;
					
					if (GeometryUtil.isBetween(intersection.pos, start, end)) {
						
						centerlineXZ.add(intersection.pos);
						
						connectors.add(new O2WEleConnector(intersection.pos,
								null, getGroundState()));
						
					}
					
				} else if (overlap instanceof MapOverlapWA
						&& overlap.type == MapOverlapType.INTERSECT) {
					
					if (!(overlap.getOther(segment).getPrimaryRepresentation()
							instanceof AbstractAreaWorldObject)) continue;
					
					MapOverlapWA overlapWA = (MapOverlapWA) overlap;
					
					for (int i = 0; i < overlapWA.getIntersectionPositions().size(); i++) {
					
						VectorXZ pos = overlapWA.getIntersectionPositions().get(i);
						
						if (GeometryUtil.isBetween(pos, start, end)) {
							
							centerlineXZ.add(pos);
							
							connectors.add(new O2WEleConnector(pos,
									null, getGroundState()));
							
						}
					
					}
					
				}
				
				
			}
			
			// finish the centerline
			
			centerlineXZ.add(end);

			connectors.add(new O2WEleConnector(end,
					segment.getEndNode(), getGroundState(segment.getEndNode())));
			
			if (centerlineXZ.size() > 3) {
				
				// sort by distance from start
				Collections.sort(centerlineXZ, new Comparator<VectorXZ>() {
					@Override
					public int compare(VectorXZ v1, VectorXZ v2) {
						return Double.compare(
								distanceSquared(v1, start),
								distanceSquared(v2, start));
					}
				});
				
			}
			
		}
		
		{ /* calculate left and right outlines */
			
			leftOutlineXZ = new ArrayList<VectorXZ>(centerlineXZ.size());
			rightOutlineXZ = new ArrayList<VectorXZ>(centerlineXZ.size());
			
			assert centerlineXZ.size() >= 2;
			
			double halfWidth = getWidth() * 0.5f;
			
			VectorXZ centerStart = centerlineXZ.get(0);
			leftOutlineXZ.add(centerStart.add(startCutVector.mult(-halfWidth)));
			rightOutlineXZ.add(centerStart.add(startCutVector.mult(halfWidth)));
			
			connectors.add(new O2WEleConnector(leftOutlineXZ.get(0),
					segment.getStartNode(), getGroundState(segment.getStartNode())));
			connectors.add(new O2WEleConnector(rightOutlineXZ.get(0),
					segment.getStartNode(), getGroundState(segment.getStartNode())));
			
			for (int i = 1; i < centerlineXZ.size() - 1; i++) {
				
				leftOutlineXZ.add(centerlineXZ.get(i).add(segment.getRightNormal().mult(-halfWidth)));
				rightOutlineXZ.add(centerlineXZ.get(i).add(segment.getRightNormal().mult(halfWidth)));
				
				connectors.add(new O2WEleConnector(leftOutlineXZ.get(i),
						null, getGroundState()));
				connectors.add(new O2WEleConnector(rightOutlineXZ.get(i),
						null, getGroundState()));
				
			}
			
			VectorXZ centerEnd = centerlineXZ.get(centerlineXZ.size() - 1);
			leftOutlineXZ.add(centerEnd.add(endCutVector.mult(-halfWidth)));
			rightOutlineXZ.add(centerEnd.add(endCutVector.mult(halfWidth)));
			
			connectors.add(new O2WEleConnector(leftOutlineXZ.get(leftOutlineXZ.size() - 1),
					segment.getEndNode(), getGroundState(segment.getEndNode())));
			connectors.add(new O2WEleConnector(rightOutlineXZ.get(rightOutlineXZ.size() - 1),
					segment.getEndNode(), getGroundState(segment.getEndNode())));
			
		}
		
		{ /* calculate the outline loop */
			
			List<VectorXZ> outlineLoopXZ =
					new ArrayList<VectorXZ>(centerlineXZ.size() * 2 + 1);
			
			outlineLoopXZ.addAll(rightOutlineXZ);
			
			List<VectorXZ> left = new ArrayList<VectorXZ>(leftOutlineXZ);
			Collections.reverse(left);
			outlineLoopXZ.addAll(left);
			
			outlineLoopXZ.add(outlineLoopXZ.get(0));
			
			// check for brokenness
			
			try {
				outlinePolygonXZ = new SimplePolygonXZ(outlineLoopXZ);
				broken = outlinePolygonXZ.isClockwise();
			} catch (InvalidGeometryException e) {
				broken = true;
				connectors = O2WEleConnectorGroup.EMPTY;
			}
		
		}
		
	}
	
	/**
	 * determines whether the node is connected to the terrain based on the
	 * segments connected to it
	 * 
	 * @param node  one of the nodes of {@link #segment}
	 */
	private GroundState getGroundState(MapNode node) {
		
		WorldObject primaryWO = node.getPrimaryRepresentation();
		
		if (primaryWO != null) {
			
			return primaryWO.getGroundState();
			
		} else if (this.getGroundState() == ON) {
			
			return ON;
			
		} else {
			
			boolean allAbove = true;
			boolean allBelow = true;
			
			for (MapWaySegment segment : node.getConnectedWaySegments()) {
				if (segment.getPrimaryRepresentation() != null) {
					switch (segment.getPrimaryRepresentation().getGroundState()) {
					case ABOVE: allBelow = false; break;
					case BELOW: allAbove = false; break;
					case ON: return ON;
					}
				}
			}
			
			if (allAbove) {
				return ABOVE;
			} else if (allBelow) {
				return GroundState.BELOW;
			} else {
				return ON;
			}
			
		}
		
	}
	
	/**
	 * implementation of {@link WorldObject#getGroundState()}.
	 * This version checks for bridge and tunnel tags to make the decision.
	 * If that is not desired, subclasses may override the method.
	 */
	@Override
	public GroundState getGroundState() {
		if (BridgeModule.isBridge(segment.getTags())) {
			return ABOVE;
		} else if (TunnelModule.isTunnel(segment.getTags())) {
			return GroundState.BELOW;
		} else {
			return ON;
		}
	}

	@Override
	public O2WEleConnectorGroup getEleConnectors() {
		
		if (connectors == null) {
			calculateXZGeometry();
		}
		
		return connectors;
		
	}
	
	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) {
		
		if (isBroken()) return;
		
		//TODO: maybe save connectors separately right away
		
		List<O2WEleConnector> center = getCenterlineEleConnectors();
		List<O2WEleConnector> left = connectors.getConnectors(getOutlineXZ(false));
		List<O2WEleConnector> right = connectors.getConnectors(getOutlineXZ(true));
		
		/* left and right connectors have the same ele as their center conn. */
		
		for (int i = 0; i < center.size(); i++) {
			
			enforcer.requireSameEle(center.get(i), left.get(i));
			enforcer.requireSameEle(center.get(i), right.get(i));
			
		}
		
		/* incline should be honored */
		
		String inclineValue = segment.getTags().getValue("incline");
				
		if (inclineValue != null) {
			
			double minIncline = NaN;
			double maxIncline = NaN;
			
			if ("up".equals(inclineValue)) {
				
				minIncline = 0.1;
				
			} else if ("down".equals(inclineValue)) {
				
				maxIncline = -0.1;
				
			} else {
				
				Float incline = parseIncline(inclineValue);
				
				if (incline != null) {
					if (incline > 0) {
						minIncline = incline / 100.0 * 0.5;
						maxIncline = incline / 100.0 * 1.1;
					} else if (incline < 0) {
						maxIncline = incline / 100.0 * 0.5;
						minIncline = incline / 100.0 * 1.1;
					} else {
						
						enforcer.requireSameEle(center);
						
					}
				}
				
			}
			
			if (!isNaN(minIncline)) {
				enforcer.requireIncline(MIN, minIncline, center);
			}
			
			if (!isNaN(maxIncline)) {
				enforcer.requireIncline(MAX, maxIncline, center);
			}
			
		}
		
		//TODO sensible maximum incline for road and rail; and waterway down-incline
		// ... take incline differences, steps etc. into account => move into Road, Rail separately
		
		/* ensure a smooth transition from previous segment */
		
		//TODO this might be more elegant with an "Invisible Connector WO"
		
		List<MapWaySegment> connectedSegments =
				segment.getStartNode().getConnectedWaySegments();
		
		if (connectedSegments.size() == 2) {
			
			MapWaySegment previousSegment = null;
			
			for (MapWaySegment connectedSegment : connectedSegments) {
				if (connectedSegment != this.segment) {
					previousSegment = connectedSegment;
				}
			}
			
			WorldObject previousWO = previousSegment.getPrimaryRepresentation();
			
			if (previousWO instanceof AbstractNetworkWaySegmentWorldObject) {
				
				AbstractNetworkWaySegmentWorldObject previous =
						(AbstractNetworkWaySegmentWorldObject)previousWO;
				
				if (!previous.isBroken()) {
					
					List<O2WEleConnector> previousCenter =
							previous.getCenterlineEleConnectors();
					
					enforcer.requireSmoothness(
							previousCenter.get(previousCenter.size() - 2),
							center.get(0),
							center.get(1));
					
				}
				
			}

		}
		
		/* ensure smooth transitions within the way itself */
		
		for (int i = 0; i + 2 < center.size(); i++) {
			enforcer.requireSmoothness(
					center.get(i),
					center.get(i+1),
					center.get(i+2));
		}
		
	}
	
	protected List<O2WEleConnector> getCenterlineEleConnectors() {

		if (isBroken()) return emptyList();
		
		return connectors.getConnectors(getCenterlineXZ());
		
	}
	
	/**
	 * returns a sequence of node running along the center of the
	 * line from start to end (each with offset).
	 * Uses the {@link WaySegmentElevationProfile} for adding
	 * elevation information.
	 */
	public List<VectorXZ> getCenterlineXZ() {
		
		if (centerlineXZ == null) {
			calculateXZGeometry();
		}
		
		return centerlineXZ;
		
	}
	
	/**
	 * 3d version of {@link #getCenterlineXZ()}.
	 * Only available after elevation calculation.
	 */
	public List<VectorXYZ> getCenterline() {
		return connectors.getPosXYZ(getCenterlineXZ());
	}
	
	/**
	 * Variant of {@link #getOutline(boolean)}.
	 * This one is already available before elevation calculation.
	 */
	public List<VectorXZ> getOutlineXZ(boolean right) {
		
		if (right) {
			
			if (rightOutlineXZ == null) {
				calculateXZGeometry();
			}
			
			return rightOutlineXZ;
			
		} else { //left
			
			if (leftOutlineXZ == null) {
				calculateXZGeometry();
			}
			
			return leftOutlineXZ;
			
		}
		
	}
	
	/**
	 * provides the left or right border (a line at an appropriate distance
	 * from the center line), taking into account cut vectors, offsets and
	 * elevation information.
	 * Available after cut vectors, offsets and elevation information
	 * have been calculated.
	 * 
	 * Left and right border have the same number of nodes as the elevation
	 * profile's {@link WaySegmentElevationProfile#getPointsWithEle()}.
	 * //TODO: compatible with future offset/clearing influences?
	 */
	public List<VectorXYZ> getOutline(boolean right) {
		return connectors.getPosXYZ(getOutlineXZ(right));
	}
	
	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {
		
		if (outlinePolygonXZ == null) {
			calculateXZGeometry();
		}
		
		if (isBroken()) {
			return null;
		} else {
			return outlinePolygonXZ;
		}
		
	}
	
	@Override
	public PolygonXYZ getOutlinePolygon() {
		
		if (isBroken()) {
			return null;
		} else {
			return connectors.getPosXYZ(outlinePolygonXZ);
		}
		
	}
	
	/**
	 * checks whether this segment has a broken outline.
	 * That can happen e.g. if it lies between two junctions that are too close
	 * together.
	 */
	public boolean isBroken() {
		
		if (broken == null) {
			calculateXZGeometry();
		}
		
		//TODO filter out broken objects during creation in the world module
		return broken;
		
	}
	
	/**
	 * returns a point on the start or end cut line.
	 * 
	 * @param start  point is on the start cut if true, on the end cut if false
	 * @param relativePosFromLeft  0 is the leftmost point, 1 the rightmost.
	 *                             Values in between are for interpolation.
	 */
	public VectorXYZ getPointOnCut(boolean start, double relativePosFromLeft) {
		
		assert 0 <= relativePosFromLeft && relativePosFromLeft <= 1;
		
		VectorXZ position = start ? getStartWithOffset() : getEndWithOffset();
		VectorXZ cutVector = start ? getStartCutVector() : getEndCutVector();
		
		VectorXYZ left = connectors.getPosXYZ(position.add(
				cutVector.mult(-0.5 * getWidth())));
		VectorXYZ right = connectors.getPosXYZ(position.add(
				cutVector.mult(+0.5 * getWidth())));
		
		if (relativePosFromLeft == 0) {
			return left;
		} else if (relativePosFromLeft == 1) {
			return right;
		} else {
			return interpolateBetween(left, right, relativePosFromLeft);
		}
		
	}

	@Override
	public VectorXZ getStartOffset() {
		return startOffset;
	}

	@Override
	public VectorXZ getEndOffset() {
		return endOffset;
	}
	
	@Override
	public VectorXZ getStartPosition() {
		return segment.getStartNode().getPos().add(getStartOffset());
	}
	
	@Override
	public VectorXZ getEndPosition() {
		return segment.getEndNode().getPos().add(getEndOffset());
	}
	
	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		
		if (isBroken() || getOutlinePolygonXZ() == null) {
			return null;
		} else {
			return new AxisAlignedBoundingBoxXZ(
					getOutlinePolygonXZ().getVertexCollection());
		}
		
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + segment + ")";
	}
	
}
