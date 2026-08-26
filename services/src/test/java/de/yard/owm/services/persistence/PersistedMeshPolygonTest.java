package de.yard.owm.services.persistence;

import de.yard.owm.services.PlatformService;
import de.yard.owm.services.mesh.MeshService;
import de.yard.owm.services.osm.OsmService;
import de.yard.owm.testutils.TestServices;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshPolygonType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.parameters.P;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestServices.class)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@Slf4j
public class PersistedMeshPolygonTest {

    @Autowired
    private OsmService osmService;

    @Autowired
    private MeshService meshService;

    @Autowired
    TerrainMeshManager terrainMeshManager;

    @Autowired
    private MeshNodeRepository meshNodeRepository;

    @Autowired
    private MeshRepository meshRepository;

    @Autowired
    private MeshPolygonRepository meshPolygonRepository;

    @Autowired
    private MeshPolygonNodeRepository meshPolygonNodeRepository;

    @Autowired
    private TestServices testServices;

    @BeforeEach
    void setUp() throws Exception {
        testServices.cleanup();
    }

    @ParameterizedTest
    @CsvSource({
            "true",
            "false"
    })
    public void testPolygonValidness(boolean valid) throws Exception {

        PersistedMesh persistedMesh = new PersistedMesh();
        persistedMesh.setName("testmesh");
        meshRepository.save(persistedMesh);

        PersistedMeshPolygon meshPolygon = new PersistedMeshPolygon();
        meshPolygon.setType(MeshPolygonType.WAY);
        meshPolygon.setMesh(persistedMesh);

        if (valid) {
            meshPolygon.addNode(meshService.buildMeshNode(DesdorfTestData.SW, persistedMesh));
            meshPolygon.addNode(meshService.buildMeshNode(DesdorfTestData.SE, persistedMesh));
            meshPolygon.addNode(meshService.buildMeshNode(DesdorfTestData.NE, persistedMesh));
            meshPolygon.addNode(meshService.buildMeshNode(DesdorfTestData.NW, persistedMesh));
        }else {
            meshPolygon.addNode(meshService.buildMeshNode(DesdorfTestData.SW, persistedMesh));
            meshPolygon.addNode(meshService.buildMeshNode(DesdorfTestData.NE, persistedMesh));
            meshPolygon.addNode(meshService.buildMeshNode(DesdorfTestData.SE, persistedMesh));
            meshPolygon.addNode(meshService.buildMeshNode(DesdorfTestData.NW, persistedMesh));
        }
        meshPolygon.close();
        assertEquals(valid, meshPolygon.isValidForSave());

        meshPolygonRepository.save(meshPolygon);
    }

}
