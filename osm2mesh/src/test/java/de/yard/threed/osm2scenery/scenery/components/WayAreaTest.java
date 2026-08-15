package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.TestUtil;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.platform.Platform;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.core.testutil.CoreTestFactory;
import de.yard.threed.core.testutil.PlatformFactoryTestingCore;
import de.yard.threed.core.testutil.TestUtils;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.MainGrid;
import de.yard.threed.osm2mesh.testutils.*;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import de.yard.threed.osm2scenery.util.SvgWriter;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.MapWaySegment2;
import de.yard.threed.osm2world.OSMData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.yard.threed.osm2graph.osm.JtsUtil.toVector2;
import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.ROAD;
import static de.yard.threed.osm2world.Materials.ASPHALT;
import static org.junit.jupiter.api.Assertions.*;

public class WayAreaTest {

    Platform platform = CoreTestFactory.initPlatformForTest(new PlatformFactoryTestingCore(), null);


    @Test
    public void testDesdorfLowerK41() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(gridCellBounds -> new MeshServiceMock(gridCellBounds),()->new ValidatorServiceForMocking()        );

        MapWay way = desdorfTestData.fullMapData.findMapWay(24927839);
        double width = 7.7;
        WayArea wayArea = (WayArea) WayArea.buildOutlinePolygonFromCenterLine(way.getCenterline(way.getMapNodes()), way.getMapNodes(), width, null, null, way.segment2s.get(0));
        assertNotNull(wayArea);
        assertEquals(5, wayArea.node2position.size());

        // replace at 54286220
        //wayArea.replace(0,)

    }

    @Test
    public void testDesdorfUpperK41() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(gridCellBounds -> new MeshServiceMock(gridCellBounds),()->new ValidatorServiceForMocking()        );

        MapWay way = desdorfTestData.fullMapData.findMapWay(182152619);
        double width = 7.7;
        WayArea wayArea = (WayArea) WayArea.buildOutlinePolygonFromCenterLine(way.getCenterline(way.getMapNodes()), way.getMapNodes(), width, null, null, way.segment2s.get(0));
        assertNotNull(wayArea);
        assertEquals(6, wayArea.node2position.size());

        // starts at 255563538
        CoordinatePair startPair = wayArea.getMultiplePair(0)/*19.3.26 [0]*/;
        assertTrue(startPair.left().x < startPair.right().x);

        // 'shift' should go to '-x'
        CoordinatePair shiftedStart = wayArea.shiftStartOrEnd(way.getStartNode(), 5.5);
        assertTrue(shiftedStart.right().x < startPair.right().x);
        assertTrue(shiftedStart.left().x < startPair.left().x);

        // replace at 54286220
        //wayArea.replace(0,)

    }

    @Test
    public void testDesdorfK43() throws Exception {

        DesdorfTestData desdorfTestData = new DesdorfTestData(gridCellBounds -> new MeshServiceMock(gridCellBounds),()->new ValidatorServiceForMocking()        );

        MapWay way = desdorfTestData.fullMapData.findMapWay(107468171);
        double width = 7.7;
        MapWaySegment2 segment0=way.segment2s.get(0);
        WayArea wayArea = (WayArea) WayArea.buildOutlinePolygonFromCenterLine(MapWay.getCenterline(segment0.getMapNodes()), segment0.getMapNodes(), width, null, null, segment0);
        assertNotNull(wayArea);
        // segment has 5 nodes
        assertEquals(5, wayArea.node2position.size());

        // starts ends at ...
        CoordinatePair endPair = wayArea.getMultiplePair(4);
        assertTrue(endPair.left().x < endPair.right().x);

        // 'shift' should go to '-x'
        CoordinatePair shiftedEnd = wayArea.shiftStartOrEnd(segment0.getEndNode(), 5.5);
        assertTrue(shiftedEnd.right().x < endPair.right().x);
        assertTrue(shiftedEnd.left().x < endPair.left().x);

        // replace at ??
        //wayArea.replace(0,)

    }

    @Test
    public void testReduce() throws MeshInconsistencyException {
        double width = 0.5, width2 = width / 2;

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
    public void testVerticalReduce() throws MeshInconsistencyException {
        double width = 0.5, width2 = width / 2;

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
        double width = 0.5, width2 = width / 2;

        return new CoordinatePair(new Coordinate(x, -width2), new Coordinate(x, width2));
    }

    private WayArea buildWayArea() throws MeshInconsistencyException {
        return buildWayArea(0);
    }

    private WayArea buildWayArea(int flag) throws MeshInconsistencyException {
        double width = 0.5, width2 = width / 2;

        List<Vector2> centerline = new ArrayList<>();
        centerline.add(new Vector2(0, 0));
        centerline.add(new Vector2(3, 0));
        if (flag == 4) {
            centerline.add(new Vector2(4, 0));
        }
        centerline.add(new Vector2(5, 0));

        WayArea wayArea = (WayArea) WayArea.buildOutlinePolygonFromCenterLine(centerline, null, width, null, null, null);
        assertEquals((flag == 4) ? 4 : 3, wayArea.getLength(), "wayArea.length");
        TestUtils.assertVector2(new Vector2(0, width2), toVector2(wayArea.getPair(0).getSecond()),"wayArea.pair[0].left");
        TestUtils.assertVector2( new Vector2(0, -width2), toVector2(wayArea.getPair(0).getFirst()),"wayArea.pair[0].right");
        TestUtils.assertVector2( new Vector2(3, width2), toVector2(wayArea.getPair(1).getSecond()),"wayArea.pair[1].left");
        TestUtils.assertVector2( new Vector2(3, -width2), toVector2(wayArea.getPair(1).getFirst()),"wayArea.pair[1].right");
        return wayArea;
    }

}
