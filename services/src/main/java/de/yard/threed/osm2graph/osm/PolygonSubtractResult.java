package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

import java.util.List;

/**
 * Das Ergebnis einer "subtract" Operation zwischen zwei Polygonen.
 */
public class PolygonSubtractResult {
    public Polygon polygon;
    //Die Seam, die sich durch den Subtract zwischen den beiden Polygonen ergeben hat.
    //Manchmal gibt es aber keine(??)
    // koennte evtl. auch eine Liste sein??
    public /*List<LineString*/ List<Coordinate>seam;

    public PolygonSubtractResult(Polygon difference) {
        this.polygon = difference;
    }

    public PolygonSubtractResult(Polygon difference, List<Coordinate> seam) {
        this.polygon = difference;
        this.seam = seam;
    }

}
