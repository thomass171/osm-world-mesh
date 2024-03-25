package de.yard.threed.osm2scenery.modules.common;

import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.util.TagMap;
import de.yard.threed.osm2world.*;


/**
 * common superclass for bridges and tunnels
 * 1.8.18: Ist zunächtst mal das (Bruecken)bauwerk, hat aber eine Road/Railway zur Fortsetzung der angrenzenden Ways.
 * Das Road/Railway Segment wird aber nicht in den BG cut.
 * <p>
 * 1.8.18: Ist zwar im Grunde ein GraphBasedObject, aber kein SceneryAreaObject.
 * Auch eine Brücke braucht zwar eine Fahrbahn. Trotzdem hat eine Bridge ein Volumen.
 * Das mit der Ableitung ist noch nicht rund. Aber es ist doch ein SceneryWay,
 * der halt in einem Volumen ist.
 * Es ist doch so vieles gleich. Darum die Area fuer eine Brücke als Overlay erzeugen.
 * Und damit muesste das hier eine Ableitung von Road, Railway o.ae sein.
 * 11.4.19: Das mit Volumen scheint fragwürdig, aber Overlay könnte Sinn machen. Aber: nicht zu dicht wegen Z-Fighting in 3D!!
 *
 * 3.6.19: Ist jetzt (statt als roadorrailway zu enthalten), der Way über/unter. Die Area selber ist als Overlay (obwohl das zu hoch fuer Pverlay ist) markiert und das
 * Bauwerk ein (optionales) VolumeComponent. Kein TerrainProvider, weil es nicht zum Mesh beiträgt.
 * 27.8.19: Kein Overlay mehr. Das ist einfach ein SceneryObject.
 * 12.9.19: Die Ableitung von Highway ist doch unpassend, Railways gehen auch durch Tunnel/Bridges. Sollte es nicht besser eine Component sein?
 * Es bleibt ja im Grunde ein Highway mit Lanes, Bürgersteig, etc. Undf auch eine Railway mit all ihren Eigenschaften.
 */
public abstract class BridgeOrTunnel extends HighwayModule.Highway/*SceneryVolumeOverlayObject/*SceneryWayObject/*implements WaySegmentWorldObject*/ {

    //protected MapWaySegment segment;

    protected AbstractNetworkWaySegmentWorldObject primaryRep;
    //17.8.18: Das ist hier nur eine Referenz. Das Objekt liegt auch in der globalen Liste.
    //Wird auch ueber die globale Liste gerendered.
    //3.6.19: Bridge IST jetzt Highway, statt ihn zu enthalten.
    //public SceneryWayObject roadorrailway;
   public SceneryWayObject sroad;
    public SceneryWayObject eroad;

    public BridgeOrTunnel(String creatortag, MapWay mapWay, TagMap materialmap, Category category/*/* SceneryWayObject roadorrailway,* / /*MapWay mapWay/*MapWaySegment segment,
                          AbstractNetworkWaySegmentWorldObject primaryRepresentation*/) {
        //super(creatortag, category);
        super(mapWay,materialmap);
        this.creatortag=creatortag;
        //this.roadorrailway = roadorrailway;
        isTerrainProvider=false;
        //vorerst um den vom Way loszuwerden. Ableiter kann neu setzen.
        terrainMeshAdder=null;
        //3.6.19 mal ohne versuchen flatComponent.isOverlay = true;
        // shares OSM id with way segment
        //3.6.19 doppelt osmIds.add(this/*roadorrailway*/.mapWay.getOsmId());

        //return eg;
        //this.segment = segment;
        //this.primaryRep = primaryRepresentation;
    }

    //@Override
    /*public MapWaySegment getPrimaryMapElement() {
        return segment;
    }*/

    /**
     * Geht ueber den BridgeTerrainMeshAdder in SceneryFlatObject. Methode ist hier nur zur Doku dass das so ist.
     */
    /*@Override
    public void addToTerrainMesh() {
        super.addToTerrainMesh();
    }


    //@Override
    public VectorXZ getEndPosition() {
        return primaryRep.getEndPosition();
    }

    //@Override
    public VectorXZ getStartPosition() {
        return primaryRep.getStartPosition();
    }


    public abstract GroundState getGroundState();*/

    /*3.6.19@Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this/*roadorrailway* / + ")";
    }*/

    @Override
    public final void buildEleGroups() {
        super.buildEleGroups();
        //Die ElevationGroups sind "einfach" die des darueberlaufenden Ways.
        EleConnectorGroupSet eg = this/*roadorrailway*/.getEleConnectorGroups();
        for (EleConnectorGroup e : eg.eleconnectorgroups) {
            e.setGroundState(GroundState.ABOVE);
        }
        elevations=eg;

    }

    /**
     * Zum Debuggen.
     */
  /*  @Override
    public void registerCoordinatesToElegroups() {
        super.registerCoordinatesToElegroups();
    }
*/
    /**
     * Zum Debuggen.
     */
  /*  @Override
    public void calculateElevations() {
        /*17.8.18 ueber globale Liste if (roadorrailway != null) {
            roadorrailway.fixElevationGroups();
        }* /
        super.calculateElevations();
    }
*/

    public SceneryWayObject getConnectedRoad(MapNode rampNode) {
        if (rampNode==this/*roadorrailway*/.mapWay.getStartNode()){
            return sroad;
        }
        return eroad;
    }
}
