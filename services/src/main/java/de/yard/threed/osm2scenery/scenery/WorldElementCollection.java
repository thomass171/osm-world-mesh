package de.yard.threed.osm2scenery.scenery;

import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.OsmOrigin;
import de.yard.threed.osm2world.ShapeXZ;
import de.yard.threed.osm2world.TriangleXYZ;
import de.yard.threed.osm2world.VectorXYZ;
import de.yard.threed.osm2world.VectorXYZList;
import de.yard.threed.osm2world.VectorXZ;

import java.util.List;

public class WorldElementCollection {
    public void add(WorldElement worldElement){

    }

    public void drawTriangleStrip(Material materialWallWithWindows, VectorXYZList vectorXYZList, List<List<VectorXZ>> mainWallTexCoordLists, Object o) {

    }

    public void drawTriangles(Material materialWall, List<TriangleXYZ> trianglesXYZ, List<List<VectorXZ>> triangleTexCoordLists, OsmOrigin osmOrigin) {

    }

    public void drawExtrudedShape(Material materialRoof, ShapeXZ spindleShape, List<VectorXYZ> path, List<VectorXYZ> nCopies, List<Double> scaleFactors, List<List<VectorXZ>> spindleTexCoordLists, Object o) {

    }
}
