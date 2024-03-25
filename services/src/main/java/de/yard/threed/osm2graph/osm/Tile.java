package de.yard.threed.osm2graph.osm;


import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.Vector3;
import de.yard.threed.core.geometry.SimpleGeometry;
import de.yard.threed.osm2graph.RenderData;
import de.yard.threed.osm2graph.viewer.DrawHelper;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.OsmOrigin;
import de.yard.threed.osm2world.VectorXZ;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 10.4.19: Ist seit SuperTexture nicht nur zur Visualisierung, sondern auch zur Erzeugung der SuperTexture.
 * 11.9.19: Das Zeichnen von Circles für Indicator u.ae. gehört hier doch gar nicht hin.
 * Nur Polygone, die bei einem Click erkannt werden sollen.
 * <p>
 * Created by thomass on 30.01.17.
 */
public class Tile implements TileProjection{
    private BufferedImage img;
    //static final int SIZE = 1024;
    public Dimension size;
    Logger logger = Logger.getLogger(Tile.class);
    float scale;
    double xoffset, yoffset;
    BufferedImage sampleImage = buildSampleImage();
    boolean antialiasing = false;
    List<PolygonInformation> awtpolies = new ArrayList<PolygonInformation>();
    //lightcyn
    private Color backgroundcolor = new Color(224, 255, 255);
    VectorXZ zoomfrom, zoomto;

    /**
     * Die Groesse ergibt sich aus den gerenderten Daten. Die muessen eigentlich so sein, dass sie zu einer
     * 2**2 Texture fuehrt. Aber das lass ich erstmal. scale ist nur zur Visualisierung.
     * <p>
     * 24.7.18: Ohnehin ist das Ziel einer Textur Erzeugung ja fragwürdig/unklar.
     */
    @Deprecated
    public Tile(Rectangle2D projectionarea, float scale, VectorXZ zoomfrom, VectorXZ zoomto) {
        this.scale = scale;
        this.zoomfrom = zoomfrom;
        this.zoomto = zoomto;
        xoffset = -projectionarea.getX() * scale;
        yoffset = -projectionarea.getY() * scale;
        double width = projectionarea.getWidth();
        size = new Dimension((int) Math.round(projectionarea.getWidth() * scale), (int) Math.round(projectionarea.getHeight() * scale));

        if (zoomfrom != null) {
            // der legt auch scale und damit auch irgendwie die size, zuzmindest das Verhaeltnis fest
            // der scale kann hier aber nicht ermittelt werden, weil er sich beim Ausschneiden ergeben haben muss.
            float zoomwidth = (float) (zoomto.x - zoomfrom.x);
            float zoomheight = (float) Math.abs((zoomto.z - zoomfrom.z));
            int imagewidth = 800;
            int imageheigth = (int) (imagewidth * zoomheight / zoomwidth);
            size = new Dimension(imagewidth, imageheigth);
            scale = this.scale = (((float) size.width) / zoomwidth);

            //xoffset = -projectionarea.getX() * scale;
            //yoffset = -projectionarea.getY() * scale;

            xoffset = -zoomfrom.x * scale;
            yoffset = -zoomfrom.z * scale;

        }
        //logger.debug("xoffset=" + xoffset + ",yoofset=" + yoffset);


        img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(backgroundcolor);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();

    }

    /**
     * SuperTexture Constructor. Hier ist der scale so vorgegeben, dass das Grid gerade so in das Image passt,
     * evtl mit Verschnitt. Skizze 51
     * Aber wie geht zoom? Im Prinzip wie sonst auch, nur greift die Texturgroesse dann nicht,
     * sondern die Groesse des Zoomausschnitts.
     * <p>
     * 10.4.19
     */
    public Tile(int width, int height, double scale, VectorXZ zoomfrom, VectorXZ zoomto) {
        this.scale = (float) scale;
        this.zoomfrom = zoomfrom;
        this.zoomto = zoomto;
        //11.4.19 scale darf nicht mit in den Offset
        xoffset = (width / 2) /** scale*/;
        yoffset = (height / 2) /** scale*/;
        //double width = projectionarea.getWidth();
        //size = new Dimension((int) Math.round(width * scale), (int) Math.round(height * scale));
        size = new Dimension((int) Math.round(width), (int) Math.round(height));

        if (zoomfrom != null) {
            // der legt auch scale und damit auch irgendwie die size, zuzmindest das Verhaeltnis fest
            // der scale kann hier aber nicht ermittelt werden, weil er sich beim Ausschneiden ergeben haben muss.
            float zoomwidth = (float) (zoomto.x - zoomfrom.x);
            float zoomheight = (float) Math.abs((zoomto.z - zoomfrom.z));
            int imagewidth = 800;
            int imageheigth = (int) (imagewidth * zoomheight / zoomwidth);
            size = new Dimension(imagewidth, imageheigth);
            scale = this.scale = (((float) size.width) / zoomwidth);

            xoffset = -zoomfrom.x * scale;
            yoffset = -zoomfrom.z * scale;

        }
        //logger.debug("xoffset=" + xoffset + ",yoofset=" + yoffset);
        img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(backgroundcolor);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
    }


    public void setBackgroundImage(BufferedImage tex) {
        Graphics2D g2d = getGraphics();
        g2d.drawImage(tex, 0, 0, null);
        g2d.dispose();
    }

    /**
     * Ist auch fuer Hilfslinien, wir z.B. Grid.
     *
     * @param xzlist
     * @param polygonOrigin
     * @param color
     * @param fill
     * @return
     */
    public PolygonInformation drawPolygon(String name,List<VectorXZ> xzlist, OsmOrigin polygonOrigin, Color color, boolean fill) {
        Polygon p = toPolygon(xzlist);
        Area a = new Area(p);
        return drawPolygon(name, a, polygonOrigin, color, fill);
    }

    public PolygonInformation drawPolygon(String name,com.vividsolutions.jts.geom.Polygon polygon, OsmOrigin osmOrigin, Color color, boolean fill) {
        Area p = toArea(polygon);
        return drawPolygon(name,p, osmOrigin, color, fill);
    }

    public PolygonInformation drawPolygon(String name,Area p, OsmOrigin polygonOrigin, Color color, boolean fill) {
        Graphics2D g2d = getGraphics();
        g2d.setColor(color);
        if (fill) {
            g2d.fill/*Polygon*/(p);
        } else {
            g2d.draw/*Polygon*/(p);
        }
        PolygonInformation poly = new PolygonInformation(/*toArea*/(p), polygonOrigin, null);
        //30.7.19: auch herkunft hinterlegen.
        poly.creatortag=name;
        awtpolies.add(poly);
        g2d.dispose();
        return poly;
    }

    public PolygonInformation drawGeometry(SimpleGeometry geo, OsmOrigin polygonOrigin, Color color, boolean fill, Material material) {
        List<TriangleAWT> plist = toPolygonList(geo);

        Graphics2D g2d = getGraphics();
        g2d.setColor(color);
        for (TriangleAWT p : plist) {
            if (fill) {
                g2d.fillPolygon(p.p);
            } else {
                g2d.drawPolygon(p.p);
            }
        }
        PolygonInformation poly = new PolygonInformation(plist, geo, polygonOrigin, material);
        awtpolies.add(poly);
        g2d.dispose();
        return poly;
    }

    /**
     * immer wireframe (kein fill)
     *
     * @param vertexData
     */
    public PolygonInformation drawGeometry(VertexData vertexData, OsmOrigin polygonOrigin, Color color, Material material) {
        if (vertexData.validate() != null) {
            logger.error(vertexData.validate());
            return null;
        }
        List<TriangleAWT> plist = toPolygonList(vertexData);

        Graphics2D g2d = getGraphics();
        g2d.setColor(color);
        for (TriangleAWT p : plist) {
            g2d.drawPolygon(p.p);
        }
        PolygonInformation poly = new PolygonInformation(plist, null, polygonOrigin, material);
        awtpolies.add(poly);
        g2d.dispose();
        return poly;
    }

    /**
     * Parameter muss Polygon statt Geometry sein, um Holes finden zu können.
     * Holes lassen sich nicht vermeiden. Zumindest waere das aufwändig.
     * TODO ueber drawhelper. 11.9.19. jetzt zu fummelig
     * @param highlightcolor um ein Polygon (unabhaengig von wireframe, hervorheben zu koennen.
     */
    public PolygonInformation drawGeometry(com.vividsolutions.jts.geom.Polygon polygon, OsmOrigin polygonOrigin, Color color, boolean fill, Material material, Color highlightcolor) {
        //List<Polygon> plist = toPolygon(polygon);
        if (polygon.getCoordinates().length < 3) {
            //19.4.19: Sowas gibt es, z.B. Connector
            //logger.warn("drawGeometry: skipping empty polygon");
            return null;
        }
        //13.8.18: Mit area sieht man aber bei wireframe keine Holes? Die sind wohl manchmal einfach zu klein.
        Area a = toArea(polygon);
        List<Polygon> polys = toPolygons(polygon);
        Graphics2D g2d = getGraphics();
        g2d.setColor(color);
        if (fill) {
            g2d.draw(a);
            g2d.fill(a);
        } else {
            //g2d.draw(a);
            for (Polygon p : polys) {
                g2d.draw(p);
            }
        }
        if (highlightcolor != null) {
            g2d.setColor(highlightcolor);
            g2d.draw(a);
        }

        PolygonInformation poly = new PolygonInformation(a, polygonOrigin, material);
        awtpolies.add(poly);
        g2d.dispose();
        return poly;
    }

/**
 * Die Texture muss schon gekachelt sein. Hier wird das Image nur noch daraus kopiert. 16.6.18: Stimmt der Kommentar noch?
 * 10.7.18: Nein, keine Texturdarstellung, siehe Header.
 * <p>
 * Ein Helper
 */
    /*public PolygonInformation drawGeometry(SimpleGeometry geo, OsmOrigin polygonOrigin, BufferedImage tex) {
        List<TriangleAWT> plist = toPolygonList(geo);

        Graphics2D g2d = getGraphics();
        for (TriangleAWT p : plist) {
            g2d.setClip(p.p);

            g2d.drawImage(tex, 0, 0, null);

            //g2d.fillPolygon(p.p);

        }
        g2d.setClip(null);
        PolygonInformation poly = new PolygonInformation(plist, geo, polygonOrigin);
        awtpolies.add(poly);
        g2d.dispose();
        return poly;
    }*/

    /**
     * Ein Helper
     */
    public static void drawMarkedPolygon(Graphics2D g2d, Polygon p, boolean close) {
        //Graphics2D g2d = getGraphics();
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawPolygon(p);
        if (close) {
            g2d.dispose();
        }
    }

    public static void drawMarkedPolygon(Graphics2D g2d, Area p, boolean close, Color color) {
        //Graphics2D g2d = getGraphics();
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(3));
        g2d.draw(p);
        if (close) {
            g2d.dispose();
        }
    }

    public static void drawMarkedPolygon(Graphics2D g2d, Area p, boolean close) {
        drawMarkedPolygon(g2d, p, close, Color.RED);
    }

    public void drawMarkedPolygon(Graphics2D g2d, com.vividsolutions.jts.geom.Polygon p, boolean close, Color color) {
        drawMarkedPolygon(g2d, toArea(p), close, color);
    }

    public void drawLine(Point from, Point to, Color color) {

        Graphics2D g2d = getGraphics();
        //g2d.setColor(color);
        //g2d.drawLine(from.x, from.y, to.x, to.y);
        DrawHelper.drawLine(g2d,from,to,color);
        g2d.dispose();
    }

    public void drawLine(VectorXZ from, VectorXZ to, Color color) {
        drawLine(vectorxzToPoint(from), vectorxzToPoint(to), color);
    }

    public void drawLine(Coordinate from, Coordinate to, Color color) {
        drawLine(coordinateToPoint(from), coordinateToPoint(to), color);
    }

    /**
     * Die Position soll center sein.
     *
     * @param v
     * @param radius
     * @param color
     */
    @Deprecated
    public void drawCircle(VectorXZ v, int radius, Color color) {
        Point p = vectorxzToPoint(v);
        Graphics2D g2d = getGraphics();
        DrawHelper.drawCircle(g2d,p,radius,color);
        g2d.dispose();
    }

    /**
     * Die Position soll center sein.
     *
     * @param v
     * @param radius
     * @param color
     */
    @Deprecated
    public void drawCircle(Coordinate v, int radius, Color color) {
        Point p = coordinateToPoint(v);
        Graphics2D g2d = getGraphics();
        //g2d.setColor(color);
        //g2d.drawArc(p.x - radius, p.y - radius, 2 * radius, 2 * radius, 0, 360);
        //g2d.dispose();
        DrawHelper.drawCircle(g2d,p,radius,color);
        g2d.dispose();
    }



    /**
     * Die Texture muss schon gekachelt sein. Hier wird das Image nur noch daraus kopiert.
     *
     * @param xzlist
     * @param tex
     */
    public PolygonInformation drawPolygon(List<VectorXZ> xzlist, OsmOrigin polygonOrigin, BufferedImage tex/*, List<VectorXZ> texcoord*/) {
        Polygon p = toPolygon(xzlist);
        Graphics2D g2d = getGraphics();
        g2d.setClip(p);
        //g2d.drawImage(sampleImage, 0, 0, null);
        //0,0 duerfte immer passen unabhaenig von texcoordinaten weil es so eine Art Overlay ist.
        g2d.drawImage(tex, 0, 0, null);
        g2d.setClip(null);
        g2d.dispose();
        PolygonInformation poly = new PolygonInformation(toArea(p), polygonOrigin, null);
        awtpolies.add(poly);
        //drawLine(new Point(-500,-500),new Point(100000,100000),Color.GREEN);
        return poly;
    }


    private BufferedImage buildSampleImage() {
        BufferedImage img = new BufferedImage(4096, 4096, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.YELLOW);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
        return img;
    }


    private Polygon toPolygon(List<VectorXZ> xzlist) {
        Polygon p = new Polygon();

        int size = xzlist.size();
        //CCW oder nicht isr doch egal,oder?
        //for (int i=size-1;i>=0;i--)
        for (int i = 0; i < size; i++) {
            VectorXZ v = xzlist.get(i);
            addPoint(p, v);
        }
        return p;
    }

    private List<TriangleAWT> toPolygonList(SimpleGeometry geo) {
        List<TriangleAWT> plist = new ArrayList<TriangleAWT>();
        List<Vector2> uvs = new ArrayList<Vector2>();
        VectorXZ v;
        int[] indices = geo.getIndices();
        for (int i = 0; i < indices.length; i += 3) {
            Polygon p = new Polygon();
            VectorXZ[] ov = new VectorXZ[3];
            ov[0] = addPolyPoint(geo, indices[i], p, uvs);
            ov[1] = addPolyPoint(geo, indices[i + 1], p, uvs);
            ov[2] = addPolyPoint(geo, indices[i + 2], p, uvs);
            plist.add(new TriangleAWT(p, uvs, ov));
        }
        return plist;
    }

    private List<TriangleAWT> toPolygonList(VertexData vertexData) {
        List<TriangleAWT> plist = new ArrayList<TriangleAWT>();
        // 26.4.19 keine uvs ist zwar ungewoehnlich, gibts aber immer wenn keine Textur vorliegt.
        // Das ist nicht unbedingt ein Fehler.
        if (vertexData.uvs == null) {
            //logger.error("no uvs in VertexData");
            //return plist;
        }
        List<Vector2> uvs = new ArrayList<Vector2>();
        int[] indices = vertexData.indices;
        for (int i = 0; i < indices.length; i += 3) {
            Polygon p = new Polygon();
            VectorXZ[] ov = new VectorXZ[3];
            ov[0] = addPolyPoint(vertexData.vertices, indices[i], vertexData.uvs, p, uvs);
            ov[1] = addPolyPoint(vertexData.vertices, indices[i + 1], vertexData.uvs, p, uvs);
            ov[2] = addPolyPoint(vertexData.vertices, indices[i + 2], vertexData.uvs, p, uvs);
            plist.add(new TriangleAWT(p, uvs, ov));
        }
        return plist;
    }

    /**
     * AWT Polygon kann keine Holes, darum Area.
     *
     * @param polygon
     * @return
     */
    @Override
    public Area toArea(com.vividsolutions.jts.geom.Polygon polygon) {
        Area area = toArea(polygon.getExteriorRing().getCoordinates());
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            area.subtract(toArea(polygon.getInteriorRingN(i).getCoordinates()));
        }
        return area;
    }

    /**
     * 13.8.18: AWT artea zeichnet keine Holes?
     *
     * @param polygon
     * @return
     */
    private List<Polygon> toPolygons(com.vividsolutions.jts.geom.Polygon polygon) {
        List<Polygon> plist = new ArrayList<>();
        plist.add(toPolygon(polygon.getExteriorRing().getCoordinates()));
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            plist.add(toPolygon(polygon.getInteriorRingN(i).getCoordinates()));
        }
        return plist;
    }

    private Polygon toPolygon(Coordinate[] coors) {
        Polygon p = new Polygon();
        for (Coordinate c : coors) {
            addPoint(p, c);
        }
        return p;
    }

    private Area toArea(Coordinate[] coors) {
        Path2D path = new Path2D.Double();
        for (Coordinate c : coors) {
            addPoint(path, c);
        }
        path.closePath();
        return new Area(path);
    }

    private Area toArea(Polygon p) {
        Path2D path = new Path2D.Double();
        for (int i = 0; i < p.npoints; i++) {
            addPoint(path, new VectorXZ(p.xpoints[i], p.ypoints[i]));
        }
        path.closePath();
        return new Area(path);
    }

    private VectorXZ addPolyPoint(SimpleGeometry geo, int i, Polygon p, List<Vector2> uvs) {
        VectorXZ v = buildVectorXZ(geo.getVertices().getElement(i));
        addPoint(p, v);
        uvs.add(geo.getUvs().getElement(i));
        return v;
    }

    /**
     * geht nicht static
     */
    private VectorXZ addPolyPoint(List<Coordinate> vertices, int i, List<VectorXZ> uvs, Polygon p, List<Vector2> duvs) {
        VectorXZ v = new VectorXZ(vertices.get(i).x, vertices.get(i).y);
        addPoint(p, v);
        if (uvs != null) {
            VectorXZ uv = uvs.get(i);
            duvs.add(new Vector2((float) uv.x, (float) uv.z));
        }
        return v;
    }

    private VectorXZ buildVectorXZ(Vector3 v) {
        return new VectorXZ(v.getX(), v.getZ());
    }

    private void addPoint(Path2D p, Coordinate c) {
        VectorXZ v = new VectorXZ(c.x, c.y);
        addPoint(p, v);
    }

    private void addPoint(Path2D p, VectorXZ v) {
        Point p1 = vectorxzToPoint(v);
        if (p.getCurrentPoint() == null) {
            p.moveTo(p1.x, p1.y);
        } else {
            p.lineTo(p1.x, p1.y);
        }
    }

    /**
     * geht nicht static.
     *
     * @param p
     * @param v
     */
    private void addPoint(Polygon p, VectorXZ v) {
        if (Math.abs(v.getX()) > 512) {
            //logger.warn("large x:"+v.getX());
        }
        if (Math.abs(v.getZ()) > 512) {
            //logger.warn("large z:"+v.getZ());
        }
        //int y = (int) Math.round(v.getZ() * scale + yoffset);
        //p.addPoint((int) Math.round(v.getX() * scale + xoffset), getEffectiveY(y));
        Point p1 = vectorxzToPoint(v);
        p.addPoint(p1.x, p1.y);
    }

    private void addPoint(Polygon p, Coordinate c) {
        Point v = coordinateToPoint(c);
        p.addPoint(v.x, v.y);
    }

    private int getEffectiveY(int y) {
        return size.height - y - 1;
    }

    public Point vectorxzToPoint(VectorXZ v) {
        int x = (int) Math.round(v.getX() * scale + xoffset);
        int y = (int) Math.round(v.getZ() * scale + yoffset);
        return new Point(x, getEffectiveY(y));
    }

    public Point coordinateToPoint(Coordinate v) {
        int x = (int) Math.round(v.x * scale + xoffset);
        int y = (int) Math.round(v.y * scale + yoffset);
        return new Point(x, getEffectiveY(y));
    }

    private VectorXZ coordinateToVectorXZ(Coordinate c) {
        return new VectorXZ(c.x,c.y);
    }

    public VectorXZ pointToVectorXZ(Point p) {
        float x = p.x;
        float y = size.height - p.y - 1;
        return new VectorXZ((x - xoffset) / scale, (y - yoffset) / scale);
    }

    /**
     * fuer tests
     */
    public void drawText(VectorXZ pos, String text) {
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.red);
        g2d.setFont(new Font("TimesRoman", Font.PLAIN, 24));
        Point p = vectorxzToPoint(pos);
        int x = p.x;
        int y = p.y;
        //g2d.rotate(-Math.PI/4
        float angle = 145;
        g2d.translate((float) x, (float) y);
        g2d.rotate(Math.toRadians(angle));
        g2d.drawString(text, 0, 0);

        //g2d.rotate(-Math.toRadians(angle));
        ///g2d.translate(-(float)x,-(float)y);

        //g2d.drawString(text, p.x, p.y/*getEffectiveY(p.y)*/);
        g2d.dispose();
    }

    public BufferedImage getImage() {
        return img;
    }


    public void setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
    }

    public Graphics2D getGraphics() {
        Graphics2D g2d = img.createGraphics();
        if (antialiasing) {
            g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);

        }
        /*if (zoomarea != null) {
            g2d.translate(-zoomarea.x, -zoomarea.y);
            double sd = 2;
            g2d.scale(sd,sd);
        }*/
        return g2d;
    }

    public List<PolygonInformation> getPolygonsAtLocation(Point p) {
        List<PolygonInformation> ids = new ArrayList<PolygonInformation>();
        for (PolygonInformation poly : awtpolies) {
            if (poly.contains(p)) {
                ids.add(poly);
            }
        }
        return ids;
    }

    public VectorXZ getProjectionLocation(Point p) {
        VectorXZ v = pointToVectorXZ(p);
        //logger.debug("getProjectionLocation for "+p+" found "+v);
        return v;
    }

    @Override
    public double getScale(){
        return scale;
    }

    public GraphicsTarget paintToImage(RenderData results, boolean wireframe, boolean triangulated, boolean drawVolumes, boolean terrainProviderOnly) {
        //MapProjection projection = results.getMapProjection();
        //tile.setBackgroundImage(Materials.GRASS.getTexture(0));
        GraphicsTarget target = new GraphicsTarget(this);
        target.setWireframe(wireframe);
        target.setTriangulated(triangulated);
        target.setDrawVolumes(drawVolumes);
        target.setAntialiasing(antialiasing);
        target.setTerrainProviderOnly(terrainProviderOnly);
        //target.setVisualizeNodes(visualizeNodes);

        boolean underground = Config.getCurrentConfiguration().getBoolean("renderUnderground", true);

        try {
            //23.8.18: Das mit der Rendermap ist aber auch so'ne Kruecke
            target.rendermap = results.sceneryresults.sceneryMesh.render(target);
            logger.debug("rendering completed");
        } catch (Exception e) {
            logger.error("exception during rendering");
            e.printStackTrace();
        }
        return target;
    }
}


