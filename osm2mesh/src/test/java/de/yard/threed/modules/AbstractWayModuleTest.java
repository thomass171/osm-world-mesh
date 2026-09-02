package de.yard.threed.modules;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.osm2mesh.testutils.DefaultMockingSceneryTest;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.modules.common.WayModule;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.util.SvgWriter;
import de.yard.threed.osm2world.MapWay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static de.yard.threed.TestUtil.validateMeshPolygon;
import static de.yard.threed.osm2mesh.DesdorfExpectations.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractWayModuleTest extends DefaultMockingSceneryTest {

    protected abstract DesdorfTestData setupForDesdorf(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception;

    DesdorfTestData desdorfTestData;
    WayModule wayModule;

    @BeforeEach
    public void setUp() throws Exception {
        desdorfTestData = setupForDesdorf(getMeshServiceFactory(), getValidatorServiceFactory());
        wayModule = new HighwayModule(desdorfTestData.meshService, desdorfTestData.gridCellBounds.getProjection(), "Desdorf");
    }

    @Test
    public void testWay24879711() throws Exception {

        MapWay way = desdorfTestData.fullMapData.findMapWay(24879711);

        wayModule.applyTo(way, SceneryContext.getInstance());

        SvgWriter.build()
                .addMeshPolygons(desdorfTestData.meshService.loadMesh("Desdorf").polygons, desdorfTestData.getGridCellBounds().getProjection())
                .writeTmpFile();
    }

    @Test
    public void testWay107468171() throws Exception {

        MapWay way = desdorfTestData.fullMapData.findMapWay(107468171);

        wayModule.applyTo(way, SceneryContext.getInstance());

        SvgWriter.build()
                .addMeshPolygons(desdorfTestData.meshService.loadMesh("Desdorf").polygons, desdorfTestData.getGridCellBounds().getProjection())
                .writeTmpFile();
    }

    @Test
    public void testDesdorfLowerK41() throws Exception {

        MapWay way = desdorfTestData.fullMapData.findMapWay(24927839);

        wayModule.applyTo(way, SceneryContext.getInstance());
    }

    @Test
    public void testDesdorfUpperK41() throws Exception {

        MapWay way = desdorfTestData.fullMapData.findMapWay(182152619);
        double width = 7.7;

       wayModule.applyTo(way, SceneryContext.getInstance());

        /*assertEquals(255563538, firstSegment.getStartConnector().getOsmId());
        assertEquals(SceneryWayObject.WayOuterMode.CONNECTOR, firstSegment.getStartMode());

        assertNotNull(firstSegment.getEndConnector());
        assertEquals(SceneryWayObject.WayOuterMode.CONNECTOR, firstSegment.getEndMode());
        wayModule.applyTo(way, SceneryContext.getInstance());
*/

       /* assertEquals(251517906/*correct??* /, lastSegment.getStartConnector().getOsmId());
        assertEquals(SceneryWayObject.WayOuterMode.CONNECTOR, lastSegment.getStartMode());

        assertNull(lastSegment.getEndConnector());
        assertEquals(SceneryWayObject.WayOuterMode.DEADEND/*why not boundary?? 18.3.26 we have no cut, so why not connector?* /, lastSegment.getEndMode());
*/
        SvgWriter.build(/*desdorfTestData.gridCellBounds*/)
                // terrainMesh not populated
                .addMeshPolygons(desdorfTestData.terrainMesh.polygons, desdorfTestData.getGridCellBounds().getProjection())
                .writeTmpFile();
    }

    /**
     * The small paths 33817500 and 33817501 are connected at node 387409895
     */
    @Test
    public void testSimpleMainConnectionInDesdorf() throws Exception {

        MapWay way33817500 = desdorfTestData.fullMapData.findMapWay(33817500);
        MapWay way33817501 = desdorfTestData.fullMapData.findMapWay(33817501);

        wayModule.applyTo(way33817500, SceneryContext.getInstance());

        MeshPolygon meshWayConnector =  desdorfTestData.meshService.getConnector(387409895);
        validateMeshPolygon(expectedConnector387409895, meshWayConnector);

        //SVG not possible?
        SvgWriter.build()
                // terrainMesh not populated
                .addMeshPolygons(desdorfTestData.terrainMesh.polygons, desdorfTestData.getGridCellBounds().getProjection())
                .writeTmpFile();
    }

    /**
     * 107468169 had bad polygon once
     */
    @Test
    public void testConnector445410497InDesdorf() throws Exception {

        MapWay way107468169 = desdorfTestData.fullMapData.findMapWay(107468169);

        wayModule.applyTo(way107468169, SceneryContext.getInstance());

        MeshPolygon meshWayConnector =  desdorfTestData.meshService.getConnector(445410497);
        validateMeshPolygon(expectedConnector445410497, meshWayConnector);

        SvgWriter.build()
                .addMeshPolygons(desdorfTestData.meshService.loadMesh("Desdorf").polygons, desdorfTestData.getGridCellBounds().getProjection())
                .writeTmpFile();

    }
}
