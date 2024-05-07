package de.yard.threed.osm2scenery.polygon20;

import de.yard.threed.osm2scenery.scenery.components.AbstractArea;

/**
 * 2.5.24: Now an interface like MeshLine and MeshNode
 */
public interface MeshArea {
    /*public static MeshArea instance = new MeshArea();

    public void insert(AbstractArea area){

    }*/
    void setOsmWay(OsmWay osmWay);
    OsmWay getOsmWay();
}
