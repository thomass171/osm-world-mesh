package de.yard.threed.osm2graph.osm;


import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Util;
import de.yard.threed.core.geometry.SimpleGeometry;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2world.JTSConversionUtil;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.OsmOrigin;
import de.yard.threed.osm2world.VectorXZ;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;


/**
 * 10.7.18: Hat i.d.R. einen Bezug zu einer SimpleGeometry, kann aber auch ein beliebiger Polygon sein.
 * 12.7.18: Mit newgen auch wieder Polygon, bzw. Area wegen Holes.
 * 18.8.18: Der Originalpolygon ist aber auch wertvoll. Dazu gehoert auch trifailed, darum kommt das mit in OsmOrigin. Ist aber eigentlich auch doof.
 * <p>
 * Created by thomass on 06.02.17.
 */
public class PolygonInformation {
    private Area/*Polygon*/ area;
    public String id;
    public OsmOrigin polygonOrigin;
    private Material material;
    private List<TriangleAWT> plist;
    public EleConnectorGroupSet eleconnectors;
    public VertexData vertexData;
    public String creatortag;
    //bei Ways z.B. der Start
    public List<VectorXZ> pois=new ArrayList();

    public PolygonInformation(Area area, OsmOrigin polygonOrigin, Material material) {
        this.area = area;
        if (area == null) {
            Util.notyet();
        }
        this.polygonOrigin = polygonOrigin;
        id = "";
        /*18.7.18 for (int i = 0; i < p.npoints; i++) {
            id += "" + p.xpoints[i] + "" + p.ypoints[i];
        }*/
        this.material = material;
    }

    public PolygonInformation(List<TriangleAWT> plist, SimpleGeometry geo, OsmOrigin polygonOrigin, Material material) {
        this.plist = plist;
        this.polygonOrigin = polygonOrigin;
        id = "";
        this.material = material;
        /*for (int i = 0; i < p.npoints; i++) {
            id += "" + p.xpoints[i] + "" + p.ypoints[i];
        }*/
    }

    public String getMaterialAsString() {
        if (material==null){
            return "null";
        }
        String texturename = GraphicsTarget.getTextureNameFromMaterial(material);
        String s = material.getName() + "(";
        s += (texturename != null) ? texturename : "no texture";
        s += ")";
        return s;
    }

    public String getPolygonDetails(String delimiter, boolean withVertexData) {
       /*TODO java.util.List<VectorXZ> osmpoly= polygonOrigin.getOsmPolygon();
        if (osmpoly==null){
            return "no osm polygon found";
        }
        if (osmpoly.size()!=p.npoints){
            return "inconsistent poly size";
        }*/
        String s = "";
        /*for (int i=0;i<p.npoints;i++){
            s += osmpoly.get(i).getX()+"(),"+osmpoly.get(i).getZ()+delimiter+"()";
        }*/
        if (plist != null) {
            s += "" + plist.size() + " triangles";
        }
        if (polygonOrigin != null) {
            s += ",trifailed=" + polygonOrigin.trifailed + ",texturizer=" + polygonOrigin.texturizer + ",wascut=" + polygonOrigin.wascut;
        }
        if (eleconnectors != null && eleconnectors.size() > 0) {
            if (s.length() > 0) {
                s += ", ";
            }
            s += "elevations:";
            for (EleConnectorGroup eleConnectorGroup : eleconnectors.eleconnectorgroups) {
                //for (EleConnector e:eg.eleConnectors) {
                if (!eleConnectorGroup.hasElevation()) {
                    s += "-";
                } else {
                    s += eleConnectorGroup.getElevation();
                }
                s += "(" + eleConnectorGroup.eleConnectors.size() + "x)";
                //}
                s += ",";
            }
        } else {
            s += ", no elevations";
        }
        if (withVertexData && vertexData != null) {
            s += "\n " + vertexData.vertices.size() + " vertices:\n";

            for (int i = 0; i < vertexData.vertices.size(); i++) {
                String uv = "";
                if (vertexData.uvs != null) {
                    uv = "" + vertexData.uvs.get(i);
                }
                s += vertexData.vertices.get(i) + " " + uv + "\n";
            }
        }
        return s;
    }

    public boolean contains(Point point) {
        if (area != null) {
            return area.contains(point);
        }
        for (TriangleAWT pi : plist) {
            if (pi.p.contains(point)) {
                return true;
            }
        }
        return false;
    }

    public TriangleAWT getTriangle(Point point) {
        if (plist != null) {

            for (TriangleAWT pi : plist) {
                if (pi.p.contains(point)) {
                    return pi;
                }
            }
        }
        return null;
    }

    public Area/*Polygon*/ getPolygon() {
        return area;
    }

    public String getNearVertexInfo(VectorXZ xz) {
        String s = "";
        Coordinate cref = JTSConversionUtil.vectorXZToJTSCoordinate(xz);
        if (vertexData != null) {

            for (int i = 0; i < vertexData.vertices.size(); i++) {
                Coordinate c = vertexData.vertices.get(i);
                if (c.distance(cref) < 5) {
                    s += c;
                }
            }
        }
        return s;
    }

    public Material getMaterial() {
        return material;
    }
}
