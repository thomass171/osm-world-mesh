package de.yard.threed.osm2world;

import java.util.EnumSet;
import java.util.List;

/**
 * Flags describing available options for
 * {@link Target#drawExtrudedShape(Material, ShapeXZ, List, List, List, List, EnumSet)}.
 */
public enum ExtrudeOption {
	
	/**
	 * determines whether the beginning of the "pipe" created by the extrusion should be capped
	 * or left open. Only works for simple, closed shapes (e.g. polygons or circles).
	 */
	START_CAP,
	
	/**
	 * determines whether the end of the "pipe" created by the extrusion should be capped
	 * or left open. Only works for simple, closed shapes (e.g. polygons or circles).
	 */
	END_CAP
	
}
