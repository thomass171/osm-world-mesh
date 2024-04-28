package de.yard.threed;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Degree;
import de.yard.threed.core.Pair;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.core.testutil.TestUtils;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Created on 17.05.18.
 */
public class JtsUtilTest {
    //EngineHelper platform = PlatformHomeBrew.init(new HashMap<String, String>());
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(JtsUtilTest.class);

    @BeforeAll
    public static void setup() {
        TerrainMesh.meshFactoryInstance = new TraditionalMeshFactory();
    }

    @Test
    public void testExtendLine() {
        LineSegment line = new LineSegment(new Coordinate(1, 1), new Coordinate(3, 1));
        LineSegment result = JtsUtil.extendLineSegment(line, 0.5f);
        TestUtil.assertCoordinate("result.p0", result.p0, new Coordinate(1, 1));
        TestUtil.assertCoordinate("result.p1", result.p1, new Coordinate(3.5, 1));

        result = JtsUtil.extendLineSegment(line, -0.5f);
        TestUtil.assertCoordinate("result.p0", result.p0, new Coordinate(0.5f, 1));
        TestUtil.assertCoordinate("result.p1", result.p1, new Coordinate(3, 1));

        result = JtsUtil.extendLineSegment2(line, 0.5f);
        TestUtil.assertCoordinate("result.p0", result.p0, new Coordinate(0.5f, 1));
        TestUtil.assertCoordinate("result.p1", result.p1, new Coordinate(3.5, 1));
    }

    /**
     * Seam an Desdorf farmland split
     */
    @Test
    public void testSeam() {
        //links
        Polygon p0 = (Polygon) JtsUtil.buildFromWKT("POLYGON ((-194.72693444838995 -147.54421705226304, -189.271 -144.585, -128.129 -105.947, -29.583631658243526 -46.71583445938842, -10.331215484378772 -69.97815636074009, 1.8212668641783163 -84.73414056445222, 25.95269498944791 -116.65819608688334, -194.72693444838995 -147.54421705226304))");
        Polygon cutway = (Polygon) JtsUtil.buildFromWKT("POLYGON ((-32.91058627006136 -42.695939583214184, -10.331215484378772 -69.97815636074009, 1.8212668641783163 -84.73414056445222, 26.540499282125694 -117.43581673902032, 29.33255140757157 -115.32526114183905, 4.56873300230728 -82.5658624873056, -7.57278384423451 -67.82384559238491, -26.943353876601886 -39.03762053739388, -32.91058627006136 -42.695939583214184))");
        List<Coordinate> seam = JtsUtil.getSeam(p0, cutway);
        Assertions.assertEquals(4, seam.size(), "seam.size");

        Vector2 normaluntenrechts = JtsUtil.getNormalAtCoordinate(p0, seam.get(3));
        TestUtils.assertVector2(new Vector2(0.9240562991837489, -0.38225640078203255), normaluntenrechts);

        //rechts
        Polygon p1 = (Polygon) JtsUtil.buildFromWKT("POLYGON ((-24.02487391336316 -43.37471649790983, -2.227 -30.273, 113.387 27.179, 132.547 -4.792, 156.252 -44.344, 191.11145582579155 -93.54280042122068, 27.936520561168752 -116.38054255648765, 29.33255140757157 -115.32526114183905, 4.56873300230728 -82.5658624873056, -7.57278384423451 -67.82384559238491, -24.02487391336316 -43.37471649790983))");
        seam = JtsUtil.getSeam(p1, cutway);
        //offset causes invalid polygon
        Polygon resized = JtsUtil.createResizedPolygon(p1, seam, 1.8);
        assertNull(resized);
        //better offset
        resized = JtsUtil.createResizedPolygon(p1, seam, 0.2);
        assertNotNull(resized);
    }

    @Test
    public void removeHoleOnEdge() {
        Polygon pwithHoleOnEdge = (Polygon) JtsUtil.buildFromWKT("POLYGON ((-203.68804451804323 -143.90224592432773, -146.037677864888 -81.68671650237948, -134.33298205417938 -100.2523716696354, -183.2535593278347 -131.2822714651834, -203.68804451804323 -143.90224592432773), \n" +
                "  (-148.89352223268804 -84.76870677589056, -184.02080290124107 -122.67761335918948, -141.847 -96.174, -148.89352223268804 -84.76870677589056))");

        Polygon pwithouthole = JtsUtil.removeHoleOnEdge(pwithHoleOnEdge);
        //nicht fertig
        assertNotNull(pwithouthole, "pwithouthole");
        double diff = Math.abs(pwithHoleOnEdge.getArea() - pwithouthole.getArea());
        logger.debug("diff=" + diff);
        Assertions.assertTrue(diff < 0.0000001, "polygon.sizes.equal");
    }

    @Test
    public void testInside() {
        Polygon p0 = (Polygon) JtsUtil.buildFromWKT("POLYGON ((-32.91058627006136 -42.695939583214184, -10.331215484378772 -69.97815636074009, 1.8212668641783163 -84.73414056445222, 26.540499282125694 -117.43581673902032, 29.33255140757157 -115.32526114183905, 4.56873300230728 -82.5658624873056, -7.57278384423451 -67.82384559238491, -26.943353876601886 -39.03762053739388, -32.91058627006136 -42.695939583214184))");
        Point p = (Point) JtsUtil.buildFromWKT("POINT (1.8997665167586097 -84.6721905479488)");


        Assertions.assertTrue(JtsUtil.isPartOfPolygon(p.getCoordinate(), p0), "polygon.inside");
    }

    @Test
    public void testCommon() {
        LineString ls1 = (LineString) JtsUtil.buildFromWKT("LINESTRING (1 2, 1 3, 1 4, 1 5, 1 6, 1 7, 1 8, 1 9)");
        LineString ls2 = (LineString) JtsUtil.buildFromWKT("LINESTRING (1 4, 1 5, 1 6)");
        LineString ls3 = (LineString) JtsUtil.buildFromWKT("LINESTRING (1 6, 1 5, 1 4)");

        int[] fromto = JtsUtil.findCommon(ls1, ls2);
        Assertions.assertEquals(2, fromto[0]);
        Assertions.assertEquals(4, fromto[1]);
        LineString[] result = JtsUtil.removeCoordinatesFromLine(ls1, fromto);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(3, result[0].getNumPoints(), "points");
        Assertions.assertEquals(4, result[1].getNumPoints(), "points");

        fromto = JtsUtil.findCommon(ls1, ls3);
        Assertions.assertEquals(2, fromto[0]);
        Assertions.assertEquals(4, fromto[1]);

        LineString ls4 = (LineString) JtsUtil.buildFromWKT("LINESTRING (1 9, 1 2)");
        LineString ls5 = (LineString) JtsUtil.buildFromWKT("LINESTRING (1 2, 1 9)");

        fromto = JtsUtil.findCommon(ls1, ls4);
        Assertions.assertEquals(7, fromto[0]);
        Assertions.assertEquals(0, fromto[1]);
        result = JtsUtil.removeCoordinatesFromLine(ls1, fromto);
        Assertions.assertEquals(1, result.length);
        //bleiben 8
        Assertions.assertEquals(8, result[0].getNumPoints(), "points");

        fromto = JtsUtil.findCommon(ls1, ls5);
        Assertions.assertEquals(7, fromto[0]);
        Assertions.assertEquals(0, fromto[1]);


    }

    /**
     * Real life cases.
     */
    @Test
    public void testCommon1() {
        LineString ls1 = (LineString) JtsUtil.buildFromWKT("LINESTRING (-70.003 -57.43492702668433, -70.003 -52.557, -60.003 -52.557, -60.003 -67.557, -61.516516280305936 -67.557)");
        LineString partcandidate = (LineString) JtsUtil.buildFromWKT(" LINESTRING (-74.09272039689336 -52.557, -70.003 -52.557)");

        int[] fromto;
        fromto = JtsUtil.findCommon(ls1, partcandidate);
        assertNull(fromto);
    }

    /**
     * Real life cases.
     */
    @Test
    public void testCommon2() {
        LineString ls6 = (LineString) JtsUtil.buildFromWKT("LINESTRING (1 2, 1 3, 1 4, 1 5, 1 6, 1 7, 1 8, 1 9, 1 2)");
        LineString[] result = JtsUtil.removeCoordinatesFromLine(ls6, new int[]{6, 7});
        Assertions.assertEquals(1, result.length);
        //liefert eine zwishcne 18 und 19 open line.
        Assertions.assertEquals(8, result[0].getNumPoints(), "points");


    }

    @Test
    public void testCreateTriangleForSector() {
        Coordinate origin = new Coordinate(3, 4, 0);
        // too large
        Polygon sectorTriangle = JtsUtil.createTriangleForSector(origin, new Degree(0), new Degree(270), 6);
        assertNull(sectorTriangle);
        // also no triangle possible
        sectorTriangle = JtsUtil.createTriangleForSector(origin, new Degree(0), new Degree(180), 6);
        assertNull(sectorTriangle);
        // roughly triangle possible
        sectorTriangle = JtsUtil.createTriangleForSector(origin, new Degree(0), new Degree(179), 6);
        assertNotNull(sectorTriangle);

        // b)
        sectorTriangle = JtsUtil.createTriangleForSector(origin, new Degree(270), new Degree(180), 6);
        assertNull(sectorTriangle);

        // c)
        sectorTriangle = JtsUtil.createTriangleForSector(origin, new Degree(90), new Degree(180), 6);
        assertNotNull(sectorTriangle);

        // e)
        sectorTriangle = JtsUtil.createTriangleForSector(origin, new Degree(180), new Degree(90), 6);
        assertNull(sectorTriangle);
    }

    /**
     * sketch ??
     */
    @Test
    public void testReduceSector() {
        Coordinate origin = new Coordinate(3, 4, 0);
        // a
        Pair<Degree,Degree> reducedSector = JtsUtil.reduceSector(new Pair<>(new Degree(0), new Degree(270)), new Degree(90));
        assertEquals(new Degree(90), reducedSector.getFirst());
        assertEquals(new Degree(180), reducedSector.getSecond());

        // d)
        reducedSector = JtsUtil.reduceSector(new Pair<>(new Degree(0), new Degree(180)), new Degree(90));
        assertEquals(new Degree(45), reducedSector.getFirst());
        assertEquals(new Degree(135), reducedSector.getSecond());

        // b)
        reducedSector = JtsUtil.reduceSector(new Pair<>(new Degree(270), new Degree(180)), new Degree(90));
        assertEquals(new Degree(0), reducedSector.getFirst());
        assertEquals(new Degree(90), reducedSector.getSecond());

        // c)
        reducedSector = JtsUtil.reduceSector(new Pair<>(new Degree(90), new Degree(180)), new Degree(90));
        assertEquals(new Degree(90), reducedSector.getFirst());
        assertEquals(new Degree(180), reducedSector.getSecond());

        // e)
        reducedSector = JtsUtil.reduceSector(new Pair<>(new Degree(180), new Degree(90)), new Degree(90));
        assertEquals(new Degree(270), reducedSector.getFirst());
        assertEquals(new Degree(0), reducedSector.getSecond());
    }
}
