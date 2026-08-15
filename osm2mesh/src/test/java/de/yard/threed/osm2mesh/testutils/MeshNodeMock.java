package de.yard.threed.osm2mesh.testutils;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;

import java.util.List;

public class MeshNodeMock implements MeshNode {

    private final GeoCoordinate geoCoordinate;

    public MeshNodeMock(GeoCoordinate geoCoordinate){
        this.geoCoordinate=geoCoordinate;
    }

    @Override
    public Coordinate getCoordinate() {
        return null;
    }

    @Override
    public GeoCoordinate getGeoCoordinate() {
        return geoCoordinate;
    }

    @Override
    public void addLine(MeshLine line) {

    }

    @Override
    public void removeLine(MeshLine line) {

    }

    @Override
    public int getLineCount() {
        return 0;
    }

    @Override
    public List<MeshLine> getLines() {
        return List.of();
    }

    @Override
    public String getLabel() {
        return "";
    }
}
