package de.yard.threed.osm2graph;


import de.yard.threed.core.Util;
import de.yard.threed.core.loader.PortableModel;
import de.yard.threed.graph.Graph;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.Processor;

/**
 * The result of a Processor.
 *
 * 3.11.21
 */
public class ProcessResults {
    // just for easier migration
    @Deprecated
    public Processor processor;
    public RenderData results;
    public Graph roadGraph;
    public Graph railwayGraph;
    //24.3.26 public GltfBuilderResult gltfstring;
    public GridCellBounds gridCellBounds;

    public PortableModel getPortableModelList() {
        Util.notyet();
        return null;//processor.pml;
    }
}
