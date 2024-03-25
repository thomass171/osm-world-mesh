package de.yard.threed.osm2scenery.modules;

import de.yard.threed.core.Util;
import de.yard.threed.core.Vector3;
import de.yard.threed.graph.Graph;
import de.yard.threed.graph.GraphEdge;
import de.yard.threed.graph.GraphNode;


/**
 *
 */
/*27.5.19 public class GraphModule  extends SceneryModule  {
    private Graph graph = new Graph();

    @Override
    public SceneryObjectList applyTo(MapData grid) {
        //#Das Graphmodule wird gar nicht mehr gebraucht. die static unten aber schon
        Util.nomore();
 /*       for (MapWaySegment line : grid.getMapWaySegments()) {
            ///TODOif (insideBounds(line))
                if (isRoad(line.getTags())) {
                    addSegmentToGraph(graph, line,0);
                }
        }* /
        return new SceneryObjectList();
    }


    private static boolean isRoad(TagGroup tags) {
        if (tags.containsKey("highway")
                && !tags.contains("highway", "construction")
                && !tags.contains("highway", "proposed")) {
            return true;
        } else {
            return tags.contains("railway", "platform")
                    || tags.contains("leisure", "track");
        }
    }

}*/