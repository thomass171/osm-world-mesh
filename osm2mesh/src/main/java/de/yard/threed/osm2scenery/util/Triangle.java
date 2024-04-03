package de.yard.threed.osm2scenery.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.math.Vector2D;
import de.yard.threed.osm2graph.osm.JtsUtil;

/**
 * Helper class for easy vertex ordering. Brauch ich aber nicht.
 * Created on 23.08.18.
 */
@Deprecated
public class Triangle {
    Coordinate c0, c1,  c2;
    
    public Triangle(Coordinate c0, Coordinate c1, Coordinate c2){
        this.c0=c0;
        Vector2D v0 =new Vector2D(c1).subtract(new Vector2D(c0));
        Vector2D v1 =new Vector2D(c2).subtract(new Vector2D(c0));
        double cross = JtsUtil.getCrossProduct(v0,v1);
        if (cross < 0){
            this.c1 = c2;
            this.c2 = c1;
        }else {
            this.c1 = c1;
            this.c2 = c2;
        }
    }
}
