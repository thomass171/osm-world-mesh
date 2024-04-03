package de.yard.threed.osm2scenery;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.scenery.WorldElement;
import de.yard.threed.osm2scenery.util.RenderedArea;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.OsmOrigin;

/**
 * Created on 11.07.18.
 */
public  interface SceneryRenderer {
    /**
     * Rendern einer (Grund)fläche.
     *
     * Polygon damit holes drin sein können.
     * "name" ist nur zur Analyse, hat aber keine funktionale Bedeutung. Das ist wichtig.
     * 18.8.18: Ich stecke mal den Polygon und trifailed in OsmOrigin rein. 24.5.19: Diese Mehrfachnutzung ist doof. osmOrigin sollte null sein ohne OSM Herkunft.
     * 23.8.18: Might return a kind of reference to the drawn element, eg. PolygonInformation
     * 26.4.19: Die Methode ist eigentlich primär zur 2D Darstellung und Analyse.
     */
    RenderedArea drawArea(String name, Material material, Polygon sceneryArea, VertexData vertexData, OsmOrigin osmOrigin, EleConnectorGroupSet eleconnectors);

    /**
     * Universeller. Rendern von Vertex Daten. Das können Grundflächen wie auch Buildings sein.
     * Das koennte die obere Methode ablösen, wenn mehr in sowas wie OsmOrigin gesteckt wird.
     * "name" ist nur zur Analyse, hat aber keine funktionale Bedeutung.
     *
     * 26.4.19
     */
    RenderedArea drawElement(String name, WorldElement element, OsmOrigin osmOrigin);

    /**
     * Both to be called once!
     */
    void beginRendering();
    void endRendering();

    boolean isTerrainProviderOnly();

    //25.4.19 boolean renderVolumeOverlay();
}
