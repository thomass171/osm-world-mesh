package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2scenery.util.Poly2TriTriangulationUtil;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.TexCoordFunction;
import de.yard.threed.osm2world.VectorXYZ;
import de.yard.threed.osm2world.VectorXZ;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static de.yard.threed.osm2world.NamedTexCoordFunction.GLOBAL_X_Z;
import static de.yard.threed.osm2world.TexCoordUtil.texCoordLists;

/**
 * Created on 13.07.18.
 */
public class TextureUtil {
    static Logger logger = Logger.getLogger(TextureUtil.class);

    /**
     * erst triangluate und dann texturieren, denn triangulate kann weitere Vertices anlegen.
     */
    public static VertexData triangulateAndTexturizePolygon(Polygon poly, Material material) {
        if (material == null) {
            //Sonderlocke, texutils brauchen material
            return null;
        }
        List<VectorXZ> uvs;
        //Coordinate[] vertices;
        int[] indices;
        //vertices = poly.getCoordinates();
        VertexData vertexData = TextureUtil.triangulate(null, poly);
        if (vertexData == null) {
            return null;
        }

        uvs = TextureUtil.texturizePolygon(poly, vertexData.vertices, material);

        vertexData.uvs = uvs;
        return vertexData;
    }

    public static List<VectorXZ> texturizePolygon(Polygon XXp, Coordinate[] vertices, Material material) {
        //triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z)
        List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
        for (Coordinate c : vertices) {
            //JTS z ist unbenutzt
            vs.add(new VectorXYZ(c.x, 0, c.y));
        }
        List<List<VectorXZ>> uvs = triangleTexCoordLists(vs, material, GLOBAL_X_Z);
        return uvs.get(0);
    }

    public static List<VectorXZ> texturizePolygon(Polygon XXp, List<Coordinate> vertices, Material material) {
        return texturizeVertices(vertices, material, GLOBAL_X_Z);
    }

    /**
     * 11.4.19: Etwas generischer.
     * Die defaulttexCoordFunction wird nur verwendet, wenn Material keine hat.
     *
     * @param vertices
     * @param material
     * @return
     */
    public static List<VectorXZ> texturizeVertices(List<Coordinate> vertices, Material material, TexCoordFunction defaulttexCoordFunction) {
        if (material.getTextureDataList().size() == 0) {
            //no texture defined for material-> No uvs.
            return null;
        }
        List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
        for (Coordinate c : vertices) {
            //JTS z ist unbenutzt
            vs.add(new VectorXYZ(c.x, 0, c.y));
        }
        List<List<VectorXZ>> uvs = triangleTexCoordLists(vs, material, defaulttexCoordFunction);
        if (uvs.size() == 0) {
            logger.error("no uv. No texture defined in config file for material '" + material.getName() + "'?");
            return null;
        }
        return uvs.get(0);
    }

    public static List<List<VectorXZ>> triangleTexCoordLists(List<VectorXYZ> vs, Material material, TexCoordFunction defaultCoordFunction) {
        return texCoordLists(vs, material, defaultCoordFunction);

    }

    /**
     * Aus JTSTriangulationUtil
     * <p>
     * Holes schliesse ich aus? Kann man vielleicht gar nicht. Hmm, aber erstmal.
     * Wir nehmen auch erstmal den einfachen ohne Constraints
     * Die Constraints werden aber wohl gebraucht, damit er bei konkaven(konvex?) nicht aussen herum verbindet.
     * Das ist alles ziemlich tricky. Je nach Algorithmus können Vertices dazukommen.
     * Liefert null bei Fehler.
     * Sollte für empty polygons gar nicht erst aufgerufen werden.
     */
    public static VertexData triangulate(
            Coordinate[] XXvertices,
            Polygon/*SimplePolygonXZ*/ polygon
        /*Collection<SimplePolygonXZ> holes,
        Collection<LineSegmentXZ> segments,
        Collection<VectorXZ> points*/) {

        if (!polygon.isValid()) {
            logger.warn("triangulate:Polygon not valid");
        }
        if (polygon.getNumInteriorRing() > 0) {
            //kein warning, das kann passieren
            //logger.debug("triangulate:Polygon has holes");
        }
        if (polygon.getCoordinates().length < 4) {
            //kein warn sondern error, weil ja nichts erzeugt wird
            logger.error("inconsistent? empty polygon");
            return null;
        }


        //18.7.18: immer den wegen z.B. CCW Handling. Der ConformingDelaunayTriangulationBuilder baut offenbar immr CCW,
        //der EarClippingTriangulationUtil offenbar nicht. 9.8.18: Dann muss das gerichtet werden. Earclippping ist jetzt fallback.
        //NeeNee, der entfernt Holes zu aufwändig. Poly2Tri als Fallback
        //19.8.19: Fuer TerrainMesh sind zusätzliche Coordinates problematisch.Darum bei ohne Hole bevorzugt earclipping versuchen.
        int[] indices = null;

        List<Polygon> triangles = null;
        if (polygon.getNumInteriorRing() == 0) {
            triangles = JtsUtil.triangulatePolygonByEarClippingRespectingHoles(polygon);
        }
        if (triangles == null) {
            triangles = JtsUtil.triangulatePolygonByDelaunay(polygon, true);
        }
        if (triangles == null) {
            //already logged. Dann per EarClipping. der kann keine Holes? 9.8.18: Doch, kann er.
            //aber mit vielen Holes ist der extrem!! langsam
            //triangles = JtsUtil.triangulatePolygonByEarClippingRespectingHoles(polygon);
            //logger.debug("using poly2tri");
            triangles = Poly2TriTriangulationUtil.triangulate(polygon);
        }

        if (triangles == null) {
            String details = "" + polygon.getNumInteriorRing() + " holes," + polygon.getExteriorRing().getCoordinates().length + " points";
            logger.error("Triangulation finally failed(" + details + "):" + JtsUtil.toWKT(polygon));
            return null;
        }

        // die resultierende effektive Vertexliste mit CCW bauen. Hier koennen welche dazukommen. Warum?
        List<Coordinate> vertices = new ArrayList<>();
        indices = buildIndexes(triangles, vertices);

        //return triangles;

        return new VertexData(vertices, indices, null);
    }

    /**
     * resultingvertices wird hier unique mit den Coordinates der Triangles gefuellt.
     * 26.4.19: Das geht nur für Ebenen/Terrain!
     */
    public static int[] buildIndexes(List<Polygon> triangles, List<Coordinate> resultingvertices) {
        int trianglecnt = triangles.size();//triangulationResult.getNumGeometries();
        int[] indices = new int[trianglecnt * 3];
        for (int i = 0; i < trianglecnt; i++) {
            Geometry geo = triangles.get(i);//triangulationResult.getGeometryN(i);
            Coordinate[] coords = geo.getCoordinates();
            if (!CGAlgorithms.isCCW(coords)) {
                Coordinate ct = coords[2];
                coords[2] = coords[1];
                coords[1] = ct;
            }
            setIndex(indices, 3 * i, 0, coords, resultingvertices);
            setIndex(indices, 3 * i, 1, coords, resultingvertices);
            setIndex(indices, 3 * i, 2, coords, resultingvertices);

        }
        return indices;
    }

    private static void setIndex(int[] indices, int index, int offset, Coordinate[] coords, List<Coordinate> vertices) {
        int idx = JtsUtil.findVertexIndex(coords[offset], vertices);
        if (idx == -1) {
            //logger.warn("vertex not found");
            vertices.add(coords[offset]);
            idx = vertices.size() - 1;
        }
        indices[index + offset] = idx;
    }
}
