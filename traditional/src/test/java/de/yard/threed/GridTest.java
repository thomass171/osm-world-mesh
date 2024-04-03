package de.yard.threed;


import de.yard.threed.core.LatLon;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.VectorXZ;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests fuer GridBounds, Projection, usw., was nicht in OsmGridTest getestet wird.
 * Genauer: Ohne Nutzung des Processor und der ConversionFacade.
 * <p>
 * Created on 09.04.19
 */
public class GridTest {
    double EPSILON = 0.000001;

    //EngineHelper platform = PlatformHomeBrew.init(new HashMap<String, String>());
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));

    @BeforeAll
    public static void setup(){
        TerrainMesh.meshFactoryInstance = new TraditionalMeshFactory();
    }

    //11.4.19 witzlos, weil lineare Projection witzlos @Test
    public void testLoadDesdorfGrid() throws IOException {

        GridCellBounds gridCellBounds = GridCellBounds.buildGrid("Desdorf", null);
        assertEquals(6.5874427, gridCellBounds.getLeft(), "left.lon");
        assertEquals(6.5934, gridCellBounds.getRight(), "right.lon");
        assertEquals(50.9502612, gridCellBounds.getTop(), "top.lat");
        assertEquals(50.9475747, gridCellBounds.getBottom(), "bottom.lat");

        SceneryProjection projection = gridCellBounds.getProjection();
        LatLon topleft = gridCellBounds.getTopLeft();
        LatLon bottomleft = gridCellBounds.getBottomLeft();
        assertEquals(50.948917949999995, gridCellBounds.getOrigin().getLatDeg().getDegree(), "origin.lat");
        assertEquals(6.59042135, gridCellBounds.getOrigin().getLonDeg().getDegree(), "origin.lon");
        assertEquals(0.0, OsmUtil.calcPos(projection, gridCellBounds.getOrigin()).x, EPSILON, "origin.x");
        assertEquals(0.0, OsmUtil.calcPos(projection, gridCellBounds.getOrigin()).z, EPSILON, "origin.y");
        //Die -115 sind plausibel->uebernommen
        assertEquals(-115.44558776609989, OsmUtil.calcPos(projection, bottomleft).z, EPSILON, "bottom.y");
        assertEquals(-256.0, OsmUtil.calcPos(projection, topleft).x, EPSILON, "left.x");
        assertEquals(-115.44558776609989, OsmUtil.calcPos(projection, bottomleft).z, EPSILON, "left.y");
        //Die 115 sind genau das Gegenstück zu oben, weil es drei Gridpoints gibt
        VectorXZ ptopleft = OsmUtil.calcPos(projection, topleft);
        assertEquals(115.44558776609989, ptopleft.z, EPSILON, "top.y");
        assertEquals(50.9502612, OsmUtil.calcLatLon(projection, ptopleft).getLatDeg().getDegree(), EPSILON, "unproject.top.lat");
        assertEquals(gridCellBounds.getOrigin().getLonDeg().getDegree(), OsmUtil.calcLatLon(projection, new VectorXZ(0, 0)).getLonDeg().getDegree(), EPSILON, "unproject.origin.lon");
        assertEquals(gridCellBounds.getOrigin().getLatDeg().getDegree(), OsmUtil.calcLatLon(projection, new VectorXZ(0, 0)).getLatDeg().getDegree(), EPSILON, "unproject.origin.lat");

        gridCellBounds.init(gridCellBounds.getProjection());

        assertEquals(3, gridCellBounds.getPolygon().getCoordinates().length - 1, "desdorfgrid.nodes");


    }
}
