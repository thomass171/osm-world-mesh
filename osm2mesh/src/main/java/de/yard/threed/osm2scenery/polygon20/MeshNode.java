package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;

import java.util.Collections;
import java.util.List;

public interface MeshNode {

    Coordinate getCoordinate();

    /**
     * Just adding the line to the node. No line creation.
     */
    void addLine(MeshLine line);

    /**
     * Just removing the line from the node. No line removal.
     */
    void removeLine(MeshLine line);

    int getLineCount();

    List<MeshLine> getLines();

    // 28.4.24 wierd, no good idea . Only the caller knows what a good label is
    String getLabel();
}
