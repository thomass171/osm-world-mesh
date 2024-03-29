package de.yard.owm.services.persistence;

import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The only instance that should know repositories.
 */
@Service
public class TerrainMeshManager {

    @Autowired
    private MeshNodeRepository meshNodeRepository;

    @Autowired
    private MeshLineRepository meshLineRepository;

    public TerrainMesh loadTerrainMesh(GridCellBounds gridCellBounds) {
        TerrainMesh terrainMesh = TerrainMesh.init(gridCellBounds);
        // TODO filter mesh inside grid
        meshNodeRepository.findAll().forEach(n->terrainMesh.points.add(n));
        meshLineRepository.findAll().forEach(l->terrainMesh.lines.add(l));
        // TODO make sure to have full outline in mesh
        return terrainMesh;
    }
}
