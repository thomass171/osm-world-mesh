package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class CutResult {
    //the two points of grid intersection, otherwise null.
    public Coordinate[] intersectcoorinates;
    public Polygon[] polygons;

    public CutResult(Polygon[] polygons, Coordinate[] intersectcoorinates){
this.polygons=polygons;
this.intersectcoorinates=intersectcoorinates;
    }
}
