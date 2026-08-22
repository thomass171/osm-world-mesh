package de.yard.threed.osm2scenery.modules;

import de.yard.threed.osm2scenery.SceneryModuleBuilder;
import de.yard.threed.osm2scenery.modules.common.WayModule;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.TagGroup;

import static de.yard.threed.osm2scenery.modules.HighwayModule.isHighway;
import static de.yard.threed.osm2scenery.modules.HighwayModule.isPath;

public class OsmClassifier {
    // major roads, rivers, lakes
    public static int LOD_BASIC = 1;
    // parking areas, forest, agricultural areas
    public static int LOD_EXTENDED = 2;
    SceneryModuleBuilder moduleBuilder;
    int lod;

    // See README.md
    public static int WAY = 1;
    public static int RIVER = 2;

    public OsmClassifier(int lod, SceneryModuleBuilder moduleBuilder) {
        this.moduleBuilder = moduleBuilder;
        this.lod = lod;
    }

    public SceneryModuleBuilder getModule() {
        return moduleBuilder;
    }

    /**
     * For simplicity for now doesn't use tag filter from config
     */
    public static OsmClassifier classify(MapWay mapWay) {
        TagGroup tags = mapWay.getTags();
        if (isHighway(tags) /*&& tagfilter.isAccepted(mapWay.getTags()*/) {
            if (isPath(tags)) {
                return (new OsmClassifier(LOD_EXTENDED, (f, p, m)->new WayModule(f, p, m)));
            } else {
                return (new OsmClassifier(LOD_BASIC, (f, p, m)->new WayModule(f, p, m)));
            }
        }
        return null;
    }

    public boolean lodMatches(int lod) {
        return lod >= this.lod;
    }
}
