package de.yard.owm.modules;

import de.yard.owm.services.mesh.MeshService;
import de.yard.owm.services.persistence.*;
import de.yard.owm.testutils.TestServices;
import de.yard.owm.testutils.TestUtils;
import de.yard.threed.MeshServiceFactory;
import de.yard.threed.ValidatorServiceFactory;
import de.yard.threed.core.platform.Platform;
import de.yard.threed.core.testutil.CoreTestFactory;
import de.yard.threed.core.testutil.PlatformFactoryTestingCore;
import de.yard.threed.modules.AbstractWayModuleTest;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.threed.osm2mesh.testutils.MeshServiceMock;
import de.yard.threed.osm2mesh.testutils.ValidatorServiceFacade;
import de.yard.threed.osm2mesh.testutils.ValidatorServiceForMocking;
import de.yard.threed.osm2world.MetricMapProjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = {TestServices.class})
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public class WayModuleTest extends AbstractWayModuleTest {

    @Autowired
    private MeshNodeRepository meshNodeRepository;

    @Autowired
    private MeshLineRepository meshLineRepository;

    @Autowired
    private MeshPolygonRepository meshPolygonRepository;

    @Autowired
    private MeshPolygonNodeRepository meshPolygonNodeRepository;

    @Autowired
    private MeshAreaRepository meshAreaRepository;

    @Autowired
    private OsmWayRepository osmWayRepository;

    @Autowired
    private OsmNodeRepository osmNodeRepository;

    @Autowired
    private OsmWayNodeRepository osmWayNodeRepository;

    @Autowired
    private TestServices testServices;

    @Autowired
    private TerrainMeshManager manager;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    MeshService meshService;

    @Autowired
    TerrainMeshManager terrainMeshManager;

    @BeforeEach
    public void setUp() throws Exception {
        testServices.cleanup();
        super.setUp();
    }

    @AfterEach
    void teardown() {

    }

    @Override
    protected DesdorfTestData setupForDesdorf(MeshServiceFactory meshServiceFactory, ValidatorServiceFactory validatorServiceFactory) throws Exception {
        //return OsmServiceTest.setupForDesdorf(meshService, testServices, terrainMeshManager);
        return new DesdorfTestData(meshServiceFactory, validatorServiceFactory);

    }

    @Override
    protected MeshServiceFactory getMeshServiceFactory() {
        return gridCellBounds -> MeshService.buildMeshServiceFacade();
    }

    @Override
    protected ValidatorServiceFactory getValidatorServiceFactory() {
        return new ValidatorServiceFactory() {
            @Override
            public ValidatorServiceFacade createService() {
                return testServices;
            }
        };
    }

}
