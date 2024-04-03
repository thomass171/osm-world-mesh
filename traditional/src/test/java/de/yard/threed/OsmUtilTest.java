package de.yard.threed;


import de.yard.threed.core.Vector2;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.core.testutil.TestUtils;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.VectorXZ;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * Created on 17.05.19.
 */
public class OsmUtilTest {
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(OsmUtilTest.class);

    @BeforeAll
    public static void setup(){
        TerrainMesh.meshFactoryInstance = new TraditionalMeshFactory();
    }

    @Test
    public void testDirection() {
        MapNode n0 = new MapNode(new VectorXZ(0,0),null,null);
        MapNode n1 = new MapNode(new VectorXZ(2,0),null,null);
        MapNode n2 = new MapNode(new VectorXZ(4,2),null,null);

        MapWay w0 = new MapWay(n0,null);
        w0.add(n1,null);

        MapWay w1 = new MapWay(n1,null);
        w1.add(n2,null);

        TestUtils.assertVector2(new Vector2(1,0), OsmUtil.getDirectionToNode(w0,n1),"dir");
        TestUtils.assertVector2(new Vector2(-1,0), OsmUtil.getDirectionFromNode(w0,n1),"dir");

        TestUtils.assertVector2(new Vector2(-0.7071067811865475,-0.7071067811865475), OsmUtil.getDirectionToNode(w1,n1),"dir");
        TestUtils.assertVector2(new Vector2(0.7071067811865475,0.7071067811865475), OsmUtil.getDirectionFromNode(w1,n1),"dir");
    }
}
