package de.yard.threed.osm2world;

/**
 * representation that only uses methods from {@link Target}
 * and can therefore render to all targets supporting these features
 */
public interface RenderableToAllTargets extends Renderable {
	
	public void renderTo(Target<?> target);
	
}
