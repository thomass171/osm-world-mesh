package de.yard.threed.osm2scenery.elevation;


import de.yard.threed.traffic.geodesy.ElevationProvider;

/**
 *  //28.7.18: bis einer richtig laueft.
 //der OpenDingens Server ist langsam und mit fragwürdiger Genauigkeit. Google ist kostenpflichtig,
 //selber bauen ist wegen Projektio u.ä. knifflig. Das ist alles Hölle kompliziert. 
 // Darum gehe ich mal immer von 68 aus.
 //Das dürfte fuer die ersten Tests erstmal reichen.
 * Created on 02.08.18.
 */
public class FixedElevationProvider implements ElevationProvider {
    private final double elevation;

    public FixedElevationProvider(){
        this(0);
    }
    
    public FixedElevationProvider(double elevation){
        this.elevation=elevation;
    }
    
    @Override
    public Double getElevation(double latitudedeg, double longitudedeg) {
        return elevation;
    }
}
