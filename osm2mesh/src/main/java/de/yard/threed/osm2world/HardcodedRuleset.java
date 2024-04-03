package de.yard.threed.osm2world;

import java.util.Collection;
import java.util.HashSet;

/**
 * 9.9.19: Das ist ja für closed Ways->Area Erkennung. Darum durchaus großzügiger.
 */
public class HardcodedRuleset implements Ruleset {

	private static Collection<Tag> areaTags = new HashSet<Tag>();
	private static Collection<String> areaKeys = new HashSet<String>();
	
	private static Collection<Tag> landTags = new HashSet<Tag>();
	private static Collection<Tag> seaTags = new HashSet<Tag>();
	
	static {
		areaTags.add(new Tag("area", "yes"));
		areaTags.add(new Tag("amenity", "fountain"));
		areaTags.add(new Tag("amenity", "parking"));
		areaTags.add(new Tag("amenity", "swimming_pool"));
		areaTags.add(new Tag("leisure", "pitch"));
		areaTags.add(new Tag("leisure", "swimming_pool"));
		areaTags.add(new Tag("natural", "beach"));
		areaTags.add(new Tag("natural", "sand"));
		areaTags.add(new Tag("natural", "water"));
		areaTags.add(new Tag("natural", "wood"));
		areaTags.add(new Tag("natural", "scrub"));
		areaTags.add(new Tag("power", "generator"));
		areaTags.add(new Tag("waterway", "riverbank"));
		//23.4.19: Apron
		areaTags.add(new Tag("aeroway", "apron"));
		
		areaKeys.add("building");
		areaKeys.add("building:part");
		areaKeys.add("golf");
		areaKeys.add("landuse");
		// 9.9.19: leisure ganz allgemein auch. Das kann zwar auch eine Node sein, aber an einem Way soll es immer zu einer Area fuehren (z.B. 48703221)
		areaKeys.add("leisure");
		areaKeys.add("amenity");

		landTags.add(new Tag("landuse", "forest"));
		landTags.add(new Tag("natural", "water"));
		landTags.add(new Tag("natural", "wood"));
		landTags.add(new Tag("waterway", "river"));
		landTags.add(new Tag("waterway", "stream"));

		seaTags.add(new Tag("maritime", "yes"));
		seaTags.add(new Tag("route", "ferry"));
		seaTags.add(new Tag("seamark", "buoy"));
		seaTags.add(new Tag("seamark:type", "buoy_cardinal"));
		seaTags.add(new Tag("seamark:type", "buoy_isolated_danger"));
		seaTags.add(new Tag("seamark:type", "buoy_lateral"));
		seaTags.add(new Tag("seamark:type", "buoy_safe_water"));
		seaTags.add(new Tag("seamark:type", "buoy_special_purpose"));
		seaTags.add(new Tag("seamark:type", "cable_submarine"));
		seaTags.add(new Tag("submarine", "yes"));
		seaTags.add(new Tag("wetland", "tidalflat"));
	}
	
	@Override
	public boolean isAreaTag(Tag tag) {
		return areaKeys.contains(tag.key)
			|| areaTags.contains(tag);
	}
	
	@Override
	public boolean isLandTag(Tag tag) {
		return landTags.contains(tag);
	}
	
	@Override
	public boolean isSeaTag(Tag tag) {
		return seaTags.contains(tag);
	}
		
}
