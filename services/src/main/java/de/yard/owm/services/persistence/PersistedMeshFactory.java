package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.polygon20.MeshArea;
import de.yard.threed.osm2scenery.polygon20.MeshFactory;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.polygon20.OsmNode;
import de.yard.threed.osm2scenery.polygon20.OsmWay;
import de.yard.threed.osm2world.MetricMapProjection;
import de.yard.threed.osm2world.O2WOriginMapProjection;
import de.yard.threed.traffic.geodesy.GeoCoordinate;
import org.springframework.security.core.parameters.P;

import java.util.ArrayList;
import java.util.List;

public class PersistedMeshFactory implements MeshFactory {

    public MetricMapProjection projection;
    TerrainMeshManager terrainMeshManager;

    public PersistedMeshFactory(MetricMapProjection projection, TerrainMeshManager terrainMeshManager) {
        this.projection = projection;
        this.terrainMeshManager = terrainMeshManager;
    }

    @Override
    public MeshNode buildMeshNode(Coordinate coordinate) {
        PersistedMeshNode newNode = new PersistedMeshNode(coordinate, projection.unproject(coordinate));
        // persist it to give it an id which is needed for equals.
        terrainMeshManager.persistNode(newNode);
        return newNode;
    }

    @Override
    public MeshLine buildMeshLine(MeshNode from, MeshNode to) {
        return new PersistedMeshLine((PersistedMeshNode) from, (PersistedMeshNode) to);
    }

    @Override
    public MeshArea buildMeshArea() {
        return new PersistedMeshArea();
    }

    @Override
    public OsmNode buildOsmNode(long osmId) {
        PersistedOsmNode osmNode = new PersistedOsmNode();
        osmNode.setOsmId(osmId);
        return null;//osmNode;
    }

    @Override
    public OsmWay buildOsmWay(long osmId, List<Long> osmnodeIds) {
        PersistedOsmWay osmWay = new PersistedOsmWay();
        osmWay.setOsmId(osmId);
        // save early enogh to have id for FKs
        terrainMeshManager.persist(osmWay);
        int index = 0;
        for (long osmNodeId : osmnodeIds) {
            PersistedOsmNode osmNode = terrainMeshManager.findOsmNode(osmNodeId);
            if (osmNode == null) {
                osmNode = new PersistedOsmNode(osmNodeId);
                terrainMeshManager.persist(osmNode);
            }
            osmWay.add(osmNode, index++);
        }
        return osmWay;
    }

    @Override
    public List<MeshLine> buildMeshLines(Coordinate[] coordinates, LineString line) {
        //List<PersistedMeshNode> nodes = new ArrayList<>();
        PersistedMeshNode lastNode = null;
        List<MeshLine> lines = new ArrayList<>();
        for (Coordinate c : coordinates) {
            PersistedMeshNode existingNode = null;//TODO find
            if (existingNode == null) {
                existingNode = new PersistedMeshNode(c,GeoCoordinate.fromLatLon(projection.unproject(c), c.z)/*, projection*/);
            }
            if (lastNode != null) {
                lines.add(new PersistedMeshLine(lastNode, existingNode));
            }
            lastNode = existingNode;
        }
        return lines;
    }

    @Override
    public void deleteMeshLine(MeshLine line) {
        terrainMeshManager.deleteMeshLine((PersistedMeshLine) line);
    }
}
