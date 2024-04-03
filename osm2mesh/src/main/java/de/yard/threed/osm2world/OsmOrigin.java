package de.yard.threed.osm2world;

import com.vividsolutions.jts.geom.Polygon;

import java.util.List;

/**
 * Information about the basic raw OSM data leading to drawing. Not needed for rendering but helpful for debugging,
 * analysis and further visualization.
 *
 * 30.7.19: So ganz passend ist das aber doch nicht, denn es werden ja auch Elemente ohne OSM Ursprung gerendered. Vielleicht
 * doch wieder die Klasse PolygonOrigin recyclen? Und die wirklich nur für OSM origin?
 * <p>
 * Created on 30.05.18.
 */
public class OsmOrigin {
    public SimplePolygonXZ outlinePolygonXZ = null;
    public MapNode mapNode = null;
    public MapArea mapArea = null;
    public MapWaySegment segment = null;
    //public MapElement mapElement = null;
    // eg. the class name of the Renderable
    //24.5.19 gehoert hier nicht hin public String creatortag = "";
    public String texturizer = "";
    public boolean wascut;
    List<Long> osmIds;
    //18.8.18: irgendwie doof hier
    public Polygon polygon;
    public boolean trifailed;
    MapWay mapway;

    /**
     * for Area
     *
     * @param creatortag
     * @param mapArea
     * @param outlinePolygonXZ
     */
    public OsmOrigin(String creatortag, MapArea mapArea, SimplePolygonXZ outlinePolygonXZ) {
        //24.5.19 gehoert hier nicht hin this.creatortag = creatortag;
        this.mapArea = mapArea;
        this.outlinePolygonXZ = outlinePolygonXZ;
    }

    public OsmOrigin(String creatortag, MapNode mapNode, SimplePolygonXZ outlinePolygonXZ) {
        //24.5.19 gehoert hier nicht hin this.creatortag = creatortag;
        this.mapNode = mapNode;
        this.outlinePolygonXZ = outlinePolygonXZ;
    }

    public OsmOrigin(String creatortag, MapWaySegment segment, SimplePolygonXZ outlinePolygonXZ) {
        //24.5.19 gehoert hier nicht hin this.creatortag = creatortag;
        this.segment = segment;
        this.outlinePolygonXZ = outlinePolygonXZ;
    }

    public OsmOrigin(String creatortag, MapWaySegment segment) {
        //24.5.19 gehoert hier nicht hin this.creatortag = creatortag;
        this.segment = segment;
        this.outlinePolygonXZ = outlinePolygonXZ;
    }

    /**
     * Ganz konsistent ist das nicht, viele IDs und ein Way, aber naja.
     *
     * @param creatortag
     * @param osmIds
     * @param mapway
     */
    public OsmOrigin(String creatortag, List<Long> osmIds, MapWay mapway) {
        //24.5.19 gehoert hier nicht hin this.creatortag = creatortag;
        this.segment = null;
        this.outlinePolygonXZ = null;
        this.osmIds = osmIds;
        this.mapway = mapway;
    }

    public OsmOrigin(String creatortag, List<Long> osmIds, MapArea mapArea) {
        //24.5.19 gehoert hier nicht hin this.creatortag = creatortag;
        this.segment = null;
        this.outlinePolygonXZ = null;
        this.osmIds = osmIds;
        this.mapArea = mapArea;
    }

    public OsmOrigin(String creatortag, List<Long> osmIds) {
        //24.5.19 gehoert hier nicht hin this.creatortag = creatortag;
        this.segment = null;
        this.outlinePolygonXZ = null;
        this.osmIds = osmIds;
    }

    public OsmOrigin(PolygonWithHolesXZ polygon) {

    }

    public OsmOrigin(String creatortag) {
        //24.5.19 gehoert hier nicht hin this.creatortag = creatortag;

    }

    @Override
    public String toString() {
        String s = "";//creatortag;
        if (mapArea != null) {
            s += "osmarea=" + mapArea.getOsmObject().id;
        }
        if (segment != null) {
            s += "osmway=" + segment.getOsmWay().id;
        }
        if (osmIds != null) {
            s += "osmids=";
            for (Long l : osmIds) {
                s += "" + l + ",";
            }
        }
        return s;
    }

    public String getInfo() {
        String info = "" + toString();
        if (mapway != null) {
            info += taginfo(mapway.getTags());
        }
        if (mapArea != null) {
            info += taginfo(mapArea.getTags());
        }
        return info;
    }

    private String taginfo(TagGroup tags) {
        String info = "(";
        for (Tag tag : tags) {
            info += tag.key + "=" + tag.value + ",";
        }
        info += ")";
        return info;
    }
}
