package de.yard.threed.modules;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.osm2mesh.testutils.DefaultMockingSceneryTest;
import de.yard.threed.osm2mesh.testutils.MeshTestData;
import de.yard.threed.osm2scenery.modules.BuildingModule;
import de.yard.threed.osm2scenery.modules.SurfaceAreaModule;
import de.yard.threed.osm2scenery.util.SvgWriter;
import de.yard.threed.osm2world.MapWay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractSurfaceAreaModuleTest extends DefaultMockingSceneryTest {

    protected abstract MeshTestData setupForDesdorf(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception;

    MeshTestData desdorfTestData;
    SurfaceAreaModule surfaceAreaModule;

    @BeforeEach
    public void setUp() throws Exception {
        desdorfTestData = setupForDesdorf(getMeshServiceFactory(), getValidatorServiceFactory());
        surfaceAreaModule = new SurfaceAreaModule(desdorfTestData.meshService, desdorfTestData.gridCellBounds.getProjection(), "Desdorf");
    }

    @Test
    public void testSurface322751236() throws Exception {

        MapWay way = desdorfTestData.fullMapData.findMapWay(322751236);

        surfaceAreaModule.applyTo(way, desdorfTestData.fullMapData);

        SvgWriter.build()
                .addMeshPolygons(desdorfTestData.meshService.loadMesh("Desdorf").polygons, desdorfTestData.getGridCellBounds().getProjection())
                .writeTmpFile();
    }

}
