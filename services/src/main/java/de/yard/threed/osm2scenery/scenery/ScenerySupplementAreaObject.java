package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.polygon20.MeshFillCandidate;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2scenery.scenery.components.DefaultTerrainMeshAdder;
import de.yard.threed.osm2world.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Just a polygon. Z.B. füer die Gapfiller unter Brücken. Aber auch andere Transitions, z.B. Ramps und Brückensockel.
 * Gilt daher als "secondary". Entscheidend ist, dass diese keinen eigenen OSM Bezug haben, sondern einen Bezug
 * zu einem "Primary", der dann den OSM Bezug hat. Und diese Supplements tragen zum
 * TerrainMesh bei. Sonst bleiben da durch Background gefuellte Lücken.
 * Daher umbenannt SimpleSceneryObject->ScenerySupplementAreaObject
 * <p>
 * 18.4.19: Auch für Markierungen? 25.4.19 Nein, dafür ist {@link SceneryDecoration}
 * 27.7.19: Nicht mehr abstract. Sollte wegen Vereinfachung vielleicht keine Ableitung mehr haben.
 * 9.8.19: Die sollen keine eigene Elegroup haben, sonder die der Nachbarn verwenden. Und
 * soll auf TerrainMesh basieren.
 * <p>
 * <p>
 * Created on 15.08.18.
 */
public /*26.7.19abstract*/ class ScenerySupplementAreaObject extends SceneryFlatObject {
    @Deprecated
    protected Polygon basepolygon;
    //MeshPolygon meshPolygon;
    public static int deprecatedusage = 0;

    /**
     * 18.7.19: Eigentlich doch unschoen, Polygone schon ohne createPolygon() zu haben?
     * 26.7.19: durch iterative eigentlich doch  OK.
     * 9.8.18: Aber nur als TerrainMesh Teil. Darum doch deprecated.
     */
    @Deprecated
    public ScenerySupplementAreaObject(String creatortag, Polygon polygon, Material material) {
        super(creatortag, material, null, new Area(polygon, material));
        // den Cycle  lege ich jetzt einfach mal so fest
        cycle = Cycle.SUPPLEMENT;
        basepolygon = polygon;

        EleConnectorGroupSet areaElevation = new EleConnectorGroupSet();
        areaElevation.eleconnectorgroups = new ArrayList<>();

        EleConnectorGroup egr = new EleConnectorGroup(null);
        areaElevation.eleconnectorgroups.add(egr);
        elevations = areaElevation;
        deprecatedusage++;
        terrainMeshAdder=new DefaultTerrainMeshAdder(this);
    }

    /**
     * Fuer ein BG Element.
     * Ein Supplement daraus zu machen passt eigentlich nicht in die Verarbeitung. Aber nur so koennen left/right im Mesh gesetzt
     * werden.
     * TODO:kein MeshPolygon??
     */
    @Deprecated
    public ScenerySupplementAreaObject(String creatortag, MeshPolygon meshPolygon, Material material) {
        //this(creatortag, (Polygon) null, material);
        super(creatortag, material, null, new Area(meshPolygon, material, true));
        // den Cycle  lege ich jetzt einfach mal so fest
        cycle = Cycle.SUPPLEMENT;
        //in area this.meshPolygon = meshPolygon;
        //die Area muss direkt eingetragen werden, sonst geht später der getMeshPolygon nicht
        for (MeshLine meshLine : meshPolygon.lines) {
            TerrainMesh.getInstance().completeLine(meshLine, flatComponent[0]);
        }
        elevations = new EleConnectorGroupSet();

        isClipped = true;
        isCut = true;
        //muss ja nicht mehr ins mesh
        terrainMeshAdder=null;
    }

    /**
     *
     * Ein Supplement daraus zu machen passt eigentlich nicht in die Verarbeitung. Aber nur so koennen left/right im Mesh gesetzt
     * werden.
     */
    public ScenerySupplementAreaObject(String creatortag, MeshFillCandidate candidate, Material material) {
        //this(creatortag, (Polygon) null, material);
        super(creatortag, material, null, null);

        // den Cycle  lege ich jetzt einfach mal so fest
        cycle = Cycle.SUPPLEMENT;

        //TO DO auch left. Passiert doch spaeter im register()
        /*for (MeshLine line : candidate.lines) {
            if (line.right != null) {
                throw new RuntimeException("inconsistent");
            }
            line.right = this;
        }*/
        MeshPolygon meshPolygon=candidate.getMeshPolygon();
        flatComponent = new AbstractArea[]{new Area(meshPolygon, material, true)};

        //die Area muss direkt eingetragen werden, sonst geht später der getMeshPolygon nicht
        //aber nicht per complete, denn es sind u.U. beide Areas leer. lines sind CCW
        meshPolygon.setInner(this.flatComponent[0],candidate.leftIndicator);

        //der soll ja bestehdne Groups verwenden.
        elevations = new EleConnectorGroupSet();

        isClipped = true;
        isCut = true;
        //muss ja nicht mehr ins mesh
        terrainMeshAdder=null;
    }

    public ScenerySupplementAreaObject(String creatortag, Material material, Category category) {
        super(creatortag, material, category, new Area(null, material));
        // den Cycle  lege ich jetzt einfach mal so fest
        cycle = Cycle.SUPPLEMENT;

    }

    public void resolveSupplementOverlaps(List<SceneryFlatObject> overlaps) {

    }

    /**
     * Erstmal wird nur eine Grpou fuer das ganze Polygon gebaut.
     * 31.8.18: Tricky, denn hier muessen Punkte an fremde Groups gehangen werden. Besser im createPolygon mitmachen.
     *
     * @return
     */
    @Override
    protected void/*EleConnectorGroupSet*/ registerCoordinatesToElegroups() {
        /*if (poly.wascut) {
            //Util.notyet();
            logger.warn("Ignoring cut area for elevation");
        }*/

        /*if (roadorrailway!=null) {
            assignEleConnectorToGroups(roadorrailway.mapWay);
        }*/
        /*List<EleConnector> els = poly.polygonMetadata.getEleConnectors(null);
        EleConnectorGroup egr = elevations.eleconnectorgroups.get(0);
        egr.addAll(els);*/
        //return areaElevation;
    }

    /**
     * 23.8.19: Passt zur Logik vom Supplement, mesh teile zu füllen.
     * 27.8.19: Supplements entstehen aber nicht zwangsläufig aus dem TerrainMesh->das hier ist doof.
     * Mal wie in area und Runway machen.
     */
    /*28.8.19 nur noch Superklasse @Override
    public void addToTerrainMesh() {
        super.addToTerrainMesh();
        /*doof if (isMeshArea()) {
            TerrainMesh tm = TerrainMesh.getInstance();
            for (AbstractArea m : flatComponent) {
                //hier sollte es schon MeshPolygon geben
                MeshPolygon meshPolygon = m.getMeshPolygon();
                if (meshPolygon != null) {
                    for (MeshLine meshLine : meshPolygon.lines) {
                        tm.completeLine(meshLine, m);
                    }
                }
            }
        }* /

        // es kann ja mehrere Polygone geben
        /*for (AbstractArea abstractArea : flatComponent) {
            if (!abstractArea.isEmpty()) {
                Area.addAreaToTerrainMesh((Area) abstractArea, this, new ArrayList<>());
            }
        }* /
    }*/

    /**
     * Fuer jeden Polygonpunkt die Elevation eintragen. Fuer den Polygon kann das die Superklasse machen.
     */
    @Override
    public void calculateElevations() {
        super.calculateElevations();
    }

}
