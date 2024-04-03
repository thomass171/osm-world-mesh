package de.yard.threed.osm2graph.viewer;

import de.yard.threed.core.Vector2;

import java.awt.*;
import java.awt.geom.Area;
import java.util.List;

public class DrawHelper {
    public static void drawLine(Graphics2D g2d , Point from, Point to, Color color) {
        g2d.setColor(color);
        g2d.drawLine(from.x, from.y, to.x, to.y);
    }

    /**
     * Die Position soll center sein.
     */
    public static void drawCircle(Graphics2D g2d, Point p, int radius, Color color) {
        //Point p = coordinateToPoint(v);
        //Graphics2D g2d = getGraphics();
        g2d.setColor(color);
        g2d.drawArc(p.x - radius, p.y - radius, 2 * radius, 2 * radius, 0, 360);

    }

    /**
     * Parameter muss Polygon statt Geometry sein, um Holes finden zu können.
     * Holes lassen sich nicht vermeiden. Zumindest waere das aufwändig.
     *
     *  highlightcolor um ein Polygon (unabhaengig von wireframe, hervorheben zu koennen.
     */
    public static void drawArea( Graphics2D g2d,Area a,  Color color, boolean fill/*, Color highlightcolor*/) {
        //List<Polygon> plist = toPolygon(polygon);
       /* if (polygon.getCoordinates().length < 3) {
            //19.4.19: Sowas gibt es, z.B. Connector
            //logger.warn("drawGeometry: skipping empty polygon");
            return null;
        }*/
        //13.8.18: Mit area sieht man aber bei wireframe keine Holes? Die sind wohl manchmal einfach zu klein.
        //Area a = toArea(polygon);
        List<Polygon> polys = null;//19.9.19 todo toPolygons(polygon);
        g2d.setColor(color);

        if (fill) {
            g2d.draw(a);
            g2d.fill(a);
        } else {
            //11.9.19 TODO das wireframe thema klären und dann wieder polyies?
            g2d.draw(a);
            /*for (Polygon p : polys) {
                g2d.draw(p);
            }*/
        }
        /*if (highlightcolor != null) {
            g2d.setColor(highlightcolor);
            g2d.draw(a);
        }*/
    }

    /**
     * Derived from OSM2World debugviewer.
     *
     */
    public static void drawArrow(Graphics2D g2d, Point from, Point to, Color color,double width,double len   ) {

        drawLine(g2d, from, to,color);

        Vector2 vfrom = new Vector2(from.x,from.y);
        Vector2 vto = new Vector2(to.x,to.y);
        Vector2 arrowVector = vto.subtract(vfrom);
        Vector2 arrowDir = arrowVector.normalize();

        Vector2 headBase = vto.subtract(arrowDir.multiply(len));
        Vector2 headRight = headBase.add(arrowDir.rightNormal().multiply(0.5*width));
        Vector2 headLeft = headBase.subtract(arrowDir.rightNormal().multiply(0.5*width));

        drawLine(g2d,new Point((int)headRight.x,(int)headRight.y), to,color);
        drawLine(g2d,new Point((int)headLeft.x,(int)headLeft.y), to,color);
    }

    /*public void drawArrow( Coordinate from, Coordinate to, Color color) {
        drawArrow(coordinateToVectorXZ(from), coordinateToVectorXZ(to), color);
    }*/
}
