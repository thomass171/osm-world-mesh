package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import org.apache.log4j.Logger;

import java.util.*;

public class MeshLine {
    Logger logger = Logger.getLogger(MeshLine.class);
    private Coordinate[] coordinates;
    private MeshPoint from, to;
    public boolean isBoundary = false;
    //Bei Boundary always "left" isType set, because gridbounds are CCW.
    private AbstractArea left, right;
    //zur Visualisierung und Validierung
    public LineString line;

    private MeshLine(Coordinate[] coordinates, LineString line) {
        this.coordinates = coordinates;
        this.line = line;
        validate();
    }

    public static MeshLine buildMeshLine(Coordinate[] coordinates) {
        LineString line = JtsUtil.createLine(coordinates);
        if (line == null) {
            //already logged
            return null;
        }
        return new MeshLine(coordinates, line);
    }

    public int length() {
        return coordinates.length;
    }

    public Coordinate get(int i) {
        return coordinates[i];
    }

    public int size() {
        return coordinates.length;
    }

    @Override
    public String toString() {
        return "" + from.coordinate + "->" + to.coordinate;
    }

    public MeshPoint getFrom() {
        return from;
    }

    public MeshPoint getTo() {
        return to;
    }

    /**
     * duplicate to below
     *
     * @param coor
     * @return
     */
    @Deprecated
    public int findLineIndex(Coordinate coor) {
        List<LineSegment> l = JtsUtil.buildLineSegmentList(coordinates);
        int index = JtsUtil.getCoveringLine(coor, l);
        return index;
    }

    public int getCoveringSegment(Coordinate c) {
        List<LineSegment> l = JtsUtil.buildLineSegmentList(coordinates);
        //problems will ocuur when point isType connecting point hitting two lines?
        return JtsUtil.getCoveringLine(c, l);
    }

    public int findCoordinate(Coordinate coor) {
        int index = JtsUtil.findCoordinate(coor, coordinates);
        return index;
    }

    public boolean contains(Coordinate coordinate) {
        for (int i = 0; i < coordinates.length; i++) {
            if (coordinates[i].equals2D(coordinate)) {
                return true;
            }
        }
        return false;
    }

    public Coordinate[] getCoordinates() {
        return coordinates;//ineffizient Collections.unmodifiableList(Arrays.asList(coordinates));
    }

    /**
     * Add additional coordinate.
     *
     * @param c
     */
    public void insert(int index, Coordinate c) {
        List<Coordinate> l = new ArrayList(Arrays.asList(coordinates));
        l.add(index, c);
        coordinates = (Coordinate[]) l.toArray(new Coordinate[0]);
        line = JtsUtil.createLine(coordinates);
        validate();
    }

    public void setFrom(MeshPoint p) {
        from = p;
        validate();
    }

    public void setTo(MeshPoint p) {
        to = p;
        validate();
    }

    public void validate() {
        if (from != null && to != null && from == to) {
            //warum sollte from nicht gleich to sein? Wenns doch closed ist.
            //logger.error("from==to");
            //SceneryContext.getInstance().warnings.add("invalid mesh line found");
        }

        //Konsistenzcheck auf doppelte
        boolean isClosed = isClosed();
        for (int i = 0; i < coordinates.length - ((isClosed) ? 1 : 0); i++) {
            if (JtsUtil.findVertexIndex(coordinates[i], coordinates) != i) {
                logger.error("duplicate coordinate?");
                SceneryContext.getInstance().warnings.add("invalid mesh line found");
            }
        }
        if (from != null && !from.coordinate.equals2D(coordinates[0])) {
            logger.error("from not first coordinate");
            SceneryContext.getInstance().warnings.add("invalid mesh line found");
        }
        if (to != null && !to.coordinate.equals2D(coordinates[length() - 1])) {
            logger.error("to not last coordinate");
            SceneryContext.getInstance().warnings.add("invalid mesh line found");
        }
    }

    public void setCoordinatesAndTo(Coordinate[] toArray, MeshPoint p) {
        this.coordinates = toArray;
        to = p;
        validate();
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
        return coordinates[0].equals2D(coordinates[coordinates.length - 1]);
    }

    /**
     * Returns closest distance found.
     *
     * @param c
     */
    public double getDistance(Coordinate c) {
        double bestdistance = Double.MAX_VALUE;
        LineSegment[] segs = JtsUtil.toLineSegments(coordinates);
        for (LineSegment seg : segs) {
            double distance = seg.distance(c);
            if (distance < bestdistance) {
                bestdistance = distance;
            }
        }
        return bestdistance;
    }
}
