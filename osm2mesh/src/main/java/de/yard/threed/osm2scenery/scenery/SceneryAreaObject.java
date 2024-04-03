package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.elevation.EleCoordinate;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2scenery.scenery.components.DefaultTerrainMeshAdder;
import de.yard.threed.osm2scenery.util.PolygonMetadata;
import de.yard.threed.osm2scenery.util.SmartPolygon;
import de.yard.threed.osm2world.JTSConversionUtil;
import de.yard.threed.osm2world.MapArea;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.SimplePolygonXZ;
import de.yard.threed.osm2world.VectorXZ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Basiert auf einer OSM Area.
 * Kann jetzt auch eine generische Area sein, damit SceneryFlatObject wieder abstract sein kann.
 * Gegenstück zu SceneryNodeObject.
 * Auch für die Grundfläche von Buildings.
 * <p>
 * Created on 15.08.18.
 */
public class SceneryAreaObject extends SceneryFlatObject {
    //either isType used!
    MapArea maparea;
    //14.6.19: TODO Fuer eine Node gibt es doch SceneryNodeObject
    @Deprecated
    MapNode mapNode;

    public SceneryAreaObject(MapArea maparea, String creatortag, Material material, Category category) {
        super(creatortag, material, category, new Area((Polygon) null, material));
        this.maparea = maparea;
        osmIds.add(maparea.getOsmId());
        terrainMeshAdder=new DefaultTerrainMeshAdder(this);
    }

    public SceneryAreaObject(MapNode mapNode, String creatortag, Material material, Category category, AbstractArea area) {
        super(creatortag, material, category, area);
        this.mapNode = mapNode;
        terrainMeshAdder=new DefaultTerrainMeshAdder(this);
    }


    @Override
    public void buildEleGroups() {
        elevations = new EleConnectorGroupSet();
        EleConnectorGroupSet areaElevation = new EleConnectorGroupSet();
        areaElevation.eleconnectorgroups = new ArrayList<>();

        if (maparea != null) {
            // Fuer jede Node gibt es eine Group.
            for (int i = 0; i < maparea.getBoundaryNodes().size(); i++) {
                MapNode node = maparea.getBoundaryNodes().get(i);
                EleConnectorGroup egr = new EleConnectorGroup(node);
                areaElevation.eleconnectorgroups.add(egr);
            }
        } else {
            // genau eine Group, scheint plausibel. Fuer Runway passt das.
            EleConnectorGroup egr = new EleConnectorGroup(mapNode);
            areaElevation.eleconnectorgroups.add(egr);
        }
        elevations = areaElevation;
    }

    /**
     * flatcomponent existiert wahrscheinlich schon.
     */
    @Override
    public List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds, TerrainMesh tm, SceneryContext sceneryContext) {
        if (maparea != null) {
            SimplePolygonXZ pXZ = maparea.getOuterPolygon();

            PolygonMetadata polygonMetadata = new PolygonMetadata(this);

            if (maparea.getOsmId() == 87822834) {
                int h = 9;
            }

            //boundary nodes isType a closed ring
            List<MapNode> mapNodes = maparea.getBoundaryNodes();
            Coordinate[] uncutcoord = new Coordinate[mapNodes.size()];
            for (int i = 0; i < mapNodes.size(); i++) {
                MapNode node = mapNodes.get(i);
                VectorXZ v = node.getPos();
                uncutcoord[i] = JTSConversionUtil.vectorXZToJTSCoordinate(v);
                if (i < mapNodes.size() - 1) {
                    // last already exists
                    EleCoordinate eleConnector = polygonMetadata.addPoint(node, uncutcoord[i]);
                    //25.4.19: zu frueh getEleConnectorGroups().eleconnectorgroups.get(i).add(eleConnector);
                }
                for (MapArea adj : node.getAdjacentAreas()) {
                    if (adj != maparea) {
                        if (adjacentmapareas.get(adj) == null) {
                            adjacentmapareas.put(adj, new ArrayList<Coordinate>());
                        }
                        adjacentmapareas.get(adj).add(uncutcoord[i]);
                    }
                }
            }

            if (uncutcoord.length < 4) {
                // even possible?
                logger.warn("invalid polygon with uncutcoord.length=" + uncutcoord.length);
                // will fail later with NPE
                return null;
            }
            Polygon polygon = JtsUtil.createPolygon(uncutcoord);
            //8.8.19: Aus Prinzipgründen kann man wohl kein CCW erzwingen

            //poly = polygon;
            //uncutPolygon = polygon;
            //23.7.19: Direkter cut und keine Metadaten mehr.
            flatComponent[0].poly = new SmartPolygon(polygon/*, polygonMetadata*/);
            //das Flag isCut besagt ja nur, das cut() aufgerufen wurde.
            boolean wasCut =  super.cutArea0(gridbounds);

            if (category == Category.BUILDING) {
                if (SceneryBuilder.FTR_BUILDINGASOVERLAY) {
                    flatComponent[0].isOverlay = true;
                }
                if (wasCut) {
                    // das kann ich drehen und wenden wie ich will. Ein Building, ob Overlay oder nicht, kann nicht geteilt werden. Darum erstmal ganz weglassen.
                    logger.debug("Cut building. Setting area to empty:" + getOsmIdsAsString());
                    flatComponent = new AbstractArea[]{AbstractArea.EMPTYAREA};
                }
            }
            flatComponent[0].parentInfo = this.toString();

            if (isTerrainProvider() && objects != null) {
                OverlapResolver.resolveOverlaps(this,objects,maparea.getOsmId(), tm);
            }
            //6.8.19: Stimmt das wohl so im Context? Ja.
            isClipped = true;

        } else {
            //no maparea, eine Mapnode
            //dann gibt es den Polygon schon
            //23.7.19: Ob das noch das Wahre ist?
        }
        return null;
    }


    @Override
    public void cut(GridCellBounds gridbounds) {
        //already happened in createPolygon(). Ja, aber das Ausschneiden aus dem BG wurde noch nicht gemacht. Also doch aufrufen.
        //NeeNee, das ist doch was anderes. 8.7.19: Aber es gibt Areas ohne OSM Bezug (z.B. Runway), für die in createPolygon()
        //kein cut gemacht wurde. Es wird auch nicht schaden, weil es nicht doppelt passiert.
        super.cut(gridbounds);
    }

    /**
     */
    /*28.8.19 nur noch in Superklasse @Override
    public void addToTerrainMesh() {
        super.addToTerrainMesh();
        if (maparea != null && maparea.getOsmId() == 87822834) {
            int h = 9;
        }

    }*/


    /**
     * 31.8.18: macht schon createPolygon
     * 25.4.19: NeeNee, das geht nicht mehr.
     * 12.6.19: Die Polygonmetadaten sind echt ein Fremdkörper. Es gibt hier die Elegroups und irgendeine area. Soll die
     * das doch machen.
     */
    @Override
    protected void registerCoordinatesToElegroups(TerrainMesh tm) {

        if (flatComponent != null) {
            if (maparea.getOsmId() == 87822834) {
                int h = 9;
            }
            for (AbstractArea area : flatComponent) {
                area.registerCoordinatesToElegroups(elevations, tm);
            }

            if (SceneryBuilder.FTR_TRACKEDBPCOORS) {
                //Die durch cut im BE entstandenen Coordinates registrieren. Erstmal einfach an die erste EleGroup. TODO improve
                EleConnectorGroup egr = elevations.eleconnectorgroups.get(0);
                if (newcoordinates != null) {
                    for (Coordinate c : newcoordinates) {
                        if (!EleConnectorGroup.hasGroup(c)) {
                            egr.add(new EleCoordinate(c, egr, "new BG coordinate"));
                        }
                    }
                }
            }
            //16.6.19: BG kommt vielleicht noch spater egr.locked = true;
        }
    }


    public static void registerAdjacentAreas(SceneryAreaObject area1, SceneryAreaObject area2) {
        if (area1.adjacentmapareas.keySet().contains(area2.maparea) || area2.adjacentmapareas.keySet().contains(area1.maparea)) {
            AreaSeam areaSeam = area1.adjacentareas.get(area2);
            if (areaSeam == null) {
                areaSeam = area2.adjacentareas.get(area1);
                if (areaSeam == null) {
                    areaSeam = new AreaSeam(null, null);

                    //es muesste doch egal sein, aus welcher Area die Coordinates genommen werden.
                    areaSeam.shareCandidate = JtsUtil.createLineFromCoordinates(area1.adjacentmapareas.get(area2.maparea));
                    if (areaSeam.shareCandidate == null) {
                        slogger.warn("shareCandidate==null: Ignoring seam");
                        return;
                    }
                    area2.adjacentareas.put(area1, areaSeam);
                }
                area1.adjacentareas.put(area2, areaSeam);
            } else {
                area2.adjacentareas.put(area1, areaSeam);

            }

        }
    }

    public Map<SceneryAreaObject, AreaSeam> getAdjacent() {
        return adjacentareas;
    }
}
