package de.yard.threed.osm2world;


import static de.yard.threed.osm2world.ValueStringParser.parseAngle;
import static de.yard.threed.osm2world.ValueStringParser.parseOsmDecimal;

/**
 * utility class that can be used by { WorldModule}s
 * to interpret frequently used value formats.
 */
public class WorldModuleParseUtil {

	private WorldModuleParseUtil() { }

	/**
	 * returns the value of the first key that exists,
	 * or the fallback value if none of the keys exists
	 */
	public static final String getValueWithFallback(String fallback,
			TagGroup tags, String... keys) {
		for (String key : keys) {
			if (tags.containsKey(key)) {
				return tags.getValue(key);
			}
		}
		return fallback;
	}
	
	/**
	 * retrieves width using (in this priority order)
	 * width tag, est_width tag, defaultValue parameter
	 */
	public static final float parseWidth(TagGroup tags, float defaultValue) {
		return parseMeasure(tags, defaultValue, "width", "est_width");
	}

	/**
	 * retrieves length using (in this priority order)
	 * length tag, defaultValue parameter
	 */
	public static final float parseLength(TagGroup tags, float defaultValue) {
		return parseMeasure(tags, defaultValue, "length");
	}

	/**
	 * retrieves height using (in this priority order)
	 * height tag, building:height tag, est_height tag, defaultValue parameter
	 */
	public static final float parseHeight(TagGroup tags, float defaultValue) {
		return parseMeasure(tags, defaultValue, "height", "building:height", "est_height");
	}

	/**
	 * retrieves clearing using (in this priority order)
	 * practical:maxheight tag, maxheight tag, defaultValue parameter
	 */
	public static final float parseClearing(TagGroup tags, float defaultValue) {
		return parseMeasure(tags, defaultValue, "maxheight:physical", "maxheight");
	}
	
	/**
	 * parses the direction tag and returns the direction
	 * (or a default value) as radians
	 */
	public static final double parseDirection(TagGroup tags, double defaultValue) {
		
		Float directionAngle = null;
		
		if (tags.containsKey("direction")) {
			directionAngle = parseAngle(tags.getValue("direction"));
		}
		
		if (directionAngle != null) {
			return Math.toRadians(directionAngle);
		} else {
			return defaultValue;
		}
		
	}

	public static final int parseInt(TagGroup tags, int defaultValue, String key) {
		if(tags.containsKey(key)) {
			Float value = parseOsmDecimal(tags.getValue(key), false);
			if (value != null) {
				return(int) (float) value;
			}
		}
		return defaultValue;
	}
	
	private static final float parseMeasure(TagGroup tags, float defaultValue,
			String... keys) {

		for (String key : keys) {
			if (tags.containsKey(key)) {
				Float value = ValueStringParser.parseMeasure(tags.getValue(key));
				if (value != null) {
					return value;
				}
			}
		}
		
		return defaultValue;
		
	}
	
	
}
