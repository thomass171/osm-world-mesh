package de.yard.threed.osm2world;


import java.util.List;

import static de.yard.threed.osm2world.EleConstraintEnforcer.ConstraintType.MAX;
import static de.yard.threed.osm2world.EleConstraintEnforcer.ConstraintType.MIN;
import static de.yard.threed.osm2world.GeometryUtil.isBetween;
import static de.yard.threed.osm2world.GroundState.ABOVE;
import static de.yard.threed.osm2world.GroundState.ON;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * common superclass for bridges and tunnels
 */
public abstract class BridgeOrTunnel implements WaySegmentWorldObject {

	protected final MapWaySegment segment;
	protected final AbstractNetworkWaySegmentWorldObject primaryRep;
	
	public BridgeOrTunnel(MapWaySegment segment,
                          AbstractNetworkWaySegmentWorldObject primaryRepresentation) {
		this.segment = segment;
		this.primaryRep = primaryRepresentation;
	}

	@Override
	public MapWaySegment getPrimaryMapElement() {
		return segment;
	}

	@Override
	public VectorXZ getEndPosition() {
		return primaryRep.getEndPosition();
	}

	@Override
	public VectorXZ getStartPosition() {
		return primaryRep.getStartPosition();
	}
	
	@Override
	public Iterable<O2WEleConnector> getEleConnectors() {
		return emptyList();
	}
	
	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) {
		
		@SuppressWarnings("unchecked")
		List<List<VectorXZ>> lines = asList(
				primaryRep.getCenterlineXZ(),
				primaryRep.getOutlineXZ(true),
				primaryRep.getOutlineXZ(false));
		
		SimplePolygonXZ outlinePolygonXZ = primaryRep.getOutlinePolygonXZ();
		
		/* ensure a minimum vertical distance to ways and areas below,
		 * at intersections */
		
		for (MapOverlap<?,?> overlap : segment.getOverlaps()) {
			
			MapElement other = overlap.getOther(segment);
			WorldObject otherWO = other.getPrimaryRepresentation();
			
			if (otherWO == null
					|| otherWO.getGroundState() != ON)  //TODO remove the ground state check
				continue;
			
			boolean thisIsUpper = this.getGroundState() == ABOVE; //TODO check layers
			
			double distance = 10.0; //TODO base on clearing
			
			if (overlap instanceof MapIntersectionWW) {
				
				MapIntersectionWW intersection = (MapIntersectionWW) overlap;
				
				if (otherWO instanceof AbstractNetworkWaySegmentWorldObject) {
					
					AbstractNetworkWaySegmentWorldObject otherANWSWO =
							((AbstractNetworkWaySegmentWorldObject)otherWO);
					
					O2WEleConnector thisConn = primaryRep.getEleConnectors()
							.getConnector(intersection.pos);
					O2WEleConnector otherConn = otherANWSWO.getEleConnectors()
							.getConnector(intersection.pos);
					
					if (thisIsUpper) {
						enforcer.requireVerticalDistance(
								MIN, distance, thisConn, otherConn);
					} else {
						enforcer.requireVerticalDistance(
								MIN, distance, otherConn, thisConn);
					}
					
				}
				
			} else if (overlap instanceof MapOverlapWA) {
				
				/*
				 * require minimum distance at intersection points
				 * (these have been inserted into this segment,
				 * but not into the area)
				 */
				
				MapOverlapWA overlapWA = (MapOverlapWA) overlap;
				
				if (overlap.type == MapOverlapType.INTERSECT
						&& otherWO instanceof AbstractAreaWorldObject) {
					
					AbstractAreaWorldObject otherAAWO =
							((AbstractAreaWorldObject)otherWO);
					
					for (int i = 0; i < overlapWA.getIntersectionPositions().size(); i++) {
						
						VectorXZ pos =
								overlapWA.getIntersectionPositions().get(i);
						MapAreaSegment areaSegment =
								overlapWA.getIntersectingAreaSegments().get(i);
						
						O2WEleConnector thisConn = primaryRep.getEleConnectors()
								.getConnector(pos);
						
						O2WEleConnector base1 = otherAAWO.getEleConnectors()
								.getConnector(areaSegment.getStartNode().getPos());
						O2WEleConnector base2 = otherAAWO.getEleConnectors()
								.getConnector(areaSegment.getEndNode().getPos());
												
						if (thisConn != null && base1 != null && base2 != null) {
							
							if (thisIsUpper) {
								enforcer.requireVerticalDistance(MIN, distance,
										thisConn, base1, base2);
							} else {
								enforcer.requireVerticalDistance(MAX, -distance,
										thisConn, base1, base2);
							}
							
						}
						
					}
					
				}
				
				/*
				 * require minimum distance to the area's elevation connectors.
				 * There is usually no direct counterpart for these in this segment.
				 * Examples include trees on terrain above tunnels.
				 */
				
				/*11.11.21if (!(otherWO instanceof Forest)) continue; //TODO enable and debug for other WO classes*/
				
				eleConnectors:
				for (O2WEleConnector c : otherWO.getEleConnectors()) {
					
					if (outlinePolygonXZ == null ||
							!outlinePolygonXZ.contains(c.pos))
						continue eleConnectors;
					
					for (List<VectorXZ> line : lines) {
						for (int i = 0; i+1 < line.size(); i++) {
							
							VectorXZ v1 = line.get(i);
							VectorXZ v2 = line.get(i+1);
							
							if (isBetween(c.pos, v1, v2)) {
								
								O2WEleConnector base1 = primaryRep.getEleConnectors().getConnector(v1);
								O2WEleConnector base2 = primaryRep.getEleConnectors().getConnector(v2);
								
								if (base1 != null && base2 != null) {
									
									if (thisIsUpper) {
										enforcer.requireVerticalDistance(
												MAX, -distance,
												c, base1, base2);
									} else {
										enforcer.requireVerticalDistance(
												MIN, distance,
												c, base1, base2);
									}
								
								}
								
								continue eleConnectors;
								
							}
							
						}
					}
					
				}
				
			}
			
		}
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + segment + ")";
	}
	
}
