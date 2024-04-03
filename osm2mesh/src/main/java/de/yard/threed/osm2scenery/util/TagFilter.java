package de.yard.threed.osm2scenery.util;

import de.yard.threed.osm2world.TagGroup;
import org.apache.commons.lang3.StringUtils;


/**
 * Einfach erstmal nur ein String der From key=value
 */
public class TagFilter {
    String tagfilter;

    public TagFilter(String tagfilter) {
        this.tagfilter = tagfilter;
    }

    public boolean isAccepted(TagGroup tags) {
        if (tagfilter == null) {
            return true;
        }
        String[] parts = tagfilter.split("=");
        if (parts.length != 2) {
            throw new RuntimeException("invalid tagfiler " + tagfilter);
        }
        String tagvalue = tags.getValue(parts[0]);
        if (parts[1].equals("*")) {
            return true;
        }
        if (StringUtils.isEmpty(tagvalue) && StringUtils.isEmpty(parts[1])) {
            return true;
        }
        if (!StringUtils.isEmpty(tagvalue) && parts[1].contains(tagvalue)) {
            return true;
        }
        return false;
    }
}
