package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Eine Beschriftung/Markierung.
 *
 * Wird als (Multi)Polygon abgebildet, der dann aufgeklebt/eingeschnitten wird.
 * Als Masseinheit gilt erstmal Meter.
 *
 * created 18.4.19
 */
public interface Decoration {
    Geometry getGeometry();
}
