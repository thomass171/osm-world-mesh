package de.yard.threed.osm2graph.osm;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Util;
import de.yard.threed.core.Vector3;


import de.yard.threed.core.geometry.SimpleGeometry;
import de.yard.threed.core.loader.PortableMaterial;
import de.yard.threed.core.loader.PortableModelDefinition;
import de.yard.threed.core.loader.PortableModelList;
import de.yard.threed.osm2scenery.SceneryRenderer;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.scenery.WorldElement;
import de.yard.threed.osm2scenery.util.RenderedArea;
import de.yard.threed.core.Color;

import de.yard.threed.osm2world.*;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;


import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Nachdem alles gedrawed ist, muss endRendering aufgerufen werden.
 * <p>
 * Created by thomass on 07.06.18.
 */

public class PortableModelTarget extends AbstractTarget implements SceneryRenderer {
    Logger logger = Logger.getLogger(PortableModelTarget.class);
    public PortableModelList pml;
    //2.4.19: Die Normale (0, 1, 0) ist doch fuer z0 nicht richtig. z als Normale nehmen.
    //2.5.19 doch fragwürdig private Vector3 defaultnormal = new Vector3(0, 0, 1);
    //private HashMap<PortableMaterial,String> mat2matname = new HashMap<>();
    private HashMap<String, PortableMaterial> matname2mat = new HashMap<>();
    List<String> usedtextures = new ArrayList<>();
    // Die Simplegeos werden zur Optimierung pro Material zusammengefasst (matname->SimpleGeo).
    // Spaetestens mit Bridges wird das aber wohl ueber eine TerrainClass gehen müssen.
    // Muss ueberlegt werden: Materials können sich leicht unterscheiden.
    private HashMap<String, SimpleGeometry> matname2geo = new HashMap<>();
    // ein Counter fuer Statistik/Pruefzwecke
    private HashMap<String, Integer> matnameCounter = new HashMap<>();
    //int outputmode = SceneryBuilder.BUILDMODE_2D;
    //Optimierung deaktivierbar
    boolean mergeobjects = true;

    public PortableModelTarget() {
        pml = new PortableModelList(null);
    }

    public PortableModelTarget(int outputmode) {
        this();
        //not used  this.outputmode = outputmode;
    }

    public Class getRenderableType() {
        return PortableModelTarget.class;
    }

    public void setConfiguration(Configuration config) {

    }

    /*26.4.19public void render(RenderableToAllTargets renderable) {
        renderable.renderTo(this);
    }*/


    public void beginObject(WorldObject object) {

    }

    public void drawShape(Material material, SimpleClosedShapeXZ shape, VectorXYZ point, VectorXYZ frontVector, VectorXYZ upVector) {

        Util.notyet();
    }

    public void drawExtrudedShape(Material material, ShapeXZ shape, List<VectorXYZ> path, List<VectorXYZ> upVectors, List<Double> scaleFactors, List<List<VectorXZ>> texCoordLists, EnumSet<ExtrudeOption> options) {
        logger.warn("drawExtrudedShape:not yet");
    }

    public void drawBox(Material material, VectorXYZ bottomCenter, VectorXZ faceDirection, double height, double width, double depth) {
        logger.warn("drawBox:not yet");
    }

    public void drawColumn(Material material, Integer corners, VectorXYZ base, double height, double radiusBottom, double radiusTop, boolean drawBottom, boolean drawTop) {
        SimpleGeometry geo = buildGeometryForColumn(corners, base, height, radiusBottom, radiusTop, drawBottom, drawTop);
        buildObject("Columns", geo, material);
    }

    public void finish() {

    }

    public void drawConvexPolygon(Material material, List vs, List texCoordLists) {
        logger.warn("drawConvexPolygon:not yet");
    }

    public void drawTriangleFan(Material material, List vs, List texCoordLists) {
        logger.warn("drawTriangleFan:not yet");
    }

    /**
     *
     */
    public void drawTriangleStrip(Material material, VectorXYZList vs, List<List<VectorXZ>> texCoordLists, OsmOrigin osmOrigin) {
        SimpleGeometry geo = buildGeometryForTriangleStrip(vs, texCoordLists, osmOrigin);
        buildObject("Strip", geo, material);
    }

    public void drawTrianglesWithNormals(Material material, Collection triangles, List texCoordLists) {
        Util.notyet();
    }

    public void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles, List<List<VectorXZ>> texCoordLists, OsmOrigin osmOrigin) {
        SimpleGeometry geo = buildGeometryForTriangles(triangles, texCoordLists, osmOrigin);
        buildObject("Triangle", geo, material);
    }

    /**
     * Immer Namen anbegen, dann kann man es nachher besser wiederfinden.
     *
     * @param name
     * @param geo
     * @param material
     */
    private void buildObject(String name, SimpleGeometry geo, Material material) {
        PortableModelDefinition pmd = new PortableModelDefinition();
        pmd.addGeoMat(geo, getMaterial(material));
        pmd.name = name;
        //6.2.23 pml.objects.add(pmd);
        pml.addModel(pmd);
    }

    private void buildObject(String name, SimpleGeometry geo, String material) {
        PortableModelDefinition pmd = new PortableModelDefinition();
        pmd.addGeoMat(geo, material);
        pmd.name = name;
        //6.2.23 pml.objects.add(pmd);
        pml.addModel(pmd);
    }

    public void draw(AbstractAreaWorldObject awo) {
        logger.debug("draw awo " + awo.toString());
        //29.5.18 tile.drawText(120, 30, "hallo");
        SimplePolygonXZ poly = awo.getOutlinePolygonXZ();
        //29.5.18tile.drawPolygon(poly.getVertices(),Color.orange);
    }

    private String getMaterial(Material material) {
        PortableMaterial mat = matname2mat.get(material.getName());
        if (mat == null) {
            PortableMaterial m = new PortableMaterial();
            m.name = material.getName();
            //OSM2Grapph applies a diffusefactor to the color value, mostly 0.5! This results in lower values.
            m.color = buildColor(material.diffuseColor());
            if (material.getTextureDataList().size() > 0) {
                //erstmal nur die erste
                TextureData texturedata = material.getTextureDataList().get(0);
                String filename = texturedata.file/*.getName()*/;
                String path = material.getBasePath();
                if (path == null) {
                    // Pfad ist bezogen auf das Layout in osmscenery
                    m.texture = "../textures/" + filename;
                } else {
                    m.texture = path + "/" + filename;
                }
                //TODO nicht immer true
                m.wraps = true;
                m.wrapt = true;
                usedtextures.add(filename);
            }
            m.shaded = true;
            matname2mat.put(material.getName(), m);
            pml.materials.add(m);
        }
        return material.getName();
    }

    private Color buildColor(java.awt.Color c) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    /**
     * z negieren um aus left handed right handed zu machen. (2.4.19: Ist doch Asbach?)
     * 19.7.18: und z0 statt y0. negieren?
     *
     * @param p
     * @return
     */
    @Override
    Vector3 buildVector3(VectorXYZ p) {
        //return new Vector3((float) p.x, (float) p.y, -(float) p.z);
        //VectorXYZ.y ist die Elevation? Muesste.
        return buildVector3inZ0(p.x, /*-*/ p.z, p.y);
    }

    @Override
    Vector3 buildVector3(Coordinate p) {
        //return new Vector3((float) p.x, 0/*(float) p.z*/, -(float) p.y);
        //Coordinate.z ist die Elevation.
        //bridges eg. have z set.
        double z = 0;
        if (!Double.isNaN(p.z)) {
            z = p.z;
        }
        //return new Vector3((float) p.x, /*-*/(float) p.y, (float)z);
        return buildVector3inZ0(p.x, /*-*/ p.y, z);
    }

    /**
     * z enthaelt Elevation.  In 2D gibt es HIER keine Unterscheidung der Elevation! Denn Relief durch z.B. Brücken soll es ja geben.
     *
     * @return
     */
    Vector3 buildVector3inZ0(double x, double y, double z) {
        if (z > 70) {
            //for debugging
            z = z;
        }
        return new Vector3((float) x, (float) y, (float) z);
    }

   /* @Override
    Vector3 getDefaultNormal() {
        return defaultnormal;
    }*/

    @Override
    public RenderedArea drawArea(String name, Material material, Polygon sceneryAreaNotUsedHere, VertexData vertexData, OsmOrigin osmOriginNotUsedHere, EleConnectorGroupSet elevationsNotUsedHere) {
        if (vertexData == null) {
            logger.warn("No vertex data. Skipping area. Should not be called if empty");
            return null;
        }
        String msg;
        if ((msg = vertexData.validate()) != null) {
            logger.warn(msg + ". Skipping area " + name);
            return null;
        }
        SimpleGeometry geo = buildGeometry(vertexData.vertices, vertexData.indices, vertexData.uvs, vertexData.normals);
        if (geo == null) {
            logger.warn("No geometry. Skipping area");
            return null;
        }
        /*if (material != GRASS){
            logger.warn("debug skip");
            return null;
        }*/

        //buildObject (name,geo,material);
        String materialname = getMaterial(material);
        if (mergeobjects) {
            if (materialname.equals("WALL_BRICK_RED")) {
                int h = 9;
            }
            SimpleGeometry existinggeo = matname2geo.get(materialname);
            Integer cnt = matnameCounter.get(materialname);
            if (existinggeo != null) {
                geo = existinggeo.add(geo);
                cnt += 1;
            } else {
                cnt = 1;
            }
            matname2geo.put(materialname, geo);
            matnameCounter.put(materialname, cnt);
        } else {
            buildObject(materialname, geo, materialname);
        }
        return null;
    }

    @Override
    public RenderedArea drawElement(String name, WorldElement element, OsmOrigin osmOrigin) {
        // Das scheint zunaechst mal einfach delegierbar.
        return drawArea(name, element.material, null, element.vertexData, null, null);
    }


    @Override
    public void beginRendering() {
    }

    /**
     * Jetzt erst die Objekte bauen.
     */
    @Override
    public void endRendering() {
        if (mergeobjects) {
            // Fuer jedes Material wird eine einzige grosse Geo erstellt.
            Set<String> matnames = matname2geo.keySet();
            for (String matname : matnames) {
                SimpleGeometry geo = matname2geo.get(matname);
                String s;
                if ((s = geo.validate()) != null) {
                    logger.error("invalid geometry: " + s);
                }
                buildObject(matname, geo, matname);
                int cnt = matnameCounter.get(matname);
                String details = "" + geo.getVertices().size() + " vertices," + (geo.getIndices().length / 3) + " triangles";
                //debug->info. Ist immer interessant
                logger.info("" + cnt + " scenery objects (" + details + ") created into one mesh for material " + matname);
            }
        }
    }

    @Override
    public boolean isTerrainProviderOnly() {
        return false;
    }

    /*25.4.19 @Override
    public boolean renderVolumeOverlay() {
        return true;
    }*/

    public int getSceneryObjectCount(String materianame) {
        int cnt = matnameCounter.get(materianame);
        return cnt;
    }
}
