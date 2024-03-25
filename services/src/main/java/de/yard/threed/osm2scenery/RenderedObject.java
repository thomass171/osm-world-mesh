package de.yard.threed.osm2scenery;

import de.yard.threed.osm2graph.osm.PolygonInformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 23.08.18.
 * <p>
 * Das muesste eigentlich ein Interface sein.
 */
public class RenderedObject {
    public List<PolygonInformation> pinfo = new ArrayList<>();
    public List<PolygonInformation> volumeinfo = new ArrayList<>();
    public List<PolygonInformation> decorationinfo = new ArrayList<>();

    public RenderedObject(PolygonInformation pinfo) {
        this.pinfo.add(pinfo);
    }


    public RenderedObject() {

    }
}
