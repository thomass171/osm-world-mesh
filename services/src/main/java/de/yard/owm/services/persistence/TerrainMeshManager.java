package de.yard.owm.services.persistence;

import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.OsmWay;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The only instance that should know repositories.
 */
@Service
public class TerrainMeshManager {

    @Autowired
    private MeshNodeRepository meshNodeRepository;

    @Autowired
    private MeshPolygonRepository meshPolygonRepository;

    @Autowired
    private MeshAreaRepository meshAreaRepository;

    @Autowired
    private OsmWayRepository osmWayRepository;

    @Autowired
    private OsmNodeRepository osmNodeRepository;

    @Autowired
    private MeshRepository meshRepository;



    /**
     * We should rely on Hibernates autosave
     * @param terrainMesh
     * @throws MeshInconsistencyException
     */
    public void persist(TerrainMesh terrainMesh) throws MeshInconsistencyException {
        terrainMesh.validate();
        terrainMesh.areas.forEach(a -> {
            if (a.getOsmWay() != null) {
                osmWayRepository.save((PersistedOsmWay) a.getOsmWay());
            }
            meshAreaRepository.save((PersistedMeshArea) a);
        });
        terrainMesh.points.forEach(p -> meshNodeRepository.save((PersistedMeshNode) p));
        //terrainMesh.lines.forEach(p -> meshLineRepository.save((PersistedMeshLine) p));
    }

    public void deleteMeshLine(PersistedMeshLine line) {
       // meshLineRepository.delete(line);
    }

    public void persistNode(PersistedMeshNode node) {
        meshNodeRepository.save(node);
    }

    public List<OsmWay> findOsmWays() {
        List<OsmWay> result = new ArrayList<>();
        osmWayRepository.findAll().forEach(w -> result.add(w));
        return result;
    }

    public PersistedOsmNode findOsmNode(long osmNodeId) {
        return osmNodeRepository.findByOsmId(osmNodeId);
    }

    public void persist(PersistedOsmWay osmWay) {
        osmWayRepository.save(osmWay);
    }

    public void persist(PersistedOsmNode osmNode) {
        osmNodeRepository.save(osmNode);
    }

    public PersistedMesh findMesh(String meshName) {
        return meshRepository.findByName(meshName);
    }
}
