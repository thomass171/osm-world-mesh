package de.yard.threed.osm2graph;


import de.yard.threed.core.loader.PortableModelList;
import de.yard.threed.graph.Graph;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.tools.GltfBuilderResult;

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
    public GltfBuilderResult gltfstring;
    public GridCellBounds gridCellBounds;

    public PortableModelList getPortableModelList() {
        return processor.pml;
    }
}
