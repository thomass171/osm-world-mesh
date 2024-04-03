package de.yard.threed.osm2tex;

import com.vividsolutions.jts.geom.Geometry;
import de.yard.threed.jts.Sample;

/**
 * Created by thomass on 30.01.17.
 */
public class TileBuilder {
    public static Tile buildSample(){
        Tile tile = new Tile();
        Geometry geo = Sample.buildSampleA();
        tile.drawGeometry(geo);
        tile.drawGeometry(Sample.buildSampleB());
        return tile;
    }
}
