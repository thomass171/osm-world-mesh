package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.Vector3;
import de.yard.threed.graph.GraphNode;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.elevation.EleCoordinate;
import de.yard.threed.osm2scenery.elevation.ElevationMap;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.GraphComponent;
import de.yard.threed.osm2scenery.scenery.components.WayArea;
import de.yard.threed.osm2scenery.scenery.components.WayTerrainMeshAdder;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import de.yard.threed.osm2world.GroundState;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.MapWaySegment;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.VectorXZ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.ROAD;

/**
 * Scenery object that relates to a graph, like road, railway, river
 * and with visualisation.
 * <p>
 * 4.4.19: Ein Way ist meistens ein "StandardWay" (WayArea), mit immer paarweise links/rechts Coordinates, auch nach einem cut.
 * Und pro einem solchen Pärchen gibt es genau eine EleConnectorgroup.
 *
 * <p>
 * Created on 26.07.18.
 */
public class SceneryWayObject extends SceneryFlatObject {
    public MapWay mapWay;
    //corresponds to mapway segments
    GraphComponent graphComponent = null;
    public SceneryWayConnector startConnector = null;
    public SceneryWayConnector endConnector = null;
    //jetzt in abstractArea Coordinate[] uncutcoord;
    //public LineSegment baselineStart;
    //LineSegment baselineEnd;
    double width = 0;
    //10.7.19 private double cliplenStart = 0, cliplenEnd = 0;
    // private boolean standardway = true;
    WidthProvider widthProvider;
    //DEADEND isType just the default
    public WayOuterMode startMode = WayOuterMode.DEADEND, endMode = WayOuterMode.DEADEND;
    //start/end bezogen aufs grid
    public int startNode, endNode;
    public List<SceneryWayConnector> innerConnector;
    //logische way position->connector
    public Map<Integer, SceneryWayConnector> innerConnectorMap;
    public List<MapNode> effectiveNodes;

    protected SceneryWayObject(String creatortag, MapWay mapWay, Material material, Category category, WidthProvider widthProvider, SceneryContext sceneryContext) {
        super(creatortag, material, category, null/*new WayArea(material)*/);
        this.mapWay = mapWay;
        this.widthProvider = widthProvider;
        this.cycle = Cycle.WAY;
        graphComponent = new GraphComponent(this, category, sceneryContext);
        //11.4.19: Die soll doch wohl nachgehalten werden
        osmIds.add(mapWay.getOsmId());
        setNameFromOsm(mapWay.getTags());
        startNode = 0;
        endNode = mapWay.getMapNodes().size() - 1;
        int firstgridnode = OsmUtil.getFirstGridNode(mapWay);
        if (firstgridnode != -1) {
            if (firstgridnode > 0) {
                if (mapWay.getStartNode().location == MapNode.Location.OUTSIDEGRID) {
                    startMode = WayOuterMode.GRIDBOUNDARY;
                    startNode = firstgridnode;
                } else if (mapWay.getStartNode().location == MapNode.Location.INSIDEGRID) {
                    endMode = WayOuterMode.GRIDBOUNDARY;
                    endNode = firstgridnode;
                    //startMode.gridboundarynode = firstgridnode;
                }
            } else {
                //TODO koennte auch nach outside gehen
                startMode = WayOuterMode.GRIDBOUNDARY;
                //startMode.gridboundarynode = firstgridnode;
            }

            int lastgridnode = OsmUtil.getLastGridNode(mapWay);
            if (lastgridnode != -1) {
                if (lastgridnode < mapWay.getMapNodes().size() - 1) {
                    if (mapWay.getEndNode().location == MapNode.Location.OUTSIDEGRID) {
                        endMode = WayOuterMode.GRIDBOUNDARY;
                        //endMode.gridboundarynode = lastgridnode;
                        endNode = lastgridnode;
                    } else if (mapWay.getStartNode().location == MapNode.Location.INSIDEGRID) {
                        // startMode = WayOuterMode.;
                        //startMode.gridboundarynode = firstgridnode;
                    }
                } else {
                    //TODO koennte auch nach outside gehen
                    //startMode = WayOuterMode.GRIDBOUNDARY;
                    //startMode.gridboundarynode = firstgridnode;
                }
            }
        } else {
            // no node on grid. All outside?
            if (mapWay.getMapNodes().get(0).location == MapNode.Location.OUTSIDEGRID) {
                //all outside
                startNode = endNode = -1;
            }
        }

        effectiveNodes = new ArrayList();
        for (int i = startNode; startNode != -1 && i <= endNode; i++) {
            effectiveNodes.add(mapWay.getMapNodes().get(i));
        }
        terrainMeshAdder = new WayTerrainMeshAdder(this);
    }

    /**
     * 13.8.19:Keine mehr ausserhalb vom Grid.
     */
    @Override
    public void buildEleGroups() {
        elevations = new EleConnectorGroupSet();
        for (int i = 0; i < mapWay.getMapNodes().size(); i++) {
            MapNode node = mapWay.getMapNodes().get(i);
            if (node.location != MapNode.Location.OUTSIDEGRID) {
                EleConnectorGroup eleConnectorGroup = getEleConnectorGroup(node);
                getEleConnectorGroups().add(eleConnectorGroup);
            /*if (ElevationMap.hasInstance() && ElevationMap.getInstance().isGridNode(node)) {
                ElevationMap.getInstance().gridCellBounds.gridnodes.add(node);
            }*/
                if (ElevationMap.isGridNode(node)) {
                    EleConnectorGroup.gridCellBounds.additionalGridnodes.add(node);
                }
            }
        }
    }

    @Override
    public List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds, TerrainMesh tm, SceneryContext sceneryContext) {
        createPolygon(widthProvider.getWidth(), gridbounds, sceneryContext);
        return null;
    }

    public double getWidth() {
        return widthProvider.getWidth();
    }

    public void setWidthProvider(WidthProvider widthProvider) {
        this.widthProvider = widthProvider;
    }

    /**
     * Eigentlich wohl Teil des Construcots, braucht aber width.
     * Ausserdem kann es günstig sein, den Polygon erst dann anzulegen, wenn alle
     * Verbindungen bekannt sind. Denn ergibt sich vielleicht ein sicher verbreiterndes Element.(??)
     *
     * @param width
     */
    private/*protected*/ void createPolygon(double width, GridCellBounds gridbounds, SceneryContext sceneryContext) {
        this.width = width;
        //polygon = MapDataHelper.getOutlinePolygon(mapWay, width);
        if (mapWay.getOsmId() == 107468171) {
            int h = 9;
        }
        if (effectiveNodes.size() < 2) {
            logger.info("only " + effectiveNodes.size() + " effective nodes for way " + mapWay.getOsmId() + ". Setting to empty");
            flatComponent = new AbstractArea[]{AbstractArea.EMPTYAREA};
        } else {
            AbstractArea area = WayArea.buildOutlinePolygonFromCenterLine(graphComponent.getCenterLine(), effectiveNodes/* mapWay.getMapNodes()*/, width, this, material);
            if (area == null) {
                flatComponent = new AbstractArea[]{AbstractArea.EMPTYAREA};
            } else {
                area.parentInfo = this.toString();
                flatComponent = new AbstractArea[]{area};
                if (flatComponent[0] instanceof WayArea) {
                    ((WayArea) flatComponent[0]).osmid = mapWay.getOsmId();
                }
                //Für P20 die Segmente zwischen Connector separat durchgehen. Dann kann ein intermediate Connector zunächst wie
                //ein EndConnector behandelt werden.
                //10.7.19:NeeNee. Bei MidConnector bleibt der Way erhalten, bekommt nur 4 andere Coordinates
                if (getArea()[0] instanceof WayArea) {
                    extendMidwayConnector(sceneryContext);
                }
            }
        }
    }

    /**
     * Den Polygon fuer die inner Connector erweitern.
     */
    private void extendMidwayConnector(SceneryContext sceneryContext) {
        WayArea wayArea = getWayArea();
        if (effectiveNodes/*mapWay.getMapNodes()*/.size() != wayArea.getLength()) {
            logger.error("mapnode list inconsistent");
            return;
        }
        innerConnector = new ArrayList<>();
        innerConnectorMap = new HashMap<>();
        for (int i = 1; i < effectiveNodes/*mapWay.getMapNodes()*/.size() - 1; i++) {
            MapNode mapNode = effectiveNodes/*mapWay.getMapNodes()*/.get(i);
            SceneryWayConnector connector = sceneryContext.wayMap.getConnector(ROAD, mapNode.getOsmId());
            if (connector != null) {
                if (connector.getType() != SceneryWayConnector.WayConnectorType.SIMPLE_INNER_SINGLE_JUNCTION &&
                        connector.getType() != SceneryWayConnector.WayConnectorType.SIMPLE_INNER_DOUBLE_JUNCTION/* &&
                        connector.getType() != SceneryWayConnector.WayConnectorType.CROSSING*/) {
                    logger.error("inconsistent inner connector " + connector.getType() + " at node " + mapNode.getOsmId());
                } else {
                    Integer position = getWayArea().getPosition(mapNode);
                    if (position != null) {
                        innerConnector.add(connector);
                        innerConnectorMap.put(position, connector);
                        double offset = 3.5;
                        CoordinatePair s0 = wayArea.shift(position, -offset);
                        CoordinatePair s1 = wayArea.shift(position, offset);
                        if (s0 != null && s1 != null) {
                            if (!wayArea.replace(position, new CoordinatePair[]{s0, s1})) {
                                logger.debug("Trying smaller offset for inner connector additional pair");
                                offset = 0.5;
                                s0 = wayArea.shift(position, -offset);
                                s1 = wayArea.shift(position, offset);
                                if (s0 != null && s1 != null) {
                                    if (!wayArea.replace(position, new CoordinatePair[]{s0, s1})) {
                                        logger.warn("Smaller offset failed too");
                                    }
                                }
                            }
                        }
                    } else {
                        logger.warn("wasn das?");
                    }
                }
            }
        }
    }

    /**
     * Abschneiden des Way an einem Ende. Z.B. weil eine Junction angelegt wurde.
     * Die wegfallenden EleConnector werden durch den der Junction ersetzt (was i.d.R. immer dieselben sein
     * dürften).
     * Weil der clip erstmal nur für Junction ist, wird nicht per Polygon, senkrecht an einem Ende geschnitten.
     * Der Aufrufer baut aus den Schnitten dann die Junction.
     * Das ganze muss vor createPolygon erfolgen. Es wird nur als Info dafuer hinterlegt.
     * 10.7.19: Nee, umgekehrt.
     */
    @Override
    public void clip(/*10.7.19 boolean atstart, double len*/TerrainMesh tm) {

        if (isClipped) {
            return;
        }
        super.clip(tm);
        if (mapWay.getOsmId() == 33817500 || mapWay.getOsmId() == 173190487) {
            int h = 9;
        }
        if (mapWay.getOsmId() == 33817499 || mapWay.getOsmId() == 189650380) {
            int h = 9;
        }
        if (flatComponent != null && flatComponent[0].isEmpty(tm)) {
            return;
        }

        if (flatComponent == null || flatComponent[0].uncutcoord == null) {
            logger.error("too early for clip. coord doesnt exist");
            return;
        }
        if (!isValid()) {
            logger.error("way not valid, no clip");
            return;
        }
        if (!(flatComponent[0] instanceof WayArea)) {
            logger.error("no way area, no clip");
            return;
        }
        // logger.debug("clip of way");


        WayArea wayArea = (WayArea) flatComponent[0];
        //Der Connector gibt die Coordinates vor. Er hat beim createPolygon schon den clip vorbereitet.
        //Die Inner Connector sind schon erledigt.
        switch (startMode) {
            case DEADEND:
                break;
            case CONNECTOR:
                CoordinatePair attachpair = startConnector.getAttachCoordinates(mapWay);
                if (attachpair != null) {
                    if (isClosed()) {
                        if (!wayArea.replaceStartEnd(attachpair)) {
                            logger.error("replace for clip failed at connector " + startConnector.getOsmIdsAsString());
                        }
                    } else {
                        if (!wayArea.replaceStart(attachpair)) {
                            logger.error("replace for clip failed at connector " + startConnector.getOsmIdsAsString());
                        }
                    }
                    // Manche Connector erstellen für den minor attach ein additionl pair, das hier in den Way eingebaut wird.
                    if (startConnector.additionalmain0pair != null) {
                        if (startConnector.majorway0 == -1) {
                            logger.error("startconnector without major0:" + startConnector.node.getOsmId());
                        } else {
                            if (startConnector.getMajor0().getArea()[0] == wayArea) {
                                wayArea.replaceStart(new CoordinatePair[]{wayArea.getStartPair(tm)[0], startConnector.additionalmain0pair});
                            }
                        }
                    }
                } else {
                    SceneryContext.getInstance().warnings.add("no attach");
                    failureCounter++;
                }
                break;
        }
        if (!isClosed()) {
            switch (endMode) {
                case DEADEND:
                    break;
                case CONNECTOR:
                    CoordinatePair attachpair = endConnector.getAttachCoordinates(mapWay);
                    if (attachpair != null) {
                        if (!((WayArea) flatComponent[0]).replaceEnd(attachpair)) {
                            logger.error("replace for clip failed at connector " + endConnector.getOsmIdsAsString());
                        }
                        // Manche Connector erstellen für den minor attach ein additionl pair, das hier in den Way eingebaut wird.
                        if (endConnector.additionalmain0pair != null) {
                            if (endConnector.majorway0 == -1) {
                                logger.error("endconnector without major0:" + endConnector.node.getOsmId());
                            } else {
                                if (endConnector.getMajor0().getArea()[0] == wayArea) {
                                    wayArea.replaceEnd(new CoordinatePair[]{endConnector.additionalmain0pair, wayArea.getEndPair()[0]});
                                }
                            }
                        }
                    } else {
                        SceneryContext.getInstance().warnings.add("no attach");
                        failureCounter++;
                    }
                    break;
            }
        }
    }

    /**
     * start/end werden im connector resolved
     *
     * @param overlap
     */
    public void resolveWayOverlaps(AbstractArea overlap, TerrainMesh tm) {
        WayArea wayArea = getWayArea();

        if (startConnector != null) {
            startConnector.resolveOverlaps(overlap, tm);
        }
        /*CoordinatePair reduced = OverlapResolver.resolveSingleWayOverlap(getWayArea(),0,overlap);
        if (reduced!=null){
            logger.debug("unhandled start conn reduce at connector. Might be resolved by counterpart. ");
        }*/
        if (wayArea == null) {
            logger.error("no way");
            return;
        }
        for (int i = 1; i < wayArea.getLength() - 1; i++) {
            /*CoordinatePair[] pair = wayToReduce.getMultiplePair(i);
            if (pair.length != 1) {
                logger.error("not yet");
                return;
            }
            Polygon polygon = overlappedarea.getPolygon();
            //TODO zu gross?iterativ
            double offset = 1;
            if (JtsUtil.isInside(pair[0].left(), polygon)) {
                /*if (node.getOsmId() == 1353883859) {
                    int h = 9;
                }* /
                CoordinatePair reduced = wayToReduce.reduce(i, offset);
                if (!wayToReduce.replace(new int[]{i}, reduced)) {
                    logger.error("replace for adjust failed at way " + wayToReduce.parentInfo);
                }
            }*/
        }
       /*  reduced = OverlapResolver.resolveSingleWayOverlap(getWayArea(),wayArea.getLength()-1,overlap);
        if (reduced!=null){
            logger.debug("unhandled end conn reduce. Might be resolved by counterpart.");
        }*/
        if (endConnector != null) {
            endConnector.resolveOverlaps(overlap, tm);
        }
    }

    /**
     * 28.8.18: Nur noch fuer die "inner nodes", die anderen sind in den Connector Nodes
     * NeeNee, hier werden ja vor allem die EleConnector an die Group gehangen. Die Group wird
     * aber nur fuer inner (und isolierte outer) neu angelegt.
     * Die Groups werden jetzt schon im Constructor gebaut.
     * 31.8.18: Nur noch zuordnen, und das geht wirklich am einfachsten ueber den Way. Geht irgendwie nicht schon in createPolygon().
     *
     * @return
     */
    @Override
    public void/*EleConnectorGroupSet*/ registerCoordinatesToElegroups(TerrainMesh tm) {
        if (getOsmIdsAsString().contains("37935545")) {
            int h = 9;
        }

        if (flatComponent != null && !flatComponent[0].isEmpty(tm)) {
            if (isCut) {
                //Util.notyet();
                //13.8.19: wofuer ist das?? logger.warn("Ignoring cut area for elevation");
            }

            flatComponent[0].registerCoordinatesToElegroups(elevations, tm);
        }
        //Die durch cut im BE entstandenen Coordinates registrieren. Erstmal einfach an die erste EleGroup. TODO improve
        if (newcoordinates != null && newcoordinates.size() > 0) {
            EleConnectorGroup egr = elevations.eleconnectorgroups.get(0);

            for (Coordinate c : newcoordinates) {
                if (!EleConnectorGroup.hasGroup(c)) {
                    egr.add(new EleCoordinate(c, egr, "new BG coordinate"));
                }
            }
        }
        //16.6.19: BG kommt vielleicht noch spater egr.locked = true;
    }


    public void addToWayMap(Category category, SceneryContext sceneryContext) {
        sceneryContext.wayMap.registerWayAtNode(category, mapWay.getStartNode(), this);
        for (MapWaySegment seg : mapWay.getMapWaySegments()) {
            sceneryContext.wayMap.registerWayAtNode(category, seg.getEndNode(), this);
        }
    }

    /**
     * Returns groups shared with other ways. Might not only be start/end but also intermediate. And GridNodes and isolated way end points.
     * TODO intermediate
     * <p>
     * 24.4.19: Das ist aber eine reichlich unspezifische Ergebnismenge. Wer soll damit etwas sinnvolles tun?
     * 24.4.19: Das passt aber doch nicht zum Namen. Rename getOuterEleConnectorGroups-> getSharedEleConnectorGroups
     *
     * @return
     */
    @Override
    public EleConnectorGroupSet getSharedEleConnectorGroups() {
        EleConnectorGroupSet egr = new EleConnectorGroupSet();
        Map<Long, SceneryWayConnector> wayconnectors = SceneryContext.getInstance().wayMap.getConnectorMapForCategory(getCategory());
        for (int i = 0; i < effectiveNodes/*mapWay.getMapNodes()*/.size(); i++) {
            MapNode mapNode = effectiveNodes/*mapWay.getMapNodes()*/.get(i);
            SceneryNodeObject wayconnector = wayconnectors.get(mapNode.getOsmId());
            if (wayconnector != null) {
                egr.addAll(wayconnector.elevations.eleconnectorgroups);
            } else {
                if (mapNode.getOsmId() < 0) {
                    //grid node
                    egr.add(EleConnectorGroup.elegroups.get(mapNode.getOsmId()));
                } else {
                    //start/end
                    if (i == 0 || i == effectiveNodes/*mapWay.getMapNodes()*/.size() - 1) {
                        egr.add(EleConnectorGroup.elegroups.get(mapNode.getOsmId()));
                    }
                }
            }
        }
        return egr;
    }

    /**
     * Aus oberer Methode wieder reduziert, um wirklich nur die beiden Endpunkte zu liefern.
     *
     * @return
     */
    public EleConnectorGroupSet getOuterEleConnectorGroups() {
        EleConnectorGroupSet egr = new EleConnectorGroupSet();
        egr.add(elevations.eleconnectorgroups.get(0));
        egr.add(elevations.eleconnectorgroups.get(elevations.eleconnectorgroups.size() - 1));
        return egr;
    }

    /**
     * Liefert die EleConnectorGroups der angrenzenden Ways. Liefert nur die direkten unmittelbar angrenzenden Nachbarn.
     * 5.9.18: Ist das nicht obselet durch Roadconnector?
     * 28.9.18:Zumindest bei Bridge Oberteilen nicht, denn die haben doch keine richtigen Connector?
     *
     * @return
     */
    public EleConnectorGroupSet getConnectedElevations() {
        EleConnectorGroupSet eleConnectorGroupSet = new EleConnectorGroupSet();
        List<SceneryWayObject> ways = getConnectedWays();
        for (SceneryWayObject way : ways) {
            // es kann ja nur immer entweder start oder eende connected sein
            EleConnectorGroup g = way.getEleConnectorGroups().getEleConnectorGroup(mapWay.getStartNode());
            if (g == null) {
                g = way.getEleConnectorGroups().getEleConnectorGroup(mapWay.getEndNode());
            }
            eleConnectorGroupSet.add(g);
        }
        return eleConnectorGroupSet;
    }

    /**
     * 5.9.18: Ist das nicht obselet durch Roadconnector?
     *
     * @return
     */
    public List<SceneryWayObject> getConnectedWays() {
        List<SceneryWayObject> l = new ArrayList<>();
        List<SceneryWayObject> ways = SceneryContext.getInstance().wayMap.get(ROAD, mapWay.getStartNode().getOsmId());
        l.addAll(ways);
        l.addAll(SceneryContext.getInstance().wayMap.get(ROAD, mapWay.getEndNode().getOsmId()));
        // 2 times!
        l.remove(this);
        l.remove(this);
        return l;
    }

    /**
     * Die Elevation zwischen erster und letzter Group interpolieren und in den mittleren Gropups eintragen.
     * 24.4.19: Mittlere Groups konnen ja auch schon eine Elevation haben. Darum stepbystep.
     */
    public void interpolateGroupElevations() {
        //EleConnectorGroupSet outer = getOuterEleConnectorGroups();//.getOuter();
        if (/*outer == null || */mapWay.getOsmId() == 8033747) {
            int h = 9;
        }
        List<EleConnectorGroup> empty = new ArrayList<>();

        List<EleConnectorGroup> egrs = getEleConnectorGroups().eleconnectorgroups;
        EleConnectorGroup lastgr = egrs.get(0);
        if (lastgr.getElevation() == null) {
            logger.error("no elevation in getFirst group");
            return;
        }
        if (egrs.get(egrs.size() - 1).getElevation() == null) {
            logger.error("no elevation in last group");
            return;
        }
        for (int i = 1; i < egrs.size(); i++) {
            EleConnectorGroup egr = egrs.get(i);
            if (egr.getElevation() == null) {
                empty.add(egr);
            } else {
                //erstmal einfach nur durchschnitt. TODO interpolate
                double average = (lastgr.getElevation() + egr.getElevation()) / 2;

                for (EleConnectorGroup egr1 : empty) {
                    egr1.fixElevation(average);
                }
                lastgr = egr;
                empty.clear();
            }
        }
        for (EleConnectorGroup egr : egrs) {
            if (!egr.isFixed()) {
                logger.error("still unfixed group");
                return;
            }
        }
            /*for (int i = 0; i < outer.size(); i++) {
                else {
                    average += outer.get(i).getElevation();
                }
            }
        }
        average /= outer.size();
        //erstmal nur durchschnitt. , gridnode und intermediate beachten
        EleConnectorGroupSet all = getEleConnectorGroups();
        for (int i = 0; i < all.size(); i++) {
            EleConnectorGroup eg = all.get(i);
            if (!eg.isFixed()) {
                //logger.debug("Setting average "+average);
                eg.setElevation(average);
            }
        }*/
    }

    /**
     * Fuer jeden Polygonpunkt die Elevation eintragen. Fuer den Polygon kann das die Superklasse machen.
     * Hier dann noch fuer den Graph.
     */
    @Override
    public void calculateElevations(TerrainMesh tm) {
        if (mapWay.getOsmId() == 8033747) {
            int z = 99;
        }
        super.calculateElevations(tm);
        if (graphComponent != null) {
            for (int i = 0; i < effectiveNodes/*mapWay.getMapNodes()*/.size(); i++) {
                MapNode mapNode = effectiveNodes/*mapWay.getMapNodes()*/.get(i);
                GraphNode gn = graphComponent.findNodeFromOsmNode(mapNode);
                EleConnectorGroup eleConnectorGroup = getEleConnectorGroups().getEleConnectorGroup(mapNode);
                if (eleConnectorGroup == null) {
                    logger.error("eleConnectorGroup==null");
                    continue;
                }
                if (eleConnectorGroup.getElevation() == null) {
                    logger.error("eleConnectorGroup.getElevation()==null");
                    continue;
                }
                if (gn == null) {
                    logger.error("graph node ==null");
                    continue;
                }
                double elevation = eleConnectorGroup.getElevation();
                if (elevation < 20) {
                    int kiij = 99;
                }
                Vector3 v = new Vector3(gn.getLocation().getX(), gn.getLocation().getY(), elevation);
                gn.setLocationOnlyForSpecialPurposes(v);
            }
        }
    }

    public void raiseBridgeApproach(MapNode bridgenode) {
        //AreaElevation isType expected to exist
        EleConnectorGroup eleConnectorGroup = elevations.getEleConnectorGroup(bridgenode);
        if (eleConnectorGroup == null) {
            //kann passieren, wenn die bridgenode ausserhabl des grid liegt.
            logger.debug("bridge not raised due to missing elegroup for" + mapWay.getOsmId() + ". Bridge node " + bridgenode + " out of grid?");
            return;
        }
        eleConnectorGroup.setGroundState(GroundState.ABOVE);
            /*TODO Coordinate[] coor = poly.getCoordinatesForOsmNode(bridgenode.getOsmNode().id);
            coor[0].setOrdinate(Coordinate.Z, 8);
            coor[1].setOrdinate(Coordinate.Z, 8);*/
    }

    public List<SceneryWayObject> getConnectedRoads() {
        List<SceneryWayObject> l = HighwayModule.getConnectedWays(mapWay.getStartNode(), true);
        l.addAll(HighwayModule.getConnectedWays(mapWay.getEndNode(), true));
        return l;
    }

    public List<SceneryWayConnector> getInnerConnector() {
        return innerConnector;
        /*4.9.19 List<SceneryWayConnector> l = new ArrayList<>();
        for (int i = 1; i < effectiveNodes/*mapWay.getMapNodes()* /.size() - 1; i++) {
            MapNode mapNode = effectiveNodes/*mapWay.getMapNodes()* /.get(i);
            SceneryWayConnector connector = SceneryContext.getInstance().wayMap.getConnector(ROAD, mapNode.getOsmId());
            if (connector != null) {
                l.add(connector);

            }
        }
        return l;*/
    }

    public void setStartConnector(SceneryWayConnector startConnector) {
        if (mapWay.getOsmId() == 107468171 || mapWay.getOsmId() == 23696494) {
            int h = 9;
        }
        this.startConnector = startConnector;
        this.startMode = WayOuterMode.CONNECTOR;
        //this.startMode.wayConnector = startConnector;
    }

    public SceneryWayConnector getStartConnector() {
        return startConnector;
    }

    public void setEndConnector(SceneryWayConnector endConnector) {
        this.endConnector = endConnector;
        this.endMode = WayOuterMode.CONNECTOR;
        //this.endMode.wayConnector = endConnector;
    }

    public SceneryWayConnector getEndConnector() {
        if (endConnector == null) {
            int h = 9;
        }
        return endConnector;
    }

    public SceneryWayConnector getOppositeConnector(SceneryWayConnector connector) {
        if (connector == startConnector) {
            return endConnector;
        }
        if (connector == endConnector) {
            return startConnector;
        }
        logger.error("invalid usage");
        return null;
    }

    public boolean isOuterNode(MapNode node) {
        return node == mapWay.getStartNode() || node == mapWay.getEndNode();
    }

    /**
     * TODO: 26.7.19 soll der nicht das Grid beachten?
     *
     * @param node
     * @return
     */
    public boolean isStartNode(MapNode node) {
        return node == mapWay.getStartNode();
    }

    public boolean isEndNode(MapNode node) {
        return node == mapWay.getEndNode();
    }

    public MapNode getStartNode() {
        return mapWay.getMapNodes().get(startNode);
    }

    public MapNode getEndNode() {
        return mapWay.getMapNodes().get(endNode);
    }

    /**
     * Returns indexth cross line from start/end node.
     * Line coordinates will be from left to right looking from node always.
     * <p>
     * <p>
     * 13.8.19: Ways haben keine uncutcoord mehr. Umgestellt auf getPair.
     * 7.9.19: Da kann auch ein multiple pair kommen. Da muss der Aufrufer dann mal sehen.
     * 27.3.24: TerrainMesh not available??!!
     */
    public CoordinatePair[] getPairRelatedFromNode(MapNode node, int index/*, TerrainMesh tm*/) {
        TerrainMesh tm = null;
        CoordinatePair[] pair = null;
        //bei Connector muss evtl multiple genutzt werden? Hmm, etwas unklar im Moment.
        //7.9.19: Ja, das ist wichtig an manchen Connector.
        if (node == getStartNode()) {
            pair = getWayArea().getMultiplePair(index);
        } else {
            if (node == getEndNode()) {
                pair = getWayArea().getMultiplePair(getWayArea().getLength() - 1 - index);
                //die order der pairs tauschen, damit sie aus Sicht von "node" sind.
                switch (pair.length) {
                    case 1:
                        break;
                    case 2:
                        CoordinatePair p = pair[0];
                        pair[0] = pair[1];
                        pair[1] = p;
                        break;
                    default:
                        logger.error("getPairRelatedFromNode: not yet");
                }
            }
        }
        if (pair == null) {
            int h = 9;
            logger.error("no pair in " + this.toString());
            pair = getWayArea().getMultiplePair(0);
        }
        return pair;//new LineSegment(pair.left(), pair.right());
    }


    /*public boolean isStandard() {
        if (standardway==false){
            //there isType no return from no standard
            return  false;
        }
        if (flatComponent.poly.uncutPolygon.getCoordinates().length % 2 == 0) {
            logger.debug("way " + getOsmIdsAsString() + " never was standard with " + flatComponent.poly.uncutPolygon.getCoordinates().length + "uncut coordinates");
            standardway = false;
        }
        if (flatComponent.poly.polygon.length != 1) {
            logger.debug("multiple polygons. no standard.");
            standardway = false;
        } else {
            if (flatComponent.poly.polygon[0].getCoordinates().length == 0) {
                // vielleicht beim cut ganz rausgefallen.
                logger.debug("way " + getOsmIdsAsString() + " isType no standard with " + flatComponent.poly.polygon[0].getCoordinates().length + "coordinates");
                standardway = false;
                flatComponent = AbstractArea.EMPTYAREA;
            } else {
                if (flatComponent.poly.polygon[0].getCoordinates().length % 2 == 0) {
                    logger.debug("way " + getOsmIdsAsString() + " isType no standard with " + flatComponent.poly.polygon[0].getCoordinates().length + "coordinates");
                    standardway = false;
                }
            }
        }
        if (!standardway && !flatComponent.isEmpty()) {
            if (flatComponent.material.getName().equals(Materials.ROAD.getName())) {
                //ASPHALT hat kein STRIP_FIT
                logger.debug("switching from material ROAD to ASPHALT");
                flatComponent.material = ASPHALT;
            }
        }
        return standardway;
    }*/

    public GraphComponent getGraphComponent() {
        return graphComponent;
    }

    public List<Vector2> getCenterLine() {
        return graphComponent.getCenterLine();
    }

    public WayArea getWayArea() {
        //Bewusst kein Check? Bei Exception stimmt was im workflow nicht.
        //Doch check, denn es kann Ways ohne WayArea geben.
        if (!(flatComponent[0] instanceof WayArea)) {
            return null;
        }
        return (WayArea) flatComponent[0];
    }

    /**
     * Doesn't change the way itself. Returns pair of node moved by offset (positive in end direction, negative in start direction)
     *
     * @param node
     * @param v
     * @return
     */
    public CoordinatePair shiftStartOrEnd(MapNode node, double v) {
        if (!(flatComponent[0] instanceof WayArea)) {
            logger.error("reduce: area isType no way: " + mapWay.getOsmId());
            return null;
        }
        WayArea wa = (WayArea) flatComponent[0];
        if (isStartNode(node)) {
            return wa.shiftStart(v);
        }
        if (isEndNode(node)) {
            return wa.shiftEnd(-v);
        }
        logger.error("neither start nor end node");
        return null;
    }

    public boolean isClosed() {
        return getStartNode() == getEndNode();
    }

    /**
     * Convert a CoordinatePair from "start" orientation to orientation from "node", which must be either start or end.
     */
    public CoordinatePair toNodeOrientation(MapNode node, CoordinatePair coordinatePair) {
        if (isEndNode(node)) {
            return coordinatePair.swap();
        } else {
            if (isStartNode(node)) {
                return coordinatePair;
            }
        }
        logger.error("neither start nor end");
        return null;
    }

    public static enum WayOuterMode {
        GRIDBOUNDARY, CONNECTOR, DEADEND;
        //Fatal public SceneryWayConnector wayConnector;
        //Fatal public int gridboundarynode = -1;
    }

    @Override
    protected List<VectorXZ> getPois() {
        List<VectorXZ> l = new ArrayList();
        l.add(mapWay.getStartNode().getPos());
        return l;
    }

    @Override
    public boolean isPartOfMesh(TerrainMesh tm) {
        if (!(getArea()[0] instanceof WayArea)) {
            return false;
        }
        return getWayArea().getLeftLines(tm) != null && getWayArea().getRightLines(tm) != null;
    }
}
