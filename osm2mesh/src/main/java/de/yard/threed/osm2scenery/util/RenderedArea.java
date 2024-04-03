package de.yard.threed.osm2scenery.util;

import de.yard.threed.osm2graph.osm.PolygonInformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 4.6.19.
 * <p>

 */
public class RenderedArea {
    public List<PolygonInformation> pinfo = new ArrayList<>();

    public RenderedArea(PolygonInformation pinfo) {
        this.pinfo.add(pinfo);
    }


    public RenderedArea() {

    }
}
