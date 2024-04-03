package de.yard.threed;

import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.core.OutlineBuilder;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * In core gibt es auch noch Tests.
 */
public class OutlineBuilderTest {
    //EngineHelper platform = PlatformHomeBrew.init(new HashMap<String, String>());
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));

    @BeforeAll
    public static void setup(){
        TerrainMesh.meshFactoryInstance = new TraditionalMeshFactory();
    }

    /**
     * Skizze 11c
     */
    @Test
    public void testSimpleOutline() {
        double offset = 3;
        double offset45 = Math.sqrt(offset * offset / 2);
        List<Vector2> vlist = new ArrayList<Vector2>();
        vlist.add(new Vector2(20, 30));
        vlist.add(new Vector2(25, 25));
        vlist.add(new Vector2(25, 30));
        vlist.add(new Vector2(25, 35));
        vlist.add(new Vector2(30, 40));
        vlist.add(new Vector2(35, 45));
        vlist.add(new Vector2(40, 40));
        vlist.add(new Vector2(45, 35));
        LineString centerline = JtsUtil.createLine(vlist);

        //rechts
        LineString outline = JtsUtil.createLine(OutlineBuilder.getOutline(vlist, 3));
        //System.out.println("centerline="+centerline);
        //System.out.println("outline="+outline);
        Assertions.assertFalse( centerline.intersects(outline),"intersect");

        outline = JtsUtil.createLine(OutlineBuilder.getOutline(vlist, -3));
        //System.out.println("centerline="+centerline);
        //System.out.println("outline="+outline);
        Assertions.assertFalse( centerline.intersects(outline),"intersect");


    }

    /**
     * Mit Desdorf,(super)detail,grid ist da ein Problem with width 4.1
     * Ursache ist aber eine zu kurze Edge wegen Gridnode=mapnode eines graphsegment.
     */
    @Test
    public void testOsm107468171() {

        LineString centerline =(LineString) JtsUtil.buildFromWKT ("LINESTRING (-248.9290008544922 -183.40699768066406, -218.61700439453125 -172.60899353027344, -189.8699951171875 -158.66099548339844, -189.8699951171875 -158.66099548339844, -163.13499450683594 -142.1529998779297, -112.73100280761719 -110.18299865722656, -64.31199645996094 -81.26300048828125, -11.993000030517578 -48.21200180053711, 12.939000129699707 -33.3849983215332, 107.86399841308594 19.58099937438965, 137.4600067138672 36.60200119018555)");
        List<Vector2> vlist = JtsUtil.createLine(centerline);

        //rechts
        LineString outline = JtsUtil.createLine(OutlineBuilder.getOutline(vlist, 3));
        Assertions.assertFalse( centerline.intersects(outline),"intersect");

        //links
        outline = JtsUtil.createLine(OutlineBuilder.getOutline(vlist, -3));
        //System.out.println("centerline="+centerline);
        //System.out.println("outline="+outline);
        Assertions.assertFalse(centerline.intersects(outline),"intersect");

    }
}
