package de.yard.owm.services.osm;

import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.modules.SceneryModule;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.Phase;

import de.yard.threed.osm2world.MapWay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static de.yard.threed.osm2scenery.scenery.SceneryObject.Cycle.WAY;

@Service
@Slf4j
public class OsmElementService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<SceneryObject> process(MapWay mapWay, List<? extends SceneryModule> modules, TerrainMesh tm, SceneryContext sceneryContext){

        List<SceneryObject> sceneryObjects = new ArrayList<>();

        Phase.updatePhase(Phase.OBJECTS);

        for (SceneryModule module : modules) {
            SceneryObjectList areas = module.applyTo(mapWay, tm, sceneryContext);
            sceneryObjects.addAll(areas.objects);
        }
        // objects are only in sceneryObjects and not yet in TerrainMesh

        Phase.updatePhase(Phase.ELEGROUPS);

        //4.4.19 schon gemacht? sceneryMesh.createConnections();
        //Bridgeapproaches werden fuer Polygon gebraucht.
        //24.4.19: Und die brauchen Elegroups für die Groundstates.
        EleConnectorGroup.clear();
        //TODO 29.3.24 EleConnectorGroup.init((GridCellBounds) targetBounds, mapProjection);
        SceneryMesh.createElevationGroups(sceneryObjects);

        // 23.5.19 buildBridgeApproaches besser in Phasen abstrahieren?
        SceneryMesh.buildBridgeApproaches(sceneryObjects, sceneryContext);

        // Eine halbwegs schlüssige Klassifizierung (z.B. Garage) der Objekte geht erst jetzt, wenn der Kontext bekannt ist.
        /*3.4.24 skip for now until its clear how to do Phase.updatePhase(Phase.CLASSIFY);
        for (SceneryModule module : worldModules) {
            module.classify(mapData);
        }*/

        //20.8.19: ist doch zu frueh sceneryMesh.connectAreas(sceneryMesh.sceneryObjects.objects);

        //erst dann, wenn alle Objekte und Verbindungen bekannt sind, die Polygone dazu erstellen
        Phase.updatePhase(Phase.WAYS);
        processCycle( WAY, tm, sceneryObjects, tm.getGridCellBounds(), sceneryContext);

        return sceneryObjects;
    }

    private void processCycle(SceneryObject.Cycle cycle, TerrainMesh tm, List<SceneryObject> sceneryObjects,
                              GridCellBounds gridbounds, SceneryContext sceneryContext) {
        //Phase.updatePhase(Phase.POLYGONS);
        //sceneryMesh.createNonWaysPolygons();
        List<ScenerySupplementAreaObject> supplements = SceneryMesh.createPolygons(cycle, sceneryObjects, gridbounds, tm, sceneryContext);
        if (SceneryBuilder.FTR_OVERLAPCAUSESSUPPLEMENT) {
            //supplements verarbeiten fehlt.
            Util.notyet();
        }

        //Phase.updatePhase(Phase.CLIP);
        //sceneryMesh.clipNonWays();
        //3.4.24 sceneryMesh.clip(cycle);

        // und aus dem Background ausschneiden und selber zuschneiden.
        //Phase.updatePhase(Phase.CUT);
        //3.4.24 sceneryMesh.insertSceneryObjectsIntoBackgroundAndCut(cycle, tm);
    }
}
