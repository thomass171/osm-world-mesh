package de.yard.threed.scenery.util;

import de.yard.threed.core.*;
import de.yard.threed.trafficcore.EllipsoidCalculations;
import de.yard.threed.trafficcore.ElevationProvider;

/**
 * 21.9.23: What does "Simple" mean? The whole class is a fake.
 */
public class SimpleRoundBodyCalculations extends EllipsoidCalculations {

   /* @Override
    public Quaternion buildRotation(GeoCoordinate location, Degree heading, Degree pitch) {
        return null;
    }*/

    @Override
    public Vector3 getNorthHeadingReference(GeoCoordinate location) {
        return null;
    }

    @Override
    public GeoCoordinate fromCart(Vector3 cart) {
        return null;
    }

    @Override
    public Vector3 toCart(GeoCoordinate geoCoordinate, ElevationProvider elevationprovider, GeneralParameterHandler<GeoCoordinate> missingElevationHandler) {
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
