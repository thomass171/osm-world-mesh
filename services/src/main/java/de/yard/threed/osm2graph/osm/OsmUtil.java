package de.yard.threed.osm2graph.osm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import de.yard.threed.core.Degree;
import de.yard.threed.core.LatLon;
import de.yard.threed.core.Util;
import de.yard.threed.core.Vector2;
import de.yard.threed.osm2world.ConfMaterial;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.Materials;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.OSMWay;
import de.yard.threed.osm2world.Osm2WorldMapProjection;
import de.yard.threed.osm2world.TagGroup;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.traffic.geodesy.GeoCoordinate;
import de.yard.threed.trafficfg.FgCalculations;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.yard.threed.osm2world.EmptyTagGroup.EMPTY_TAG_GROUP;

/**
 * Alles was z.B. mit VectorXZ zu tun hat.
 * <p>
 * Created on 13.06.18.
 */
public class OsmUtil {
    private static int dummyid = -1;
    static Logger logger = Logger.getLogger(OsmUtil.class);

    public static LatLon unproject(Osm2WorldMapProjection projection, VectorXZ xz) {
        return LatLon.fromDegrees(projection.calcLat(xz), projection.calcLon(xz));
    }

    public static LatLon unproject(SceneryProjection projection, VectorXZ xz) {
        LatLon u = projection.unproject(xz);
        return u;
    }

    public static Vector2 toVector2(VectorXZ xz) {
        return new Vector2(xz.x, xz.z);
    }

    public static Vector2 toVector2(Coordinate xz) {
        return new Vector2(xz.x, xz.y);
    }

    public static VectorXZ project(Osm2WorldMapProjection projection, LatLon coord) {
        return projection.calcPos(coord);
    }

    public static VectorXZ project(SceneryProjection projection, LatLon coord) {
        return projection.project(coord);
    }

    public static VectorXZ toVectorXZ(Vector2 v) {
        return new VectorXZ(v.x, v.y);
    }

    public static GeoCoordinate toSGGeod(LatLon latLon) {
        return new GeoCoordinate(new Degree(latLon.getLatDeg().getDegree()), new Degree(latLon.getLonDeg().getDegree()), 0);
    }

    public static LatLon toLatLon(GeoCoordinate coord) {
        return coord;//22.12.21 LatLon.fromDegrees(coord.getLatitudeDeg().getDegree(), coord.getLongitudeDeg().getDegree());
    }

    public static OSMNode buildDummyNode(SceneryProjection mapProjection, VectorXZ xz) {
        LatLon latLon = calcLatLon(mapProjection, xz);
        return buildDummyNode(latLon.getLatDeg().getDegree(), latLon.getLonDeg().getDegree());
    }

    public static OSMNode buildDummyNode(Osm2WorldMapProjection mapProjection, VectorXZ xz) {
        return buildDummyNode(mapProjection.calcLat(xz), mapProjection.calcLon(xz));
    }

    public static OSMNode buildDummyNode(double lat, double lon) {
        OSMNode node = new OSMNode(lat, lon, EMPTY_TAG_GROUP, dummyid);
        dummyid--;
        return node;
    }

    public static OSMWay buildDummyWay(TagGroup tagGroup, List<OSMNode> nodes) {
        OSMWay node = new OSMWay(tagGroup, dummyid, nodes);
        dummyid--;
        return node;
    }

    public static VectorXZ calcPos(SceneryProjection mapProjection, LatLon latlon) {
        return (project(mapProjection, latlon));
    }

    public static VectorXZ calcPos(SceneryProjection mapProjection, double lat, double lon) {
        return (project(mapProjection, LatLon.fromDegrees(lat, lon)));
    }

    public static LatLon calcLatLon(SceneryProjection mapProjection, VectorXZ xz) {
        return (mapProjection.unproject(xz));
    }

    public static VectorXZ round(VectorXZ xz, int i) {
        VectorXZ rounded = new VectorXZ(Util.roundDouble(xz.x, i), Util.roundDouble(xz.z, i));
        return rounded;
    }

    public static GeoCoordinate round(GeoCoordinate xz, int i) {
        GeoCoordinate rounded = new GeoCoordinate(new Degree(Util.roundDouble(xz.getLatDeg().getDegree(), i)),new Degree(Util.roundDouble(xz.getLonDeg().getDegree(), i)),  xz.getElevationM());
        return rounded;
    }

    public static String getNameFromOsm(TagGroup tags) {
        String n = tags.getValue("name");

        return n;
    }

    /**
     * From OSM2World.Materials.
     */
    public static void loadMaterialConfiguration(Configuration config, ConfMaterial material, boolean filterbymaterialname) {
        Iterator<String> keyIterator = config.getKeys();

        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            Matcher matcher = Pattern.compile(Materials.CONF_KEY_REGEX).matcher(key);

            if (matcher.matches()) {
                String materialName = matcher.group(1);
                String attribute = matcher.group(2);
                //21.5.19: Das war tatsaechlich mal gedacht, dass die config spezifisch ist und komplett, unabhaengig vom material name gelesen wird.
                //Ist nicht so ganz sauber, darum optional
                if (!filterbymaterialname || materialName.equals(material.getName())) {
                    Materials.applyConfigEntryToMaterial(attribute, key, config, material, materialName);
                }
            }
        }
    }

    public static LatLon getLatLon(OSMNode osmNode) {
        return LatLon.fromDegrees(osmNode.lat, osmNode.lon);
    }

    public static Degree getHeadingAtEnd(MapWay mapWay) {
        List<MapNode> nodes = mapWay.getMapNodes();
        MapNode end = nodes.get(nodes.size() - 1);
        MapNode beforeend = nodes.get(nodes.size() - 2);
        Degree heading = new FgCalculations().courseTo(toSGGeod(getLatLon(beforeend.getOsmNode())),(toSGGeod(getLatLon(end.getOsmNode()))));
        return heading;
    }

    public static int getFirstGridNode(MapWay mapWay) {
        int index = 0;
        for (MapNode mapNode : mapWay.getMapNodes()) {
            if (mapNode.location == MapNode.Location.GRIDNODE) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static int getLastGridNode(MapWay mapWay) {
        for (int index = mapWay.getMapNodes().size() - 1; index >= 0; index--) {
            MapNode mapNode = mapWay.getMapNodes().get(index);
            if (mapNode.location == MapNode.Location.GRIDNODE) {
                return index;
            }
        }
        return -1;
    }

    public static Vector2 getDirection(VectorXZ from, VectorXZ to) {
        return toVector2(to).subtract(toVector2(from)).normalize();
    }

    /**
     * Ein Way kann closed sein, dann ist start==end. Darum erst die endNode pruefen.
     */
    public static Vector2 getDirectionToNode(MapWay w, MapNode node) {
        Vector2 dir;
        if (node == w.getEndNode()) {
            int i = w.getMapNodes().size();
            dir = getDirection(w.getMapNodes().get(i - 2).getPos(), w.getMapNodes().get(i - 1).getPos());
        } else {
            if (node == w.getStartNode()) {
                dir = getDirection(w.getMapNodes().get(1).getPos(), w.getMapNodes().get(0).getPos());
            } else {
                logger.error("inconsistent");
                return null;
            }
        }
        return dir;
    }

    /**
     * Ein Way kann closed sein, dann ist start==end. Darum erst die startNode pruefen.
     * Result isType normalized.
     */
    public static Vector2 getDirectionFromNode(MapWay w, MapNode node) {
        Vector2 dir;
        if (node == w.getStartNode()) {
            dir = getDirection(w.getMapNodes().get(0).getPos(), w.getMapNodes().get(1).getPos());
        } else {
            if (node == w.getEndNode()) {
                int i = w.getMapNodes().size();
                dir = getDirection(w.getMapNodes().get(i - 1).getPos(), w.getMapNodes().get(i - 2).getPos());
            } else {
                logger.error("inconsistent");
                return null;
            }
        }
        return dir;
    }

    /**
     * TO DO: getDirection*Node von oben verwenden
     *
     * @param w0
     * @param w1
     * @param node
     * @return
     */
    public static Degree getAngle(MapWay w0, MapWay w1, MapNode node) {
        Vector2 dir0, dir1 = null;
        if (node == w0.getStartNode()) {
            dir0 = getDirection(w0.getMapNodes().get(0).getPos(), w0.getMapNodes().get(1).getPos());
            if (node == w1.getStartNode()) {
                dir1 = getDirection(w1.getMapNodes().get(0).getPos(), w1.getMapNodes().get(1).getPos());
                return Degree.buildFromRadians(Vector2.getAngleBetween(dir0, dir1));
            } else if (node == w1.getEndNode()) {
                int l = w1.getMapNodes().size();
                dir1 = getDirection(w1.getMapNodes().get(l - 1).getPos(), w1.getMapNodes().get(l - 2).getPos());
                return Degree.buildFromRadians(Vector2.getAngleBetween(dir0, dir1));
            }
        } else {
            if (node == w0.getEndNode()) {
                int i = w0.getMapNodes().size();
                dir0 = getDirection(w0.getMapNodes().get(i - 1).getPos(), w0.getMapNodes().get(i - 2).getPos());
                if (node == w1.getStartNode()) {
                    dir1 = getDirection(w1.getMapNodes().get(0).getPos(), w1.getMapNodes().get(1).getPos());
                    return Degree.buildFromRadians(Vector2.getAngleBetween(dir0, dir1));
                } else if (node == w1.getEndNode()) {
                    int l = w1.getMapNodes().size();
                    dir1 = getDirection(w1.getMapNodes().get(l - 1).getPos(), w1.getMapNodes().get(l - 2).getPos());
                }
            } else {
                logger.error("inconsistent");
                return null;
            }
        }

        Degree degree = Degree.buildFromRadians(Vector2.getAngleBetween(dir0, dir1));
        return degree;
    }

    /**
     * Returns [way,fromnodeindex]
     */
    public static Object[] findClosestLine(VectorXZ v, List<OSMWay> ways, SceneryProjection projection) {
        double bestdistance = Double.MAX_VALUE;
        OSMWay best = null;
        Integer bestindex = null;
        for (int i = 0; i < ways.size(); i++) {
            OSMWay way = ways.get(i);
            Object[] res = getDistance(v, way, projection);
            double distance = (Double) res[0];
            if (distance < bestdistance) {
                bestdistance = distance;
                best = way;
                bestindex = (Integer) res[1];

            }
        }
        //return best;
        return new Object[]{best, bestindex};
    }

    /**
     * Returns closest distance found.
     * Return [distance,fromnodeindex]
     */
    public static Object[] getDistance(VectorXZ v, OSMWay way, SceneryProjection projection) {
        double bestdistance = Double.MAX_VALUE;
        int bestindex = -1;
        Coordinate[] coordinates = new Coordinate[way.getNodes().size()];
        for (int i = 0; i < way.getNodes().size(); i++) {
            OSMNode node = way.getNodes().get(i);
            coordinates[i] = JtsUtil.toCoordinate(projection.project(LatLon.fromDegrees(node.lat, node.lon)));
        }
        LineSegment[] segs = JtsUtil.toLineSegments(coordinates);
        for (int i = 0; i < segs.length; i++) {
            LineSegment seg = segs[i];
            double distance = seg.distance(JtsUtil.toCoordinate(v));
            if (distance < bestdistance) {
                bestdistance = distance;
                bestindex = i;
            }
        }
        return new Object[]{new Double(bestdistance), new Integer(bestindex)};
    }

    public static long findHighestId(OSMData osmData) {
        long best = 0;
        for (OSMWay way : osmData.getWays()) {
            if (way.id > best) {
                best = way.id;
            }
        }
        for (OSMNode n : osmData.getNodes()) {
            if (n.id > best) {
                best = n.id;
            }
        }
        return best;

    }

    public static int finfWayIndexById(List<OSMWay> ways, long id) {
        for (int i = 0; i < ways.size(); i++) {
            if (ways.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    public static int finfNodeIndexById(List<OSMNode> nodes, long id) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }
}
