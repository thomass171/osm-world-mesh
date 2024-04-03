package de.yard.threed.osm2scenery.elevation;

import com.vividsolutions.jts.geom.Coordinate;

import java.util.List;

public class SimpleEleConnectorGroupFinder implements EleConnectorGroupFinder {
    List<EleConnectorGroup> eleConnectorGroups;

    public SimpleEleConnectorGroupFinder( List<EleConnectorGroup> eleConnectorGroups) {
        this.eleConnectorGroups = eleConnectorGroups;
    }

    @Override
    public EleConnectorGroup findGroupForCoordinate(Coordinate coordinate) {
        //sollte vielleicht die nächstgelegen liefern
        if (eleConnectorGroups.size()==0){
            int h=9;
        }
        return eleConnectorGroups.get(0);
    }
}
