package de.yard.threed.osm2graph.viewer.layer;

import de.yard.threed.osm2graph.osm.TileProjection;
import de.yard.threed.osm2world.VectorXZ;

import java.awt.*;

/**
 * 11.9.19: Layer sollen nicht mehr ins TileImage painten. Das Zeichnen im repaijt macht das Scrollen aber langsamer.
 * In den mouse Handlern darf nicht gepaintet werden, es folgt ja eine repaint, zumindest beim drag.
 */
public abstract class Layer {
    public boolean enabled;

    public Layer() {
    }

    /**
     * Das ist fuer jeden Repaint!
     * @param g
     * @param tileProjection
     */
    public abstract void draw(Graphics2D g,TileProjection tileProjection);


    public boolean isEnabled() {
        return enabled;
    }

    public String mouseReleased(TileProjection tileProjection, VectorXZ mappedClick) {
        return null;
    }

    public String mousePressed(TileProjection tileProjection, VectorXZ mappedClick) {
        return null;
    }

    public void mouseDragged( VectorXZ mappedstartpoint, VectorXZ mappedendpoint) {
    }

    public void openPopup(Component component, Point p, VectorXZ mappedClick) {
    }
}

