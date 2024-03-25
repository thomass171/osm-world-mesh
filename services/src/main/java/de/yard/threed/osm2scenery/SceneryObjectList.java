package de.yard.threed.osm2scenery;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2scenery.modules.BridgeModule;
import de.yard.threed.osm2scenery.modules.BuildingModule;
import de.yard.threed.osm2scenery.scenery.*;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.BuildingComponent;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Das mit dem merge verfolge ich - zumindest an dieser Stelle - nicht weiter.
 * Das ist jetzt einfach eine Liste von Scenery Objects.
 * Ja, aber die von den Modulen gelieferten Teillisten mal behalten? Das kann jedes Modul halten wie es will.
 *
 * <p>
 * Created on 11.07.18.
 */
public class SceneryObjectList {
    public List<SceneryObject> objects = new ArrayList<>();
    static Logger logger = Logger.getLogger(SceneryObjectList.class);

    /**
     * wenn die neue Area sich mit einer bestehenden schneidet, die beiden vereinen. Sonst nicht, das gibt fiese Verbindungen weil es dann trotzdem ein
     * Polygon wird.
     * 18.7.18: Eigentlich muesste/koennte dann nochmal geprüft werden, ob nicht auch andere dann wieder gemrged werden können. Lass ich aber erstmal.
     * 24.7.18: Gerade fuer Roads ist das eh heikel, weil damit komplexeste Polygone entstehen, was leicht zu TopologyExceptions oder TriangulateExceptions
     * führen kann. Darum aufgeteilt in einen add() und merge()
     *
     * @param area
     */
    /*Komplikationen? public void merge(SceneryArea area) {
        for (SceneryArea a : areas) {
            if (a.getPolygon().intersects(area.getPolygon())) {
                a.merge(area);
                return;
            }
        }
        areas.add(area);

    }*/
    public void add(SceneryObject/*Area*/ area) {
        objects.add(area);

    }

    public SceneryObject findObjectByOsmId(long osmid) {
        for (SceneryObject obj : objects) {
            if (obj.getOsmIds().contains(osmid)) {
                return obj;
            }
        }
        return null;
    }

    public BuildingModule.Building findBuildingByOsmId(long osmid) {
        SceneryAreaObject buildingso = (SceneryAreaObject) findObjectByOsmId(osmid);
        BuildingComponent bc = (BuildingComponent) buildingso.volumeProvider;
        BuildingModule.Building building = bc.building;
        return building;
    }

    public SceneryObject findObjectById(int objectid) {
        for (SceneryObject obj : objects) {
            if (obj.id == objectid) {
                return obj;
            }
        }
        return null;
    }

    public List<SceneryObject> findObjectsByCategory(SceneryObject.Category category) {
        List<SceneryObject> result = objects.stream().filter(o -> o.getCategory() == category).collect(Collectors.toList());
        return result;
    }

    public List<SceneryObject> findObjectsByCycle(SceneryObject.Cycle cycle) {
        List<SceneryObject> result = objects.stream().filter(o -> o.getCycle() == cycle).collect(Collectors.toList());
        return result;
    }

    public List<SceneryAreaObject> findAreasByCoordinate(Coordinate coordinate) {
        List<SceneryAreaObject> result = new ArrayList<>();
        for (SceneryObject o : objects) {
            if ((o instanceof SceneryAreaObject) && o.covers(coordinate)) {
                result.add((SceneryAreaObject) o);
            }

        }
        return result;
    }

    /**
     * 3.6.19: Ein Helper, weil es Category BRIDGE nicht mehr gibt.
     *
     * @return
     */
    public List<SceneryObject> findBridges() {
        List<SceneryObject> result = objects.stream().filter(o -> o instanceof BridgeModule.Bridge).collect(Collectors.toList());
        return result;
    }

    public List<SceneryWayObject> findWaysByCategory(SceneryObject.Category category) {
        List<SceneryWayObject> result = new ArrayList();
        for (SceneryObject so : objects) {
            if (so.getCategory() == category && so instanceof SceneryWayObject) {
                result.add((SceneryWayObject) so);
            }
        }
        return result;
    }

    public List<SceneryObject> findObjectsByCreatorTag(String tag) {
        List<SceneryObject> l = new ArrayList<>();
        for (SceneryObject obj : objects) {
            if (obj.getCreatorTag().equals(tag)) {
                l.add(obj);
            }

        }
        return l;
    }

    public AbstractArea findDecorationByName(String name) {
        List<SceneryObject> l = new ArrayList<>();
        for (SceneryObject obj : objects) {
            for (AbstractArea decoration : obj.getDecorations()) {
                if (name.equals(decoration.getName())) {
                    return decoration;
                }
            }
        }
        return null;
    }

    public int size() {
        return objects.size();
    }

    public SceneryObject get(int i) {
        return objects.get(0);
    }

    /**
     * Alle Overlaps mit anderen TerrainProvidern ermitteln.
     *
     * @param objects
     * @param fo
     * @return
     */
    public static List<SceneryFlatObject> getTerrainOverlaps( SceneryFlatObject fo,List<SceneryObject> objects) {
        return getOverlaps(objects, fo, true);
    }

    public static List<SceneryFlatObject> getOverlaps(List<SceneryObject> objects, SceneryFlatObject fo,
                                                      boolean terrainonly) {
        //23.7.19 Phase.OVERLAPS.assertCurrent();
        List<SceneryFlatObject> overlaps = new ArrayList<>();
        if (fo == null) {
            logger.error("way isType null");
            return overlaps;
        }

        for (SceneryObject obj : objects) {
            if (obj instanceof SceneryFlatObject) {
                if (!terrainonly || (obj.isTerrainProvider() && fo.isTerrainProvider())) {
                    //don't check way itself
                    if (obj != fo ) {
                        if (((SceneryFlatObject) obj).overlaps(fo)) {
                            overlaps.add((SceneryFlatObject) obj);
                        }
                    }
                }
            }
        }
        return overlaps;
    }

    public static void iterateWays(List<SceneryObject> objects, WayHandler wayHandler) {
        for (SceneryObject obj : objects) {
            if (obj instanceof SceneryWayObject) {
                wayHandler.handleWay((SceneryWayObject) obj);
            }
        }
    }

    public static void iterateWayConnectors(List<SceneryObject> objects, ConnectorHandler wayHandler) {
        for (SceneryObject obj : objects) {
            if (obj instanceof SceneryWayConnector) {
                wayHandler.handleConnector((SceneryWayConnector) obj);
            }
        }
    }

    public static void iterateSupplements(List<SceneryObject> objects, SupplementHandler wayHandler) {
        for (SceneryObject obj : objects) {
            if (obj instanceof ScenerySupplementAreaObject) {
                wayHandler.handleSupplement((ScenerySupplementAreaObject) obj);
            }
        }
    }

    public static void iterateAreas(List<SceneryObject> objects, AreaHandler areaHandler) {
        for (SceneryObject obj : objects) {
            if (obj instanceof SceneryAreaObject) {
                areaHandler.handleArea((SceneryAreaObject) obj);
            }
        }
    }

    @FunctionalInterface
    public static interface WayHandler {
        void handleWay(SceneryWayObject way);
    }

    @FunctionalInterface
    public static interface ConnectorHandler {
        void handleConnector(SceneryWayConnector connector);
    }

    @FunctionalInterface
    public static interface AreaHandler {
        void handleArea(SceneryAreaObject area);
    }

    @FunctionalInterface
    public static interface SupplementHandler {
        void handleSupplement(ScenerySupplementAreaObject area);
    }
}
