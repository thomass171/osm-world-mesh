package de.yard.threed;

import de.yard.threed.osm2graph.osm.DefaultTarget;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.OsmOrigin;
import de.yard.threed.osm2world.TriangleXYZ;
import de.yard.threed.osm2world.VectorXYZList;
import de.yard.threed.osm2world.VectorXZ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created on 10.07.18.
 */
public class TestTarget extends DefaultTarget {
    public List<Material> materials = new ArrayList<>();
    public List<Collection<? extends TriangleXYZ>> triangles = new ArrayList<Collection<? extends TriangleXYZ>>();
    public List<VectorXYZList> trianglestrips = new ArrayList<VectorXYZList>();
    public List<List<List<VectorXZ>>> texCoordLists = new ArrayList<List<List<VectorXZ>>>();
    public List<OsmOrigin> rawRenderData = new ArrayList<>();
    public int itemcnt = 0;

    @Override
    public void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles, List<List<VectorXZ>> texCoordLists, OsmOrigin rawRenderData) {
        addDrawItem(material, triangles, texCoordLists, rawRenderData);
    }

    @Override
    public void drawTriangleStrip(Material material, VectorXYZList vs, List<List<VectorXZ>> texCoordLists, OsmOrigin rawRenderData) {
        addDrawItem(material, vs, texCoordLists, rawRenderData);
    }
    
    private void addDrawItem(Material material, Collection<? extends TriangleXYZ> triangles, List<List<VectorXZ>> texCoordLists, OsmOrigin rawRenderData) {
        materials.add(material);
        this.triangles.add(triangles);
        this.texCoordLists.add(texCoordLists);
        this.rawRenderData.add(rawRenderData);
        itemcnt++;
    }

    private void addDrawItem(Material material, VectorXYZList vs, List<List<VectorXZ>> texCoordLists, OsmOrigin rawRenderData) {
        materials.add(material);
        this.trianglestrips.add(vs);
        this.texCoordLists.add(texCoordLists);
        this.rawRenderData.add(rawRenderData);
        itemcnt++;
    }
}
