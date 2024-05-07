package de.yard.threed;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.core.Util;
import de.yard.threed.osm2scenery.polygon20.MeshArea;
import de.yard.threed.osm2scenery.polygon20.MeshFactory;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.polygon20.OsmNode;
import de.yard.threed.osm2scenery.polygon20.OsmWay;

import java.util.List;

public class TraditionalMeshFactory implements MeshFactory {
    @Override
    public MeshNode buildMeshNode(Coordinate coordinate) {
        return new TraditionalMeshNode(coordinate);
    }

    @Override
    public MeshLine buildMeshLine(MeshNode from, MeshNode to) {
        // 15.4.24: Probably not correct
        return new TraditionalMeshLine();
    }

    @Override
    public MeshArea buildMeshArea() {
        Util.notyet();
        return null;
    }

    @Override
    public OsmNode buildOsmNode(long osmId) {
        Util.notyet();
        return null;
    }

    @Override
    public OsmWay buildOsmWay(long osmId, List<Long> osmnodeIds) {
        Util.notyet();
        return null;
    }

    @Override
    public List<MeshLine> buildMeshLines(Coordinate[] coordinates, LineString line) {
        return List.of(new TraditionalMeshLine(coordinates, line));
    }

    @Override
    public void deleteMeshLine(MeshLine line) {
        Util.notyet();
    }
}
