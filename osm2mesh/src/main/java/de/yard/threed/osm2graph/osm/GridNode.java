package de.yard.threed.osm2graph.osm;


import de.yard.threed.osm2world.OSMNode;

/**
 * Das ist eine OSM Node inmitten eines Way ohne Abzewigung und mit Abstand zu den Enden.
 * Created on 01.06.18.
 */
public class GridNode {
    OSMNode osmNode;
    
    public GridNode(OSMNode osmNode){
    this.osmNode=osmNode;    
    }
}
