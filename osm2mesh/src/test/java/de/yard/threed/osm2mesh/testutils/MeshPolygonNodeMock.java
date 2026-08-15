package de.yard.threed.osm2mesh.testutils;

import de.yard.threed.osm2scenery.polygon20.MeshPolygonNode;

public class MeshPolygonNodeMock implements MeshPolygonNode {
    private final int index;
    private final MeshNodeMock meshNode;

    public MeshPolygonNodeMock(int index, MeshNodeMock meshNode) {
        this.index = index;
        this.meshNode = meshNode;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public MeshNodeMock getMeshNode() {
        return meshNode;
    }
}
