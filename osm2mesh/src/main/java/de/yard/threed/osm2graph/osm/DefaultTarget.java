package de.yard.threed.osm2graph.osm;

import de.yard.threed.osm2world.*;
import org.apache.commons.configuration2.Configuration;


import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Gedacht fuer z.B. Tests. Darum abstract.
 * 
 * Created on 15.06.18.
 */
public abstract class DefaultTarget implements Target<RenderableToAllTargets> {
    @Override
    public Class<RenderableToAllTargets> getRenderableType() {
        return RenderableToAllTargets.class;
    }

    @Override
    public void setConfiguration(Configuration config) {

    }

    @Override
    public void render(RenderableToAllTargets renderable) {
        renderable.renderTo(this);
    }

    @Override
    public void beginObject(WorldObject object) {

    }

    @Override
    public void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles, List<List<VectorXZ>> texCoordLists, OsmOrigin rawRenderData) {
    }

    @Override
    public void drawTrianglesWithNormals(Material material, Collection<? extends TriangleXYZWithNormals> triangles, List<List<VectorXZ>> texCoordLists) {

    }

    @Override
    public void drawTriangleStrip(Material material, VectorXYZList vs, List<List<VectorXZ>> texCoordLists, OsmOrigin rawRenderData) {

    }

    @Override
    public void drawTriangleFan(Material material, List<VectorXYZ> vs, List<List<VectorXZ>> texCoordLists) {

    }

    @Override
    public void drawConvexPolygon(Material material, List<VectorXYZ> vs, List<List<VectorXZ>> texCoordLists) {

    }

    @Override
    public void drawShape(Material material, SimpleClosedShapeXZ shape, VectorXYZ point, VectorXYZ frontVector, VectorXYZ upVector) {

    }

    @Override
    public void drawExtrudedShape(Material material, ShapeXZ shape, List<VectorXYZ> path, List<VectorXYZ> upVectors, List<Double> scaleFactors, List<List<VectorXZ>> texCoordLists, EnumSet<ExtrudeOption> options) {

    }

    @Override
    public void drawBox(Material material, VectorXYZ bottomCenter, VectorXZ faceDirection, double height, double width, double depth) {

    }

    @Override
    public void drawColumn(Material material, Integer corners, VectorXYZ base, double height, double radiusBottom, double radiusTop, boolean drawBottom, boolean drawTop) {

    }

    @Override
    public void finish() {

    }
}
