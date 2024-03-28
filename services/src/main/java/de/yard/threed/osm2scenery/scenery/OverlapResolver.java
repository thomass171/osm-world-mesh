package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.PolygonSubtractResult;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2scenery.scenery.components.WayArea;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import de.yard.threed.osm2world.Material;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resolves different types of overlaping areas.
 * <p>
 */
public class OverlapResolver {
    static Logger logger = Logger.getLogger(OverlapResolver.class);

    /**
     * Resolves the potential overlap of an area with other areas.
     * Not intended for ways. Zumindest nicht in erster Linie als Hauptobjekt. Overlapping ways sollten schon vorher resolved sein.
     */
    public static void resolveOverlaps(SceneryFlatObject sceneryAreaObject, List<SceneryObject> objects, long osmid, TerrainMesh tm) {
        if (sceneryAreaObject instanceof SceneryWayObject) {
            throw new RuntimeException("invalid usage");
        }
        if (objects != null) {
            List<SceneryFlatObject> overlaps = SceneryObjectList.getTerrainOverlaps(sceneryAreaObject, objects);
            if (overlaps.size() > 0) {
                List<Area> areas = new ArrayList<Area>();
                for (AbstractArea aa : sceneryAreaObject.getArea()) {
                    areas.add((Area) aa);
                }
                resolveTerrainOverlaps(sceneryAreaObject, overlaps, osmid, tm);
                //check again
                reCheck(sceneryAreaObject, objects, tm);
            }
        }
    }

    public static void reCheck(SceneryFlatObject sceneryAreaObject, List<SceneryObject> objects, TerrainMesh tm) {
        List<SceneryFlatObject> overlaps = SceneryObjectList.getTerrainOverlaps(sceneryAreaObject, objects);
        if (overlaps.size() > 0) {
            logger.warn(overlaps.size() + " unresolved overlaps remaining for " + sceneryAreaObject.toString());
            for (AbstractArea aa : sceneryAreaObject.getArea()) {
                logger.warn(" overlapping isType " + aa.getPolygon(tm));
            }

            for (SceneryFlatObject overlap : overlaps) {
                if (overlap.getArea().length > 1) {
                    logger.error("unexpected multiple area");
                }
                logger.warn("overlapped isType " + overlap.getArea()[0].getPolygon(tm));
            }
            SceneryContext.getInstance().unresolvedoverlaps += overlaps.size();
        }

    }

    /**
     * Resolve der inner overlaps ohne jeden Connector.
     * Keine Connector anpacken. Das ist knifflig und wird woanders gemacht.
     */
    public static void resolveInnerWayOverlaps(SceneryWayObject wayToReduce, AbstractArea overlappedarea, TerrainMesh tm) {
        WayArea wayArea = wayToReduce.getWayArea();

        for (int i = 1; i < wayArea.getLength() - 1; i++) {
            if (wayToReduce.innerConnectorMap.get(i) == null) {
                resolveSingleWayOverlap(wayArea, i, overlappedarea, tm);
            }
        }
    }

    /**
     * resolve way overlap by reducing the way at some specific location if required.
     * Returns new CoordinatePair if way was reduced, otherwise null.
     */
    public static CoordinatePair resolveSingleWayOverlap(WayArea wayToReduce, int position, AbstractArea overlappedarea, TerrainMesh tm) {
        //double check to avoid unnecessary operations
        /*lass ich mal if (!wayToReduce.overlaps(overlappedarea)) {
            return;
        }*/
        if (wayToReduce == null) {
            logger.error("invalid usage");
            return null;
        }
        CoordinatePair reduced = null;
        CoordinatePair[] pair = wayToReduce.getMultiplePair(position);
        if (pair == null || pair.length != 1) {
            logger.error("resolve:not yet");
            return reduced;
        }
        Polygon polygon = overlappedarea.getPolygon(tm);
        //TODO zu gross?iterativ
        double offset = 1;
        //nur wirklich coordinates innerhalb bearbeiten
        if ((JtsUtil.findVertexIndex(pair[0].left(), polygon.getCoordinates()) == -1 && JtsUtil.isPartOfPolygon(pair[0].left(), polygon)) ||
                (JtsUtil.findVertexIndex(pair[0].right(), polygon.getCoordinates()) == -1 && JtsUtil.isPartOfPolygon(pair[0].right(), polygon))) {
            /*if (node.getOsmId() == 1353883859) {
                    int h = 9;
                }*/
            reduced = wayToReduce.reduce(position, offset, tm);
            if (!wayToReduce.replace(new int[]{position}, reduced)) {
                logger.error("replace for adjust failed at way " + wayToReduce.parentInfo);
                reduced = null;
            } else {
                if (SceneryBuilder.OverlapResolverDebugLog) {
                    logger.debug("overlapping way coordinates reduced");
                }
            }

        }
        return reduced;
    }

    /**
     * Resolve an area overlap by reducing the areas polygon. Not intended for ways.
     * Das geht immer nur mit einer Area, die dann iterativ immer weiter verwendet wird!
     */
    public static void resolveTerrainOverlaps(SceneryFlatObject sceneryAreaObject, List<SceneryFlatObject> overlaps, long osmid, TerrainMesh tm) {
        String s = "";
        for (SceneryFlatObject overlap : overlaps) {
            s += overlap.toString() + ",";
        }
        if (SceneryBuilder.OverlapResolverDebugLog) {
            logger.debug("area " + osmid + " overlaps " + overlaps.size() + " existing(" + s + "). Trying to resolve.");
        }
        if (osmid == 225794253) {
            int h = 9;
        }
        boolean resolved;
        int cntr = 0;
        do {
            resolved = false;
            List<AbstractArea> totalresult = new ArrayList<>();
            for (AbstractArea area : sceneryAreaObject.getArea()) {
                List<AbstractArea> result = resolveSingleTerrainOverlap((Area) area, overlaps, osmid, sceneryAreaObject.material, sceneryAreaObject.polydiffs, tm);
                if (result == null) {
                    //no resolve, keep area.
                    totalresult.add(area);
                } else {
                    resolved = true;
                    for (AbstractArea aa : result) {
                        aa.parentInfo = sceneryAreaObject.toString();
                    }
                    totalresult.addAll(result);
                }
            }
            sceneryAreaObject.flatComponent = totalresult.toArray(new AbstractArea[0]);
        } while (resolved && cntr++ < 100);
    }

    /**
     * Resolve nur fuer einen einzigen Overlap.
     */
    static private List<AbstractArea> resolveSingleTerrainOverlap(Area area, List<SceneryFlatObject> overlaps,
                                                                  long osmid, Material material, List<PolygonSubtractResult> polydiffs,
                                                                  TerrainMesh tm) {

        //Ob das handling mehrerer Overlaps in einer Schleife wirklich geht, muss sich noch zeigen.
        //wihtig ist, dass immer das vorherige result weiterverwendet wird.

        for (SceneryFlatObject overlapobject : overlaps) {
            // die Area wurde durch einen Way(?) in zwei Areas geteilt oder overlaps nur leicht.
            // Weil ja noch areas dazukommen, muss der Overlap nicht unbedingt mit einem Way sein.
            for (AbstractArea overlaparea : overlapobject.getArea()) {
                List<PolygonSubtractResult> poliesWithoutOverlap = JtsUtil.subtractPolygons(area.poly.polygon, overlaparea.getPolygon(tm));
                if (poliesWithoutOverlap == null) {
                    //logger.debug("no overlap for current area to resolve");
                } else {

                    switch (poliesWithoutOverlap.size()) {
                        case 1:
                            // Apparently only a slight overlap.
                            if (SceneryBuilder.OverlapResolverDebugLog) {
                                logger.debug("area " + osmid + " slightly overlaps area of " + overlapobject.getOsmIdsAsString());
                            }
                            if (poliesWithoutOverlap.get(0).seam != null) {
                                //iterativ möglichst wenig verkleinern.
                                AbstractArea result = null;
                                double offset = 1;
                                do {
                                    Polygon polygon = reducePolygonAtSeamPoints(poliesWithoutOverlap.get(0).polygon, poliesWithoutOverlap.get(0).seam, offset);
                                    if (polygon == null) {
                                        //result = new AbstractArea[]{AbstractArea.EMPTYAREA};
                                    } else {
                                        if (!JtsUtil.overlaps(polygon, overlaparea.getPolygon(tm))) {
                                            result = new Area(polygon, material);
                                        }
                                    }
                                    offset += 1;
                                } while (result == null && offset < 10);
                                if (result == null) {
                                    logger.warn("reducing polygon failed. Using empty.");
                                } else {
                                    return Arrays.asList(new AbstractArea[]{result});
                                }
                            } else {
                                //error? Was ist das?
                                logger.warn("no seam; cannot resolve. Setting empty");
                            }
                            break;
                        case 2:
                            // Diff Result two polygons: way oder so was durch eine area.
                            // Erster Ansatz ist ein kleiner "Graben" zwischen den beiden Splithaelften statt eine echte Seam.
                            // Vielleicht auch immer, denn es gibt noch waytoareafiller.
                            if (SceneryBuilder.OverlapResolverDebugLog) {
                                logger.debug("area " + osmid + " split into two");
                            }
                            polydiffs.addAll(poliesWithoutOverlap);
                            Polygon[] p = new Polygon[2];
                            List<AbstractArea> overallresult = new ArrayList<>();

                            for (int i = 0; i < 2; i++) {
                                PolygonSubtractResult polydiff = poliesWithoutOverlap.get(i);
                                p[i] = polydiff.polygon;
                                if (polydiff.seam != null) {

                                    //1.8m breiter Gap.
                                    p[i] = reducePolygonAtSeamPoints(polydiff.polygon, polydiff.seam, 1.8);
                                    if (p[i] == null) {
                                        logger.warn("reducing polygon failed. Using empty.");
                                    } else {
                                        overallresult.add(new Area(p[i], material));
                                    }
                                } else {
                                    //muss es doch geben?
                                    logger.error("no seam between " + osmid + " and splitted poly.");
                                }
                            }
                            return overallresult;
                        default:
                            logger.warn("unexpected diff result size " + poliesWithoutOverlap.size());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns new polygon on success, null otherwise.
     * Evtl. offset so lange verkleinern, bis ein gültiger Polygon entsteht.
     * Die seam coordinates sind nicht zwingend ein LineString
     */
    private static Polygon reducePolygonAtSeamPoints(Polygon polygon, List<Coordinate> seamcoors, double offset) {

        Polygon newp;
        do {
            newp = JtsUtil.createResizedPolygon(polygon, seamcoors, offset);
            offset -= 0.4;
        }
        while (newp == null && offset > 0.1);
        if (newp == null) {
            logger.warn("finally invalid split polygon created for " /*+ osmid*/);
            SceneryContext.getInstance().warnings.add("invalid polygon created");
            return null;//JtsUtil.createEmptyPolygon();
        }
        return newp;

    }

}
