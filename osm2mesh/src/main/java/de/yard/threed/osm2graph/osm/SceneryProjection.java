package de.yard.threed.osm2graph.osm;


import de.yard.threed.core.LatLon;
import de.yard.threed.osm2world.MetricMapProjection;
import de.yard.threed.osm2world.VectorXZ;

/**
 * Nochmal sowas, aber speziell ausgelegt auf Scenery.
 *
 * Nutzung von MapProjection ist doof, wergen SGGeod und der bloeden FlightLocation Methode. SGGeod ist aber doch nicht schlimm? Vielleicht etwas gross mit Elevation.
 * 4.6.19: LatLon in engine waere gut.
 *
 * 11.4.19
 *
 */
public interface SceneryProjection {
    VectorXZ project(LatLon latlon);
    LatLon unproject(VectorXZ v);

    MetricMapProjection getBaseProjection();
}
