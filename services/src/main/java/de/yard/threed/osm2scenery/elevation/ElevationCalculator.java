package de.yard.threed.osm2scenery.elevation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.modules.BridgeModule;
import de.yard.threed.osm2scenery.scenery.Background;
import de.yard.threed.osm2scenery.scenery.BackgroundElement;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.util.PolygonMetadata;
import de.yard.threed.traffic.geodesy.ElevationProvider;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * <p>
 * Ich gehe jetzt so vor. Brücken/Tunnel spielen dabei eine wichtige Rolle.
 * 1) Festlegung von 1-n Fixpunkten. Die ergeben sich auf jeden Fall aus den Gridendpunkten, denen explizit eine
 * Elevation zugeordnet ist. Auf den Gridkanten kann es optional auch noch weitere Fixpunkte geben.
 * Auch innerhalb des Grid kann es z.B. aus den OSM Daten Fixpunkte geben.
 * Da wo ways das Grid verlassen, werden die Elevations interpoliert, wenn sie nicht explizit vorliegen.
 * 1a) An allen GridEnter Ways einen Fix setzen. Wobei das doch auch für Flächen gemacht werden muss.
 * 2) Elevation für Fluesse, weil die die konstanteste Steigung haben. Und an den Gridgrenzen gibt es auch Fixe.
 * 3) Zu den Rivern dann die Brücken
 * 4) Elevation zu Railways weil die ähnlich flache Steigungen haben, anhand deren Brücken. Wenn es keine Brücken gibt, sollten
 * sie etwas über
 * den Rivern liegen.
 * 5) Brücken über die Railways.
 * 6) Dann die Autobahnen, weil die eher flacher sind.
 * 7) Alle Brücken durchgehen dass jede eine obere und untere Elevation hat.
 * 8) Dann die restlichen Strassen. Dazu alle Bereiche zwischen bekannten Elevations (Brücken, GridEnter)
 * nacheinander durchgehen. Auch Taxiway,
 * 9) Die verbliebenen Strassen, die noch übriggeblieben sind. Wie genau, wird sich noch zeigen.
 * 9a) Hier die Areas?
 * 10) Wenn alle {@link EleConnectorGroup} gefixed sind, die Elevation aller Objekte (z in Coordinate der Polygone) setzen.
 * 11) Die Elevations der Background setzen
 * <p>
 * Created on 27.07.18.
 */
public class ElevationCalculator {
    static Logger logger = Logger.getLogger(ElevationCalculator.class);

    public static void fixElevationGroups(SceneryObjectList sceneryObjects, ElevationProvider elevationProvider) {
        //1 Gridgrenzen sind schon in ElevationMap
        //1a Alle Grid Enter Ways fixen
        fixGridEnterings(sceneryObjects);
        //2
        elevateWays(sceneryObjects.findWaysByCategory(SceneryObject.Category.RIVER));
        //4 Railways
        //fixRailways(sceneryObjects);
        elevateWays(sceneryObjects.findWaysByCategory(SceneryObject.Category.RAILWAY));
        //7
        completeBridges();
        //8
        elevateWays(sceneryObjects.findWaysByCategory(SceneryObject.Category.ROAD));
        elevateWays(sceneryObjects.findWaysByCategory(SceneryObject.Category.TAXIWAY));
        elevateWays(sceneryObjects.findWaysByCategory(SceneryObject.Category.RAILWAY));

        // 9a
        for (SceneryObject obj : sceneryObjects.objects) {
            //TODO doofe Klassenhierarchie
            if (obj instanceof SceneryFlatObject && !(obj instanceof SceneryWayObject)) {
                elevateArea((SceneryFlatObject) obj, elevationProvider);
            }
        }
    }

    public static void calculateElevations(SceneryObjectList sceneryObjects, ElevationProvider elevationProvider) {
        validateEleGroups(sceneryObjects);
        // 10 erst die Objekte, dann das Grid um auch auessere Polygonpunkte zu registrieren. Die Reihenfolge ist hier aber egal.
        // wegen besseren Intuition aber erst grid
        ElevationMap.getInstance().registerGridElevations();
        for (SceneryObject obj : sceneryObjects.objects) {
            obj.calculateElevations();
        }
        //TODO Background auch hier machen
    }

    /**
     * Check that every EleGroup isType fixed.
     *
     * @param sceneryObjects
     */
    private static void validateEleGroups(SceneryObjectList sceneryObjects) {
        for (SceneryObject so : sceneryObjects.objects) {
            for (EleConnectorGroup elegroup : so.getEleConnectorGroups().eleconnectorgroups) {
                if (!elegroup.isFixed()) {
                    throw new RuntimeException("unfixed elegroup");
                }
            }
        }
    }


    /**
     * Nicht nur fuer Ways. Für alle EleGroups an den grid node und allen nicht innerhalb des Grid
     * die Elevation anhand des Grid fixen.
     *
     * @param sceneryObjects
     */
    private static void fixGridEnterings(SceneryObjectList sceneryObjects) {
        for (SceneryObject obj : sceneryObjects.objects) {
            for (EleConnectorGroup eg : obj.getEleConnectorGroups().eleconnectorgroups) {
                // Die Objekte enthalten ja nur Referenzen, darum können hier durchaus Groups mehrfach vorkommen. Die
                // dann aber skippen.
                if (!eg.isFixed()) {
                    ElevationMap.getInstance().fixOuterAndGridnodeEleConnectorGroup(eg);
                }
            }
        }
    }

    /**
     * Im Background gibt es keine EleConnector so wie sonst. Das wird hier temporär angelegt.
     * Eigentlich muessten die BG Coords doch an die Connectoren der angrenzeden SceneryObjects.
     * Genau. Sucht fuer jede Coordinate eine bestehende Group.
     * 12.6.19: Die Coordinates des BG sollten schon alle in einer EGR eines SO stehen, vor allem die neuen vom cut.
     * Bei LazyCut aber nur mit FTR_TRACKEDBPCOORS.
     *
     * @param background
     * @param sceneryObjects
     */
    public static void registerBackgroundElevations(List<BackgroundElement> background, SceneryObjectList sceneryObjects) {
        for (BackgroundElement be : background) {
            //be.eleConnectorGroupSet = new EleConnectorGroupSet();
            PolygonMetadata polygonMetadata = new PolygonMetadata(be);
            Coordinate[] coord = be.polygon.getCoordinates();
            for (int i = 0; i < coord.length - 1; i++) {
                EleCoordinate e = polygonMetadata.addPoint(null, coord[i]);
                //EleConnectorGroup egr = new EleConnectorGroup(null, Util.toList(e));
                //be.eleConnectorGroupSet.add(egr);
                Double elevation = null;
                EleConnectorGroup egr = EleConnectorGroup.getGroup(coord[i], false, " for background", !SceneryBuilder.FTR_TRACKEDBPCOORS);
                if (egr == null) {
                    // Wie kann das sein? 28.9.18: Solange es keine richtigen RoadConnector gibt, fehlen die intersections der Ways ja als Coordinate!
                    // 12.6.19: Kann aber auch durch Triangulation passieren, oder? Und Lazycut. Darum erst später loggen.
                    boolean success = findEgrGroupForCoordinate(coord[i], sceneryObjects.objects);
                    if (!success) {
                        logger.warn("no group found for bg point " + coord[i] + ". ");
                        SceneryContext.getInstance().warnings.add("no group found for bg point " + coord[i] + ". ");
                        elevation = findElevationForCoordinate(coord[i]);

                    }
                } else {
                    //12.6.19 muesste schon drin sein egr.add(e);
                    elevation = egr.getElevation();
                }

                //egr.setElevation(elevation);
            }
        }
    }

    public static void calculateBackgroundElevations(Background background) {
        for (BackgroundElement be : background.background) {
            PolygonMetadata polygonMetadata = new PolygonMetadata(be);

            calculateElevationsForPolygon(be.polygon, polygonMetadata, be.vertexData, null);
        }
        for (AbstractArea be : background.getBgfiller()) {
            if (be.isEmpty()) {
                logger.warn("empty BG filler?");
            } else {
                calculateElevationsForCoordinates(be.getPolygon().getCoordinates(), null);
                calculateElevationsForVertexCoordinates(be.getVertexData().vertices, null);
            }
        }
    }

    /**
     * In den Sceneryobjekten nach der Elevation einer Coordinate suchen. Die gibt es evtl. aber gar nicht, oder?
     * ISt ja für Background. Muesste es nicht immer
     * einen geben? TODO
     * Nicht aus den Objekten, denn da gibt es die Groups ja mehrfach und somit bekomme ich die Elevations mehrfahc.
     * Einach aus der Map (16.6.19 ????).
     * es koennte auch mehrere geben, z.B. an BridgeRamps, zumindest z.Z. Ist jetzt aber wegdefiniert.
     * 12.6.19: BG coordinates sollten SO Elegroups zugeordnet sein. Dies hier ist nur, falls da nichts gefunden wird.
     * Oder bei LazyCut/!FTR_TRACKEDBPCOORS. Die SO doch durchgehen, aber auch auf den PolygonEdges suchen, nicht
     * nur Coordinates. Und auch den uncutcoord.
     *
     * @param coordinate
     * @param sceneryObjects
     * @return
     */
    private static boolean findEgrGroupForCoordinate(Coordinate coordinate, List<SceneryObject> sceneryObjects) {
        //List<Float> elevations = new ArrayList<>();
        for (SceneryObject obj : sceneryObjects) {
            if (obj instanceof SceneryFlatObject) {
                SceneryFlatObject area = ((SceneryFlatObject) obj);
                EleConnectorGroup eleConnector = area.findEleGroupForBgCoordinate(coordinate);
                if (eleConnector != null) {
                    eleConnector.add(new EleCoordinate(coordinate, eleConnector, ""));
                /*Double elevation = eleConnector.getElevation();
                /*if (elevation != null) {*/
                    //16.6.19: Return oder nicht
                    return true;
                    //elevations.add(elevation);
                }
            }
        }
        /*elevations.add(ElevationMap.getInstance().cmap.get(coordinate));
        if (elevations.size() == 0) {
            logger.warn("no elevation found for " + coordinate);
            return 0;
        }
        if (elevations.size() == 1) {
            Float e = elevations.get(0);*/

        return false;
    }

    /**
     * Nicht aus den Objekten, denn da gibt es die Groups ja mehrfach und somit bekomme ich die Elevations mehrfahc.
     * Einach aus der Map (16.6.19 ????).
     * es koennte auch mehrere geben, z.B. an BridgeRamps, zumindest z.Z. Ist jetzt aber wegdefiniert.
     * <p>
     * 16.6.19: Wofuer ist die jetzt genau?
     *
     * @param coordinate
     * @return
     */
    private static double findElevationForCoordinate(Coordinate coordinate) {

        Double elevation = ElevationMap.getInstance().getElevationForCoordinate(coordinate);
        if (elevation == null) {
            //wie kann das sein? Mit Triangulation kann das doch nicht zu tun haben.
            //30.8.18: An den GridEnter fehlt ja noch der cut. TODO
            logger.error("elevation not found for background");
            ElevationMap.getInstance().problemlist.add(coordinate);
            elevation = ElevationMap.getInstance().getElevationForClosestCoordinate(coordinate);
        }
        return elevation;
        /*String s = "";
        float max = 0;
        for (Float e : elevations) {
            s += "" + e.toString() + ",";
            if (e > max) {
                max = e;
            }
        }
        logger.info("multiple elevations " + s + " found for " + coordinate + ". Using " + max);
        return max;*/
    }

    /**
     * Final setting of elevation for all vertices of a polygon. Called after triangulation.
     * These elevation are registered per coordinate (if not BG).
     * <p>
     * But not for background (sfo will be null)
     * Hier muss jeder Point des Polygon einer Group zugeordnet sein. Ist das möglich?
     * Nicht unbedingt, zumindest nicht im BG?
     * Unbekannte Vertices kann es durch die Triangulation immer geben. Da die wahrscheinlich auf der Boundary liegen,
     * muesste man da gewichtet mitteln.
     * 17.6.19:Zumindest BG points muessten mittlerweile alle registriert sein.
     * <p>
     * TODO SmartPolygone wieder einfuehren?
     *
     * @param poly
     * @param polygonMetadata
     */
    public static void calculateElevationsForPolygon(Polygon poly, PolygonMetadata polygonMetadata, VertexData vertexData, String debuglabel) {

        Coordinate[] coor = poly.getCoordinates();
        //Teil nach unten ausgelagert
        calculateElevationsForCoordinates(coor, debuglabel);

        // 4.8.18: Auch in der vertexdata eintragen.
        Double efound = null;
        if (vertexData == null || vertexData.vertices == null) {
            //19.7.19: Betrachte ich nicht mehr als warning?
            logger.warn("calculateElevationsForPolygon: poly has no vertex data. Skipping elevation");
            return;
        }
        calculateElevationsForVertexCoordinates(vertexData.vertices, debuglabel);

        /*if (efound!=null){
            for (Coordinate c:vertexData.vertices){
                if (Double.isNaN(c.z)){
                    c.setOrdinate(Coordinate.Z,efound);
                }
            }
        }*/
    }

    /**
     * Es muss eine EleGroup für die Coordinate geben, sonst ist etwas schief gelaufen.
     *
     * @param coor
     * @param debuglabel
     */
    public static void calculateElevationsForCoordinates(Coordinate[] coor, String debuglabel) {
        for (Coordinate c : coor) {
            //EleConnector eleConnector = polygonMetadata.getEleConnector(c);
            // null isType never returned
            //elevationProvider.getElevation((float) mapNode.getOsmNode().lat, (float) mapNode.getOsmNode().lon);
            //Float elevation = eleConnector.getGroup().getElevation();
            EleConnectorGroup eleConnectorGroup = EleConnectorGroup.getGroup(c, false, "for polygon " + debuglabel, false);
            Double elevation = new Double(0);
            if (eleConnectorGroup == null) {
                logger.warn("group for coordinate " + c + " not found (using 0) in " + debuglabel);
            } else {
                elevation = eleConnectorGroup.getElevation();
                if (elevation == null) {
                    logger.warn("no elevation set in group. ussng 0.");
                    elevation = new Double(0);
                }
            }
            c.setOrdinate(Coordinate.Z, elevation);
        }
    }

    public static void calculateElevationsForVertexCoordinates(List<Coordinate> coor, String debuglabel) {
        for (Coordinate c : coor) {
            /*if (!Double.isNaN(c.z)){
                efound= c.z;
            }*/
            //EleConnector eleConnector = polygonMetadata.getEleConnector(c);
            EleConnectorGroup eleConnectorGroup = EleConnectorGroup.getGroup(c, true, "for vertex", false);
            //5.9.18: 68, bis alle Ungereimtheiten weg sind, damit Tests nicht scheitern. TODO
            Double elevation = new Double(68);
            if (eleConnectorGroup == null) {
                // durch cut oder Triangulation. Obwohl, beim cut werden sie doch neu zugeordnet. Aber Triangulation halt.
                // siehe Kopf. eigentlich muesste gewichtet gemittelt werden, vor allem am Grid. TODO
                //den warning erstmal weglassen, weil die Lösung nicht zu schlecht ist.
                //5.9.18: Doch, die ist schlecht. Führt bei Ramp z.B. zu nicht sichtbaren Triangles.
                //28.9.18: debug statt warning, weil das bei Triangulation immer sein kann und voellig unbedenklich ist. Bei onboundary darum
                //gar nicht loggen
                //13.8.19: Das mit PolygonBoundary wird jetzt nicht mehr gemacht. Und auch nicht metadaten. Die Coordinate MUSS im TerrainMesh da sein.
                boolean onBoundary = false;//JtsUtil.onBoundary(c, poly);
                if (!onBoundary) {
                    logger.debug("group for vertex not found:" + c + ", onBoundary=" + onBoundary + " for creatortag " + debuglabel);
                    SceneryContext.getInstance().warnings.add("group for vertex not found:" + c + ", onBoundary=" + onBoundary + " for creatortag " + debuglabel);
                }
                if (onBoundary) {
                    LineSegment[] s = null;//JtsUtil.getBoundaryLine(c, poly);
                    // Die z Werte muessten ja schon in der Group drin stehen.
                    EleConnectorGroup g0 = EleConnectorGroup.getGroup(s[0].p0, false, "for boundary", false);
                    EleConnectorGroup g1 = EleConnectorGroup.getGroup(s[0].p1, false, "for boundary", false);
                    if (g0 == null || g1 == null || g0.getElevation() == null || g1.getElevation() == null) {
                        //wie kommt das denn schon wieder?
                        logger.error("g0 or g1 isType null");
                    } else {
                        elevation = new Double((g0.getElevation() + g1.getElevation()) / 2f);
                    }
                } else {
                    // die schlehctere Lösung. 23.7.19: Metadata gibt es nicht mehr immer
                    /*if (polygonMetadata != null) {
                        List<Coordinate> other = polygonMetadata.findClosest(c);
                        for (Coordinate oc : other) {
                            if ((eleConnectorGroup = EleConnectorGroup.getGroup(oc, false, "no boundary", false)) != null) {
                                break;
                            }
                        }
                    }*/
                    if (eleConnectorGroup == null) {
                        logger.warn("still no group for vertex:" + c);
                    } else {
                        elevation = eleConnectorGroup.getElevation();
                    }
                }
            } else {
                elevation = eleConnectorGroup.getElevation();
            }
            // durch possible interpolation there isType no group here.
            if (elevation == null) {
                logger.warn("no elevation foundp. eleConnectorGroup=" + eleConnectorGroup);
                elevation = new Double(0);
            }

            c.setOrdinate(Coordinate.Z, elevation);
            /*19.7.19 if (sfo != null) {
                ElevationMap.getInstance().registerElevation(c, elevation, eleConnectorGroup/*eleConnector.getGroup()* /);
            }*/

        }
    }

    /**
     * 7
     */

    private static void completeBridges() {
        for (BridgeModule.Bridge bridge : SceneryContext.getInstance().bridges.values()) {
            bridge.fixElevation();
        }
    }

    /**
     * 8
     */
    private static void elevateWays(List<SceneryWayObject> ways) {
        int i = 0;
        do {
            //FixedEleConnectorGroupSet fixedEleConnectorGroupSet = new FixedEleConnectorGroupSet();
            ElevationArea unfixed = findUnfixedSegement(ways);
            if (unfixed.unfixed.size() == 0) {
                //no more way elements without elevation
                return;
            }
            unfixed.fixUnfixed();

        } while (i++ < 100);
        throw new RuntimeException("not aus");
    }

    /**
     * Einen Bereich fuer eine bestimmt Categorie bestimmen, in dem es noch ungefixte EleGroups gibt.
     * Liefert ein Menge von zusammenhaengenden Ways, die noch einen Fix brauchen.
     */
    private static ElevationArea findUnfixedSegement(List<SceneryWayObject> ways) {
        ElevationArea elevationArea = new ElevationArea();

        for (SceneryWayObject way : ways) {
            if (!way.hasFixedElevationGroups()) {
                addUnfixedSegement(elevationArea, way);
            }
        }
        return elevationArea;
    }

    private static void addUnfixedSegement(ElevationArea elevationArea, SceneryWayObject road) {

        elevationArea.addSegment(road);

        List<SceneryWayObject> connected = road.getConnectedRoads();
        for (SceneryWayObject r : connected) {
            if (!r.hasFixedElevationGroups() && !elevationArea.unfixed.contains(r)) {
                addUnfixedSegement(elevationArea, r);
            }
        }
    }

    /**
     * Die unfixed Groups der area fixen. Das ist nicht fuer BG.
     * <p>
     * TODO einbauen in ElevationMAp. Das hier ist noch nicht stimmig. 5.9.18: Warum nicht?
     */
    private static void elevateArea(SceneryFlatObject area, ElevationProvider elevationProvider) {
        if (area.getEleConnectorGroups() == null) {
            logger.error("no ele group");
        }
        if (area.getOsmIdsAsString().contains("231544305")) {
            int h = 9;
        }
        if (area.creatortag.equals("RunwaySegment")) {
            int h = 9;
        }
        for (EleConnectorGroup eg : area.getEleConnectorGroups().eleconnectorgroups) {
            if (!eg.isFixed()) {
                double elevation;
                elevation = elevationProvider.getElevation(0, 0);
                //11.6.19: Muesste dann eigentlich inside, sonst waere es schon gefixed(?)
                //12.7.19 entbehrlich?eg.fixLocation(EleConnectorGroup.Location.INSIDEGRID);
                eg.fixElevation(elevation);
            }
        }

    }
}
