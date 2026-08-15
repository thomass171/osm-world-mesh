package de.yard.threed.osm2scenery.util;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Vector2Array;
import de.yard.threed.core.Vector3Array;
import de.yard.threed.osm2graph.osm.VertexData;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
public class Dumper {
    public static void dumpVertexData( VertexData vertexData){
        for (int i=0;i<vertexData.vertices.size();i++){
            log.debug(""+i+":"+ vertexData.vertices.get(i)+",uv="+vertexData.getUV(i));
        }
        for (int i=0;i<vertexData.indices.length;i++){
            log.debug(""+i+":"+ vertexData.indices[i]);
        }
    }

    public static void dumpVertexData(Logger logger, Vector3Array vertices, Vector2Array uvs, int[] indices){
        for (int i=0;i<vertices.size();i++){
            log.debug(""+i+":"+ vertices.getElement(i)+",uv="+uvs.getElement(i));
        }
        for (int i=0;i<indices.length;i++){
            log.debug(""+i+":"+ indices[i]);
        }
    }

    public static void dumpPolygon(Logger logger, Coordinate[] coord){
        for (int i=0;i<coord.length;i++){
            log.debug(""+i+":"+ coord[i]);

        }
    }
}
