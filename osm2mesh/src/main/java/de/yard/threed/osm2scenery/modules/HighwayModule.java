package de.yard.threed.osm2scenery.modules;


import de.yard.threed.core.Util;
import de.yard.threed.core.Vector2;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.scenery.FixedWidthProvider;
import de.yard.threed.osm2scenery.scenery.SceneryNodeObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryObjectFactory;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2scenery.scenery.components.RoadDecorator;
import de.yard.threed.osm2scenery.util.TagFilter;
import de.yard.threed.osm2scenery.util.TagMap;
import de.yard.threed.osm2world.ConfMaterial;
import de.yard.threed.osm2world.GroundState;
import de.yard.threed.osm2world.MapArea;
import de.yard.threed.osm2world.MapBasedTagGroup;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.MapWaySegment;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.Materials;
import de.yard.threed.osm2world.NetworkAreaWorldObject;
import de.yard.threed.osm2world.OsmOrigin;
import de.yard.threed.osm2world.PolygonXYZ;
import de.yard.threed.osm2world.PolylineXZ;
import de.yard.threed.osm2world.RenderableToAllTargets;
import de.yard.threed.osm2world.ShapeXZ;
import de.yard.threed.osm2world.Tag;
import de.yard.threed.osm2world.TagGroup;
import de.yard.threed.osm2world.Target;
import de.yard.threed.osm2world.TerrainBoundaryWorldObject;
import de.yard.threed.osm2world.TexCoordFunction;
import de.yard.threed.osm2world.TexCoordUtil;
import de.yard.threed.osm2world.TextureData;
import de.yard.threed.osm2world.TriangleXYZ;
import de.yard.threed.osm2world.TunnelModule;
import de.yard.threed.osm2world.VectorXYZ;
import de.yard.threed.osm2world.VectorXYZList;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.osm2world.VisibleConnectorNodeWorldObject;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.ROAD;
import static de.yard.threed.osm2world.EmptyTagGroup.EMPTY_TAG_GROUP;
import static de.yard.threed.osm2world.Materials.*;
import static de.yard.threed.osm2world.NamedTexCoordFunction.GLOBAL_X_Z;
import static de.yard.threed.osm2world.NamedTexCoordFunction.STRIP_FIT_HEIGHT;
import static de.yard.threed.osm2world.TexCoordUtil.texCoordLists;
import static de.yard.threed.osm2world.ValueStringParser.parseOsmDecimal;
import static de.yard.threed.osm2world.VectorXYZ.addYList;
import static de.yard.threed.osm2world.WorldModuleGeometryUtil.createLineBetween;
import static de.yard.threed.osm2world.WorldModuleGeometryUtil.createTriangleStripBetween;
import static de.yard.threed.osm2world.WorldModuleParseUtil.parseWidth;
import static java.lang.Math.max;
import static java.util.Arrays.asList;


/**
 * adds roads to the world
 * Abgeleitet vom OSM2World RoadModule.java.
 * Legt auch Bridges (ueber die Roads fuehren) an.
 * <p>
 * <p>
 * Arbeitet aber auf MapWay statt auf MapWaySegemt.
 * Das ist das Module fuer alles, was in OSM den key "highway", also auch Pfade, Wege und Stufen.
 * Darum umbenannt RoadModule->HighwayModule.
 * <p>
 * 20.4.19: Auch für z.B. Parkplätze, halt alles, was zum Kontext Strassen(verkehr) gehört.
 */
public class HighwayModule extends SceneryModule {
    static Logger logger = Logger.getLogger(HighwayModule.class);

    /**
     * determines whether right-hand or left-hand traffic isType the default
     */
    private static final boolean RIGHT_HAND_TRAFFIC_BY_DEFAULT = true;
    private boolean cutConnectors = false;
    SceneryObjectList roadsAndBridges;
    //static  private Map<Long, List<Road>> roadmap = new HashMap<>();

    @Override
    public SceneryObjectList applyTo(MapData tile) {
        // In die Liste kommen vorerst auch die Filler unter der Brücke
        roadsAndBridges = new SceneryObjectList();
        /*for (MapWaySegment line : grid.getMapWaySegments()) {
            ///TODO if (insideBounds(line))
                if (isRoad(line.getTags())) {
                    Road road = new Road(line, line.getTags());
                    line.addRepresentation(road);
                    roads.add(road);
                }
        }*/

        TagFilter tagfilter = getTagFilter("tagfilter");
        TagMap materialmap = getTagMap("materialmap");
        for (MapWay mapway : tile.getMapWays()) {
            if (mapway.getOsmId() == 8610418) {
                int osmid = 6;
            }
            if (mapway.getOsmId() == 225794273) {
                //Sonderlocke wegen zweier LazyCut an selber Stelle (B55B477 junction). Verhindert SmartGrid
                continue;
            }
            ///23.4.19:hinfällig? if (insideBounds(line))
            if (isHighway(mapway.getTags()) && tagfilter.isAccepted(mapway.getTags())) {
                long osmid = mapway.getOsmId();

                //Ein Way ohne Segmente? Das ist doch bestimmt was inkonsistentes.
                if (mapway.getMapWaySegments().size() > 0) {
                    if (BridgeModule.isBridge(mapway.getTags())) {
                        //return GroundState.ABOVE;
                        //3.6.19: Bridge IST jetzt Highway, statt ihn zu enthalten.
                        BridgeModule.Bridge bridge/*Highway roadoverbridge*/ = new BridgeModule.Bridge /*Highway*/(mapway, materialmap/*, mapway.getTags(), mapway.getOsmId()*/);
                        //Nur konsequent, dass auch als Road zu registrieren. 17.8.18: Und auch in die globale Liste aufnehmen.
                        SceneryContext.getInstance().highways.put(osmid, bridge/*roadoverbridge*/);
                        //roadsAndBridges.add(bridge.roadoverbridge);

                        //BridgeModule.Bridge bridge = new BridgeModule.Bridge(roadoverbridge/*mapway/*, mapway.getTags(), mapway.getOsmId()*/);
                    /*Polygon p = bridge.getPolygon().polygon;
                    if (p == null) {
                        logger.warn("no/broken polygon for osm way " + mapway.getOsmId());
                    }*/
                        roadsAndBridges.add(bridge);
                        SceneryContext.getInstance().bridges.put(osmid, bridge);
                        /*roadover*/
                        bridge.addToWayMap(ROAD);

                        /*roadsAndBridges.add(bridge.gap);
                        if (bridge.ramp0 != null) {
                            roadsAndBridges.add(bridge.ramp0);
                            roadsAndBridges.add(bridge.ramp1);
                            roadsAndBridges.add(bridge.ramp2);
                            roadsAndBridges.add(bridge.ramp3);
                        }*/

                    } else if (TunnelModule.isTunnel(mapway.getTags())) {
                        //erstmal wie ein Road behandeln, einfach um Lücken zu vermeioden, z.B. Luxemburger Str.
                        Highway road = new Highway(mapway, materialmap/*, mapway.getTags(), mapway.getOsmId()*/);
                        SceneryContext.getInstance().highways.put(osmid, road);
                        roadsAndBridges.add(road);
                        road.addToWayMap(ROAD);
                    } else {
                        // normaler Highway
                        Highway road = new Highway(mapway, materialmap/*, mapway.getTags(), mapway.getOsmId()*/);
                        SceneryContext.getInstance().highways.put(osmid, road);

                        //SceneryArea area = new SceneryArea("Road", p, ASPHALT, mapway.getOsmId());
                        roadsAndBridges.add(road/*area*/);
                        road.addToWayMap(ROAD);
                    }
                } else {
                    logger.warn("Ignoring mapyway " + mapway.getOsmId() + " with only " + mapway.getMapWaySegments().size() + " segments");
                }
            }
        }

        // jetzt sind alle Roads des Tile angelegt.

        for (MapArea area : tile.getMapAreas()) {
            if (isHighway(area.getTags())) {
                //4.4.19:Wann gibt es dnn sowas?
                //area.addRepresentation(new RoadArea(area));
            }
        }

        // Junctions/RoadConnector erstellen. Aber warum sollte man dafuer ALLE Nodes durchgehen?
        //for (MapNode node : grid.getMapNodes()) {
        //19.4.19: Connector/Junction sind normale SceneryObjects, die vielleich auch eine Darstellung haben
        for (List<SceneryWayObject> roads : SceneryContext.getInstance().wayMap.getMapForCategory(ROAD).values()) {
            for (SceneryWayObject road : roads) {
                if (road.getOsmIdsAsString().contains("23696494")) {
                    int h = 9;
                }
                SceneryWayConnector connector = processConnectionCandidate(road.mapWay.getStartNode(), roadsAndBridges);
                if (connector != null) {
                    connector.add(road);
                    road.setStartConnector(connector);
                }
                connector = processConnectionCandidate(road.mapWay.getEndNode(), roadsAndBridges);
                if (connector != null) {
                    connector.add(road);
                    road.setEndConnector(connector);
                }
            }
        }

        //30.7.18 hier zu frueh, weils noch keine Eles gibt. Und es gibts auch fuer Rails und River
        //die Eles muss es aber doch schon geben?
        //16.8.18 buildBridgeApproaches(roadsAndBridges);
        return roadsAndBridges;
    }

    @Override
    public SceneryObjectList applyTo(MapWay mapway, TerrainMesh terrainMesh) {
        // Also contains Filler unter der Brücke
        roadsAndBridges = new SceneryObjectList();

        // material not needed yet?
        TagMap materialmap = null;//getTagMap("materialmap");
        if (mapway.getOsmId() == 8610418) {
            int h = 6;
        }
        // just to be sure
        if (isHighway(mapway.getTags())) {
            long osmid = mapway.getOsmId();

            //Ein Way ohne Segmente? Das ist doch bestimmt was inkonsistentes.
            if (mapway.getMapWaySegments().size() > 0) {
                if (BridgeModule.isBridge(mapway.getTags())) {
                    //3.6.19: Bridge IST jetzt Highway, statt ihn zu enthalten.
                    BridgeModule.Bridge bridge = new BridgeModule.Bridge /*Highway*/(mapway, materialmap/*, mapway.getTags(), mapway.getOsmId()*/);
                    //Nur konsequent, dass auch als Road zu registrieren. 17.8.18: Und auch in die globale Liste aufnehmen.
                    SceneryContext.getInstance().highways.put(osmid, bridge);
                    roadsAndBridges.add(bridge);
                    SceneryContext.getInstance().bridges.put(osmid, bridge);
                    bridge.addToWayMap(ROAD);
                } else if (TunnelModule.isTunnel(mapway.getTags())) {
                    //erstmal wie ein Road behandeln, einfach um Lücken zu vermeioden, z.B. Luxemburger Str.
                    Highway road = new Highway(mapway, materialmap);
                    SceneryContext.getInstance().highways.put(osmid, road);
                    roadsAndBridges.add(road);
                    road.addToWayMap(ROAD);
                } else {
                    // regular highway
                    Highway road = new Highway(mapway, materialmap);
                    SceneryContext.getInstance().highways.put(osmid, road);
                    roadsAndBridges.add(road);
                    road.addToWayMap(ROAD);
                }
            } else {
                logger.warn("Ignoring mapyway " + mapway.getOsmId() + " with only " + mapway.getMapWaySegments().size() + " segments");
            }
        }

        // Junctions/RoadConnector erstellen.
        //19.4.19: Connector/Junction sind normale SceneryObjects, die vielleich auch eine Darstellung haben
        //29.3.24: Are now taken from terrain mesh.
        //for (List<SceneryWayObject> roads : SceneryContext.getInstance().wayMap.getMapForCategory(ROAD).values()) {
        for (List<SceneryWayObject> roads : terrainMesh.wayMap.getMapForCategory(ROAD).values()) {
            for (SceneryWayObject road : roads) {
                if (road.getOsmIdsAsString().contains("23696494")) {
                    int h = 9;
                }
                SceneryWayConnector connector = processConnectionCandidate(road.mapWay.getStartNode(), roadsAndBridges);
                if (connector != null) {
                    connector.add(road);
                    road.setStartConnector(connector);
                }
                connector = processConnectionCandidate(road.mapWay.getEndNode(), roadsAndBridges);
                if (connector != null) {
                    connector.add(road);
                    road.setEndConnector(connector);
                }
            }
        }
        //Elevation and BridgeApproaches comes later
        return roadsAndBridges;
    }

    @Override
    public void classify(MapData mapData) {
        List<SceneryWayConnector> connectors = SceneryContext.getInstance().wayMap.getConnectors(ROAD);
        for (SceneryWayConnector connector : connectors) {
            connector.classify();
        }
    }

    @Override
    public List<ScenerySupplementAreaObject> createSupplements(List<SceneryObject> objects) {
        List<ScenerySupplementAreaObject> supplements = new ArrayList<>();
        for (BridgeModule.Bridge bridge : SceneryContext.getInstance().bridges.values()) {
            bridge.createSupplements();
            supplements.add(bridge.gap);
            if (bridge.startHead.ramp0 != null) {
                supplements.add(bridge.startHead.ramp0);
                supplements.add(bridge.startHead.ramp1);
                supplements.add(bridge.endHead.ramp0);
                supplements.add(bridge.endHead.ramp1);
            }
        }
        return supplements;
    }

    /**
     * Fuer eine MapNode eine Junction oder RoadConnector erstellen, oder eine schon bestehende liefern.
     * Liefert null, wenn es an der Node weder Junction noch Connector gibt (bei nur zwei Ways).
     * <p>
     * 14.6.19: RoadConnector und Junction vereint in abstrakten WayConnector, der auch eine Flaeche hat.
     *
     * @param node
     * @param roadsAndBridges
     * @return
     */
    private SceneryWayConnector processConnectionCandidate(MapNode node, SceneryObjectList roadsAndBridges) {
        TagGroup tags = node.getOsmNode().tags;

        //10.7.19: Don't create connector for outside node
        if (node.location == MapNode.Location.OUTSIDEGRID) {
            return null;
        }
        SceneryWayConnector connector = SceneryContext.getInstance().wayMap.getConnector(ROAD, node.getOsmId());
        if (connector != null) {
            return connector;
        }
        List<SceneryWayObject> connectedRoads = getConnectedWays(node, false);
        if (connectedRoads.size() == 0) {
            // no way? strange.
            return null;
        }
        if (connectedRoads.size() == 1) {
            // mid way node or unconnected enter node->no connector
            return null;
        }
         /*?? else if (connectedRoads.size() == 2
                && tags.contains("highway", "crossing")
                && !tags.contains("crossing", "no")) {

            node.addRepresentation(new RoadCrossingAtConnector(node));

        }*/
        //if (connectedRoads.size() == 2) {
        Highway road1 = (Highway) connectedRoads.get(0);
        Highway road2 = (Highway) connectedRoads.get(1);

            /*if (road1.getWidth() != road2.getWidth()
                        /* TODO: || lane layouts not identical * /) {
                node.addRepresentation(new RoadConnector(node));
            }*/

        //if (road1.isOuterNode(node) && road2.isOuterNode(node)) {
        connector = new SceneryWayConnector("RoadConnector", node, ASPHALT, ROAD);
        //10.9.19: Besser alle Ways an den Connector haengen stat nur zwei, sonst geht mir nachher evtl. eine inner connector way durch. (z.B. 1379039502)
        for (SceneryWayObject way : connectedRoads) {
            //   connector.add(road1);
            // connector.add(road2);
            connector.add(way);
        }
        SceneryContext.getInstance().wayMap.addConnector(ROAD, node, connector);
        roadsAndBridges.add(connector);
        return connector;
        // }
        // }
        // connectedRoads.size() > 2

        //node.addRepresentation(new RoadJunction(node));
      /*  connector = new RoadJunction(node);

        SceneryContext.getInstance().wayMap.add(ROAD, node, connector);
        roadsAndBridges.add(connector);
        /*23.5.19 marking kein SO mehr if (((RoadJunction) connector).marking != null) {
            roadsAndBridges.add(((RoadJunction) connector).marking);
        }* /
        return connector;*/
    }

    /**
     * Die Way Enden zu Bridges erhöhen.
     * 30.8.18: AuchLuecke unter Brücke schliessen um Holes im Backgroudn zu vermeiden. Erstmal nur so.
     * Aber nicht als "Raod", weil das dann auch ein Graph wird.
     * Nicht mehr hier wegen Coupling.
     */
    public static void buildBridgeApproaches(/*SceneryObjectList*/List<SceneryObject> roadsAndBridges) {
        for (BridgeModule.Bridge bridge : SceneryContext.getInstance().bridges.values()) {
            List<SceneryWayObject> sroads = getConnectedWays(bridge./*roadorrailway.*/mapWay.getStartNode(), true);
            sroads.remove(bridge/*.roadorrailway*/);
            if (checkRoadsAtBridge(sroads)) {
                continue;
            }
            raiseBridgeApproach(sroads, bridge/*.roadorrailway*/.mapWay.getStartNode());
            List<SceneryWayObject> eroads = getConnectedWays(bridge/*.roadorrailway*/.mapWay.getEndNode(), true);
            eroads.remove(bridge/*.roadorrailway*/);
            if (checkRoadsAtBridge(eroads)) {
                continue;
            }
            raiseBridgeApproach(eroads, bridge/*.roadorrailway*/.mapWay.getEndNode());
            //vorerst kommen die da mal mit rein.
            bridge.sroad = sroads.get(0);
            bridge.eroad = eroads.get(0);
            //roadsAndBridges.add(bridge.closeBridgeGap(sroads.get(0), eroads.get(0)));

        }
    }


    private static void raiseBridgeApproach(List<SceneryWayObject> connectedroads, MapNode bridgenode) {
        for (SceneryWayObject road : connectedroads) {
            road.raiseBridgeApproach(bridgenode);
        }
    }

    private static boolean checkRoadsAtBridge(List<SceneryWayObject> roadsAtStart) {
        if (roadsAtStart.size() == 1) {
            //return roadsAtStart.get(0);
        }
        if (roadsAtStart.size() == 0) {
            logger.warn("No bridge connector found. Brigde will be corrupted");
            return true;
        }
        return false;
    }

    public List<Highway> getRoads() {
        return new ArrayList(SceneryContext.getInstance().highways.values());
    }

    /**
     * Die Bridge einer Road gilt auch als Road
     * 17.4.19: Road->Highway
     *
     * @param tags
     * @return
     */
    private static boolean isHighway(TagGroup tags) {
        if (tags.containsKey("highway")
                && !tags.contains("highway", "construction")
                && !tags.contains("highway", "proposed")) {
            return true;
        } else {
            //17.4.19: was soll das?
            return tags.contains("railway", "platform")
                    || tags.contains("leisure", "track");
        }
    }

    private static boolean isSteps(TagGroup tags) {
        return tags.contains(new Tag("highway", "steps"));
    }

    private static boolean isPath(TagGroup tags) {
        String highwayValue = tags.getValue("highway");
        return "path".equals(highwayValue)
                || "footway".equals(highwayValue)
                || "cycleway".equals(highwayValue)
                || "bridleway".equals(highwayValue)
                || "steps".equals(highwayValue);
    }

    private static boolean isOneway(TagGroup tags) {
        return tags.contains("oneway", "yes")
                || (!tags.contains("oneway", "no")
                && (tags.contains("highway", "motorway")
                || (tags.contains("highway", "motorway_link"))));
    }

    private static int getDefaultLanes(TagGroup tags) {
        String highwayValue = tags.getValue("highway");
        if (highwayValue == null
                || isPath(tags)
                || highwayValue.endsWith("_link")
                || "service".equals(highwayValue)
                || "track".equals(highwayValue)
                || "residential".equals(highwayValue)
                || "living_street".equals(highwayValue)
                || "pedestrian".equals(highwayValue)
                || "platform".equals(highwayValue)) {
            return 1;
        } else if ("motorway".equals(highwayValue)) {
            return 2;
        } else {
            return isOneway(tags) ? 1 : 2;
        }
    }

    /**
     * Ein kurzer Verbindungsweg.
     * Z.B. die Rechtsabbieger short cut Spuren in manchen Kreuzungen. Auch Motorway Auffahrten.
     * Das ist noch nicht final.
     * Links gibt es auch "gross". Daher zusaetzlich auf one way pruefen. Das wird aber auch nicht reichen.
     *
     * @param tags
     * @return
     */
    public static boolean isMinorLink(TagGroup tags) {

        return isLink(tags) && isOneway(tags);
    }

    /**
     * Ein kurzer Verbindungsweg. Nicht zwingend one way (see {link HighwayModule.isMinorLink()}.
     * Z.B. die Rechtsabbieger short cut Spuren in manchen Kreuzungen.
     * Das ist noch nicht final. Da fehlen noch andere.
     *
     * @param tags
     * @return
     */
    public static boolean isLink(TagGroup tags) {
        if (tags.contains("highway", "primary_link")) {
            return true;
        }
        if (tags.contains("highway", "motorway_link")) {
            return true;
        }
        return false;
    }

    /**
     * determines surface for a junction or connector/crossing.
     * If the node has an explicit surface tag, this isType evaluated.
     * Otherwise, the result depends on the surface values of adjacent roads.
     */
    private static Material getSurfaceForNode(MapNode node) {

        Material surface = getSurfaceMaterial(
                node.getTags().getValue("surface"), null);

        if (surface == null) {

            /* choose the surface of any adjacent road */

            for (MapWaySegment segment : node.getConnectedWaySegments()) {

                if (segment.getPrimaryRepresentation() instanceof Highway) {
                    Highway road = (Highway) segment.getPrimaryRepresentation();
                    surface = road.getSurface();
                    break;
                }

            }

        }

        return surface;

    }

    /**
     * Das Material des Highway ermitteln. ASPHALT ist eigentlich immer der Default.
     *
     * @param tags
     * @param defaultSurface
     * @return
     */
    private static Material getSurfaceForHighway(TagGroup tags,
                                                 Material defaultSurface) {

        Material result;

        if (tags.containsKey("tracktype")) {
            if (tags.contains("tracktype", "grade1")) {
                result = ASPHALT;
            } else if (tags.contains("tracktype", "grade2")) {
                result = GRAVEL;
            } else {
                result = EARTH;
            }
        } else {
            result = defaultSurface;
        }
        // Jetzt ist das Material anhand des highway key klar. Aber es könnte noch explizit bzw. genauer
        // mit dem key surface spezifiziert sein. Dann das nehmen.
        return getSurfaceMaterial(tags.getValue("surface"), result);

    }

    private static Material getSurfaceMiddleForRoad(TagGroup tags,
                                                    Material defaultSurface) {

        Material result;

        if (tags.contains("tracktype", "grade4")
                || tags.contains("tracktype", "grade5")) {
            result = TERRAIN_DEFAULT;
            // ideally, this would be the terrain type surrounds the track...
        } else {
            result = defaultSurface;
        }

        result = getSurfaceMaterial(tags.getValue("surface:middle"), result);

        if (result == GRASS) {
            result = TERRAIN_DEFAULT;
        }

        return result;

    }

    /**
     * returns ALL roads connected to a node
     *
     * @param requireLanes only include roads that are not paths and have lanes
     */
    public static List<SceneryWayObject> getConnectedWays(MapNode node,
                                                          boolean requireLanes) {


        List<SceneryWayObject> connectedRoadsWithLanes = new ArrayList<>();

        /*for (MapWaySegment segment : node.getConnectedWaySegments()) {

            /*25.7.18: Wird doch immer eine Road sein. Geht eh nicht ueber die Representation
            //if (segment.getPrimaryRepresentation() instanceof Road) {
                Road road = (Road) segment.getPrimaryRepresentation();
                if (!requireLanes ||
                        (road.getLaneLayout() != null && !isPath(road.tags))) {
                    connectedRoadsWithLanes.add(road);
                }
            }

        }*/
        if (SceneryContext.getInstance().wayMap.get(ROAD, node.getOsmId()) != null) {
            for (SceneryWayObject way : SceneryContext.getInstance().wayMap.get(ROAD, node.getOsmId())) {
                connectedRoadsWithLanes.add((Highway) way);
            }
        }
        return connectedRoadsWithLanes;

    }

    /**
     * find matching lane pairs
     * (lanes that can be connected at a junction or connector)
     */
    private static Map<Integer, Integer> findMatchingLanes(
            List<Lane> lanes1, List<Lane> lanes2,
            boolean isJunction, boolean isCrossing) {

        Map<Integer, Integer> matches = new HashMap<Integer, Integer>();

        /*
         * iterate from inside to outside
         * (only for connectors, where it will lead to desirable connections
         * between straight motorcar lanes e.g. at highway exits)
         */

        if (!isJunction) {

            for (int laneI = 0; laneI < lanes1.size()
                    && laneI < lanes2.size(); ++laneI) {

                final Lane lane1 = lanes1.get(laneI);
                final Lane lane2 = lanes2.get(laneI);

                if (isCrossing && !lane1.type.isConnectableAtCrossings) {
                    continue;
                } else if (isJunction && !lane1.type.isConnectableAtJunctions) {
                    continue;
                }

                if (lane2.type.equals(lane1.type)) {

                    matches.put(laneI, laneI);

                }

            }

        }

        /* iterate from outside to inside.
         * Mostly intended to gather sidewalks and other non-car lanes. */

        for (int laneI = 0; laneI < lanes1.size()
                && laneI < lanes2.size(); ++laneI) {

            int lane1Index = lanes1.size() - 1 - laneI;
            int lane2Index = lanes2.size() - 1 - laneI;

            final Lane lane1 = lanes1.get(lane1Index);
            final Lane lane2 = lanes2.get(lane2Index);

            if (isCrossing && !lane1.type.isConnectableAtCrossings) {
                continue;
            } else if (isJunction && !lane1.type.isConnectableAtJunctions) {
                continue;
            }

            if (matches.containsKey(lane1Index)
                    || matches.containsKey(lane2Index)) {
                continue;
            }

            if (lane2.type.equals(lane1.type)) {
                matches.put(lane1Index, lane2Index);
            }

        }

        return matches;

    }

    /**
     * determines connected lanes at a junction, crossing or connector
     */
    private static List<LaneConnection> buildLaneConnections(
            MapNode node, boolean isJunction, boolean isCrossing) {

        Util.notyet();
        List<Highway> roads = null;//getConnectedRoads(node, true);

        /* check whether the oneway special case applies */

        if (isJunction) {

            /*boolean allOneway = true;
            int firstInboundIndex = -1;

            for (int i = 0; i < roads.size(); i++) {

                Road road = roads.get(i);

                if (!isOneway(road.tags)) {
                    allOneway = false;
                    break;
                }

                if (firstInboundIndex == -1 && road.segment.getEndNode() == node) {
                    firstInboundIndex = i;
                }

            }

            if (firstInboundIndex != -1) {

                // sort into inbound and outbound oneways
                // (need to be sequential blocks in the road list)

                List<Road> inboundOnewayRoads = new ArrayList<Road>();
                List<Road> outboundOnewayRoads = new ArrayList<Road>();

                int i = 0;

                for (i = firstInboundIndex; i < roads.size(); i++) {

                    if (roads.get(i).segment.getEndNode() != node) {
                        break; //not inbound
                    }

                    inboundOnewayRoads.add(roads.get(i));

                }

                reverse(inboundOnewayRoads);

                for (/* continue previous loop * /;
                                                 i % roads.size() != firstInboundIndex; i++) {

                    outboundOnewayRoads.add(roads.get(i % roads.size()));

                }

                if (allOneway) {
                    return buildLaneConnections_allOneway(node,
                            inboundOnewayRoads, outboundOnewayRoads);
                }

            }
*/
        }

        /* apply normal treatment (not oneway-specific) */

        List<LaneConnection> result = new ArrayList<LaneConnection>();

        for (int i = 0; i < roads.size(); i++) {

            final Highway road1 = roads.get(i);
            final Highway road2 = roads.get(
                    (i + 1) % roads.size());

            addLaneConnectionsForRoadPair(result,
                    node, road1, road2,
                    isJunction, isCrossing);

        }

        return result;

    }

    /**
     * builds lane connections at a junction of just oneway roads.
     * Intended to handle motorway merges and splits well.
     * Inbound and outbound roads must not be mixed,
     * but build two separate continuous blocks instead.
     *
     * @param inboundOnewayRoadsLTR  inbound roads, left to right
     * @param outboundOnewayRoadsLTR outbound roads, left to right
     */
    private static List<LaneConnection> buildLaneConnections_allOneway(
            MapNode node, List<Highway> inboundOnewayRoadsLTR,
            List<Highway> outboundOnewayRoadsLTR) {

        List<Lane> inboundLanes = new ArrayList<Lane>();
        List<Lane> outboundLanes = new ArrayList<Lane>();

        for (Highway road : inboundOnewayRoadsLTR) {
            inboundLanes.addAll(road.getLaneLayout().getLanesLeftToRight());
        }
        for (Highway road : outboundOnewayRoadsLTR) {
            outboundLanes.addAll(road.getLaneLayout().getLanesLeftToRight());
        }

        Map<Integer, Integer> matches = findMatchingLanes(inboundLanes,
                outboundLanes, false, false);

        /* build connections */

        List<LaneConnection> result = new ArrayList<LaneConnection>();

        for (int lane1Index : matches.keySet()) {

            final Lane lane1 = inboundLanes.get(lane1Index);
            final Lane lane2 = outboundLanes.get(matches.get(lane1Index));

            result.add(buildLaneConnection(lane1, lane2,
                    RoadPart.LEFT, //TODO: road part isType not always the same
                    false, true));

        }

        return result;

    }

    /**
     * determines connected lanes at a junction, crossing or connector
     * for a pair of two of the junction's roads.
     * Only connections between the left part of road1 with the right part of
     * road2 will be taken into account.
     */
    private static void addLaneConnectionsForRoadPair(
            List<LaneConnection> result,
            MapNode node, Highway road1, Highway road2,
            boolean isJunction, boolean isCrossing) {

        /* get some basic info about the roads */

        final boolean isRoad1Inbound = false;//road1.segment.getEndNode() == node;
        final boolean isRoad2Inbound = false;//road2.segment.getEndNode() == node;
        Util.notyet();

        final List<Lane> lanes1, lanes2;

        lanes1 = road1.getLaneLayout().getLanes(
                isRoad1Inbound ? RoadPart.LEFT : RoadPart.RIGHT);

        lanes2 = road2.getLaneLayout().getLanes(
                isRoad2Inbound ? RoadPart.RIGHT : RoadPart.LEFT);

        /* determine which lanes are connected */

        Map<Integer, Integer> matches =
                findMatchingLanes(lanes1, lanes2, isJunction, isCrossing);

        /* build the lane connections */

        for (int lane1Index : matches.keySet()) {

            final Lane lane1 = lanes1.get(lane1Index);
            final Lane lane2 = lanes2.get(matches.get(lane1Index));

            result.add(buildLaneConnection(lane1, lane2, RoadPart.LEFT,
                    !isRoad1Inbound, !isRoad2Inbound));

        }

        //TODO: connect "disappearing" lanes to a point on the other side
        //      or draw caps (only for connectors)

    }

    private static LaneConnection buildLaneConnection(
            Lane lane1, Lane lane2, RoadPart roadPart,
            boolean atLane1Start, boolean atLane2Start) {

        List<VectorXYZ> leftLaneBorder = new ArrayList<VectorXYZ>();
        leftLaneBorder.add(lane1.getBorderNode(
                atLane1Start, atLane1Start));
        leftLaneBorder.add(lane2.getBorderNode(
                atLane2Start, !atLane2Start));

        List<VectorXYZ> rightLaneBorder = new ArrayList<VectorXYZ>();
        rightLaneBorder.add(lane1.getBorderNode(
                atLane1Start, !atLane1Start));
        rightLaneBorder.add(lane2.getBorderNode(
                atLane2Start, atLane2Start));

        return new LaneConnection(lane1.type, RoadPart.LEFT,
                lane1.road.rightHandTraffic,
                leftLaneBorder, rightLaneBorder);

    }

    /**
     * representation for junctions between roads.
     */
    public static class RoadJunction extends SceneryNodeObject {


        public RoadJunction(MapNode node) {
            super("RoadJunction", node, ASPHALT, ROAD);
            if (node.getOsmId() == 54286220 /*&& false*/) {
                //nur mal so ne Test Decoration.23.5.19  Wird eh nicht mehr gerendered, weil Juniton keine area hat.
                //marking = new SceneryDecoration("Marking", GRASS);
                Area marking = SceneryObjectFactory.createDecoration(new RoadDecorator());
                addDecoration(marking);
            }
        }


        /*@Override
        public GroundState getGroundState() {
            GroundState currentGroundState = null;
            checkEachLine:
            {
                for (MapWaySegment line : this.node.getConnectedWaySegments()) {
                    if (line.getPrimaryRepresentation() == null) continue;
                    GroundState lineGroundState = line.getPrimaryRepresentation().getGroundState();
                    if (currentGroundState == null) {
                        currentGroundState = lineGroundState;
                    } else if (currentGroundState != lineGroundState) {
                        currentGroundState = GroundState.ON;
                        break checkEachLine;
                    }
                }
            }
            return currentGroundState;
        }*/

    }

    /* TODO: crossings at junctions - when there isType, e.g., a footway connecting to the road!
     * (ideally, this would be implemented using more flexibly configurable
     * junctions which can have "preferred" segments that influence
     * the junction shape more/exclusively)
     */


    /**
     * representation for crossings (zebra crossing etc.) on roads
     */
    public static class RoadCrossingAtConnector
            extends VisibleConnectorNodeWorldObject
            implements RenderableToAllTargets, TerrainBoundaryWorldObject {

        private static final float CROSSING_WIDTH = 3f;

        public RoadCrossingAtConnector(MapNode node) {
            super(node);
        }

        @Override
        public float getLength() {
            return parseWidth(node.getTags(), CROSSING_WIDTH);
        }

        @Override
        public void renderTo(Target<?> target) {

            VectorXYZ startLeft = getEleConnectors().getPosXYZ(
                    startPos.subtract(cutVector.mult(0.5 * startWidth)));
            VectorXYZ startRight = getEleConnectors().getPosXYZ(
                    startPos.add(cutVector.mult(0.5 * startWidth)));

            VectorXYZ endLeft = getEleConnectors().getPosXYZ(
                    endPos.subtract(cutVector.mult(0.5 * endWidth)));
            VectorXYZ endRight = getEleConnectors().getPosXYZ(
                    endPos.add(cutVector.mult(0.5 * endWidth)));

            /* determine surface material */

            Material surface = getSurfaceForNode(node);

            if (node.getTags().contains("crossing", "zebra")
                    || node.getTags().contains("crossing_ref", "zebra")) {

                surface = surface.withAddedLayers(
                        ROAD_MARKING_ZEBRA.getTextureDataList());

            } else if (!node.getTags().contains("crossing", "unmarked")) {

                surface = surface.withAddedLayers(
                        ROAD_MARKING_CROSSING.getTextureDataList());

            }

            /* draw crossing */

            VectorXYZList vs = new VectorXYZList(asList(endLeft, startLeft, endRight, startRight));

            target.drawTriangleStrip(surface, vs,
                    texCoordLists(vs.vs, surface, GLOBAL_X_Z), null);

            /* draw lane connections */

            List<LaneConnection> connections = buildLaneConnections(
                    node, false, true);

            for (LaneConnection connection : connections) {
                connection.renderTo(target);
            }

        }

        @Override
        public GroundState getGroundState() {
            GroundState currentGroundState = null;
            checkEachLine:
            {
                for (MapWaySegment line : this.node.getConnectedWaySegments()) {
                    if (line.getPrimaryRepresentation() == null) continue;
                    GroundState lineGroundState = line.getPrimaryRepresentation().getGroundState();
                    if (currentGroundState == null) {
                        currentGroundState = lineGroundState;
                    } else if (currentGroundState != lineGroundState) {
                        currentGroundState = GroundState.ON;
                        break checkEachLine;
                    }
                }
            }
            return currentGroundState;
        }

    }

    /**
     * representation of a road
     * Das ist für alle Ways, die in OSM den key "highway", also auch Pfade, Wege und Stufen.
     * Darum umbenannt Road->Highway.
     */
    public static class Highway extends SceneryWayObject
            /*extends AbstractNetworkWaySegmentWorldObject
            implements RenderableToAllTargets, TerrainBoundaryWorldObject*/ {

        protected static final float DEFAULT_LANE_WIDTH = 3.5f;

        protected static final float DEFAULT_ROAD_CLEARING = 5;
        protected static final float DEFAULT_PATH_CLEARING = 2;

        protected static final ShapeXZ HANDRAIL_SHAPE = new PolylineXZ(
                new VectorXZ(+0.02, -0.05), new VectorXZ(+0.02, 0),
                new VectorXZ(-0.02, 0), new VectorXZ(-0.02, -0.05));

        public final boolean rightHandTraffic;

        public final LaneLayout laneLayout;
        public final float width;

        final private TagGroup tags;
        final public VectorXZ startCoord, endCoord;

        final private boolean steps;
        private AbstractArea marking;

        public Highway(MapWay/*Segment*/ line/*, TagGroup tags/*, long osmid*/, TagMap materialmap) {

            //super(line);
            super("Road", line, Highway.getMaterialForHighway(line.getTags(), materialmap, ASPHALT)/*;Materials.ROAD/*ASPHALT*/, ROAD, null);//mapWay = line;
            logger = Logger.getLogger(Highway.class.getName());


            this.tags = line.getTags();//tags;
            this.startCoord = line.getStartNode().getPos();
            this.endCoord = line.getEndNode().getPos();

            if (RIGHT_HAND_TRAFFIC_BY_DEFAULT) {
                if (tags.contains("driving_side", "left")) {
                    rightHandTraffic = false;
                } else {
                    rightHandTraffic = true;
                }
            } else {
                if (tags.contains("driving_side", "right")) {
                    rightHandTraffic = true;
                } else {
                    rightHandTraffic = false;
                }
            }

            this.steps = isSteps(tags);

            if (steps) {
                this.laneLayout = null;
                this.width = parseWidth(tags, 1.0f);
            } else {
                this.laneLayout = buildBasicLaneLayout();
                this.width = calculateWidth();
                laneLayout.setCalculatedValues(width);
            }

            //16.8.18: jetzt von aussen super.createPolygon(getWidth());

            super.setWidthProvider(new FixedWidthProvider(width));
        }

        private static Material getMaterialForHighway(TagGroup tags, TagMap materialmap, ConfMaterial defaultmaterial) {
            if (materialmap == null) {
                return defaultmaterial;
            }
            String value = materialmap.getValue(tags);
            if (value != null) {
                value = value.toLowerCase();
                Material material = Materials.getSurfaceMaterial(value);
                if (material == null) {
                    slogger.error("Material not found by name:" + value);
                } else {
                    return material;
                }
            }
            return defaultmaterial;
        }

        /**
         * creates a lane layout from several basic tags.
         */
        private LaneLayout buildBasicLaneLayout() {

            boolean isOneway = isOneway(tags);

            // determine which special lanes and attributes exist

            String divider = tags.getValue("divider");
            String sidewalk = tags.containsKey("sidewalk") ? tags.getValue("sidewalk") : tags.getValue("footway");

            boolean leftSidewalk = "left".equals(sidewalk) || "both".equals(sidewalk);
            boolean rightSidewalk = "right".equals(sidewalk) || "both".equals(sidewalk);
            boolean leftCycleway = tags.contains("cycleway:left", "lane") || tags.contains("cycleway", "lane");
            boolean rightCycleway = tags.contains("cycleway:right", "lane") || tags.contains("cycleway", "lane");
            boolean leftBusBay = tags.contains("bus_bay", "left") || tags.contains("bus_bay", "both");
            boolean rightBusBay = tags.contains("bus_bay", "right") || tags.contains("bus_bay", "both");

            // get individual values for each lane

            TagGroup[] laneTagsRight = getPerLaneTags(RoadPart.RIGHT);
            TagGroup[] laneTagsLeft = getPerLaneTags(RoadPart.LEFT);

            // determine the number of lanes

            Float lanes = null;

            if (tags.containsKey("lanes")) {
                lanes = parseOsmDecimal(tags.getValue("lanes"), false);
            }

            Float lanesRight = null;
            Float lanesLeft = null;

            //TODO handle oneway case

            String rightKey = rightHandTraffic ? "lanes:forward" : "lanes:backward";

            if (laneTagsRight != null) {
                lanesRight = (float) laneTagsRight.length;
            } else if (tags.containsKey(rightKey)) {
                lanesRight = parseOsmDecimal(tags.getValue(rightKey), false);
            }

            String leftKey = rightHandTraffic ? "lanes:backward" : "lanes:forward";

            if (laneTagsLeft != null) {
                lanesLeft = (float) laneTagsLeft.length;
            } else if (tags.containsKey(leftKey)) {
                lanesLeft = parseOsmDecimal(tags.getValue(leftKey), false);
            }

            int vehicleLaneCount;
            int vehicleLaneCountRight;
            int vehicleLaneCountLeft;

            if (lanesRight != null && lanesLeft != null) {

                vehicleLaneCountRight = (int) (float) lanesRight;
                vehicleLaneCountLeft = (int) (float) lanesLeft;

                vehicleLaneCount = vehicleLaneCountRight + vehicleLaneCountLeft;

                //TODO incorrect in case of center lanes

            } else {
                if (lanes == null) {
                    vehicleLaneCount = getDefaultLanes(tags);
                } else {
                    vehicleLaneCount = (int) (float) lanes;
                }
                if (lanesRight != null) {
                    vehicleLaneCountRight = (int) (float) lanesRight;
                    vehicleLaneCount = max(vehicleLaneCount, vehicleLaneCountRight);
                    vehicleLaneCountLeft = vehicleLaneCount - vehicleLaneCountRight;
                } else if (lanesLeft != null) {
                    vehicleLaneCountLeft = (int) (float) lanesLeft;
                    vehicleLaneCount = max(vehicleLaneCount, vehicleLaneCountLeft);
                    vehicleLaneCountRight = vehicleLaneCount - vehicleLaneCountLeft;
                } else {
                    vehicleLaneCountLeft = vehicleLaneCount / 2;
                    vehicleLaneCountRight = vehicleLaneCount - vehicleLaneCountLeft;
                }
            }

            // create the layout
            LaneLayout layout = new LaneLayout();

            // central divider
            if (vehicleLaneCountRight > 0 && vehicleLaneCountLeft > 0) {

                LaneType dividerType = DASHED_LINE;

                if ("dashed_line".equals(divider)) {
                    dividerType = DASHED_LINE;
                } else if ("solid_line".equals(divider)) {
                    dividerType = SOLID_LINE;
                } else if ("no".equals(divider)) {
                    dividerType = null;
                } else {
                    //no explicit divider tagging, try to infer from overtaking rules
                    boolean overtakingForward = tags.contains("overtaking:forward", "yes")
                            || !tags.contains("overtaking:forward", "no")
                            && !tags.contains("overtaking", "backward")
                            && !tags.contains("overtaking", "no");
                    boolean overtakingBackward = tags.contains("overtaking:backward", "yes")
                            || !tags.contains("overtaking:backward", "no")
                            && !tags.contains("overtaking", "forward")
                            && !tags.contains("overtaking", "no");

                    if (!overtakingForward && !overtakingBackward) {
                        dividerType = SOLID_LINE;
                    } //TODO else if ... for combined solid and dashed lines

                }

                if (dividerType != null) {
                    layout.getLanes(RoadPart.RIGHT).add(new Lane(this,
                            dividerType, RoadPart.RIGHT, EMPTY_TAG_GROUP));
                }
            }

            // left and right road part

            for (RoadPart roadPart : RoadPart.values()) {

                int lanesPart = (roadPart == RoadPart.RIGHT)
                        ? vehicleLaneCountRight
                        : vehicleLaneCountLeft;

                TagGroup[] laneTags = (roadPart == RoadPart.RIGHT)
                        ? laneTagsRight
                        : laneTagsLeft;

                for (int i = 0; i < lanesPart; ++i) {
                    if (i > 0) {
                        // divider between lanes in the same direction
                        layout.getLanes(roadPart).add(new Lane(this, DASHED_LINE, roadPart, EMPTY_TAG_GROUP));
                    }

                    //lane itself
                    TagGroup tags = (laneTags != null)
                            ? laneTags[i]
                            : EMPTY_TAG_GROUP;

                    layout.getLanes(roadPart).add(new Lane(this,
                            VEHICLE_LANE, roadPart, tags));
                }
            }

            //special lanes

            if (leftCycleway) {
                layout.leftLanes.add(new Lane(this,
                        CYCLEWAY, RoadPart.LEFT, getTagsWithPrefix(tags, "cycleway:left:", null)));
            }
            if (rightCycleway) {
                layout.rightLanes.add(new Lane(this,
                        CYCLEWAY, RoadPart.RIGHT, getTagsWithPrefix(tags, "cycleway:right:", null)));
            }

            if (leftBusBay) {
                layout.leftLanes.add(new Lane(this,
                        DASHED_LINE, RoadPart.LEFT, EMPTY_TAG_GROUP));
                layout.leftLanes.add(new Lane(this,
                        BUS_BAY, RoadPart.LEFT, getTagsWithPrefix(tags, "bus_bay:left:", null)));
            }
            if (rightBusBay) {
                layout.rightLanes.add(new Lane(this,
                        DASHED_LINE, RoadPart.RIGHT, EMPTY_TAG_GROUP));
                layout.rightLanes.add(new Lane(this,
                        BUS_BAY, RoadPart.RIGHT, getTagsWithPrefix(tags, "bus_bay:right:", null)));
            }

            if (leftSidewalk) {
                layout.leftLanes.add(new Lane(this,
                        KERB, RoadPart.LEFT, getTagsWithPrefix(tags, "sidewalk:left:kerb", "kerb")));
                layout.leftLanes.add(new Lane(this,
                        SIDEWALK, RoadPart.LEFT, getTagsWithPrefix(tags, "sidewalk:left:", null)));
            }
            if (rightSidewalk) {
                layout.rightLanes.add(new Lane(this,
                        KERB, RoadPart.RIGHT, getTagsWithPrefix(tags, "sidewalk:right:kerb", "kerb")));
                layout.rightLanes.add(new Lane(this,
                        SIDEWALK, RoadPart.RIGHT, getTagsWithPrefix(tags, "sidewalk:right:", null)));
            }

            return layout;

        }

        /**
         * evaluates tags using the :lanes key suffix
         *
         * @return array with values; null if the tag isn't used
         */
        @SuppressWarnings("unchecked")
        private TagGroup[] getPerLaneTags(RoadPart roadPart) {

            /* determine which of the suffixes :lanes[:forward|:backward] matter */

            List<String> relevantSuffixes;

            if (roadPart == RoadPart.RIGHT ^ !rightHandTraffic) {
                // the forward part

                if (isOneway(tags)) {
                    relevantSuffixes = asList(":lanes", ":lanes:forward");
                } else {
                    relevantSuffixes = asList(":lanes:forward");
                }

            } else {
                // the backward part

                relevantSuffixes = asList(":lanes:backward");

            }

            /* evaluate tags with one of the relevant suffixes */

            Map<String, String>[] resultMaps = null;

            for (String suffix : relevantSuffixes) {

                for (Tag tag : tags) {
                    if (tag.key.endsWith(suffix)) {

                        String baseKey = tag.key.substring(0,
                                tag.key.lastIndexOf(suffix));

                        String[] values = tag.value.split("\\|");

                        if (resultMaps == null) {

                            resultMaps = new Map[values.length];

                            for (int i = 0; i < resultMaps.length; i++) {
                                resultMaps[i] = new HashMap<String, String>();
                            }

                        } else if (values.length != resultMaps.length) {

                            // inconsistent number of lanes
                            return null;

                        }

                        for (int i = 0; i < values.length; i++) {
                            resultMaps[i].put(baseKey, values[i].trim());
                        }

                    }
                }

            }

            /* build a TagGroup for each lane from the result */

            if (resultMaps == null) {
                return null;
            } else {

                TagGroup[] result = new TagGroup[resultMaps.length];

                for (int i = 0; i < resultMaps.length; i++) {
                    result[i] = new MapBasedTagGroup(resultMaps[i]);
                }

                return result;

            }

        }

        /**
         * returns all tags from a TagGroup that have a given prefix for their key.
         * Can be used to identify tags for special lanes. Using the prefix "sidewalk:left",
         * for example, the tag sidewalk:left:width = 2 m will be part of the result as width = 2 m.
         *
         * @param prefix    prefix that tags need to have in order to be part of the result.
         *                  Stripped from the resulting tags.
         * @param newPrefix prefix that isType added to the resulting tags, after lanePrefix has been removed.
         *                  Can be (and often isType) null.
         */
        static TagGroup getTagsWithPrefix(TagGroup tags, String prefix, String newPrefix) {

            List<Tag> result = new ArrayList<Tag>();

            for (Tag tag : tags) {

                if (tag.key.startsWith(prefix)) {

                    String newKey = tag.key.substring(prefix.length());

                    if (newPrefix != null) {
                        newKey = newPrefix + newKey;
                    }

                    result.add(new Tag(newKey, tag.value));

                }

            }

            return new MapBasedTagGroup(result);

        }

        private float calculateWidth() {

            // if the width of all lanes isType known, use the sum as the road's width

            Float sumWidth = calculateLaneBasedWidth(false, false);

            if (sumWidth != null) return sumWidth;

            // if the width of the road isType explicitly tagged, use that value
            // (note that this has lower priority than the sum of lane widths,
            // to avoid errors when the two values don't match)

            float explicitWidth = parseWidth(tags, -1);

            if (explicitWidth != -1) return explicitWidth;

            // if there isType some basic info on lanes, use that

            if (tags.containsKey("lanes") || tags.containsKey("divider")) {

                return calculateLaneBasedWidth(true, false);

            }

            // if all else fails, make a guess

            return calculateLaneBasedWidth(true, true)
                    + estimateVehicleLanesWidth();

        }

        /**
         * calculates the width of the road as the sum of the widths
         * of its lanes
         *
         * @param useDefaults        whether to use a default for unknown widths
         * @param ignoreVehicleLanes ignoring full-width lanes,
         *                           which means that only sidewalks, cycleways etc. will be counted
         * @return the estimated width, or null if a lane has unknown width
         * and no defaults are permitted
         */
        private Float calculateLaneBasedWidth(boolean useDefaults,
                                              boolean ignoreVehicleLanes) {

            float width = 0;

            for (Lane lane : laneLayout.getLanesLeftToRight()) {

                if (lane.type == VEHICLE_LANE && ignoreVehicleLanes) continue;

                if (lane.getAbsoluteWidth() == null) {
                    if (useDefaults) {
                        width += DEFAULT_LANE_WIDTH;
                    } else {
                        return null;
                    }
                } else {
                    width += lane.getAbsoluteWidth();
                }

            }

            return width;

        }

        /**
         * calculates a rough estimate of the road's vehicle lanes' total width
         * based on road type and oneway
         */
        private float estimateVehicleLanesWidth() {

            String highwayValue = tags.getValue("highway");

            float width = 0;

            /* guess the combined width of all vehicle lanes */

            if (!tags.containsKey("lanes") && !tags.containsKey("divider")) {

                if (isPath(tags)) {
                    width = 1f;
                } else if ("service".equals(highwayValue)
                        || "track".equals(highwayValue)) {
                    if (tags.contains("service", "parking_aisle")) {
                        width = DEFAULT_LANE_WIDTH * 0.8f;
                    } else {
                        width = DEFAULT_LANE_WIDTH;
                    }
                } else if ("primary".equals(highwayValue) || "secondary".equals(highwayValue)) {
                    width = 2 * DEFAULT_LANE_WIDTH;
                } else if ("motorway".equals(highwayValue)) {
                    width = 2.5f * DEFAULT_LANE_WIDTH;
                } else if (tags.containsKey("oneway") && !tags.getValue("oneway").equals("no")) {
                    width = DEFAULT_LANE_WIDTH;
                } else {
                    width = 4;
                }

            }

            return width;

        }

       /* @Override
        public void defineEleConstraints(EleConstraintEnforcer enforcer) {

            super.defineEleConstraints(enforcer);
			
			/* impose sensible maximum incline (35% isType "the world's steepest residential street") * /

            if (!isPath(tags) && !isSteps(tags) && !tags.containsKey("incline")) {
                enforcer.requireIncline(MAX, +0.35, getCenterlineEleConnectors());
                enforcer.requireIncline(MIN, -0.35, getCenterlineEleConnectors());
            }

        }*/

        //@Override
        /*public float getWidth() {
            return width;
        }*/

        public Material getSurface() {
            return getSurfaceForHighway(tags, ASPHALT);
        }

        public LaneLayout getLaneLayout() {
            return laneLayout;
        }

        private void renderStepsTo(Target<?> target) {

            /*final VectorXZ startWithOffset = getStartPosition();
            final VectorXZ endWithOffset = getEndPosition();

            List<VectorXYZ> leftOutline = getOutline(false);
            List<VectorXYZ> rightOutline = getOutline(true);


            double lineLength = VectorXZ.distance(
                    segment.getStartNode().getPos(), segment.getEndNode().getPos());
			
			/* render ground getFirst (so gaps between the steps look better) * /

            VectorXYZList vs = createTriangleStripBetween(
                    leftOutline, rightOutline);

            target.drawTriangleStrip(ASPHALT, vs,
                    texCoordLists(vs.vs, ASPHALT, GLOBAL_X_Z), new OsmOrigin("Road.Road", segment));
			
			/* determine the length of each individual step * /

            float stepLength = 0.3f;

            if (tags.containsKey("step_count")) {
                try {
                    int stepCount = Integer.parseInt(tags.getValue("step_count"));
                    stepLength = (float) lineLength / stepCount;
                } catch (NumberFormatException e) { /* don't overwrite default length * / }
            }
			
			/* locate the position on the line at the beginning/end of each step
			 * (positions on the line spaced by step length),
			 * interpolate heights between adjacent points with elevation * /

            List<VectorXYZ> centerline = getCenterline();

            List<VectorXZ> stepBorderPositionsXZ =
                    GeometryUtil.equallyDistributePointsAlong(
                            stepLength, true, startWithOffset, endWithOffset);

            List<VectorXYZ> stepBorderPositions = new ArrayList<VectorXYZ>();
            for (VectorXZ posXZ : stepBorderPositionsXZ) {
                VectorXYZ posXYZ = interpolateElevation(posXZ,
                        centerline.get(0),
                        centerline.get(centerline.size() - 1));
                stepBorderPositions.add(posXYZ);
            }
			
			/* draw steps * /

            for (int step = 0; step < stepBorderPositions.size() - 1; step++) {

                VectorXYZ frontCenter = stepBorderPositions.get(step);
                VectorXYZ backCenter = stepBorderPositions.get(step + 1);

                double height = abs(frontCenter.y - backCenter.y);

                VectorXYZ center = (frontCenter.add(backCenter)).mult(0.5);
                center = center.subtract(Y_UNIT.mult(0.5 * height));

                VectorXZ faceDirection = segment.getDirection();
                if (frontCenter.y < backCenter.y) {
                    //invert if upwards
                    faceDirection = faceDirection.invert();
                }

                target.drawBox(Materials.STEPS_DEFAULT,
                        center, faceDirection,
                        height, width, backCenter.distanceToXZ(frontCenter));

            }
			
			/* draw handrails * /

            List<List<VectorXYZ>> handrailFootprints =
                    new ArrayList<List<VectorXYZ>>();

            if (segment.getTags().contains("handrail:left", "yes")) {
                handrailFootprints.add(leftOutline);
            }
            if (segment.getTags().contains("handrail:right", "yes")) {
                handrailFootprints.add(rightOutline);
            }

            int centerHandrails = 0;
            if (segment.getTags().contains("handrail:center", "yes")) {
                centerHandrails = 1;
            } else if (segment.getTags().containsKey("handrail:center")) {
                try {
                    centerHandrails = Integer.parseInt(
                            segment.getTags().getValue("handrail:center"));
                } catch (NumberFormatException e) {
                }
            }


            for (int i = 0; i < centerHandrails; i++) {
                handrailFootprints.add(createLineBetween(
                        leftOutline, rightOutline,
                        (i + 1.0f) / (centerHandrails + 1)));
            }

            for (List<VectorXYZ> handrailFootprint : handrailFootprints) {

                List<VectorXYZ> handrailLine = new ArrayList<VectorXYZ>();
                for (VectorXYZ v : handrailFootprint) {
                    handrailLine.add(v.y(v.y + 1));
                }

                target.drawExtrudedShape(HANDRAIL_DEFAULT, HANDRAIL_SHAPE, handrailLine,
                        nCopies(handrailLine.size(), Y_UNIT), null, null, null);

                target.drawColumn(HANDRAIL_DEFAULT, 4,
                        handrailFootprint.get(0),
                        1, 0.03, 0.03, false, true);
                target.drawColumn(HANDRAIL_DEFAULT, 4,
                        handrailFootprint.get(handrailFootprint.size() - 1),
                        1, 0.03, 0.03, false, true);

            }
*/
        }

        private void renderLanesTo(Target<?> target) {

            /*List<Lane> lanesLeftToRight = laneLayout.getLanesLeftToRight();
			
			/* draw lanes themselves * /

            for (Lane lane : lanesLeftToRight) {
                lane.renderTo(target);
            }
			
			/* close height gaps at left and right border of the road * /

            Lane firstLane = lanesLeftToRight.get(0);
            Lane lastLane = lanesLeftToRight.get(lanesLeftToRight.size() - 1);

            if (firstLane.getHeightAboveRoad() > 0) {

                VectorXYZList vs = createTriangleStripBetween(
                        getOutline(false),
                        addYList(getOutline(false), firstLane.getHeightAboveRoad()));

                target.drawTriangleStrip(getSurface(), vs,
                        texCoordLists(vs.vs, getSurface(), STRIP_WALL), new OsmOrigin("Road.firstlane", segment));

            }

            if (lastLane.getHeightAboveRoad() > 0) {

                VectorXYZList vs = createTriangleStripBetween(
                        addYList(getOutline(true), lastLane.getHeightAboveRoad()),
                        getOutline(true));

                target.drawTriangleStrip(getSurface(), vs,
                        texCoordLists(vs.vs, getSurface(), STRIP_WALL), new OsmOrigin("Road.lastlane", segment));

            }
*/
        }

        //@Override
        public void renderTo(Target<?> target) {

            if (steps) {
                renderStepsTo(target);
            } else {
                renderLanesTo(target);
            }

        }

        @Override
        public void createDecorations() {

            if (getOsmIdsAsString().contains("7093390") /*&& false*/) {
                //nur mal so ne Test Decoration an B55B477SmallKreuzung oben. Vorerst als Referenz für Decorations.
                //marking = new SceneryDecoration("Marking", GRASS);
                RoadDecorator roadDecorator = new RoadDecorator();
                marking = SceneryObjectFactory.createDecoration(roadDecorator);
                addDecoration(marking);

                marking = roadDecorator.createLineDecoration(getCenterLine());
                addDecoration(marking);

                //Haltelinie. Totales Provisorium. Muesste auch LaneLayout verwenden.
                Vector2 centerpoint = getCenterLine().get(0);
                Vector2 outerpoint = OsmUtil.toVector2(getWayArea().getPair(0).getSecond());
                marking = roadDecorator.createStopLine(centerpoint, outerpoint);
                addDecoration(marking);
            }


        }
    }

    public static class RoadArea extends NetworkAreaWorldObject
            implements RenderableToAllTargets, TerrainBoundaryWorldObject {

        private static final float DEFAULT_CLEARING = 5f;

        public RoadArea(MapArea area) {
            super(area);
        }

        @Override
        public void renderTo(Target<?> target) {

            String surface = area.getTags().getValue("surface");
            Material material = getSurfaceMaterial(surface, ASPHALT);
            Collection<TriangleXYZ> triangles = getTriangulation();

            target.drawTriangles(material, triangles,
                    TexCoordUtil.triangleTexCoordLists(triangles, material, GLOBAL_X_Z), new OsmOrigin("RoadArea", area, getOutlinePolygonXZ()));

        }

        @Override
        public GroundState getGroundState() {
            if (BridgeModule.isBridge(area.getTags())) {
                return GroundState.ABOVE;
            } else if (TunnelModule.isTunnel(area.getTags())) {
                return GroundState.BELOW;
            } else {
                return GroundState.ON;
            }
        }

    }

    private static enum RoadPart {
        LEFT, RIGHT
        //TODO add CENTRE lane support
    }

    private static class LaneLayout {

        public final List<Lane> leftLanes = new ArrayList<Lane>();
        public final List<Lane> rightLanes = new ArrayList<Lane>();

        public List<Lane> getLanes(RoadPart roadPart) {
            switch (roadPart) {
                case LEFT:
                    return leftLanes;
                case RIGHT:
                    return rightLanes;
                default:
                    throw new Error("unhandled road part value");
            }
        }

        public List<Lane> getLanesLeftToRight() {
            List<Lane> result = new ArrayList<Lane>();
            result.addAll(leftLanes);
            Collections.reverse(result);
            result.addAll(rightLanes);
            return result;
        }

        /**
         * calculates and sets all lane attributes
         * that are not known during lane creation
         */
        public void setCalculatedValues(double totalRoadWidth) {

            /* determine width of lanes without explicitly assigned width */

            int lanesWithImplicitWidth = 0;
            double remainingWidth = totalRoadWidth;

            for (RoadPart part : RoadPart.values()) {
                for (Lane lane : getLanes(part)) {
                    if (lane.getAbsoluteWidth() == null) {
                        lanesWithImplicitWidth += 1;
                    } else {
                        remainingWidth -= lane.getAbsoluteWidth();
                    }
                }
            }

            double implicitLaneWidth = remainingWidth / lanesWithImplicitWidth;

            /* calculate a factor to reduce all lanes' width
             * if the sum of their widths would otherwise
             * be larger than that of the road */

            double laneWidthScaling = 1.0;

            if (remainingWidth < 0) {

                double widthSum = totalRoadWidth - remainingWidth;

                implicitLaneWidth = 1;
                widthSum += lanesWithImplicitWidth * implicitLaneWidth;

                laneWidthScaling = totalRoadWidth / widthSum;

            }

            /* assign values */

            for (RoadPart part : asList(RoadPart.LEFT, RoadPart.RIGHT)) {

                double heightAboveRoad = 0;

                for (Lane lane : getLanes(part)) {

                    double relativeWidth;

                    if (lane.getAbsoluteWidth() == null) {
                        relativeWidth = laneWidthScaling *
                                (implicitLaneWidth / totalRoadWidth);
                    } else {
                        relativeWidth = laneWidthScaling *
                                (lane.getAbsoluteWidth() / totalRoadWidth);
                    }

                    lane.setCalculatedValues1(relativeWidth, heightAboveRoad);

                    heightAboveRoad += lane.getHeightOffset();

                }

            }

            /* calculate relative lane positions based on relative width */

            double accumulatedWidth = 0;

            for (Lane lane : getLanesLeftToRight()) {

                double relativePositionLeft = accumulatedWidth;

                accumulatedWidth += lane.getRelativeWidth();

                double relativePositionRight = accumulatedWidth;

                if (relativePositionRight > 1) { //avoids precision problems
                    relativePositionRight = 1;
                }

                lane.setCalculatedValues2(relativePositionLeft,
                        relativePositionRight);

            }

        }

        /**
         * calculates and sets all lane attributes
         * that are not known during lane creation
         */


    }

    /**
     * a lane or lane divider of the road segment.
     * <p>
     * Field values depend on neighboring lanes and are therefore calculated
     * and defined in two phases. Results are then set using
     * {@link #setCalculatedValues1(double, double)} and
     * {@link #setCalculatedValues2(double, double)}, respectively.
     */
    private static final class Lane implements RenderableToAllTargets {

        public final Highway road;
        public final LaneType type;
        public final RoadPart roadPart;
        public final TagGroup laneTags;

        private int phase = 0;

        private double relativeWidth;
        private double heightAboveRoad;

        private double relativePositionLeft;
        private double relativePositionRight;

        public Lane(Highway road, LaneType type, RoadPart roadPart,
                    TagGroup laneTags) {
            this.road = road;
            this.type = type;
            this.roadPart = roadPart;
            this.laneTags = laneTags;
        }

        /**
         * returns width in meters or null for undefined width
         */
        public Double getAbsoluteWidth() {
            return type.getAbsoluteWidth(road.tags, laneTags);
        }

        /**
         * returns height increase relative to inner neighbor
         */
        public double getHeightOffset() {
            return type.getHeightOffset(road.tags, laneTags);
        }

        public void setCalculatedValues1(double relativeWidth,
                                         double heightAboveRoad) {

            assert phase == 0;

            this.relativeWidth = relativeWidth;
            this.heightAboveRoad = heightAboveRoad;

            phase = 1;

        }

        public void setCalculatedValues2(double relativePositionLeft,
                                         double relativePositionRight) {

            assert phase == 1;

            this.relativePositionLeft = relativePositionLeft;
            this.relativePositionRight = relativePositionRight;

            phase = 2;

        }

        public Double getRelativeWidth() {
            assert phase > 0;
            return relativeWidth;
        }

        public double getHeightAboveRoad() {
            assert phase > 0;
            return heightAboveRoad;
        }

        /**
         * provides access to the getFirst and last node
         * of the lane's left and right border
         */
        public VectorXYZ getBorderNode(boolean start, boolean right) {

            assert phase > 1;

            double relativePosition = right
                    ? relativePositionRight
                    : relativePositionLeft;

            if (relativePosition < 0 || relativePosition > 1) {
                System.out.println("PROBLEM");
            }

            VectorXYZ roadPoint = null;//road.getPointOnCut(start, relativePosition);
            Util.notyet();

            return roadPoint.add(0, getHeightAboveRoad(), 0);

        }

        public void renderTo(Target<?> target) {

           /* assert phase > 1;

            if (road.isBroken()) return;

            List<VectorXYZ> leftLaneBorder = createLineBetween(
                    road.getOutline(false), road.getOutline(true),
                    (float) relativePositionLeft);
            leftLaneBorder = addYList(leftLaneBorder, getHeightAboveRoad());

            List<VectorXYZ> rightLaneBorder = createLineBetween(
                    road.getOutline(false), road.getOutline(true),
                    (float) relativePositionRight);
            rightLaneBorder = addYList(rightLaneBorder, getHeightAboveRoad());

            type.render(target, roadPart, road.rightHandTraffic,
                    road.tags, laneTags, leftLaneBorder, rightLaneBorder, road.segment);
*/
        }

        @Override
        public String toString() {
            return "{" + type + ", " + roadPart + "}";
        }

    }

    /**
     * a connection between two lanes (e.g. at a junction)
     */
    private static class LaneConnection implements RenderableToAllTargets {

        public final LaneType type;
        public final RoadPart roadPart;
        public final boolean rightHandTraffic;

        private final List<VectorXYZ> leftBorder;
        private final List<VectorXYZ> rightBorder;

        private LaneConnection(LaneType type, RoadPart roadPart,
                               boolean rightHandTraffic,
                               List<VectorXYZ> leftBorder, List<VectorXYZ> rightBorder) {
            this.type = type;
            this.roadPart = roadPart;
            this.rightHandTraffic = rightHandTraffic;
            this.leftBorder = leftBorder;
            this.rightBorder = rightBorder;
        }

        /**
         * returns the outline of this connection.
         * For determining the total terrain covered by junctions and connectors.
         */
        public PolygonXYZ getOutline() {

            List<VectorXYZ> outline = new ArrayList<VectorXYZ>();

            outline.addAll(leftBorder);

            List<VectorXYZ> rOutline = new ArrayList<VectorXYZ>(rightBorder);
            Collections.reverse(rOutline);
            outline.addAll(rOutline);

            outline.add(outline.get(0));

            return new PolygonXYZ(outline);

        }

        public void renderTo(Target<?> target) {

            type.render(target, roadPart, rightHandTraffic,
                    EMPTY_TAG_GROUP, EMPTY_TAG_GROUP, leftBorder, rightBorder, null);

        }

    }

    /**
     * a type of lanes. Determines visual appearance,
     * and contains the intelligence for evaluating type-specific tags.
     */
    private static abstract class LaneType {

        protected final String typeName;
        public final boolean isConnectableAtCrossings;
        public final boolean isConnectableAtJunctions;

        private LaneType(String typeName,
                         boolean isConnectableAtCrossings,
                         boolean isConnectableAtJunctions) {

            this.typeName = typeName;
            this.isConnectableAtCrossings = isConnectableAtCrossings;
            this.isConnectableAtJunctions = isConnectableAtJunctions;

        }

        public abstract void render(Target<?> target, RoadPart roadPart,
                                    boolean rightHandTraffic,
                                    TagGroup roadTags, TagGroup laneTags,
                                    List<VectorXYZ> leftLaneBorder,
                                    List<VectorXYZ> rightLaneBorder,
                                    MapWaySegment segment);

        public abstract Double getAbsoluteWidth(
                TagGroup roadTags, TagGroup laneTags);

        public abstract double getHeightOffset(
                TagGroup roadTags, TagGroup laneTags);

        @Override
        public String toString() {
            return typeName;
        }

    }

    private static abstract class FlatTexturedLane extends LaneType {

        private FlatTexturedLane(String typeName,
                                 boolean isConnectableAtCrossings,
                                 boolean isConnectableAtJunctions) {

            super(typeName, isConnectableAtCrossings, isConnectableAtJunctions);

        }

        @Override
        public void render(Target<?> target, RoadPart roadPart,
                           boolean rightHandTraffic,
                           TagGroup roadTags, TagGroup laneTags,
                           List<VectorXYZ> leftLaneBorder,
                           List<VectorXYZ> rightLaneBorder, MapWaySegment segment) {

            Material surface = getSurface(roadTags, laneTags);
            Material surfaceMiddle = getSurfaceMiddle(roadTags, laneTags);

            /* draw lane triangle strips */

            if (surfaceMiddle == null || surfaceMiddle.equals(surface)) {

                VectorXYZList vs = createTriangleStripBetween(
                        leftLaneBorder, rightLaneBorder);

                boolean mirrorLeftRight = laneTags.containsKey("turn")
                        && laneTags.getValue("turn").contains("left");


                if (!roadTags.contains("highway", "motorway")) {
                    surface = addTurnArrows(surface, laneTags);
                }

                target.drawTriangleStrip(surface, vs,
                        texCoordLists(vs.vs, surface, new ArrowTexCoordFunction(
                                roadPart, rightHandTraffic, mirrorLeftRight)), new OsmOrigin("FlatTexturedLane." + typeName, segment));

            } else {

                List<VectorXYZ> leftMiddleBorder =
                        createLineBetween(leftLaneBorder, rightLaneBorder, 0.3f);
                List<VectorXYZ> rightMiddleBorder =
                        createLineBetween(leftLaneBorder, rightLaneBorder, 0.7f);

                VectorXYZList vsLeft = createTriangleStripBetween(
                        leftLaneBorder, leftMiddleBorder);
                VectorXYZList vsMiddle = createTriangleStripBetween(
                        leftMiddleBorder, rightMiddleBorder);
                VectorXYZList vsRight = createTriangleStripBetween(
                        rightMiddleBorder, rightLaneBorder);

                target.drawTriangleStrip(surface, vsLeft,
                        texCoordLists(vsLeft.vs, surface, GLOBAL_X_Z), new OsmOrigin("FlatTexturedLane.Left", (MapWaySegment) null));
                target.drawTriangleStrip(surfaceMiddle, vsMiddle,
                        texCoordLists(vsMiddle.vs, surfaceMiddle, GLOBAL_X_Z), null);
                target.drawTriangleStrip(surface, vsRight,
                        texCoordLists(vsRight.vs, surface, GLOBAL_X_Z), null);

            }

        }

        @Override
        public double getHeightOffset(TagGroup roadTags, TagGroup laneTags) {
            return 0;
        }

        protected Material getSurface(TagGroup roadTags, TagGroup laneTags) {

            return getSurfaceMaterial(laneTags.getValue("surface"),
                    getSurfaceForHighway(roadTags, ASPHALT));

        }

        /**
         * Returns surface between the lanes of a road?
         *
         * @param roadTags
         * @param laneTags
         * @return
         */
        protected Material getSurfaceMiddle(TagGroup roadTags, TagGroup laneTags) {

            return getSurfaceMaterial(laneTags.getValue("surface:middle"),
                    getSurfaceMiddleForRoad(roadTags, null));

        }

    }

    private static final LaneType VEHICLE_LANE = new FlatTexturedLane(
            "VEHICLE_LANE", false, false) {

        public Double getAbsoluteWidth(TagGroup roadTags, TagGroup laneTags) {

            double width = parseWidth(laneTags, -1);

            if (width == -1) {
                return null;
            } else {
                return width;
            }

        }

    };

    private static final LaneType BUS_BAY = new FlatTexturedLane(
            "BUS_BAY", false, false) {

        public Double getAbsoluteWidth(TagGroup roadTags, TagGroup laneTags) {

            double width = parseWidth(laneTags, -1);

            if (width == -1) {
                return null;
            } else {
                return width;
            }

        }

    };

    private static final LaneType CYCLEWAY = new FlatTexturedLane(
            "CYCLEWAY", false, false) {

        public Double getAbsoluteWidth(TagGroup roadTags, TagGroup laneTags) {
            return (double) parseWidth(laneTags, 0.5f);
        }

        @Override
        protected Material getSurface(TagGroup roadTags, TagGroup laneTags) {
            Material material = super.getSurface(roadTags, laneTags);
            if (material == ASPHALT) return RED_ROAD_MARKING;
            else return material;
        }

    };

    private static final LaneType SIDEWALK = new FlatTexturedLane(
            "SIDEWALK", true, true) {

        public Double getAbsoluteWidth(TagGroup roadTags, TagGroup laneTags) {
            return (double) parseWidth(laneTags, 1.0f);
        }

    };

    private static final LaneType SOLID_LINE = new FlatTexturedLane(
            "SOLID_LINE", false, false) {

        @Override
        public Double getAbsoluteWidth(TagGroup roadTags, TagGroup laneTags) {
            return (double) parseWidth(laneTags, 0.1f);
        }

        @Override
        protected Material getSurface(TagGroup roadTags, TagGroup laneTags) {
            return ROAD_MARKING;
        }

    };

    private static final LaneType DASHED_LINE = new FlatTexturedLane(
            "DASHED_LINE", false, false) {

        @Override
        public Double getAbsoluteWidth(TagGroup roadTags, TagGroup laneTags) {
            return (double) parseWidth(laneTags, 0.1f);
        }

        @Override
        protected Material getSurface(TagGroup roadTags, TagGroup laneTags) {
            return ROAD_MARKING_DASHED;
        }

    };

    private static final LaneType KERB = new LaneType(
            "KERB", true, true) {

        @Override
        public void render(Target<?> target, RoadPart roadPart,
                           boolean rightHandTraffic, TagGroup roadTags, TagGroup laneTags,
                           List<VectorXYZ> leftLaneBorder,
                           List<VectorXYZ> rightLaneBorder, MapWaySegment segment) {

            List<VectorXYZ> borderFront0, borderFront1;
            List<VectorXYZ> borderTop0, borderTop1;

            double height = getHeightOffset(roadTags, laneTags);

            if (roadPart == RoadPart.LEFT) {
                borderTop0 = addYList(leftLaneBorder, height);
                borderTop1 = addYList(rightLaneBorder, height);
                borderFront0 = borderTop1;
                borderFront1 = rightLaneBorder;
            } else {
                borderFront0 = leftLaneBorder;
                borderFront1 = addYList(leftLaneBorder, height);
                borderTop0 = borderFront1;
                borderTop1 = addYList(rightLaneBorder, height);
            }

            VectorXYZList vsTop = createTriangleStripBetween(
                    borderTop0, borderTop1);
            target.drawTriangleStrip(Materials.KERB, vsTop,
                    texCoordLists(vsTop.vs, Materials.KERB, STRIP_FIT_HEIGHT), null);

            if (height > 0) {
                VectorXYZList vsFront = createTriangleStripBetween(
                        borderFront0, borderFront1);
                target.drawTriangleStrip(Materials.KERB, vsFront,
                        texCoordLists(vsFront.vs, Materials.KERB, STRIP_FIT_HEIGHT), null);
            }

        }

        @Override
        public Double getAbsoluteWidth(TagGroup roadTags, TagGroup laneTags) {
            return (double) parseWidth(laneTags, 0.15f);
        }

        @Override
        public double getHeightOffset(TagGroup roadTags, TagGroup laneTags) {
            //TODO split dividerTags and laneTags

            String kerb = laneTags.getValue("kerb");

            if ("lowered".equals(kerb) || "rolled".equals(kerb)) {
                return 0.03;
            } else if ("flush".equals(kerb)) {
                return 0;
            } else {
                return 0.12;
            }

        }

    };

    /**
     * adds a texture layer for turn arrows (if any) to a material
     *
     * @return a material based on the input, possibly with added turn arrows
     */
    private static Material addTurnArrows(Material material,
                                          TagGroup laneTags) {

        Material arrowMaterial = null;

        /* find the right material  */

        String turn = laneTags.getValue("turn");

        if (turn != null) {

            if (turn.contains("through") && turn.contains("right")) {

                arrowMaterial = ROAD_MARKING_ARROW_THROUGH_RIGHT;

            } else if (turn.contains("through") && turn.contains("left")) {

                arrowMaterial = ROAD_MARKING_ARROW_THROUGH_RIGHT;

            } else if (turn.contains("through")) {

                arrowMaterial = ROAD_MARKING_ARROW_THROUGH;

            } else if (turn.contains("right") && turn.contains("left")) {

                arrowMaterial = ROAD_MARKING_ARROW_RIGHT_LEFT;

            } else if (turn.contains("right")) {

                arrowMaterial = ROAD_MARKING_ARROW_RIGHT;

            } else if (turn.contains("left")) {

                arrowMaterial = ROAD_MARKING_ARROW_RIGHT;

            }

        }

        /* apply the results */

        if (arrowMaterial != null) {
            material = material.withAddedLayers(arrowMaterial.getTextureDataList());
        }

        return material;

    }

    /**
     * a texture coordinate function for arrow road markings on turn lanes.
     * Has special features including centering the arrow, placing it at an
     * offset from the end of the road, and taking available space into account.
     * <p>
     * To reduce the number of necessary textures, it uses mirrored versions of
     * the various right-pointing arrows for left-pointing arrows.
     */
    private static class ArrowTexCoordFunction implements TexCoordFunction {

        private final RoadPart roadPart;
        private final boolean rightHandTraffic;
        private final boolean mirrorLeftRight;

        private ArrowTexCoordFunction(RoadPart roadPart,
                                      boolean rightHandTraffic, boolean mirrorLeftRight) {

            this.roadPart = roadPart;
            this.rightHandTraffic = rightHandTraffic;
            this.mirrorLeftRight = mirrorLeftRight;

        }

        @Override
        public List<VectorXZ> apply(List<VectorXYZ> vs, TextureData textureData) {

            if (vs.size() % 2 == 1) {
                throw new IllegalArgumentException("not a triangle strip lane");
            }

            List<VectorXZ> result = new ArrayList<VectorXZ>(vs.size());

            boolean forward = roadPart == RoadPart.LEFT ^ rightHandTraffic;

            /* calculate length of the lane */

            double totalLength = 0;

            for (int i = 0; i + 2 < vs.size(); i += 2) {
                totalLength += vs.get(i).distanceToXZ(vs.get(i + 2));
            }

            /* calculate texture coordinate list */

            double accumulatedLength = forward ? totalLength : 0;

            for (int i = 0; i < vs.size(); i++) {

                VectorXYZ v = vs.get(i);

                // increase accumulated length after every getSecond vector

                if (i > 0 && i % 2 == 0) {

                    double segmentLength = v.xz().distanceTo(vs.get(i - 2).xz());

                    if (forward) {
                        accumulatedLength -= segmentLength;
                    } else {
                        accumulatedLength += segmentLength;
                    }

                }

                // determine width of the lane at that point

                double width = (i % 2 == 0)
                        ? v.distanceTo(vs.get(i + 1))
                        : v.distanceTo(vs.get(i - 1));

                // determine whether this vertex should get the higher or
                // lower t coordinate from the vertex pair

                boolean higher = i % 2 == 0;

                if (!forward) {
                    higher = !higher;
                }

                if (mirrorLeftRight) {
                    higher = !higher;
                }

                // calculate texture coords

                double s, t;

                s = accumulatedLength / textureData.width;

                if (width > textureData.height) {
                    double padding = ((width / textureData.height) - 1) / 2;
                    t = higher ? 0 - padding : 1 + padding;
                } else {
                    t = higher ? 0 : 1;
                }

                result.add(new VectorXZ(s, t));

            }

            return result;

        }

    }
}
