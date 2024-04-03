package de.yard.threed;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.osm2scenery.polygon20.MeshFactory;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;

public class TraditionalMeshFactory implements MeshFactory {
    @Override
    public MeshNode buildMeshNode(Coordinate coordinate) {
        return new TraditionalMeshNode(coordinate);
    }

    @Override
    public MeshLine buildMeshLine(Coordinate[] coordinates, LineString line) {
        return new TraditionalMeshLine(coordinates, line);
    }
}
