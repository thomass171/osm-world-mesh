package de.yard.threed.osm2world;

import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * this class defines materials that can be used by all { WorldModule}s
 */
public final class Materials {
    static Logger logger = Logger.getLogger(Materials.class);

    /**
     * prevents instantiation
     */
    private Materials() {
    }

    /**
     * material for "empty" ground
     */
    public static final ConfMaterial TERRAIN_DEFAULT =
            new ConfMaterial("TERRAIN_DEFAULT", Material.Interpolation.SMOOTH, Color.GREEN);

    public static final ConfMaterial WATER =
            new ConfMaterial("WATER", Material.Interpolation.FLAT, Color.BLUE);
    public static final ConfMaterial PURIFIED_WATER =
            new ConfMaterial(Material.Interpolation.FLAT, Color.BLUE);

    public static final ConfMaterial ASPHALT =
            new ConfMaterial("ASPHALT", Material.Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
    public static final ConfMaterial BRICK =
            new ConfMaterial("BRICK", Material.Interpolation.FLAT, new Color(1.0f, 0.5f, 0.25f));
    public static final ConfMaterial COBBLESTONE =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
    public static final ConfMaterial CONCRETE =
            new ConfMaterial("CONCRETE", Material.Interpolation.FLAT, new Color(0.55f, 0.55f, 0.55f));
    public static final ConfMaterial EARTH =
            new ConfMaterial("EARTH", Material.Interpolation.FLAT, new Color(0.3f, 0, 0));
    public static final ConfMaterial GLASS =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
    public static final ConfMaterial GRASS =
            new ConfMaterial("GRASS", Material.Interpolation.FLAT, new Color(0.0f, 0.8f, 0.0f));
    public static final ConfMaterial GRASS_PAVER =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.5f, 0.3f));
    public static final ConfMaterial SCRUB =
            new ConfMaterial("SCRUB", Material.Interpolation.FLAT, new Color(0.0f, 0.8f, 0.0f));
    public static final ConfMaterial SETT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
    public static final ConfMaterial GRAVEL =
            new ConfMaterial("GRAVEL", Material.Interpolation.FLAT, new Color(0.4f, 0.4f, 0.4f));
    public static final ConfMaterial PAVING_STONE =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.4f, 0.4f, 0.4f));
    public static final ConfMaterial PEBBLESTONE =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.4f, 0.4f, 0.4f));
    public static final ConfMaterial PLASTIC =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0, 0, 0));
    public static final ConfMaterial PLASTIC_GREY =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(184, 184, 184));
    public static final ConfMaterial SAND =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(241, 233, 80));
    public static final ConfMaterial STEEL =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(200, 200, 200));
    public static final ConfMaterial UNHEWN_COBBLESTONE =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
    public static final ConfMaterial WOOD =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
    public static final ConfMaterial WOOD_WALL =
            new ConfMaterial("WOOD_WALL", Material.Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
    public static final ConfMaterial TARTAN =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(206, 109, 90));
    public static final ConfMaterial FARMLAND =
            new ConfMaterial("FARMLAND", Material.Interpolation.FLAT, new Color(0.0f, 0.6f, 0.0f));


    public static final ConfMaterial ROAD_MARKING =
            new ConfMaterial("ROAD_MARKING", Material.Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
    public static final ConfMaterial ROAD_MARKING_DASHED =
            new ConfMaterial("ROAD_MARKING_DASHED", Material.Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
    public static final ConfMaterial ROAD_MARKING_ZEBRA =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
    public static final ConfMaterial ROAD_MARKING_CROSSING =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
    public static final ConfMaterial ROAD_MARKING_ARROW_THROUGH =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
    public static final ConfMaterial ROAD_MARKING_ARROW_THROUGH_RIGHT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
    public static final ConfMaterial ROAD_MARKING_ARROW_RIGHT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
    public static final ConfMaterial ROAD_MARKING_ARROW_RIGHT_LEFT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
    public static final ConfMaterial RED_ROAD_MARKING =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.6f, 0.3f, 0.3f));
    public static final ConfMaterial KERB =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.4f, 0.4f, 0.4f));
    public static final ConfMaterial STEPS_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, Color.DARK_GRAY);
    public static final ConfMaterial HANDRAIL_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, Color.LIGHT_GRAY);

    public static final ConfMaterial RAIL_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, Color.LIGHT_GRAY);
    public static final ConfMaterial RAIL_SLEEPER_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
    public static final ConfMaterial RAIL_BALLAST_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, Color.DARK_GRAY);

    //12.4.19: Warum es einfach "RAIL" wohl nicht gab?
    public static final ConfMaterial RAIL =
            new ConfMaterial("RAIL", Material.Interpolation.FLAT, Color.LIGHT_GRAY);
    // 16.4.19:Einfaches Road Element (idealerweise mit MArkierungen)
    public static final ConfMaterial ROAD =
            new ConfMaterial("ROAD", Material.Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));

    public static final ConfMaterial BUILDING_DEFAULT =
            new ConfMaterial("BUILDING_DEFAULT", Material.Interpolation.FLAT, new Color(1f, 0.9f, 0.55f));
    public static final ConfMaterial BUILDING_WINDOWS =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(1f, 0.9f, 0.55f));
    public static final ConfMaterial ROOF_DEFAULT =
            new ConfMaterial("ROOF_DEFAULT", Material.Interpolation.FLAT, new Color(0.8f, 0, 0));
    public static final ConfMaterial GLASS_ROOF =
            new ConfMaterial("GLASS_ROOF", Material.Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
    public static final ConfMaterial PANTILE_ROOF_DARK =
            new ConfMaterial("PANTILE_ROOF_DARK", Material.Interpolation.FLAT, new Color(0.2f, 0.2f, 0.2f));

    public static final ConfMaterial ENTRANCE_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.2f, 0, 0));
    public static final ConfMaterial GARAGE_DOORS =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(1f, 0.9f, 0.55f));

    public static final ConfMaterial WALL_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, Color.GRAY);
    public static final ConfMaterial WALL_GABION =
            new ConfMaterial(Material.Interpolation.FLAT, Color.GRAY);
    public static final ConfMaterial WALL_BRICK_RED =
            new ConfMaterial("WALL_BRICK_RED", Material.Interpolation.FLAT, Color.red);

    public static final ConfMaterial HEDGE =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0, 0.5f, 0));

    public static final ConfMaterial FENCE_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
    public static final ConfMaterial SPLIT_RAIL_FENCE =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
    public static final ConfMaterial CHAIN_LINK_FENCE =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(188, 198, 204));
    public static final ConfMaterial METAL_FENCE =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(188, 198, 204));
    public static final ConfMaterial METAL_FENCE_POST =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(188, 198, 204));

    public static final ConfMaterial BRIDGE_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, Color.GRAY);
    public static final ConfMaterial BRIDGE_PILLAR_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, Color.GRAY);

    public static final ConfMaterial TUNNEL_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, Color.GRAY, 0.2f, 0.5f,
                    Material.Transparency.FALSE, Collections.<TextureData>emptyList());

    public static final ConfMaterial TREE_TRUNK =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
    public static final ConfMaterial TREE_CROWN =
            new ConfMaterial(Material.Interpolation.SMOOTH, new Color(0, 0.5f, 0));
    public static final ConfMaterial TREE_BILLBOARD_BROAD_LEAVED =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0, 0.5f, 0), 1f, 0f,
                    Material.Transparency.FALSE, Collections.<TextureData>emptyList());
    public static final ConfMaterial TREE_BILLBOARD_BROAD_LEAVED_FRUIT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0, 0.5f, 0), 1f, 0f,
                    Material.Transparency.FALSE, Collections.<TextureData>emptyList());
    public static final ConfMaterial TREE_BILLBOARD_CONIFEROUS =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0, 0.5f, 0), 1f, 0f,
                    Material.Transparency.FALSE, Collections.<TextureData>emptyList());

    public static final ConfMaterial POWER_TOWER_VERTICAL =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(.7f, .7f, .7f), 1f, 0f,
                    Material.Transparency.BINARY, Collections.<TextureData>emptyList());
    public static final ConfMaterial POWER_TOWER_HORIZONTAL =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(.7f, .7f, .7f), 1f, 0f,
                    Material.Transparency.BINARY, Collections.<TextureData>emptyList());

    public static final ConfMaterial ADVERTISING_POSTER =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(1, 1, 0.8f));

    public static final ConfMaterial BUS_STOP_SIGN =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.98f, 0.90f, 0.05f));

    public static final ConfMaterial SIGN_DE_250 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.RED);

    public static final ConfMaterial SIGN_DE_206 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.RED);

    public static final ConfMaterial SIGN_DE_625_11 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);

    public static final ConfMaterial SIGN_DE_625_21 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);

    public static final ConfMaterial SIGN_DE_101 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_10 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_11 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_12 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_13 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_14 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_15 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_20 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_21 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_22 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_23 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_24 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_25 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_51 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_52 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_53 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_54 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_101_55 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_102 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_103_10 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_103_20 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_105_10 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_105_20 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_108_10 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_110_12 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_112 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_114 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_117_10 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_117_20 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_120 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_121_10 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_121_20 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_123 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_124 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_125 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_131 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_133_10 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_133_20 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_136_10 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_136_20 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_138_10 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_138_20 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_142_10 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_142_20 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_145 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_151 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);
    public static final ConfMaterial SIGN_DE_301 =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);

    public static final ConfMaterial GRITBIN_DEFAULT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.5f, 0.4f));

    public static final ConfMaterial POSTBOX_DEUTSCHEPOST =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(1f, 0.8f, 0f));
    public static final ConfMaterial POSTBOX_ROYALMAIL =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.8f, 0, 0));
    public static final ConfMaterial TELEKOM_MANGENTA =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.883f, 0f, 0.453f));

    public static final ConfMaterial FIREHYDRANT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.8f, 0, 0));

    public static final ConfMaterial FLAGCLOTH =
            new ConfMaterial(Material.Interpolation.SMOOTH, new Color(1f, 1f, 1f));

    public static final ConfMaterial SOLAR_PANEL =
            new ConfMaterial(Material.Interpolation.FLAT, Color.BLUE);

    public static final ConfMaterial PITCH_BEACHVOLLEYBALL =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(241, 233, 80));
    public static final ConfMaterial PITCH_SOCCER =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.0f, 0.8f, 0.0f));
    public static final ConfMaterial PITCH_TENNIS_ASPHALT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
    public static final ConfMaterial PITCH_TENNIS_CLAY =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.8f, 0.0f, 0.0f));
    public static final ConfMaterial PITCH_TENNIS_GRASS =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.0f, 0.8f, 0.0f));
    public static final ConfMaterial PITCH_TENNIS_SINGLES_ASPHALT =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
    public static final ConfMaterial PITCH_TENNIS_SINGLES_CLAY =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.8f, 0.0f, 0.0f));
    public static final ConfMaterial PITCH_TENNIS_SINGLES_GRASS =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0.0f, 0.8f, 0.0f));

    public static final ConfMaterial TENNIS_NET =
            new ConfMaterial(Material.Interpolation.FLAT, Color.WHITE);

    public static final ConfMaterial SKYBOX =
            new ConfMaterial(Material.Interpolation.FLAT, new Color(0, 0, 1),
                    1, 0, Material.Transparency.FALSE, null);

    public static final ConfMaterial TAXIWAY_YELLOW =
            //23.5.19: 0.8->0.9
            new ConfMaterial("TAXIWAY YELLOW", Material.Interpolation.FLAT, new Color(0.9f, 0.9f, 0f));


    private static Map<String, ConfMaterial> surfaceMaterialMap;
    private static Map<ConfMaterial, String> fieldNameMap;

    /**
     * 11.11.21: Now explicit reinit() instead of static.
     */
    static void reinit() {
        surfaceMaterialMap = new HashMap<String, ConfMaterial>();
        fieldNameMap = new HashMap<ConfMaterial, String>();

        surfaceMaterialMap.put("asphalt", ASPHALT);
        surfaceMaterialMap.put("cobblestone", COBBLESTONE);
        surfaceMaterialMap.put("compacted", GRAVEL);
        surfaceMaterialMap.put("concrete", CONCRETE);
        surfaceMaterialMap.put("grass", GRASS);
        surfaceMaterialMap.put("gravel", GRAVEL);
        surfaceMaterialMap.put("grass_paver", GRASS_PAVER);
        surfaceMaterialMap.put("ground", EARTH);
        surfaceMaterialMap.put("paved", ASPHALT);
        surfaceMaterialMap.put("paving_stones", PAVING_STONE);
        surfaceMaterialMap.put("pebblestone", PEBBLESTONE);
        surfaceMaterialMap.put("sand", SAND);
        surfaceMaterialMap.put("sett", SETT);
        surfaceMaterialMap.put("steel", STEEL);
        surfaceMaterialMap.put("tartan", TARTAN);
        surfaceMaterialMap.put("unpaved", EARTH);
        surfaceMaterialMap.put("unhewn_cobblestone", UNHEWN_COBBLESTONE);
        surfaceMaterialMap.put("wood", WOOD);
        surfaceMaterialMap.put("scrub", SCRUB);
        //16.8.18
        surfaceMaterialMap.put("farmland", FARMLAND);
        surfaceMaterialMap.put("water", WATER);
        //12.4.19
        surfaceMaterialMap.put("rail", RAIL_DEFAULT);
        surfaceMaterialMap.put("road", ROAD);

        try {
            for (Field field : Materials.class.getFields()) {
                if (field.getType().equals(ConfMaterial.class)) {
                    fieldNameMap.put(
                            (ConfMaterial) field.get(null),
                            field.getName());
                }
            }
        } catch (Exception e) {
            throw new Error(e);
        }

    }

    /**
     * returns all materials defined here
     */
    public static final Collection<ConfMaterial> getMaterials() {
        return fieldNameMap.keySet();
    }

    /**
     * returns a material defined here based on its field name
     */
    public static final ConfMaterial getMaterial(String fieldName) {
        for (Entry<ConfMaterial, String> entry : fieldNameMap.entrySet()) {
            if (entry.getValue().equals(fieldName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * returns a material for a surface value; null if none is found
     */
    public static final Material getSurfaceMaterial(String value) {
        return getSurfaceMaterial(value, null);
    }

    /**
     * same as {@link #getSurfaceMaterial(String)}, but with fallback value
     */
    public static final Material getSurfaceMaterial(String value,
                                                    Material fallback) {
        Material material = surfaceMaterialMap.get(value);
        if (material != null) {
            return material;
        } else {
            return fallback;
        }
    }

    /**
     * returns a human-readable, unique name for a material defined
     * within this class, null for all other materials.
     */
    public static final String getUniqueName(Material material) {
        return fieldNameMap.get(material);
    }

    public static final String CONF_KEY_REGEX =
            "material_(.+)_(color|specular|shininess|shadow|ssao|transparency|texture\\d*_(?:file|width|height|bumpmap))";

    /**
     * configures the attributes of the materials within this class
     * based on external configuration settings
     */
    public static final void configureMaterials(Configuration config) {

        // 11.11.21: Jetzt auch die statics resetten.
        reinit();

        // unchecked type parameter necessary due to Apache libs' old interface
        @SuppressWarnings("unchecked")
        Iterator<String> keyIterator = config.getKeys();

        while (keyIterator.hasNext()) {

            String key = keyIterator.next();

            Matcher matcher = Pattern.compile(CONF_KEY_REGEX).matcher(key);

            if (matcher.matches()) {

                String materialName = matcher.group(1);
                ConfMaterial material = getMaterial(materialName);

                if (material != null) {
                    String attribute = matcher.group(2);
                    //bringt nichts String value=config.getString(key);
                    // 11.11.21 Driss, dass die alle static sind. Da sind ja noch die alten Werte drin. darum halbherzig resetten
                    // TODO alle statics entfernen
                    material.color=Color.WHITE;
                    material.setTextureDataList(Collections.emptyList());
                    applyConfigEntryToMaterial(attribute, key, config, material, materialName);

                } else {
                    //18.7.19: Don't consider this to be an error. There might be config files with material definitions for materials, that are not known here, eg.
                    //'unknown material: RUNWAY. Used in key material_RUNWAY_texture0_file'
                    //logger.error("unknown material: " + materialName + ". Used in key " + key);
                }

            }

        }

    }

    /**
     * Extracted from above.
     */
    public static void applyConfigEntryToMaterial(String attribute, String key, Configuration config, ConfMaterial material, String materialName
    ) {

        if ("color".equals(attribute)) {
            Color color = ConfigUtil.parseColor(config.getString(key));
            if (color != null) {
                material.setColor(color);
            } else {
                logger.error("incorrect color value: " + config.getString(key));
            }
        } else if ("specular".equals(attribute)) {
            float specular = config.getFloat(key);
            material.setSpecularFactor(specular);
        } else if ("shininess".equals(attribute)) {
            int shininess = config.getInt(key);
            material.setShininess(shininess);
        } else if ("shadow".equals(attribute)) {
            String value = config.getString(key).toUpperCase();
            Material.Shadow shadow = Material.Shadow.valueOf(value);

            if (shadow != null) {
                material.setShadow(shadow);
            }
        } else if ("ssao".equals(attribute)) {
            String value = config.getString(key).toUpperCase();
            Material.AmbientOcclusion ao = Material.AmbientOcclusion.valueOf(value);

            if (ao != null) {
                material.setAmbientOcclusion(ao);
            }
        } else if ("transparency".equals(attribute)) {
            String value = config.getString(key).toUpperCase();
            Material.Transparency transparency = Material.Transparency.valueOf(value);

            if (transparency != null) {
                material.setTransparency(transparency);
            }
        } else if (attribute.startsWith("texture")) {
            List<TextureData> textureDataList = new ArrayList<TextureData>();
            for (int i = 0; i < 32; i++) {

                String fileKey = "material_" + materialName + "_texture" + i + "_file";
                String widthKey = "material_" + materialName + "_texture" + i + "_width";
                String heightKey = "material_" + materialName + "_texture" + i + "_height";
                String wrapKey = "material_" + materialName + "_texture" + i + "_wrap";
                String coordFunctionKey = "material_" + materialName + "_texture" + i + "_coord_function";
                String colorableKey = "material_" + materialName + "_texture" + i + "_colorable";
                String bumpmapKey = "material_" + materialName + "_texture" + i + "_bumpmap";
                String segmentsKey = "material_" + materialName + "_texture" + i + "_segments";
                String segmentKey = "material_" + materialName + "_texture" + i + "_segment";
                String fromKey = "material_" + materialName + "_texture" + i + "_from";
                String toKey = "material_" + materialName + "_texture" + i + "_to";
                String cellKey = "material_" + materialName + "_texture" + i + "_cell";

                if (config.getString(fileKey) == null) break;

                String/*File*/ file = new String/*File*/(config.getString(fileKey));

                double width = config.getDouble(widthKey, 1);
                double height = config.getDouble(heightKey, 1);
                boolean colorable = config.getBoolean(colorableKey, false);
                boolean isBumpMap = config.getBoolean(bumpmapKey, false);

                String wrapString = config.getString(wrapKey);
                TextureData.Wrap wrap = TextureData.Wrap.REPEAT;
                if ("clamp_to_border".equalsIgnoreCase(wrapString)) {
                    wrap = TextureData.Wrap.CLAMP_TO_BORDER;
                } else if ("clamp".equalsIgnoreCase(wrapString)) {
                    wrap = TextureData.Wrap.CLAMP;
                }

                String coordFunctionString = config.getString(coordFunctionKey);
                TexCoordFunction coordFunction = null;
                if (coordFunctionString != null) {
                    coordFunction = NamedTexCoordFunction.valueOf(
                            coordFunctionString.toUpperCase());
                }
                int segments = config.getInt(segmentsKey, 1);
                int segment = config.getInt(segmentKey, 0);

                VectorXZ from = getConfigVectorXZ(config.getString(fromKey));
                VectorXZ to = getConfigVectorXZ(config.getString(toKey));
                String cellclause = config.getString(cellKey);

                // bumpmaps are only supported in the shader implementation, skip for others
                if (!isBumpMap || "shader".equals(config.getString("joglImplementation"))) {
                    TextureData textureData = new TextureData(
                            file, width, height, wrap, coordFunction, colorable, isBumpMap, segment, segments, from, to, TextureData.AtlasCell.buildFromString(cellclause));
                    textureDataList.add(textureData);
                }
            }
            if (material.name.equals("WATER")){
                int h=9;
            }
            material.setTextureDataList(textureDataList);
        } else {
            logger.error("unknown material attribute: " + attribute);
        }
    }

    static VectorXZ getConfigVectorXZ(String s) {
        if (s == null) {
            return null;
        }
        String[] p = s.split(",");
        if (p.length != 2) {
            return null;
        }
        return new VectorXZ(Float.parseFloat(p[0]), Float.parseFloat(p[1]));
    }
}
