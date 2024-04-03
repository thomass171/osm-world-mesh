package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Util;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.PolygonSubtractResult;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2scenery.RenderedObject;
import de.yard.threed.osm2scenery.SceneryRenderer;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.elevation.SimpleEleConnectorGroupFinder;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2scenery.scenery.components.CutResult;
import de.yard.threed.osm2scenery.scenery.components.DecoratorComponent;
import de.yard.threed.osm2scenery.scenery.components.TerrainMeshAdder;
import de.yard.threed.osm2scenery.util.RenderedArea;
import de.yard.threed.osm2scenery.util.SmartPolygon;
import de.yard.threed.osm2world.MapArea;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.OsmOrigin;
import de.yard.threed.osm2world.VectorXZ;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All flat non volumetric parts created from OSM. (aus TerrainSceneryObject)
 * Scenery object that has an area(polygon) to a graph, like road, railway, river. Als Bruecke hat es aber Volumen.
 * <p>
 * Stammt aus der alten SceneryArea, obwohl die mehrere OSMID darstellen konnte (durch merge). Das muss hier dann noch wieder rein,
 * wenn ich den merge wieder machen sollte.
 * SmartPolygon ist auch hier aufgegangen.
 * <p>
 * Dies kann auch ein Overlay sein, eine Area über anderen (Brücke).
 * <p>
 * 25.4.19: Mal nicht mehr abstract, um über SceneryObjectFactory universeller anlgebar zu sein.
 * 11.6.19: Dann brauchts aber eine MapNode für die Elegroup! Und damit wird es richig unrund. Entweder nicht abstrakt und keine Ableitung oder abstrakt mit
 * Ableitung. Sonst ist das unsauber. Lieber SceneryAreaObject ausbauen. Also:  wieder abstract. Naja, wal sehen. 30.7.19:Mal probieren.
 *
 * <p>
 * Created on 27.07.18.
 */
public abstract class /*Abstract*/SceneryFlatObject extends SceneryObject {
    public  Logger logger = Logger.getLogger(SceneryFlatObject.class);
    protected static Logger slogger = Logger.getLogger(SceneryFlatObject.class);
    //flatComponent wird durch createPolygon gesetzt.
    //18.4.19: flatComponent kann in exotischen Fällen auch mal null sein, z.B. bei BridgeRamps. Dann hat das SceneryObject einfach keine Fläche.
    //23.4.19: null ist aber keine gültiger Wert. Muss dann regulär empty polygon sein um abzugrenzen, ob es (noch) gar nicht gesetzt wurde.
    //12.8.19:Multiple jetzt hier statt in der Area. Das Array soll nicht null sein, im Zweifel leer, um Loops zu vereinfachen.
    public AbstractArea[] flatComponent;
    public DecoratorComponent decoratorComponent = null;
    public String texturizer = "";
    //Cutter cutter = new DefaultCutter();
//Das Flag heisst nur, dass cut bearbeitet wurde, nicht dass effektiv was cut wurde.
    public boolean cutIntoBackground = false;
    // Es kann auch mehrere Material pro SceneryObject geben, z.B. Runway. Anders abbilden? Ja, Runway wird anders,
    Material material;
    public List<Coordinate> newcoordinates;
    //just for analysis and testing. Eigentlich nur fuer "Area"
    public List<PolygonSubtractResult> polydiffs = new ArrayList<>();
    protected TerrainMeshAdder terrainMeshAdder=null;
    //enthaelt die gemeinsamen Coordinates mit anderen Areas. Das ist aber nur noch optional, evtl. deprecated
    //zugunsten der adHoc Ermittlung.
    Map<MapArea, List<Coordinate>> adjacentmapareas = new HashMap();
    public Map<SceneryAreaObject, AreaSeam> adjacentareas = new HashMap();


    /**
     * 9.8.19: Hier eine Area reinstecken widerspricht doch dem MeshPolygon Gedanken? Aber Buildings als Overlay?
     */
    public SceneryFlatObject(String creatortag, Material material, Category category, AbstractArea flatComponent) {
        super(creatortag, category);
        if (flatComponent != null) {
            //12.8.19:Zumindest null mal ignorieren
            this.flatComponent = new AbstractArea[]{flatComponent};//new AbstractArea(material);
        }
        this.material = material;
        if (category == Category.BUILDING && SceneryBuilder.FTR_BUILDINGASOVERLAY) {
            isTerrainProvider = false;
        }
    }

    /**
     * Universal Constructor
     * 25.4.19
     */
    /*4.6.19 public SceneryFlatObject(String creatortag, Material material, Category category, AbstractArea flatComponent, boolean isDecoration) {
        super(creatortag, category);
        //this.material = material;
        this.flatComponent = flatComponent;//new AbstractArea(material);
        this.material = material;
        this.isDecoration = isDecoration;
    }*/
    //public PolygonMetadata polygonMetadata;


    /**
     * Sollte nur gemacht werden, wenn die beiden auch intersecten.
     * 24.7.18: Doof dass leere entstehen. Die werden spaeter aber entfernt.
     *
     * @param
     */
    /*Komplikationen? public void merge(SceneryArea area) {
        if (!poly.intersects(area.poly)){
            logger.warn("merge: no intersection");
        }
        // Evtl. liefert der union() ein MultiPolygon
        poly = (Polygon) poly.union(area.poly);
        osmIds.addAll(area.osmIds);
        //Aus der gemrgten area den Inhalt entfernen
        area.osmIds.clear();
        area.poly = null;
        area.wasmerged = true;
    }*/

    /**
     * 25.4.19: Mal eine Defaultimplementierung mit genau einer Group. Fuer Runway passt das.
     */
    @Override
    public void buildEleGroups() {
        //erstmal einfach eine einzelne Group ohne mapnode
      /*11.6.19 doch nicht  elevations = new EleConnectorGroupSet();
        EleConnectorGroup egr = new EleConnectorGroup(null);
        getEleConnectorGroups().eleconnectorgroups.add(egr);*/
    }

    public void createDecorations() {
        //default, nothing to do
    }

    /**
     * 25.4.19: Mal eine Defaultimplementierung.
     */
    @Override
    public List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds, TerrainMesh tm) {
        //Nothing to do. Wahrscheinlich, area/poly kam schon als Parameter.
        return null;
    }


    /**
     * zuschneiden der Area auf die GridBounds. Wenns dumm laeuft zerschneidet das die area. Erstmal ignorieren. TODO
     * <p>
     * Hier passiert:
     * 1) klar: Den uncutpolygon zurechtschneiden ans grid
     * 2) die neu entstandenen Coordinates einer neuen(?) eleconnectorgroups zuordnen.
     * TODO das muss am grid doch immer eine neue sein. ausser die gridnode ist genau eine map node?
     * <p>
     * 4.4.19: Vielleicht kann das nicht in der Superklasse bleiben, weil Ways auch einen Cut für Junctions haben. NeeNee, das ist ein clip.
     * Das hier bleibt speziell für Tile/Grid.
     * 12.4.19: Und es ist generell für alle Areas geeignet, da es polygonbasiert ist. Damit ist der Cut auch zuverlässig richtig. Wegen
     * Texturing ist es für Ways aber ungünstig/unpraktisch.
     * 16.8.19: Obacht: durch PreCut kann der cut schon erfolgt sein. Dann wird das hier geskipped.
     *
     * @param gridbounds
     */
    @Override
    public void cut(GridCellBounds gridbounds) {
        if (getOsmIdsAsString().contains("87822834")) {
            int h = 9;
        }
        if (isCut) {
            return;
        }
        super.cut(gridbounds);


        if (flatComponent != null/* && flatComponent[0].poly != null*/) {
            for (int i = 0; i < flatComponent.length; i++) {
                AbstractArea abstractArea = flatComponent[i];

                CutResult cutResult = abstractArea.cut(gridbounds.getPolygon(), this, elevations);
                if (cutResult == null) {
                    //wasn das? 13.8.19: dann gab es halt keinen Cut
                    int h = 9;

                } else {
                    // Es ist ein Cut erfolgt. Oder die Area liegt komplett ausserhalb des Grid

                    if (cutResult.polygons.length == 1) {
                        if (cutResult.polygons[0].isEmpty()) {
                            //komplett ausserhalb
                            flatComponent[i] = AbstractArea.EMPTYAREA;
                        } else {
                            flatComponent[i].poly = new SmartPolygon(cutResult.polygons[0]);
                        }
                    } else {
                        if (flatComponent.length > 1) {
                            Util.notyet();
                        }

                        flatComponent = new AbstractArea[cutResult.polygons.length];
                        for (int j = 0; j < cutResult.polygons.length; j++) {
                            flatComponent[j] = new Area(cutResult.polygons[j], material);
                        }
                    }
                }
            }
        }
    }


    /**
     * 11.4.19: Das ist ziemlich generisch für alle möglichen Polygone. Das könnten Ways z.B. anders machen.
     */
    @Override
    public void triangulateAndTexturize(TerrainMesh tm) {
        if (creatortag.equals("RunwaySegment")) {
            int h = 9;
        }
        if (getOsmIdsAsString().contains("107468171")) {
            int h = 9;
        }
        if (flatComponent != null) {
            for (AbstractArea abstractArea : flatComponent) {
                if (!abstractArea.isEmpty(tm)) {
                    SimpleEleConnectorGroupFinder simpleEleConnectorGroupFinder = null;
                    if (getEleConnectorGroups() != null && getEleConnectorGroups().size() > 0) {

                        simpleEleConnectorGroupFinder = new SimpleEleConnectorGroupFinder(getEleConnectorGroups().eleconnectorgroups);
                    } else {
                        //17.8.19: strange? Z.B. bei WayToAreaFiller.
                        //logger.warn("strange: no ele groups?");
                    }
                    if (!abstractArea.triangulateAndTexturize(simpleEleConnectorGroupFinder, tm)) {
                        logger.error("Triangulation failed for area " + getOsmOrigin());
                        //TODO evtl. umstellen Area statt WayArea?
                    }

                }
            }
        }
        if (volumeProvider != null) {
            volumeProvider.triangulateAndTexturize(tm);
        }
        for (AbstractArea deco : decorations) {
            deco.triangulateAndTexturize(null, tm);
        }
    }

    public OsmOrigin getOsmOrigin() {
        if (this instanceof SceneryWayObject) {
            MapWay mapWay = null;
            mapWay = ((SceneryWayObject) this).mapWay;
            return new OsmOrigin(creatortag, getOsmIds(), mapWay);
        }
        if (this instanceof SceneryAreaObject) {
            MapArea mapArea;
            mapArea = ((SceneryAreaObject) this).maparea;
            return new OsmOrigin(creatortag, getOsmIds(), mapArea);
        }
        OsmOrigin osmOrigin = new OsmOrigin(creatortag, getOsmIds());
        return osmOrigin;
    }

    @Override
    boolean isValid() {
        if (!super.isValid()) {
            return false;
        }
        if (flatComponent != null /*&& flatComponent.poly != null*/) {
            for (AbstractArea abstractArea : flatComponent) {

                // for (Polygon p : abstractArea.poly.polygon) {
                if (abstractArea.poly != null && abstractArea.poly.polygon.getCoordinates().length < 4) {
                    //kein warn sondern error, weil ja nichts erzeugt wird
                    //es kann zwischenzeitlich durcuas leere geben. Die werden nachher entfernt.
                    //logger.error("SceneryArea:inconsistent? empty polygon");
                }

            }
        }
        return true;
    }

    /*jetzt getArea public SmartPolygon getPolygon() {
        if (flatComponent == null || flatComponent.poly == null) {
            return null;
        }
        return flatComponent.poly;
    }*/

    public boolean isEmpty(TerrainMesh tm) {
        if (flatComponent == null /*|| flatComponent.poly == null*/) {
            return true;
        }
        for (AbstractArea p : flatComponent) {
            if (!p.isEmpty(tm)) {
                return false;
            }
        }
        return true;
    }

    /*26.9.18 public Coordinate[] getCoordinatesForOsmNode(MapNode node) {
        //List<VectorXZ> v = nodemap.get(osmid);
        List<Coordinate> v = poly.polygonMetadata.getCoordinated(node);
        if (v == null) {
            return new Coordinate[0];
        }
        Coordinate[] pcoor = poly.polygon.getCoordinates();
        Coordinate[] coor = new Coordinate[v.size()];
        for (int i = 0; i < coor.length; i++) {
            coor[i] = pcoor[JtsUtil.findVertexIndex(/*new Coordinate(v.get(i).getX(), v.get(i).getZ()* /v.get(i), pcoor)];
        }
        return coor;
    }*/

    /**
     * Fuer jeden Polygonpunkt die Elevation eintragen.
     * Dafuer muss ich fuer jeden Polygonpunkt wissen, auf welche OSM Node er zurückgeht.
     * 2.8.18: Wobei das doch aus dem {@link EleConnectorGroup} hervorgeht? Aber cut Polygone sind doof.
     */
    @Override
    public void calculateElevations(TerrainMesh tm) {
        if (creatortag.equals("RunwaySegment")) {
            int h = 9;
        }

        Double baseelevation = null;
        if (flatComponent != null) {
            for (AbstractArea abstractArea : flatComponent) {
            /*for (Polygon p : flatComponent.poly.polygon) {
                ElevationCalculator.calculateElevationsForPolygon(p, flatComponent.poly.polygonMetadata, flatComponent.vertexData, this);
            }*/
                abstractArea.calculateElevations(this,false, tm);
                if (getEleConnectorGroups().size() > 0) {
                    //for now just from getFirst
                    baseelevation = getEleConnectorGroups().get(0).getElevation();
                }
            }
        }

        if (volumeProvider != null) {
            if (baseelevation != null) {
                volumeProvider.adjustElevation(baseelevation);
            } else {
                logger.warn("no base elevation for volume");
            }
        }

        for (AbstractArea deco : decorations) {
            //ob embedded oder als Overlay. Die Decoartion hat keine eigene EleGroup (und damit keine rehistrierten Coordinates) und muss die Group/elevation dieses Objects verwenden.
            deco.calculateElevations(this,true, tm);
        }
    }

    /**
     * Das muessen die ableitenden Klassen schon selber machen. Das hier ist zum Check.
     * Jetzt mal per Component.
     */
    public void addToTerrainMesh(TerrainMesh tm) {
        if (!isCut || !isClipped) {
            throw new RuntimeException("neither cut or clipped:" + getOsmIdsAsString());
        }
        // hier das Flag isterrainprovider zu prufen ist riskant, denn Bridges z.B. sind als Way keine TerrainProvider, als Container für Ramps und Gap aber schon.
        if (terrainMeshAdder!=null) {
            terrainMeshAdder.addToTerrainMesh(getArea(), tm);
        }
    }


    /**
     * 25.4.19: Mal eine Defaultimplementierung, die alle Coordinates der einen Group zuordnet? Fuer Runway passt das.
     * NeeNee, lieber abstract
     */
    @Override
    protected abstract void registerCoordinatesToElegroups(TerrainMesh tm) ;



    @Override
    public EleConnectorGroupSet getEleConnectorGroups() {
        return super.getEleConnectorGroups();
    }

/*12.6.19: Verschoben nach WayArea.register...
    protected void assignEleConnectorToGroups(MapWay mapWay) {

    }*/
    //abstract  /*Smart*/Polygon getPolygon();

    public Polygon getUncutPolygon() {
        if (flatComponent == null || flatComponent.length==0|| flatComponent[0].poly == null) {
            return null;
        }
        return flatComponent[0].poly.uncutPolygon;
    }

    //abstract EleConnectorGroupSet buildAreaElevation();

    /**
     * Rendern der Polygons und der VertexData und damit für alle Objects, auch Ways.
     *
     * @param sceneryRenderer
     * @return
     */
    @Override
    public RenderedObject render(SceneryRenderer sceneryRenderer, TerrainMesh tm) {
        OsmOrigin osmOrigin = getOsmOrigin();
        AbstractArea[] ars = getArea();
        RenderedObject ro = new RenderedObject();

        if (creatortag.contains("WayTo")) {
            int h = 9;
        }
        if (ars != null && (isTerrainProvider || !sceneryRenderer.isTerrainProviderOnly())) {
            for (AbstractArea ar : ars) {
                //leere kann es schon mal geben, z.B. Connector
                //SmartPolygon p = ar.poly;
                Polygon p = ar.getPolygon(tm);
                if (p != null) {
                    osmOrigin.polygon = p;
                    osmOrigin.texturizer = this.texturizer;
                    osmOrigin.wascut = /*flatComponent.poly.*/isCut;

                    RenderedArea r = ar.render(sceneryRenderer, creatortag, osmOrigin, getEleConnectorGroups(), tm);
            /*Polygon[] polies = ar.poly.polygon;
            //ro.pinfo = new PolygonInformation[getPolygon().polygon.length];
            for (int i = 0; i < polies.length; i++) {
                RenderedObject r = sceneryRenderer.drawArea(creatortag, flatComponent.material, polies[i], flatComponent.vertexData, osmOrigin, );
                if (r != null && r.pinfo != null) {
                    ro.pinfo.addAll(r.pinfo);
                }
            }*/

                    if (r != null && r.pinfo != null) {
                        // pois nicht nur fuer index 0?
                        if (r.pinfo.size() > 0) {
                            r.pinfo.get(0).pois = getPois();
                        }
                        ro.pinfo.addAll(r.pinfo);
                    }
                    //Decorator gibt es nur, wenn es auch eine Area gibt.
                    for (AbstractArea decoration : decorations) {
                        //OsmOrigin decoOsmOrigin = new OsmOrigin();
                        //kein OsmOrigin, weil es nicht aus OSM kommt.
                        r = decoration.render(sceneryRenderer, "Decoration", null, getEleConnectorGroups(), tm);
                        if (r != null && r.pinfo != null) {
                            ro.decorationinfo.addAll(r.pinfo);
                        }
                    }
                }
            }
        }
        if (volumeProvider != null) {
            for (int i = 0; i < volumeProvider.getWorldElements().size(); i++) {
                RenderedArea r = sceneryRenderer.drawElement(creatortag, volumeProvider.getWorldElements().get(i), osmOrigin);
                if (r != null && r.pinfo != null) {
                    ro.volumeinfo.addAll(r.pinfo);
                }
            }
        }
        return ro;
    }

    @Override
    public boolean isOverlay() {
        if (flatComponent == null || flatComponent.length==0) {
            return false;
        }
        return flatComponent[0].isOverlay;

    }

    protected List<VectorXZ> getPois() {
        return new ArrayList();
    }

    /**
     * 27.8.19: Warum war der creatortag denn raus? Ist doch hilfreich.
     * @return
     */
    @Override
    public String toString() {
        return creatortag+" " + getOsmOrigin();
    }

    public VertexData getVertexData() {
        return flatComponent[0].getVertexData();
    }

    /**
     * 9.8.19 Sollte nie null liefern. Im Zweifel empty.
     * 19.8.19:Naja ist doch ein Array. Und das ist bei empty halt leer. Trotzdem aber null nicht zulassen.
     * Das ist auch kein warning mehr. bei checkoverlaps kann das immer passieren, weil noch nicht alle Polygone da sind.
     * @return
     */
    public AbstractArea[] getArea() {
        if (flatComponent == null) {
            //logger.warn("shouldn't be null");
        }
        return flatComponent;
    }

    public Material getMaterial() {
        if (flatComponent == null) {
            return null;
        }
        return flatComponent[0].material;
    }


    public void setAdditionalBackgroundCoordinates(List<Coordinate> newcoordinates) {
        this.newcoordinates = newcoordinates;
    }

    /**
     * Wenn die (BG)Coordinate auf einem Polygon dieser Area liegt, deren Group liefern.
     *
     * @param coordinate
     * @return
     */
    public EleConnectorGroup findEleGroupForBgCoordinate(Coordinate coordinate) {
        /*12.8.19 if (flatComponent != null && flatComponent.poly != null) {
            Polygon poly = flatComponent.poly.polygon[0];
            //auch in uncutccord suchen. Aber nicht per Polygon, denn der kann invalid sein.
            List<LineSegment> l1 = JtsUtil.buildLineSegmentList(poly.getExteriorRing().getCoordinates());
            List<List<LineSegment>> l = new ArrayList();
            l.add(l1);
            if (flatComponent.uncutcoord != null) {
                l1 = JtsUtil.buildLineSegmentList(flatComponent.uncutcoord);
                l.add(l1);
            }
            for (List<LineSegment> p : l) {
                if (JtsUtil.onLine(coordinate, p)) {
                    //TODO richtige Group liefern
                    //logger.debug("found BG coordinate " + coordinate + "+ on poly " + creatortag);
                    if (elevations.size() > 0) {
                        return elevations.get(0);
                    }
                    logger.error("but has no group");
                }
            }
        }*/
        return null;
    }

    @Override
    public boolean covers(Coordinate coordinate, TerrainMesh tm) {
        if (flatComponent == null) {
            return false;
        }
        for (AbstractArea abstractArea : flatComponent) {
            if (!abstractArea.isEmpty(tm) && JtsUtil.isPartOfPolygon(coordinate, abstractArea.poly.polygon)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPartOfMesh(TerrainMesh tm) {
        //TODO und die anderen? Nicht eindeutig
        return ((Area) flatComponent[0]).isPartOfMesh;
    }

    //nicht eindeutig
    /*27.8.19 es gibt noch ispartofmesh @Deprecated
    public boolean isMeshArea() {
        if (flatComponent == null) {
            return false;
        }
        if (flatComponent[0] instanceof Area) {
            if (((Area) flatComponent[0]).isPartOfMesh) {
                return true;
            }
        }
        return false;
    }*/

    public boolean overlaps(SceneryFlatObject sfo) {
        if (getArea() == null) {
            return false;
        }
        for (AbstractArea a1 : getArea()) {
            if (sfo.getArea() != null) {
                for (AbstractArea a2 : sfo.getArea()) {

                    if (a1.overlaps(a2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean overlaps(Polygon polygon) {
        for (AbstractArea a1 : getArea()) {
            if (a1.overlaps(polygon)) {
                return true;
            }
        }
        return false;
    }

    /**
     * resolve overlaps
     * @param sfo
     */
    /*geht nicht generisch bzw schlecht. public void resolveOverlaps(AbstractArea o){
        //default. nothing to do
    }*/

    /**
     * Ausgelagert fuer breiteren Nutzen. Einen!, den 0er Polygon cutten.
     *
     * @param gridbounds
     */
    protected boolean cutArea0(GridCellBounds gridbounds) {
        boolean wasCut = false;
        if (gridbounds != null) {
            if (SceneryBuilder.FTR_DYNAMICGRID) {
                Util.nomore();
                /*if (!gridbounds.extend(polygon)) {
                    // completely outside grid. Make it empty
                    flatComponent = null;
                    return null;
                }*/
            } else {
                CutResult cutResult = flatComponent[0].cut(gridbounds.getPolygon(), this, getEleConnectorGroups());
                if (cutResult != null) {
                    // es ist ein Cut erfolgt
                    flatComponent = new AbstractArea[cutResult.polygons.length];
                    for (int i = 0; i < flatComponent.length; i++) {
                        Polygon p = cutResult.polygons[i];
                        flatComponent[i] = new Area(cutResult.polygons[i], material);

                    }
                    //geht hier noch nicht, weil die Area ja vielleicht noch geteilt wird. Und weils das Mesh noch nicht gibt.
                    //MeshLine meshLine = TerrainMesh.getInstance().splitBoundary(intersectionpoints);
                    wasCut = true;
                }
                isCut = true;
            }
        }
        return wasCut;
    }
}
