package de.yard.threed.osm2world;

import java.util.Collection;


public class MapIntersectionGrid implements MapDataIndex {
	
	private final IntersectionGrid<MapElement> intersectionGrid;
			
	public MapIntersectionGrid(AxisAlignedBoundingBoxXZ dataBoundary) {
		
		AxisAlignedBoundingBoxXZ gridBounds = dataBoundary.pad(10);
		
		intersectionGrid = new IntersectionGrid<MapElement>(
				gridBounds,
				gridBounds.sizeX() / 50,
				gridBounds.sizeZ() / 50);
		
	}
	
	@Override
	public void insert(MapElement e) {
		intersectionGrid.insert(e);
	}
	
	@Override
	public Collection<? extends Iterable<MapElement>> insertAndProbe(MapElement e) {
		insert(e);
		return intersectionGrid.cellsFor(e);
	}
	
	@Override
	public Iterable<? extends Iterable<MapElement>> getLeaves() {
		return intersectionGrid.getCells();
	}
	
}
