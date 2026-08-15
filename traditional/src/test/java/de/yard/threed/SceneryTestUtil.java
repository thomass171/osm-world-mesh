package de.yard.threed;

import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.ElevationArea;
import de.yard.threed.osm2scenery.elevation.ElevationMap;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.Materials;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMDataReader;
import de.yard.threed.osm2world.OSMFileReader;

import java.io.File;
import java.io.IOException;

import static de.yard.threed.osm2graph.SceneryBuilder.loadConfig;
import static de.yard.threed.osm2graph.SceneryBuilder.loadMaterialConfig;
import static de.yard.threed.osm2graph.osm.OsmUtil.toVector2;

public class SceneryTestUtil {

    public static GridCellBounds gridCellBounds;
    public static OSMData osmData;
    public static MapData mapData;
    public static OSMToSceneryDataConverter converter;

    /**
     * Alles fuer einen Test ohne Processor vorbereiten.
     */
    public static void prepareTest(String osmfile, String gridname, String materialconfigsuffix, String lodconfigsuffix) throws IOException, MeshInconsistencyException {

        SceneryContext.clear();
        ElevationArea.history.clear();
        ElevationMap.drop();
        EleConnectorGroup.clear();

        Config.reinit(Processor.defaultconfigfile, loadMaterialConfig(materialconfigsuffix), loadConfig(lodconfigsuffix), null);
        Materials.configureMaterials(Config.getCurrentConfiguration());

        OSMDataReader dataReader = new OSMFileReader(new File(osmfile));
        osmData = dataReader.getData();
        osmData.source = osmfile;
        gridCellBounds = GridCellBounds.buildGrid(gridname, null);
        //mapProjection =  new MetricTextureProjection();
        //mapProjection.setOrigin(osmData);

        //9.4.19: Ohne Grid ist doch völlig witzlos
        //if (gridCellBounds != null) {
        //gridCellBounds.setOrigin(osmData);
        gridCellBounds.init(gridCellBounds.getProjection());

        EleConnectorGroup.init((GridCellBounds) gridCellBounds, gridCellBounds.getProjection());

        // Das folgende muesste man zentral unterbringen.
        converter = new OSMToSceneryDataConverter(gridCellBounds.getProjection(), gridCellBounds);
        mapData = converter.createMapData(osmData);
        SceneryContext.init(mapData);

        //zu frueh wegen rearrangeTerrainMesh.init(gridCellBounds);
    }

    public static void prepareTest(String osmfile, String gridname, String lodconfigsuffix) throws IOException, MeshInconsistencyException {
        prepareTest(osmfile, gridname, "flight", lodconfigsuffix);
    }








}
