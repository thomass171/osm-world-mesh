package de.yard.threed.osm2scenery.scenery.components;

import de.yard.threed.osm2scenery.scenery.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class DefaultTerrainMeshAdder implements TerrainMeshAdder {
    Logger logger = Logger.getLogger(DefaultTerrainMeshAdder.class);
    SceneryFlatObject sceneryFlatObject;

    public DefaultTerrainMeshAdder(SceneryFlatObject sceneryFlatObject) {
        this.sceneryFlatObject = sceneryFlatObject;
    }

    public void addToTerrainMesh(AbstractArea[] areas) {
        // es ist wichtig, null fuer die Seams zu uebergeben, wenn es keine gibt bzw. keine hier hinterlegt sind.
        List<AreaSeam> adjacentareas = (sceneryFlatObject.adjacentareas.size() == 0) ? null : new ArrayList<AreaSeam>(sceneryFlatObject.adjacentareas.values());
        TerrainMesh tm = TerrainMesh.getInstance();
        // es kann ja mehrere Polygone geben
        for (AbstractArea abstractArea : areas) {
            //keine leeren und nicht doppelt. Die Doppelgefahr besteht z.B. bei Supplements, die direkt aus dem Mesh erstellt wurden. Wird aber gelogged, weil
            //es nicht ganz koscher ist, hier hin zu kommen.
            if (!abstractArea.isEmpty()) {
                if (abstractArea.isPartOfMesh) {
                    logger.warn("area already part of mesh");
                } else {
                    Area.addAreaToTerrainMesh((Area) abstractArea, sceneryFlatObject, adjacentareas);
                }
            }
        }
    }
}
