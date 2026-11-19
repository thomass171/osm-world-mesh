package de.yard.threed.modules;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.osm2mesh.testutils.DefaultMockingSceneryTest;
import de.yard.threed.osm2mesh.testutils.MeshTestData;
import de.yard.threed.osm2scenery.modules.BuildingModule;
import de.yard.threed.osm2scenery.util.SvgWriter;
import de.yard.threed.osm2world.MapWay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractBuildingModuleTest extends DefaultMockingSceneryTest {

    protected abstract MeshTestData setupForDesdorf(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception;

    MeshTestData desdorfTestData;
    BuildingModule buildingModule;

    @BeforeEach
    public void setUp() throws Exception {
        desdorfTestData = setupForDesdorf(getMeshServiceFactory(), getValidatorServiceFactory());
        buildingModule = new BuildingModule(desdorfTestData.meshServiceFacade, desdorfTestData.gridCellBounds.getProjection(), "Desdorf");
    }

    @Test
    public void testBuilding322751224() throws Exception {

        MapWay way = desdorfTestData.fullMapData.findMapWay(322751224);

        buildingModule.applyTo(way, desdorfTestData.fullMapData);

        SvgWriter.build()
                .addMeshPolygons(desdorfTestData.meshServiceFacade.loadMesh("Desdorf").polygons, desdorfTestData.getGridCellBounds().getProjection())
                .writeTmpFile();
    }

}
