package de.yard.threed.scenery.util;

import de.yard.threed.core.Degree;
import de.yard.threed.core.LatLon;
import de.yard.threed.core.Pair;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.util.TagHelper;
import de.yard.threed.osm2world.MapBasedTagGroup;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.OSMWay;
import de.yard.threed.traffic.EllipsoidCalculations;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Synthetische OSM Daten erstellen.
 * Einfach mal in den "B55-B477-small" Bounds. Die Masse sind wichtig, weil sie bei der Road gerade so genau einen (Lazy)Cut ergeben. Rechts ist Deadend,
 * das aber rüberhängt. Darum manuell eine "Simplified" Version angelegt.
 * <p>
 * Aufruf: Einfach hier in der Console und Output in eine Datei koperien.
 *
 * <p>
 * Created on 16.4.2019
 */
public class OsmTestDataBuilder {
    GridCellBounds gridCellBounds;
    List<OSMWay> ways;
    List<OSMNode> nodes;
    LatLon origin;
    int id = 100;

    public static void main(String[] arg) {
        //PlatformHomeBrew.init(new HashMap<String, String>());
        SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
        new OsmTestDataBuilder();
    }

    OsmTestDataBuilder() {
        gridCellBounds = GridCellBounds.buildGrid("B55-B477-small", null);
        ways = new ArrayList();
        nodes = new ArrayList();

        OSMData osmData = new OSMData(null, nodes, ways, null);
        origin = gridCellBounds.getOrigin();
        createUshapedWay(100);
        createBuilding(add(origin, 30, new Degree(180)));
        createRunway(add(origin, 60, new Degree(180)));

        //Schreibt bewusst nicht in eine Datei um versehentliches Ueberschreiben zu vermeiden.
        //hardcoded bounds von B55-B477
        String bounds="<bounds minlat=\"50.9450000\" minlon=\"6.5944000\" maxlat=\"50.9479000\" maxlon=\"6.5997000\"/>";
        PrintStream pw = System.out;
        OsmWriter.write(osmData,pw,bounds);

    }

    /**
     * U-shape
     */
    private OSMWay createUshapedWay(double len) {
        LatLon latloneast = add(origin, len / 2, new Degree(90));
        LatLon latlonwest = add(origin, len / 2, new Degree(-90));
        LatLon latlonuppereast = add(latloneast, len, new Degree(0));
        LatLon latlonupperwest = add(latlonwest, len, new Degree(0));
        List<OSMNode> way = new ArrayList<>();
        way.add(createNode(latlonupperwest));
        way.add(createNode(latlonwest));
        way.add(createNode(latloneast));
        way.add(createNode(latlonuppereast));
        OSMWay osmWay = new OSMWay(TagHelper.buildTagGroup("railway", "rail"), id++, way);
        ways.add(osmWay);
        return osmWay;
    }

    /**
     * Ein Standardbuilding.
     *
     * @return
     */
    private OSMWay createBuilding(LatLon start) {
        double width = 10;
        double depth = 5;
        List<OSMNode> way = new ArrayList<>();
        OSMNode startnode = createNode(start);
        way.add(startnode);
        LatLon ll = add(start, depth, new Degree(90));
        way.add(createNode(ll));
        ll = add(ll, width, new Degree(0));
        way.add(createNode(ll));
        ll = add(ll, depth, new Degree(-90));
        way.add(createNode(ll));
        way.add(startnode);
        OSMWay osmWay = new OSMWay(TagHelper.buildTagGroup("building", "yes"), id++, way);
        ways.add(osmWay);
        return osmWay;
    }

    /**
     * 800m lang. Zwar etwas kurz, das muesste aber reichen.
     * Einfach zwei Nodes.
     * Die Bounds sollen aber so bleiben. die Runway ragt dann rechts raus.
     *
     * @return
     */
    private OSMWay createRunway(LatLon start) {
        double len = 800;
        int segments = 6;
        List<OSMNode> way = new ArrayList<>();
        //die Runway ragt dann rechts raus
        start = add(start, 120, new Degree(270));
        OSMNode startnode = createNode(start);
        way.add(startnode);
        for (int i = 0; i < segments; i++) {
            start = add(start, len / segments, new Degree(90));
            way.add(createNode(start));
        }
        MapBasedTagGroup tags = TagHelper.buildTagGroup(new Pair[]{
                new Pair("aeroway", "runway"),
                new Pair("width", "15"),
        });

        OSMWay osmWay = new OSMWay(tags, id++, way);
        ways.add(osmWay);
        return osmWay;
    }

    private LatLon add(LatLon from, double distance, Degree heading) {
        //22.12.21 SGGeod sgGeod = OsmUtil.toSGGeod(from);
        //sgGeod = sgGeod.applyCourseDistance(heading, distance);
        //sgGeod = sgGeod.applyCourseDistance(heading, distance);
        //return OsmUtil.toLatLon(sgGeod);
        EllipsoidCalculations rbc = new SimpleRoundBodyCalculations();
        return rbc.applyCourseDistance(from,heading, distance);
    }

    private OSMNode createNode(LatLon latlon) {
        OSMNode osmNode = new OSMNode(latlon.getLatDeg().getDegree(), latlon.getLonDeg().getDegree(), null/*buildTagGroup("railway","rail")*/, id++);
        nodes.add(osmNode);
        return osmNode;
    }


}

