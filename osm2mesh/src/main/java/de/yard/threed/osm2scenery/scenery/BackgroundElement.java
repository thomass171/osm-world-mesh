package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2graph.osm.VertexData;
import lombok.extern.slf4j.Slf4j;


/**
 * Created on 02.08.18.
 */
@Slf4j
public class BackgroundElement {
    //kein EleGroup. Der BG muss sich das aus den angrenzenden Objekten raussuchen
    //Macht er auch, aber zum Speichern ist die Group einfach prfaktisch.
    //public EleConnectorGroupSet eleConnectorGroupSet;
    public VertexData vertexData;
    public Polygon polygon;
    public boolean trifailed;

    public BackgroundElement(Polygon polygon) {
        this.polygon=polygon;
        if (polygon.getCoordinates().length < 4) {
            //kein warn sondern error, weil ja nichts erzeugt wird
            log.error("inconsistent? empty polygon");
           
        }
    }
}
