package de.yard.threed.osm2graph.osm;

/**
 * Nur mal so als Container
 * <p>
 * Created on 02.06.18.
 */
public class DataSet {
    public GridCellBounds gridCellBounds;
    public String osmfile = null;

    public DataSet(String name, String osmfile, GridCellBounds gridCellBounds) {
        this.osmfile = osmfile;
        this.gridCellBounds = gridCellBounds;
    }

    public DataSet(String name, String osmfile) {
        this(name, osmfile, (GridCellBounds) null);
    }
}
