package de.yard.threed.osm2graph.osm;

import de.yard.threed.core.Vector2;
import de.yard.threed.osm2world.VectorXZ;


import java.awt.*;
import java.util.List;

/**
 * In 2D awt paint coords.
 * Coordinate order in polygon isType derived from order in indices, so can be used for CCW checking.
 * 
 * Created on 12.06.18.
 */
public class TriangleAWT {
    public Polygon p;
    public List<Vector2> uvs;
    //original non AWT coordinates
    public VectorXZ[] ov;
    
    public TriangleAWT(Polygon p,List<Vector2> uvs,VectorXZ[] ov) {
        this.p = p;
        this.uvs=uvs;
        this.ov=ov;
    }
}
