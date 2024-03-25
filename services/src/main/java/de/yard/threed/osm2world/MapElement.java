package de.yard.threed.osm2world;



import java.util.Collection;
import java.util.List;

public interface MapElement extends IntersectionTestObject {
	
	public int getLayer();
	
	/**
	 * returns the visual representations of this element.
	 * 
	 * The order should match the order in which they were added,
	 * so that dependencies are preserved (elements that depend on
	 * another element should be placed after that element).
	 * The first element is considered the "primary" representation,
	 * and for some purposes - such as elevation calculation -, only this
	 * representation will be used.
	 */
	public List<? extends WorldObject> getRepresentations();
	
	/**
	 * returns the primary representation, or null if the object doesn't have any.
	 * @see #getRepresentations()
	 */
	public WorldObject getPrimaryRepresentation();
	
	/**
	 * returns all overlaps between this {@link MapElement}
	 * and other {@link MapElement}s.
	 */
	public Collection<MapOverlap<? extends MapElement, ? extends MapElement>> getOverlaps();

	/**
	 * returns the tags of the underlying {@link OSMElement}
	 */
	TagGroup getTags();
	
	long getOsmId();
	
}
