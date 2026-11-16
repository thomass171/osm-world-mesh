package de.yard.threed.osm2scenery.scenery;

import de.yard.threed.osm2world.TagGroup;

/**
 * Persisted via its code (see getCode()/fromDbValue()), the same way MeshPolygonType handles its database value.
 */
public enum LandClass {
    //River,lake,sea,bach
    WATER("Wt"),
    RIVER("Rv"),
    LAKE("Lk"),
    Sea("Sea"),
    Asphalt("As"),
    FARMLAND("Fl"),
    ;
    //asphalt, kies,pflaster,sand,beton,parkplatz
    //forest,gras,busch,rasen,golf,acker

    private final String code;

    LandClass(String code) {
        this.code = code;
    }

    public static LandClass fromDbValue(String dbValue) {
        for (LandClass c : values()) {
            if (c.getCode().equals(dbValue)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown DB value: " + dbValue);
    }

    /**
     * Derives the land class from the OSM tags of an area (eg. of a MapArea).
     * Returns null if no land class is known for the tags.
     * Not intended for ways, only for areas.
     */
    public static LandClass osmTagsToLandClass(TagGroup tags) {
        if (tags == null) {
            return null;
        }
        if (tags.contains("landuse", "farmland")) {
            return FARMLAND;
        }
        return null;
    }

    public String getCode() {
        return code;
    }
}
