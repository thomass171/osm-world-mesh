package de.yard.owm.services.osm;

import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.modules.SceneryModule;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.Phase;

import de.yard.threed.osm2world.MapWay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class OsmElementService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<SceneryObject> process(MapWay mapWay, List<? extends SceneryModule> modules, TerrainMesh tm){

        List<SceneryObject> sceneryObjects = new ArrayList<>();

        for (SceneryModule module : modules) {
            SceneryObjectList areas = module.applyTo(mapWay, tm);
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
        SceneryMesh.buildBridgeApproaches(sceneryObjects);

        return sceneryObjects;
    }
}
