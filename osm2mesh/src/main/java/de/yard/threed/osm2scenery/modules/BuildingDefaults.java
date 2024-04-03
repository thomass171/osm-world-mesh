package de.yard.threed.osm2scenery.modules;

import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.*;
import org.apache.log4j.Logger;


/**
 * Fuer eine halbwegs schlüssige Klassifizierung (z.B. Garage) ist es hier zu früh. Es sei denn, die
 * Info kommt aus OSM.
 *
 * Damit werden sich auch diese Defaultwerte später nochmal aendern können.
 */
public class BuildingDefaults {
    public BuildingModule.BuildingClassification classification=null;
    Logger logger = Logger.getLogger(BuildingDefaults.class);
    public int defaultLevels;
    public String defaultRoofShape = "flat";
    Material defaultMaterialWall = Materials.BUILDING_DEFAULT;
    Material defaultMaterialRoof = Materials.ROOF_DEFAULT;
    Material defaultMaterialWindows = Materials.BUILDING_WINDOWS;

    /**
     * Die Defaultermittlung geht in zwei Schritten:
     * 1)aus OSM
     * 2)Annahmen.
     * Ueberschneidet sich mit setAttributes(). TODO Abgrenzung
     *
     * @param area
     */
    public BuildingDefaults(MapArea area) {
        defaultLevels = Config.getCurrentConfiguration().getInt("buildingdefaultLevels", 1);
        String buildingValue = area.getTags().getValue("building");

        // erstmal sehen, was die expliziten Tags hergeben.
        // TODO Statt knowntypeOfBuilding classification setzen
        boolean knowntypeOfBuilding = false;
        if ("greenhouse".equals(buildingValue)) {
            defaultLevels = 1;
            defaultMaterialWall = Materials.GLASS;
            defaultMaterialRoof = Materials.GLASS_ROOF;
            defaultMaterialWindows = null;
            knowntypeOfBuilding = true;
        } else if ("garage".equals(buildingValue)
                || "garages".equals(buildingValue)) {
            defaultLevels = 1;
            defaultMaterialWall = Materials.CONCRETE;
            defaultMaterialRoof = Materials.CONCRETE;
            defaultMaterialWindows = Materials.GARAGE_DOORS;
            classification = BuildingModule.BuildingClassification.GARAGE;
            knowntypeOfBuilding = true;
        } else if ("hut".equals(buildingValue)
                || "shed".equals(buildingValue)) {
            defaultLevels = 1;
            knowntypeOfBuilding = true;
        } else if ("cabin".equals(buildingValue)) {
            defaultLevels = 1;
            defaultMaterialWall = Materials.WOOD_WALL;
            defaultMaterialRoof = Materials.WOOD;
            knowntypeOfBuilding = true;
        } else if ("roof".equals(buildingValue)) {
            defaultLevels = 1;
            defaultMaterialWindows = null;
            knowntypeOfBuilding = true;
        } else if ("church".equals(buildingValue)
                || "hangar".equals(buildingValue)
                || "industrial".equals(buildingValue)) {
            defaultMaterialWindows = null;
            knowntypeOfBuilding = true;
        } else {
            if (area.getTags().getValue("building:levels") == null) {
                defaultMaterialWindows = null;
            }
        }


        //if (area.getBoundaryNodes().size() != 5) {
        //logger.debug("switching defaultRoofShape to flat due to non simple area");
        //defaultRoofShape = "flat";
        //      defaultMaterialRoof = Materials.ROOF_DEFAULT;
        //} else


    }
}
