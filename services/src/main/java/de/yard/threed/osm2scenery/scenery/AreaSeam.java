package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.scenery.components.Area;

public class AreaSeam {
    public MeshLine meshLine;
    Area area1,area2;
    public LineString shareCandidate;

    public AreaSeam(Area area1, Area area2){
        this.area1=area1;
        this.area2=area2;
    }
}
