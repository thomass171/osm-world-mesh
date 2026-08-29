package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Degree;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2graph.osm.CoordinateList;
import de.yard.threed.osm2scenery.polygon20.*;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.util.GeoCoordinatePair;
import de.yard.threed.osm2world.MapWaySegment2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;

import java.util.*;
import java.util.stream.Collectors;

import static de.yard.threed.osm2graph.osm.JtsUtil.geoCoordinatesToCoordinates;

/**
 * A wayConnector is just a special polygon
 */
@Data
@Entity
@Table(name = "meshpolygon")
@Slf4j
@NoArgsConstructor
public class PersistedMeshPolygon extends AuditedEntity implements MeshPolygon/*10.8.26, MeshWayConnector */{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meshpolygon_id_generator")
    @SequenceGenerator(name = "meshpolygon_id_generator", sequenceName = "meshpolygon_seq", allocationSize = 1)
    private Long id;

    // 1=, 22=boundary
    @Convert(converter = MeshPolygonTypeConverter.class)
    @Column(name = "type")
    private MeshPolygonType type;

    @Column(name = "osmid")
    private Long osmId;

    /*@ManyToMany
    @JoinColumn(name = "from_node", nullable = false)
    private List<PersistedMeshNode> fromNode;*/

    // Even though using a List might be inefficient in case of modifications
    @OneToMany(mappedBy = "meshPolygon", /* orphanRemoval = true,leads to unsolvable NOT NULL constraint violations*/ cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    //@Setter
    //No @Getter because it is not sorted. Use getNodes();
    private List<PersistedMeshPolygonNode> meshPolygonNodes = new ArrayList<>();

    @OneToMany(mappedBy = "meshPolygon",/* orphanRemoval = true,leads to unsolvable NOT NULL constraint violations*/ cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<PersistedMeshNodePair> nodePairs = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "mesh_id", referencedColumnName = "id", nullable = false)
    private PersistedMesh mesh;

    /**
     * Optional index for better testing/debugging
     * 28.4.24 wierd, no good idea
     *
     * @Transient private int lineIndex = uniqueIndex++;
     * private static int uniqueIndex = 0;
     */

    public PersistedMeshPolygon(Coordinate[] coordinates, LineString line) {
        Util.nomore();
        /*this.coordinates = coordinates;
        this.line = line;
        //TODO validate();*/
    }


    public static PersistedMeshPolygon buildMeshLine(Coordinate[] coordinates) {
        LineString line = JtsUtil.createLine(coordinates);
        if (line == null) {
            //already logged
            return null;
        }
        return new PersistedMeshPolygon(coordinates, line);
    }

    public int length() {
        return getCoordinates().length;
    }

    public Coordinate get(int i) {
        return getCoordinates()[i];
    }

    public int size() {
        return getCoordinates().length;
    }

    /**
     * Better have custom one instead of Lombok, which might lead to StackOverflowError because of the bidirectional relationship between MeshPolygon and MeshNode.
     */
    @Override
    public String toString() {
        return "PersistedMeshPolygon(osmId=" + getOsmId() + ")";
    }

    /**
     * duplicate to below
     *
     * @param coor
     * @return
     */
    @Deprecated
    public int findLineIndex(Coordinate coor) {
        List<LineSegment> l = JtsUtil.buildLineSegmentList(getCoordinates());
        int index = JtsUtil.getCoveringLine(coor, l);
        return index;
    }

    public int getCoveringSegment(Coordinate c) {
        List<LineSegment> l = JtsUtil.buildLineSegmentList(getCoordinates());
        //problems will ocuur when point isType connecting point hitting two lines?
        return JtsUtil.getCoveringLine(c, l);
    }

    public int findCoordinate(Coordinate coor) {
        int index = JtsUtil.findCoordinate(coor, getCoordinates());
        return index;
    }

    public boolean contains(Coordinate coordinate) {
        for (int i = 0; i < getCoordinates().length; i++) {
            if (getCoordinates()[i].equals2D(coordinate)) {
                return true;
            }
        }
        return false;
    }

    public Coordinate[] getCoordinates() {
        Util.notyet();
        return null;//new Coordinate[]{fromNode.getCoordinate(), toNode.getCoordinate()};//ineffizient Collections.unmodifiableList(Arrays.asList(coordinates));
    }

    /**
     * Add additional coordinate.
     *
     * @param c
     */
    public void insert(int index, Coordinate c) {
        Util.nomore();
        /*9.4.24 List<Coordinate> l = new ArrayList(Arrays.asList(coordinates));
        l.add(index, c);
        coordinates = (Coordinate[]) l.toArray(new Coordinate[0]);
        line = JtsUtil.createLine(coordinates);*/
    }


    public void setCoordinatesAndTo(Coordinate[] toArray, MeshNode p) {
        Util.nomore();
        /*9.4.24 this.coordinates = toArray;
        toNode = (PersistedMeshNode) p;*/
    }


    public boolean isClosed() {
        Util.nomore();
        return false;
        //9.4.24 return coordinates[0].equals2D(coordinates[coordinates.length - 1]);
    }

    /**
     * Returns sorted list by index including the duplicate end node.
     */
    /*11.8.26 Moved to interface @Override
    public List<MeshNode> getNodes() {
        List<MeshNode> result = new ArrayList<>();
        meshPolygonNodes.stream().sorted(new Comparator<PersistedMeshPolygonNode>() {
            @Override
            public int compare(PersistedMeshPolygonNode o1, PersistedMeshPolygonNode o2) {
                return Integer.compare(o1.getIndex(), o2.getIndex());
            }
        }).forEach(pn -> result.add(pn.meshNode));
        return result;
    }*/

    @Override
    public GeoPolygon getGeoPolygon() {

        try {
            return new GeoPolygon(getPolygonCoordinates());
        } catch (MeshInconsistencyException e) {
            // 11.7.26: this should not happen because the polygon is already validated when created
            // if it happens though, it is to be solved during creation of the polygon, not here. So throw a runtime exception.
            // 26.8.26: Better not that restrictive, because it might just happen. Only log it and return null. The caller should handle it.
            log.error("MeshInconsistencyException while creating GeoPolygon for OSM element {}: " + e.getMessage(), osmId);
            //throw new RuntimeException(e);
            return null;
        }
    }

    private List<GeoCoordinate> getPolygonCoordinates() {
        List<GeoCoordinate> geoCoordinates = getGeoCoordinates();
        // Need to add closing node to make it a valid polygon
        geoCoordinates.add(geoCoordinates.get(0));
        return geoCoordinates;
    }

    /**
     * Because the return value is no polygon, it will not contain the duplicate end node.
     */
    public List<GeoCoordinate> getGeoCoordinates() {
        List<MeshNode> nodes = getNodesSortedByIndex();
        List<GeoCoordinate> result = new ArrayList<>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            result.add(((PersistedMeshNode) nodes.get(i)).getGeoCoordinate());
        }
        return result;
    }


    /*28.4.24 wierd, no good idea public static void resetIndex() {
        uniqueIndex=0;
    }*/

    public void addNode(PersistedMeshNode n) {
        PersistedMeshPolygonNode pn = new PersistedMeshPolygonNode();
        pn.setMeshPolygon(this);
        pn.setMeshNode(n);
        pn.setIndex(meshPolygonNodes.size());
        meshPolygonNodes.add(pn);
    }

    public void close() {
        PersistedMeshPolygonNode pn = new PersistedMeshPolygonNode();
        pn.setMeshPolygon(this);
        pn.setMeshNode(meshPolygonNodes.get(0).getMeshNode());
        pn.setIndex(meshPolygonNodes.size());
        meshPolygonNodes.add(pn);
    }



    public Pair<Integer, Integer> getAttachIndices(long wayOsmId, Degree heading) {
        for (PersistedMeshNodePair np : nodePairs) {
            if (np.getOsmId() == wayOsmId && Math.abs(np.getHeading().getDegree() - heading.getDegree()) < 1.0) {
                return new Pair<>(np.getRight(), np.getLeft());
            }
        }
        return null;
    }

    @Override
    public List<? extends MeshPolygonNode> getPolygonNodes(){
        return meshPolygonNodes;
    }

    public boolean isValidForSave() {
        try {
            // 'silently' will not throw an exception but return null in case of invalid polygon
            return JtsUtil.createLinearRingFromCoordinateList(new CoordinateList(geoCoordinatesToCoordinates(getPolygonCoordinates())), true) != null;
        } catch (MeshInconsistencyException e) {
            return false;
        }
    }

    /*10.8.26 too special public int getNodeIndex(PersistedMeshNode meshNode) {
        for (MeshNode pn : meshPolygonNodes) {
            if (pn.getId() == meshNode.getId()) {
                // Avoid returning the closing node index, which is the same as the first node index.
                // This is important for the getAttachCoordinates() method.
                if (pn.getIndex() != meshPolygonNodes.size() - 1) {
                    return pn.getIndex();
                }
            }
        }
        return -1;
    }*/

    @Converter
    public static class MeshPolygonTypeConverter implements AttributeConverter<MeshPolygonType, Integer> {

        @Override
        public Integer convertToDatabaseColumn(MeshPolygonType meshType) {
            return meshType.getType();
        }

        @Override
        public MeshPolygonType convertToEntityAttribute(Integer dbValue) {
            return MeshPolygonType.fromDbValue(dbValue);
        }
    }
}
