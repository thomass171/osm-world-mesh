package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Degree;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2graph.osm.JtsUtil;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A sector CCW from 'first' to 'second'.
 */
@Slf4j
public class Sector extends Pair<Degree, Degree> {

    MeshNode origin;

    public Sector(MeshNode origin, Degree first, Degree second) {
        super(first, second);
        this.origin = origin;
    }

    public List<MeshNode> getNodesOfPolygonInSector(MeshPolygonOld polygon) {

        // build triangle for sector
        double len = 10000;
        Polygon sectorTriangle = JtsUtil.createTriangleForSector(origin.getCoordinate(),
                getFirst(), getSecond(), len);
        if (sectorTriangle == null) {
            log.warn("no sector triangle");
            return Collections.EMPTY_LIST;
        }

        List<MeshNode> result = new ArrayList<>();
        for (MeshNode n : polygon.getNodes()) {
            if (JtsUtil.contains(sectorTriangle, n.getCoordinate())) {
                result.add(n);
            }
        }
        return result;
    }

    public Sector reduce(Degree destinationAngle) {
        Pair<Degree, Degree> newSec = JtsUtil.reduceSector(this, destinationAngle);
        return new Sector(origin, newSec.getFirst(), newSec.getSecond());
    }
}
