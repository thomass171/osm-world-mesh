package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static de.yard.threed.osm2graph.osm.JtsUtil.*;

/**
 * Der Incremental algorithm aus https://en.wikipedia.org/wiki/Point_set_triangulation
 * 9.8.18: Ist auch Kappes; kann mit holes nicht funktionieren.
 * Abwarten.
 * Arbeitet im Prinzip wie der JTS Conforming...
 * Triangulation einer Punktwolke mit vorgebenen Constraints. Arbeitet dafuer erstmal nur mit Lines.
 * Triangles entstehen erst zum Schluss
 * 
 * 11.8.18: Das geht nicht mit Holes (vorgegebenen Constraint Edges)!
 * 
 * <p>
 * Created on 10.08.18.
 */
public class Triangulator {
    List<LineSegment> lines = new ArrayList<LineSegment>();
    Coordinate[] coor;
    List<Polygon> pholes;
    Polygon polygon;
    Map<Coordinate, List<LineSegment>> linemap = new HashMap<>();

    public Triangulator(Polygon polygon) {
        this.polygon = polygon;
        coor = polygon.getCoordinates();
        pholes = new ArrayList<>();
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            Coordinate[] holeoutline = polygon.getInteriorRingN(i).getCoordinates();
            Polygon hole = GF.createPolygon(holeoutline);
            pholes.add(hole);
        }
    }

    /**
     */
    public List<LineSegment> buildGrid() {

        // da gibt es auf jeden Fall doppelte. Remove ueber Hashmap und sortieren
        HashSet<Coordinate> set = new HashSet<Coordinate>();
        for (int i = 0; i < coor.length; i++) {
            set.add(coor[i]);
        }
        Arrays.sort(coor, new Comparator<Coordinate>() {
            @Override
            public int compare(Coordinate o1, Coordinate o2) {
                if (o1.x < o2.x)
                    return -1;
                if (o1.x > o2.x)
                    return 1;
                return 0;
            }
        });
        coor = new Coordinate[set.size()];
        Iterator<Coordinate> it = set.iterator();
        int index = 0;
        while (it.hasNext()) {
            coor[index] = it.next();
            //nur fuer Debug
            coor[index].setOrdinate(Coordinate.Z, index);
            index++;
        }

        // Lines fuer die Hole outlines vorgeben
        for (Polygon hole : pholes) {
            lines.addAll(buildLineSegmentList(hole.getExteriorRing().getCoordinates()));
        }

        // Initiales Triangle
        lines.add(addLine(coor[0], coor[1]));
        lines.add(addLine(coor[1], coor[2]));
        lines.add(addLine(coor[0], coor[2]));
        for (int i = 3; i < coor.length; i++) {
            // Coordinate last = null;
            Coordinate current = coor[i];
            for (int j = 0; j < i; j++) {
                Coordinate c = coor[j];
                if (isVisible(c, current)) {

                    //       if (last != null) {
                    //triangles.add(createTriangle(current, c, last));
                    lines.add(new LineSegment(c, current));

                }
                //     last = c;
                //}
            }
        }

        List<Polygon> triangles = new ArrayList<Polygon>();


        triangles = removeTrianglesInHolesAndOutside(polygon, pholes, triangles);
        //return triangles;
        return lines;
    }

    boolean isVisible(Coordinate checkpoint, Coordinate fromhere) {
        LineSegment viewline = new LineSegment(checkpoint, fromhere);
        for (LineSegment p : lines) {
            Coordinate intersectionPoint;
            if ((intersectionPoint = viewline.intersection(p)) != null) {
                // Die Blicklinie schneidet eine existierende Edge.
                if (!intersectionPoint.equals(checkpoint)) {
                    // Schnittpunkt ist nicht der Zielpunkt. Dann ist er nicht visible
                    return false;
                }

            }
        }
        return true;
    }

    private LineSegment addLine(Coordinate c0, Coordinate c1) {
        LineSegment line = new LineSegment(c0, c1);
        addToMap(c0, line);
        return line;
    }

    private void addToMap(Coordinate c, LineSegment line) {
        List<LineSegment> l = linemap.get(c);
        if (l == null) {
            l = new ArrayList<>();
            linemap.put(c, l);
        }
        l.add(line);
    }
}
