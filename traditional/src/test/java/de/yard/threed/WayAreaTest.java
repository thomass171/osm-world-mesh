package de.yard.threed;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.core.testutil.TestUtils;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.scenery.components.WayArea;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.yard.threed.osm2graph.osm.JtsUtil.toVector2;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * 11.4.19: Einfachere gezieltere Tests für WayArea als in OsmGridTest
 */
public class WayAreaTest {
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(WayAreaTest.class);
    double width = 0.5, width2 = width / 2;

    @BeforeAll
    public static void setup(){
        TerrainMesh.meshFactoryInstance = new TraditionalMeshFactory();
    }

    /**
     *
     */
    @Test
    public void testReduce() {
        WayArea wayArea = buildWayArea();
        CoordinatePair shift = wayArea.shiftStart(0.7);
        wayArea.replaceStart(shift);
        TestUtils.assertVector2( new Vector2(0.7, width2), toVector2(wayArea.getPair(0).getSecond()),"wayArea.pair[0].left");
        TestUtils.assertVector2(new Vector2(0.7, -width2), toVector2(wayArea.getPair(0).getFirst()),"wayArea.pair[0].right");
        TestUtils.assertVector2(new Vector2(3, width2), toVector2(wayArea.getPair(1).getSecond()),"wayArea.pair[1].left");
        TestUtils.assertVector2( new Vector2(3, -width2), toVector2(wayArea.getPair(1).getFirst()),"wayArea.pair[1].right");
        TestUtils.assertVector2( new Vector2(5, width2), toVector2(wayArea.getPair(2).getSecond()),"wayArea.pair[2].left");
        TestUtils.assertVector2( new Vector2(5, -width2), toVector2(wayArea.getPair(2).getFirst()),"wayArea.pair[2].right");
        shift = wayArea.shiftEnd(-0.7);
        wayArea.replaceEnd(shift);
        TestUtils.assertVector2( new Vector2(0.7, width2), toVector2(wayArea.getPair(0).getSecond()),"wayArea.pair[0].left");
        TestUtils.assertVector2(new Vector2(0.7, -width2), toVector2(wayArea.getPair(0).getFirst()),"wayArea.pair[0].right");
        TestUtils.assertVector2( new Vector2(3, width2), toVector2(wayArea.getPair(1).getSecond()),"wayArea.pair[1].left");
        TestUtils.assertVector2(new Vector2(3, -width2), toVector2(wayArea.getPair(1).getFirst()),"wayArea.pair[1].right");
        TestUtils.assertVector2( new Vector2(5 - 0.7, width2), toVector2(wayArea.getPair(2).getSecond()),"wayArea.pair[2].left");
        TestUtils.assertVector2( new Vector2(5 - 0.7, -width2), toVector2(wayArea.getPair(2).getFirst()),"wayArea.pair[2].right");

    }

    @Test
    public void testAdd() {
        List<Vector2> centerline = new ArrayList<>();
        WayArea wayArea = buildWayArea();

        CoordinatePair shiftleft = wayArea.shift(1, -0.2);
        CoordinatePair shiftright = wayArea.shift(1, 0.4);
        wayArea.replace(1, new CoordinatePair[]{shiftleft, shiftright});
        //logical length bleibt
        assertEquals(3, wayArea.getLength(), "wayArea.length");
        assertEquals(9, wayArea.poly.polygon.getCoordinates().length, "wayArea.length");

        TestUtils.assertVector2(new Vector2(0, width2), toVector2(wayArea.getPair(0).getSecond()),"wayArea.pair[0].left");
        TestUtils.assertVector2( new Vector2(0, -width2), toVector2(wayArea.getPair(0).getFirst()),"wayArea.pair[0].right");
        Assertions.assertNull( wayArea.getPair(1),"wayArea.pair[1].left");
        Assertions.assertNull( wayArea.getPair(1),"wayArea.pair[1].right");
        TestUtils.assertVector2( new Vector2(3 - 0.2, width2), toVector2(wayArea.getMultiplePair(1)[0].getSecond()),"wayArea.pair[1].left");
        TestUtils.assertVector2(new Vector2(3 - 0.2, -width2), toVector2(wayArea.getMultiplePair(1)[0].getFirst()),"wayArea.pair[1].right");
        TestUtils.assertVector2( new Vector2(3 + 0.4, width2), toVector2(wayArea.getMultiplePair(1)[1].getSecond()),"wayArea.pair[1].left");
        TestUtils.assertVector2( new Vector2(3 + 0.4, -width2), toVector2(wayArea.getMultiplePair(1)[1].getFirst()),"wayArea.pair[1].right");
        TestUtils.assertVector2(new Vector2(5, width2), toVector2(wayArea.getPair(2).getSecond()),"wayArea.pair[2].left");
        TestUtil.assertCoordinate("wayArea.pair[2].right", new Coordinate(5, -width2), wayArea.getPair(2).getFirst());
    }

    @Test
    public void testSegments() {
        List<Vector2> centerline = new ArrayList<>();
        WayArea wayArea = buildWayArea();
        //1-1-1
        assertEquals(3, wayArea.getLength(), "wayArea.length");
        assertEquals(3, wayArea.getRawLength(), "wayArea.rawlength");
        assertEquals(1, wayArea.getSegmentCount(), "wayArea.getSegmentCount");
        assertEquals(0, wayArea.getSegmentIndex(0, true), "wayArea.getSegment[0].Start");
        assertEquals(2, wayArea.getSegmentIndex(0, false), "wayArea.getSegment[0].End");

        wayArea = buildWayArea();
        wayArea.replace(1, new CoordinatePair[]{wayArea.getPair(1), buildPair(3.5)});
        //1-2-1: 0-3,3.5-5
        assertEquals(3, wayArea.getLength(), "wayArea.length");
        assertEquals(4, wayArea.getRawLength(), "wayArea.rawlength");
        assertEquals(2, wayArea.getSegmentCount(), "wayArea.getSegmentCount");
        assertEquals(0, wayArea.getSegmentIndex(0, true), "wayArea.getSegment[0].Start");
        assertEquals(1, wayArea.getSegmentIndex(0, false), "wayArea.getSegment[0].End");
        assertEquals(1, wayArea.getSegmentIndex(1, true), "wayArea.getSegment[1].Start");
        assertEquals(2, wayArea.getSegmentIndex(1, false), "wayArea.getSegment[1].End");

        assertEquals(2, wayArea.getPairsOfSegment(0).length, "wayArea.getPairsOfSegment[0].size");
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment[0]", new Coordinate(0, -width2), wayArea.getPairsOfSegment(0)[0].right(), 0.01);
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment[0]", new Coordinate(3, -width2), wayArea.getPairsOfSegment(0)[1].right(), 0.01);
        assertEquals(2, wayArea.getPairsOfSegment(1).length, "wayArea.getPairsOfSegment[1].size");
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment[1]", new Coordinate(3.5, -width2), wayArea.getPairsOfSegment(1)[0].right(), 0.01);
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment[1]", new Coordinate(5, -width2), wayArea.getPairsOfSegment(1)[1].right(), 0.01);

        wayArea = buildWayArea(4);
        wayArea.replace(1, new CoordinatePair[]{wayArea.getPair(1), buildPair(3.5)});
        //1-2-1-1: 0-3,3.5-4-5
        assertEquals(4, wayArea.getLength(), "wayArea.length");
        assertEquals(5, wayArea.getRawLength(), "wayArea.rawlength");
        assertEquals(2, wayArea.getSegmentCount(), "wayArea.getSegmentCount");
        assertEquals(0, wayArea.getSegmentIndex(0, true), "wayArea.getSegment[0].Start");
        assertEquals(1, wayArea.getSegmentIndex(0, false), "wayArea.getSegment[0].End");
        assertEquals(1, wayArea.getSegmentIndex(1, true), "wayArea.getSegment[1].Start");
        assertEquals(3, wayArea.getSegmentIndex(1, false), "wayArea.getSegment[1].End");

        assertEquals(2, wayArea.getPairsOfSegment(0).length, "wayArea.getPairsOfSegment[0].size");
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment[0]", new Coordinate(0, -width2), wayArea.getPairsOfSegment(0)[0].right(), 0.01);
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment[0]", new Coordinate(3, -width2), wayArea.getPairsOfSegment(0)[1].right(), 0.01);
        assertEquals(3, wayArea.getPairsOfSegment(1).length, "wayArea.getPairsOfSegment[1].size");
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment[1]", new Coordinate(3.5, -width2), wayArea.getPairsOfSegment(1)[0].right(), 0.01);
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment[1]", new Coordinate(4, -width2), wayArea.getPairsOfSegment(1)[1].right(), 0.01);
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment[2]", new Coordinate(5, -width2), wayArea.getPairsOfSegment(1)[2].right(), 0.01);

        TerrainMesh tm = TerrainMesh.init(SceneryTestUtil.gridCellBounds);

        wayArea = buildWayArea();
        wayArea.replaceStart(new CoordinatePair[]{wayArea.getStartPair(tm)[0], buildPair(1)});
        wayArea.replace(1, new CoordinatePair[]{wayArea.getPair(1), buildPair(3.5)});
        //2-2-1: 0,1-3,3.5-5
        assertEquals(2, wayArea.getSegmentCount(), "wayArea.getSegmentCount");
        assertEquals(0, wayArea.getSegmentIndex(0, true), "wayArea.getSegment[0].Start");
        assertEquals(1, wayArea.getSegmentIndex(0, false), "wayArea.getSegment[0].End");

        wayArea = buildWayArea();
        wayArea.replaceStart(new CoordinatePair[]{wayArea.getStartPair(tm)[0], buildPair(1)});
        wayArea.replaceEnd(new CoordinatePair[]{buildPair(4.5), wayArea.getPair(2),});
        //2-1-2: 0,1-3-4.5,5
        assertEquals(1, wayArea.getSegmentCount(), "wayArea.getSegmentCount");
        assertEquals(0, wayArea.getSegmentIndex(0, true), "wayArea.getSegment[0].Start");
        assertEquals(2, wayArea.getSegmentIndex(0, false), "wayArea.getSegment[0].End");
        assertEquals(5, wayArea.getPairsOfSegment(0).length, "wayArea.getPairsOfSegment0.size");
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment0[0]", new Coordinate(0, -width2), wayArea.getPairsOfSegment(0)[0].right(), 0.01);
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment0[1]", new Coordinate(1, -width2), wayArea.getPairsOfSegment(0)[1].right(), 0.01);
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment0[2]", new Coordinate(3, -width2), wayArea.getPairsOfSegment(0)[2].right(), 0.01);
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment0[3]", new Coordinate(4.5, -width2), wayArea.getPairsOfSegment(0)[3].right(), 0.01);
        TestUtil.assertCoordinate("wayArea.getPairsOfSegment0[4]", new Coordinate(5, -width2), wayArea.getPairsOfSegment(0)[4].right(), 0.01);

    }

    @Test
    public void testVerticalReduce() {
        WayArea wayArea = buildWayArea();
        double offset = 0.2;
        CoordinatePair reduced = wayArea.reduce(1, offset, null);
        wayArea.replace(new int[]{1}, reduced);
        TestUtils.assertVector2(new Vector2(0, width2), toVector2(wayArea.getPair(0).getSecond()),"wayArea.pair[0].left");
        TestUtils.assertVector2( new Vector2(0, -width2), toVector2(wayArea.getPair(0).getFirst()),"wayArea.pair[0].right");
        TestUtils.assertVector2( new Vector2(3, width2 - offset), toVector2(wayArea.getPair(1).getSecond()),"wayArea.pair[1].left");
        TestUtils.assertVector2(new Vector2(3, -width2 + offset), toVector2(wayArea.getPair(1).getFirst()),"wayArea.pair[1].right");
        TestUtils.assertVector2( new Vector2(5, width2), toVector2(wayArea.getPair(2).getSecond()),"wayArea.pair[2].left");
        TestUtils.assertVector2( new Vector2(5, -width2), toVector2(wayArea.getPair(2).getFirst()),"wayArea.pair[2].right");

    }

    private CoordinatePair buildPair(double x) {
        return new CoordinatePair(new Coordinate(x, -width2), new Coordinate(x, width2));
    }

    private WayArea buildWayArea() {
        return buildWayArea(0);
    }

    private WayArea buildWayArea(int flag) {
        List<Vector2> centerline = new ArrayList<>();
        centerline.add(new Vector2(0, 0));
        centerline.add(new Vector2(3, 0));
        if (flag == 4) {
            centerline.add(new Vector2(4, 0));
        }
        centerline.add(new Vector2(5, 0));

        WayArea wayArea = (WayArea) WayArea.buildOutlinePolygonFromCenterLine(centerline, null, width, null, null);
        assertEquals((flag == 4) ? 4 : 3, wayArea.getLength(), "wayArea.length");
        TestUtils.assertVector2(new Vector2(0, width2), toVector2(wayArea.getPair(0).getSecond()),"wayArea.pair[0].left");
        TestUtils.assertVector2( new Vector2(0, -width2), toVector2(wayArea.getPair(0).getFirst()),"wayArea.pair[0].right");
        TestUtils.assertVector2( new Vector2(3, width2), toVector2(wayArea.getPair(1).getSecond()),"wayArea.pair[1].left");
        TestUtils.assertVector2( new Vector2(3, -width2), toVector2(wayArea.getPair(1).getFirst()),"wayArea.pair[1].right");
        return wayArea;
    }

}
