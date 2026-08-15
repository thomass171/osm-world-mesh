package de.yard.threed.osm2graph.osm;


import de.yard.threed.core.Util;
import de.yard.threed.core.loader.PortableModel;
import de.yard.threed.osm2graph.RenderData;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryConversionFacade;
import de.yard.threed.osm2scenery.elevation.ElevationArea;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMDataReader;
import de.yard.threed.osm2world.OSMFileReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;


import java.io.File;
import java.io.IOException;


/**
 * Nur mal so als Container.
 * <p>
 * OsmData wird auch durch "getConversionFacade" veraendert (neue Nodes durch Coastline und EmptyTerrain).
 * Darum osmdata erst im process einlesen.
 * <p>
 * <p>
 * Created on 02.06.18.
 */
@Slf4j
public class Processor {
    public File dataSet;
    public OSMData osmData;
    public MapData mapData;
    //OSM2World osm2World;
    //public ConversionFacade cf;
    public SceneryConversionFacade scf;
    //9.4.19: immer false
    public boolean useosm2world = false;
    //public ConversionFacade.Results results;
    public SceneryConversionFacade.Results sresults;
    public GridCellBounds gridCellBoundsused;
    public PortableModel pml;
    // nur fuer Testzwecke!
    public PortableModelTarget pmt;
    public static String defaultconfigfile = ("config/configuration-base.properties");

    public Processor(File dataSet) throws IOException {
        this(dataSet, null, null);
    }

    public Processor(File dataSet, String configfilesuffix, Configuration customconfig) throws IOException {
        // Config immer wieder einheitlich vorbereiten.
        // 18.7.19: Das macht der Aufrufer wegen zusaetzlicher Configs doch eh, oder sollte. Darum hier nicht mehr
        //reinitConfig(configfilesuffix, customconfig);
        this.dataSet = dataSet;

    }

    /**
     * Extrahiert, um danach noch (vor dem process, was ändern zu können.
     */
    public void reinitConfig(String materialconfigsuffix,String lodconfigfilesuffix, Configuration customconfig) {
        Util.nomore();
       //24.3.26 after removing dependencies, shouldn't be here any longer anyway Config.reinit(defaultconfigfile, loadMaterialConfig(materialconfigsuffix), loadConfig(lodconfigfilesuffix), customconfig);

    }

    public void process(GridCellBounds gridCellBounds) throws IOException {
        SceneryContext.clear();
        ElevationArea.history.clear();
        gridCellBoundsused = gridCellBounds;

        long starttime = System.currentTimeMillis();
        OSMDataReader dataReader = new OSMFileReader(dataSet);
        osmData = dataReader.getData();
        osmData.source =dataSet.getName();
        log.info("Loading OSM data from "+dataSet+" took "+((System.currentTimeMillis()-starttime)/1000)+" seconds.");

        //mapProjection =  new MetricTextureProjection();
        //mapProjection.setOrigin(osmData);

        //9.4.19: Ohne Grid ist doch völlig witzlos
        //if (gridCellBounds != null) {
        //gridCellBounds.setOrigin(osmData);
        try {
            gridCellBounds.init(gridCellBounds.getProjection());
        } catch (MeshInconsistencyException e) {
            throw new RuntimeException(e);
        }
        //}

        scf = new SceneryConversionFacade(osmData);

        //9.4.19 Config.getInstance().setTargetBounds(gridCellBounds);

        try {
            sresults = scf.createRepresentations(gridCellBounds, gridCellBounds.getProjection());
        } catch (MeshInconsistencyException e) {
            throw new RuntimeException(e);
        }
        mapData = scf.getMapData();

        //4.8.18 vor elevationcalc sresults.sceneryMesh.texturize();

    }

    public RenderData getResults() {
        RenderData renderData = new RenderData();
        //renderData.osm2worldresults = results;
        renderData.sceneryresults = sresults;
        return renderData;
    }

    public MapData getMapData() {
        return mapData;
    }

    public SceneryProjection getProjection() {
        return gridCellBoundsused.getProjection();
    }

    public TerrainMesh getTerrainMesh() {
        return sresults.sceneryMesh.terrainMesh;
    }

    public File getFileUsed(){
        return dataSet;
    }
}
