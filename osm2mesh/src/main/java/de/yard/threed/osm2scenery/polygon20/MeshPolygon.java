package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Degree;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2graph.osm.SceneryProjection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 2.5.24: Now an interface like MeshLine and MeshNode
 * 12.2.26: Also no setter
 */
public interface MeshPolygon {

    /**
     * returns unspecific order
     */
    List<? extends MeshPolygonNode> getPolygonNodes();

    default List<MeshPolygonNode> getPolygonNodesSortedByIndex() {
        List<MeshPolygonNode> result = new ArrayList<>();
        getPolygonNodes().stream().sorted(new Comparator<MeshPolygonNode>() {
            @Override
            public int compare(MeshPolygonNode o1, MeshPolygonNode o2) {
                return Integer.compare(o1.getIndex(), o2.getIndex());
            }
        }).forEach(pn -> result.add(pn));
        return result;
    }

    /**
     * Returns sorted list by index including the duplicate end node.
     */
    default List<MeshNode> getNodesSortedByIndex() {
        List<MeshNode> result = new ArrayList<>();
        getPolygonNodesSortedByIndex().stream().forEach(pn -> result.add(pn.getMeshNode()));
        return result;
    }

    //Moved to MeshHelper, but now we have getGeoPolygon() Polygon getProjectedPolygon(SceneryProjection projection);

    /**
     * 11.7.26 Really throwing? Shouln't a MeshPolygon be consistent? Try without.
     */
    GeoPolygon getGeoPolygon();//11.7.26  throws MeshInconsistencyException;

    Long getOsmId();

    MeshPolygonType getType();

    /// 10.8.26 Following moved here from MeshWayConnector
    /**
     * Connector is expected to exist, so we can return nodes
     * Pair in orientation of way (right=first).
     * 13.5.26 No, in orientation from heading from connector
     * 10.8.26 it is easier to derive node from index than index from node. So nly have that one.
     */
    /*GeoCoordinatePair*///Pair<? extends MeshNode,? extends MeshNode> getAttachCoordinates(long wayOsmId, Degree heading/*MapWaySegmentKey key*/) throws MeshInconsistencyException;
    default Pair<? extends MeshNode, ? extends MeshNode> getAttachCoordinates(long wayOsmId, Degree heading/*MapWaySegmentKey key*/) throws MeshInconsistencyException {
            /*for (PersistedMeshNodePair np :  {
                if (np.getOsmId() == wayOsmId && (int) np.getHeading().getDegree() == (int) heading.getDegree()) {
                    // first=right
                    return new Pair<>(getNodes().get(np.getRight()), getNodes().get(np.getLeft()));
                }
            }*/
        Pair<Integer, Integer> attachIndices = getAttachIndices(wayOsmId, heading);
        if (attachIndices != null) {
            List<MeshNode> l = getNodesSortedByIndex();
            return new Pair<>(l.get(attachIndices.getFirst()), l.get(attachIndices.getSecond()));
        }
        return null;
    }

    /**
     * Should round degree to int.
     */
    /*GeoCoordinatePair*/Pair<Integer, Integer> getAttachIndices(long wayOsmId, Degree heading) throws MeshInconsistencyException;


}
