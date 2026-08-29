package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.polygon20.MeshPolygonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "meshpolygonnode")
@Slf4j
@NoArgsConstructor
public class PersistedMeshPolygonNode extends AuditedEntity implements MeshPolygonNode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meshpolygon_id_generator")
    @SequenceGenerator(name = "meshpolygon_id_generator", sequenceName = "meshpolygon_seq", allocationSize = 1)
    private Long id;

    @Column(name = "index")
    @Setter
    @Getter
    private int index;

    @Getter
    @Setter
    @ManyToOne
    //@MapsId("studentId")
    @JoinColumn(name = "meshpolygon_id")
    PersistedMeshPolygon meshPolygon;

    // Nodes might be shared, so cannot be deleted cascade
    @Getter
    @Setter
    @ManyToOne
    //@MapsId("courseId")
    @JoinColumn(name = "meshnode_id")
    PersistedMeshNode meshNode;

    /**
     * Optional index for better testing/debugging
     28.4.24 wierd, no good idea
    @Transient
    private int lineIndex = uniqueIndex++;
    private static int uniqueIndex = 0;*/

    public PersistedMeshPolygonNode(Coordinate[] coordinates, LineString line) {
        Util.nomore();
        /*this.coordinates = coordinates;
        this.line = line;
        //TODO validate();*/
    }


    public static PersistedMeshPolygonNode buildMeshLine(Coordinate[] coordinates) {
        LineString line = JtsUtil.createLine(coordinates);
        if (line == null) {
            //already logged
            return null;
        }
        return new PersistedMeshPolygonNode(coordinates, line);
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
/*
    @Override
    public String toString() {
        return "" + ":" + fromNode.getCoordinate() + "->" + toNode.getCoordinate();
    }

  */

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




    /*28.4.24 wierd, no good idea public static void resetIndex() {
        uniqueIndex=0;
    }*/

    @Override
    public int getIndex() {
        return index;
    }
}
