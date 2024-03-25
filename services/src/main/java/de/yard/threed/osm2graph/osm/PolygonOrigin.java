package de.yard.threed.osm2graph.osm;


import de.yard.threed.osm2world.MapArea;
import de.yard.threed.osm2world.MapElement;
import de.yard.threed.osm2world.VectorXZ;

import java.util.List;

/**
 * Created by thomass on 06.02.17.
 * Wandert jetzt nach OsmOrigin.
 * 30.7.19: Aber es gibt doch Polygone ohne OSM Origin. Das ist auch doof.
 */
@Deprecated
public class PolygonOrigin {
    MapElement mapElement;
    String creatortag;

    public PolygonOrigin(MapElement mapElement) {
        this.mapElement = mapElement;
    }

    public PolygonOrigin(String creatortag) {
        this.creatortag = creatortag;
    }

    public String getId() {
        if (mapElement != null) {
            if (mapElement instanceof MapArea) {
                return "osmarea id=" + ((MapArea) mapElement).getOsmObject().id;
            }
            /*if (mapElement instanceof MyMapGraph) {
                return "osmgraph osmwayid=" + ((MyMapGraph) mapElement).getOsmWay().id;
            }*/
            return "unknown Mapelement " + mapElement;
        } else {
            return creatortag;
        }
    }

    public List<VectorXZ> getOsmPolygon() {
        if (mapElement != null) {
            if (mapElement instanceof MapArea) {
                return null;
            }
            /*if (mapElement instanceof MyMapGraph) {
                List<? extends WorldObject> repr = ((MyMapGraph) mapElement).getRepresentations();
                if (repr.size() > 0) {
                    // Der Cast muesste bei MyMapGraph immer aufgehen
                    MyAbstractNetworkWaySegmentWorldObject r = (MyAbstractNetworkWaySegmentWorldObject) repr.get(0);
                    List<VectorXZ> poly = r.getOutlineListXZ();
                    return poly;
                }
            }*/
            return null;
        } else {
            return null;
        }
    }

    
}
