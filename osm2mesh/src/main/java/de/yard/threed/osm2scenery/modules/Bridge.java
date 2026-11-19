package de.yard.threed.osm2scenery.modules;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.ElevationMap;
import de.yard.threed.osm2scenery.modules.common.BridgeOrTunnel;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshPolygonOld;
import de.yard.threed.osm2scenery.scenery.BridgeGap;
import de.yard.threed.osm2scenery.scenery.BridgeSideRamp;
import de.yard.threed.osm2scenery.scenery.LandClass;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import de.yard.threed.osm2scenery.util.TagMap;
import de.yard.threed.osm2world.*;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static de.yard.threed.osm2world.GeometryUtil.*;
import static de.yard.threed.osm2world.Materials.GRASS;
import static de.yard.threed.osm2world.WorldModuleGeometryUtil.createTriangleStripBetween;
import static de.yard.threed.osm2world.WorldModuleGeometryUtil.filterWorldObjectCollisions;


/**
 * A bridge. No longer a module (27.5.19: bridges are built during way(road) processing).
 * See BridgeOrTunnel.
 *
 */
@Slf4j
public class Bridge extends BridgeOrTunnel
        /*implements RenderableToAllTargets*/ {

    public static final boolean isBridge(TagGroup tags) {
        return tags.containsKey("bridge")
                && !"no".equals(tags.getValue("bridge"));
    }

    public static final boolean isBridge(MapWaySegment segment) {
        return isBridge(segment.getTags());
    }

    public static final double BRIDGE_UNDERSIDE_HEIGHT = 0.2f;

    //public List<SceneryWayObject> ramps=new ArrayList<>();
    ScenerySupplementAreaObject groundfiller;
    //13.7.19 Warum sollte eine Brücke den Untergrund kennen. Führt nur zu doofen
    // Abhaengigkeiten private List<SceneryFlatObject> belows;
    public ScenerySupplementAreaObject gap;
    //public MapWay shadowway;
    public BridgeHead startHead, endHead;

    public Bridge(MapWaySegment2 mapWay, TagMap materialmap/*SceneryWayObject roadorrailway/*MapWay mapWay/*MapWaySegment segment,
            AbstractNetworkWaySegmentWorldObject primaryWO*/, LandClass landClass, SceneryContext sceneryContext) {
        //TODO category kann auch Railway sein.
        super("Bridge", mapWay, materialmap, SceneryObject.Category.ROAD/*BRIDGE*/, landClass, sceneryContext);
        //super("Bridge", roadorrailway, Category.BRIDGE/*mapWay/*segment, primaryWO*/);
        //shadowway = MapDataHelper.createShadowMapWay(roadorrailway.mapWay);

        //Es gibt ueber den Way schon einen adder, der wird für Bridges aber ja nicht gebraucht weil kein TerrainProvider.
        //trotzdem kann der adder nicht hierhin, weil dies kein Supplement ist und der adder damit nicht oder zu frueh aufgerufen wird.
        //BridgeGap hat den.
        //terrainMeshAdder=new BridgeTerrainMeshAdder(this);
    }

    /*@Override        public List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds){        }*/

    /**
     * 18.11.26 depends on bridge type
     */
    public void createSupplements() {
        //bridge heads need clipped ways
        // Ramp. The polygon of the approach road isType primary, so exists.
        /*18.11.26 SceneryWayObject wayAtNode = getConnectedRoad(getStartNode());
        startHead = null;//9.3.26 new BridgeHead(this, getStartNode(), wayAtNode,getStartConnector());
        wayAtNode = getConnectedRoad(getEndNode());
        endHead = null;//9.3.26 new BridgeHead(this, getEndNode(), wayAtNode,getEndConnector());

        gap = closeBridgeGap();
        startHead.ramp0 = new BridgeSideRamp("BridgeSideRamp", startHead, GRASS, this/*roadorrailway* /.mapWay.getStartNode(), true);
        startHead.ramp0.name = "ramp0";
        startHead.ramp1 = new BridgeSideRamp("BridgeSideRamp", startHead, GRASS, this/*roadorrailway* /.mapWay.getStartNode(), false);
        startHead.ramp1.name = "ramp1";
        endHead.ramp0 = new BridgeSideRamp("BridgeSideRamp", endHead, GRASS, this/*roadorrailway* /.mapWay.getEndNode(), true);
        endHead.ramp0.name = "ramp2";
        endHead.ramp1 = new BridgeSideRamp("BridgeSideRamp", endHead, GRASS, this/*roadorrailway* /.mapWay.getEndNode(), false);
        endHead.ramp1.name = "ramp3";*/
    }

    /*@Override
    public GroundState getGroundState() {
        return GroundState.ABOVE;
    }*/
    
		/*@Override
		public Iterable<EleConnector> getEleConnectors() {
			// TODO EleConnectors for pillars
			return super.getEleConnectors();
		}*/

    //@Override
    public void renderTo(Target<?> target) {

        drawBridgeUnderside(target);

        drawBridgePillars(target);

    }

    private void drawBridgeUnderside(Target<?> target) {

        List<VectorXYZ> leftOutline = primaryRep.getOutline(false);
        List<VectorXYZ> rightOutline = primaryRep.getOutline(true);

        List<VectorXYZ> belowLeftOutline = sequenceAbove(leftOutline, -BRIDGE_UNDERSIDE_HEIGHT);
        List<VectorXYZ> belowRightOutline = sequenceAbove(rightOutline, -BRIDGE_UNDERSIDE_HEIGHT);

        VectorXYZList strip1 = createTriangleStripBetween(
                belowLeftOutline, leftOutline);
        VectorXYZList strip2 = createTriangleStripBetween(
                belowRightOutline, belowLeftOutline);
        VectorXYZList strip3 = createTriangleStripBetween(
                rightOutline, belowRightOutline);

        target.drawTriangleStrip(Materials.BRIDGE_DEFAULT, strip1, null, null);
        target.drawTriangleStrip(Materials.BRIDGE_DEFAULT, strip2, null, null);
        target.drawTriangleStrip(Materials.BRIDGE_DEFAULT, strip3, null, null);

    }

    private void drawBridgePillars(Target<?> target) {

        List<VectorXZ> pillarPositions = GeometryUtil.equallyDistributePointsAlong(
                2f, false,
                primaryRep.getStartPosition(),
                primaryRep.getEndPosition());

        //make sure that the pillars doesn't pierce anything on the ground

        Collection<WorldObject> avoidedObjects = new ArrayList<WorldObject>();

        /*for (MapIntersectionWW i : segment.getIntersectionsWW()) {
            for (WorldObject otherRep : i.getOther(segment).getRepresentations()) {

                if (otherRep.getGroundState() == GroundState.ON
                        && !(otherRep instanceof Water || otherRep instanceof Waterway) //TODO: choose better criterion!
                        ) {
                    avoidedObjects.add(otherRep);
                }

            }
        }*/

        filterWorldObjectCollisions(pillarPositions, avoidedObjects);

        //draw the pillars

        for (VectorXZ pos : pillarPositions) {
            drawBridgePillarAt(target, pos);
        }

    }

    private void drawBridgePillarAt(Target<?> target, VectorXZ pos) {

        /* determine the bridge elevation at that point */

        VectorXYZ top = null;

        List<VectorXYZ> vs = primaryRep.getCenterline();

        for (int i = 0; i + 1 < vs.size(); i++) {

            if (isBetween(pos, vs.get(i).xz(), vs.get(i + 1).xz())) {
                top = interpolateElevation(pos, vs.get(i), vs.get(i + 1));
                break;
            }

        }

        /* draw the pillar */

        // TODO: start pillar at ground instead of just 100 meters below the bridge
        target.drawColumn(Materials.BRIDGE_PILLAR_DEFAULT, null,
                top.addY(-100),
                100,
                0.2, 0.2, false, false);

    }

    /**
     * Sicherstellen, dass es eine obere und untere Elevation gibt.
     * "roadorrailway" wird wohl direkt als Road gefixed.
     */
    public void fixElevation() {
        //TODO 28.8.18: average ist doch wohl nicht gut?a
        double average = ElevationMap.getInstance().getAverage();
        double elevation = average +/*3*/6;
        /*18.11.26for (EleConnectorGroup eleConnectorGroup : getEleConnectorGroups().eleconnectorgroups) {
            eleConnectorGroup.fixElevation(elevation);
            ElevationMap.getInstance().fix(eleConnectorGroup);
        }*/
        //das überbrückte Element etwas tiefer? Naja, irgendwann mal

        //aber die Rampen an der Bridge erhöhen.
        /*28.9.18: Das sind aber doch genau die Groups der Bridge selber, und die wurden oben ja schon gesetzt. Darum brauchts das nicht mehr.
        EleConnectorGroupSet eleConnectorGroupSet = ((RoadModule.Road) roadorrailway).getConnectedElevations();
        for (EleConnectorGroup ramp : eleConnectorGroupSet.eleconnectorgroups) {
            if (ramp.groundState != GroundState.ABOVE) {
                log.warn("groundstate != ABOVE. Elevation will be wrong");
            }
            ramp.setElevation(elevation);
            ElevationMap.getInstance().fix(ramp);
        }*/
    }

    /**
     *
     */
    /*3.6.19 @Override
    public void/*EleConnectorGroupSet* / prepareElevations() {
        //im finalize roadorrailway.elevations = roadorrailway.prepareElevations();
        //einfch die uebernehmen und hochsetzen

    }*/
    public SceneryObject getRoad() {
        return null;//this/*roadorrailway*/;
    }

    public SceneryObject getGroundFiller() {
        return groundfiller;
    }

    /**
     * Close ground under bridge between sroad and eroad.
     * * 14.8.18: AuchLuecke unter Brücke schliessen um Holes im Backgroudn zu vermeiden. Erstmal nur so.
     * Aber nicht als "Raod", weil das dann auch ein Graph wird.
     * 30.8.18: sroad und eroad muss vorher gesetzt worden sein. Weil er selber Polygons der Road braucht, aber ein reguläres Area Objekt
     * ist, muss er selber sein Polygone rzeugen.
     * <p>
     * 30.8.18: Splitted wie andere auch.
     *
     * @return
     */
    public ScenerySupplementAreaObject closeBridgeGap() {


        groundfiller = new BridgeGap("BridgeGroundFiller", this, GRASS/*hadowway roadorrailway*/);
        //groundfiller.createPolygon();
        //groundfiller.prepareElevationGroups();
        return groundfiller;
    }

    private LineSegment createGapLine(List<Coordinate> coors) {
        LineSegment line = JtsUtil.createLineSegment(coors.get(0), coors.get(1));
        line = JtsUtil.extendLineSegment2(line, 1.5f);
        return line;
    }
    /*13.7.19 Warum sollte eine Brücke den Untergrund kennen. Führt nur zu doofen
    // Abhaengigkeiten
    public void addBelow(SceneryFlatObject area) {
        if (belows == null) {
            belows = new ArrayList<>();
        }
        belows.add(area);
    }*/

    public double getWidth() {
        //return roadorrailway.getWidth();
        return 0;//super.getWidth();
    }

    public static class BridgeHead {
        //true, wenn der Way dort zu Ende ist und wiederum eine
        //Bridge anschliesst. Dann koennen ja nicht beide eine Standardramp bekommen. Wird generell gesetzt, wenn der Way zu kurz ist.
        //Auch eine Junction wird die Ramp behindern. Nee, besser doch nur bei "bridge again".
        public boolean connectedWayTooShortForRamp = false;
        public BridgeSideRamp ramp0, ramp1;
        public CoordinatePair roadLine;
        public LineSegment bridgebaseline;
        public CoordinatePair backline;
        public Bridge bridge;
        SceneryWayObject connectedWayAtNode;
        public MapNode headNode;
        public List<MeshLine> sharesForGap;

        BridgeHead(Bridge bridge, MapNode headNode, SceneryWayObject connectedWayAtNode, SceneryWayConnector connector) {
            this.bridge = bridge;
            this.headNode = headNode;
            this.connectedWayAtNode = connectedWayAtNode;
            if (connectedWayAtNode == null) {
                log.error("no way at node.");
                return;// null;
            }

            CoordinatePair/*19.3.26 []*/ pairs = connectedWayAtNode.getPairRelatedFromNode(headNode, 0);
            /*19.3.26 if (pairs.length > 1) {
                // minor way direkt am start? strange. Aber wir mchen mal weiter
                log.warn("minor way direkt am start? strange.");
            }*/
            roadLine = pairs;//[0];

            LineSegment ls = new LineSegment(roadLine.left(), roadLine.right());
            bridgebaseline = JtsUtil.extendLineSegment2(ls, 1.5);
            pairs = connectedWayAtNode.getPairRelatedFromNode(headNode, 1);
            /*19.3.26 if (pairs.length > 1) {
                // minor way da wo die Ramp starten soll. Naja. mal versuchen.
                // wenn das auf der anderen Seite ist, dürfte es kein Problem sein.
                // Auf Seite des minor muesste die ramp später als overlaps empty werden.
                log.debug("minor way at ramp start.");
            }*/
            //die order ist getauscht, so dass index 0 immer Richtung headnode zeigt.
            backline = pairs;//[0];
            /*9.3.26 if (connectedWayAtNode.getWayArea().getLength() == 2 && connectedWayAtNode.getOppositeConnector(connector) != null && connectedWayAtNode.getOppositeConnector(connector).hasBridge()) {
                connectedWayTooShortForRamp = true;
            }*/

        }

        /**
         * left/right ist aus Perspetive der Bridge, nicht des Way!
         *
         * @return
         */
        /*public MeshLine getConnectedWayRamp0() {
            return getConnectedWayAtRamp(true);
        }

        public MeshLine getConnectedWayRamp1() {
            return getConnectedWayAtRamp(false);
        }*/

        /**
         * Liefert die left und right line des Ways an diesem Head.
         * TODO Den Code oder Teile kann man bestimmt woanders noch brauchen
         *
         * @return
         */
        public MeshLine[] getConnectedWayLines(TerrainMesh tm) throws MeshInconsistencyException {
            if (connectedWayAtNode == null || connectedWayAtNode.getWayArea() == null) {
                log.error("huch");
                return null;
            }
            if (connectedWayAtNode.mapWay.getOsmId() == 225794249) {
                int h = 9;
            }
            //mal eine Konsistenzpruefung eingestreut.
            MeshPolygonOld mp = null;//2.5.24tm.getPolygon(connectedWayAtNode.getWayArea());
            if (mp == null) {
                log.error("inconsistent way?");
            }
            List<MeshLine> rl = null;//16.4.26 connectedWayAtNode.getWayArea().getRightLines(tm);
            List<MeshLine> ll = null;//16.4.26 connectedWayAtNode.getWayArea().getLeftLines(tm);
            if (rl == null || ll == null) {
                log.error("huch");
                return null;
            }
            if (connectedWayAtNode.getStartNode() == headNode) {

                return new MeshLine[]{ll.get(0), rl.get(0)};
            } else {

                return new MeshLine[]{rl.get(rl.size() - 1), ll.get(ll.size() - 1)};
            }
        }

    }

    public enum BridgeType {
        // without ramp, eg. over small streams
        PLAIN,
        // a bridge in quite plain area mit ramps for moving up/down
        STANDARD
    }
}
