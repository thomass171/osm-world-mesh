package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleCoordinate;

/**
 * analog {@link EleCoordinate}
 */
public class MeshCoordinate {
    public EleConnectorGroup group;
    public Coordinate coordinate;

    public  MeshCoordinate (Coordinate coordinate){
        this.coordinate=coordinate;
    }

}
