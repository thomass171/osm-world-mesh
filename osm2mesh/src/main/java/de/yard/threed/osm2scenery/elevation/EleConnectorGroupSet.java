package de.yard.threed.osm2scenery.elevation;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2world.GroundState;
import de.yard.threed.osm2world.MapNode;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Zunächst mal einfach eine Liste von {@link EleConnectorGroup}
 * Created on 01.08.18.
 */
public class EleConnectorGroupSet {
    Logger logger = Logger.getLogger(EleConnectorGroupSet.class);

    public List<EleConnectorGroup> eleconnectorgroups = new ArrayList<>();

    /**
     * 
     * @param node
     * @return
     */
    public EleConnectorGroup getEleConnectorGroup(MapNode node) {
        for (EleConnectorGroup g : eleconnectorgroups) {
            if (g.mapNode == node) {
                return g;
            }
        }
        // should never happen? Doch, doch. Das wird auch aufgerufen um sich die Pruefung zu sparen, ob es eine Node gibt. Also nicht loggen. Schoen ist das natuerlich nicht 
        //logger.warn("EleConnectorGroup not found");
        return null;
    }

    public void add(EleConnectorGroup eleConnectorGroup) {
        if (eleConnectorGroup == null) {
            //convenience. Is there any reason?
            logger.warn("group isType null");
            return;
        }
        if (!eleconnectorgroups.contains(eleConnectorGroup)) {
            eleconnectorgroups.add(eleConnectorGroup);
        }
    }

    /**
     * Das ist tricky. TODO
     * Erstmal einfach average, bzww immer wenn location null ist.
     * Liefert null, wenn keine Berechnung moeglich ist.
     *
     * @return
     */
    public Double getWeightedAverage(Coordinate location) {
        double average = 0;
        for (EleConnectorGroup g : eleconnectorgroups) {
            if (g.groundState == GroundState.ABOVE) {
                average += g.getElevation() - 6;
            } else {
                average += g.getElevation();
            }
        }
        if (eleconnectorgroups.size() > 0) {
            average = average / eleconnectorgroups.size();
        }else{
            logger.warn("getWeightedAverage(): empty FixedEleConnectorGroupSet. no elavation found.");
            return null;
        }
        if (average > 0.1f) {
            average = average;
        }
        return new Double(average);
    }

    /**
     * Return false if only one group has no elevation.
     * @return
     */
    public boolean isFixed() {
        for (EleConnectorGroup g : eleconnectorgroups) {
            if (!g.hasElevation()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Liefert - bei einem way - den ersten und letzten
     * @return
     */
    /*28.8.18 public EleConnectorGroupSet getOuter(){
        EleConnectorGroupSet egr = new EleConnectorGroupSet();
        egr.add(eleconnectorgroups.get(0));
        egr.add(eleconnectorgroups.get(eleconnectorgroups.size()-1));
        return egr;
    }*/

    public int size() {
        return eleconnectorgroups.size();
    }

    public EleConnectorGroup get(int i) {
        return eleconnectorgroups.get(i);
    }

    public List<EleCoordinate> getEleConnectors(){

        List<EleCoordinate> l = new ArrayList<>();
        for (EleConnectorGroup egr : eleconnectorgroups){
            l.addAll(egr.eleConnectors);
        }
        return l;
    }

    public void addAll(List<EleConnectorGroup> eleconnectorgroups) {
        this.eleconnectorgroups.addAll(eleconnectorgroups);
    }
}
