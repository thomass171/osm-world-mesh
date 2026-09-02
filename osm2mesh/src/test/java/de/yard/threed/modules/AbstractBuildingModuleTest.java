package de.yard.threed.modules;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.osm2mesh.testutils.DefaultMockingSceneryTest;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.modules.BuildingModule;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.modules.common.WayModule;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.util.SvgWriter;
import de.yard.threed.osm2world.MapWay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static de.yard.threed.TestUtil.validateMeshPolygon;
import static de.yard.threed.osm2mesh.DesdorfExpectations.*;

public abstract class AbstractBuildingModuleTest extends DefaultMockingSceneryTest {

    protected abstract DesdorfTestData setupForDesdorf(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception;

    DesdorfTestData desdorfTestData;
    BuildingModule buildingModule;

    @BeforeEach
    public void setUp() throws Exception {
        desdorfTestData = setupForDesdorf(getMeshServiceFactory(), getValidatorServiceFactory());
        buildingModule = new BuildingModule(desdorfTestData.meshService, desdorfTestData.gridCellBounds.getProjection(), "Desdorf");
    }

    @Test
    public void testBuilding322751224() throws Exception {

        MapWay way = desdorfTestData.fullMapData.findMapWay(322751224);

        buildingModule.applyTo(way, SceneryContext.getInstance());

        SvgWriter.build()
                .addMeshPolygons(desdorfTestData.meshService.loadMesh("Desdorf").polygons, desdorfTestData.getGridCellBounds().getProjection())
                .writeTmpFile();
    }

}
