package de.yard.threed.osm2graph.osm;



import de.yard.threed.osm2world.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Der muss auf MapWays basieren, nicht osmways, damit projected coordinaten da sind. Zumindest fuer isArea ist das wichtig
 * Created on 06.05.18.
 */
public class WaySet {
    public List<MapWaySegment> ways = new ArrayList<MapWaySegment>();

    public WaySet() {

    }

    public WaySet(Collection<MapWaySegment> waylist) {
        for (MapWaySegment w : waylist) {
            this.ways.add(w);
        }
    }

    public void add(MapWaySegment way) {
        ways.add(way);
    }

    public WaySet extractWaysByTag(Tag tag) {
        WaySet l = new WaySet();
        for (MapWaySegment w : ways) {
            if (w.getOsmWay().tags.contains(tag)) {
                l.add(w);
            }
        }
        return l;
    }

    public int size() {
        return ways.size();
    }

    public WaySet inArea(MapArea mapArea) {
        SimplePolygonXZ poly = mapArea.getOuterPolygon();

        WaySet l = new WaySet();
        for (MapWaySegment w : ways) {
            boolean partoutside = false;
            //for (OSMNode segment : w.) {
            LineSegmentXZ seg = w.getLineSegment();
            if (!poly.contains(seg.p1) || !poly.contains(seg.p2)) {
                partoutside = true;
            }
            //}
            if (!partoutside) {
                l.add(w);
            }
        }
        return l;

    }
}
