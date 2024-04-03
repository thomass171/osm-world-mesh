package de.yard.threed.osm2scenery.scenery.components;

import de.yard.threed.core.Vector2;
import de.yard.threed.core.Vector3;
import de.yard.threed.graph.Graph;
import de.yard.threed.graph.GraphEdge;
import de.yard.threed.graph.GraphNode;
import de.yard.threed.graph.GraphPathSegment;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.MapWaySegment;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.VectorXZ;

import java.util.ArrayList;
import java.util.List;

/**
 * Subgraph(edges) that represents the graph edges of one mapway inside a graph of road, railway, river, taxiway.
 * Hooked into GraphEdge for analyzing.
 * <p>
 * Created on 26.07.18.
 */
public class GraphComponent extends de.yard.threed.graph.GraphComponent {
    //4.4.19: ist eigentlich doppelte Datenhaltung
    List<GraphEdge> edgelist;
    List<GraphPathSegment> seglist;
    SceneryWayObject parent;
    //centerline isType used for building outlines.
    List<Vector2> centerLine;
    SceneryObject.Category category;

    /**
     * parent isType for analyzing/debugging?.
     *
     * @param parent
     * @param category
     */
    public GraphComponent(SceneryWayObject parent, SceneryObject.Category category, SceneryContext sceneryContext) {
        this.parent = parent;
        edgelist = new ArrayList<>();
        Graph graph = sceneryContext/*3.4.24 SceneryContext.getInstance()*/.getGraph(category);
        this.category = category;

        for (MapWaySegment line : parent.mapWay.getMapWaySegments()) {
            //13.8.19: Segment ausserhalb skippen
            if (line.getStartNode().location == MapNode.Location.OUTSIDEGRID || line.getEndNode().location == MapNode.Location.OUTSIDEGRID) {
                //ignore
            } else {
                GraphEdge edge = addSegmentToGraph(graph, line, parent.id);
                edge.customdata = this;
                edgelist.add(edge);
            }
        }

        seglist = new ArrayList<>();
        centerLine = new ArrayList<>();
        for (GraphEdge edge : edgelist) {
            //die enternode durfte fuer den Zweck der outline Bildung egal sein.
            seglist.add(new GraphPathSegment(edge, edge.from));
            if (centerLine.size() == 0) {
                centerLine.add(new Vector2(edge.from.getLocation().getX(), edge.from.getLocation().getY()));
            }
            centerLine.add(new Vector2(edge.to.getLocation().getX(), edge.to.getLocation().getY()));
        }

    }

    public GraphNode findNodeFromOsmNode(MapNode mapNode) {
        Graph graph = SceneryContext.getInstance().getGraph(/*8.11.21 SceneryObject.Category.ROAD*/category);
        GraphNode n = graph.findNodeByName("" + mapNode.getOsmId());
        return n;
    }

    public List<Vector2> getCenterLine() {
        return centerLine;
    }

    //public abstract Graph getGraph();

    /**
     * Ueber das Layer kann man nachher ausserhalb des Grid liegende Teile wieder loeschen.
     *
     * @param graph
     * @param line
     * @param layer
     * @return
     */
    public static GraphEdge addSegmentToGraph(Graph graph, MapWaySegment line, int layer) {
        GraphNode n1 = addGraphNodeToGraph(graph, line.getStartNode());
        GraphNode n2 = addGraphNodeToGraph(graph, line.getEndNode());
        return graph.connectNodes(n1, n2, "" + line.getOsmWay().id, layer);
    }

    public static GraphNode addGraphNodeToGraph(Graph graph, MapNode mapNode) {
        OSMNode osmNode = mapNode.getOsmNode();
        if (graph == null) {
            int h = 9;
        }
        GraphNode graphNode = graph.findNodeByName("" + osmNode.id);
        if (graphNode != null) {
            return graphNode;
        }
        VectorXZ l = mapNode.getPos();
        //20.7.18:jetzt auch z0
        graphNode = graph.addNode("" + osmNode.id, new Vector3((float) l.x, (float) l.z, 0));
        return graphNode;
    }


}
