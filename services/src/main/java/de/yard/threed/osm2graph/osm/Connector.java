package de.yard.threed.osm2graph.osm;

import de.yard.threed.core.Degree;
import de.yard.threed.traffic.geodesy.GeoCoordinate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 22.05.18.
 */
public class Connector {
    public List<GeoCoordinate> coors = new ArrayList();
    
    public Connector(){
        buildSample();
    }

    /**
     * Weil Motorways zweispurig sind nehme ich wirklich diese und schneide da 100m senkrecht
     * ab.
     */
    private void buildSample(){
        coors.add( new GeoCoordinate(new Degree(50.837616f),new Degree(7.203240f),0));
        coors.add(new GeoCoordinate(new Degree(50.814411f),new Degree(7.119813f),0));
        coors.add(new GeoCoordinate(new Degree(50.949359f),new Degree(7.073808f),0));
        coors.add( new GeoCoordinate(new Degree(50.935081f),new Degree(7.048402f),0));
        coors.add( new GeoCoordinate(new Degree(50.914955f),new Degree(7.065225f),0));
        coors.add( new GeoCoordinate(new Degree(50.908028f),new Degree(7.046685f),0));
        coors.add( new GeoCoordinate(new Degree(50.898068f),new Degree(7.085137f),0));
        coors.add(new GeoCoordinate(new Degree(50.910842f),new Degree(7.004456f),0));
        coors.add( new GeoCoordinate(new Degree(50.895813f),new Degree(6.986892f),0));
        coors.add( new GeoCoordinate(new Degree(50.903392f),new Degree(6.898658f),0));
        coors.add( new GeoCoordinate(new Degree(50.962029f),new Degree(6.850936f),0));
    }
}
