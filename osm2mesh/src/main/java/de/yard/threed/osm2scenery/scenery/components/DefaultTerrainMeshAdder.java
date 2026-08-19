package de.yard.threed.osm2scenery.scenery.components;

import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.scenery.*;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DefaultTerrainMeshAdder implements TerrainMeshAdder {
    SceneryFlatObject sceneryFlatObject;

    public DefaultTerrainMeshAdder(SceneryFlatObject sceneryFlatObject) {
        this.sceneryFlatObject = sceneryFlatObject;
    }

    //26.2.26 @Override
    public void addToTerrainMesh(AbstractArea[] areas, TerrainMesh tm) {

        // es ist wichtig, null fuer die Seams zu uebergeben, wenn es keine gibt bzw. keine hier hinterlegt sind.
        List<AreaSeam> adjacentareas = (sceneryFlatObject.adjacentareas.size() == 0) ? null : new ArrayList<AreaSeam>(sceneryFlatObject.adjacentareas.values());

        // es kann ja mehrere Polygone geben
        for (AbstractArea abstractArea : areas) {
            //keine leeren und nicht doppelt. Die Doppelgefahr besteht z.B. bei Supplements, die direkt aus dem Mesh erstellt wurden. Wird aber gelogged, weil
            //es nicht ganz koscher ist, hier hin zu kommen.
            if (!abstractArea.isEmpty()) {
                /*16.4.26 if (abstractArea.isPartOfMesh) {
                    log.warn("area already part of mesh");
                } else*/ {
                    Area.addAreaToTerrainMesh((Area) abstractArea, sceneryFlatObject, adjacentareas, tm);
                }
            }
        }

    }
}
