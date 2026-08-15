package de.yard.threed.osm2scenery.scenery;

public enum LandClass {
    //River,lake,sea,bach
    WATER("Wt"),
    RIVER("Rv"),
    LAKE("Lk"),
    Sea("Sea"),
    ;
    //asphalt, kies,pflaster,sand,beton,parkplatz
    //forest,gras,busch,rasen,golf,acker

    private final String code;

    LandClass(String code) {
        this.code = code;
    }
}
