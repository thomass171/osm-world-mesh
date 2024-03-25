package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.Vector3;
import de.yard.threed.core.geometry.IndexList;
import de.yard.threed.core.geometry.Primitives;
import de.yard.threed.core.geometry.SimpleGeometry;
import de.yard.threed.osm2world.OsmOrigin;
import de.yard.threed.osm2world.TriangleXYZ;
import de.yard.threed.osm2world.VectorXYZ;
import de.yard.threed.osm2world.VectorXYZList;
import de.yard.threed.osm2world.VectorXZ;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Mapping der OSM2World Model in eine SimpleGeometry, die dann fuer GLTF Erzeugung wie auch Viewer2D
 * verwendet wird.
 * <p>
 * Created on 12.06.18.
 */
public abstract class AbstractTarget /*26.4.19 implements Target<RenderableToAllTargets>*/ {
    static Logger logger = Logger.getLogger(AbstractTarget.class);

    /**
     * abstract, weil bei 3D z negiert werden muss.
     *
     * @param p
     * @return
     */
    abstract Vector3 buildVector3(VectorXYZ p);

    abstract Vector3 buildVector3(Coordinate p);

    //2.5.19 doch fragwürdig abstract Vector3 getDefaultNormal();

    public SimpleGeometry buildGeometryForTriangleStrip(VectorXYZList vs, List<List<VectorXZ>> texCoordLists, OsmOrigin osmOrigin) {
        // Ein TriangleStriup ist immer ein Vielfaches von 2 und geht links/rechts usw bis zum Ende.
        // CCW?
        List<Vector3> vertices = new ArrayList<Vector3>();
        List<Vector2> uvs = new ArrayList<Vector2>();
        //2.5.19 doch fragwürdig List<Vector3> normals = new ArrayList<>();
        IndexList indexes = new IndexList();
        //String creatortag = (osmOrigin != null) ? osmOrigin.creatortag : "";
        // erstmal nur die erste. Bei mhereren Vertices duplizieren? Hmm.
        List<VectorXZ> uvlist = null;
        if (texCoordLists != null && texCoordLists.size() > 0) {
            uvlist = texCoordLists.get(0);
        }
        Vector2 defaultuv = new Vector2();
        for (int i = 0; i < vs.vs.size(); i += 2) {
            VectorXYZ p = vs.vs.get(i);
            Vector3 v3 = buildVector3(p);
            vertices.add(v3);
            p = vs.vs.get(i + 1);
            v3 = buildVector3(p);
            vertices.add(v3);
            if (Math.abs(v3.getY()) > 0.1f) {
                v3 = v3;
            }
            //2.5.19 doch fragwürdig normals.add(getDefaultNormal());
            //2.5.19 doch fragwürdig normals.add(getDefaultNormal());
            if (uvlist != null) {
                uvs.add(buildVector2(uvlist.get(i)));
                uvs.add(buildVector2(uvlist.get(i + 1)));
            } else {
                uvs.add(defaultuv);
                uvs.add(defaultuv);
            }
            if (i > 1) {
                // Wiese aendert sich denn nicht die CCW order durch das Spiegeln von z?
                indexes.add(i - 2, i - 1, i);
                indexes.add(i - 1, i + 1, i);
                //indexes.add(i -2,i, i - 1);
                //indexes.add(i -1,i, i + 1);
            }
        }

        SimpleGeometry geo = new SimpleGeometry(vertices, uvs, null/*normals*/, indexes.getIndices());
        return geo;
    }

    /**
     * Das ist jetzt mal ohne jede Vertex Optimierung
     *
     * @param triangles
     * @param texCoordLists
     * @param osmOrigin
     * @return
     */
    public SimpleGeometry buildGeometryForTriangles(Collection<? extends TriangleXYZ> triangles, List<List<VectorXZ>> texCoordLists, OsmOrigin osmOrigin) {
        List<Vector3> vertices = new ArrayList<Vector3>();
        List<Vector2> uvs = new ArrayList<Vector2>();
        //2.5.19 doch fragwürdig List<Vector3> normals = new ArrayList<>();
        IndexList indexes = new IndexList();
        //String creatortag = (osmOrigin != null) ? osmOrigin.creatortag : "";
        // erstmal nur die erste. Bei mhereren Vertices duplizieren? Hmm.
        List<VectorXZ> uvlist = null;
        if (texCoordLists != null && texCoordLists.size() > 0) {
            uvlist = texCoordLists.get(0);
        }
        Vector2 defaultuv = new Vector2();
        int i = 0;
        for (Iterator<? extends TriangleXYZ> iter = triangles.iterator(); iter.hasNext(); ) {
            TriangleXYZ tri = iter.next();
            Vector3 v1 = buildVector3(tri.v1);
            vertices.add(v1);
            Vector3 v2 = buildVector3(tri.v2);
            vertices.add(v2);
            Vector3 v3 = buildVector3(tri.v3);
            vertices.add(v3);
            //2.5.19 doch fragwürdig normals.add(getDefaultNormal());
            //2.5.19 doch fragwürdig normals.add(getDefaultNormal());
            //2.5.19 doch fragwürdig normals.add(getDefaultNormal());
            if (uvlist != null) {
                uvs.add(buildVector2(uvlist.get(i)));
                uvs.add(buildVector2(uvlist.get(i + 1)));
                uvs.add(buildVector2(uvlist.get(i + 2)));
            } else {
                uvs.add(defaultuv);
                uvs.add(defaultuv);
                uvs.add(defaultuv);
            }
            indexes.add(i, i + 1, i + 2);
            i += 3;
        }

        SimpleGeometry geo = new SimpleGeometry(vertices, uvs, null/*normals*/, indexes.getIndices());
        return geo;
    }

    /**
     * Liefert null bei erkannten Inkonsistenzen, weil es sinnlos ist damit weiterzumachen.
     * 1.5.19: Eine Defaultnormal ist doch eigentlich Unsinn. Wenn keine bebannt sind, einfach keine hinterlegen. TODO LoaderGLTF muss natuerlich konenn.
     */
    public SimpleGeometry buildGeometry(List<Coordinate> coordinates, int[] faces, List<VectorXZ> uvlist, List<Vector3> normals) {
        List<Vector3> vertices = new ArrayList<Vector3>();
        List<Vector2> uvs = new ArrayList<Vector2>();
        //2.5.19 doch fragwürdig List<Vector3> finalnormals = new ArrayList<>();
        Vector2 defaultuv = new Vector2();
        if (uvlist != null && uvlist.size() != coordinates.size()) {
            logger.warn("inconsistent uvlist.");
            return null;//sinnlos new SimpleGeometry(vertices, uvs, normals, new int[]{});
        }
        int i = 0;
        for (Coordinate c : coordinates) {
            Vector3 v1 = buildVector3(c);
            vertices.add(v1);
            //2.5.19 doch fragwürdig finalnormals.add(getDefaultNormal());
            if (uvlist != null) {
                uvs.add(buildVector2(uvlist.get(i)));

            } else {
                uvs.add(defaultuv);
            }
            i++;
        }
        if (normals == null) {
            //2.5.19 doch fragwürdig normals = finalnormals;
        }

        SimpleGeometry geo = new SimpleGeometry(vertices, uvs, normals, faces);
        return geo;
    }


    SimpleGeometry buildGeometryForColumn(Integer corners, VectorXYZ base, double height, double radiusBottom, double radiusTop, boolean drawBottom, boolean drawTop) {
        // Primitives cylinder isType along y axis
        SimpleGeometry geo = Primitives.buildCylinderGeometry((float) radiusTop, (float) radiusBottom, (float) height, 16, 0, (float) Math.PI * 2);
        return geo;
    }

    SimpleGeometry buildGeometryForBox(VectorXYZ bottomCenter, VectorXZ faceDirection, double height, double width, double depth) {
        //TODO: ob die Zuornung w/h/d stimmt? Und die Box muss um die Hälfte noch angehoben werden, in 2D wohl egal.
        SimpleGeometry geo = Primitives.buildBox((float) width, (float) height, (float) depth);
        return geo;
    }

    private Vector2 buildVector2(VectorXZ p) {
        return new Vector2((float) p.x, (float) p.z);
    }


}
