package de.yard.threed.osm2scenery;

import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2world.MapNode;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Menge aller Ways eines Tile gruppiert nach Category (Road, River, Railway) und deren Verbindungen (Junctions).
 *
 * Enthaelt auch die Map fuer Connector.
 *
 * 19.4.19: Warum global? Warum gibt es nicht pro Category eine Map im jeweiligen Modul? Wegen Bridges?
 * Oder weil die Objekte spätestens für Elevation in einen Kontextmüssen? Das ist aber dünn.
 * Aber für eine Entkopplung von den Modules ist es gut.
 *
 * Created on 24.08.18.
 */
public class WayMap {
    Logger logger = Logger.getLogger(WayMap.class);
    //MapNode->alle Ways dazu. Von einem Way werden aber nur Start/End registriert. NeeNee, sehr haufig
    //gibt es eine junction auf einer Zwischennode. Darum müssen alle rein.
    //24.8.18: die logische Trennung hatte was statt alles in einer map, evtl. ueber Category?
    public Map<SceneryObject.Category, Map<Long, List<SceneryWayObject>>> wayMap = new HashMap<>();
    //a node may only have one NodeObject
    public Map<SceneryObject.Category, Map<Long, SceneryWayConnector>> wayConnectorMap = new HashMap<>();

    public Map<Long, List<SceneryWayObject>> getMapForCategory(SceneryObject.Category category) {
        if (wayMap.get(category) == null) {
            wayMap.put(category, new HashMap<>());
        }
        return wayMap.get(category);
    }

    public Map<Long, SceneryWayConnector> getConnectorMapForCategory(SceneryObject.Category category) {
        if (wayConnectorMap.get(category) == null) {
            wayConnectorMap.put(category, new HashMap<>());
        }
        return wayConnectorMap.get(category);
    }

    public List<SceneryWayObject> get(SceneryObject.Category category, Long osmid) {
        Map<Long, List<SceneryWayObject>> map = getMapForCategory(category);
        if (map.get(osmid) == null) {
            map.put(osmid, new ArrayList<>());
        }
        return map.get(osmid);
    }

    public SceneryWayConnector getConnector(SceneryObject.Category category, Long osmid) {
        Map<Long, SceneryWayConnector> map = getConnectorMapForCategory(category);
        return map.get(osmid);
    }

    public List<SceneryWayConnector> getConnectors(SceneryObject.Category category) {
        Map<Long, SceneryWayConnector> map = getConnectorMapForCategory(category);
        return new ArrayList<>(map.values());
    }

    public void registerWayAtNode(SceneryObject.Category category, MapNode node, SceneryWayObject sceneryWayObject) {
        //Map<Long, List<SceneryWayObject>> roadmap = SceneryContext.getInstance().wayMap;
        List<SceneryWayObject> rds = get(category, node.getOsmId());
       
        rds.add(sceneryWayObject);
    }

    public void addConnector(SceneryObject.Category category, MapNode node, SceneryWayConnector sceneryNodeObject) {
        Map<Long, SceneryWayConnector> map = getConnectorMapForCategory(category);
        if (map.get(node.getOsmId()) != null) {
            logger.error("duplicate entry");
        }
        map.put(node.getOsmId(),sceneryNodeObject);
    }
}
