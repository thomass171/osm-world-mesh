package de.yard.threed;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Util;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class TraditionalMeshNode implements MeshNode {
    private Long id;

    private double lat;

    private double lon;

    private List<MeshLine> linesOfPoint = new ArrayList();
    public Coordinate coordinate;
    public EleConnectorGroup group;

    public TraditionalMeshNode() {

    }

    public TraditionalMeshNode(Coordinate coordinate) {
        this.coordinate = coordinate;
        //  pool.add(this);
        //  this.id=pool.size();
    }

    /*public void addLineToPoint(MeshLine line) {
        /*if (!linesOfPoint.containsKey(point)) {
            linesOfPoint.put(point, new ArrayList<Integer>());
        }
        linesOfPoint.get(point).add(line);* /
        linesOfPoint.add(line);
    }
*/
    @Override
    public Coordinate getCoordinate(){
        return coordinate;
    }

    @Override
    public GeoCoordinate getGeoCoordinate() {
        Util.notyet();
        return null;
    }

    @Override
    public String toString() {
        return "" + coordinate;
    }

    @Override
    public void addLine(MeshLine line) {
        if (linesOfPoint.contains(line)) {
            //convenience
            return;
        }
        //bei Way junctions gibt es auch mal 4, darum > 3.
        if (linesOfPoint.size() > 3) {
            //21.8.19:aber da ist doch was faul
            log.warn("too many lines at point " + coordinate + "): " + linesOfPoint.size());

        }
        linesOfPoint.add(line);
    }

    @Override
    public void removeLine(MeshLine line) {
        linesOfPoint.remove(line);
    }

    @Override
    public int getLineCount() {
        return linesOfPoint.size();
    }

    @Override
    public List<MeshLine> getLines() {
        return Collections.unmodifiableList(linesOfPoint);
    }

    @Override
    public String getLabel() {
        return "?";
    }
}
