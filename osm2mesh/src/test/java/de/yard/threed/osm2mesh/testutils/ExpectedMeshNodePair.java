package de.yard.threed.osm2mesh.testutils;

import de.yard.threed.core.Degree;

public class ExpectedMeshNodePair {
    public long osmWayId;
    public Degree heading;
    // heading orientation, meanwhile connector orientation
    public int expectedLeft, expectedRight;

    public ExpectedMeshNodePair(long osmWayId, Degree heading, int expectedLeft, int expectedRight) {
        this.osmWayId = osmWayId;
        this.heading = heading;
        this.expectedLeft = expectedLeft;
        this.expectedRight = expectedRight;
    }

    @Override
    public String toString() {
        return "ExpectedMeshNodePair{" +
                "osmWayId=" + osmWayId +
                ", heading=" + heading +
                ", expectedLeft=" + expectedLeft +
                ", expectedRight=" + expectedRight +
                '}';
    }
}
