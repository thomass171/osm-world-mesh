package de.yard.threed.osm2world;


import de.yard.threed.core.Vector2;
import de.yard.threed.osm2graph.osm.OsmUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Alternative/Ergaenzung fuer MapWaySegemt, weil das zu fein erscheint (ausser fuer Bridging, da ist es erforderlich,24.8.18: wirklich?).
 * 11.7.18 Neuer Nutzungsversuch. MapWaySegment ist aber trotzdem hilfreich, um die RightDirection zu bekommen.
 * Ist halt einfach eine Abstraktionsebene über dem WaySegment.
 * 24.8.18: An Gridschnittpunkten werden zusaetzliche synthetische MapNodes eingefügt. Das
 * macht aber der Aufrufer.
 */
public class MapWay /*extends MapSegment*/ implements MapElement {
    //27.7.18: Now part of list mapnodes
    //protected MapNode startNode;
    //protected MapNode endNode;

    private final OSMWay osmWay;

    private List<WaySegmentWorldObject> representations = new ArrayList<WaySegmentWorldObject>(1);
    // Order of segments it start->end. Polygon creatore rely on this. The same applies to start/end in the Segment.
    List<MapWaySegment> mapWaySegs = new ArrayList<MapWaySegment>();
    public List<MapWaySegment2> segment2s = new ArrayList<MapWaySegment2>();
    List<MapNode> mapNodes = new ArrayList<MapNode>();
    @SuppressWarnings("unchecked") //is later checked for EMPTY_LIST using ==
    private Collection<MapOverlap<?, ?>> overlaps = Collections.EMPTY_LIST;

    /*public MapWay(MapNode startNode, MapNode endNode, OSMWay osmWay) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.osmWay = osmWay;
        //startNode.addMapWay(this);
        //endNode.addMapWay(this);
    }*/

    /**
     * if no add() will be called the end node is missing.
     * Caller must check this.
     *
     * @param startNode
     * @param osmWay
     */
    public MapWay(MapNode startNode, OSMWay osmWay) {
        //this.startNode = startNode;
        this.osmWay = osmWay;
        this.mapNodes.add(startNode);
    }

    /*3.5.26 public static MapWaySegmentKey buildWaySegmentKey(Long osmWayId, int waySegment) {
        return new MapWaySegmentKey(osmWayId, waySegment);
    }*/

    @Override
    public int getLayer() {
        if (osmWay.tags.containsKey("layer")) {
            try {
                return Integer.parseInt(osmWay.tags.getValue("layer"));
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }
        return 0;
    }

    public OSMWay getOsmWay() {
        return osmWay;
    }

    @Override
    public TagGroup getTags() {
        return getOsmWay().tags;
    }

    @Override
    public long getOsmId() {
        return osmWay.id;
    }

    public void addOverlap(MapOverlap<?, ?> overlap) {
        assert overlap.e1 == this || overlap.e2 == this;
        if (overlaps == Collections.EMPTY_LIST) {
            overlaps = new ArrayList<MapOverlap<?, ?>>();
        }
        overlaps.add(overlap);
    }

    @Override
    public Collection<MapOverlap<?, ?>> getOverlaps() {
        return overlaps;
    }

    /*11.11.21
    public Iterable<MapIntersectionWW> getIntersectionsWW() {
        return Iterables.filter(overlaps, MapIntersectionWW.class);
    }*/

    @Override
    public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
        //return new AxisAlignedBoundingBoxXZ(Arrays.asList(
        //		startNode.getPos(), endNode.getPos()));
        throw new RuntimeException("not yet");
        //return null;
    }

    @Override
    public List<WaySegmentWorldObject> getRepresentations() {
        return representations;
    }

    @Override
    public WaySegmentWorldObject getPrimaryRepresentation() {
        if (representations.isEmpty()) {
            return null;
        } else {
            return representations.get(0);
        }
    }

    public MapNode getStartNode() {
        return mapNodes.get(0);
    }

    public MapNode getEndNode() {
        return mapNodes.get(mapNodes.size() - 1);
    }

    /**
     * adds a visual representation for this way segment
     */
    /*public void addRepresentation(WaySegmentWorldObject representation) {
		this.representations.add(representation);
	}*/
    @Override
    public String toString() {
        return "" + getOsmId() + "(" + getStartNode() + "->" + getEndNode() + ")";
    }

    public void add(MapNode node, MapWaySegment mapWaySeg) {
        //this.endNode = node;
        this.mapWaySegs.add(mapWaySeg);
        this.mapNodes.add(node);
    }

    public List<MapWaySegment> getMapWaySegments() {
        return mapWaySegs;
    }

    public List<MapNode> getMapNodes() {
        return mapNodes;
    }

    public boolean isOuterNode(MapNode node) {
        return node == getStartNode() || node == getEndNode();
    }

    public boolean isStartNode(MapNode node) {
        return node == getStartNode();
    }

    public boolean isEndNode(MapNode node) {
        return node == getEndNode();
    }

    public static List<Vector2> getCenterline(List<MapNode> nodes) {
        List<Vector2> centerLine = new ArrayList<>();
        for (MapNode n : nodes) {
            centerLine.add(OsmUtil.toVector2(n.getPos()));
        }
        return centerLine;
    }

    public void addSegment(MapWaySegment2 currentSegment) {
        segment2s.add(currentSegment);
    }

    public MapWaySegment2 getFirstSegment() {
        return segment2s.get(0);
    }

    public MapWaySegment2 getLastSegment() {
        return segment2s.get(segment2s.size() - 1);
    }

    public MapWaySegment2 getSegment(int index) {
        return segment2s.get(index);
    }
}
