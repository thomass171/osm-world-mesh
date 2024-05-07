package de.yard.threed.osm2scenery.polygon20;


import de.yard.threed.osm2scenery.scenery.SceneryWayObject;

public interface OsmWay {
    long getOsmId();
    SceneryWayObject buildSceneryWayObject();
}
