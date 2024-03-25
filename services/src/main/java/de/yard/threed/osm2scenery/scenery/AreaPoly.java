package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2scenery.util.PolygonMetadata;

/**
 * nur son Container
 * Created on 01.08.18.
 */
public class AreaPoly {
    //Polygon statt Geometry wegen möglicher Holes. Holes lassen sich nie vermeiden, die können immer beim union entstehen.
    public Polygon poly;
    //original polygon not cut by grid cell
    public Polygon uncutPolygon;

    public PolygonMetadata polygonMetadata;

}
