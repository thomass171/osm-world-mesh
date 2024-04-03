package de.yard.threed.osm2scenery.scenery.components;

import de.yard.threed.osm2scenery.scenery.TerrainMesh;

public interface TerrainMeshAdder extends SceneryObjectComponent {
    public void addToTerrainMesh(AbstractArea[] areas, TerrainMesh tm);
}
