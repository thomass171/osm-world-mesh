package de.yard.threed;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.loader.PortableModelDefinition;
import de.yard.threed.core.platform.PlatformInternals;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.PortableModelTarget;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.modules.BuildingModule;
import de.yard.threed.osm2scenery.scenery.SceneryAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.WorldElement;
import de.yard.threed.osm2scenery.scenery.components.BuildingComponent;
import de.yard.threed.osm2scenery.util.Dumper;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.Materials;
import de.yard.threed.osm2world.Primitive;
import de.yard.threed.osm2world.PrimitiveBuffer;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


/**
 * 11.4.19: Einfachere gezieltere Tests als in OsmGridTest (ohne Processor und ConversionFacade)
 */
public class BuildingModuleTest {
    //EngineHelper platform = PlatformHomeBrew.init(new HashMap<String, String>());
    PlatformInternals platform = SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    Logger logger = Logger.getLogger(BuildingModuleTest.class);

    /**
     * @throws IOException
     */
    @Test
    public void testDesdorf() throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/Desdorf.osm.xml", "Desdorf", "superdetailed");


        BuildingModule buildingModule = new BuildingModule();
        SceneryObjectList buildings = buildingModule.applyTo(SceneryTestUtil.mapData);
        buildingModule.classify(SceneryTestUtil.mapData);
        // List<RoadModule.Road> roads = roadModule.getRoads();
        //23 corresponds to OSM data
        assertEquals(23, buildings.size(), "buildings");
        //322751226 ist ein schlichter closed way mit vier Ecken
        SceneryAreaObject buildingso = (SceneryAreaObject) buildings.findObjectByOsmId(322751226);
        assertNotNull(buildingso);

        // Test für einfaches komplett im Grid liegendes Building ist jetzt in TestDataTest

        //Das Doppelhaus in Desdorf: Vier OSM Ways: Zwei Garagen?(477263082,477263081), Hausnummer 3(391605368) und 5(391605370)
        //Garage candidate
        BuildingModule.Building garagecandidate = buildings.findBuildingByOsmId(477263082);
        assertTrue(garagecandidate.isGarage(), "garage");

        // Reihenhaus? in Desdorf 6(391605350),8(391605332),10(391605340),12
        buildingso = (SceneryAreaObject) buildings.findObjectByOsmId(391605332);
        BuildingComponent reihenhauscandidate = (BuildingComponent) buildingso.volumeProvider;
        BuildingModule.Building building = reihenhauscandidate.building;
        BuildingModule.BuildingCluster cluster = building.getBuildingCluster();
        assertNotNull(cluster, "cluster");
        assertEquals(4, cluster.size(), "cluster.size");
    }

    /**
     * EDDK hat einfach komplexere Buildings
     *
     * @throws IOException
     */
    @Test
    public void testEDDK() throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/EDDK-Small.osm.xml", "EDDK-Small", "superdetailed");

        BuildingModule buildingModule = new BuildingModule();
        SceneryObjectList buildings = buildingModule.applyTo(SceneryTestUtil.mapData);
        buildingModule.classify(SceneryTestUtil.mapData);

        //205069367 Gepäcksortieranlage<tag k="building" v="yes"/>
        //  <tag k="building:levels" v="1"/>
        //  <tag k="name" v="Gepäcksortieranlage"/>
        //  <tag k="roof:colour" v="#ccd4cc"/>
        //  <tag k="roof:shape" v="gabled"/>
        SceneryFlatObject baggagehallso = (SceneryFlatObject) buildings.findObjectByOsmId(205069367);
        BuildingComponent baggagehall = (BuildingComponent) baggagehallso.volumeProvider;
        List<BuildingModule.BuildingPart> buildingparts = baggagehall.getBuilding().getParts();
        assertEquals(1, buildingparts.size(), "buildingparts.size");
        BuildingModule.BuildingPart.Roof roof = buildingparts.get(0).getRoof();
        assertEquals(1.0, roof.getRoofHeight(), "roof.height");

        // auch 218058765 als part
        SceneryFlatObject starCso = (SceneryFlatObject) buildings.findObjectByOsmId(218058763);
        BuildingComponent starC = (BuildingComponent) starCso.volumeProvider;
        buildingparts = starC.getBuilding().getParts();
        //Puuh, 10 Parts?? ohne buildingsparts nur  1
        assertEquals(10, buildingparts.size(), "starC.buildingparts.size");
        //Index 9 durch probieren. alles ungar.
        Material material = buildingparts.get(9).getMaterial();
        assertEquals("OSM218058763", material.getName(), "starC.material.name");
        starC.triangulateAndTexturize(null);
        PrimitiveBuffer primitiveBuffer = starC.primitiveBuffer;
        //Warum 4
        assertEquals(4, primitiveBuffer.getMaterials().size());

    }

    /**
     * Das Building 109 hat die Masse 5x10 (49.89qm).
     *
     * @throws IOException
     */
    @Test
    public void testTestData() throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/TestData.osm.xml", "TestData", "superdetailed");

        BuildingModule buildingModule = new BuildingModule();
        SceneryObjectList buildings = buildingModule.applyTo(SceneryTestUtil.mapData);
        buildingModule.classify(SceneryTestUtil.mapData);

        assertEquals(1, buildings.size(), "buildings");

        // ein Standardbuilding mit vier Ecken
        SceneryAreaObject buildingso = (SceneryAreaObject) buildings.findObjectByOsmId(109);
        BuildingComponent buildingco = (BuildingComponent) buildingso.volumeProvider;
        BuildingModule.Building building = buildingco.building;
        assertFalse(building.isGarage(), "garage");

        buildingco.triangulateAndTexturize(null);
        PrimitiveBuffer primitiveBuffer = buildingco.primitiveBuffer;
        //Wall und Roof.
        assertEquals(2, primitiveBuffer.getMaterials().size(), "materials");
        Set<Material> materials = primitiveBuffer.getMaterials();
        Material material;
        Collection<Primitive> walls = primitiveBuffer.getPrimitives(Materials.WALL_BRICK_RED/*BUILDING_DEFAULT*/);
        //3.5.19 1->2 wegen Giebel? Ja. Die Walls haben 14 Vertices, die Giebel 12 (4 Triangles)
        assertEquals(2, walls.size(), "walls");
        WorldElement wallelement = buildingco.getWorldElementByMaterialName(Materials.WALL_BRICK_RED/*BUILDING_DEFAULT*/.getName());
        //3.5.19 10->26 wegen Giebel?
        assertEquals(26, wallelement.vertexData.vertices.size(), "wallelement.vertices");

        //2.5.19: Wegen flat shading keine Normale mehr in VertexData
        //assertEquals("wallelement.normals", 10, wallelement.vertexData.normals.size());
        PortableModelTarget pmt = new PortableModelTarget();
        pmt.drawElement("Wall", wallelement, null);
        pmt.endRendering();
        PortableModelDefinition pmlwall = pmt.pml.getObject(0);
        //Vector3Array pmlnormals = pmlwall.geolist.get(0).getNormals();
        //An welchen Kanten/Verticers die Normalen die Richtung wechseln, ist noch nicht ganz klar.
        /*Vector3 southdirection = new Vector3(0, -1, 0);
        Vector3 normal = wallelement.vertexData.normals.get(0);
        TestUtil.assertVector3("normal0", southdirection, normal);
        //TestUtil.assertVector3("pmlnormal0", southdirection, pmlnormals.getElement(0));

        Vector3 eastdirection = new Vector3(1, 0, 0);
        normal = wallelement.vertexData.normals.get(4);
        TestUtil.assertVector3("normal4", eastdirection, normal);
        //TestUtil.assertVector3("pmlnormal4", eastdirection, pmlnormals.getElement(4));

        Vector3 westdirection = new Vector3(-1, 0, 0);
        normal = wallelement.vertexData.normals.get(9);
        TestUtil.assertVector3("normal10", westdirection, normal);

        Vector3 northdirection = new Vector3(0, 1, 0);
        normal = wallelement.vertexData.normals.get(7);
        TestUtil.assertVector3("normal10", northdirection, normal);
*/

        Collection<Primitive> roofs = primitiveBuffer.getPrimitives(Materials.PANTILE_ROOF_DARK/*ROOF_DEFAULT*/);
        assertEquals(1, roofs.size(), "roofs");
        WorldElement roofelement = buildingco.getWorldElementByMaterialName(Materials.PANTILE_ROOF_DARK.getName());
        Dumper.dumpVertexData(logger, roofelement.vertexData);
        //3.5.19 6->12 wegen Giebel? Warum nicht 8? Roof ist eine Triangle...(SLOPED_TRIANGLES). Ziemlich durcheinander.
        assertEquals(12, roofelement.vertexData.vertices.size(), "roofelement.vertices");
        for (int i = 0; i < roofelement.vertexData.indices.length; i++) {
            assertEquals(i, roofelement.vertexData.indices[i], "roofelement.indices[]");
        }
        //Traufe braucht house height
        assertEquals(2.5, roofelement.vertexData.vertices.get(0).getOrdinate(Coordinate.Z), 0.1, "roofelement.height");
        //Giebel 5 wegen 45 Grad
        assertEquals(5, roofelement.vertexData.vertices.get(2).getOrdinate(Coordinate.Z), 0.1, "roofelement.height");

    }

    @Test
    public void testZieverichSued() throws IOException {
        SceneryTestUtil.prepareTest(SceneryBuilder.osmdatadir + "/Zieverich-Sued.osm.xml", "Zieverich-Sued", "superdetailed");


        BuildingModule buildingModule = new BuildingModule();
        SceneryObjectList buildings = buildingModule.applyTo(SceneryTestUtil.mapData);
        buildingModule.classify(SceneryTestUtil.mapData);

        //Ein EFH (336523597) mit Garage (336523788):
        //Explizit Garage
        BuildingModule.Building garage = buildings.findBuildingByOsmId(336523788);
        assertTrue(garage.isGarage(), "garage");

        BuildingModule.Building efh = buildings.findBuildingByOsmId(336523597);
        assertFalse(efh.isGarage(), "efh.garage");
        assertEquals(1, efh.getParts().size(), "parts");
        BuildingModule.BuildingPart.Roof roof = efh.getParts().get(0).getRoof();
        assertEquals("GabledRoof", roof.getClass().getSimpleName(), "part[0].roof");
        //45 Grad sollen es etwa sein
        assertEquals(5.1, roof.getRoofHeight(), 0.1, "part[0].roof.height");
    }

}
