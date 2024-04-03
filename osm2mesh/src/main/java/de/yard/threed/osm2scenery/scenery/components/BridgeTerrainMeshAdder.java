package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.modules.BridgeModule;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.scenery.BridgeGap;
import de.yard.threed.osm2scenery.scenery.BridgeSideRamp;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;

public class BridgeTerrainMeshAdder implements TerrainMeshAdder {
    Logger logger = Logger.getLogger(BridgeTerrainMeshAdder.class);
    BridgeGap bridgeGap;

    /**
     * Kann nicht an Bridge, weil die kein Supplement ist und der adder damit nicht oder zu frueh aufgerufen wird.
     * Ist darum im BridgeGap.
     *
     * @param bridgeGap
     */
    public BridgeTerrainMeshAdder(BridgeGap bridgeGap) {
        this.bridgeGap = bridgeGap;
    }

    @Override
    public void addToTerrainMesh(AbstractArea[] areas, TerrainMesh tm) {
        if (!SceneryBuilder.FTR_SMARTBG) {
            // return;
        }
        if (bridgeGap.bridge.mapWay.getOsmId() == 2345485946L) {
            int h = 9;
        }

        if (SceneryBuilder.TerrainMeshDebugLog) {
            logger.debug("Adding bridge to mesh: " + bridgeGap.bridge.getOsmIdsAsString());
        }
        addRamps(bridgeGap.bridge.startHead, tm);
        addRamps(bridgeGap.bridge.endHead, tm);
        if (!bridgeGap.isEmpty(tm)) {
            //might be empty due to overlap resolving.
            if (bridgeGap.getArea().length != 2) {
                logger.error("ignoring bridge gap filler with size " + bridgeGap.getArea().length);
                return;
            }
            for (int i = 0; i < 2; i++) {
                AbstractArea gapArea = bridgeGap.getArea()[i];
                LineString gapLine = gapArea.getPolygon(tm).getExteriorRing();
                BridgeModule.BridgeHead head = bridgeGap.bridge.startHead;
                int[] fromto = JtsUtil.findCommon(gapLine, JtsUtil.createLine(head.bridgebaseline.p0, head.bridgebaseline.p1));
                if (fromto == null) {
                    head = bridgeGap.bridge.endHead;
                    fromto = JtsUtil.findCommon(gapLine, JtsUtil.createLine(head.bridgebaseline.p0, head.bridgebaseline.p1));
                }
                if (fromto == null) {
                    logger.error("fromto still null");
                    return;
                }
                LineString[] result = JtsUtil.removeCoordinatesFromLine(gapLine, fromto);
                tm.createMeshPolygon(new ArrayList(Arrays.asList(result)), head.sharesForGap, gapArea.getPolygon(tm), gapArea);
            }
        }
    }

    private void addRamps(BridgeModule.BridgeHead head, TerrainMesh tm) {
        head.sharesForGap = new ArrayList<>();
        // die left und right line des Ways an diesem Head.
        MeshLine[] lines = head.getConnectedWayLines(tm);
        if (lines == null || lines.length != 2) {
            logger.error("inconsistent bridgehead");
            return;
        }
        MeshLine lineToGap = null;
        if ((lineToGap = addRamp(head.ramp0, lines, tm)) != null) {
            head.sharesForGap.add(lineToGap);
        }
        if ((lineToGap = addRamp(head.ramp1, lines, tm)) != null) {
            head.sharesForGap.add(lineToGap);
        }
        //die roadline ist ja an der selben Stelle "above". Uih.
        MeshLine roadlineOnGround = tm.findLineBetweenExistingPoints(new CoordinatePair(head.ramp0.roadpoint, head.ramp1.roadpoint));
        head.sharesForGap.add(roadlineOnGround);

    }

    /**
     * Aus den beiden Lines des connect way an dem Head die Line ermitteln, die auf der Seite der Ramp liegt
     * und die dann splitten.
     *
     * @param connectWayLines
     * @return
     */
    private MeshLine[] splitLineAtRamp(BridgeSideRamp ramp, MeshLine[] connectWayLines, TerrainMesh tm) {

        MeshLine[] result;
        int index = connectWayLines[0].findCoordinate(ramp.backpoint);
        if (index == -1) {
            index = connectWayLines[1].findCoordinate(ramp.backpoint);
            if (index == -1) {
                //das kann ein Hinweis auf einen nicht sauberen Split an dem Way sein. Gabs schon mal bei 225794249
                logger.error("ramp.backpoint on no way line:" + ramp.backpoint);
                SceneryContext.getInstance().errorCounter++;
                return null;
            }
            result = tm.split(connectWayLines[1], index);
        } else {
            result = tm.split(connectWayLines[0], index);
        }
        return result;
    }

    /**
     * Returns share with gap.
     */
    private MeshLine addRamp(BridgeSideRamp ramp, MeshLine[] connectWayLines, TerrainMesh tm) {
        if (ramp.isEmpty(tm)) {
            return null;
        }


        MeshLine share;

        MeshNode roadpoint = tm.getMeshNode(ramp.roadpoint);
        MeshLine[] splitresult = splitLineAtRamp(ramp, connectWayLines, tm);
        if (splitresult == null) {
            logger.warn("ignoring ramp for terrain mesh");
            return null;
        }
        MeshNode backpoint = tm.getMeshNode(ramp.backpoint);

        //das split Resultat ist wegen der Richtungen nicht eindeutig. Besser am road/backpoint weitermachen
        //und dann die Line des split suchen. Daran dann die beiden anderen Lines der Ramp haengen.
        AbstractArea area = ramp.getArea()[0];
        for (MeshLine line : roadpoint.getLines()) {
            if (line.getTo() == roadpoint && line.getFrom() == backpoint) {
                if (line.getLeft() == null) {
                    line.setLeft(area);
                    share = tm.registerLine(JtsUtil.createLine(ramp.roadpoint, ramp.outerpoint), area, null);
                    tm.registerLine(JtsUtil.createLine(ramp.outerpoint, ramp.backpoint), area, null);
                } else {
                    line.setRight(area);
                    share = tm.registerLine(JtsUtil.createLine(ramp.roadpoint, ramp.outerpoint), null, area);
                    tm.registerLine(JtsUtil.createLine(ramp.outerpoint, ramp.backpoint), null, area);
                }
                return share;
            }
            if (line.getFrom() == roadpoint && line.getTo() == backpoint) {
                if (line.getLeft() == null) {
                    line.setLeft(area);
                    share = tm.registerLine(JtsUtil.createLine(ramp.roadpoint, ramp.outerpoint), null, area);
                    tm.registerLine(JtsUtil.createLine(ramp.outerpoint, ramp.backpoint), null, area);
                } else {
                    line.setRight(area);
                    share = tm.registerLine(JtsUtil.createLine(ramp.roadpoint, ramp.outerpoint), area, null);
                    tm.registerLine(JtsUtil.createLine(ramp.outerpoint, ramp.backpoint), area, null);
                }
                return share;
            }
        }
        logger.error("should not be reached!");
        return null;
    }
}
