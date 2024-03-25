package de.yard.threed.osm2world;

public class MapAreaCreateException extends Exception {
    public MapAreaCreateException(String msg, InvalidGeometryException e) {
        super(msg,e);
    }
}
