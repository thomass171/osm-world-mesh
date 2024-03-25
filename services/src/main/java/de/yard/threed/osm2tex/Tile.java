package de.yard.threed.osm2tex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by thomass on 30.01.17.
 */
public class Tile {
    private BufferedImage img;
    private Dimension size = new Dimension(512,512);
    
    public Tile(){
        img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
    }
    
    public void drawPolygon(Geometry polygon){
        
    }

    public void drawGeometry(Geometry geo){
        Polygon p = new Polygon();
        Coordinate[] coor = geo.getCoordinates();
        for (Coordinate c : coor){
            p.addPoint((int)Math.round(c.x),size.height-(int)Math.round(c.y)-1);
        }
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.red);
        g2d.drawPolygon(p);
    }

    public BufferedImage getImage() {
        return img;
    }
}
