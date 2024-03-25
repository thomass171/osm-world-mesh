package de.yard.threed.osm2scenery.scenery;

import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.Primitive;

import java.util.Collection;

/**
 * Ein (zusaetzliches) (Volume)Teil eines SceneryObject, darum auch keine Ableitung davon.
 * 1.5.19: Die Klasse koennte obsolet sein. Naja, irgendwo muessen die Daten aber zwischengepeichert werden.
 * Und wie ist die Abgrnezung zu Supplement? 3.6.19: Supplement ist doch Fläche, das hier Volume.
 */
public class WorldElement {
    public Material material;
    public VertexData vertexData;
    private String name;

    public WorldElement(String name, Material material, Collection<Primitive> primitives) {
        this.material = material;
        this.name = name;
        for (Primitive primitive : primitives) {
            VertexData vd = new VertexData(primitive);
            // 2.5.19: Ist z.Z. alles fuer Buildings, und fuer die soll vorerst FlatShading verwendet werden. Darum die Normalen nullen
            vd.normals=null;
            if (vertexData == null) {
                vertexData = vd;
            } else {
                vertexData.add(vd);
            }
        }
    }

    public String getName() {
        return name;
    }
}
