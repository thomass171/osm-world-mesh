package de.yard.threed.osm2world;

/** intersection between two {@link MapWaySegment}s ("Way-Way") */
public class MapIntersectionWW extends MapOverlap<MapWaySegment, MapWaySegment> {
	
	public final VectorXZ pos;
	
	public MapIntersectionWW(MapWaySegment line1, MapWaySegment line2, VectorXZ pos) {
		super(line1, line2, MapOverlapType.INTERSECT);
		this.pos = pos;
	}
	
	/**
	 * takes one of the intersecting {@link MapWaySegment}s
	 * and returns the other one
	 */
	public MapWaySegment getOther(MapElement line) {
		return (MapWaySegment) super.getOther(line);
	}
	
}
