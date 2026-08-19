package de.yard.threed.modules;

import de.yard.threed.osm2mesh.testutils.DefaultMockingSceneryTest;
import de.yard.threed.osm2mesh.testutils.DesdorfTestData;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.modules.common.WayModule;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2world.MapWay;
import org.junit.jupiter.api.Test;

import static de.yard.threed.osm2mesh.DesdorfExpectations.expectedK43;

public abstract class AbstractWayModuleTest extends DefaultMockingSceneryTest {

    @Test
    public void testWay24879711() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());

        MapWay way = desdorfTestData.fullMapData.findMapWay(24879711);
        WayModule wayModule = new WayModule();

        SceneryWayObject w = (SceneryWayObject) wayModule.applyTo(way.getLastSegment(), desdorfTestData.terrainMesh, SceneryContext.getInstance()).get(0);
    }

    @Test
    public void testWay107468171() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(getMeshServiceFactory(), getValidatorServiceFactory());

        MapWay way = desdorfTestData.fullMapData.findMapWay(107468171);
        WayModule wayModule = new WayModule();

        for (int i=0;i<expectedK43.length;i++) {
            SceneryWayObject w = (SceneryWayObject) wayModule.applyTo(way.getSegment(i), desdorfTestData.terrainMesh, SceneryContext.getInstance()).get(0);
        }
    }
}
