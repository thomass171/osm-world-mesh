package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.elevation.EleCoordinate;
import de.yard.threed.osm2scenery.modules.BridgeModule;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.BridgeTerrainMeshAdder;
import de.yard.threed.osm2scenery.util.SmartPolygon;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.Material;

import java.util.List;

/**
 * The gap under a bridge.
 * 13.7.19: Der wird sich doch mit dem overlappen, was eh unter der Bridge ist. TODO
 *
 * <p>
 * Created on 15.08.18.
 */
public class BridgeGap extends ScenerySupplementAreaObject {
    Polygon basepolygon;
    //SceneryWayObject roadorrailway;
    //MapWay shadowway = null;
    public BridgeModule.Bridge bridge;
    MapNode rampNode = null;
    boolean onLeft;

    /**
     * Constructor uer gap unter einer Bruecke. Weil die MapNodes des OSM way fuer den Oberteil verwendet werden, lege ich Dummynodes an,
     * die an selben Stellen liegen aber mit eigenen Elegroups.
     * NeeNeeNee, den Spuk lass ich mal.
     */
    public BridgeGap(String creatortag, BridgeModule.Bridge bridge, Material material/*,SceneryWayObject roadorrailway*/) {
        super(creatortag, material, null);
        this.bridge = bridge;
        //basepolygon=polygon;
        //this.roadorrailway = roadorrailway;
        //bridgegap macht terrain mesh für alle Teile
        terrainMeshAdder = new BridgeTerrainMeshAdder(this);
    }

    @Override
    public void buildEleGroups() {
        //erstmal einfach eine einzelne Group ohne mapnode
        elevations = new EleConnectorGroupSet();
        //5.9.19: das ist doch asbach? Nein, gap muesste eigentlich sogar zwei haben, weil es nicht wie die Bridge ABOVE liegt.
        //doof das man keine mapnode reintun kann, denn die liegt ja schon ABOVE. TODO: da brauchts noch eine gute Idee.
        EleConnectorGroup egr = new EleConnectorGroup(null);
        //TODO location von above group nehmen
        egr.gridlocation= EleConnectorGroup.Location.INSIDEGRID;;
        getEleConnectorGroups().eleconnectorgroups.add(egr);

        // Dummynodes sind doof weil die GridEnter vorbehalten sind.
        // Einfach die Nodes von oben leihen. Das geht auch nicht, weil dann Groups an den Rampen doppelt sind.
        //einfach hier  mal ganz lassen
        /*for (int i = 0; i < roadorrailway.mapWay.getMapNodes().size(); i++) {
            MapNode node = roadorrailway.mapWay.getMapNodes().get(i);

            EleConnectorGroup eleConnectorGroup = getEleConnectorGroup(node);
            elevations.add(eleConnectorGroup);
        }*/

    }

    /**
     * 30.8.18: Da ich keine angrenzenden Polygon mehr kenne,...
     * Lege ich einen normal via outline an.
     * 27.8.19: der overlaps aber mit den anderen Ways. Stattdessen lieber die baselines verbinden.
     */
    @Override
    public List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds, TerrainMesh tm, SceneryContext sceneryContext) {
        //Gap Filler
        //jeweils ein Meter weiter auseinander, weil die SideRamp daran kommt.
        //double width = bridge.getWidth() + 2;
        //flatComponent[0].poly = MapDataHelper.getOutlinePolygon(bridge./*.roadorrailway*/mapWay.getMapNodes(), width);
        if (bridge.startHead.bridgebaseline == null || bridge.endHead.bridgebaseline == null) {
            logger.error("no gap baseline");
            flatComponent[0] = AbstractArea.EMPTYAREA;
            return null;
        }
        Polygon polygon = JtsUtil.createPolygonFromLines(bridge.startHead.bridgebaseline, bridge.endHead.bridgebaseline);
        if (polygon == null) {
            logger.error("no gap polygon");
            flatComponent[0] = AbstractArea.EMPTYAREA;
            return null;
        }
        flatComponent[0].poly = new SmartPolygon(polygon);

        boolean wasCut = super.cutArea0(gridbounds);
        if (wasCut) {
            logger.error("cut gap filler unhandled");
        }

        /*7.9.19 extrahiert nach resolveSupplementOverlaps()
        if (isTerrainProvider() && objects != null) {

            OverlapResolver.resolveOverlaps(this, objects, bridge.mapWay.getOsmId());
            logger.debug("resolveOverlaps returned " + getArea().length + " areas.");
        }*/
        //6.8.19: Stimmt das wohl so im Context? Ja. 7.9.19:Warum? clip hat nichts mehr mit resolveOverlap zu tun.
        isClipped = true;

        return null;
    }

    /**
     * eine Standard area reduce kommt hier eigentlich nur für split, aber nicht reduce, in Frage, weil die Verbindung zum der Gap zum Head erhalten werden muss.
     * Der Split aus dem Resolver ist ganz gut. Etwas schwer zu entscheiden, wenn es mehrere Overlaps gibt.
     * Man koennte den outer point aber nach innen ziehen. Dann verkleinert sich aber auch die Ramp.
     * Das muesste innerhalb der Bridge abgestimmt werden.
     */
    @Override
    public void resolveSupplementOverlaps(List<SceneryFlatObject> overlaps, TerrainMesh tm) {
        if (overlaps.size() == 1) {
            //mal annehmen, dass das der Overlap mit einem Way unter der Brücke ist. Da hilt dann ein Split.
            OverlapResolver.resolveTerrainOverlaps(this, overlaps, bridge.mapWay.getOsmId(), tm);
            logger.debug("resolveOverlaps returned " + getArea().length + " areas.");
        } else {
            // das kann man bestimmt auch eleganter lösen.
            logger.debug("setting overlapping bridge gap filler to empty for bridge" + bridge.mapWay.getOsmId());
            flatComponent = new AbstractArea[]{AbstractArea.EMPTYAREA};
        }
    }

    /**
     * 5.9.19: Jetzt einfach mal an eine Node der Bridge. TODO: Die muessen aber an beide seiten
     * verteilt werden, auch an überbrückte Objekt.
     */
    @Override
    public void registerCoordinatesToElegroups(TerrainMesh tm) {
        /*      if (flatComponent[0] != null && flatComponent[0].poly != null && flatComponent[0].poly.polygonMetadata != null) {
            List<EleCoordinate> els = flatComponent[0].poly.polygonMetadata.getEleConnectors(null);
            // der Einfachheit halber nur eine Group. Und die ist ohne mapnode Zuordnung, um Shadownodes zu vermeiden. Zumindest vorerst.
            EleConnectorGroup egr = getEleConnectorGroups().eleconnectorgroups.get(0);
            egr.addAll(els);
            //da kommen spaeter noch Ramps, cut und BG dran.
            //egr.locked = true;
        }*/

        //dann geht das doch ueber den Default. Den gibts hier aber nicht.
        //TODO: gibt es das nicht als OOTB Default?

        if (!isEmpty(tm)) {
            EleConnectorGroup egr = getEleConnectorGroups().get(0);
            for (AbstractArea aa : getArea()) {
                for (Coordinate c : aa.getPolygon(tm).getCoordinates()) {
                    //manche wird es schon gebe, also eigentlich genau 4.
                    if (EleConnectorGroup.getGroup(c) == null) {
                        egr.add(new EleCoordinate(c));
                    }
                }
            }
        }
        //da kommen spaeter noch Ramps, cut und BG dran.
        //egr.locked = true;

    }

}
