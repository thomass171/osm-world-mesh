package de.yard.threed.osm2world;

/**
 * overlap between a {@link MapWaySegment} and a {@link MapArea} ("Way-Area").
 * The way either intersects with the area
 * or is completely contained within the area.
 */
public class MapOverlapNA extends MapOverlap<MapNode, MapArea> {
	
	public MapOverlapNA(MapNode node, MapArea area, MapOverlapType type) {
		super(node, area, type);
	}
	
}
