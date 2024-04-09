package de.yard.threed;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.osm2scenery.polygon20.MeshFactory;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;

import java.util.List;

public class TraditionalMeshFactory implements MeshFactory {
    @Override
    public MeshNode buildMeshNode(Coordinate coordinate) {
        return new TraditionalMeshNode(coordinate);
    }

    @Override
    public List<MeshLine> buildMeshLines(Coordinate[] coordinates, LineString line) {
        return List.of(new TraditionalMeshLine(coordinates, line));
    }
}
