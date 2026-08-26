package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.*;
import de.yard.threed.osm2graph.osm.*;
import de.yard.threed.osm2scenery.MeshServiceFacade;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.TerrainMeshAdder;
import de.yard.threed.osm2scenery.scenery.components.WayArea;
import de.yard.threed.osm2scenery.scenery.components.WayTerrainMeshAdder;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import de.yard.threed.osm2scenery.util.SvgWriter;
import de.yard.threed.osm2world.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.yard.threed.osm2graph.osm.JtsUtil.toCoordinate;
import static de.yard.threed.osm2graph.osm.OsmUtil.toVector2;


/**
 * Wenn nicht anders angegeben, gilt immer die "main0perspective", d.h. alle relative Angaben (links,rechts, getFirst,getSecond im Pair) sind aus
 * Blickrichtung main0, unahbhaengig von dessen start/end.
 * <p>
 * 14.6.19: RoadConnector und Junction vereint in abstrakten WayConnector, der auch eine Flaeche hat.
 * 10.7.19: Bei innerConnector hat er aber nicht (unbedingt) eine Flaeche.
 * 18.7.19: For all ways, also railway, River, etc.
 * 27.2.26: States are
 * "initial": ways can be added
 * "classified":
 * "polygoned":
 * But no "persisted": Stored to DB, thus readonly. We have  MeshWayConnector
 * A 'lake' is also considered a way (river) connector? Maybe better not, there might be standalone lakes that could get lost.
 */
@Slf4j
public class SceneryWayConnector extends SceneryNodeObject {
    private WayConnectorType type = null;
    // 'major' have 'Right Of Way' while 'minor' have to give way (without traffic lights).
    // majors and minors are (always?) opposite each. 'major0's osmid <= 'major1's osmid.
    // 'minorway0' is always CCW between major0 and major1.
    // 'minorway1' is always CCW between major1 and major0.
    public int minorway0 = -1, majorway0 = -1, majorway1 = -1, minorway1 = -1;
    private List<MapWaySegment2/*MapWay/*19.2.26 SceneryWayObject*/> ways = new ArrayList<>();
    //pair contains indexes to polygon nodes. Die Orientierung in attachpair ist passend für den jeweiligen Way
    private Map<MapWaySegmentAtConnector, /*.3.26 CoordinatePair*/Pair<Integer, Integer>> attachpair = null;//27.2.26 new HashMap();
    private boolean broken = false;
    public CoordinatePair additionalmain0pair = null;

    //6.3.26 assume no longer needed CoordinatePair closingpair = null;
    // CCW angles of ways related to way[0]. So angles[0] is always 0.0.
    // 11.8.26: Better not, because this leads to non determinitic attach pair indices. So these are the real angles of the ways
    public double[] angles;
    // sorted CCW from heading north.
    public int[] angleorder;
    public boolean isCrossing;
    // 21.2.26 Not the final real areas but just candidates for building the connector. Way later needs to attach.
    // 20.3.26: As of MapWaySegment2 the way osmid as key is no longer sufficient
    Map<MapWaySegment2, WayArea> wayAreaCandidates = new HashMap<>();
    // set by createPolygon. 2026 approach for polygon? "second" is the osm id. Of what and for what?
    private List<Pair<GeoCoordinate, Long>> polygonLine = null;
    //24.8.26 public MeshPolygon meshWayConnector = null;

    public SceneryWayConnector(String creatortag, MapNode node, ConfMaterial material, Category category, SceneryProjection projection) {
        super(creatortag, node, material, category, projection);
        this.cycle = Cycle.WAY;
        if (node.getOsmId() == 295055704 || node.getOsmId() == 1379039502) {
            int h = 9;
        }

    }

    /**
     * Create from DB
     */
   /*better use MeshWayConnector public SceneryWayConnector(MeshPolygon meshPolygon) {
        super("creatortag", null, null, null);
        this.meshPolygon = meshPolygon;

        attachpair=
    }*/
    public void classify() throws MeshInconsistencyException {
        if (node.getOsmId() == 295055704 || node.getOsmId() == 1379039502) {
            int h = 9;
        }

        // Assume all connected ways start/end at this connector. 19.3.26 should always be true after segments2, so abort if not
        //17.4.26 boolean allouternodes = true;
        List<Integer> outerNodeWays = new ArrayList<>();
        List<Integer> innerNodeWays = new ArrayList<>();
        for (int i = 0; i < ways.size(); i++) {
            MapWaySegment2/*SceneryWayObject*/ way = ways.get(i);
            if (!way.isOuterNode(node)) {
                //17.4.26 allouternodes = false;
                if (true) throw new MeshInconsistencyException("should no longer have mid nodes");
                innerNodeWays.add(i);
            } else {
                outerNodeWays.add(i);
            }
        }
        if (angles == null/*17.4.26  && allouternodes*/) {
            calcAngles();
        }

        //ob das immer eine inner node ist? Und es koennen Fusswege connected sein, z.B. 388796251
        //auf jeden Fall betrachte ich CROSSING nur bis maximal zwei Ways. Nee, wegen z.B. 388796251, da kreuzt doch ein Fussweg.
        //Eigentlich ist das eher ein zusätzliches Attribut als ein eigenständiger Connectortype. An 54289952 kommt einfach ein Radweg(?) auf eine Strasse mit Fussgaengerampel.
        if (/*(ways.size() == 1 || (ways.size() == 2 && allouternodes)) && */node.getTags().contains("highway", "crossing") && !node.getTags().contains("crossing", "no")) {
            /*type = WayConnectorType.CROSSING;
            majorway0 = 0;
            if (ways.size() > 1) {
                majorway1 = 1;
            }
            return;*/
            isCrossing = true;
        }

        if (ways.size() == 1) {
            //closed way?
            if (/*21.2.26 ?? ways.get(0).isClosed() &&*/ ways.get(0).isOuterNode(node)) {
                //das muesste doch gehen.
                type = WayConnectorType.SIMPLE_CONNECTOR;
                majorway0 = 0;
                majorway1 = 0;
                return;
            }
        }
        if (ways.size() == 2) {
            if (ways.get(0).isOuterNode(node) && !ways.get(1).isOuterNode(node)) {
                //19.3.26 type = WayConnectorType.SIMPLE_INNER_SINGLE_JUNCTION;
                if (true) throw new MeshInconsistencyException("no longer SIMPLE_INNER_SINGLE_JUNCTION");
                minorway0 = 0;
                majorway0 = 1;

                return;
            }
            if (!ways.get(0).isOuterNode(node) && ways.get(1).isOuterNode(node)) {
                //19.3.26 type = WayConnectorType.SIMPLE_INNER_SINGLE_JUNCTION;
                if (true) throw new MeshInconsistencyException("no longer SIMPLE_INNER_SINGLE_JUNCTION");
                minorway0 = 1;
                majorway0 = 0;
                return;
            }
            /*17.4.26 if (allouternodes) */
            {
                type = WayConnectorType.SIMPLE_CONNECTOR;
                if (mainOrderFits(new int[]{0, 1})) {
                    majorway0 = 0;
                    majorway1 = 1;
                } else {
                    majorway0 = 1;
                    majorway1 = 0;
                }
                return;
            }
        }
        if (ways.size() == 3) {
            if (outerNodeWays.size() == 2 && innerNodeWays.size() == 1) {
                //TODO die minor muessen gegenüber liegen.
                //19.3.26 type = WayConnectorType.SIMPLE_INNER_DOUBLE_JUNCTION;
                if (true) throw new MeshInconsistencyException("no longer SIMPLE_INNER_SINGLE_JUNCTION");
                majorway0 = innerNodeWays.get(0);
                minorway0 = outerNodeWays.get(0);
                minorway1 = outerNodeWays.get(1);
                return;
            }
        }
        /*17.4.26 if (allouternodes) {*/
        if (checkForSTANDARD_TRI_JUNCTION()) {
            return;
        }
        if (checkForSIMPLE_JUNCTION()) {
            return;
        }
        if (checkForMOTORWAY_ENTRY_JUNCTION()) {
            return;
        }
        /*17.4.26 }
        if (allouternodes) {*/
        // all ways start/end here, but we have no predefined pattern for more than 3 ways. So generic.
        type = WayConnectorType.GENERIC;
        return;
        /*17.4.26 }
        log.warn("cannot classify connector at node " + node.getOsmNode().id);*/
    }

    /**
     * 3 Ways, von denen zwei quasi in Reihe und einer im quasi rechten Winkel abgeht, der aber keine "minor Way" ist (Mittelline haben soll).
     * Sonst macht die ConnectorArea keine Sinn (zukünftig).
     * <p>
     * Return true if type STANDARD_TRI_JUNCTION was detected.
     *
     * @return
     */
    private boolean checkForSTANDARD_TRI_JUNCTION() throws MeshInconsistencyException {
        if (ways.size() != 3) {
            return false;
        }
        if (node.getOsmId() == 54286220) {
            int h = 9;
        }

        Degree[] angles = new Degree[]{new Degree(180), new Degree(90)};
        return runPermutated(3, (order) -> {
            //for (int[] order : orders) {
            if (compliesPattern(order, angles, new double[]{0.1, 0.1})) {
                if (!(HighwayModule.isMinorLink(ways.get(order[2]).getTags()))) {
                    // have it independent from way adding order
                    if (mainOrderFits(order)) {
                        type = WayConnectorType.STANDARD_JUNCTION;
                        majorway0 = order[0];
                        majorway1 = order[1];
                        //setMinor(order, 2);
                        setMinor();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private boolean runPermutated(int size, PermutationHandler permutationHandler) throws MeshInconsistencyException {
        int[][] orders = Util.buildPermutation(size);
        if (orders == null) {
            log.error("no permutation");
            return false;
        }
        for (int[] order : orders) {
            if (permutationHandler.run(order)) {
                return true;
            }
        }
        return false;
    }

    /**
     * limit in rad!
     */
    private boolean compliesPattern(int order[], Degree[] angles, double[] limit) {
        if (limit.length != angles.length || order.length != angles.length + 1 || angles.length + 1 != ways.size() || (ways.size() != 3 && ways.size() != 4)) {
            log.error("invalid usage");
            return false;
        }
        if (node.getOsmId() == 255563538) {
            int h = 9;
        }

        Vector2 dir0 = OsmUtil.getDirectionToNode(ways.get(order[0]), node);
        Vector2 dir1 = OsmUtil.getDirectionFromNode(ways.get(order[1]), node);
        Vector2 dir2 = OsmUtil.getDirectionFromNode(ways.get(order[2]), node);
        Vector2 dir3 = null;
        if (order.length == 4) {
            dir3 = OsmUtil.getDirectionFromNode(ways.get(order[3]), node);
        }

        // Einen Probe Vektor entsprechend Pattern rotieren und prüfen, ob er in Deckung ist mit den anderen Dirs.
        // der probe vector wird immer weiter rotiert, nicht jeweils wieder von vorne!
        Vector2 probeDir = dir0.negate();
        probeDir = probeDir.rotate(angles[0]);
        if (Vector2.getAngleBetween(probeDir, dir1) > limit[0]) {
            return false;
        }
        probeDir = probeDir.rotate(angles[1]);
        if (Vector2.getAngleBetween(probeDir, dir2) > limit[1]) {
            return false;
        }
        if (dir3 != null) {
            probeDir = probeDir.rotate(angles[2]);
            if (Vector2.getAngleBetween(probeDir, dir3) > limit[2]) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@link WayConnectorType}
     * <p>
     * Return true if type SIMPLE_JUNCTION was detected.
     *
     * @return
     */
    private boolean checkForSIMPLE_JUNCTION() throws MeshInconsistencyException {
        int permutations;
        switch (ways.size()) {
            case 3:
                permutations = 3;
                break;
            case 4:
                permutations = 4;
                break;
            default:
                return false;
        }
        if (node.getOsmId() == 2345486462L) {
            int h = 9;
        }

        return runPermutated(permutations, (order) -> {
            // a link of a minor with two main is a match. TODO check second minor
            //  11.5.26 covered by below if??
            /*if ((HighwayModule.isLink(ways.get(order[2]).getTags()))) {
                type = WayConnectorType.SIMPLE_JUNCTION;
                majorway0 = order[0];
                majorway1 = order[1];
                minorway0 = order[2];
                return true;
            }*/
            if (HighwayModule.isAtLeastTertiary(ways.get(order[0]).getTags()) &&
                    HighwayModule.isAtLeastTertiary(ways.get(order[1]).getTags()) &&
                    // have it independent from way adding order
                    mainOrderFits(order) &&
                    minorCandidatesFitForSimpleJunction(order)) {
                type = WayConnectorType.SIMPLE_JUNCTION;
                majorway0 = order[0];
                majorway1 = order[1];
                //if (permutations == 3) {
                //setMinor(order, 2);
                setMinor();
                //}
                /*if (permutations == 4) {
                    //11.8.26 minorway1 = order[3];
                    setMinor(order, 3);
                }*/
                return true;
            }
            return false;
        });

    }

    private void setMinor() throws MeshInconsistencyException {
        int c=getCCwNext(majorway0);
        if (c!=majorway1){
            minorway0=c;
        }
        c=getCCwNext(majorway1);
        if (c!=majorway0){
            minorway1=c;
        }
    }

    private void setMinor(int[] order, int minorIndex) {
        if (angles[order[minorIndex]] >/*<*/ angles[order[1]]) {
            minorway0 = order[minorIndex];
        }
        if (angles[order[minorIndex]] </*>*/ angles[order[1]]) {
            minorway1 = order[minorIndex];
        }

    }

    private int getCCwNext(int wayindex) throws MeshInconsistencyException {
        for (int i=0;i<angleorder.length;i++){
            if (angleorder[i] == wayindex){
                int next = i+1;
                if (next >= angleorder.length){
                    next = 0;
                }
                return angleorder[next];
            }
        }
       throw new MeshInconsistencyException("");
    }

    private boolean mainOrderFits(int[] order) {
        if (ways.get(order[0]).getOsmId() == ways.get(order[1]).getOsmId()) {
            return ways.get(order[0]).segmentIndex <= ways.get(order[1]).segmentIndex;
        }
        return ways.get(order[0]).getOsmId() <= ways.get(order[1]).getOsmId();
    }

    private boolean minorCandidatesFitForSimpleJunction(int[] order) {

        // for now minor should be tracks or links...
        if (!minorCandidateFitsForSimpleJunction(order[2])) {
            return false;
        }
        if (!(order.length == 3 || minorCandidateFitsForSimpleJunction(order[3]))) {
            return false;
        }
        // ... and first minor should be CCW closest to major0
        /*11.8.26 don't check angles here but later if (order.length == 4) {
            if (angles[order[2]] > angles[order[3]]) {
                return false;
            }
        }*/
        return true;
    }

    private boolean minorCandidateFitsForSimpleJunction(int index) {

        // for now minor should be tracks or links...
        if (HighwayModule.isTrack(ways.get(index).getTags())) {
            return true;
        }
        if (HighwayModule.isLink(ways.get(index).getTags())) {
            return true;
        }
        return false;
    }

    /**
     * {@link WayConnectorType}
     * <p>
     * Return true if type MOTORWAY_ENTRY_JUNCTION was detected.
     *
     * @return
     */
    private boolean checkForMOTORWAY_ENTRY_JUNCTION() throws MeshInconsistencyException {
        if (ways.size() != 4) {
            return false;
        }

        if (node.getOsmId() == 1353883890L) {
            int h = 9;
        }
        if (true) {
            //return false;
        }
        //Ueber Winkel erkennen. Hmmm??
        Degree[] angles = new Degree[]{new Degree(-180), new Degree(-60), new Degree(-60)};
        return runPermutated(4, (order) -> {
            //for (int[] order : orders) {
            if (compliesPattern(order, angles, new double[]{0.1, 0.3, 0.3})) {
                if (HighwayModule.isMinorLink(ways.get(order[2]).getTags()) &&
                        HighwayModule.isMinorLink(ways.get(order[3]).getTags())) {

                    type = WayConnectorType.MOTORWAY_ENTRY_JUNCTION;
                    majorway0 = order[0];
                    majorway1 = order[1];
                    minorway0 = order[2];
                    minorway1 = order[3];
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Might be called multiple times for the same way.
     * 24.8.26: Really? Anyway it is checked.
     *
     * @param way
     */
    public void add(MapWaySegment2 way) throws MeshInconsistencyException {
        if (!ways.contains(way)) {
            ways.add(way);
            // TODO no hard coded width
            double candidateWidth = 8;
            wayAreaCandidates.put(way,
                    (WayArea) WayArea.buildOutlinePolygonFromCenterLine(way.getCenterline(), way.getMapNodes(), candidateWidth, null, null, way));
        }
    }

    /**
     * 27.2.26: This method now is also the state change to "polygoned".
     * Also contains core logic for connector building.
     * Populates "polygonline" and "attachpair"
     */
    @Override
    public List<ScenerySupplementAreaObject> createPolygon(MeshServiceFacade meshServiceFacade) throws MeshInconsistencyException {
        if (attachpair != null) {
            throw new RuntimeException("already polygoned");
        }
        attachpair = new HashMap<>();

        if (node.getOsmId() == 2345486254L || node.getOsmId() == 295055704) {
            int h = 9;
        }
        //Der Default ist empty.
        flatComponent = new AbstractArea[]{AbstractArea.EMPTYAREA};
        for (MapWaySegment2/*SceneryWayObject*/ way : ways) {
            /*21.2.26 TODO neede?? if (!(way.getArea()[0] instanceof WayArea)) {
                broken = true;
                log.error("Connector " + node.getOsmId() + " isType broken");
                minorway = -1;
                majorway0 = -1;
                majorway1 = -1;
                return null;
            }*/
        }
        if (type == null) {
            log.warn("unclassified connector " + node.getOsmId());
            return null;
        }
        if (node.getOsmId() == 2345486254L) {
            int h = 9;
        }
        polygonLine = new ArrayList<Pair<GeoCoordinate, Long>>();

        switch (type) {
            /*19.3.26 no longer mid connector  case SIMPLE_INNER_SINGLE_JUNCTION:
                //a minor way hitting a main way on an inner node.
                //Uses an additional coordinate added to main.
                //Connector bleibt empty, weil der main way die Darstellung macht. Braucht aber clip in junction.
                buildMinorAttachAtInnerMain(minorway, tm);
                break;*/
             /*19.3.26 no longer mid connector case SIMPLE_INNER_DOUBLE_JUNCTION:
                buildMinorAttachAtInnerMain(minorway, tm);
                buildMinorAttachAtInnerMain(secondminor, tm);
                break;*/
            case STANDARD_JUNCTION:
                createPolygonSTANDARD_JUNCTION(projection);
                break;
            case MOTORWAY_ENTRY_JUNCTION:
                createPolygonSTANDARD_JUNCTION(projection);
                break;
            case SIMPLE_CONNECTOR:
                buildSimpleMainConnection(projection);
                break;
            /*case CROSSING:
                //erstmal einfach so. Bei inner node erstmal nichts. Na, das koennen auch vier Ways sein (388796251).
                if (ways.size() == 2) {
                    buildSimpleMainConnection();
                }
                break;*/
            case SIMPLE_JUNCTION:
                //schwieriger als gedacht. Es muessen ja zwei Coordinates für den attach her. Darum
                //bekommt einer der beiden main ways noch ein Zusatzpair.
                //21.3.26: Why did we ever use buildSimpleMainConnection here? it has no minor. Anyhow,
                // avoid any special solution until we are sure it is a real benefit
                //CoordinatePair c = buildSimpleMainConnection();
                //buildSimpleMinorAttach(c, tm);
                createPolygonSimpleJunction(projection);
                break;
            case GENERIC:
                // just n ways, no major or minor.
                buildGenericConnection(projection);
                break;
            default:
                log.warn("unknown connector type " + type);
        }
        return null;
    }

    public List<Pair<GeoCoordinate, Long>> getPolygonLine() {
        return polygonLine;
    }

    /**
     * Die beiden mains(s) quasi verbinden. Der Connector selber bekommt damit keine Area.
     * Returns connectionPair in "main0 perspective".
     * major0 might equal major1 for closed ways.
     * 21.3.26: Will lead to a connector without polygon? Why at all should this be a connector.
     * 21.3.26 We no longer consider this a real connector. So no polygon is created only an attach par?
     * On the other hand, it is just easier to have a real connector. And nobody knows whether one more way will be added later.
     * So make a polygon like in createPolygonSTANDARD_TRI_JUNCTION()
     */
    private void /*21.2.26 CoordinatePair*/ buildSimpleMainConnection(SceneryProjection projection) throws MeshInconsistencyException {
        double width = 8;//TODO ways.get(0).getWidth();
        MapWaySegment2/*SceneryWayObject*/ main0 = ways.get(majorway0);
        MapWaySegment2/*SceneryWayObject*/ main1 = ways.get(majorway1);
        /*21.3.26 Vector2 dir0 = OsmUtil.getDirectionToNode(main0, node);
        //negate geht zumindest nicht bei closed.
        //Vector2 dir1 = OsmUtil.getDirectionToNode(main1.mapWay, node).negate();
        Vector2 dir1 = OsmUtil.getDirectionFromNode(main1, node);
        Coordinate pleft = toCoordinate(OutlineBuilder.getOutlinePointFromDirections(dir0, toVector2(node.getPos()), dir1, -width / 2));
        Coordinate pright = toCoordinate(OutlineBuilder.getOutlinePointFromDirections(dir0, toVector2(node.getPos()), dir1, width / 2));
        */

        polygonLine = createConnectorRectangle(node,
                wayAreaCandidates.get(ways.get(majorway0)),
                wayAreaCandidates.get(ways.get(majorway1)),
                projection);
        attachpair.put(main0.getKey(isStartNode(node, main0)), new Pair(0, 1)/*isStartNode(node, main0) ? new Pair(0, 1) : new Pair(1, 0)*/);
        attachpair.put(main1.getKey(isStartNode(node, main1)), new Pair(2, 3)/*isStartNode(node, main1) ? new Pair(2, 3) : new Pair(3, 2)*/);

        /*9.3.26 if (main0.isStartNode(node)) {
            attachpair.put(main0, new CoordinatePair(pleft, pright));
        } else {
            attachpair.put(main0, new CoordinatePair(pright, pleft));
        }
        if (main1.isStartNode(node)) {
            attachpair.put(main1, new CoordinatePair(pright, pleft));
        } else {
            attachpair.put(main1, new CoordinatePair(pleft, pright));
        }
        return attachpair.get(main0);//new CoordinatePair(pright, pleft);*/
    }

    /**
     * Create a CCW rectangle connector polygon between the two mains.
     * Points 0 and 1 will be at main0 while 2 and 3 are at main1.
     */
    private static List<Pair<GeoCoordinate, Long>> createConnectorRectangle(MapNode node, WayArea mainWayArea0, WayArea mainWayArea1, SceneryProjection projection) throws MeshInconsistencyException {

        CoordinatePair major0line = mainWayArea0.shiftStartOrEnd(node, 5.5);
        CoordinatePair major1line = mainWayArea1.shiftStartOrEnd(node, 5.5);
        if (major0line == null || major1line == null) {
            throw new MeshInconsistencyException("no major lines?");
        }
        List<Pair<GeoCoordinate, Long>> polygonLine = new ArrayList<Pair<GeoCoordinate, Long>>();

        // Polygon will be CCW. Vertices 0 and 1 are always on main0.
        if (mainWayArea0.mapWay.isEndNode(node)) {
            polygonLine.add(new Pair<>(JtsUtil.unproject(major0line.left(), projection), mainWayArea0.mapWay.getOsmId()));
            polygonLine.add(new Pair<>(JtsUtil.unproject(major0line.right(), projection), mainWayArea0.mapWay.getOsmId()));
            //       attachpair.put(main0.getOsmId(), new Pair(1, 0));
        } else {
            if (mainWayArea0.mapWay.isStartNode(node)) {
                polygonLine.add(new Pair<>(JtsUtil.unproject(major0line.right(), projection), mainWayArea0.mapWay.getOsmId()));
                polygonLine.add(new Pair<>(JtsUtil.unproject(major0line.left(), projection), mainWayArea0.mapWay.getOsmId()));
                ////    attachpair.put(main0.getOsmId(), new Pair(0, 1));
            }
        }

        if (mainWayArea1.isEndNode(node)) {
            polygonLine.add(new Pair<>(JtsUtil.unproject(major1line.left(), projection), mainWayArea0.mapWay.getOsmId()));
            polygonLine.add(new Pair<>(JtsUtil.unproject(major1line.right(), projection), mainWayArea1.mapWay.getOsmId()));
            //attachpair.put(main1.getOsmId(), new Pair(3, 2));
        } else {
            if (mainWayArea1.isStartNode(node)) {
                polygonLine.add(new Pair<>(JtsUtil.unproject(major1line.right(), projection), mainWayArea0.mapWay.getOsmId()));
                polygonLine.add(new Pair<>(JtsUtil.unproject(major1line.left(), projection), mainWayArea1.mapWay.getOsmId()));
            }
        }

        //close polygon
        polygonLine.add(polygonLine.get(0));

        // be sure the polygon is consistent
        Polygon geo = JtsUtil.createPolygon(polygonLine, projection);
        if (geo == null || !geo.isValid()) {
            throw MeshInconsistencyException.forInvalidPolygon("invalid connector polygon", node.getOsmId(), geo);
        }
        return polygonLine;
    }

    /**
     * Es muessen ja zwei Coordinates für den attach her. Darum
     * bekommt main0 (besser der main mit kleinerem winkel TODO) ein Zusatzpair.
     * Das kann man aber nicht hier machen, sondern nur im clip des Way.
     * mainConnection isType in major0 orientation
     * 21.3.26: avoid any special solution until we are sure it is a real benefit
     *
     * @param mainConnection (in main0perspective)
     */
    private void buildSimpleMinorAttach(CoordinatePair mainConnection, TerrainMesh tm) {
        MapWaySegment2/*SceneryWayObject*/ main0 = ways.get(majorway0), minor = null;//11.5.26  ways.get(minorway);
        WayArea mainWayArea0 = wayAreaCandidates.get(main0);

        additionalmain0pair = mainWayArea0.shiftStartOrEnd(node, 8.5);
        CoordinatePair p = additionalmain0pair;//main0.toNodeOrientation(node,additionalmain0pair);
        Util.notyet();
        /*9.3.26 if (minorHitsLeft(minorway/*, tm* /)) {
            if (main0.isStartNode(node)) {
                //OK
                buildAttachPair(p.left(), mainConnection.left(), minorway);
                //buildMinorAttachPair(mainConnection.left(), p.left(), minorway);
            } else {
                //OK
                //buildMinorAttachPair(p.left(), mainConnection.left(), minorway);
                buildAttachPair(mainConnection.left(), p.left(), minorway);
            }
        } else {
            if (main0.isStartNode(node)) {
                //buildMinorAttachPair(p.right(), mainConnection.right(), minorway);
                buildAttachPair(mainConnection.right(), p.right(), minorway);
            } else {
                //buildMinorAttachPair(mainConnection.right(), p.right(), minorway);
                buildAttachPair(p.right(), mainConnection.right(), minorway);
            }
        }*/
    }

    /**
     * A n-line polygon. For connector with more than 3 ways.
     */
    private void buildGenericConnection(SceneryProjection projection) throws MeshInconsistencyException {
        if (node.getOsmId() == 335476635) {
            int h = 9;
        }
        double defaultWidth = 5;
        double defaultDistance = 8;
        LineSegment[] segs = new LineSegment[ways.size()];

        //int cntr = 0;
        // do a probing with decreasing width until we have a valid polygon
        Polygon geo;
        do {
            //13.3.26 CoordinateList coors = new CoordinateList();
            polygonLine = new ArrayList<Pair<GeoCoordinate, Long>>();

            // Segments must be CCW. Each way gets its own two nodes, so a 4way cooector will have 8 nodes
            // 11.8.26 angle order also is CCW.
            for (int i = 0; i < ways.size(); i++) {
                int wayIndex = angleorder[i];
                MapWaySegment2/*SceneryWayObject*/ way = ways.get(wayIndex);
                Vector2 n = toVector2(node.getPos());
                Vector2 dir = OsmUtil.getDirectionFromNode(way, node);
                Vector2 nrm = dir.rightNormal().multiply(defaultWidth);
                Vector2 center = n.add(dir.multiply(defaultDistance));
                segs[i] = new LineSegment(toCoordinate(center.add(nrm)), toCoordinate(center.add(nrm.negate())));
                //13.3.26 coors.add(segs[i].p0);
                //13.3.26 coors.add(segs[i].p1);
                polygonLine.add(new Pair<>(JtsUtil.unproject(segs[i].p0, projection), way.getOsmId()));
                polygonLine.add(new Pair<>(JtsUtil.unproject(segs[i].p1, projection), way.getOsmId()));

                // refer to coords for attachpoints
                int upperIndex = polygonLine.size() - 1;
                buildAttachPair(/*12.3.26 segs[i].p0, segs[i].p1*/ upperIndex - 1, upperIndex, wayIndex);
            }
            polygonLine.add(polygonLine.get(0));
            geo = JtsUtil.createPolygon(polygonLine, projection);
            if (geo == null || !geo.isValid()) {
                log.warn("invalid polygon with width " + defaultWidth);
            } else {
                //flatComponent = new AbstractArea[]{new Area(geo, Materials.ROAD)};
                return;
            }
            defaultWidth -= 0.5;
        }
        while (defaultWidth > 0);

        if (geo != null) {
            // svg writing doesn't work for now
            SvgWriter svgWriter = new SvgWriter();
            svgWriter.addPolygon(geo, "red", SvgWriter.LabelMode.NODEBYINDEX);
            svgWriter.writeTmpFile("invalid-polygon");
        }

        //13.3.26 flatComponent = new AbstractArea[]{AbstractArea.EMPTYAREA};
        throw new MeshInconsistencyException("finally no polygon");

    }

    /**
     * 11.8.26: For deterministic attach pair indices angles are related to 'north'.
     */
    private void calcAngles/*sortByAngles*/() {
        Vector2[] dirs = new Vector2[ways.size()];
        angles = new double[ways.size()];

        for (int i = 0; i < ways.size(); i++) {
            MapWaySegment2/*SceneryWayObject*/ way = ways.get(i);
            Vector2 n = toVector2(node.getPos());
            dirs[i] = OsmUtil.getDirectionFromNode(way, node);
        }

        angles[0] = 0;
        List<Integer> angleOrder = new ArrayList<>();
        //11.8.26: Now from 'north'
        //angleOrder.add(0);
        for (int i = 0; i < ways.size(); i++) {
            double angle = Vector2.getRotationAngleBetween(dirs[i], new Vector2(0, 1)/*dirs[0]*/);
            angles[i] = angle;
            for (int j = 0; j <= angleOrder.size(); j++) {
                if (j == angleOrder.size()) {
                    angleOrder.add(i);
                    break;
                }
                if (angle /*11.8.26 have CCW order<*/ > angles[angleOrder.get(j)]) {
                    angleOrder.add(j, i);
                    break;
                }
            }
        }
        angleorder = new int[ways.size()];
        for (int i = 0; i < ways.size(); i++) {
            angleorder[i] = angleOrder.get(i);
        }
    }

    /**
     * Darf nur aufrgerufen, werden, wenn es einen minor way gibt
     * Relativ zu major0.
     * 10.6.26 how exactly is this defined?
     *
     * @return
     */
    public boolean minorHitsLeft(int mi/*, TerrainMesh tm*/) {
        MapWaySegment2/*SceneryWayObject*/ minor = ways.get(mi);
        if (minor == null) {
            throw new RuntimeException("no way " + mi);
        }
        MapWaySegment2/*SceneryWayObject*/ main0 = getMajor0();
        MapNode minorRefNode;
        if (minor.isEndNode(node)) {
            minorRefNode = minor.getMapNodes().get(minor.getMapNodes().size() - 2);
        } else {
            minorRefNode = minor.getMapNodes().get(1);
        }
        WayArea mainWayArea0 = wayAreaCandidates.get(getMajor0());
        Integer position = mainWayArea0.getPosition(node);
        CoordinatePair/*[]*/ connectorpair = mainWayArea0.getMultiplePair(position);
        /*19.3.26 if (connectorpair.length == 2) {
            // mit pair 0 oder 1??? Egal??
        }*/
        CoordinatePair refpair = connectorpair;
       /*3.9.19   if (main0.isEndNode(node)) {
            refpair = refpair.swap();
        }*/

        if (toVector2(minorRefNode.getPos()).distance(JtsUtil.toVector2(refpair.left())) <
                toVector2(minorRefNode.getPos()).distance(JtsUtil.toVector2(refpair.right()))) {
            return true;
        }
        return false;
    }

    /**
     * 10.7.19 Die uncut polies hier mergen. Simple, aber auch gut?
     * NeeNee, anhand Skizze 68 von den beiden Major was wegnehmen und minor dann anknüpfen.
     * 11.7.19: Sollte nur aufgerufen werden, wenn alles really WayAreas sind.
     * 02.09.2018: Auch für MOTORWAY_ENTRY_JUNCTION.
     * 18.4.26: A three/four way junction with constructional modifications
     */
    private void createPolygonSTANDARD_JUNCTION(SceneryProjection projection) throws MeshInconsistencyException {
        createPolygonJUNCTION(projection);
    }

    /**
     * 18.4.26: A three/four way junction without constructional modifications
     */
    private void createPolygonSimpleJunction(SceneryProjection projection) throws MeshInconsistencyException {
        createPolygonJUNCTION(projection);
    }

    /**
     * 18.4.26: A three/four way junction with or without constructional modifications
     * Results in a four node rectangle polygon.
     */
    private void createPolygonJUNCTION(SceneryProjection projection) throws MeshInconsistencyException {

        MapWaySegment2/*SceneryWayObject*/ main0 = getMajor0(), main1 = getMajor1();
        WayArea mainWayArea0 = wayAreaCandidates.get(main0);
        WayArea mainWayArea1 = wayAreaCandidates.get(main1);

        if (this.getOsmId() == 270353278) {
            int h = 9;
        }
        /*if (!(minor.getArea() instanceof WayArea)) {
            log.warn("no tri junction polygon. minor no way");
            return;
        }
        if (!(main0.getArea() instanceof WayArea)) {
            log.warn("no tri junction polygon. main0 no way");
            return;
        }
        if (!(main1.getArea() instanceof WayArea)) {
            log.warn("no tri junction polygon. main1 no way");
            return;
        }

        WayArea minorarea = (WayArea) minor.getArea();*/
        //minorarea.getLeftOutline()
        double offset = 5.5;
        if (minorway1 != -1) {
            offset = 9.5;
        }

        polygonLine = createConnectorRectangle(node,
                wayAreaCandidates.get(ways.get(majorway0)),
                wayAreaCandidates.get(ways.get(majorway1)),
                projection);
        // in Pair: first=right, second=left. 9.6.26: Now always from connector node
        attachpair.put(main0.getKey(isStartNode(node, main0)), new Pair(0, 1)/* isStartNode(node, main0) ? new Pair(0, 1) : new Pair(1, 0)*/);
        attachpair.put(main1.getKey(isStartNode(node, main1)), new Pair(2, 3)/* isStartNode(node, main1) ? new Pair(2, 3) : new Pair(3, 2)*/);

        /*CoordinatePair major0line = mainWayArea0.shiftStartOrEnd(node, 5.5);
        CoordinatePair major1line = mainWayArea1.shiftStartOrEnd(node, 5.5);
        if (major0line == null || major1line == null) {
            log.warn("no tri junction polygon");
            return;
        }
        //List<Coordinate> clist = new ArrayList<>();
        polygonLine = new ArrayList<Pair<GeoCoordinate, Long>>();

        // Polygon will be CCW?
        //CoordinatePair p0 = SceneryWayObject.toNodeOrientation(node, mainWayArea0.mapWay, major0line);
        if (mainWayArea0.mapWay.isEndNode(node)) {
            //return coordinatePair.swap();
            polygonLine.add(new Pair<>(OsmUtil.unproject(projection, major0line.left()), mainWayArea0.mapWay.getOsmId()));
            polygonLine.add(new Pair<>(OsmUtil.unproject(projection, major0line.right()), mainWayArea0.mapWay.getOsmId()));
            attachpair.put(main0.getOsmId(), new Pair(1, 0));

        } else {
            if (mainWayArea0.mapWay.isStartNode(node)) {
                polygonLine.add(new Pair<>(OsmUtil.unproject(projection, major0line.right()), mainWayArea0.mapWay.getOsmId()));
                polygonLine.add(new Pair<>(OsmUtil.unproject(projection, major0line.left()), mainWayArea0.mapWay.getOsmId()));
                attachpair.put(main0.getOsmId(), new Pair(0, 1));

            }
        }*/

        /*9.3.26 moved below attachpair.put(main1, major1line);
        CoordinatePair p1 = SceneryWayObject.toNodeOrientation(node, mainWayArea1.mapWay, major1line);*/

        if (minorway0 != -1) {
            // 9.6.26 swap 12 to 21? No
            buildAttachPair(/*p0.left(), p1.right()*/1, 2, minorway0);
        }
        if (minorway1 != -1) {
             /*9.3.26
            //Der Abstand der beiden minors mal 1.5 meter.
            CoordinatePair major0innerline = mainWayArea0.shiftStartOrEnd(node, 0.75);
            CoordinatePair major1innerline = mainWayArea1.shiftStartOrEnd(node, 0.75);
            CoordinatePair pi0 = SceneryWayObject.toNodeOrientation(node, mainWayArea0.mapWay, major0innerline);
            CoordinatePair pi1 = SceneryWayObject.toNodeOrientation(node, mainWayArea1.mapWay, major1innerline);
            polygonLine.add(new Pair<>(JtsUtil.unproject(pi0.left(), projection), mainWayArea0.mapWay.getOsmId()));
            polygonLine.add(new Pair<>(JtsUtil.unproject(pi1.right(), projection), mainWayArea1.mapWay.getOsmId()));
            //clist.add(pi0.left());
            //clist.add(pi1.right());
           buildAttachPair(p0.left(), pi0.left(), secondminor);
            buildAttachPair(pi1.right(), p1.right(), minorway);*/
            //9.6.26 swap 30 to 03. Hmm, no
            buildAttachPair(/*p0.left(), p1.right()*/3, 0, minorway1);
        } /*else {*/
        //TODO 23.3.26: possible flip?
        //TODO check way
        //CoordinatePair p1 = SceneryWayObject.toNodeOrientation(node, mainWayArea1.mapWay, major1line);
        //polygonLine.add(new Pair<>(OsmUtil.unproject(projection, p1.right()), mainWayArea0.mapWay.getOsmId()));
        //polygonLine.add(new Pair<>(OsmUtil.unproject(projection, p1.left()), mainWayArea1.mapWay.getOsmId()));
       /* if (mainWayArea1.isEndNode(node)) {
            polygonLine.add(new Pair<>(OsmUtil.unproject(projection, major1line.left()), mainWayArea0.mapWay.getOsmId()));
            polygonLine.add(new Pair<>(OsmUtil.unproject(projection, major1line.right()), mainWayArea1.mapWay.getOsmId()));
            attachpair.put(main1.getOsmId(), new Pair(3, 2));
        } else {
            if (mainWayArea1.isStartNode(node)) {
                polygonLine.add(new Pair<>(OsmUtil.unproject(projection, major1line.right()), mainWayArea0.mapWay.getOsmId()));
                polygonLine.add(new Pair<>(OsmUtil.unproject(projection, major1line.left()), mainWayArea1.mapWay.getOsmId()));
                attachpair.put(main1.getOsmId(), new Pair(2, 3));
            }
        }*/

        //clist.add(p1.right());
        //clist.add(p1.left());

        //close polygon
        /*polygonLine.add(polygonLine.get(0));

        // be sure the polygon is consistent
        Polygon geo = JtsUtil.createPolygon(polygonLine, projection);
        if (geo == null || !geo.isValid()) {
            throw MeshInconsistencyException.forInvalidPolygon("invalid connector polygon", osmIds.get(0), geo);
        }*/
       /* closingpair = new CoordinatePair(clist.get(0), clist.get(clist.size() - 2));
        flatComponent = new AbstractArea[]{new Area(geo, Materials.ROAD)};*/
    }

    private boolean isStartNode(MapNode node, MapWaySegment2 way) {
        return way.isStartNode(node);
    }

    /**
     *
     */
    /*19.3.26 no longer mid connector private void buildMinorAttachAtInnerMain(int mi, TerrainMesh tm) {
        MapWaySegment2/*SceneryWayObject* / main = getMajor0();
        WayArea mainWayArea0 = wayAreaCandidates.get(getMajor0().getOsmId());
        Integer position = mainWayArea0.getPosition(node);

        CoordinatePair[] connectorpair = mainWayArea0.getMultiplePair(position);
        if (connectorpair == null) {
            log.error("inconsistent?");
        } else {
            if (connectorpair.length == 2) {
                // Was heisst denn das? An der Node gibt es schon ein zweites Paar? Wo kommt denn das her? Das wurde schon im Way für die inner node angelegt.
                // links oder rechts?
                if (minorHitsLeft(mi/*, tm* /)) {
                    Util.notyet();
                    /*9.3.26  buildAttachPair(connectorpair[1].getSecond(), connectorpair[0].getSecond(), mi);*/

                            /*if (minor.isEndNode(node)) {
                                attachpair.put(minor.mapWay, new CoordinatePair(connectorpair[0].getSecond(), connectorpair[1].getSecond()));
                            } else {
                                attachpair.put(minor.mapWay, new CoordinatePair(connectorpair[1].getSecond(), connectorpair[0].getSecond()));
                            }* /
                } else {
                    Util.notyet();
                    /*9.3.26  buildAttachPair(connectorpair[0].getFirst(), connectorpair[1].getFirst(), mi);*/
                            /*if (minor.isEndNode(node)) {
                                attachpair.put(minor.mapWay, new CoordinatePair(connectorpair[1].getFirst(), connectorpair[0].getFirst()));
                            } else {
                                attachpair.put(minor.mapWay, new CoordinatePair(connectorpair[0].getFirst(), connectorpair[1].getFirst()));
                            }* /
                }
            } else {
                log.warn("no multiple pair at connector");
            }
        }
    }*/

    /**
     * Parameters related to start. 9.6.26: Now from connector
     */
    private void buildAttachPair(/*Coordinate*/Integer right, /*Coordinate*/Integer left, int wayindex) {
        MapWaySegment2/*SceneryWayObject*/ way = ways.get(wayindex);
        /*CoordinatePair*/
        Pair<Integer, Integer> minorattachpair;
        //9.6.26 if (way.isStartNode(node)) {
        minorattachpair = new Pair(right, left);
        /*} else {
            minorattachpair = new Pair(left, right);
        }*/
        attachpair.put(way.getKey(isStartNode(node, way))/*.mapWay*/, minorattachpair);
    }

    @Override
    public void clip() throws MeshInconsistencyException {
        super.clip();
    }

    /**
     * The 2026 way without TerrainMeshAdder. This now
     * is also the step for creating the polygon.
     *
     * @return
     */
    @Override
    public void addToTerrainMesh(TerrainMeshAdder terrainMeshAdder) throws OsmProcessException, MeshInconsistencyException {

        /**
         * TerrainMesh vorbereiten.
         * <p>
         * Vom Connector kommen die inner lines ins Mesh. Obwohl die eigentlich nicht gebraucht werden, nur für die Konsistenz.
         */
    /*26.2.26 @Override
    public void addToTerrainMesh(TerrainMesh tm) throws OsmProcessException, MeshInconsistencyException {
        super.addToTerrainMesh(tm);*/

        // flatcomponent might be null
        if (node.getOsmId() == 2345485946L) {
            int h = 9;
        }

        if (type == null) {
            throw new MeshInconsistencyException("unclassified connector " + node.getOsmId());
        }

        //10.3.26 now in cca() createPolygon(tm.getGridCellBounds(), tm, SceneryContext.getInstance());
        /*24.8.26 meshWayConnector =*/ ((WayTerrainMeshAdder)terrainMeshAdder).registerConnector(node.getOsmId(), polygonLine, attachpair);

        // 27.2.26: We are back at storing polygons, so don't go this way any more.
       /*
        WayArea mainWayArea0 = wayAreaCandidates.get(getMajor0().getOsmId());
        WayArea mainWayArea1 = wayAreaCandidates.get(getMajor1().getOsmId());

        switch (type) {
            case SIMPLE_CONNECTOR:
                //has two (identical) attach pairs
                if (majorway1 == -1 || majorway0 == -1) {
                    log.error("inconsistent connector");
                    return;
                }
                // aber nicht bei closed ways
                if (!mainWayArea0.isClosed()) {
                    addAttachPairToTerrainMesh(majorway0, ways.get(majorway1), tm);
                }
                break;
            case SIMPLE_SINGLE_JUNCTION:
                //zur Analyse. getMeshPolygon() geht hier noch nicht.
                //List<MeshLine> lines = TerrainMesh.getInstance().getLinesOfArea(ways.get(majorway0).getWayArea());
                //log.warn("not yet? immer noc?");
                // Der main way (immer der main0?) hat schon sein Extrapair. Und das ist schon hinterlegt. Aber die Line muss dort gesplittet werden.
                // Und die Verbindung der beiden mains fehlt noch. Und dann das Schliessen der Junction? Nee, nur das Eintragen
                CoordinatePair attachpair = getAttachPairInNodeOrientation(majorway0);
                //TODO tm.registerLine(JtsUtil.toList(attachpair.left(), attachpair.right()), ways.get(majorway0).getArea()[0], ways.get(majorway1).getArea()[0], false, false);
                //lines = tm.getLinesOfArea(ways.get(majorway0).getWayArea());
                //lines = tm.getLinesOfArea(ways.get(majorway1).getWayArea());
                attachpair = getAttachCoordinates(ways.get(minorway));
                //einer der beiden Attachpair Points ist kein MeshPoint. An dem wird gesplittet. 4.9.19: Da kann man sich aber nicht drauf verlassen. Der minor kann ja schon im Mesh sein.
                //MeshPoint meshPoint = tm.getMeshPoint(attachpair.getFirst());
                Coordinate splitCoordinate;

                //if (/*meshPoint == null* /(major0StartsHere() && minorHitsLeft()) ||                        (!major0StartsHere() && !minorHitsLeft())) {
                if (mainWayArea0.isOuterCoordinate(attachpair.left())) {
                    splitCoordinate = attachpair.right();
                } else {
                    splitCoordinate = attachpair.left();
                }
                //TODO: was ist denn, wenn er mehrere lines findet?
                MeshLine lineToMinor = null;//TODO tm.findLines(mainWayArea0.getArea()[0], splitCoordinate).get(0);
                if (ways.get(majorway0).getOsmId() == 7645770) {
                    int h = 9;
                }
                MeshLine[] splitresult = tm.split(lineToMinor, JtsUtil.findVertexIndex(splitCoordinate, lineToMinor.getCoordinates()));
                int splitresultindex = (major0StartsHere() ? 0 : 1);
                if (splitresultindex > splitresult.length - 1) {
                    log.error("inconsistent connector split in " + node.getOsmId());
                    return;
                }
                lineToMinor = splitresult[splitresultindex];
                //TODO tm.completeLine(lineToMinor, ways.get(minorway).getArea()[0]);
                 /*attachpair = getAttachCoordinates(ways.get(minorway).mapWay);
                if (minorStartsHere()) {
                    TerrainMesh.getInstance().registerLine(JtsUtil.toList(attachpair.left(), attachpair.right()), ways.get(minorway).getArea()[0], null, false, false);
                }else{
                    TerrainMesh.getInstance().registerLine(JtsUtil.toList(attachpair.left(), attachpair.right()), null,ways.get(minorway).getArea()[0],  false, false);
                }* /
                //lines = TerrainMesh.getInstance().getLinesOfArea(ways.get(minorway).getWayArea());

                break;
            case SIMPLE_INNER_SINGLE_JUNCTION:
                //there isType only this single one
                if (minorway == -1 || majorway0 == -1) {
                    log.error("inconsistent");
                    return;
                }
                addAttachPairToTerrainMesh(minorway, ways.get(majorway0), tm);
                break;
            case STANDARD_TRI_JUNCTION:
                addAttachPairToTerrainMesh(majorway0, this, tm);
                addAttachPairToTerrainMesh(majorway1, this, tm);
                addAttachPairToTerrainMesh(minorway, this, tm);
                tm.registerLine(JtsUtil.createLine(closingpair.left(), closingpair.right()), this.getArea()[0], null);
                break;
            case MOTORWAY_ENTRY_JUNCTION:
                addAttachPairToTerrainMesh(majorway0, this, tm);
                addAttachPairToTerrainMesh(majorway1, this, tm);
                addAttachPairToTerrainMesh(minorway, this, tm);
                addAttachPairToTerrainMesh(secondminor, this, tm);
                tm.registerLine(JtsUtil.createLine(closingpair.left(), closingpair.right()), this.getArea()[0], null);
                //und noch die Verbindung zwischen den beiden minors
                CoordinatePair p0 = getAttachPairInNodeOrientation(minorway);
                CoordinatePair p1 = getAttachPairInNodeOrientation(secondminor);
                tm.registerLine(JtsUtil.createLine(p0.right(), p1.left()), null, this.getArea()[0]);
                break;
            default:
                log.warn("unknown connector type " + type);
        }*/

    }

    /*11.5.26 private boolean minorStartsHere() {
        return ways.get(minorway).getStartNode() == this.node;
    }*/

    private boolean major0StartsHere() {
        return ways.get(majorway0).getStartNode() == this.node;
    }

    private boolean major1StartsHere() {
        return ways.get(majorway1).getStartNode() == this.node;
    }

    /*16.4.26 @Override
    public boolean isPartOfMesh(TerrainMesh tm) {
        //TODO irgendwie erkennen
        return false;
    }*/

   /*9.3.26  private void addAttachPairToTerrainMesh(int waya, SceneryFlatObject otherarea, TerrainMesh tm) {
        CoordinatePair pair = attachpair.get(ways.get(waya));
        if (pair == null) {
            log.error("not found");
            return;
        }
        if (!ways.get(waya).isStartNode(node)) {
            pair = pair.swap();
        }
        MapWay/*SceneryWayObject* / way = ways.get(waya);
        // avoid registering eg. bridges
        //TODO? 19.2.26
        //tm.registerLine(JtsUtil.toList(pair.left(), pair.right()), (way.isTerrainProvider) ? way.getArea()[0] : null, (otherarea.isTerrainProvider) ? otherarea.getArea()[0] : null, false, false);

    }*/

    public CoordinatePair getAttachCoordinates(MapWay mapWay) {
        Util.notyet();
        return null;
        /*9.3.26 CoordinatePair pair = attachpair.get(mapWay);
        if (pair == null) {
            log.warn("no attach coordinates found for way " + mapWay.getOsmId());
        }
        return pair;*/
        /*if (flatComponent == null || flatComponent.poly == null) {
            log.warn("no attach coordinates");
            return null;
        }
        Coordinate[] coors = flatComponent.poly.polygon[0].getCoordinates();
        if (coors.length < 2) {
            log.warn("too few attach coordinates");
            return null;
        }
        //
        return new Pair<>(coors[0], coors[1]);*/
    }

    /**
     * Prueft nur auf einen. Hmm. TODO
     *
     * @return
     */
    public boolean hasMinor() {
        return minorway0 != -1;
    }

    public WayConnectorType getType() {
        return type;
    }

    public MapWaySegment2/*SceneryWayObject*/ getMajor0() {
        if (majorway0 == -1) {
            return null;
        }
        return ways.get(majorway0);
    }

    public MapWaySegment2/*SceneryWayObject*/ getMajor1() {
        if (majorway1 == -1) {
            return null;
        }
        return ways.get(majorway1);
    }

    /*public SceneryWayObject getMinor() {
        if (minorway == -1) {
            return null;
        }
        return ways.get(minorway);
    }*/

    public MapWaySegment2/*SceneryWayObject*/ getWay(int index) {

        return ways.get(index);
    }

    /**
     * logical resolve.
     * <p>
     * Nach dem Clip sollten sich keine Ways mehr overlappen. There are situations, where the minor way
     * isType too close to a main way. (eg. 120831068,225794270,120831071)
     * <p>
     * Erstmal nur fuer Fälle, in denen verkleinern von minor pairs hilft.
     * <p>
     * Das ganze aber besser vom Way aus resolven? Besser nicht, denn es gibt z.B. die Fälle, wo der minor explizit betroffen ist und eine Änderung des main unangemessen wäre.
     * 7.3.26: No longer used for simplification? Probably leads to missing connector due to exception?
     * But why not doing it during building?
     */
    //@Override
    public void resolveOverlaps(TerrainMesh tm) {
        if (hasMinor()) {

            MapWaySegment2/*SceneryWayObject*/ minor = ways.get(minorway0);
            /*19.2.26 try without if (minor.overlaps(getMajor0())) {
                WayArea minorway = minor.getWayArea();
                WayArea majorway = getMajor0().getWayArea();

                log.debug("adjusting overlapping minor " + minor.getOsmIdsAsString() + " at connector " + node.getOsmId());
                OverlapResolver.resolveInnerWayOverlaps(minor, majorway, tm);
            }*/
        }
    }

    /**
     * technical resolve
     *
     * @param overlap
     */
    public void resolveOverlaps(AbstractArea overlap, TerrainMesh tm) throws MeshInconsistencyException {
        WayArea mainWayArea0 = wayAreaCandidates.get(getMajor0());
        WayArea mainWayArea1 = wayAreaCandidates.get(getMajor1());

        if (type == WayConnectorType.SIMPLE_CONNECTOR && getMajor0() != null) {
            WayArea wayArea = mainWayArea0;
            CoordinatePair reduced;
            if (major0StartsHere()) {
                reduced = OverlapResolver.resolveSingleWayOverlap(wayArea, 0, overlap, tm);
            } else {
                reduced = OverlapResolver.resolveSingleWayOverlap(wayArea, wayArea.getLength() - 1, overlap, tm);
            }
            /*9.3.26 if (reduced != null) {
                if (major0StartsHere()) {
                    attachpair.put(getMajor0(), reduced);
                } else {
                    //stimmt doch nicht reduced=reduced.swap();
                    attachpair.put(getMajor0(), reduced);
                }
                reduced = getAttachPairInNodeOrientation(majorway0);
                if (major1StartsHere()) {
                    mainWayArea1.replaceStart(reduced.swap());
                    attachpair.put(getMajor1(), reduced.swap());
                } else {
                    mainWayArea1.replaceEnd(reduced);
                    attachpair.put(getMajor1(), reduced);
                }
            }*/
        }
    }

    /**
     *
     */
   /*9.3.26 public CoordinatePair getAttachPairInNodeOrientation(int index) {
        MapWay/*SceneryWayObject* / way = ways.get(index);
        WayArea wayArea = wayAreaCandidates.get(way.getOsmId());
        if (way.getStartNode() == this.node) {
            return attachpair.get(wayArea.mapWay);
        }
        return attachpair.get(wayArea.mapWay).swap();
    }*/
    public CoordinatePair getWayStartEndPairInNodeOrientation(int index, TerrainMesh tm) {
        MapWaySegment2/*SceneryWayObject*/ way = ways.get(index);
        WayArea wayArea = wayAreaCandidates.get(way);
        if (way.getStartNode() == this.node) {
            return wayArea.getStartPair(tm);//[0];
        }
        //TODO index 0 durefte nicht immer stimmen
        return wayArea.getEndPair()/*19.3.26 [0]*/.swap();
    }

    public CoordinatePair getWayStartEndPair(int index, TerrainMesh tm) {
        MapWaySegment2/*SceneryWayObject*/ way = ways.get(index);
        WayArea wayArea = wayAreaCandidates.get(way);
        if (way.getStartNode() == this.node) {
            return wayArea.getStartPair(tm)/*19.3.26 [0]*/;
        }
        //TODO index 0 durefte nicht immer stimmen
        return wayArea.getEndPair()/*19.3.26 [0]*/;
    }

    public boolean hasBridge() {
        for (MapWaySegment2/*SceneryWayObject*/ way : ways) {
            /*21.2.26 TODO if (way instanceof BridgeOrTunnel) {
                return true;
            }*/
        }
        return false;
    }

    public int getWaysCount() {
        return ways.size();
    }

    public Long getOsmId() {
        return getOsmIds().get(0);
    }

    /**
     * All "SIMPLE*" types have in common:
     * - the main way visually continues at the connector.
     * - The connector has no visual area.
     * - Typically used for minor ways (eg. OSM one way links, tracks, etc.) using a texture without center line
     */
    public static enum WayConnectorType {
        //connection of excactly two ways at their outer nodes
        SIMPLE_CONNECTOR,
        // one/two really minor ways (e.g. country ways) running into a major way (like node 445410497). No constructional effect to any way.
        // Connector now has a polygon, but no widening or other special adjustment.
        // Otherwise STANDARD_TRI_JUNCTION should be used.
        // Also for Rechtsabbieger short cut Spuren in manchen Kreuzungen.
        SIMPLE_JUNCTION,
        // one minor way hitting a major way on an inner node (not outer node). Will have no own polygon.
        // der minor soll keine Mittelline haben. Aber mehrspurige in A-Kreuzen?
        // Z.B. die Rechtsabbieger short cut Spuren in manchen Kreuzungen.
        // 19.3.26: Deprecated because we don't have mid connector any more.
        //SIMPLE_INNER_SINGLE_JUNCTION,
        //same as SIMPLE_INNER_SINGLE_JUNCTION but with a second minor on the opposite side of main. No own polygon.
        //19.3.26 SIMPLE_INNER_DOUBLE_JUNCTION,
        // three/four? ways, all ending/starting at connector (there no longer is any other option).
        // Two of these are major (with ROW), the minor gives way (sketch 68).
        // E.g. node 2555563538
        // Has constructional effect to some of the ways. The major way is widened for 'turn right/left' lanes. If this is not intended,
        // eg. when minor is really minor like a path, type SIMPLE_SINGLE_JUNCTION should be used.
        // also for two minor? No.
        STANDARD_JUNCTION,
        // Beginn einer Autobahnauffahrt (z.B. 1353883890).
        // two quite parallel minor ways hitting a major way on an outer node (not inner node).
        MOTORWAY_ENTRY_JUNCTION,
        //Zerbrastreifen or similar, ansonsten wie SIMPLE_CONNECTOR; exakt two ways
        //Eigentlich ist das eher ein zusätzliches Attribut als ein eigenständiger Connectortype. An 54289952 kommt einfach ein Radweg(?) auf eine Strasse mit Fussgaengerampel.
        //an 388796251 kreuzt ein Fussweg. Mal attribut isCrossing
        //CROSSING,
        //generic connector with n nodes. Polygon with area. Used when we do not find one of
        // the above predefined pattern, eg. with "number of ways > 3".
        GENERIC;


    }

    public Polygon getPolygon(SceneryProjection projection) {
        //5.3.26 return flatComponent[0].getPolygon(null);
        return JtsUtil.createPolygon(polygonLine, projection);
    }

}

@FunctionalInterface
interface PermutationHandler {
    boolean run(int[] order) throws MeshInconsistencyException;
}

