package de.yard.threed.scenery.util;

import de.yard.threed.core.Degree;
import de.yard.threed.core.LatLon;
import de.yard.threed.core.Quaternion;
import de.yard.threed.core.Vector3;
import de.yard.threed.traffic.EllipsoidCalculations;
import de.yard.threed.traffic.geodesy.ElevationProvider;
import de.yard.threed.traffic.geodesy.GeoCoordinate;

/**
 * 21.9.23: What does "Simple" mean? The whole class is a fake.
 */
public class SimpleRoundBodyCalculations implements EllipsoidCalculations {

    @Override
    public Quaternion buildRotation(GeoCoordinate location, Degree heading, Degree pitch) {
        return null;
    }

    @Override
    public Vector3 getNorthHeadingReference(GeoCoordinate location) {
        return null;
    }

    @Override
    public GeoCoordinate fromCart(Vector3 cart) {
        return null;
    }

    @Override
    public Vector3 toCart(GeoCoordinate geoCoordinate, ElevationProvider elevationprovider) {
        return null;
    }

    @Override
    public Vector3 toCart(GeoCoordinate geoCoordinate) {
        return null;
    }

    /**
     * Needed by scenery:OsmTestDataBuider
     */
    @Override
    public LatLon applyCourseDistance(LatLon latLon, Degree coursedeg, double dist) {
        return null;
    }

    @Override
    public Degree courseTo(LatLon latLon, LatLon dest) {
        return null;
    }

    @Override
    public double distanceTo(LatLon latLon, LatLon dest) {
        return 0;
    }
}
