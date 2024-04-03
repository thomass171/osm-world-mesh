package de.yard.threed.osm2scenery.scenery.components;


/**
 * Zum Dekorieren eines Sceneryobjects.
 * Für was alles, muss sich noch zeigen.
 *
 * 11.4.19: Fahrbahnmarkierungen aufkleben dürfte in 3D zu z-fighting führen.
 * 25.4.19: Das koennte man auch als {@link de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject} sehen. Klingt plusibel.
 * Nee, {@link de.yard.threed.osm2scenery.scenery.SceneryDecoration}
 */
public interface DecoratorComponent {
    Decoration getDecoration();
}
