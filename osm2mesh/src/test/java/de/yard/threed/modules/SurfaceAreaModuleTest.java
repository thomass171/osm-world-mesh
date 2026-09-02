package de.yard.threed.modules;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.core.platform.Platform;
import de.yard.threed.core.testutil.CoreTestFactory;
import de.yard.threed.core.testutil.PlatformFactoryTestingCore;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.threed.osm2mesh.testutils.MeshServiceMock;
import de.yard.threed.osm2mesh.testutils.ValidatorServiceForMocking;

public class SurfaceAreaModuleTest extends AbstractSurfaceAreaModuleTest {

    Platform platform = CoreTestFactory.initPlatformForTest(new PlatformFactoryTestingCore(), null);

    @Override
    protected DesdorfTestData setupForDesdorf(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception {
        return new DesdorfTestData(meshServiceFactory, validatorServiceFactory);
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
