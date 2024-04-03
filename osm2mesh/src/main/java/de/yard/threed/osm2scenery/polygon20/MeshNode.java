package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.Collections;
import java.util.List;

public interface MeshNode {

    Coordinate getCoordinate();

     void addLine(MeshLine line);

     void removeLine(MeshLine line);

     int getLineCount();

     List<MeshLine> getLines();
}
