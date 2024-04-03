package de.yard.threed.osm2graph.osm;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Util;
import de.yard.threed.core.Vector3;
import de.yard.threed.core.geometry.SimpleGeometry;
import de.yard.threed.javacommon.ImageUtils;
import de.yard.threed.javacommon.JALog;
import de.yard.threed.osm2graph.viewer.Viewer2D;
import de.yard.threed.osm2scenery.RenderedObject;
import de.yard.threed.osm2scenery.SceneryRenderer;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.scenery.WorldElement;
import de.yard.threed.osm2scenery.util.RenderedArea;
import de.yard.threed.osm2world.AbstractAreaWorldObject;
import de.yard.threed.osm2world.ExtrudeOption;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.OsmOrigin;
import de.yard.threed.osm2world.RenderableToAllTargets;
import de.yard.threed.osm2world.ShapeXZ;
import de.yard.threed.osm2world.SimpleClosedShapeXZ;
import de.yard.threed.osm2world.SimplePolygonXZ;
import de.yard.threed.osm2world.TextureData;
import de.yard.threed.osm2world.TriangleXYZ;
import de.yard.threed.osm2world.VectorXYZ;
import de.yard.threed.osm2world.VectorXYZList;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.osm2world.WorldObject;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by thomass on 31.01.17.
 * <p>
 * 10.7.18: Das Zeichnen mit Texturen erfordert kompliziertes Mapping (), dass normalerweise die GPU macht. Darum lasse ich die Option
 * weg und stelle stattdessen bei Markieren eines Triangles den dazugehörigen Ausschnitt aus der Textur in einem eigenen Fenster dar.
 */

public class GraphicsTarget extends AbstractTarget implements SceneryRenderer {
    public final Tile tile;
    Logger logger = Logger.getLogger(GraphicsTarget.class);
    private boolean wireframe;
    HashMap<String, PolygonOrigin> polygonMapping = new HashMap<String, PolygonOrigin>();
    public int trianglestrips = 0;
    // Der key entahelt auch den Dateisuffix. static, damit die Texturen bei Neuzeichnen nicht immer neu geladen werden.
    public static HashMap<String, BufferedImage> textures = new HashMap<String, BufferedImage>();
    private boolean triangulated;
    //25.4.19 @Overrideprotected boolean volumeOverlay;
    public Map<Integer, RenderedObject> rendermap;
    private boolean drawVolumes = false;
    private boolean terrainProviderOnly;


    public GraphicsTarget(Tile tile) {
        this.tile = tile;
    }

    public Class getRenderableType() {
        return RenderableToAllTargets.class;
    }

    public void setConfiguration(Configuration config) {

    }

    /*26.4.19public void render(RenderableToAllTargets renderable) {
        renderable.renderTo(this);
    }*/


    public void beginObject(WorldObject object) {

    }

    public void drawShape(Material material, SimpleClosedShapeXZ shape, VectorXYZ point, VectorXYZ frontVector, VectorXYZ upVector) {
        logger.warn("drawShape:not yet");

    }

    /**
     * Wird z.B. für RAIL_SHAPE verwendet. Brauch ich erstmal nicht
     */
    public void drawExtrudedShape(Material material, ShapeXZ shape, List<VectorXYZ> path, List<VectorXYZ> upVectors, List<Double> scaleFactors, List<List<VectorXZ>> texCoordLists, EnumSet<ExtrudeOption> options) {
        logger.warn("drawExtrudedShape:not yet");
    }

    public void drawBox(Material material, VectorXYZ bottomCenter, VectorXZ faceDirection, double height, double width, double depth) {
        SimpleGeometry geo = buildGeometryForBox(bottomCenter, faceDirection, height, width, depth);
        String creatortag = "";//(osmOrigin != null) ? osmOrigin.creatortag : "";
        draw2DGeometry(creatortag, null, material, geo);
    }

    public void drawColumn(Material material, Integer corners, VectorXYZ base, double height, double radiusBottom, double radiusTop, boolean drawBottom, boolean drawTop) {
        SimpleGeometry geo = buildGeometryForColumn(corners, base, height, radiusBottom, radiusTop, drawBottom, drawTop);
        String creatortag = "";//(osmOrigin != null) ? osmOrigin.creatortag : "";
        draw2DGeometry(creatortag, null, material, geo);
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
    public void drawTriangleStrip(Material material, VectorXYZList vs, List texCoordLists, OsmOrigin osmOrigin) {
        // Ein TriangleStriup ist immer ein Vielfaches von 2 und geht links/rechts usw bis zum Ende. Daraus ein Polygon machen.
        SimpleGeometry geo = buildGeometryForTriangleStrip(vs, texCoordLists, osmOrigin);

        // CCW?
/*        List<VectorXZ> l = new ArrayList<VectorXZ>();
        for (int i = 1; i < vs.vs.size(); i += 2) {
            VectorXYZ p = vs.vs.get(i);
            l.add(new VectorXZ(p.getX(), p.getZ()));
        }
        for (int i = vs.vs.size() - 2; i >= 0; i -= 2) {
            VectorXYZ p = vs.vs.get(i);
            l.add(new VectorXZ(p.getX(), p.getZ()));
        }*/

        String creatortag = "";//(osmOrigin != null) ? osmOrigin.creatortag : "";

        //draw2Dxz(creatortag, osmOrigin, material, l, texCoordLists);
        draw2DGeometry(creatortag, osmOrigin, material, geo);
        trianglestrips++;
    }

    public void drawTrianglesWithNormals(Material material, Collection triangles, List texCoordLists) {
        Util.notyet();
    }

    public void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles, List<List<VectorXZ>> texCoordLists, OsmOrigin osmOrigin) {
        /*if (osmOrigin != null && osmOrigin.outlinePolygonXZ != null) {
            draw2Dxz("", null, material, osmOrigin.outlinePolygonXZ.getVertices(), texCoordLists);
        } else {
            logger.warn("not yet");
        }*/
        SimpleGeometry geo = buildGeometryForTriangles(triangles, texCoordLists, osmOrigin);
        String creatortag = "";//(osmOrigin != null) ? osmOrigin.creatortag : "";
        draw2DGeometry(creatortag, osmOrigin, material, geo);
    }

    /**
     * Returns polygon id
     * Hier muss wirklich ein Polygon reinkommen, sonst stimmt die Darstellung nicht.
     *
     * @return
     */
    //29.5.18 @Override
   /*30.7.19  public void draw2Dxz(String label, OsmOrigin polygonOrigin, Material material, List<VectorXZ> poly, List<List<VectorXZ>> texCoordLists) {
        //zu viel logger.debug("draw2d  ");
        //tile.drawText(60, 30, "hallo");
        //SimplePolygonXZ poly = owo.getOutlinePolygonXZ();
        PolygonInformation pinfo;

        String id;
        if (wireframe) {
            pinfo = tile.drawPolygon(poly, polygonOrigin, Color.red, false);
        } else {
            Color color = material.getColor();
            //24.5.18 erstmal ohne. Zu Texture siehe Header.
            BufferedImage tex = null;//material.getTexture(0);
            List<TextureData> textureDataList = material.getTextureDataList();

            if (tex != null) {
                // dann wird die Textur verwendet.
                pinfo = tile.drawPolygon(poly, polygonOrigin, tex/*,texCoordLists.get(0)* /);
            } else {
                pinfo = tile.drawPolygon(poly, polygonOrigin, color, true);
            }
        }
        //29.5.18 pinfo.setTextureName(material.getTextureName());
        /*if (getPolygonMapping()!=null && polygonOrigin != null) {
            getPolygonMapping().put(id, polygonOrigin);
        }* /
    }*/

    public void draw(AbstractAreaWorldObject awo) {
        logger.debug("draw awo " + awo.toString());
        //29.5.18 tile.drawText(120, 30, "hallo");
        SimplePolygonXZ poly = awo.getOutlinePolygonXZ();
        //29.5.18tile.drawPolygon(poly.getVertices(),Color.orange);
    }

    /*31.5.18 nicht mehr static
    public static void draw2D(Target target, String label, PolygonOrigin polygonOrigin, Material material, List<VectorXYZ> poly, List texCoordLists) {
        if (polygonOrigin == null) {
            throw new RuntimeException("polygonorigin isType null");
        }
        List<VectorXZ> l = new ArrayList<VectorXZ>();
        for (VectorXYZ p : poly) {
            l.add(new VectorXZ(p.getX(), p.getZ()));
        }
         target.draw2D(label, polygonOrigin, material, l, texCoordLists);
    }*/

    /*12.6.18 public void draw2Dxyz(String label, OsmOrigin polygonOrigin, Material material, List<VectorXYZ> poly, List texCoordLists) {
        if (polygonOrigin == null) {
            //30.5.18 throw new RuntimeException("polygonorigin isType null");
        }
        List<VectorXZ> l = new ArrayList<VectorXZ>();
        for (VectorXYZ p : poly) {
            l.add(new VectorXZ(p.getX(), p.getZ()));
        }
        draw2Dxz(label, polygonOrigin, material, l, texCoordLists);
    }*/

    /**
     * 10.7.18: Keine Texturdarstellung, seiehe Header.
     */
    public void draw2DGeometry(String label, OsmOrigin polygonOrigin, Material material, SimpleGeometry geo) {
        PolygonInformation pinfo;

        String texturename = getTextureNameFromMaterial(material);
        if (texturename != null) {
            loadTexture(texturename);
        }

        String id;
        if (wireframe) {
            pinfo = tile.drawGeometry(geo, polygonOrigin, Color.red, false, material);
        } else {
            Color color = material.getColor();

            pinfo = tile.drawGeometry(geo, polygonOrigin, color, true, material);

        }
        //29.5.18 pinfo.setTextureName(material.getTextureName());
        /*if (getPolygonMapping()!=null && polygonOrigin != null) {
            getPolygonMapping().put(id, polygonOrigin);
        }*/
    }

    public void setWireframe(boolean wireframe) {
        this.wireframe = wireframe;
    }

    public void setAntialiasing(boolean antialiasing) {
        if (tile != null) {
            tile.setAntialiasing(antialiasing);
        }
    }

    /*25.4.19public void setVolumeOverlay(boolean volumeOverlay) {
        this.volumeOverlay = volumeOverlay;
    }*/

    public HashMap<String, PolygonOrigin> getPolygonMapping() {
        return polygonMapping;
    }

    @Override
    Vector3 buildVector3(VectorXYZ p) {
        return new Vector3((float) p.x, (float) p.y, (float) p.z);
    }

    @Override
    Vector3 buildVector3(Coordinate p) {
        return new Vector3((float) p.x, (float) p.z, -(float) p.y);
    }

    /*@Override
    //2.5.19 doch fragwürdig Vector3 getDefaultNormal() {
        return new Vector3(0, 1, 0);
    }*/

    /**
     * Name mit Suffix
     *
     * @param material
     */
    public static String getTextureNameFromMaterial(Material material) {
        if (material == null) {
            return null;
        }
        List<TextureData> textureDataList = material.getTextureDataList();
        if (textureDataList != null && textureDataList.size() > 0) {
            TextureData texture = textureDataList.get(0);
            //File f = texture.file;
            String name = texture.file;//f.getName();
            return name;
        }
        return null;
    }

    /**
     * Das Image der Textur erstellen, wenns es noch nicht bekannt ist.
     */
    private void loadTexture(String name) {
        if (textures.get(name) == null) {
            BufferedImage tex = null;
            try {
                tex = ImageUtils.loadImageFromFile(new JALog(GraphicsTarget.class), new FileInputStream(new File(Viewer2D.texturedir + "/" + name)), name);
                //tex == null already logged
            } catch (FileNotFoundException e) {
                logger.error("File not found", e);
            }
            if (tex != null) {
                textures.put(name, tex);
            }
        }
    }

    /**
     * trifailed um ein Polygon (unabhaengig von wireframe), hervorheben zu koennen.
     */
    @Override
    public RenderedArea drawArea(String name, Material material, Polygon polygon, VertexData vertexData, OsmOrigin osmOrigin, EleConnectorGroupSet eleconnectors) {
        Color highlightcolor = null;
        if (osmOrigin != null && osmOrigin.trifailed) {
            highlightcolor = Color.ORANGE;
        }
        String texturename = null;
        if (material != null) {
            texturename = getTextureNameFromMaterial(material);
        }

        if (texturename != null) {
            loadTexture(texturename);
        }

        String id;
        PolygonInformation pinfo = null;
        if (wireframe) {
            // triangulated wird nur bei wireframe angezeigt und dann statt des Polygons
            if (triangulated && vertexData != null) {
                pinfo = tile.drawGeometry(vertexData, osmOrigin, Color.red, material);
            } else {
                if (polygon != null) {
                    //z.B. Volumes, Buildings
                    pinfo = tile.drawGeometry(polygon, osmOrigin, Color.red, false, material, highlightcolor);
                }
            }
        } else {

            Color color = Color.GREEN;
            if (material != null) {
                color = material.getColor();
            }
            if (polygon != null) {
                //z.B. Volumes, Buildings
                pinfo = tile.drawGeometry(polygon, osmOrigin, color, true, material, highlightcolor);
            }
        }
        RenderedArea renderedArea = null;
        if (pinfo != null) {
            pinfo.creatortag = name;
            pinfo.eleconnectors = eleconnectors;
            pinfo.vertexData = vertexData;
            renderedArea = new RenderedArea(pinfo);
        }
        return renderedArea;
    }

    /**
     * 26.4.19
     */
    @Override
    public RenderedArea drawElement(String name, WorldElement element, OsmOrigin osmOrigin) {
        // In 2D schlecht darstellbar. Mal so als Grundriss versuchen
        if (drawVolumes) {
            return drawArea(name, element.material, null, element.vertexData, osmOrigin, null);
        }
        return null;
    }

    @Override
    public void beginRendering() {

    }

    @Override
    public void endRendering() {

    }

    @Override
    public boolean isTerrainProviderOnly() {
        return terrainProviderOnly;
    }

    /*25.4.19 @Override@Override
    public boolean renderVolumeOverlay() {
        return volumeOverlay;
    }*/

    public void setTriangulated(boolean triangulated) {
        this.triangulated = triangulated;
    }


    public void setDrawVolumes(boolean drawVolumes) {
        this.drawVolumes = drawVolumes;
    }

    public void setTerrainProviderOnly(boolean terrainProviderOnly) {
        this.terrainProviderOnly=terrainProviderOnly;
    }
}
