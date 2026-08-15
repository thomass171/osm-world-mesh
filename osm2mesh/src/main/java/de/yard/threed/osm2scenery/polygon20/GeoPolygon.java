package de.yard.threed.osm2scenery.polygon20;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.osm2graph.osm.CoordinateList;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2world.JTSConversionUtil;

import java.util.Arrays;
import java.util.List;

import static de.yard.threed.osm2graph.osm.JtsUtil.geoCoordinatesToCoordinates;

/**
 * Nothing really special but just a marker/indicator that this is
 * a polygon where coordinates are not projected but GeoCoordinates.
 */
public class GeoPolygon extends Polygon {

    public GeoPolygon(List<GeoCoordinate> geoCoordinates) throws MeshInconsistencyException {
        this(geoCoordinatesToCoordinates(geoCoordinates).toArray(new Coordinate[0]));
    }

    /**
     * @param coordinates must be closed (last point equals first)
     */
    public GeoPolygon(Coordinate[] coordinates) throws MeshInconsistencyException {
        //11.7.26 proper use of JtsUtil super(new LinearRing(new CoordinateArraySequence(coordinates), JTSConversionUtil.GF), null, JTSConversionUtil.GF);
        super(JtsUtil.createLinearRingFromCoordinateList(new CoordinateList(coordinates), false), null, JTSConversionUtil.GF);
    }

    public static GeoPolygon fromPolygon(Polygon p, SceneryProjection projection) throws MeshInconsistencyException {
        List<GeoCoordinate> geoCoordinates = JtsUtil.unproject(Arrays.stream(p.getCoordinates()).toList(), projection);
        return new GeoPolygon(geoCoordinates);
    }

    /**
     * For cases where polygon isn't projected
     */
    public static GeoPolygon fromPolygon(Polygon p) throws MeshInconsistencyException {
        List<GeoCoordinate> geoCoordinates = JtsUtil.coordinatesToGeoCoordinates(Arrays.stream(p.getCoordinates()).toList());
        return new GeoPolygon(geoCoordinates);
    }
}
