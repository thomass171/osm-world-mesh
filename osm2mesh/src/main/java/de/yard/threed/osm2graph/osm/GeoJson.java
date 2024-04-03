package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import de.yard.threed.osm2world.MapWaySegment;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.styling.SLD;


import java.awt.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 22.05.18.
 */
public class GeoJson {
    //FeatureJSON fjson = new FeatureJSON();
    GeometryJSON gjson = new GeometryJSON(8);
    List<Geometry> lines = new ArrayList<Geometry>();
    StringWriter writer = new StringWriter();
    
    private GeoJson(){
        
    }
    
    public static String export(WaySet roads, WaySet river) throws IOException {
        GeoJson geoJson = new GeoJson();
        geoJson.doexportWaySet(roads);
        geoJson.doexportWaySet(river);
        GeometryCollection gcol = new GeometryFactory().createGeometryCollection((Geometry[]) geoJson.lines.toArray(new Geometry[0]));
        geoJson.gjson.write(gcol, geoJson.writer);
        String result = geoJson.writer.getBuffer().toString();
        //nicht immer praktisch. geht auch nicht 
        //result = result.replaceAll(",\\[","[\n");
        //System.out.println(result);
        return result;
    }

    public static void exportWaySet(WaySet serviceways) throws IOException {
        GeoJson geoJson = new GeoJson();
        geoJson.doexportWaySet(serviceways);
        GeometryCollection gcol = new GeometryFactory().createGeometryCollection((Geometry[]) geoJson.lines.toArray(new Geometry[0]));
        geoJson.gjson.write(gcol, geoJson.writer);
        String result = geoJson.writer.getBuffer().toString();
        //nicht immer praktisch result = result.replaceAll(",\\[","[\n");
        System.out.println(result);
    }

    public void doexportWaySet(WaySet serviceways) throws IOException {
       
//SimpleFeature f = new SimpleFeature() {
        //Point point = new Point(1,2);
        //fjson.writeFeature(feature(1), writer);

        org.geotools.styling.Style style = SLD.createLineStyle(Color.BLUE, 1);

        for (MapWaySegment w : serviceways.ways) {
            Coordinate[] coords = new Coordinate[2/*w.nodes.size() + 1*/];
            int i = 0;
           // for (OSMNode seg : w.nodes) {
             //   if (i == 0) {
                    coords[0] = new Coordinate(w.getStartNode().getOsmNode().lon, w.getStartNode().getOsmNode().lat);
               // }
                i++;
                coords[i] = new Coordinate(w.getEndNode().getOsmNode().lon, w.getEndNode().getOsmNode().lat);

            //}
            if (i > 1) {
                LineString g = new GeometryFactory().createLineString(coords);
                
                lines.add(g);
            }
        }
        
            /*coords[0]=new Coordinate(            7.007217407226562f,                    51.16104532653234f);
          
            coords[1]=new Coordinate(            7.007120847702026f,                    51.16130773388579f          );
            coords[2]=new Coordinate(            7.007174491882323f,                    51.16144902953476f          );
            coords[3]=new Coordinate(            7.007335424423218f,                    51.1614893996407f          );*/

       
       
    }
}
