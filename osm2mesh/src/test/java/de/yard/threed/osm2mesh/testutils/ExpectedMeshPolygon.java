package de.yard.threed.osm2mesh.testutils;

import de.yard.threed.osm2scenery.polygon20.MeshPolygonType;

public class ExpectedMeshPolygon {
    public final int nodes;
    public MeshPolygonType type;
    public Long osmId;
    public int segmentIndex;
    public ExpectedMeshNodePair[] expectedMeshNodePairs = new ExpectedMeshNodePair[0];

    /**
     * @param nodes exclusive the closing one, so corresponds to the number of nodes in OSM for simple polygons.
     */
    private ExpectedMeshPolygon(Long osmId, int segmentIndex, MeshPolygonType type, int nodes) {
        this.osmId = osmId;
        this.segmentIndex = segmentIndex;
        this.type = type;
        this.nodes = nodes;
    }

    public static ExpectedMeshPolygon expectedBoundary(int nodes) {
        return new ExpectedMeshPolygon(null, -1, MeshPolygonType.BOUNDARY, nodes);
    }

    public static ExpectedMeshPolygon expectedWay(long osmId, int segmentIndex, int nodes) {
        return new ExpectedMeshPolygon(osmId, segmentIndex, MeshPolygonType.WAY, nodes);
    }

    public static ExpectedMeshPolygon expectedArea(long osmId, MeshPolygonType type, int nodes) {
        return new ExpectedMeshPolygon(osmId, 0, type, nodes);
    }

    public static ExpectedMeshPolygon expectedConnector(long osmId, int nodes, ExpectedMeshNodePair... expectedMeshNodePairs) {
        ExpectedMeshPolygon emp = new ExpectedMeshPolygon(osmId, -1, MeshPolygonType.CONNECTOR, nodes);
        emp.expectedMeshNodePairs = expectedMeshNodePairs;
        return emp;
    }
}
