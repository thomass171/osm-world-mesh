package de.yard.threed.osm2world;

import java.util.List;

/**
 * Wrapper for triangle strip
 * 
 * Created on 30.05.18.
 */
public class VectorXYZList {
    public List<VectorXYZ>vs,leftOutline=null,rightOutline=null;

    public VectorXYZList(List<VectorXYZ> vs) {
        this.vs = vs;
    }

    public static VectorXYZList buildTriangleStrip(List<VectorXYZ> vs, List<VectorXYZ> leftOutline, List<VectorXYZ> rightOutline) {
        VectorXYZList vl = new VectorXYZList(vs);

        if (Config.getCurrentConfiguration().getBoolean("drawdetails", false)) {
            vl.leftOutline = leftOutline;
            vl.rightOutline = rightOutline;
        }
        return vl;
    }
}
