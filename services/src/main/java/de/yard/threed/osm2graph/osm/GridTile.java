package de.yard.threed.osm2graph.osm;

/**
 * Tile definiert als Polygon mit beliebig vielen Coords
 * Created on 22.05.18.
 */
public class GridTile {
    public GridTile(int[] coordindex) {
        
    }
    /*GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    Coordinate[] coords  =
            new Coordinate[] {new Coordinate(4, 0), new Coordinate(2, 2),
                    new Coordinate(4, 4), new Coordinate(6, 2), new Coordinate(4, 0) };*/
    
    GridTile getSample(){
        return new GridTile(new int[]{0,1,2,3});
    }
}
