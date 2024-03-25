package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import de.yard.threed.core.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Zur besseren Manipulierbarkeit.
 * not self modifying. Quatsch. Der Verwender soll sich halt eine neue anlegen.
 * 
 * <p>
 * Created on 11.08.18.
 */
public class CoordinateList {
    public List<Coordinate> coorlist;

    CoordinateList(Coordinate[] coors) {
        coorlist = new ArrayList(Arrays.asList(coors));
    }

    public CoordinateList(List<Coordinate> list) {
        this.coorlist = list;
    }

    public CoordinateList() {
        coorlist = new ArrayList();
    }

    public CoordinateList(LineSegment line) {
        coorlist = new ArrayList();
        coorlist.add(line.p0);
        coorlist.add(line.p1);
    }
    
    /**
     * cloning constructor
     * @param coordinateList
     */
    public CoordinateList(CoordinateList coordinateList) {
        this();
        coorlist.addAll(coordinateList.coorlist);
    }


    public CoordinateList[] splitByPoints(Coordinate p0, Coordinate p1) {
        int index0 = JtsUtil.findVertexIndex(p0, coorlist);
        int index1 = JtsUtil.findVertexIndex(p1, coorlist);
        // names left/right are just arbitrary
        List<Coordinate> partleft, partright;
        if (index0 < index1) {
            partleft = new ArrayList(coorlist.subList(index0, index1 + 1));
            // skip last (duplicate) coor. sublist.to isType exclusive
            partright = new ArrayList(coorlist.subList(index1, coorlist.size() - 1));
            partright.addAll(new ArrayList(coorlist.subList(0, index0 + 1)));
        } else {
            partleft = new ArrayList(coorlist.subList(index1, index0 + 1));
            partright = new ArrayList(coorlist.subList(index0, coorlist.size() - 1));
            partright.addAll(new ArrayList(coorlist.subList(0, index1 + 1)));
        }
        return new CoordinateList[]{new CoordinateList(partleft), new CoordinateList(partright)};
    }

    public Coordinate[] toArray() {
        return coorlist.toArray(new Coordinate[0]);
    }

    /**
     * not self modifying
     *
     * @return
     */
    public CoordinateList join(CoordinateList list) {
        List<Coordinate> l = new ArrayList();
        l.addAll(coorlist);
        l.addAll(list.coorlist);
        return new CoordinateList(l);
    }

    public void add(Coordinate coordinate) {
        coorlist.add(coordinate);
        }

    public void ensureClosed() {
        //29.7.19: Das ist doch Driss und kaschiert Probleme
        Util.nomore();
        if (!coorlist.get(0).equals(coorlist.get(coorlist.size()-1))) {
            coorlist.add(coorlist.get(0));
        }
    }

    public int size() {
        return coorlist.size();
    }

    public Coordinate get(int i) {
        return coorlist.get(i);
    }

    /**
     * Not self modifying
     *
     * @return
     */
    public CoordinateList reverse() {
        List<Coordinate> l = JtsUtil.toList(coorlist.toArray(new Coordinate[0]).clone());
        Collections.reverse(l);
        return new CoordinateList(l);
    }

    public void set(int i, Coordinate coordinate) {
        coorlist.set(i,coordinate);
    }

    public void add(int i, Coordinate coordinate) {
        coorlist.add(i,coordinate);
    }

    public CoordinateList clone() {
        return new CoordinateList(coorlist.toArray(new Coordinate[0]).clone());
    }
}
