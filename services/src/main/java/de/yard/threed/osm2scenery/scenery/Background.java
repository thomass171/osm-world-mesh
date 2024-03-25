package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.PolygonSubtractResult;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.Materials;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Background Terrain, das sich aus den Tilegrenzen abzüglich aller OSM definierten Flächen ergibt.
 *
 * <p>
 * Created on 13.08.18.
 */
public class Background {
    static Logger logger = Logger.getLogger(Background.class);
    static boolean avoidholes = false;

    public List<BackgroundElement> background = null;
    public Material material = Materials.TERRAIN_DEFAULT;
    public List<Area> bgfiller = new ArrayList<>();

    public Background(Polygon polygon) {
        background = new ArrayList<>();
        if (!SceneryBuilder.FTR_SMARTBG) {
            addToList(polygon, background);
        }
    }

    /*private void add(Polygon polygon) {
        addToList(polygon, background);
    }*/

    /**
     * Neue Area fuer das Gesamtmesh.
     * <p>
     * Effektiv leere Areas werden uebergangen.
     */
    public void insert(SceneryFlatObject area, boolean cutonly) {
        //SceneryArea area=sceneryObject.getSceneryArea();
        if (/*background != null &&*/ !area.isTerrainProvider()) {
            return;
        }
        // 15.8.18: Ob die Validierung hier gut ist? 26.9.18: glaube schon.
        Polygon[] polygonToUse;
        if (area.getUncutPolygon() == null) {
            //19.4.19: Sowas gibt es, z.B. Connector. Keine Meldung wert.
            //TODO aber prüfen über empty
            //logger.warn("no polygon. Skipping! creatortag=" + area.creatortag);
            //return;
            //23.7.19: uncut gibt es nicht mehr immer
            if (area.getArea() == null) {
                return;
            }
            polygonToUse = new Polygon[area.getArea().length];
            for (int i = 0; i < polygonToUse.length; i++) {
                polygonToUse[i] = area.getArea()[i].getPolygon();
            }
        } else {
            polygonToUse = new Polygon[]{area.getUncutPolygon()};
        }

        for (Polygon p : polygonToUse) {
            if (p!=null) {
                if (!p.isValid()) {
                    logger.warn("invalid polygon. Skipping! creatortag=" + area.creatortag);
                } else {

                    List<Coordinate> newcoordinates = null;
                    //  aus dem background ausschneiden. Das kann bequem einen MultiPolygon liefern.
                    if (area.getOsmIdsAsString().contains("147175482")) {
                        int h = 9;
                    }
                    newcoordinates =/*background.*/cut(p, area.isCut/*getArea().poly.wascut*/);
                    area.cutIntoBackground = true;
                    area.setAdditionalBackgroundCoordinates(newcoordinates);
                }
            }
        }

        /*if (!cutonly && !area.isEmpty()) {
            //sceneryObjects.add(area);
            add(area);
        }*/
    }

    /**
     * Eine Area  aus dem background ausschneiden. Das kann bequem einen MultiPolygon liefern.
     * Es können aber wohl auch empty polygons entstehen. Die werden dann verworfen.
     * 12.6.19: Durch diesen cut werden sehr wahrscheinlich neue Coordinates entstehen, denen eine Ele-Registrierung fehlt
     * und fuer die nachher dann keine Elevation berechnet werden kann. Darum werden die hier returned. Ob das gut ist, muss sich noch zeigen.
     * 24.7.19: When die area schon cut ist, kann ein Polygon mit Hole entstehen,dass exakt auf einer Kante liegt. Das scheitert dann evtl. Triangulation. Darum dann
     * das Hole entfernen.
     */
    public List<Coordinate> cut(Polygon uncutarea, boolean alreadycut) {
        List<BackgroundElement> newbackground = new ArrayList<>();


        if (!uncutarea.isValid()) {
            logger.error("invalid uncutarea");
        }
        for (BackgroundElement be : background) {
            Polygon pePolygon = be.polygon;
            if (pePolygon.intersects(uncutarea)) {
                List<PolygonSubtractResult> diff = JtsUtil.subtractPolygons(pePolygon, uncutarea);
                if (diff==null||diff.size() == 0) {
                    //4.9.19:nicht loggenmswert
                    //logger.error("no diff found");
                } else {
                    for (PolygonSubtractResult p0 : diff) {
                        Polygon bep = p0.polygon;
                        if (bep.getNumInteriorRing() == 1 && alreadycut) {
                            Polygon bepwithouthole = JtsUtil.removeHoleOnEdge(bep);
                            if (bepwithouthole != null) {
                                logger.debug("hole removed from polygon");
                                bep = bepwithouthole;
                            }
                        }
                        addToList(bep, newbackground);

                    }
                }
            } else {
                addToList((pePolygon), newbackground);
            }
        }
        List<Coordinate> newcoordinates = new ArrayList<>();
        if (SceneryBuilder.FTR_TRACKEDBPCOORS) {
            for (BackgroundElement be : newbackground) {
                for (Coordinate c : be.polygon.getCoordinates()) {
                    if (!hasCoordinate(background, c) && !newcoordinates.contains(c)) {
                        newcoordinates.add(c);
                    }
                }
            }
        }
        //logger.debug("cut: new background elements: " + newbackground.size() + " with area " + getArea()+". Before were "+background.size());
        background = newbackground;
        return newcoordinates;

    }

    private boolean hasCoordinate(List<BackgroundElement> background, Coordinate coordinate) {
        for (BackgroundElement be : background) {
            for (Coordinate c : be.polygon.getCoordinates()) {
                if (c.equals2D(coordinate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private double getArea() {
        double area = 0;

        for (BackgroundElement be : background) {
            area += be.polygon.getArea();
        }
        return area;
    }

    /**
     * static weil die list uebergeben wird. Irgendwie nicht rund.
     *
     * @param polygon
     * @param list
     */
    private static void addToList(Polygon polygon, List<BackgroundElement> list) {
        if (polygon.isEmpty()) {
            return;
        }
        if (avoidholes && polygon.getNumInteriorRing() > 0) {
            logger.debug("background polygon has hole. Trying to remove");
            Polygon[] splitresult = JtsUtil.removeHoleFromPolygonBySplitting(polygon);
            if (splitresult != null) {
                for (Polygon p : splitresult) {
                    BackgroundElement backgroundElement = new BackgroundElement(p);
                    list.add(backgroundElement);
                }
                return;
            }
        }
        BackgroundElement backgroundElement = new BackgroundElement(polygon);
        list.add(backgroundElement);

    }

    /**
     * Das bekannte Terrain vereinen und daraus BG ermitteln.
     * <p>
     * 15.6.19: Unfertig.
     *
     * @param sceneryObjects
     */
    public void createFromRemaining(SceneryObjectList sceneryObjects) {
        List<SceneryObject> outside = new ArrayList<>();
        Geometry soareas = JtsUtil.GF.createPolygon(new Coordinate[]{});
        for (SceneryObject obj : sceneryObjects.objects) {
            if (obj instanceof SceneryFlatObject && obj.isTerrainProvider) {
                SceneryFlatObject asf = (SceneryFlatObject) obj;
                Polygon p = null;
                if (asf.getArea() != null && asf.getArea()[0].poly != null) {
                    p = asf.getArea()[0].poly.polygon;
                }
                if (p != null) {
                    try {
                        soareas = soareas.union(p);
                    } catch (TopologyException e) {
                        logger.error("union failed:" + e.getMessage());
                    }
                }
            }
        }
        logger.debug("sum of known objects has " + soareas.getNumGeometries() + " polygons");
        logger.debug("current background has " + background.size() + " elements");

    }


    public void addFiller(Area bgfiller) {
        this.bgfiller.add(bgfiller);
    }
}
