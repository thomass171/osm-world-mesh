package de.yard.threed.osm2scenery.modules;

import de.yard.threed.osm2scenery.OSMToSceneryDataConverter;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.TagFilter;
import de.yard.threed.osm2scenery.util.TagMap;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapWay;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 11.07.18.
 */
public abstract class SceneryModule {
    public static List<? extends SceneryModule> getRelevant(List<? extends SceneryModule> worldModules, MapWay mapWay) {
        return new ArrayList<>(worldModules);
    }

    /**
     * Deprecated in favor of applyTo(MapWay mapWay, TerrainMesh terrainMesh)
     */
    @Deprecated
    public abstract SceneryObjectList applyTo(MapData mapData);

    /**
     * Default implementation. TerrainMesh is for knowing
     * the context but not for adding to it. That is done later.
     * TODO should be sufficint to pass sceneryContext.
     * 1.5.24: By 2024 design SceneryContext is still used as a container for higl level terrainmesh wrapper.
     *
     * Returns the objects to be created(or updated) in the order they should be processed.
     */
    public SceneryObjectList applyTo(MapWay mapWay, TerrainMesh terrainMesh, SceneryContext sceneryContext){
        return new SceneryObjectList();
    }

    protected String getSubConfig(String subproperty) {
        String classname = getClass().getSimpleName();
        String conf = Config.getCurrentConfiguration().getString("modules." + classname + "." + subproperty);
        return conf;
    }

    public TagFilter getTagFilter(String subproperty) {
        String filter = getSubConfig(subproperty);
        return new TagFilter(filter);
    }

    public TagMap getTagMap(String subproperty) {
        String raw = getSubConfig(subproperty);
        if (raw == null) {
            return null;
        }
        String[] elements = raw.split(",");
        TagMap tagMap = new TagMap();
        for (String s : elements) {
            String[] parts = s.split("->");
            tagMap.put(new TagFilter(parts[0]), parts[1]);
        }
        return tagMap;
    }

    /**
     * Module keonen das Default Background Material umdefinieren.
     * 21.5.19: Das ist nicht hilfreich
     * @return
     */
    /*public Material getBackgroundMaterial(){
        return null;
    }*/

    /**
     * Eine halbwegs schlüssige Klassifizierung (z.B. Garage) der Objekte geht erst, wenn der Kontext bekannt ist.
     *
     * @param mapData
     */
    public void classify(MapData mapData){
        //default, nothing to do
    }


    public void extendMapData(String osmDatasetName, MapData mapData, OSMToSceneryDataConverter converter) {
        //default, nothing to do
    }

    /**
     * z.B. Garagenzufahrten, Taxiwayarea, BridgeGap, Ramps.
     */
    public List<ScenerySupplementAreaObject> createSupplements(List<SceneryObject> objects) {
        return null;
    }
}
