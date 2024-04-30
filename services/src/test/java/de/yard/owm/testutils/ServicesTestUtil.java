package de.yard.owm.testutils;

import de.yard.owm.services.persistence.PersistedMeshFactory;
import de.yard.owm.services.persistence.TerrainMeshManager;
import de.yard.owm.services.util.OsmXmlParser;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.OSMData;

import java.io.IOException;

import static de.yard.owm.testutils.TestUtils.loadFileFromClasspath;

/**
 * Derived from SceneryTestUtil
 */
public class ServicesTestUtil {

    public  MapData mapData;
    public  GridCellBounds gridCellBounds;
    public  TerrainMesh terrainMesh;
public SceneryContext sceneryContext;

    public  ServicesTestUtil(String osmfile/*, String gridname, String materialconfigsuffix, String lodconfigsuffix*/,
                                   TerrainMeshManager terrainMeshManager) throws Exception {
        String xml = loadFileFromClasspath(osmfile);
        OsmXmlParser parser = new OsmXmlParser(xml);
        OSMData osmData = parser.getData();

        gridCellBounds = GridCellBounds.buildFromOsmData(osmData);
        TerrainMesh.meshFactoryInstance = new PersistedMeshFactory(gridCellBounds.getProjection().getBaseProjection(), terrainMeshManager);

        OSMToSceneryDataConverter converter = new OSMToSceneryDataConverter(gridCellBounds.getProjection(), gridCellBounds);
        mapData = converter.createMapData(osmData);

        SceneryBuilder.FTR_SMARTGRID = true;
        SceneryBuilder.FTR_SMARTBG = true;

        terrainMesh = TerrainMesh.init(gridCellBounds);

        TestUtils.addTerrainMeshBoundary(terrainMesh, gridCellBounds.getOrigin().getLatDeg().getDegree(), gridCellBounds.getOrigin().getLonDeg().getDegree(),
                gridCellBounds.degwidth, gridCellBounds.degheight, gridCellBounds.getProjection().getBaseProjection(), 0.0001);

        sceneryContext = new SceneryContext();
    }
}
