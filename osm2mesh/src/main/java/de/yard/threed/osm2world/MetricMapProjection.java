package de.yard.threed.osm2world;


import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.traffic.geodesy.GeoCoordinate;

import static de.yard.threed.osm2world.MercatorProjection.*;

/**
 * Map projection that is intended to use the "dense" space
 * of floating point values by making all coordinates relative to
 * the origin. 1 meter distance is roughly represented by 1 internal unit.
 */
public class MetricMapProjection extends O2WOriginMapProjection {

    private double originX;
    private double originY;
    private double scaleFactor;

    public MetricMapProjection(LatLon origin){
        setOrigin(origin);
    }

    public VectorXZ calcPos(double lat, double lon) {

        if (origin == null) throw new IllegalStateException("the origin needs to be set first");

        double x = lonToX(lon) * scaleFactor - originX;
        double y = latToY(lat) * scaleFactor - originY;

        /* snap to som cm precision, seems to reduce geometry exceptions */
        x = Math.round(x * 1000) / 1000.0d;
        y = Math.round(y * 1000) / 1000.0d;

        return new VectorXZ(x, y); // x and z(!) are 2d here
    }

    @Override
    public VectorXZ calcPos(LatLon latlon) {
        return calcPos(latlon.getLatDeg().getDegree(), latlon.getLonDeg().getDegree());
    }

    @Override
    public double calcLat(VectorXZ pos) {

        if (origin == null) throw new IllegalStateException("the origin needs to be set first");

        return yToLat((pos.z + originY) / scaleFactor);

    }

    @Override
    public double calcLon(VectorXZ pos) {

        if (origin == null) throw new IllegalStateException("the origin needs to be set first");

        return xToLon((pos.x + originX) / scaleFactor);

    }

    @Override
    public VectorXZ getNorthUnit() {
        return VectorXZ.Z_UNIT;
    }

    @Override
    public void setOrigin(LatLon origin) {
        super.setOrigin(origin);

        this.scaleFactor = earthCircumference(origin.getLatDeg().getDegree());
        this.originY = latToY(origin.getLatDeg().getDegree()) * scaleFactor;
        this.originX = lonToX(origin.getLonDeg().getDegree()) * scaleFactor;
    }

    public Coordinate project(GeoCoordinate geoCoordinate) {
        VectorXZ v = calcPos(geoCoordinate);
        if (geoCoordinate.getElevationM() == null) {
            // Coordinate.z will be NaN.
            return new Coordinate(v.x, v.z);
        } else {
            return new Coordinate(v.x, v.z, geoCoordinate.getElevationM());
        }
    }

    public GeoCoordinate unproject(Coordinate coordinate) {
        VectorXZ v = JtsUtil.fromCoordinate(coordinate);
        LatLon latLon = LatLon.fromDegrees(calcLat(v), calcLon(v));
        if (coordinate.z == Double.NaN) {
            return new GeoCoordinate(latLon.getLatDeg(), latLon.getLonDeg());
        } else {
            return new GeoCoordinate(latLon.getLatDeg(), latLon.getLonDeg(), coordinate.z);
        }
    }
}
