package de.yard.threed.osm2scenery.scenery;

public class OsmProcessException extends Exception {
    public OsmProcessException(String msg) {
        super(msg);
    }

    public OsmProcessException(Exception e) {
        super(e);
    }
}
