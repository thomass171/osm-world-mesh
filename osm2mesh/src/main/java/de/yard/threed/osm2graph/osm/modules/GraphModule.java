package de.yard.threed.osm2graph.osm.modules;

import de.yard.threed.core.Vector3;
import de.yard.threed.graph.Graph;
import de.yard.threed.graph.GraphNode;
import de.yard.threed.osm2world.*;


import static java.lang.Math.abs;
import static java.lang.Math.max;

/**
 * Variante fuer OSM2World
 * 
 * 28.8.18: das ist doch obselet.
 */
public class GraphModule extends ConfigurableWorldModule {
    public Graph graph = new Graph();

    @Override
    public void applyTo(MapData grid) {

        for (MapWaySegment line : grid.getMapWaySegments()) {
            
                if (isRoad(line.getTags())) {

                    GraphNode n1 = addGraphNode(line.getStartNode());
                    GraphNode n2 = addGraphNode(line.getEndNode());
                    graph.connectNodes(n1, n2, "" + line.getOsmWay().id);
                }
        }
    }

    private GraphNode addGraphNode(MapNode mapNode) {
        OSMNode osmNode = mapNode.getOsmNode();
        GraphNode graphNode = graph.findNodeByName("" + osmNode.id);
        if (graphNode != null) {
            return graphNode;
        }
        VectorXZ l = mapNode.getPos();
        graphNode = graph.addNode("" + osmNode.id, new Vector3((float) l.x, 0, (float) l.z));
        return graphNode;
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

}