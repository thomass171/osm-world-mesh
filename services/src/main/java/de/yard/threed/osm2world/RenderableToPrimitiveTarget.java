package de.yard.threed.osm2world;

public interface RenderableToPrimitiveTarget extends Renderable {
	
	public void renderTo(PrimitiveTarget<?> target);
	
}
