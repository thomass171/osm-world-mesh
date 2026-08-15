package de.yard.threed.osm2world;


import de.yard.threed.core.*;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;

import java.util.*;

/**
 * 18.3.26: More clear approach than MapWaySegment.
 * A segment of a way that has no other connection.
 */
public class MapWaySegment2 /*implements MapElement*/ {

    public final MapWay mapWay;

    // Indices to node list in way
    public int startNode;
    public int endNode;
    public final int/*MapWaySegmentKey*/ segmentIndex;

    public MapWaySegment2(MapWay mapWay, int startNode, int endNode, int segmentIndex) {
	/*	if (startNode == null || endNode == null) {
			throw new IllegalArgumentException();
		}*/
        this.startNode = startNode;
        this.endNode = endNode;
        this.mapWay = mapWay;
        this.segmentIndex = segmentIndex;//new MapWaySegmentKey(mapWay.getOsmId(), segmentIndex);
    }

    //@Override
/*	public int getLayer() {
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

	//@Override
	public TagGroup getTags() {
		return getOsmWay().tags;
	}

	//@Override
	public long getOsmId() {
		return osmWay.id;
	}
*/

	/*11.11.21public Iterable<MapIntersectionWW> getIntersectionsWW() {
		return Iterables.filter(overlaps, MapIntersectionWW.class);
	}*/

    //@Override
	/*public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		return new AxisAlignedBoundingBoxXZ(Arrays.asList(
				startNode.getPos(), endNode.getPos()));
	}*/

    public List<MapNode> getMapNodes() {
        List<MapNode> result = new ArrayList<>();
        for (int i = startNode; i <= endNode; i++) {
            result.add(mapWay.mapNodes.get(i));
        }
        return result;
    }

    public MapNode getStartNode() {
        return mapWay.mapNodes.get(startNode);
    }

    public MapNode getEndNode() {
        return mapWay.mapNodes.get(endNode);
    }

    public MapNode getBeforeEndNode() {
        return mapWay.mapNodes.get(endNode - 1);
    }

    @Override
    public String toString() {
        return startNode + "->" + endNode + "(" + getOsmId() + ")";
    }

    public long getOsmId() {
        return mapWay.getOsmId();
    }

    public TagGroup getTags() {
        return mapWay.getTags();
    }

    public boolean isOuterNode(MapNode node) {
        return node == getStartNode() || node == getEndNode();
    }

    public boolean isStartNode(MapNode node) {
        return node.getOsmId() == getStartNode().getOsmId();
    }

    public boolean isEndNode(MapNode node) {
        return node.getOsmId() == getEndNode().getOsmId();
    }

    public List<Vector2> getCenterline() {
        return MapWay.getCenterline(getMapNodes());
    }

    /**
     * 19.3.26 Replacing the widely used getMapWaySegment()
     */
    public List<Pair<MapNode, MapNode>> getMapWayLines() {
        List<Pair<MapNode, MapNode>> result = new ArrayList<>();
        for (int i = startNode; i < endNode; i++) {
            result.add(new Pair<>(mapWay.mapNodes.get(i), mapWay.mapNodes.get(i + 1)));
        }
        return result;
    }

    /**
     * Be prepared to use this class as key in hashmap.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapWaySegment2 mapWaySegment2 = (MapWaySegment2) o;
        return getOsmId() == mapWaySegment2.getOsmId() && segmentIndex == mapWaySegment2.segmentIndex;
    }

    /**
     * Reminder: „equal“ objects must have the same hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(getOsmId());
    }


    public MapWaySegmentAtConnector getKey(boolean atStart) {
        return new MapWaySegmentAtConnector(getOsmId(), (atStart?getStartHeading():getEndHeading()));
    }

    public Degree getStartHeading() {
        MapNode start = getStartNode();
        MapNode next = mapWay.mapNodes.get(startNode + 1);
        Degree heading = MathUtil2.getHeadingFromDirection(OsmUtil.toVector2(next.getPos()).subtract(OsmUtil.toVector2(start.getPos())));
        return heading;
        //Copilot:
        // double dx = next.getPos().getX() - start.getPos().getX();
        //double dz = next.getPos().getZ() - start.getPos().getZ();
        //double angleRad = Math.atan2(dx, dz);
        //double angleDeg = Math.toDegrees(angleRad);
        //return new Degree(angleDeg);
    }

    public Degree getEndHeading() {
        MapNode start = mapWay.mapNodes.get(endNode);
        MapNode next = mapWay.mapNodes.get(endNode-1);
        Degree heading = MathUtil2.getHeadingFromDirection(OsmUtil.toVector2(next.getPos()).subtract(OsmUtil.toVector2(start.getPos())));
        return heading;
    }
}
