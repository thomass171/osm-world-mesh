package de.yard.threed;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TraditionalMeshLine implements MeshLine {
    private Long id;

    Logger logger = Logger.getLogger(MeshLine.class);
    private Coordinate[] coordinates;
    private MeshNode from, to;
    public boolean isBoundary = false;
    //Bei Boundary always "left" isType set, because gridbounds are CCW.
    private AbstractArea left, right;
    //zur Visualisierung und Validierung
    public LineString line;

    public TraditionalMeshLine() {

    }

    public TraditionalMeshLine(Coordinate[] coordinates, LineString line) {
        this.coordinates = coordinates;
        this.line = line;
        //TODO validate();
    }

    @Override
    public int length() {
        return coordinates.length;
    }

    @Override
    public Coordinate get(int i) {
        return coordinates[i];
    }

    @Override
    public int size() {
        return coordinates.length;
    }

    @Override
    public String toString() {
        return "" + from.getCoordinate() + "->" + to.getCoordinate();
    }

    public MeshNode getFrom() {
        return from;
    }

    public MeshNode getTo() {
        return to;
    }

    /**
     * duplicate to below
     *
     * @param coor
     * @return
     */
    @Deprecated
    @Override
    public int findLineIndex(Coordinate coor) {
        List<LineSegment> l = JtsUtil.buildLineSegmentList(coordinates);
        int index = JtsUtil.getCoveringLine(coor, l);
        return index;
    }

    @Override
    public int getCoveringSegment(Coordinate c) {
        List<LineSegment> l = JtsUtil.buildLineSegmentList(coordinates);
        //problems will ocuur when point isType connecting point hitting two lines?
        return JtsUtil.getCoveringLine(c, l);
    }

    @Override
    public int findCoordinate(Coordinate coor) {
        int index = JtsUtil.findCoordinate(coor, coordinates);
        return index;
    }

    @Override
    public boolean contains(Coordinate coordinate) {
        for (int i = 0; i < coordinates.length; i++) {
            if (coordinates[i].equals2D(coordinate)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Coordinate[] getCoordinates() {
        return coordinates;//ineffizient Collections.unmodifiableList(Arrays.asList(coordinates));
    }

    /**
     * Add additional coordinate.
     *
     * @param c
     */
    @Override
    public void insert(int index, Coordinate c) {
        List<Coordinate> l = new ArrayList(Arrays.asList(coordinates));
        l.add(index, c);
        coordinates = (Coordinate[]) l.toArray(new Coordinate[0]);
        line = JtsUtil.createLine(coordinates);
    }

    public void setFrom(MeshNode p) {
        from = p;
    }

    @Override
    public void setTo(MeshNode p) {
        to = p;
    }


    @Override
    public void setCoordinatesAndTo(Coordinate[] toArray, MeshNode p) {
        this.coordinates = toArray;
        to = p;
    }

    @Override
    public AbstractArea getLeft() {
        return left;
    }

    @Override
    public AbstractArea getRight() {
        return right;
    }

    @Override
    public void setLeft(AbstractArea area) {
        if (area != null && area.toString().contains("23696494")) {
            int h = 9;
        }
        if (left != null) {
            logger.warn("overriding left?");
        }
        left = area;
    }

    @Override
    public void setRight(AbstractArea area) {
        if (area != null && area.toString().contains("23696494")) {
            int h = 9;
        }
        if (right != null) {
            logger.warn("overriding right?");
        }

        right = area;

    }

    @Override
    public boolean isClosed() {
        return coordinates[0].equals2D(coordinates[coordinates.length - 1]);
    }

    /**
     * Returns closest distance found.
     *
     * @param c
     */
    @Override
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

    public void setBoundary(boolean isBoundary) {
        this.isBoundary = isBoundary;
    }

    public boolean isBoundary() {
        return isBoundary;
    }

    public LineString getLine() {
        return line;
    }

    @Override
    public int getType() {
        return -1;
    }

    @Override
    public void setType(int type) {

    }

    @Override
    public LineSegment getLineSegment() {
       throw new RuntimeException("not possible");
    }

    @Override
    public String getLabel() {
        return "?";
    }
}
