package de.yard.threed.modules;

import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.core.platform.Platform;
import de.yard.threed.core.testutil.CoreTestFactory;
import de.yard.threed.core.testutil.PlatformFactoryTestingCore;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.threed.osm2mesh.testutils.MeshServiceMock;
import de.yard.threed.osm2mesh.testutils.ValidatorServiceForMocking;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.modules.common.WayModule;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.util.SvgWriter;
import de.yard.threed.osm2world.MapWay;
import org.junit.jupiter.api.Test;

import static de.yard.threed.TestUtil.validateMeshPolygon;
import static de.yard.threed.osm2mesh.DesdorfExpectations.*;
import static de.yard.threed.osm2scenery.SceneryWayConnectorTest.validateAttachPoint;
import static org.junit.jupiter.api.Assertions.*;

public class WayModuleTest extends AbstractWayModuleTest {

    Platform platform = CoreTestFactory.initPlatformForTest(new PlatformFactoryTestingCore(), null);





    @Override
    protected MeshServiceFactory getMeshServiceFactory() {
        return gridCellBounds -> new MeshServiceMock(gridCellBounds);
    }

    @Override
    protected ValidatorServiceFactory getValidatorServiceFactory() {
        return () -> new ValidatorServiceForMocking();
    }
}
