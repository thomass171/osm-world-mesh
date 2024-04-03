package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.polygon20.MeshFactory;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2world.MetricMapProjection;
import de.yard.threed.osm2world.O2WOriginMapProjection;

public class PersistedMeshFactory implements MeshFactory {

    MetricMapProjection projection;

    public PersistedMeshFactory(MetricMapProjection projection){
        this.projection = projection;
    }
    @Override
    public MeshNode buildMeshNode(Coordinate coordinate) {
        return new PersistedMeshNode(coordinate, projection);
    }

    @Override
    public MeshLine buildMeshLine(Coordinate[] coordinates, LineString line) {
        return new PersistedMeshLine(coordinates, line);
    }
}
