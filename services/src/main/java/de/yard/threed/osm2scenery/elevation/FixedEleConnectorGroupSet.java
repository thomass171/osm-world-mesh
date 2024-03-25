package de.yard.threed.osm2scenery.elevation;


import org.apache.log4j.Logger;

/**
 * List of fixed
 * Created on 01.08.18.
 */
public class FixedEleConnectorGroupSet extends EleConnectorGroupSet {
    Logger logger = Logger.getLogger(FixedEleConnectorGroupSet.class);

    

    @Override
    public void add(EleConnectorGroup eleConnectorGroup) {
        if (!eleConnectorGroup.hasElevation()) {
            
            throw new RuntimeException("no elevation");
        }
        super.add(eleConnectorGroup);

    }
}
