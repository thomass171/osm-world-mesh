package de.yard.owm.services.osm;

import de.yard.owm.services.persistence.TerrainMeshManager;
import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.modules.SceneryModule;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.OsmWay;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.ScenerySupplementAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.Phase;

import de.yard.threed.osm2world.MapWay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.yard.threed.osm2scenery.scenery.SceneryObject.Cycle.SUPPLEMENT;
import static de.yard.threed.osm2scenery.scenery.SceneryObject.Cycle.WAY;

@Service
@Slf4j
public class OsmElementService {

    @Autowired
    TerrainMeshManager terrainMeshManager;

    /**
     * Building TerrainMesh here every time might be more consistent, but is also a waste of resources.
     * Currently we consider OsmService to be the master service creating and maintaining TerrainMesh and SceneryContext.
     * However, this is the TX.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<SceneryObject> process(MapWay mapWay, List<? extends SceneryModule> modules, TerrainMesh tm, SceneryContext sceneryContext) throws OsmProcessException, MeshInconsistencyException {

        List<SceneryObject> sceneryObjects = new ArrayList<>();

        Phase.updatePhase(Phase.OBJECTS);

        for (SceneryModule module : modules) {
            SceneryObjectList areas = module.applyTo(mapWay, tm, sceneryContext);
            sceneryObjects.addAll(areas.objects);
        }
        // objects are only in sceneryObjects and not yet in TerrainMesh

        // not sure whether persist is good here
        for (SceneryObject sceneryObject : sceneryObjects) {
            if (sceneryObject instanceof SceneryWayObject swo) {
                swo.osmWay = tm.meshFactoryInstance.buildOsmWay(swo.mapWay.getOsmId(),
                        swo.mapWay.getMapNodes().stream().map(osmNode -> osmNode.getOsmId()).collect(Collectors.toList()));
            }
        }

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
        processCycle(WAY, tm, sceneryObjects, tm.getGridCellBounds(), sceneryContext);

            /*26.3.24 TODO


            log.info("Resolving way overlaps");
            sceneryMesh.resolveWaysAndConnectorOverlaps();
            SceneryContext.getInstance().overlappingways = sceneryMesh.checkForOverlappingAreas(true);
            log.debug("After resolving still " + SceneryContext.getInstance().overlappingways + " overlapping terrain provider way areas");

            //Ermitteln, was unter den Bridges ist und die Approaches der Bridges. Dazu werden die Polygone gebraucht.
            //13.7.19 nicht mehr sceneryMesh.completeBridgeRelations();
            //bridge gap soll eigentlich eine normale Area sein, braucht aber Polygon. Und "below".
            //sceneryMesh.closeBridgeGaps();

            if (SceneryBuilder.FTR_SMARTGRID) {
                Phase.updatePhase(Phase.GRIDREARRANGE);

                // 26.3.24 no longer cut targetBounds.rearrangeForWayCut(sceneryMesh.sceneryObjects.objects);
            }

            //die Areas brauchen für den Cut das finale Grid mit LazyCuts
            Phase.updatePhase(Phase.BUILDINGSANDAREAS);
            for (SceneryObject.Cycle cycle : new SceneryObject.Cycle[]{SceneryObject.Cycle.BUILDING, GENERICAREA, UNKNOWN}) {
                processCycle(sceneryMesh, cycle, terrainMesh);
            }
            SceneryContext.getInstance().overlappingterrain = sceneryMesh.checkForOverlappingAreas(true);
            log.debug(SceneryContext.getInstance().overlappingterrain + " overlapping terrain areas");

           end of TODO */

        //erst wenn alle Polygone/Areas da sind, können adjacent areas ermittelt werden.
        SceneryMesh.connectAreas(sceneryObjects);

        //Konsistenzcheck. OSM Objekte sind jetzt alle angelegt. Supplements darf noch nicht geben.
        List<SceneryObject> supples = SceneryObjectList.findObjectsByCycle(sceneryObjects, SUPPLEMENT);
        if (supples.size() > 0) {
            throw new RuntimeException("supplements not yet expected");
        }

        // Vor den Supplements das Mesh erstellen. Die Supplements koennen dann daran anschliessen, muessen aber nicht (oder auch teilweise).
        // Ohne Smartgrid kann das nicht konsistent werden.
        Phase.updatePhase(Phase.TERRAINMESH);
        log.info("Updating terrain mesh with " + sceneryObjects.size() + " scenery objects (ways and areas).");
        //sonst geht waytoarea filler nicht if (SceneryBuilder.FTR_SMARTBG) {
        //erst die Ways, danach areas, um Komplkationen zu vermeiden.

        log.info("adding ways to terrain mesh");
        tm.addWays(sceneryObjects);
        log.info("adding areas to terrain mesh");
        tm.addAreas(sceneryObjects);


        //}


            /*26.3.24 TODO
            //Supplements anlegen und verarbeiten
            Phase.updatePhase(Phase.SUPPLEMENTS);
            log.info("Creating supplements for " + sceneryMesh.sceneryObjects.size() + " scenery objects.");

            for (SceneryModule module : worldModules) {
                //Das durefen definitionsgemaess nur Supplements mit Cycle SUPPLEMENT sein.
                List<ScenerySupplementAreaObject> supplements = module.createSupplements(Collections.unmodifiableList(sceneryMesh.sceneryObjects.objects));
                if (supplements != null) {
                    for (ScenerySupplementAreaObject s : supplements) {
                        //Supplement haben vielleicht gar keine eigenen
                        s.prepareElevationGroups();
                    }
                    sceneryMesh.sceneryObjects.objects.addAll(supplements);
                }
            }
            processCycle(sceneryMesh, SUPPLEMENT);



            log.info("Resolving supplement overlaps");
            sceneryMesh.resolveSupplementOverlaps();
            // wenn durch Supplements overlaps entstanden sind, wird das mit Sicherheit zu Problemen im TerrainMesh führen.
            // SceneryContext.getInstance().unresolvedoverlaps ist aber nur der Zaehler fuer versuchte und gescheiterte!
            // Darum neu zählen.
            String comment = "no terrain overlaps";
            SceneryContext.getInstance().overlappingTerrainWithSupplements = sceneryMesh.checkForOverlappingAreas(true);
            if (SceneryContext.getInstance().overlappingTerrainWithSupplements > 0) {
                comment = "" + SceneryContext.getInstance().overlappingTerrainWithSupplements + " terrain overlaps";
            }
            log.info("Created " + sceneryMesh.sceneryObjects.findObjectsByCycle(SUPPLEMENT).size() + " supplements. Now " + sceneryMesh.sceneryObjects.size() + " scenery objects (" +
                    comment + ").Start adding to mesh.");

            // Supplements muessen auch ins TerrainMesh
            TerrainMesh.getInstance().addSupplements(sceneryMesh.sceneryObjects.findObjectsByCycle(SUPPLEMENT));
            boolean meshValid = TerrainMesh.getInstance().isValid(true);
            //gap filler sind zwar auch supplements. Aber die haengen sich schon selber ins mesh.
            log.info("Supplements added to terrain mesh (mesh " + ((meshValid) ? "valid" : "invalid") + "). Creating gap filler");
            int cnt = sceneryMesh.createWayToAreaFiller();
            log.info("Created " + cnt + " gap filler");

            Phase.updatePhase(Phase.BACKGROUND);
            sceneryMesh.createBackground();

            //lieber erst nach bridgeaaproaches dekorieren. 24.5.19 braucht doch Polygone. Die Aussenpolygone ändern sich nicht,
            //darum ist nach "cut" OK.
            Phase.updatePhase(Phase.DECORATION);
            sceneryMesh.createDecorations();

            Phase.updatePhase(Phase.OVERLAPS);
            sceneryMesh.processOverlaps();

            //25.4.19: Gefällt mir besser: Erst nach dem cut die Polygone in die Elegroups connecten. Dann spart man sich den
            //Huddle mit den Änderungen durch den cut. Und die Elegroups sind halbwegs frei von alten Coordinates.
            //Sogar erst nach Triangulation. Dann hat man wirklich alle Coordinates.
            Phase.updatePhase(Phase.POLYGONSREADY);
            //sceneryMesh.connectElevationGroups();
            end of TODO */

        // persisting is not really needed (because its done by entity manager at TX end), but also does a final consistency check.
        terrainMeshManager.persist(tm);
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
