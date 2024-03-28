package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Degree;
import de.yard.threed.core.OutlineBuilder;
import de.yard.threed.core.Util;
import de.yard.threed.core.Vector2;
import de.yard.threed.osm2graph.osm.CoordinateList;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.modules.common.BridgeOrTunnel;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2scenery.scenery.components.WayArea;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import de.yard.threed.osm2world.ConfMaterial;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.Materials;

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
 * 18.7.19: Diese Connector sind für alle Ways, auch Railway, River, etc.
 */
public class SceneryWayConnector extends SceneryNodeObject {
    private WayConnectorType type = null;
    public int minorway = -1, majorway0 = -1, majorway1 = -1, secondminor = -1;
    private List<SceneryWayObject> ways = new ArrayList<>();
    //Die Orientierung in attachpair ist passend für den jeweiligen Way
    private Map<MapWay, CoordinatePair> attachpair = new HashMap();
    private boolean broken = false;
    public CoordinatePair additionalmain0pair = null, closingpair = null;
    //Angles der Ways CCW sortiert nach angle zu way0.Nur bei allouternodes!
    public double[] angles;
    //sortiert ab way0
    public int[] angleorder;
public boolean isCrossing;

    public SceneryWayConnector(String creatortag, MapNode node, ConfMaterial material, Category category) {
        super(creatortag, node, material, category);
        this.cycle = Cycle.WAY;
        if (node.getOsmId() == 295055704 || node.getOsmId() == 1379039502) {
            int h = 9;
        }

    }

    public void classify() {
        if (node.getOsmId() == 295055704 || node.getOsmId() == 1379039502) {
            int h = 9;
        }

        boolean allouternodes = true;
        List<Integer> outerNodeWays = new ArrayList<>();
        List<Integer> innerNodeWays = new ArrayList<>();
        for (int i = 0; i < ways.size(); i++) {
            SceneryWayObject way = ways.get(i);
            if (!way.isOuterNode(node)) {
                allouternodes = false;
                innerNodeWays.add(i);
            } else {
                outerNodeWays.add(i);
            }
        }
        if (angles == null && allouternodes) {
            sortByAngles();
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
            isCrossing=true;
        }

        if (ways.size() == 1) {
            //closed way?
            if (ways.get(0).isClosed() && ways.get(0).isOuterNode(node)) {
                //das muesste doch gehen.
                type = WayConnectorType.SIMPLE_CONNECTOR;
                majorway0 = 0;
                majorway1 = 0;
                return;
            }
        }
        if (ways.size() == 2) {
            if (ways.get(0).isOuterNode(node) && !ways.get(1).isOuterNode(node)) {
                type = WayConnectorType.SIMPLE_INNER_SINGLE_JUNCTION;
                minorway = 0;
                majorway0 = 1;

                return;
            }
            if (!ways.get(0).isOuterNode(node) && ways.get(1).isOuterNode(node)) {
                type = WayConnectorType.SIMPLE_INNER_SINGLE_JUNCTION;
                minorway = 1;
                majorway0 = 0;
                return;
            }
            if (allouternodes) {
                type = WayConnectorType.SIMPLE_CONNECTOR;
                majorway0 = 0;
                majorway1 = 1;
                return;
            }
        }
        if (ways.size() == 3) {
            if (outerNodeWays.size() == 2 && innerNodeWays.size() == 1) {
                //TODO die minor muessen gegenüber liegen.
                type = WayConnectorType.SIMPLE_INNER_DOUBLE_JUNCTION;
                majorway0 = innerNodeWays.get(0);
                minorway = outerNodeWays.get(0);
                secondminor = outerNodeWays.get(1);
                return;
            }
        }
        if (allouternodes) {
            if (checkForSTANDARD_TRI_JUNCTION()) {
                return;
            }
            if (checkForSIMPLE_SINGLE_JUNCTION()) {
                return;
            }
            if (checkForMOTORWAY_ENTRY_JUNCTION()) {
                return;
            }
        }
        if (allouternodes) {
            type = WayConnectorType.GENERIC;
            return;
        }
        logger.warn("cannot classify connector at node " + node.getOsmNode().id);
    }

    /**
     * 3 Ways, von denen zwei quasi in Reihe und einer im quasi rechten Winkel abgeht, der aber keine "minor Way" ist (Mittelline haben soll).
     * Sonst macht die ConnectorArea keine Sinn (zukünftig).
     * <p>
     * Return true if type STANDARD_TRI_JUNCTION was detected.
     *
     * @return
     */
    private boolean checkForSTANDARD_TRI_JUNCTION() {
        if (ways.size() != 3) {
            return false;
        }
        if (node.getOsmId() == 54286220) {
            int h = 9;
        }

        Degree[] angles = new Degree[]{new Degree(-180), new Degree(-90)};
        return runPermutated(3, (order) -> {
            //for (int[] order : orders) {
            if (compliesPattern(order, angles, new double[]{0.1, 0.1})) {
                if (!(HighwayModule.isMinorLink(ways.get(order[2]).mapWay.getTags()))) {

                    type = WayConnectorType.STANDARD_TRI_JUNCTION;
                    majorway0 = order[0];
                    minorway = order[2];
                    majorway1 = order[1];
                    return true;
                }
            }
            return false;
        });
    }

    private boolean runPermutated(int size, PermutationHandler permutationHandler) {
        int[][] orders = Util.buildPermutation(size);
        if (orders == null) {
            logger.error("no permutation");
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
            logger.error("invalid usage");
            return false;
        }
        if (node.getOsmId() == 255563538) {
            int h = 9;
        }

        Vector2 dir0 = OsmUtil.getDirectionToNode(ways.get(order[0]).mapWay, node);
        Vector2 dir1 = OsmUtil.getDirectionFromNode(ways.get(order[1]).mapWay, node);
        Vector2 dir2 = OsmUtil.getDirectionFromNode(ways.get(order[2]).mapWay, node);
        Vector2 dir3 = null;
        if (order.length == 4) {
            dir3 = OsmUtil.getDirectionFromNode(ways.get(order[3]).mapWay, node);
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
     * Return true if type SIMPLE_SINGLE_JUNCTION was detected.
     *
     * @return
     */
    private boolean checkForSIMPLE_SINGLE_JUNCTION() {
        if (ways.size() != 3) {
            return false;
        }
        if (node.getOsmId() == 2345486462L) {
            int h = 9;
        }

        return runPermutated(3, (order) -> {
            if ((HighwayModule.isLink(ways.get(order[2]).mapWay.getTags()))) {
                type = WayConnectorType.SIMPLE_SINGLE_JUNCTION;
                majorway0 = order[0];
                majorway1 = order[1];
                minorway = order[2];
                return true;
            }
            return false;
        });

    }

    /**
     * {@link WayConnectorType}
     * <p>
     * Return true if type MOTORWAY_ENTRY_JUNCTION was detected.
     *
     * @return
     */
    private boolean checkForMOTORWAY_ENTRY_JUNCTION() {
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
                if (HighwayModule.isMinorLink(ways.get(order[2]).mapWay.getTags()) &&
                        HighwayModule.isMinorLink(ways.get(order[3]).mapWay.getTags())) {

                    type = WayConnectorType.MOTORWAY_ENTRY_JUNCTION;
                    majorway0 = order[0];
                    majorway1 = order[1];
                    minorway = order[2];
                    secondminor = order[3];
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Might be called multiple times for the same way.
     *
     * @param way
     */
    public void add(SceneryWayObject way) {
        if (!ways.contains(way)) {
            ways.add(way);
        }
    }

    @Override
    public List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds, TerrainMesh tm) {
        if (node.getOsmId() == 2345486254L || node.getOsmId() == 295055704) {
            int h = 9;
        }
        //Der Default ist empty.
        flatComponent = new AbstractArea[]{AbstractArea.EMPTYAREA};
        for (SceneryWayObject way : ways) {
            if (!(way.getArea()[0] instanceof WayArea)) {
                broken = true;
                logger.error("Connector " + node.getOsmId() + " isType broken");
                minorway = -1;
                majorway0 = -1;
                majorway1 = -1;
                return null;
            }
        }
        if (type == null) {
            logger.warn("unclassified connector " + node.getOsmId());
            return null;
        }
        if (node.getOsmId() == 2345486254L) {
            int h = 9;
        }
        switch (type) {
            case SIMPLE_INNER_SINGLE_JUNCTION:
                //a minor way hitting a main way on an inner node.
                //Uses an additional coordinate added to main.
                //Connector bleibt empty, weil der main way die Darstellung macht. Braucht aber clip in junction.
                buildMinorAttachAtInnerMain(minorway, tm);
                break;
            case SIMPLE_INNER_DOUBLE_JUNCTION:
                buildMinorAttachAtInnerMain(minorway, tm);
                buildMinorAttachAtInnerMain(secondminor, tm);
                break;
            case STANDARD_TRI_JUNCTION:
                createPolygonSTANDARD_TRI_JUNCTION();
                break;
            case MOTORWAY_ENTRY_JUNCTION:
                createPolygonSTANDARD_TRI_JUNCTION();
                break;
            case SIMPLE_CONNECTOR:
                buildSimpleMainConnection();
                break;
            /*case CROSSING:
                //erstmal einfach so. Bei inner node erstmal nichts. Na, das koennen auch vier Ways sein (388796251).
                if (ways.size() == 2) {
                    buildSimpleMainConnection();
                }
                break;*/
            case SIMPLE_SINGLE_JUNCTION:
                //schwieriger als gedacht. Es muessen ja zwei Coordinates für den attach her. Darum
                //bekommt einer der beiden main ways noch ein Zusatzpair.
                CoordinatePair c = buildSimpleMainConnection();
                buildSimpleMinorAttach(c, tm);
                break;
            case GENERIC:
                // just n ways, no major or minor.
                buildGenericConnection();
                break;
            default:
                logger.warn("unknown connector type " + type);
        }
        return null;
    }

    /**
     * Die beiden mains(s) quasi verbinden. Der Connector selber bekommt damit keine Area.
     * Returns connectionPair in "main0 perspective".
     * major0 might equal major1 for closed ways.
     */
    private CoordinatePair buildSimpleMainConnection() {
        double width = ways.get(0).getWidth();
        SceneryWayObject main0 = ways.get(majorway0);
        SceneryWayObject main1 = ways.get(majorway1);
        Vector2 dir0 = OsmUtil.getDirectionToNode(main0.mapWay, node);
        //negate geht zumindest nicht bei closed.
        //Vector2 dir1 = OsmUtil.getDirectionToNode(main1.mapWay, node).negate();
        Vector2 dir1 = OsmUtil.getDirectionFromNode(main1.mapWay, node);
        Coordinate pleft = toCoordinate(OutlineBuilder.getOutlinePointFromDirections(dir0, toVector2(node.getPos()), dir1, -width / 2));
        Coordinate pright = toCoordinate(OutlineBuilder.getOutlinePointFromDirections(dir0, toVector2(node.getPos()), dir1, width / 2));
        if (main0.isStartNode(node)) {
            attachpair.put(main0.mapWay, new CoordinatePair(pleft, pright));
        } else {
            attachpair.put(main0.mapWay, new CoordinatePair(pright, pleft));
        }
        if (main1.isStartNode(node)) {
            attachpair.put(main1.mapWay, new CoordinatePair(pright, pleft));
        } else {
            attachpair.put(main1.mapWay, new CoordinatePair(pleft, pright));
        }
        return attachpair.get(main0.mapWay);//new CoordinatePair(pright, pleft);
    }

    /**
     * Es muessen ja zwei Coordinates für den attach her. Darum
     * bekommt main0 (besser der main mit kleinerem winkel TODO) ein Zusatzpair.
     * Das kann man aber nicht hier machen, sondern nur im clip des Way.
     * mainConnection isType in major0 orientation
     *
     * @param mainConnection (in main0perspective)
     */
    private void buildSimpleMinorAttach(CoordinatePair mainConnection, TerrainMesh tm) {
        SceneryWayObject main0 = ways.get(majorway0), minor = ways.get(minorway);

        additionalmain0pair = main0.shiftStartOrEnd(node, 8.5);
        CoordinatePair p = additionalmain0pair;//main0.toNodeOrientation(node,additionalmain0pair);
        if (minorHitsLeft(minorway, tm)) {
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
        }
    }

    /**
     * Ein n-Eck
     */
    private void buildGenericConnection() {
        if (node.getOsmId() == 335476635) {
            int h = 9;
        }
        double defaultWidth = 5;
        double defaultDistance = 8;
        LineSegment[] segs = new LineSegment[ways.size()];

        //int cntr = 0;
        do {
            CoordinateList coors = new CoordinateList();

            // Die Segmente muessten CCW sein
            for (int i = 0; i < ways.size(); i++) {
                int wayIndex = angleorder[i];
                SceneryWayObject way = ways.get(wayIndex);
                Vector2 n = OsmUtil.toVector2(node.getPos());
                Vector2 dir = OsmUtil.getDirectionFromNode(way.mapWay, node);
                Vector2 nrm = dir.rightNormal().multiply(defaultWidth);
                Vector2 center = n.add(dir.multiply(defaultDistance));
                segs[i] = new LineSegment(toCoordinate(center.add(nrm)), toCoordinate(center.add(nrm.negate())));
                coors.add(segs[i].p0);
                coors.add(segs[i].p1);
                buildAttachPair(segs[i].p0, segs[i].p1, wayIndex);
            }
            coors.add(coors.get(0));
            Polygon geo = JtsUtil.createPolygon(coors.coorlist);
            if (geo == null || !geo.isValid()) {
                logger.debug("invalid polygon with width " + defaultWidth);
            } else {
                flatComponent = new AbstractArea[]{new Area(geo, Materials.ROAD)};
                return;
            }
            defaultWidth -= 0.5;
        }
        while (defaultWidth > 0);
        flatComponent = new AbstractArea[]{AbstractArea.EMPTYAREA};
        logger.error("finally no polygon");

    }

    /**
     * Liefert die Indizes der Ways CCW sortiert nach angle zu way0.
     *
     * @return
     */
    private void sortByAngles() {
        Vector2[] dirs = new Vector2[ways.size()];
        angles = new double[ways.size()];

        for (int i = 0; i < ways.size(); i++) {
            SceneryWayObject way = ways.get(i);
            Vector2 n = OsmUtil.toVector2(node.getPos());
            dirs[i] = OsmUtil.getDirectionFromNode(way.mapWay, node);
        }

        angles[0] = 0;
        List<Integer> angleOrder = new ArrayList<>();
        angleOrder.add(0);
        for (int i = 1; i < ways.size(); i++) {
            double angle = Vector2.getRotationAngleBetween(dirs[0], dirs[i]);
            angles[i] = angle;
            for (int j = 0; j <= angleOrder.size(); j++) {
                if (j == angleOrder.size()) {
                    angleOrder.add(i);
                    break;
                }
                if (angle < angles[angleOrder.get(j)]) {
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
     *
     * @return
     */
    public boolean minorHitsLeft(int mi, TerrainMesh tm) {
        SceneryWayObject minor = ways.get(mi);
        if (minor == null) {
            throw new RuntimeException("no way " + mi);
        }
        SceneryWayObject main0 = getMajor0();
        MapNode minorRefNode;
        if (minor.isEndNode(node)) {
            minorRefNode = minor.mapWay.getMapNodes().get(minor.mapWay.getMapNodes().size() - 2);
        } else {
            minorRefNode = minor.mapWay.getMapNodes().get(1);
        }
        Integer position = main0.getWayArea().getPosition(node);
        CoordinatePair[] connectorpair = main0.getWayArea().getMultiplePair(position);
        if (connectorpair.length == 2) {
            // mit pair 0 oder 1??? Egal??
        }
        CoordinatePair refpair = connectorpair[0];
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
     */
    private void createPolygonSTANDARD_TRI_JUNCTION() {
        SceneryWayObject main0 = getMajor0(), main1 = getMajor1();
        /*if (!(minor.getArea() instanceof WayArea)) {
            logger.warn("no tri junction polygon. minor no way");
            return;
        }
        if (!(main0.getArea() instanceof WayArea)) {
            logger.warn("no tri junction polygon. main0 no way");
            return;
        }
        if (!(main1.getArea() instanceof WayArea)) {
            logger.warn("no tri junction polygon. main1 no way");
            return;
        }

        WayArea minorarea = (WayArea) minor.getArea();*/
        //minorarea.getLeftOutline()
        double offset = 5.5;
        if (secondminor != -1) {
            offset = 9.5;
        }
        CoordinatePair major0line = main0.shiftStartOrEnd(node, 5.5);
        CoordinatePair major1line = main1.shiftStartOrEnd(node, 5.5);
        if (major0line == null || major1line == null) {
            logger.warn("no tri junction polygon");
            return;
        }
        List<Coordinate> clist = new ArrayList<>();

        attachpair.put(main0.mapWay, major0line);
        CoordinatePair p0 = main0.toNodeOrientation(node, major0line);
        clist.add(p0.right());
        clist.add(p0.left());

        attachpair.put(main1.mapWay, major1line);
        CoordinatePair p1 = main1.toNodeOrientation(node, major1line);

        if (secondminor != -1) {
            //Der Abstand der beiden minors mal 1.5 meter.
            CoordinatePair major0innerline = main0.shiftStartOrEnd(node, 0.75);
            CoordinatePair major1innerline = main1.shiftStartOrEnd(node, 0.75);
            CoordinatePair pi0 = main0.toNodeOrientation(node, major0innerline);
            CoordinatePair pi1 = main1.toNodeOrientation(node, major1innerline);
            clist.add(pi0.left());
            clist.add(pi1.right());
            buildAttachPair(p0.left(), pi0.left(), secondminor);
            buildAttachPair(pi1.right(), p1.right(), minorway);
        } else {
            buildAttachPair(p0.left(), p1.right(), minorway);
        }
        clist.add(p1.right());
        clist.add(p1.left());

        //close polygon
        clist.add(clist.get(0));

        Polygon geo = JtsUtil.createPolygon(clist);
        if (geo == null || !geo.isValid()) {
            logger.error("invalid polygon");
        }
        closingpair = new CoordinatePair(clist.get(0), clist.get(clist.size() - 2));
        flatComponent = new AbstractArea[]{new Area(geo, Materials.ROAD)};
    }

    /**
     *
     */
    private void buildMinorAttachAtInnerMain(int mi, TerrainMesh tm) {
        SceneryWayObject main = getMajor0();
        Integer position = main.getWayArea().getPosition(node);
        CoordinatePair[] connectorpair = main.getWayArea().getMultiplePair(position);
        if (connectorpair == null) {
            logger.error("inconsistent?");
        } else {
            if (connectorpair.length == 2) {
                // Was heisst denn das? An der Node gibt es schon ein zweites Paar? Wo kommt denn das her? Das wurde schon im Way für die inner node angelegt.
                // links oder rechts?
                if (minorHitsLeft(mi, tm)) {
                    buildAttachPair(connectorpair[1].getSecond(), connectorpair[0].getSecond(), mi);
                            /*if (minor.isEndNode(node)) {
                                attachpair.put(minor.mapWay, new CoordinatePair(connectorpair[0].getSecond(), connectorpair[1].getSecond()));
                            } else {
                                attachpair.put(minor.mapWay, new CoordinatePair(connectorpair[1].getSecond(), connectorpair[0].getSecond()));
                            }*/
                } else {
                    buildAttachPair(connectorpair[0].getFirst(), connectorpair[1].getFirst(), mi);
                            /*if (minor.isEndNode(node)) {
                                attachpair.put(minor.mapWay, new CoordinatePair(connectorpair[1].getFirst(), connectorpair[0].getFirst()));
                            } else {
                                attachpair.put(minor.mapWay, new CoordinatePair(connectorpair[0].getFirst(), connectorpair[1].getFirst()));
                            }*/
                }
            } else {
                logger.warn("no multiple pair at connector");
            }
        }
    }

    /**
     * Parameters related to start
     */
    private void buildAttachPair(Coordinate right, Coordinate left, int wayindex) {
        SceneryWayObject way = ways.get(wayindex);
        CoordinatePair minorattachpair;
        if (way.isStartNode(node)) {
            minorattachpair = new CoordinatePair(right, left);
        } else {
            minorattachpair = new CoordinatePair(left, right);
        }
        attachpair.put(way.mapWay, minorattachpair);
    }

    @Override
    public void clip(TerrainMesh tm) {
        super.clip(tm);
    }

    /**
     * TerrainMesh vorbereiten.
     * <p>
     * Vom Connector kommen die inner lines ins Mesh. Obwohl die eigentlich nicht gebraucht werden, nur für die Konsistenz.
     */
    @Override
    public void addToTerrainMesh(TerrainMesh tm) {
        super.addToTerrainMesh(tm);
        // flatcomponent might be null
        if (node.getOsmId() == 2345485946L) {
            int h = 9;
        }

        if (type == null) {
            logger.error("unclassified connector " + node.getOsmId());
            return;
        }

        switch (type) {
            case SIMPLE_CONNECTOR:
                //has two (identical) attach pairs
                if (majorway1 == -1 || majorway0 == -1) {
                    logger.error("inconsistent connector");
                    return;
                }
                // aber nicht bei closed ways
                if (!getMajor0().isClosed()) {
                    addAttachPairToTerrainMesh(majorway0, ways.get(majorway1), tm);
                }
                break;
            case SIMPLE_SINGLE_JUNCTION:
                //zur Analyse. getMeshPolygon() geht hier noch nicht.
                //List<MeshLine> lines = TerrainMesh.getInstance().getLinesOfArea(ways.get(majorway0).getWayArea());
                //logger.warn("not yet? immer noc?");
                // Der main way (immer der main0?) hat schon sein Extrapair. Und das ist schon hinterlegt. Aber die Line muss dort gesplittet werden.
                // Und die Verbindung der beiden mains fehlt noch. Und dann das Schliessen der Junction? Nee, nur das Eintragen
                CoordinatePair attachpair = getAttachPairInNodeOrientation(majorway0);
                tm.registerLine(JtsUtil.toList(attachpair.left(), attachpair.right()), ways.get(majorway0).getArea()[0], ways.get(majorway1).getArea()[0], false, false);
                //lines = tm.getLinesOfArea(ways.get(majorway0).getWayArea());
                //lines = tm.getLinesOfArea(ways.get(majorway1).getWayArea());
                attachpair = getAttachCoordinates(ways.get(minorway).mapWay);
                //einer der beiden Attachpair Points ist kein MeshPoint. An dem wird gesplittet. 4.9.19: Da kann man sich aber nicht drauf verlassen. Der minor kann ja schon im Mesh sein.
                //MeshPoint meshPoint = tm.getMeshPoint(attachpair.getFirst());
                Coordinate splitCoordinate;

                //if (/*meshPoint == null*/(major0StartsHere() && minorHitsLeft()) ||                        (!major0StartsHere() && !minorHitsLeft())) {
                if (getMajor0().getWayArea().isOuterCoordinate(attachpair.left())) {
                    splitCoordinate = attachpair.right();
                } else {
                    splitCoordinate = attachpair.left();
                }
                //TODO: was ist denn, wenn er mehrere lines findet?
                MeshLine lineToMinor = tm.findLines(getMajor0().getArea()[0], splitCoordinate).get(0);
                if (ways.get(majorway0).mapWay.getOsmId() == 7645770) {
                    int h = 9;
                }
                MeshLine[] splitresult = tm.split(lineToMinor, JtsUtil.findVertexIndex(splitCoordinate, lineToMinor.getCoordinates()));
                int splitresultindex = (major0StartsHere() ? 0 : 1);
                if (splitresultindex > splitresult.length - 1) {
                    logger.error("inconsistent connector split in " + node.getOsmId());
                    return;
                }
                lineToMinor = splitresult[splitresultindex];
                tm.completeLine(lineToMinor, ways.get(minorway).getArea()[0]);
                 /*attachpair = getAttachCoordinates(ways.get(minorway).mapWay);
                if (minorStartsHere()) {
                    TerrainMesh.getInstance().registerLine(JtsUtil.toList(attachpair.left(), attachpair.right()), ways.get(minorway).getArea()[0], null, false, false);
                }else{
                    TerrainMesh.getInstance().registerLine(JtsUtil.toList(attachpair.left(), attachpair.right()), null,ways.get(minorway).getArea()[0],  false, false);
                }*/
                //lines = TerrainMesh.getInstance().getLinesOfArea(ways.get(minorway).getWayArea());

                break;
            case SIMPLE_INNER_SINGLE_JUNCTION:
                //there isType only this single one
                if (minorway == -1 || majorway0 == -1) {
                    logger.error("inconsistent");
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
                logger.warn("unknown connector type " + type);
        }

    }

    private boolean minorStartsHere() {
        return ways.get(minorway).getStartNode() == this.node;
    }

    private boolean major0StartsHere() {
        return ways.get(majorway0).getStartNode() == this.node;
    }

    private boolean major1StartsHere() {
        return ways.get(majorway1).getStartNode() == this.node;
    }

    @Override
    public boolean isPartOfMesh(TerrainMesh tm) {
        //TODO irgendwie erkennen
        return false;
    }

    private void addAttachPairToTerrainMesh(int waya, SceneryFlatObject otherarea, TerrainMesh tm) {
        CoordinatePair pair = attachpair.get(ways.get(waya).mapWay);
        if (pair == null) {
            logger.error("not found");
            return;
        }
        if (!ways.get(waya).isStartNode(node)) {
            pair = pair.swap();
        }
        SceneryWayObject way = ways.get(waya);
        // avoid registering eg. bridges
        tm.registerLine(JtsUtil.toList(pair.left(), pair.right()), (way.isTerrainProvider) ? way.getArea()[0] : null, (otherarea.isTerrainProvider) ? otherarea.getArea()[0] : null, false, false);

    }

    public CoordinatePair getAttachCoordinates(MapWay mapWay) {
        CoordinatePair pair = attachpair.get(mapWay);
        if (pair == null) {
            logger.warn("no attach coordinates found for way " + mapWay.getOsmId());
        }
        return pair;
        /*if (flatComponent == null || flatComponent.poly == null) {
            logger.warn("no attach coordinates");
            return null;
        }
        Coordinate[] coors = flatComponent.poly.polygon[0].getCoordinates();
        if (coors.length < 2) {
            logger.warn("too few attach coordinates");
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
        return minorway != -1;
    }

    public WayConnectorType getType() {
        return type;
    }

    public SceneryWayObject getMajor0() {
        if (majorway0 == -1) {
            return null;
        }
        return ways.get(majorway0);
    }

    public SceneryWayObject getMajor1() {
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

    public SceneryWayObject getWay(int index) {

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
     */
    //@Override
    public void resolveOverlaps(TerrainMesh tm) {
        if (hasMinor()) {

            SceneryWayObject minor = ways.get(minorway);
            if (minor.overlaps(getMajor0())) {
                WayArea minorway = minor.getWayArea();
                WayArea majorway = getMajor0().getWayArea();

                logger.debug("adjusting overlapping minor " + minor.getOsmIdsAsString() + " at connector " + node.getOsmId());
                OverlapResolver.resolveInnerWayOverlaps(minor, majorway, tm);
            }
        }
    }

    /**
     * technical resolve
     *
     * @param overlap
     */
    public void resolveOverlaps(AbstractArea overlap, TerrainMesh tm) {

        if (type == WayConnectorType.SIMPLE_CONNECTOR && getMajor0() != null) {
            WayArea wayArea = getMajor0().getWayArea();
            CoordinatePair reduced;
            if (major0StartsHere()) {
                reduced = OverlapResolver.resolveSingleWayOverlap(wayArea, 0, overlap, tm);
            } else {
                reduced = OverlapResolver.resolveSingleWayOverlap(wayArea, wayArea.getLength() - 1, overlap, tm);
            }
            if (reduced != null) {
                if (major0StartsHere()) {
                    attachpair.put(getMajor0().mapWay, reduced);
                } else {
                    //stimmt doch nicht reduced=reduced.swap();
                    attachpair.put(getMajor0().mapWay, reduced);
                }
                reduced = getAttachPairInNodeOrientation(majorway0);
                if (major1StartsHere()) {
                    getMajor1().getWayArea().replaceStart(reduced.swap());
                    attachpair.put(getMajor1().mapWay, reduced.swap());
                } else {
                    getMajor1().getWayArea().replaceEnd(reduced);
                    attachpair.put(getMajor1().mapWay, reduced);
                }
            }
        }
    }

    /**
     *
     */
    public CoordinatePair getAttachPairInNodeOrientation(int index) {
        SceneryWayObject way = ways.get(index);
        if (way.getStartNode() == this.node) {
            return attachpair.get(way.mapWay);
        }
        return attachpair.get(way.mapWay).swap();
    }

    public CoordinatePair getWayStartEndPairInNodeOrientation(int index, TerrainMesh tm) {
        SceneryWayObject way = ways.get(index);
        if (way.getStartNode() == this.node) {
            return way.getWayArea().getStartPair(tm)[0];
        }
        //TODO index 0 durefte nicht immer stimmen
        return way.getWayArea().getEndPair()[0].swap();
    }

    public CoordinatePair getWayStartEndPair(int index, TerrainMesh tm) {
        SceneryWayObject way = ways.get(index);
        if (way.getStartNode() == this.node) {
            return way.getWayArea().getStartPair(tm)[0];
        }
        //TODO index 0 durefte nicht immer stimmen
        return way.getWayArea().getEndPair()[0];
    }

    public boolean hasBridge() {
        for (SceneryWayObject way : ways) {
            if (way instanceof BridgeOrTunnel) {
                return true;
            }
        }
        return false;
    }

    public int getWaysCount() {
        return ways.size();
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
        // one minor way hitting a major way on an outer node (not inner node). Will have no own polygon.
        // 3 Ways, von denen einer ein "minor Way" ist (keine Mittelline haben soll). Aber mehrspurige in A-Kreuzen?
        // Die anderen gelten als fortlaufender main Way.
        // Der Connector selber bekommt kein Polygon.
        // Z.B. die Rechtsabbieger short cut Spuren in manchen Kreuzungen.
        SIMPLE_SINGLE_JUNCTION,
        // one minor way hitting a major way on an inner node (not outer node). Will have no own polygon.
        // der minor soll keine Mittelline haben. Aber mehrspurige in A-Kreuzen?
        // Z.B. die Rechtsabbieger short cut Spuren in manchen Kreuzungen.
        SIMPLE_INNER_SINGLE_JUNCTION,
        //same as SIMPLE_INNER_SINGLE_JUNCTION but with a second minor on the opposite side of main. No own polygon.
        SIMPLE_INNER_DOUBLE_JUNCTION,
        // three ways, all ending/starting at connector. Zwei davon sind ein Hauptweg, damit Skizze 68 greifen kann.
        // der minor Way soll aber nicht wirklich minor sein, sonst ist das der Typ SIMPLE_SINGLE_JUNCTION
        STANDARD_TRI_JUNCTION,
        // Beginn einer Autobahnauffahrt (z.B. 1353883890).
        // two quite parallel minor ways hitting a major way on an outer node (not inner node).
        MOTORWAY_ENTRY_JUNCTION,
        //Zerbrastreifen or similar, ansonsten wie SIMPLE_CONNECTOR; exakt two ways
        //Eigentlich ist das eher ein zusätzliches Attribut als ein eigenständiger Connectortype. An 54289952 kommt einfach ein Radweg(?) auf eine Strasse mit Fussgaengerampel.
        //an 388796251 kreuzt ein Fussweg. Mal attribut isCrossing
        //CROSSING,
        //generic connector with n nodes. Polygon with area.
        GENERIC;


    }


}

@FunctionalInterface
interface PermutationHandler {
    boolean run(int[] order);
}

