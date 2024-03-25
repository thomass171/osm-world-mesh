package de.yard.threed.osm2scenery.elevation;

import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Eine Menge von Ways, die noch ungefixte ElevationGroups haben. Die Elevation fuer die ungefixten wird aus einer Menge bekannter Elevations ermittelt.
 * Nur ein voruebergehender Container.
 * 
 * Created on 01.08.18.
 */
public class ElevationArea {
    Logger logger = Logger.getLogger(ElevationArea.class.getName());
    public List<SceneryWayObject> unfixed = new ArrayList<>();
    // die bekannten fixed groups in dieser Menge
    private FixedEleConnectorGroupSet fixedEleConnectorGroupSet=new FixedEleConnectorGroupSet();
    static public List<ElevationArea> history = new ArrayList<>();
    
    public ElevationArea(){
        history.add(this);
    }
    
    public void addSegment(SceneryWayObject road){
        unfixed.add(road);

        for (EleConnectorGroup e : road.getEleConnectorGroups().eleconnectorgroups) {
            if (e.isFixed()) {
                fixedEleConnectorGroupSet.add(e);
            }
        }
        
    }

    /**
     * Fuer alle Groups in dieser Menge an Ways, die noch keine Elevation haben, eine Elevation ermitteln.
     */
    public void fixUnfixed() {
        for (SceneryWayObject way : unfixed) {
            if (way.getOsmIdsAsString().contains("8033747")){
                int h=8;
            }
            // Nur offene Endpunkte ueber fixedEleConnectorGroupSet setzen. Innerhalb des Way interpolieren. Gridnodes dürften/muessten schon eine Elevation haben.
            EleConnectorGroupSet outer = way.getOuterEleConnectorGroups();
            for (EleConnectorGroup g : outer.eleconnectorgroups) {

                if (!g.isFixed()) {
                    double elevation;
                    //24.8.18: Das ist doch äusserst fragwürdig. Connectete Ways muessen doch dieselbe Elevation haben, nicht eine average von irgendwas!
                    //13.8.19: Fuer DeadEnd kann man aber hierhin kommen.
                    if (fixedEleConnectorGroupSet.size()==0){
                        logger.warn("No fixed elevationgroup. Using average elevation");
                        elevation=ElevationMap.getInstance().getAverage();
                    }else {
                        elevation = fixedEleConnectorGroupSet.getWeightedAverage(g.location);
                    }
                    //11.6.19: Muesste dann eigentlich inside, sonst waere es schon gefixed(?)
                    //12.7.19 entbehrlich? g.fixLocation( EleConnectorGroup.Location.INSIDEGRID);
                    g.fixElevation(elevation);
                }
            }
            way.interpolateGroupElevations();
            
            // Gegenprobe
            if (!way.getEleConnectorGroups().isFixed()){
                logger.error("way not fixed");
            }
        }

    }
}
