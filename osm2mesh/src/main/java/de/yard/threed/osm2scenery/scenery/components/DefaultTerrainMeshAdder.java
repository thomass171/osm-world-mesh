package de.yard.threed.osm2scenery.scenery.components;

import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.MeshServiceFacade;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshPolygonType;
import de.yard.threed.osm2scenery.scenery.*;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class DefaultTerrainMeshAdder extends TerrainMeshAdder {
    SceneryFlatObject sceneryFlatObject;

    public DefaultTerrainMeshAdder(String meshName, MeshServiceFacade meshService, SceneryProjection projection, SceneryFlatObject sceneryFlatObject) {
        super(meshName,meshService,projection);
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

    /**
     * Like persistWay.
     */
    public /*static*/ void persistArea(SceneryAreaObject sceneryAreaObject) throws MeshInconsistencyException, OsmProcessException {
        // for now only the first area is used. TODO: add all areas to the mesh.
        AbstractArea area = sceneryAreaObject.flatComponent[0];
        //area.getPolygon()
        registerArea(sceneryAreaObject.getOsmIds().get(0), MeshPolygonType.AREA, Arrays.asList(area.poly.polygon.getCoordinates()));
    }
}
