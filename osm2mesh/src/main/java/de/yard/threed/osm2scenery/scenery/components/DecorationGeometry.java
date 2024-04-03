package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Geometry;

public class DecorationGeometry implements Decoration {
    Geometry geometry;

    public DecorationGeometry(Geometry geometry){
    this.geometry=geometry;
    }
    @Override
    public Geometry getGeometry() {
        return geometry;
    }
}
