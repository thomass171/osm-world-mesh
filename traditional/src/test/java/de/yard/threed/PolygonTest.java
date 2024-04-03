package de.yard.threed;


import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.Poly2TriTriangulationUtil;
import de.yard.threed.osm2scenery.util.PolygonCollection;
import de.yard.threed.osm2world.MapArea;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.MetricMapProjection;
import de.yard.threed.osm2world.MultipolygonAreaBuilder;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.OSMRelation;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created on 17.05.18.
 */
public class PolygonTest {
    // im kleinen ist das Areodrome nicht komplett drin.
    static String TESTFILE = "/Users/thomas/Projekte/OSM2World/EDDK-Complete-Large.osm";

    @BeforeAll
    public static void setup(){
        TerrainMesh.meshFactoryInstance = new TraditionalMeshFactory();
    }

    /**
     * Soll aerodrom Test sein.
     * 29.4.19: Ob der noch eine Bewandnis hat?
     */
    //@Test
    public void testEDDK() {
        try {
            File inputfile = new File(TESTFILE);
            //9.4.19 OSM2World osm2World = OSM2World.buildInstance(inputfile, null);
            Util.notyet();
            OSMData osmData = null;//osm2World.getData();
            OSMRelation eddkaerodrome = null;
            for (OSMRelation r : osmData.getRelations()) {
                if (r.id == 2269304) {
                    eddkaerodrome = r;
                    break;
                }
            }
            assertNotNull(eddkaerodrome, "eddkaerodrome");
            MetricMapProjection prj = new MetricMapProjection(null);
            prj.setOrigin(osmData);
            Map<OSMNode, MapNode> nodeMap = SceneryBuilder.buildNodeMap(osmData, null, prj);
            Collection<MapArea> areas = MultipolygonAreaBuilder.createAreasForMultipolygon(eddkaerodrome, nodeMap);
            assertEquals(1, areas.size(), "eddkaerodrome.areas");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSimplePolygonSplit() {
        //Polygon rectangle = SceneryMesh.buildRectangle3();
        //Polygon[] result = JtsUtil.splitPolygon(rectangle);
        //assertEquals("result.length", 2, result.length);
        Polygon rectangleWithHole = SceneryMesh.buildRectangleWithHole();
        List<LineSegment> ocs = JtsUtil.getOutConnections(rectangleWithHole, 0);
        assertEquals(3, ocs.size(), "result.length");
        // das sind ja nun mal immer 3
        for (int i = 0; i < 4; i++) {
            assertEquals(3, JtsUtil.getOutConnections(rectangleWithHole, i).size(), "result.length");
        }
        Polygon[] splitresult = JtsUtil.removeHoleFromPolygonBySplitting(rectangleWithHole);
        assertEquals(2, splitresult.length, "splitresult.length");
        assertEquals(splitresult[0].getArea(), splitresult[1].getArea(), "splitresult.areas");
        //assertFalse("splitresult.intersects", splitresult[0].covers(splitresult[1]));
        //assertFalse("splitresult.intersects", splitresult[0].coveredBy(splitresult[1]));
        assertFalse(splitresult[0].crosses(splitresult[1]), "splitresult.crosses");

        Polygon rectangleWithoutHole = SceneryMesh.buildRectangle3();
        assertEquals(4, JtsUtil.getPossibleSplitConnections(rectangleWithoutHole).size(), "rectangleWithoutHole.PossibleSplitConnections");

        splitresult = JtsUtil.splitPolygon(rectangleWithoutHole);
        assertEquals(2, splitresult.length, "splitresult.length");
        assertFalse(splitresult[0].crosses(splitresult[1]), "splitresult.crosses");
        assertEquals(splitresult[0].getArea(), splitresult[1].getArea(), "splitresult.areas");
    }

    /**
     * 22.8.18:poly2tri kann es auch nicht->Stackoverflow, trotz -Xss512m
     */
    @Test
    public void testMonsterPolygonSplit() {
        // triangulate scheitert bei dem (136 holes,932 points)
        Polygon monsterpolygonausmaingrid = null;
        try {
            monsterpolygonausmaingrid = (Polygon) JtsUtil.buildFromWKT(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("monsterpolygonfrommaingrid.txt")));

            //Polygon[] result = JtsUtil.splitPolygon(monsterpolygonausmaingrid);
            //assertEquals("result.length", 2, result.length);
            if (!monsterpolygonausmaingrid.isValid()) {
                fail("monster not valid");
            }
            //List<Polygon> triangles = JtsUtil.triangulatePolygonByDelaunay(monsterpolygonausmaingrid);
            //List<Polygon> triangles = Poly2TriTriangulationUtil.triangulate(monsterpolygonausmaingrid);
            //assertEquals("result.polygons", 8, triangles.size());
            //triangles = JtsUtil.triangulatePolygonByEarClippingRespectingHoles(monsterpolygonausmaingrid);
            //assertNotNull("triangles",triangles);
        } catch (Exception e) {
            e.printStackTrace();
            fail("file not found?");
        }
    }

    /**
     * Skizze 61
     * Eigener Triangulator ist witzlos
     *
     * @return
     */
    @Test
    public void testPolygonTriangulate() {
        Polygon rectangleWithHole = SceneryMesh.buildRectangleWithHole();
        List<Polygon> result = Poly2TriTriangulationUtil.triangulate(rectangleWithHole);
        assertEquals(8, result.size(), "result.polygons");

    }

    @Test
    public void testPolygonSplit() {
        Polygon p = PolygonCollection.getSplitFailPolygon();
        Polygon[] splitresult = JtsUtil.removeHoleFromPolygonBySplitting(p);
        assertEquals(2, splitresult.length, "splitresult.length");
        assertFalse(splitresult[0].crosses(splitresult[1]), "splitresult.crosses");

        //der hat 3 Holes
        p = PolygonCollection.getTriFailPolygon();
        assertEquals(3, p.getNumInteriorRing(), "p.holes");

        splitresult = JtsUtil.splitPolygon(p);
        assertEquals(2, splitresult.length, "splitresult.length");
        assertFalse(splitresult[0].crosses(splitresult[1]), "splitresult.crosses");
        assertEquals(3, splitresult[0].getNumInteriorRing() + splitresult[1].getNumInteriorRing(), "splitresult.holes");

    }


}
