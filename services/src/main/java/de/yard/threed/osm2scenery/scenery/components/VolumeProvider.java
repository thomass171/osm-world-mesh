package de.yard.threed.osm2scenery.scenery.components;



import de.yard.threed.osm2scenery.scenery.WorldElement;

import java.util.List;

/**
 * 26.4.19
 */
public interface VolumeProvider extends SceneryObjectComponent{

     List<WorldElement> getWorldElements();

    void adjustElevation(double baseelevation);

    /**
     * name from OSM
     * @return
     */
    String getName();

    void triangulateAndTexturize();
}
