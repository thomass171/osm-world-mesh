package de.yard.threed.osm2scenery.util;



import de.yard.threed.osm2world.TagGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Eine oder multiple Kombinationen Tagftiler->String
 */
public class TagMap {
    List<TagFilter> tagfilter = new ArrayList();
    List<String> value = new ArrayList();

  public  TagMap() {

    }

    public void put(TagFilter tagFilter, String mappedvalue) {
        this.tagfilter.add(tagFilter);
        this.value.add(mappedvalue);
    }

    /**
     * Liefert den Wert des ersten passenden Tagfilters.
     *
     * @param tags
     * @return
     */
    public String getValue(TagGroup tags) {
        for (int i = 0; i < tagfilter.size(); i++) {
            if (tagfilter.get(i).isAccepted(tags)) {
                return value.get(i);
            }
        }
        return null;
    }

    public int getSize() {
        return tagfilter.size();
    }
}
