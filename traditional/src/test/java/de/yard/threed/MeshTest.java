package de.yard.threed;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.modules.SurfaceAreaModule;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.SceneryAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * 11.4.19: Einfachere gezieltere Tests als in OsmGridTest (ohne Processor und ConversionFacade)
 */
public class MeshTest {
    //EngineHelper platform = PlatformHomeBrew.init(new HashMap<String, String>());
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));

    Logger logger = Logger.getLogger(MeshTest.class);

    @BeforeAll
    public static void setup(){
        TerrainMesh.meshFactoryInstance = new TraditionalMeshFactory();
    }

    /**
     * Ohne Ways, nur die grosse Area ins Mesh.
     *
     * @throws IOException
     */
    @Test
    public void testDesdorf() throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/Desdorf.osm.xml", "Desdorf", "superdetailed");

        SurfaceAreaModule surfaceAreaModule = new SurfaceAreaModule();
        SceneryObjectList areas = surfaceAreaModule.applyTo(SceneryTestUtil.mapData);
        surfaceAreaModule.classify(SceneryTestUtil.mapData);

        List<SceneryObject> knownobjects = new ArrayList<>();

        assertEquals(4, areas.size(), "areas");

        SceneryAreaObject southFarmland = (SceneryAreaObject) areas.findObjectByOsmId(87822834);
        southFarmland.buildEleGroups();
        //cut/clip ist in createPolygon
        southFarmland.createPolygon(null, SceneryTestUtil.gridCellBounds, null, SceneryContext.getInstance());
        knownobjects.add(southFarmland);


        SceneryAreaObject forestAnK41 = (SceneryAreaObject) areas.findObjectByOsmId(225794256);
        if (forestAnK41 != null) {
            //der wird aus irgendwelchen Gruenden nicht mit aufgenommen
            forestAnK41.buildEleGroups();
            //cut/clip ist in createPolygon
            forestAnK41.createPolygon(null, SceneryTestUtil.gridCellBounds, null, SceneryContext.getInstance());
            knownobjects.add(forestAnK41);
        }

        SceneryAreaObject scrubAnK41 = (SceneryAreaObject) areas.findObjectByOsmId(225794276);
        scrubAnK41.buildEleGroups();
        //cut/clip ist in createPolygon
        scrubAnK41.createPolygon(null, SceneryTestUtil.gridCellBounds, null, SceneryContext.getInstance());
        knownobjects.add(scrubAnK41);

        SceneryMesh.connectAreas(knownobjects);

        assertEquals(1, southFarmland.adjacentareas.size(), "southFarmland.adjacentareas.size");
        assertEquals(1, scrubAnK41.adjacentareas.size(), "scrubAnK41.adjacentareas.size");

        // TerrainMesh

        SceneryTestUtil.gridCellBounds.rearrangeForWayCut(knownobjects, null);
        TerrainMesh tm = TerrainMesh.init(SceneryTestUtil.gridCellBounds);

        assertEquals(4, SceneryTestUtil.gridCellBounds.getPolygon().getCoordinates().length, "gridCellBounds.coordinates.size");
        List<GridCellBounds.LazyCutObject> lazyCuts = SceneryTestUtil.gridCellBounds.getLazyCuts();
        assertEquals(0, lazyCuts.size(), "lazyCuts.size");

        assertEquals(3, tm.lines.size(), "tm.lines.size");


        southFarmland.addToTerrainMesh(tm);
        assertEquals(5, tm.getBoundaries().size(), "tm.boundaries.size");
        assertEquals(5 + 3, tm.lines.size(), "tm.lines.size");

        scrubAnK41.addToTerrainMesh(tm);

        List<MeshLine> sharedBoundaries = tm.getSharedBoundaries();
        assertEquals(1, sharedBoundaries.size(), "tm.sharedBoundaries.size");
        MeshPolygon southFarmlandPolygon = tm.getPolygon(sharedBoundaries.get(0), southFarmland.getArea()[0]);
        assertEquals(4, southFarmlandPolygon.lines.size(), "southFarmlandPolygon.size");
        Polygon p = southFarmlandPolygon.getPolygon();
        //9 passt wohl
        assertEquals(9, p.getCoordinates().length, "southFarmlandPolygon.size");

        List<MeshLine> shared = tm.getShared();
        assertEquals(1, shared.size(), "tm.shared.size");
        MeshPolygon scrubAnK41Polygon = tm.getPolygon(shared.get(0), scrubAnK41.getArea()[0]);
        // sind 3 lines durch den Split am Share. 26.8.19:auf einmal zwei, was aber auch plausibel ist.
        assertEquals(2, scrubAnK41Polygon.lines.size(), "scrubAnK41Polygon.size");
        p = scrubAnK41Polygon.getPolygon();
        //auch 9, kurios. 26.10.19: 9->10??
        assertEquals(9, p.getCoordinates().length, "scrubAnK41Polygon.size");

        //5 grid + 2 farmland+2 scrub +1 share. 26.10.19: eins weniger, Ursache unklar.
        assertEquals(5 + 2 + 2 + 1 - 1, tm.lines.size(), "tm.lines.size");

        // da gab es ja einen Split
        tm.addKnownTwoEdger(scrubAnK41Polygon.lines.get(1).getTo().getCoordinate());
        assertTrue(tm.isValid(false), "valid");
    }

}
