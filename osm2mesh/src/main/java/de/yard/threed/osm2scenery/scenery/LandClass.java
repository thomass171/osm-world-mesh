package de.yard.threed.osm2scenery.scenery;

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

    public String getCode() {
        return code;
    }
}
