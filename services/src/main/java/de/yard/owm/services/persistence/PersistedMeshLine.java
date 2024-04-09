package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import lombok.Getter;
import org.apache.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.*;

@Entity
@Table(name="meshline")
public class PersistedMeshLine implements MeshLine {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meshline_id_generator")
    @SequenceGenerator(name = "meshline_id_generator", sequenceName = "meshline_seq", allocationSize = 1)
    private Long id;

    @Transient
    Logger logger = Logger.getLogger(PersistedMeshLine.class);

    @ManyToOne
    @JoinColumn(name="from_node", nullable=false)
    private PersistedMeshNode fromNode;

    @ManyToOne
    @JoinColumn(name="to_node", nullable=false)
    private PersistedMeshNode toNode;
    @Transient
    public boolean isBoundary = false;
    //Bei Boundary always "left" isType set, because gridbounds are CCW.
    @Transient
    private AbstractArea left, right;
    //zur Visualisierung und Validierung
    @Transient
    public LineString line;

    public PersistedMeshLine() {

    }

    public PersistedMeshLine(Coordinate[] coordinates, LineString line) {
        Util.nomore();
        /*this.coordinates = coordinates;
        this.line = line;
        //TODO validate();*/
    }

    public PersistedMeshLine(PersistedMeshNode from, PersistedMeshNode to) {
        this.fromNode = from;
        this.toNode = to;
        from.addLine(this);
        to.addLine(this);
        //TODO validate();
    }

    public static PersistedMeshLine buildMeshLine(Coordinate[] coordinates) {
        LineString line = JtsUtil.createLine(coordinates);
        if (line == null) {
            //already logged
            return null;
        }
        return new PersistedMeshLine(coordinates, line);
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

    @Override
    public String toString() {
        return "" + fromNode.getCoordinate() + "->" + toNode.getCoordinate();
    }

    public MeshNode getFrom() {
        return fromNode;
    }

    public MeshNode getTo() {
        return toNode;
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
        return new Coordinate[]{fromNode.getCoordinate(), toNode.getCoordinate()};//ineffizient Collections.unmodifiableList(Arrays.asList(coordinates));
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

    @Override
    public void setFrom(MeshNode p) {
        fromNode = (PersistedMeshNode) p;
    }

    @Override
    public void setTo(MeshNode p) {
        toNode = (PersistedMeshNode) p;
    }

    public void setCoordinatesAndTo(Coordinate[] toArray, MeshNode p) {
        Util.nomore();
        /*9.4.24 this.coordinates = toArray;
        toNode = (PersistedMeshNode) p;*/
    }

    public AbstractArea getLeft() {
        return left;
    }

    public AbstractArea getRight() {
        return right;
    }

    public void setLeft(AbstractArea area) {
        if (area != null && area.toString().contains("23696494")) {
            int h = 9;
        }
        if (left != null) {
            logger.warn("overriding left?");
        }
        left = area;
    }

    public void setRight(AbstractArea area) {
        if (area != null && area.toString().contains("23696494")) {
            int h = 9;
        }
        if (right != null) {
            logger.warn("overriding right?");
        }

        right = area;

    }

    public boolean isClosed() {
        Util.nomore();
        return false;
        //9.4.24 return coordinates[0].equals2D(coordinates[coordinates.length - 1]);
    }

    /**
     * Returns closest distance found.
     *
     * @param c
     */
    public double getDistance(Coordinate c) {
        double bestdistance = Double.MAX_VALUE;
        LineSegment[] segs = JtsUtil.toLineSegments(getCoordinates());
        for (LineSegment seg : segs) {
            double distance = seg.distance(c);
            if (distance < bestdistance) {
                bestdistance = distance;
            }
        }
        return bestdistance;
    }

    @Override
    public void setBoundary(boolean isBoundary) {

    }

    @Override
    public boolean isBoundary() {
        Util.notyet();
        return false;
    }

    @Override
    public LineString getLine() {
        return null;
    }
}
