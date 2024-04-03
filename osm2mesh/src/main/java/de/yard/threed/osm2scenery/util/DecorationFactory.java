package de.yard.threed.osm2scenery.util;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Vector2;

import de.yard.threed.core.geometry.Shape;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.scenery.components.Decoration;
import de.yard.threed.osm2scenery.scenery.components.DecorationGeometry;
import org.apache.log4j.Logger;

/**
 * Verwendet zumindest teilweise Shapes, weil die schon rotate, transform etc. haben.
 * 28.5.19: Deprecated zugunsten GeneralPath und DecorationTexture und tools/DecorationFactory
 */
@Deprecated
public class DecorationFactory {
    static Logger logger = Logger.getLogger(DecorationFactory.class);

    /**
     * z.B. für Runway Nummern
     *
     * @param s
     * @return
     */
    public static Decoration buildFromString(String s) {
        return null;
    }

    /**
     * Liefert die gelben Markierungen.
     * Parameter noch unklar. XML? oder graph?
     *
     * @return
     */
    public static Decoration buildFromGroundnet() {
        return null;
    }

    /**
     * Ein (Abbiege)Pfeil.
     * Per default Richtung +y.
     * In CCW, obwohl das egal sein könnte.
     * positiver angle dreht nach links
     */
    @Deprecated
    public static Decoration buildRoadArrow(double len, double basewidth, double angle) {
        Shape shape = new Shape(true);
        double len2 = len / 2, len4 = len / 4;
        double basewidth2 = basewidth / 2;

        shape.addPoint(new Vector2(-basewidth2, -len2));
        shape.addPoint(new Vector2(basewidth2, -len2));
        shape.addPoint(new Vector2(basewidth2, len4));
        shape.addPoint(new Vector2(basewidth2 + len4, len4));
        shape.addPoint(new Vector2(0, len2));
        shape.addPoint(new Vector2(-basewidth2 - len4, len4));
        shape.addPoint(new Vector2(-basewidth2, len4));
        shape = shape.rotate(angle);
        return buildPolygonFromShape(shape);

    }

    public static DecorationGeometry buildPolygonFromShape(Shape shape) {
        Polygon ls = JtsUtil.createPolygon(shape);
        if (ls == null) {
            logger.error("couldn't create polygon");
        }
        return new DecorationGeometry(ls);
    }
}
