package de.yard.threed.osm2scenery;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.TestUtil;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.core.Degree;
import de.yard.threed.osm2mesh.DesdorfExpectations;
import de.yard.threed.osm2mesh.testutils.DefaultMockingSceneryTest;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.util.SvgWriter;
import de.yard.threed.osm2world.MapWay;
import org.junit.jupiter.api.Test;

import static de.yard.threed.TestUtil.validateConnector;
import static de.yard.threed.TestUtil.validateMeshPolygon;
import static de.yard.threed.osm2mesh.DesdorfExpectations.*;
import static de.yard.threed.osm2scenery.SceneryWayConnectorTest.validateAttachPoint;
import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.ROAD;
import static de.yard.threed.osm2world.Materials.ASPHALT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
public abstract class AbstractSceneryWayConnectorTest extends DefaultMockingSceneryTest {


    protected abstract DesdorfTestData setupForDesdorf(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception;

    @Test
    public void testDesdorfConnector255563538() throws Exception {
        DesdorfTestData desdorfTestData = setupForDesdorf(getMeshServiceFactory(), getValidatorServiceFactory());// new DesdorfTestData(getMeshServiceFactory());

        MapWay way182152619 = desdorfTestData.fullMapData.findMapWay(182152619);
        MapWay way107468171 = desdorfTestData.fullMapData.findMapWay(107468171);
        MapWay way24927839 = desdorfTestData.fullMapData.findMapWay(24927839);

        SceneryWayConnector wayConnector = new SceneryWayConnector("RoadConnector", desdorfTestData.fullMapData.findMapNode(255563538), ASPHALT, ROAD);
        wayConnector.add(way182152619.getFirstSegment());
        wayConnector.add(way107468171.getLastSegment());
        wayConnector.add(way24927839.getLastSegment());
        assertEquals(255563538, wayConnector.getOsmIds().get(0));

        wayConnector.classify();
        assertEquals(SceneryWayConnector.WayConnectorType.STANDARD_JUNCTION, wayConnector.getType());
        assertEquals(2, wayConnector.majorway0);
        assertEquals(0, wayConnector.majorway1);
        assertEquals(-1, wayConnector.minorway0);
        assertEquals(1, wayConnector.minorway1);

        // DB persist
        wayConnector.cca(desdorfTestData.terrainMesh, SceneryContext.getInstance());
        MeshServiceFacade meshService = desdorfTestData.terrainMesh.meshService;
        MeshPolygon/*MeshWayConnector*/ meshWayConnector = meshService.getConnector(255563538);
        assertNotNull(meshWayConnector);
        // angle values appear correct
        SceneryWayConnectorTest.validateAttachPoint(meshWayConnector, 182152619L, new Degree(332), 3, 2);
        SceneryWayConnectorTest.validateAttachPoint(meshWayConnector, 24927839, new Degree(149), 1, 0);
        SceneryWayConnectorTest.validateAttachPoint(meshWayConnector, 107468171, new Degree(240), 0, 3);

        TestUtil.assertDesdorfK41K43connector(wayConnector);

        SvgWriter.build()
                .addPolygon(wayConnector.getPolygon(desdorfTestData.gridCellBounds.getProjection()))
                .writeTmpFile();
    }

    /**
     * The small paths 33817500 and 33817501 are connected at node 387409895
     */
    @Test
    public void testConnector387409895() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());
        MapWay way33817500 = desdorfTestData.fullMapData.findMapWay(33817500);
        MapWay way33817501 = desdorfTestData.fullMapData.findMapWay(33817501);

        SceneryWayConnector wayConnector = new SceneryWayConnector("RoadConnector", desdorfTestData.fullMapData.findMapNode(387409895), ASPHALT, ROAD);
        // order of adding ways effects final polygon vertices! Really? SHouldn't
        wayConnector.add(way33817500.getLastSegment());
        wayConnector.add(way33817501.getFirstSegment());

        wayConnector.classify();
        assertEquals(SceneryWayConnector.WayConnectorType.SIMPLE_CONNECTOR, wayConnector.getType());
        assertEquals(0, wayConnector.majorway0);
        assertEquals(1, wayConnector.majorway1);
        assertEquals(-1, wayConnector.minorway0);
        assertEquals(-1, wayConnector.minorway1);

        // DB persist
        wayConnector.cca(desdorfTestData.terrainMesh, SceneryContext.getInstance());
        MeshServiceFacade meshService = desdorfTestData.terrainMesh.meshService;
        MeshPolygon/*MeshWayConnector*/ meshWayConnector = meshService.getConnector(387409895);
        TestUtil.validateMeshPolygon(DesdorfExpectations.expectedConnector387409895, meshWayConnector);

        SvgWriter.build()
                .addPolygon(wayConnector.getPolygon(desdorfTestData.gridCellBounds.getProjection()), SvgWriter.LabelMode.NODEBYINDEX)
                .writeTmpFile();
    }


    @Test
    public void testConnector445410497() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());
        MapWay way107468169 = desdorfTestData.fullMapData.findMapWay(107468169);
        MapWay way23696493 = desdorfTestData.fullMapData.findMapWay(23696493);
        MapWay way107468171 = desdorfTestData.fullMapData.findMapWay(107468171);
        MapWay way37935654 = desdorfTestData.fullMapData.findMapWay(37935654);

        SceneryWayConnector wayConnector = new SceneryWayConnector("RoadConnector", desdorfTestData.fullMapData.findMapNode(445410497), ASPHALT, ROAD);
        wayConnector.add(way107468169.getFirstSegment());
        wayConnector.add(way23696493.getLastSegment());
        wayConnector.add(way107468171.getFirstSegment());
        wayConnector.add(way37935654.getLastSegment());
        assertEquals(445410497, wayConnector.getOsmIds().get(0));

        wayConnector.classify();
        assertEquals(SceneryWayConnector.WayConnectorType.SIMPLE_JUNCTION, wayConnector.getType());
        assertEquals(1, wayConnector.majorway0);
        assertEquals(2, wayConnector.majorway1);
        assertEquals(0, wayConnector.minorway0);
        assertEquals(3, wayConnector.minorway1);

        // DB persist
        wayConnector.cca(desdorfTestData.terrainMesh, SceneryContext.getInstance());
        assertEquals(4 + 1, wayConnector.getPolygonLine().size());
        SvgWriter.build()
                .addPolygon(wayConnector.getPolygon(desdorfTestData.gridCellBounds.getProjection()), SvgWriter.LabelMode.NODEBYINDEX)
                .writeTmpFile();

        MeshServiceFacade meshService = desdorfTestData.terrainMesh.meshService;
        MeshPolygon meshWayConnector = meshService.getConnector(445410497);
        TestUtil.validateMeshPolygon(DesdorfExpectations.expectedConnector445410497, meshWayConnector);
    }

    @Test
    public void testConnector251517906() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());
        MapWay way182152619 = desdorfTestData.fullMapData.findMapWay(182152619);
        MapWay way225794271 = desdorfTestData.fullMapData.findMapWay(225794271);

        SceneryWayConnector wayConnector = new SceneryWayConnector("RoadConnector", desdorfTestData.fullMapData.findMapNode(251517906), ASPHALT, ROAD);
        wayConnector.add(way182152619.getFirstSegment());
        wayConnector.add(way182152619.getLastSegment());// 'last' is 'second' here
        wayConnector.add(way225794271.getLastSegment());
        assertEquals(251517906, wayConnector.getOsmIds().get(0));

        wayConnector.classify();
        assertEquals(SceneryWayConnector.WayConnectorType.SIMPLE_JUNCTION, wayConnector.getType());
        assertEquals(0, wayConnector.majorway0);
        assertEquals(1, wayConnector.majorway1);
        assertEquals(-1, wayConnector.minorway0);
        assertEquals(2, wayConnector.minorway1);

        wayConnector.cca(desdorfTestData.terrainMesh, SceneryContext.getInstance());
        assertEquals(4 + 1, wayConnector.getPolygonLine().size());
        SvgWriter.build()
                .addPolygon(wayConnector.getPolygon(desdorfTestData.gridCellBounds.getProjection()), SvgWriter.LabelMode.NODEBYINDEX)
                .writeTmpFile();

        MeshServiceFacade meshService = desdorfTestData.terrainMesh.meshService;
        MeshPolygon meshWayConnector = meshService.getConnector(251517906);
        validateMeshPolygon(expectedConnector251517906, meshWayConnector);

    }

    @Test
    public void testConnector255563537() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());
        MapWay way225794271 = desdorfTestData.fullMapData.findMapWay(225794271);
        MapWay way107468171 = desdorfTestData.fullMapData.findMapWay(107468171);

        SceneryWayConnector wayConnector = new SceneryWayConnector("RoadConnector", desdorfTestData.fullMapData.findMapNode(255563537), ASPHALT, ROAD);
        wayConnector.add(way225794271.getLastSegment());
        wayConnector.add(way107468171.segment2s.get(3));
        wayConnector.add(way107468171.segment2s.get(4));
        assertEquals(255563537, wayConnector.getOsmIds().get(0));

        wayConnector.classify();
        assertEquals(SceneryWayConnector.WayConnectorType.SIMPLE_JUNCTION, wayConnector.getType());
        assertEquals(1, wayConnector.majorway0);
        assertEquals(2, wayConnector.majorway1);
        assertEquals(-1, wayConnector.minorway0);
        assertEquals(0, wayConnector.minorway1);

        wayConnector.cca(desdorfTestData.terrainMesh, SceneryContext.getInstance());
        assertEquals(4 + 1, wayConnector.getPolygonLine().size());
        SvgWriter.build()
                .addPolygon(wayConnector.getPolygon(desdorfTestData.gridCellBounds.getProjection()), SvgWriter.LabelMode.NODEBYINDEX)
                .writeTmpFile();

        MeshServiceFacade meshService = desdorfTestData.terrainMesh.meshService;
        MeshPolygon meshWayConnector = meshService.getConnector(255563537);
        validateMeshPolygon(expectedConnector255563537, meshWayConnector);

    }

    /**
     * A 'GENERIC' connector with 3 ways
     */
    @Test
    public void testConnector387409892() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());
        MapWay way33817500 = desdorfTestData.fullMapData.findMapWay(33817500);
        MapWay way33817499 = desdorfTestData.fullMapData.findMapWay(33817499);

        SceneryWayConnector wayConnector = new SceneryWayConnector("RoadConnector", desdorfTestData.fullMapData.findMapNode(387409892), ASPHALT, ROAD);
        wayConnector.add(way33817500.getFirstSegment());
        wayConnector.add(way33817499.getFirstSegment());
        wayConnector.add(way33817499.getLastSegment());

        wayConnector.classify();
        assertEquals(SceneryWayConnector.WayConnectorType.GENERIC, wayConnector.getType());
        assertEquals(-1, wayConnector.majorway0);
        assertEquals(-1, wayConnector.majorway1);
        assertEquals(-1, wayConnector.minorway0);
        assertEquals(-1, wayConnector.minorway1);

        wayConnector.cca(desdorfTestData.terrainMesh, SceneryContext.getInstance());
        MeshServiceFacade meshService = desdorfTestData.terrainMesh.meshService;
        MeshPolygon meshWayConnector = meshService.getConnector(387409892);
        validateMeshPolygon(expectedConnector387409892, meshWayConnector);

        SvgWriter.build()
                .addPolygon(wayConnector.getPolygon(desdorfTestData.gridCellBounds.getProjection()), SvgWriter.LabelMode.NODEBYINDEX)
                .writeTmpFile();
    }

    /**
     * A 'STANDARD_JUNCTION' connector with 3 ways
     */
    @Test
    public void testConnector270353278() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());
        MapWay way24879711 = desdorfTestData.fullMapData.findMapWay(24879711);
        MapWay way107468171 = desdorfTestData.fullMapData.findMapWay(107468171);

        SceneryWayConnector wayConnector = new SceneryWayConnector("RoadConnector", desdorfTestData.fullMapData.findMapNode(270353278), ASPHALT, ROAD);
        wayConnector.add(way24879711.getFirstSegment());
        wayConnector.add(way107468171.getFirstSegment());
        wayConnector.add(way107468171.segment2s.get(1));

        wayConnector.classify();
        assertEquals(SceneryWayConnector.WayConnectorType.STANDARD_JUNCTION, wayConnector.getType());
        assertEquals(1, wayConnector.majorway0);
        assertEquals(2, wayConnector.majorway1);
        assertEquals(-1, wayConnector.minorway0);
        assertEquals(0, wayConnector.minorway1);

        wayConnector.cca(desdorfTestData.terrainMesh, SceneryContext.getInstance());
        MeshServiceFacade meshService = desdorfTestData.terrainMesh.meshService;
        MeshPolygon meshWayConnector = meshService.getConnector(270353278);
        validateMeshPolygon(expectedConnector270353278, meshWayConnector);

        SvgWriter.build()
                .addPolygon(wayConnector.getPolygon(desdorfTestData.gridCellBounds.getProjection()), SvgWriter.LabelMode.NODEBYINDEX)
                .writeTmpFile();
    }

    /**
     * Not sure why this is generic. Probably it shouldn't be.
     */
    @Test
    public void testConnector445409643() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());
        MapWay way37935545 = desdorfTestData.fullMapData.findMapWay(37935545);
        MapWay way107468171 = desdorfTestData.fullMapData.findMapWay(107468171);

        SceneryWayConnector wayConnector = new SceneryWayConnector("RoadConnector", desdorfTestData.fullMapData.findMapNode(445409643), ASPHALT, ROAD);
        wayConnector.add(way37935545.getFirstSegment());
        wayConnector.add(way107468171.segment2s.get(1));
        wayConnector.add(way107468171.segment2s.get(2));

        wayConnector.classify();
        assertEquals(SceneryWayConnector.WayConnectorType.GENERIC, wayConnector.getType());
        assertEquals(-1, wayConnector.majorway0);
        assertEquals(-1, wayConnector.majorway1);
        assertEquals(-1, wayConnector.minorway0);
        assertEquals(-1, wayConnector.minorway1);

        wayConnector.cca(desdorfTestData.terrainMesh, SceneryContext.getInstance());
        MeshServiceFacade meshService = desdorfTestData.terrainMesh.meshService;
        MeshPolygon meshWayConnector = meshService.getConnector(445409643);
        validateMeshPolygon(expectedConnector445409643, meshWayConnector);

        SvgWriter.build()
                .addPolygon(wayConnector.getPolygon(desdorfTestData.gridCellBounds.getProjection()), SvgWriter.LabelMode.NODEBYINDEX)
                .writeTmpFile();
    }

    /**
     * Not sure why this is generic. Probably it shouldn't be.
     */
    @Test
    public void testConnector387409890() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());
        MapWay way33817499 = desdorfTestData.fullMapData.findMapWay(33817499);
        MapWay way107468171 = desdorfTestData.fullMapData.findMapWay(107468171);

        SceneryWayConnector wayConnector = new SceneryWayConnector("RoadConnector", desdorfTestData.fullMapData.findMapNode(387409890), ASPHALT, ROAD);
        wayConnector.add(way33817499.getFirstSegment());
        wayConnector.add(way107468171.segment2s.get(2));
        wayConnector.add(way107468171.segment2s.get(3));

        wayConnector.classify();
        assertEquals(SceneryWayConnector.WayConnectorType.GENERIC, wayConnector.getType());
        assertEquals(-1, wayConnector.majorway0);
        assertEquals(-1, wayConnector.majorway1);
        assertEquals(-1, wayConnector.minorway0);
        assertEquals(-1, wayConnector.minorway1);

        wayConnector.cca(desdorfTestData.terrainMesh, SceneryContext.getInstance());
        MeshServiceFacade meshService = desdorfTestData.terrainMesh.meshService;
        MeshPolygon meshWayConnector = meshService.getConnector(387409890);
        validateMeshPolygon(expectedConnector387409890, meshWayConnector);

        SvgWriter.build()
                .addPolygon(wayConnector.getPolygon(desdorfTestData.gridCellBounds.getProjection()), SvgWriter.LabelMode.NODEBYINDEX)
                .writeTmpFile();
    }
}
