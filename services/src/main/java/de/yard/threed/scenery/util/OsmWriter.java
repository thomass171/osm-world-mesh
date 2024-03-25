package de.yard.threed.scenery.util;


import de.yard.threed.osm2world.MapBasedTagGroup;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.OSMWay;
import de.yard.threed.osm2world.Tag;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Schreibt bewusst nicht in eine Datei um versehentliches Ueberschreiben zu vermeiden.
 */
public class OsmWriter {
    public static void write(OSMData osmData, PrintStream printStream, String bounds) {
        printStream.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        printStream.println("<!-- synthetic test data created by OsmTestDataBuilder with manual bounds of -->");
        printStream.println("<osm version=\"0.6\">");
        printStream.println(bounds);
        List<OSMNode> nodes = new ArrayList(osmData.getNodes());
        Collections.sort(nodes,new Comparator<OSMNode>() {
            @Override
            public int compare(OSMNode o1, OSMNode o2) {
                return o1.id > o2.id?1:-1;
            }
        });
        for (OSMNode n : nodes) {
            printStream.println(String.format("<node id=\"%d\" version=\"1\" lat=\"%f\" lon=\"%f\"/>", n.id, n.lat, n.lon).replaceAll(",", "."));
        }
        List<OSMWay> ways = new ArrayList(osmData.getWays());
        Collections.sort(ways,new Comparator<OSMWay>() {
            @Override
            public int compare(OSMWay o1, OSMWay o2) {
                return o1.id > o2.id?1:-1;
            }
        });
        for (OSMWay w :ways ) {
            printStream.println(String.format("<way id=\"%d\" version=\"1\">", w.id));
            for (OSMNode n : w.getNodes()) {
                printStream.println(String.format("<nd ref=\"%d\"/>", n.id));
            }
            MapBasedTagGroup tags = (MapBasedTagGroup) w.tags;
            Iterator<Tag> iter = tags.iterator();
            while (iter.hasNext()) {
                Tag tag = iter.next();
                printStream.println(String.format("<tag k=\"%s\" v=\"%s\"/>", tag.key, tag.value));
            }
            printStream.println(String.format("</way>"));
        }
        printStream.println("</osm>");
    }
}
