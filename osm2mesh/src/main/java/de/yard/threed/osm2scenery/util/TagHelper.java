package de.yard.threed.osm2scenery.util;

import de.yard.threed.core.Pair;
import de.yard.threed.osm2world.MapBasedTagGroup;

import java.util.HashMap;
import java.util.Map;

public class TagHelper {
    public static MapBasedTagGroup buildTagGroup(String key, String value) {
        Map<String, String> tagMap = new HashMap<>();
        tagMap.put(key, value);
        MapBasedTagGroup mapBasedTagGroup = new MapBasedTagGroup(tagMap);
        return mapBasedTagGroup;
    }

    public static MapBasedTagGroup buildTagGroup() {
        Map<String, String> tagMap = new HashMap<>();
         MapBasedTagGroup mapBasedTagGroup = new MapBasedTagGroup(tagMap);
        return mapBasedTagGroup;
    }

    public static MapBasedTagGroup buildTagGroup(Pair<String,String>[] keyvalue) {
        Map<String, String> tagMap = new HashMap<>();
        for (int i = 0; i < keyvalue.length; i++) {
            tagMap.put(keyvalue[i].getFirst(), keyvalue[i].getSecond());
        }
        MapBasedTagGroup mapBasedTagGroup = new MapBasedTagGroup(tagMap);
        return mapBasedTagGroup;
    }

    public static MapBasedTagGroup buildTagGroup(Map<String, String> tagMap) {

        MapBasedTagGroup mapBasedTagGroup = new MapBasedTagGroup(tagMap);
        return mapBasedTagGroup;
    }
}
