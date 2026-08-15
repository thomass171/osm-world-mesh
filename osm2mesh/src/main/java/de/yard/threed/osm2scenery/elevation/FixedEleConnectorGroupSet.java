package de.yard.threed.osm2scenery.elevation;




/**
 * List of fixed
 * Created on 01.08.18.
 */
public class FixedEleConnectorGroupSet extends EleConnectorGroupSet {

    

    @Override
    public void add(EleConnectorGroup eleConnectorGroup) {
        if (!eleConnectorGroup.hasElevation()) {
            
            throw new RuntimeException("no elevation");
        }
        super.add(eleConnectorGroup);

    }
}
