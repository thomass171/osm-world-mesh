package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;

public interface MeshLine {


    int length();

     Coordinate get(int i);
     int size();

     MeshNode getFrom();

     MeshNode getTo();

    /**
     * duplicate to below
     *
     * @param coor
     * @return
     */
    @Deprecated
    int findLineIndex(Coordinate coor);

    public int getCoveringSegment(Coordinate c) ;

    public int findCoordinate(Coordinate coor);

    public boolean contains(Coordinate coordinate);

    public Coordinate[] getCoordinates() ;

    /**
     * Add additional coordinate.
     *
     * @param c
     */
     void insert(int index, Coordinate c) ;

     void setFrom(MeshNode p) ;

     void setTo(MeshNode p);

     void setCoordinatesAndTo(Coordinate[] toArray, MeshNode p);

    MeshArea getLeft();

    MeshArea getRight();

     void setLeft(MeshArea area) ;

     void setRight(MeshArea area);

     boolean isClosed();

    /**
     * Returns closest distance found.
     *
     * @param c
     */
     double getDistance(Coordinate c);

     public void setBoundary(boolean isBoundary);

    boolean isBoundary();

    LineString getLine();

    int getType();

    static boolean isBackgroundTriangulation(int type) {
        return type == 2;
    }

    void setType(int type);

    LineSegment getLineSegment();

    // 28.4.24 wierd, no good idea . Only the caller knows what a good label is
    String getLabel();
}
