package de.yard.threed.osm2scenery;

import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.modules.SceneryModule;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;
import de.yard.threed.osm2world.*;


import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Abgeleitet vom OSM2World WorldCreator.java
 * 2.8.18: Die Klasse brauchts doch eigentlich gar nicht.
 */
@Deprecated
public class SceneryCreator {
    Logger logger = Logger.getLogger(SceneryMesh.class);
    private Collection<SceneryModule> modules;
    //public SceneryMesh sceneryMesh = new SceneryMesh();

    public SceneryCreator(Configuration config, SceneryModule... modules) {
        this(config, Arrays.asList(modules));
    }

    public SceneryCreator(Configuration config, List<SceneryModule> modules) {
        this.modules = modules;
        /*11.7.18 for (SceneryModule module : modules) {
			module.setConfiguration(config);
		}*/
    }

    /**
     * Erstellt nur noch die Objects, den Rest macht Aufrufer.
     * @param mapData
     */
    public static SceneryMesh buildSceneryObjects(MapData mapData, GridCellBounds targetBounds, List<SceneryModule>  modules) {
        SceneryMesh sceneryMesh = new SceneryMesh();
        if (targetBounds != null) {
            sceneryMesh.setBackgroundMesh(targetBounds);
        }
        // 1 Scenery Objekte erstellen und auf zwei/drei Listen verteilen
        for (SceneryModule module : modules) {
            //if (module.isTerrainProvider()) {
                SceneryObjectList areas = module.applyTo(mapData);
                /*for (SceneryObject so : areas.objects) {
                    if (so instanceof SceneryAreaObject) {
                        //Polygone gibt es hier ja auch noch nicht
                        //((AbstractSceneryFlatObject) so).createPolygon();
                        //sceneryMesh.insert((AbstractSceneryFlatObject) so,false);
                       sceneryMesh.sceneryAreaObjects.add((SceneryAreaObject) so);
                    } else {
                        if (so instanceof SceneryWayObject) {
                           sceneryMesh.sceneryWayObjects.add((SceneryWayObject) so);
                        } else {
                            logger.error("unexpected SceneryObject");
                        }
                    }
                }*/
                sceneryMesh.sceneryObjects.objects.addAll(areas.objects);
            //}
        }

        // Ways ohne Polygone
        /*for (SceneryModule module : modules) {
            if (!module.isTerrainProvider()) {
                SceneryObjectList areas = module.applyTo(mapData);
                //
                for (SceneryObject so : areas.objects) {
                    if (so instanceof AbstractSceneryFlatObject) {
                        //16.8.18: cut erst nach Polygon 
                        // sceneryMesh.insert((AbstractSceneryFlatObject) so);
                        sceneryMesh.add((AbstractSceneryFlatObject) so);
                    } else {
                        sceneryMesh.add((SceneryVolumeOverlayObject) so);
                    }
                }
            }
        }*/
        
        return sceneryMesh;
    }

   /* public SceneryMesh getSceneryMesh() {
        return sceneryMesh;
    }*/


}
