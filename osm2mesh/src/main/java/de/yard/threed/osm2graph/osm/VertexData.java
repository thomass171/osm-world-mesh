package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.Vector3;
import de.yard.threed.osm2world.*;
import org.apache.log4j.Logger;


import java.util.ArrayList;
import java.util.List;

/**
 * Ist es gueltig, keine uvs zu haben? Vielleicht ohne Texturen?
 * 1.5.19: Klar. Aber normals fehlen. Zumindest optional. Wie uvs.
 * Created on 13.07.18.
 */
public class VertexData {
    Logger logger = Logger.getLogger(VertexData.class);

    public List<VectorXZ> uvs = null;
    public List<Coordinate> vertices;
    public List<Vector3> normals = null;
    public int[] indices;

    public VertexData(List<Coordinate> vertices, int[] indices, List<VectorXZ> uvs) {
        if (vertices == null) {
            throw new RuntimeException("vertices isType null");
        }
        if (uvs == null) {
            //allowed. uvs are set later.
        }
        this.vertices = vertices;
        this.indices = indices;
        this.uvs = uvs;
    }

    public VertexData(Primitive primitive) {
        int cnt = primitive.vertices.size();
        vertices = new ArrayList<>(cnt);
        normals = new ArrayList<>(cnt);
        if (primitive.texCoordLists.size() == 0) {
            // Material ohne Texture
        } else {
            //Warum gibt es wohl mehrere?
            List<VectorXZ> uvlist = primitive.texCoordLists.get(0);
            uvs = new ArrayList(cnt);
            for (int i = 0; i < cnt; i++) {
                uvs.add(uvlist.get(i));
            }
        }
        for (int i = 0; i < cnt; i++) {
            VectorXYZ v = primitive.vertices.get(i);
            vertices.add(new Coordinate(v.getX(), v.getZ(), v.getY()));
        }
        for (int i = 0; i < cnt; i++) {
            VectorXYZ v = primitive.normals.get(i);
            normals.add(new Vector3(v.getX(), v.getZ(), v.getY()));
        }
        switch (primitive.type) {
            case TRIANGLES:
                indices = createTriangleIndices(vertices);
                break;
            case TRIANGLE_STRIP:
                indices = createTriangleStripIndices(vertices);
                break;

            default:
                logger.error("unknown type " + primitive.type);
                //TODO andere Typen
        }
        String msg = validate();
        if (msg != null) {
            logger.error("" + msg);
        }
    }

    public String validate() {
        if (uvs != null && vertices.size() != uvs.size()) {
            return "vertices.size(" + vertices.size() + ")!=uvs.size(" + uvs.size() + ")";
        }
        if (normals != null && vertices.size() != normals.size()) {
            return "vertices.size(" + vertices.size() + ")!=normals.size(" + normals.size() + ")";
        }
        return null;
    }

    public void add(VertexData vd) {
        if ((uvs == null) != (vd.uvs == null)) {
            logger.error("inconsistent uv lists. Ignroring add.");
            return;
        }
        if ((normals == null) != (vd.normals == null)) {
            logger.error("inconsistent normals lists. Ignroring add.");
            return;
        }
        int vsize=vertices.size();
        vertices.addAll(vd.vertices);
        if (normals != null) {
            normals.addAll(vd.normals);
        }
        if (uvs != null && vd.uvs != null) {
            uvs.addAll(vd.uvs);
        }
        int size = indices.length;
        int[] nindices = new int[indices.length + vd.indices.length];
        for (int i = 0; i < size; i++) {
            nindices[i] = indices[i];
        }
        for (int i = 0; i < vd.indices.length; i++) {
            //26.4.19: auch verschieben (+size), 6.5.19:aber um vertexsize
            nindices[size + i] = vd.indices[i] + vsize;//size;
        }
        indices = nindices;
    }

    public Vector2 getUV(int i) {
        return OsmUtil.toVector2(uvs.get(i));
    }

    /**
     * Der uebergebene Way Polygon ist counterclockwise. Begonnen wird "links/oben" (OSM2World Konvention?) und weil
     * die TexCoordFunction so davon ausgeht.
     * 26.4.19
     * In JtsUtil gibts was aehnliches.
     */
    public int[] createTriangleStripIndices(List<Coordinate> vertices) {
        if (vertices.size() % 2 == 1) {
            logger.error("not a triangle strip ");
            return null;
        }
        int len = vertices.size() / 2;
        int trianglecnt = (len - 1) * 2;

        int[] indices = new int[trianglecnt * 3];

        int offset = 0;
        for (int i = 0; i < (trianglecnt / 2) * 6; i += 6) {
            indices[i] = offset + 0;
            indices[i + 1] = offset + 1;
            indices[i + 2] = offset + 2;
            indices[i + 3] = offset + 2;
            indices[i + 4] = offset + 1;
            indices[i + 5] = offset + 3;
            //offset 2 ist richtig, weil die Vorgaenger ja wiederverwendet werden.
            offset += 2;
        }

        return indices;
    }

    public int[] createTriangleIndices(List<Coordinate> vertices) {
        if (vertices.size() % 3 != 0) {
            logger.error("not a triangle list ");
            return null;
        }
        int trianglecnt = vertices.size() / 3;

        int[] indices = new int[trianglecnt * 3];

        int offset = 0;
        for (int i = 0; i < trianglecnt; i++) {
            indices[3 * i] = offset + 0;
            indices[3 * i + 1] = offset + 1;
            indices[3 * i + 2] = offset + 2;
            offset += 3;
        }

        return indices;
    }

}
