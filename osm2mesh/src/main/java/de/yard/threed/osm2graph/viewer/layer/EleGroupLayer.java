package de.yard.threed.osm2graph.viewer.layer;

import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2graph.osm.TileProjection;
import de.yard.threed.osm2graph.viewer.DrawHelper;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.VectorXZ;

import java.awt.*;
import java.util.Map;

public class EleGroupLayer extends Layer {
    private OSMData osmData;
    SceneryProjection projection;
    Map<VectorXZ, OSMNode> paintedNodes ;
//Tile tile;

    public EleGroupLayer() {
    }

    public void draw(/*Tile tile*/Graphics2D g, TileProjection tileProjection) {
        //Viewer2D.logger.debug("drawing elegroups");
        for (EleConnectorGroup no : EleConnectorGroup.elegroups.values()) {
            //tile.drawCircle(no.mapNode.getPos(), 15, Color.orange);
            DrawHelper.drawCircle(g,tileProjection.vectorxzToPoint(no.mapNode.getPos()), 15, Color.orange);
        }
    }

}
