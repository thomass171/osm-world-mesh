package de.yard.threed.osm2scenery.scenery.components;

import de.yard.threed.osm2scenery.MeshServiceFacade;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;

public interface TerrainMeshAdder extends SceneryObjectComponent {
    // Now disabled for 2026 DB persist
    // public void addToTerrainMesh(AbstractArea[] areas, TerrainMesh tm) throws OsmProcessException, MeshInconsistencyException;
    //void addToTerrainMesh(MeshServiceFacade meshService) throws OsmProcessException, MeshInconsistencyException;
}
