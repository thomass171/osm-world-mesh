package de.yard.owm.testutils;

import de.yard.owm.services.mesh.MeshService;
import de.yard.owm.services.osm.OsmElementService;
import de.yard.owm.services.persistence.MeshAreaRepository;
import de.yard.owm.services.persistence.MeshLineRepository;
import de.yard.owm.services.persistence.MeshNodeRepository;
import de.yard.owm.services.persistence.OsmNodeRepository;
import de.yard.owm.services.persistence.OsmWayNodeRepository;
import de.yard.owm.services.persistence.OsmWayRepository;
import de.yard.owm.services.persistence.PersistedOsmWay;
import de.yard.owm.services.persistence.TerrainMeshManager;
import de.yard.threed.TestUtil;
import de.yard.threed.osm2scenery.modules.HighwayModule;
import de.yard.threed.osm2scenery.modules.OsmClassifier;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2world.MapWay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * For easy reuse of "Desdorf" in tests
 * 19.3.26 deprecated in favor of DesdorfTestdata in OsmServiceTest
 */
@Deprecated
public class DesdorfTestUtil {

    public ServicesTestUtil stu;
    public MapWay k41Low, k41Upper;
    OsmElementService osmElementService;

    public DesdorfTestUtil(TerrainMeshManager terrainMeshManager, OsmElementService osmElementService) throws Exception {
        stu = new ServicesTestUtil("Desdorf.osm.xml", terrainMeshManager);
        this.osmElementService=osmElementService;
        k41Low = stu.mapData.findMapWays(24927839).get(0);
        k41Upper = stu.mapData.findMapWays(182152619).get(0);
    }

    public void processK41Low() throws MeshInconsistencyException, OsmProcessException {

        //Processor processor = sb.execute(desdorfk41, configsuffix, "Desdorf", false, customconfig, MATERIAL_FLIGHT).processor;
        HighwayModule roadModule = new HighwayModule(null,null);
        List<SceneryObject> sceneryObjects = null;//osmElementService.process(k41Low, List.of(roadModule), stu.terrainMesh, stu.sceneryContext, OsmClassifier.LOD_BASIC);

        stu.terrainMesh.writeToSvg();

        assertEquals(1, sceneryObjects.size(), "scenery.objects");

    }

    public void processK41Upper() throws MeshInconsistencyException, OsmProcessException {
        HighwayModule roadModule = new HighwayModule(null,null);
        List<SceneryObject> sceneryObjects = null;//osmElementService.process(k41Upper, List.of(roadModule), stu.terrainMesh, stu.sceneryContext, OsmClassifier.LOD_BASIC);

        stu.terrainMesh.writeToSvg();

        assertEquals(1, sceneryObjects.size(), "scenery.objects");
    }
}


