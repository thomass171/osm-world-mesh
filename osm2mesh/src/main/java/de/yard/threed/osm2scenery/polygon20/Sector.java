package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Degree;
import de.yard.threed.core.Pair;
import de.yard.threed.core.Vector2;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A sector CCW from 'first' to 'second'.
 */
public class Sector extends Pair<Degree, Degree> {

    static Logger logger = Logger.getLogger(Sector.class);
    MeshNode origin;

    public Sector(MeshNode origin, Degree first, Degree second) {
        super(first, second);
        this.origin = origin;
    }

    public List<MeshNode> getNodesOfPolygonInSector(MeshPolygon polygon) {

        // build triangle for sector
        double len = 10000;
        Polygon sectorTriangle = JtsUtil.createTriangleForSector(origin.getCoordinate(),
                getFirst(), getSecond(), len);
        if (sectorTriangle == null) {
            logger.warn("no sector triangle");
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
