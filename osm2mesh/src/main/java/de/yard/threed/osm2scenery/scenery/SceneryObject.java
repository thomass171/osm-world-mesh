package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2scenery.RenderedObject;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryRenderer;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.VolumeProvider;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.TagGroup;

import java.util.ArrayList;
import java.util.List;


/**
 * Everything created from OSM.
 * <p>
 * Created on 25.07.18.
 */
public abstract class SceneryObject {
    public int id;
    private static int idvalue = 1;
    //public boolean isDecoration;
    // hier sind nur Referenzen auf die von diesem Object used Groups in der ElEleConnectorGroup.elegroups.
    // 2.9.18 eigentlich nicht gut weil errorprone to maintain. Vielleicht besser onthfly by polygon points?
    // 5.9.18: Das macht es beim cut aber knifflig und unübersichtlich, weil die da ueberarbeitet werden. Daher besser nicht onthefly.
    //26.9.18:Gibt es auch ohne Elevation, weil es ja auch Connector sind.
    //25.4.19: Das ist jetzt eh anders, die Groups brauchts aber für Groundstate ziemlich früh, und hier Referenzen ist zumindest nicht ganz schlecht.
    protected EleConnectorGroupSet elevations = null;
    protected List<Long> osmIds = new ArrayList<>();
    // Default isType true, because thats most common.
    // 28.8.19 das Flag isterrainprovider  ist riskant, denn Bridges z.B. sind als Way keine TerrainProvider, als Container für Ramps und Gap aber schon.
    // vielleicht besser ueber terrainadder != null. Mal deprecated. Obwohl das in der Verwendung leichte Unterschiede hat, z.B. bei BridgeGap.
    @Deprecated
    protected boolean isTerrainProvider = true;
    public String creatortag;
    Category category;
    public CustomData customData = null;
    //Name from OSM
    public String name = "";
    //hier statt in FlatObject weil es praktischer ist.
    public VolumeProvider volumeProvider = null;
    protected List<AbstractArea> decorations = new ArrayList<>();
    // 3.4.24: The cycle when the object (polygons?) was created?
    public Cycle cycle = Cycle.UNKNOWN;
    //The two "is" only indicate that the function was executed. Ob wirklich ein cut gemacht wurde, steht in der Area?
    public boolean isCut = false;
    boolean isClipped = false;
    public int failureCounter=0;

    public SceneryObject(String creatortag, Category category) {
        this.creatortag = creatortag;
        this.category = category;
        this.id = idvalue++;
    }

    public SceneryObject(String creatortag, Category category, long osmid) {
        this.creatortag = creatortag;
        this.category = category;
        this.id = idvalue++;
        osmIds.add(osmid);
    }

    /**
     * @param node
     * @return
     */
    protected EleConnectorGroup getEleConnectorGroup(MapNode node) {
        if (EleConnectorGroup.elegroups.get(node.getOsmId()) == null) {
            EleConnectorGroup eleConnectorGroup = new EleConnectorGroup(node);
            EleConnectorGroup.elegroups.put(node.getOsmId(), eleConnectorGroup);
        }
        return EleConnectorGroup.elegroups.get(node.getOsmId());
    }

    /**
     * TODO anders ordentlich. 16.8.18: Wird jetzt nach createPolygon gemacht.
     * 28.8.18: Deprecated fuer Group Anlage, weil im Cosntrctor. Nur noch fuer einhaengen Connector.
     */
    public void prepareElevationGroups() {
        buildEleGroups();
        // prepareElevations();
        // isValid();
    }

    /**
     *
     */
    public void connectElevationGroups(TerrainMesh tm) {
        registerCoordinatesToElegroups(tm);
        isValid();
    }

    public void cut(GridCellBounds gridbounds) {
        if (isCut) {
            return;
        }
        //wird nachher geprüft, darum immer setzen.
        isCut = true;
    }

    /**
     * Default implementation
     */
    boolean isValid() {
        return true;
    }

    public EleConnectorGroupSet getEleConnectorGroups() {
        return elevations;
    }

    /**
     * Returns groups shared with other scenery objects.
     * Default implementation.
     * 24.4.19: Das passt aber doch nicht zum Namen. Rename getOuterEleConnectorGroups-> getSharedEleConnectorGroups
     *
     * @return
     */
    public EleConnectorGroupSet getSharedEleConnectorGroups() {
        EleConnectorGroupSet egr = new EleConnectorGroupSet();
        return egr;
    }

    /**
     * Return false if only one group has no elevation.
     * @return
     */
    public boolean hasFixedElevationGroups() {
        return elevations.isFixed();
    }

    public List<Long> getOsmIds() {
        return osmIds;
    }

    /**
     * Better name maybe finalize()? includes triangulation.
     * Dabei werden auch die Vertices final festgelegt, damit die Zuordnung passt.
     * Und wenn ich grad dabei bin, auch triangulate.
     * Immer erst triangluate und dann texturieren, denn triangulate könnte weitere Vertices anlegen.
     */
    public abstract void triangulateAndTexturize(TerrainMesh tm);

    /**
     * Aus den fixed {@link EleConnectorGroup}s die Elevation (z Coordinate) aller Vertices ermitteln.
     * Läuft deswegen nach der Triangulation.
     */
    public abstract void calculateElevations(TerrainMesh tm);

    /**
     * 28.8.18: Nur noch zum registrieren der Polygon Coordinates in den Groups.
     * Das könnte eigentlich auch schon im createPolygon() mitgemacht werden, daziwschen liegt aber noch der cut.
     * NeeNee, der cut kommt danach. Als gehts es doch zusammen.
     * 25.4.19: Stimmt nicht mehr. Das passiert jetzt NACH dem cut.
     * 12.8.19: Bei SmartBG kommen die TerrainMesh (Points?) Coordinates an die Groups.
     */
    protected abstract void registerCoordinatesToElegroups(TerrainMesh tm);

    /**
     * 24.4.19: Doch eigenständig, weil die Polygone final vorliegen sollten.
     */
    protected abstract void buildEleGroups();

    public abstract RenderedObject render(SceneryRenderer sceneryRenderer, TerrainMesh tm);


    /**
     * Der (Smart)Polygon erstellen. Ist hier und nicht im ...Flat..., weil es acuh VolumeObjects mit Polygonen gibt (z.B. Bridges)
     * 18.4.19: Das kann in exotischen Fällen auch mal scheitern, z.B. bei BridgeRamps. Dann hat das SceneryObject einfach keine Fläche.
     * 18.7.19: Es könnten Supplements z.B. duch Overlaps entstehen.
     * 23.7.19: GridBounds auch übergeben, um optionasl schon einen cut machen zu können.
     */
    public abstract List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds, TerrainMesh tm, SceneryContext sceneryContext);

    public void clip(TerrainMesh tm) {
        if (isClipped) {
            return;
        }
        isClipped = true;
    }

    /**
     * True for areas that will be part of the terrain mesh, ie. cut into the background.
     * Die Fahrbeinteile ueber Bruecken z.B. gehoeren nicht dazu. Senkrechte Flächen der Rampen auch nicht, auch wenn sie Terrain mitbilden. 3.6.19: Ist das nicht unlogisch
     * oder unsauber definiert?
     * <p>
     * Default isType true, because thats most common.
     *
     * @return
     */
    public boolean isTerrainProvider() {
        return isTerrainProvider;
    }

    public String getCreatorTag() {
        return creatortag;
    }

    public Category getCategory() {
        return category;
    }

    public String getOsmIdsAsString() {
        String s = "";
        for (long l : osmIds) {
            s += l;
        }
        return s;
    }

    protected void setNameFromOsm(TagGroup tags) {
        String n = OsmUtil.getNameFromOsm(tags);
        if (n != null) {
            name = n;
        }
    }

    public String getName() {
        return name;
    }

    public void addDecoration(AbstractArea area) {
        if (area != null) {
            decorations.add(area);
        }
    }

    public List<AbstractArea> getDecorations() {
        return decorations;
    }

    public boolean isOverlay() {
        return false;
    }

    public boolean covers(Coordinate coordinate, TerrainMesh tm) {
        return false;
    }

    public Cycle getCycle() {
        return cycle;
    }

    public boolean isClipped() {
        return isClipped;
    }

    //22.8.18: Ist Category ein Ersatz fuer creatoretag? Erstmal nicht. Es kann immer nur eine Category pro Object geben, oder?
    //auf jeden Fall darf sie null sein.
    //22.4.19: Aber was genau ist das? Sowas wie ein Verbund/Netz? Strassenetz, Fluss/Seennetz, Eisenbahnnetz, Groundnet?
    //Und hat es überhaupt einen Einsatzzweck? Ja, z.B. beim ermitteln von Connections. Alle SceneryObjects (ways) einer Category sind
    //untereinder verbunden oder zumindest verbindbar sind (wichtig für Elevation) und haben die selbe Klasse, z.B. "Highway".
    //Darum zunächst mal auch Taxiway und Runway. 3.6.19: Und darum nicht mehr BRIDGE, weil das jetzt eine ROAD ist.
    //Betrifft auch die Nuzung der WayMap.
    public enum Category {
        ROAD, RIVER, RAILWAY, /*BRIDGE,*/ RUNWAY, TAXIWAY, BUILDING, APRON, GENERICAREA;

    }

    /**
     * Cycle defines, when the polygons were created.
     */
    public enum Cycle {
        WAY, BUILDING, GENERICAREA, UNKNOWN, SUPPLEMENT;

    }
}
