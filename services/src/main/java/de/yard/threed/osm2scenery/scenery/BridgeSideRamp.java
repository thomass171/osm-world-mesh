package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2graph.osm.CoordinateList;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.MapDataHelper;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleCoordinate;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.modules.BridgeModule;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2world.*;

import java.util.List;

/**
 * Skizze 69
 */
public class BridgeSideRamp extends ScenerySupplementAreaObject {
    BridgeModule.BridgeHead bridgeHead;
    MapNode rampNode = null;
    boolean onLeft;
    //Die additional outside Coordinate der Ramp.
    Coordinate additional = null;
    public Coordinate backpoint, outerpoint, roadpoint;

    /**
     * BridgeSideRamp
     */
    public BridgeSideRamp(String creatortag, BridgeModule.BridgeHead bridgeHead, Material material, MapNode rampNode, boolean onLeft) {
        super(creatortag, material, null);
        this.bridgeHead = bridgeHead;
        this.rampNode = rampNode;
        this.onLeft = onLeft;
    }

    /**
     * Hat keine eigene Elegroup.
     * Bekommt spaeter drei fremde.
     */
    @Override
    public void buildEleGroups() {
        elevations = new EleConnectorGroupSet();
    }

    /**
     * Den Polygon anhand der bekannten Bridge Referenzpunkte erstellen.
     * Noch keine Overlaperkennung.
     */
    @Override
    public List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds) {

        if (bridgeHead == null || bridgeHead.backline == null) {
            logger.error("incomplete bridge head");
            return null;
        }


        CoordinateList cl = new CoordinateList();
        if (onLeft) {
            backpoint = bridgeHead.backline.left();
            outerpoint = bridgeHead.bridgebaseline.p0;
            roadpoint = bridgeHead.roadLine.left();
            cl.add(bridgeHead.backline.left());
            cl.add(bridgeHead.roadLine.left());
            cl.add(bridgeHead.bridgebaseline.p0);
            cl.add(bridgeHead.backline.left());
        } else {
            backpoint = bridgeHead.backline.right();
            outerpoint = bridgeHead.bridgebaseline.p1;
            roadpoint = bridgeHead.roadLine.right();
            cl.add(bridgeHead.backline.right());
            cl.add(bridgeHead.roadLine.right());
            cl.add(bridgeHead.bridgebaseline.p1);
            cl.add(bridgeHead.backline.right());
        }
        if (bridgeHead.connectedWayTooShortForRamp){
            //erst jetzt pruefen, weil die Refernzpunkte trotzdem gesetzt werden sollen.
            logger.debug("connectedWayTooShortForRamp at head"+bridgeHead.headNode.getOsmId());
            flatComponent=new AbstractArea[]{AbstractArea.EMPTYAREA};

        }else {

            flatComponent[0].poly = MapDataHelper.createSmartPolygon(cl);
            flatComponent[0].parentInfo = "bridge ramp to outer " + outerpoint;
            //Die Groups der ersten beiden Coordinates sind schon implizit gesetzt, weil die Coordinates an einem Way liegen. Die dritte
            //kommt an die filler group.
            //24.4.19: das ist doch zu früh, darum gibts auch immer ein Warning. Und warum sollten hier ueberhaupt Elegroups geadded werden. Mal weglassen.
            additional = /*9.9.19 new EleCoordinate*/(cl.get(2));
            //elevations.add(EleConnectorGroup.getGroup(cl.get(0), false, "for bridge ramp"));
            //elevations.add(EleConnectorGroup.getGroup(cl.get(1), false, "for bridge ramp"));
            //elevations.add(EleConnectorGroup.getGroup(cl.get(2), false, "for bridge ramp"));
        }
        return null;
    }

    /**
     * eine Standard area reduce kommt hier nicht in Frage, weil die Verbindung zum Way erhalten werden muss.
     * Man koennte den outer point aber nach innen ziehen. Dann verkleinert sich aber auch der Gap.
     * Das muesste innerhalb der Bridge abgestimmt werden.
     *
     */
    @Override
    public void resolveSupplementOverlaps(List<SceneryFlatObject> overlaps){
        int h=9;
        // das kann man bestimmt auch eleganter lösen.
        logger.debug("setting overlapping bridge ramp to empty for bridge"+bridgeHead.bridge.mapWay.getOsmId());
        flatComponent[0]= AbstractArea.EMPTYAREA;
    }

    /**
     * Geht ueber den BridgeTerrainMeshAdder in SceneryFlatObject. Methode ist hier nur zur Doku dass das so ist.
     */
    @Override
    public void addToTerrainMesh() {
        super.addToTerrainMesh();
    }

    @Override
    public void registerCoordinatesToElegroups() {
        if (additional == null) {
            //dann ist nichts weiter zu registrieren. Ansonsten aber wohl auch nicht(??)
            return;
        }
        //der additional muss an den gap, weil er ja genauso tief liegt.
        //aber der ist wohl schon ueber den Gap registriert. Darum nicht mehr. Aber der gap koennte empty sein. Also doch
        //evtl. hier registrieren. aber trotzdem an den gap.
        //bridgeHead.bridge.gap.getEleConnectorGroups().eleconnectorgroups.get(0).add(additional);
        //bridgeHead.bridge.getEleConnectorGroup(bridgeHead.headNode).add(additional);
        if (EleConnectorGroup.getGroup(additional)==null){
            bridgeHead.bridge.gap.getEleConnectorGroups().eleconnectorgroups.get(0).add(new EleCoordinate(additional));
        }
    }
}