package de.yard.threed.modules;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.core.Degree;
import de.yard.threed.core.platform.Platform;
import de.yard.threed.core.testutil.CoreTestFactory;
import de.yard.threed.core.testutil.PlatformFactoryTestingCore;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.threed.osm2mesh.testutils.MeshPolygonMock;
import de.yard.threed.osm2mesh.testutils.MeshServiceMock;
import de.yard.threed.osm2mesh.testutils.ValidatorServiceForMocking;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryWayConnectorTest;
import de.yard.threed.osm2scenery.modules.common.WayModule;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.util.SvgWriter;
import de.yard.threed.osm2world.MapWay;
import org.junit.jupiter.api.Test;

import static de.yard.threed.TestUtil.validateMeshPolygon;
import static de.yard.threed.osm2mesh.Expectations.*;
import static de.yard.threed.osm2scenery.SceneryWayConnectorTest.validateAttachPoint;
import static org.junit.jupiter.api.Assertions.*;

public class WayModuleTest extends AbstractWayModuleTest {

    Platform platform = CoreTestFactory.initPlatformForTest(new PlatformFactoryTestingCore(), null);

    @Test
    public void testDesdorfLowerK41() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());

        MapWay way = desdorfTestData.fullMapData.findMapWay(24927839);
        WayModule wayModule = new WayModule();

        SceneryWayObject w = (SceneryWayObject) wayModule.applyTo(way.getLastSegment(), desdorfTestData.terrainMesh, SceneryContext.getInstance()).get(0);


    }

    @Test
    public void testDesdorfUpperK41() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());

        MapWay way = desdorfTestData.fullMapData.findMapWay(182152619);
        double width = 7.7;

        WayModule wayModule = new WayModule();
        SceneryWayObject firstSegment = (SceneryWayObject) wayModule.applyTo(way.getFirstSegment(), desdorfTestData.terrainMesh, SceneryContext.getInstance()).get(0);

        assertEquals(255563538, firstSegment.getStartConnector().getOsmId());
        assertEquals(SceneryWayObject.WayOuterMode.CONNECTOR, firstSegment.getStartMode());

        assertNotNull(firstSegment.getEndConnector());
        assertEquals(SceneryWayObject.WayOuterMode.CONNECTOR, firstSegment.getEndMode());

        SceneryWayObject lastSegment = (SceneryWayObject) wayModule.applyTo(way.getLastSegment(), desdorfTestData.terrainMesh, SceneryContext.getInstance()).get(0);

        assertEquals(251517906/*correct??*/, lastSegment.getStartConnector().getOsmId());
        assertEquals(SceneryWayObject.WayOuterMode.CONNECTOR, lastSegment.getStartMode());

        assertNull(lastSegment.getEndConnector());
        assertEquals(SceneryWayObject.WayOuterMode.DEADEND/*why not boundary?? 18.3.26 we have no cut, so why not connector?*/, lastSegment.getEndMode());

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

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());

        MapWay way33817500 = desdorfTestData.fullMapData.findMapWay(33817500);
        MapWay way33817501 = desdorfTestData.fullMapData.findMapWay(33817501);

        WayModule wayModule = new WayModule();
        wayModule.applyTo(way33817500.getFirstSegment(), desdorfTestData.terrainMesh, SceneryContext.getInstance());

        MeshServiceMock meshServiceMock = (MeshServiceMock) desdorfTestData.terrainMesh.meshService;
        MeshPolygon meshWayConnector =  meshServiceMock.getConnector(387409895);
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

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());

        MapWay way107468169 = desdorfTestData.fullMapData.findMapWay(107468169);

        WayModule wayModule = new WayModule();
        wayModule.applyTo(way107468169.getFirstSegment(), desdorfTestData.terrainMesh, SceneryContext.getInstance());

        MeshServiceMock meshServiceMock = (MeshServiceMock) desdorfTestData.terrainMesh.meshService;
        MeshPolygon meshWayConnector =  meshServiceMock.getConnector(445410497);
        validateMeshPolygon(expectedConnector445410497, meshWayConnector);

        SvgWriter.build()
                // terrainMesh not populated
                .addMeshPolygons(desdorfTestData.terrainMesh.polygons, desdorfTestData.getGridCellBounds().getProjection())
                .writeTmpFile();

    }

    @Override
    protected MeshServiceFactory getMeshServiceFactory() {
        return gridCellBounds -> new MeshServiceMock(gridCellBounds);
    }

    @Override
    protected ValidatorServiceFactory getValidatorServiceFactory() {
        return () -> new ValidatorServiceForMocking();
    }
}
