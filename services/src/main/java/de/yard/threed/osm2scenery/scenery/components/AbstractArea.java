package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Util;
import de.yard.threed.core.Vector2;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2scenery.SceneryRenderer;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupFinder;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.elevation.EleCoordinate;
import de.yard.threed.osm2scenery.elevation.ElevationCalculator;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.RenderedArea;
import de.yard.threed.osm2scenery.util.SmartPolygon;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.OsmOrigin;
import org.apache.log4j.Logger;


/**
 * All flat non volumetric parts created from OSM. (aus TerrainSceneryObject)
 * Scenery object that has an area(polygon) to a graph, like road, railway, river.
 * Eine Fläche halt. Ob die Teil des Terrain oder ein Overlay ist, ist hier ubedeutend. 3.6.19: Nee, das ist hier jetzt property.
 * <p>
 * Als Bruecke hat es aber Volumen. Wenn damit das Model der Brücke gemeint ist, das ist dann was eigentsaendiges SceneryObjekt.
 * <p>
 * <p>
 * Dies kann auch ein Overlay sein, eine Area über anderen (Brücke).
 * 3.6.19: Brücken gelten doch nicht mehr als Overlay, sondern Volume? Aber die Fahrbahn oben drüber?
 * <p>
 * Created on 27.07.18.
 * <p>
 * 11.4.19: Aus AbstractSceneryFlatObject als Component ausgelagert. Ob das wirklich Sinn macht?
 * 23.4.19: Wohl schon. Nur das Material scheint etwas unpassend.
 * 12.6.19: Hier gibt es keine OSM/Map Daten und keine Elegroup. Die Polygonmetadaten sind da echt ein Fremdkörper.
 * 9.8.19: Zunehmend basierend auf TerrainMesh und damit nur noch fuer Terrain, nicht mehr für irgendwelche Flächen, z.B. (Decorations??).
 * Fuer Overlays aber auch, dann doch als Polygon? Vielleicht muss es bis zum Register auch immer erstmal ein Polygon bleiben?
 * Als TerrainProvider Teil des Mesh, als Overlay z.B. aber nicht.
 */
public abstract class AbstractArea {
    public static final AbstractArea EMPTYAREA = AbstractArea.createEmpty();
    //Werte wie 0.1 führen zu sichtbaren Lücken.
    public static double OVERLAYOFFSET = 0.01;

    protected Logger logger = Logger.getLogger(AbstractArea.class);

    //Polygon statt Geometry wegen möglicher Holes. Holes lassen sich nie vermeiden, die können immer beim union entstehen.
    public SmartPolygon poly;
    public Material material;
    //vertexdata isType created during triangulation/texturizing
    protected VertexData vertexData;
    public Coordinate[] uncutcoord;
    public boolean empty = false;
    public boolean isOverlay = false;
    //not just a helper for analysis. Also used for lookup.
    private String name;
    // optional parent for analysis
    public String parentInfo;
    //flag, ob area im mesh registriert ist. Dann läßt sich dort auch der Polygon dazu holen.
    public boolean isPartOfMesh = false;
    //Flag, ob wirklich ein Cut gemacht wurde.
    public boolean wasCut;

    public AbstractArea() {
        empty = true;
    }

    public AbstractArea(Material material) {
        this.material = material;
    }
    //public PolygonMetadata polygonMetadata;

    /**
     * Zuschneiden der Area aufs grid. Aber nicht ausschneiden aus BG.
     *
     * Returns
     * Können aber auch mal mehr sein.
     * 12.8.19:Nicht mehr sich selbständernd und damit sich selbst teilend. Und liefert null,
     * wenn es keinen Cut gab.
     *
     * @param gridbounds
     * @param abstractSceneryFlatObject
     * @param elevations
     * @return
     */
    public abstract CutResult cut(Geometry gridbounds, SceneryFlatObject abstractSceneryFlatObject, EleConnectorGroupSet elevations);

    /**
     * Returns true on success
     *
     * @return
     */
    public abstract boolean triangulateAndTexturize(EleConnectorGroupFinder eleConnectorGroupFinder, TerrainMesh tm);

    private static AbstractArea createEmpty() {
        return new AbstractArea() {
            @Override
            public CutResult cut(Geometry gridbounds, SceneryFlatObject abstractSceneryFlatObject, EleConnectorGroupSet elevations) {
                return null;
            }

            @Override
            public boolean triangulateAndTexturize(EleConnectorGroupFinder eleConnectorGroupFinder, TerrainMesh tm) {
                return true;
            }

            @Override
            public void registerCoordinatesToElegroups(EleConnectorGroupSet elevations, TerrainMesh tm) {

            }

            @Override
            public Vector2 getNormalAtCoordinate(Coordinate coordinate) {
                Util.notyet();
                return null;
            }

            @Override
            public MeshLine findMeshLineWithCoordinates(Coordinate c0, Coordinate c1, TerrainMesh tm) {
                Util.notyet();
                return null;
            }
        };
    }

    /**
     * 27.3.24: No longer use TerrainMesh?. This is a hell of dependencies. And why?
     * @return
     */
    public boolean isEmpty(TerrainMesh tm) {
        if (empty) {
            return true;
        }
        if (isPartOfMesh) {
            // who needs this? Almost every top level method/test. The reason is unclear. Appears weird.
            //Util.nomore();
            if (tm == null) {
                logger.warn("27.3.24: Not using TerrainMesh");
            } else {
                return getMeshPolygon(tm) == null;
            }
        }
        if (poly == null) {
            return true;
        }
        //for (Polygon p : poly.polygon) {
        if (poly.polygon.isEmpty()) {
            return true;
        }
        //}
        return false;
    }

    /**
     * Nicht einfach registrieren. Es koennte eine shared coordinate mit einer anderen Area sein.
     * Darum erst pruefen und ggfs skippen. Damit haengt eine Coordinate dieser Area evtl. an einer
     * fremden Group.
     */
    protected void registerCoordinateToElegroup(Coordinate c, EleConnectorGroup egr, TerrainMesh tm) {
        if (EleConnectorGroup.getGroup(c, false, "", true, tm) == null) {
            egr.add(new EleCoordinate(c, egr, "Area Polygon"));
        }
    }

    final public void calculateElevations(SceneryFlatObject parent, boolean isDeco, TerrainMesh tm) {
        if (!isEmpty(tm)) {
            if (isDeco){
                //ob embedded oder als Overlay. Die Decoartion hat keine eigene EleGroup (und damit keine registrierten Coordinates) und muss die Group/elevation des Parent verwenden.
                if (vertexData == null || vertexData.vertices == null) {
                    //19.7.19: Betrachte ich nicht mehr als warning?
                    logger.warn("area has no vertex data. Skipping elevation");
                } else {
                    for (Coordinate vertex:vertexData.vertices){
                        //TODO mitteln/average oder irgendwas??
                        vertex.z = parent.getEleConnectorGroups().get(0).getElevation();
                    }
                }
            }else {
                if (SceneryBuilder.FTR_SMARTBG) {
                    //ueber Meshpoints. Das passiert aber nicht hier auf Objektebene sondern ist schon VORHER im TerrainMesh passiert. Aber nur für die Polygone. VertexData muss
                    //dann doch hier passieren. Laut debug ist z schon gesetzt TODO pruefen
                    if (vertexData == null || vertexData.vertices == null) {
                        //19.7.19: Betrachte ich nicht mehr als warning?
                        logger.warn("area has no vertex data. Skipping elevation");
                    } else {
                        ElevationCalculator.calculateElevationsForVertexCoordinates(vertexData.vertices, "" + parent + "" + name + ",material=" + ((material == null) ? "" : material.getName()), tm);
                    }
                } else {
                    if (poly != null) {
                        Polygon p = poly.polygon;
                        ElevationCalculator.calculateElevationsForPolygon(p, poly.polygonMetadata, vertexData, "" + parent + "" + name + ",material=" + ((material == null) ? "" : material.getName()), tm);
                    }
                }
            }
            if (isOverlay && vertexData != null) {
                for (Coordinate v : vertexData.vertices) {
                    //Bei Aufruf mit "-p" (projected) wird das nicht aufgerufen!
                    v.z += OVERLAYOFFSET;
                }
            }
        }
    }

    public RenderedArea render(SceneryRenderer sceneryRenderer, String creatortag, OsmOrigin osmOrigin, EleConnectorGroupSet eleConnectorGroups, TerrainMesh tm) {
        RenderedArea ro = new RenderedArea();

        if (!isEmpty(tm)) {
            Polygon p = getPolygon(tm);
            if (p == null) {
                logger.error("np polygon");
                return ro;
            }
            RenderedArea r = sceneryRenderer.drawArea(creatortag, material, p, vertexData, osmOrigin, eleConnectorGroups);
            if (r != null && r.pinfo != null) {
                ro.pinfo.addAll(r.pinfo);
            }
        }
        return ro;
    }

    /**
     * Returns true, if any overlap was detected, false otherwise.
     * <p>
     * zu intersects,touches und overlap: http://docs.geotools.org/stable/userguide/library/jts/relate.html
     * Die JTS Logik ist etwas speziell. Und tricky wegen Rundungen?
     *
     * @param area
     * @return
     */
    public boolean overlaps(AbstractArea area) {
        if (this.poly == null || area == null || area.poly == null) {
            //should never happen. Why? just empty polygons
            return false;
        }
        Polygon p2 = area.poly.polygon;

        Polygon p1 = this.poly.polygon;

        if (!p1.isValid() || !p2.isValid()) {
            logger.error("overlap check with invalid polygon");
        } else {
            Boolean overlaps = JtsUtil.overlaps(p1, p2);
            if (overlaps == null) {
                logger.error("overlaps undecided");
                //Naja false? true?
                return true;
            }
            if (overlaps) {
                return true;
            }


        }
        return false;
    }

    /**
     * Schlecht mit oben zusammenfassbar wegen undecided.
     *
     * @param polygon
     * @return
     */
    public boolean overlaps(Polygon polygon) {
        if (this.poly == null) {
            //should never happen. Why? just empty polygons
            return false;
        }
        Polygon p1 = this.poly.polygon;
        if (!p1.isValid() || !polygon.isValid()) {
            logger.error("overlap check with invalid polygon");
        } else {
            Boolean overlaps = JtsUtil.overlaps(p1, polygon);
            if (overlaps == null) {
                logger.error("overlaps undecided");
            }
            if (overlaps) {
                return true;
            }
        }

        return false;
    }

    public VertexData getVertexData() {
        return vertexData;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void registerCoordinatesToElegroups(EleConnectorGroupSet elevations, TerrainMesh tm);

    /**
     * TODO 28.8.19 : muss der nicht mesh beruecksichtigen?
     * @return
     */
    public Polygon getPolygon(TerrainMesh tm) {
        if (poly == null) {
            return null;
        }
        return poly.polygon;
    }

    /**
     * Ob das der wahr Jakob ist??
     *
     * @param coordinate
     * @return
     */
    public Vector2 getNormalAtCoordinate(Coordinate coordinate) {
        Polygon p = poly.polygon;
        Vector2 normal;
        if ((normal = JtsUtil.getNormalAtCoordinate(p, coordinate)) != null) {
            return normal;
        }

        logger.error("no normal found. Returning nonsense default");
        return new Vector2(1, 0).normalize();
    }

    /**
     * Looks for polygon/line coordinates, not on edges.
     *
     * @param c0
     * @param c1
     * @return
     */
    public abstract MeshLine findMeshLineWithCoordinates(Coordinate c0, Coordinate c1, TerrainMesh tm);

    /**
     * Fuer Way wie auch Area geeignet.
     *
     * @return
     */
    final public MeshPolygon getMeshPolygon(TerrainMesh tm) {
        return tm.getPolygon(this);
    }


    public String getParentInfo() {
        if (parentInfo != null) {
            return "parent:" + parentInfo;
        }
        return "parent:unknown";
    }

    @Override
    public String toString(){
        return ""+getParentInfo();//Rekursionsgefahr! +",empty="+isEmpty();
    }
}
