package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.OutlineBuilder;
import de.yard.threed.core.Util;
import de.yard.threed.core.Vector2;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.CoordinateList;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.MapDataHelper;
import de.yard.threed.osm2graph.osm.TextureUtil;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupFinder;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.elevation.EleCoordinate;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshPoint;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.CoordinatePair;
import de.yard.threed.osm2scenery.util.PolygonMetadata;
import de.yard.threed.osm2scenery.util.SmartPolygon;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.NamedTexCoordFunction;
import de.yard.threed.osm2world.VectorXZ;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Area that relates to a graph, like road, railway, river
 * 4.4.19: Ein "StandardWay", mit immer paarweise links/rechts Coordinates, auch nach einem cut.
 * Manchmal (SIMPLE_INNER_SINGLE_JUNCTION) auch auch 4 statt nur 2 für eine MapNode.
 * 28.8.19: Es gibt auch closed ways, z.B. circle. Da hat ein Polygon dann Holes. Das führt
 * nur zu Komplikationen. Darum stelle ich für Way von Polygon (bleibt aber erhalten) als Basis auf Coordinate Listen um.
 * 8.9.19: circle und andere closed way werden jetzt aber aufgebrochen.
 */
public class WayArea extends AbstractArea {
    static Logger logger = Logger.getLogger(WayArea.class);
    //Mapping von einer logischen Position auf die dortigen Coordinates.
    //Vorhalten von Indizes macht replace mit add() sehr unuebersichtlich. Lieber Segmentlängen speichern.
    private List<Integer> positionSize = null;
    //only for debugging
    public long osmid;
    //optionales Mapping, wenn der Way auf OSM beruht, was die Regel sein duerfte.
    Map<MapNode, Integer> node2position;
    //die Reihenfolge der Coordinates ist immer Start->End des Way. Wirklich immer?
    //9.9.19 nicht ganze line Listen vorhalten, es koennte splits z.B. durch bridges geben. Nur noch als from/to?
    //private MeshPoint  leftto, rightto;
    //private MeshLine leftfrom=null,rightfrom=null;
    private List<MeshLineData> leftlines = null, rightlines = null;
    //public List<MeshLine> leftlines = null, rightlines = null;
    // die neue wirkliche Representation des Way. Beide muessen immer gleich lang sein.
    // was wird denn daraus bei inner connector mit polygon? Vermutlich gehen diese lines "einfach darüber hinweg". TODO
    private CoordinateList rightline, leftline;

    /**
     * 23.4.19: Wofuer ist material? Darin ist die texcoord function, die speater gebraucht wird.
     *
     * @param material
     * @param rightline
     * @param leftline
     */
    public WayArea(Material material, SmartPolygon poly, List<MapNode> nodelist, CoordinateList rightline, CoordinateList leftline) {
        super(material);
        this.poly = poly;
        this.rightline = rightline;
        this.leftline = leftline;
        //Coordinate[] coors = poly.poly gon/*[0]*/.getCoordinates();
        int len = rightline.size();//(coors.length - 1) / 2;
        positionSize = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            positionSize.add(1);
        }
        if (rightline.size() != leftline.size()) {
            throw new RuntimeException("inconsistent");
        }
        if (nodelist != null) {
            if (nodelist.size() != len) {
                throw new RuntimeException("inconsistent");
            }
            node2position = new HashMap<>();
            for (int i = 0; i < len; i++) {
                node2position.put(nodelist.get(i), i);
            }
        }
        validate();
    }

    /**
     * edges need to be connected!
     * Hier werden auch die EleConnector erstellt, weil hier noch der Kontext der Entstehung bekannt ist. Stimmt doch nicht mehr?
     * Das muss eigentlich mehr ganzheitlich sein. Auch der Gridcell cut muss hier betrachtet werden.
     * Das geht jetzt aber noch nicht, weil erst die Area aus dem Grid gecuttetd wird.
     * Ich machs hier und behalte den Original Polygon fuer den cut.
     * Der Polygon wird CCW.
     * 4.4.19: width und seglist rausgezogen, weil die spaeter auch noch gebraucht werden können. Bleiben aber Parameter wegen static option.
     * 5.4.19: Die Nutzung von outline ist nur bei "StandardWays" möglich, man denke nur an U und V Shapes.
     * 12.4.19: Hier direkt den logischen cut zu machen, ist auch Hölle komplex.
     * 23.5.19: Wird jetzt universell, z.B. auch Decoration genutzt. Deswegen darf nodelist vielleicht auch null sein?
     * 14.6.19: P20: Hier dann doch den cut/clip schon beachten. Dafuer optional start/endPair reingeben. 28.8.19:Das hat sich durch LazyCut erledigt.
     * 28.8.19: Bei closed ways (z.B. roundabout) sind die EndOutlines durch die Art der Berechnung (kein average an den Enden) nicht deckungsgleich. Darum
     * in solchen Fällen das Startpair auch am Ende nutzen.
     */
    public static AbstractArea buildOutlinePolygonFromCenterLine(List<Vector2> centerline, List<MapNode> nodelist, double width, Object parent, Material material) {
        /*if (mapWay.getOsmId() == 26927466) {
            int osmid = 6;
        }*/
        String osmid="?";
        if (parent instanceof SceneryObject){
            osmid=((SceneryObject)parent).getOsmIdsAsString();
        }
        List<Vector2> leftoutline = OutlineBuilder.getOutline(centerline, -width / 2);
        List<Vector2> rightoutline = OutlineBuilder.getOutline(centerline, width / 2);
        //List<MapWaySegment> segs = mapWay.getMapWaySegments();
        Map<Long, List<VectorXZ>> nodemap = null;
        /*PolygonMetadata*/
        PolygonMetadata polygonMetadata = new PolygonMetadata(parent);

        // bauen in CCW
        Coordinate c;
        int segcnt;
        List<Coordinate> cutcoord = new ArrayList<>();
        boolean isClosed = false;
        if (nodelist != null) {
            segcnt = nodelist.size() - 1;
            if (segcnt + 1 != centerline.size()) {
                //13.6.19 kommt bei grossen Tiles schon mal vor
                //throw new RuntimeException("inconsistency");
                logger.error("inconsistency. centerline.size=" + centerline.size() + ",nodelist.size=" + nodelist.size());
                return null;
            }
            //size==0?? TODO wasn das?
            if (nodelist.size() > 0 && nodelist.get(0) == nodelist.get(nodelist.size() - 1)) {
                isClosed = true;
            }
        } else {
            segcnt = centerline.size() - 1;
        }
        Coordinate[] uncutcoord = new Coordinate[(segcnt + 1) * 2 + 1];
        CoordinateList rightline = new CoordinateList();
        //right
        for (int i = 0; i < centerline.size(); i++) {
            //MapWaySegment seg = segs.get(i);
           /* if (i == 0) {
                c = JtsUtil.buildCoordinateFromZ0(rightoutline.get(0));
                uncutcoord[i] = c;
                /*if (JtsUtil.contains(gridbounds,centerline.get(0))){
                    cutcoord.add(c);
                }* /
                polygonMetadata.addPoint(seg.getStartNode(), uncutcoord[0]);
            }*/
            c = JtsUtil.buildCoordinateFromZ0(rightoutline.get(i));
            uncutcoord[i] = c;
            /*if (JtsUtil.contains(gridbounds,centerline.get(i+1))){
                cutcoord.add(c);
            }*/
            polygonMetadata.addPoint((nodelist == null) ? null : nodelist.get(i)/*seg.getEndNode()*/, uncutcoord[i]);
            rightline.add(c);
        }
        //left
        CoordinateList leftline = new CoordinateList();
        for (int i = 0; i < centerline.size(); i++) {
            int rindex = centerline.size() - i - 1;
            //MapWaySegment seg = segs.get(rindex);
           /* if (i == 0) {
                c = JtsUtil.buildCoordinateFromZ0(leftoutline.get(rindex + 1));
                uncutcoord[segs.size() + 1] = c;
                polygonMetadata.addPoint(seg.getEndNode(), uncutcoord[segs.size() + 1]);
            }*/
            c = JtsUtil.buildCoordinateFromZ0(leftoutline.get(rindex));
            int cindex = centerline.size() + i;
            uncutcoord[cindex] = c;
            polygonMetadata.addPoint((nodelist == null) ? null : nodelist.get(rindex)/*seg.getStartNode()*/, uncutcoord[cindex]);
            leftline.add(c);
        }
        leftline = leftline.reverse();
        if (uncutcoord.length < 4) {
            // even possible? Yes, eg. due to precut.
            logger.warn("invalid polygon with uncutcoord.length=" + uncutcoord.length + ". Using empty polygon for way ?");
            return AbstractArea.EMPTYAREA;
        }

        GeometryFactory geometryFactory = new GeometryFactory();
        AbstractArea abstractArea;
        Polygon polygon;
        if (isClosed) {
            //dann mit Hole
            //die beiden Am Ende durch Anfang ersetzen. Verzerrt zwar, jetzt aber most easy.
            /*uncutcoord[centerline.size() - 1] = uncutcoord[0];
            uncutcoord[centerline.size()] = uncutcoord[centerline.size() + centerline.size() - 1];

            CoordinateList rightcoordinateList=new CoordinateList(JtsUtil.sublist(uncutcoord,0,centerline.size() - 1));
            polygon = JtsUtil.createPolygon(rightcoordinateList.coorlist);
            CoordinateList leftcoordinateList=new CoordinateList(JtsUtil.sublist(uncutcoord,centerline.size() ,uncutcoord.length-2));
            Polygon hole = JtsUtil.createPolygon(coordinateList.coorlist);
            polygon = (Polygon) polygon.difference(hole);*/
            rightline.set(rightline.size() - 1, rightline.get(0));
            leftline.set(rightline.size() - 1, leftline.get(0));
            polygon = JtsUtil.createPolygonFromWayOutlines(rightline, leftline);

        } else {
            uncutcoord[centerline.size() + centerline.size()] = uncutcoord[0];
            //polygon = geometryFactory.createPolygon(uncutcoord);
            polygon = JtsUtil.createPolygonFromWayOutlines(rightline, leftline);
        }
        if (polygon != null && polygon.isValid()) {
            abstractArea = new WayArea(material, new SmartPolygon(polygon, polygonMetadata), nodelist, rightline, leftline);

        } else {
            logger.warn("buildOutlinePolygonFromGraph: invalid polygon created for OSM way " + osmid + " with width " + width + ". Using fallback:" + polygon +
                    " from centerline " + JtsUtil.createLine(centerline));
            // Dann als Fallback ueber gemergte Segemnte bauen. 5.4.19: Auch hier kann null kommen! Das ist irgendwie krumm.
            // TODO vermeiden oder anders: Damit wird getPolygonCrossLine() nicht mehr gehen
            abstractArea = new Area(null, material);
            /*flatComponent*/
            if (nodelist == null) {
                // 24.5.19 das ist genauso fragwürdig wie die Fallbacks. Das muss sich alles noch finden.
                logger.warn("no nodelist->no polygon");
                return null;
            }
            abstractArea.poly = MapDataHelper.getOutlinePolygon(nodelist/*mapWay*/, width);

        }
        abstractArea.uncutcoord = uncutcoord;
        //baselineStart = JtsUtil.createLineSegment(uncutcoord[0], uncutcoord[uncutcoord.length - 2]);
        //baselineEnd = JtsUtil.createLineSegment(uncutcoord[segcnt], uncutcoord[segcnt + 1]);

        //P20

        return abstractArea;
    }


    /**
     * Einen Way auf logischer Ebene cutten.
     * Der polygonbasierte Cut ist zwar richtig, für Ways aber ungünstig/unpraktisch, weil er das Texturing an den Enden
     * deutlich kompliziert. Darum mal einen WayCutter verwenden, der auf logischer Wayebene arbeiten. Damit können am Ende zwar Zipfel
     * aus dem Grid rein/rausragen, aber die Texturkoordinaten passen und wenn es das Gegenstück genauso macht. Müssten sich beide
     * gut ineinanderfügen. Für den Background in dem LückenZipfel dürfte es auch kein Problem geben.
     * <p>
     * Es können auch sonst Überhänge entstehen, aber mein Gott, im Nachbarteil sind die doch identisch. Das kann man wohl in Kauf nehmen.
     * <p>
     * Dazu wird der Way an den Gridnodes so abgeschnitten, dass die Outline Coordinates der Gridnotes einfach erhalten bleiben.
     * Der Mapway selber bleibt unverändert. Nur aus dem Polygon werden die Coordinates gelöscht.
     * Damit können die EleConnector auch bleiben wie sie sind.
     * <p>
     * Exotische Fälle, bei denen der Way das Grid mehrfach verlässt und somit zerteilt würde, werden einfach nicht behandelt.
     * Dann läuft der Way halt ausserhalb rum. "Mein Gott". TODO
     * 25.4.19: Mit diesem logischen Cut ist auch gewährleistet, dass es auch nachher ein gültiger Standardway bleibt.
     */
    @Override
    public CutResult cut(Geometry gridbounds, SceneryFlatObject abstractSceneryFlatObject, EleConnectorGroupSet elevations) {
        //13.8.19
        boolean waysMachenSchonImCreateDenLazyCut = true;
        if (waysMachenSchonImCreateDenLazyCut) {
            return null;
        }
        SceneryWayObject sceneryWayObject = (SceneryWayObject) abstractSceneryFlatObject;
        //logger.debug("cut");
        List<MapNode> mapNodes = sceneryWayObject.mapWay.getMapNodes();
        //List<Integer> positiontoremove = new ArrayList<>();
        boolean isinside = false;
        //SmartPolygon poly = sceneryWayObject.getArea().poly;
        List<Coordinate> newleftoutline = new ArrayList<>();
        List<Coordinate> newrightoutline = new ArrayList<>();

        if (elevations == null) {
            throw new RuntimeException("no ele groups");
        }
        if (osmid == 107468171) {
            int h = 9;
        }

        List<Integer> newpositionSize = new ArrayList<>();
        Map<MapNode, Integer> newnode2position = new HashMap<>();

        int position = 0;
        for (int i = 0; i < mapNodes.size(); i++) {
            MapNode mapNode = mapNodes.get(i);
            //10.7.19 if (MapDataHelper.isDummyNode(mapNode) || JtsUtil.contains(gridbounds, toVector2(mapNode.getPos()))) {
            if (mapNode.location != MapNode.Location.OUTSIDEGRID) {
                //TODO wenn gridnode erste node ist
                /*if (MapDataHelper.isDummyNode(mapNode)) {
                    if (isinside) {
                        //logger.debug("leaving gridnode at " + i);
                    } else {
                        //logger.debug("entering gridnode at " + i);
                    }

                }*/
                isinside = true;

                CoordinatePair[] pairs = getMultiplePair(i);
                newpositionSize.add(pairs.length);
                newnode2position.put(mapNode, position);
                for (CoordinatePair p : pairs) {
                    newrightoutline.add(p.getFirst());
                    newleftoutline.add(p.getSecond());
                }
                position++;
            } else {
                isinside = false;
                //positiontoremove.add(i);
            }
        }
        if (newrightoutline.size() == 0) {
            // way komplett ausserhalb
            // poly.poly gon[0] = JtsUtil.GF.createPolygon(new Coordinate[]{});
            empty = true;
            //TODO was returnen?
            return new CutResult(new Polygon[]{JtsUtil.GF.createPolygon(new Coordinate[]{})}, new Coordinate[0]);
        }

        for (int i = newleftoutline.size() - 1; i >= 0; i--) {
            newrightoutline.add(newleftoutline.get(i));
        }
        newrightoutline.add(newrightoutline.get(0));
        /*poly.poly gon[0] =*/
        JtsUtil.createPolygon(newrightoutline);
        positionSize = newpositionSize;
        node2position = newnode2position;
        //TODO: einen cutupdate in SmartPolygon o.ae.

        //Meistens dürfte es Standard Waybleiben. Muss trotzdem geprüft werden.
        //isStandard();
        //TODO was returnen?
        //return new Coordinate[0];
        return new CutResult(new Polygon[]{JtsUtil.createPolygon(newrightoutline)}, new Coordinate[0]);
    }

    /**
     * 11.4.19: Create TriangleStrip.
     */
    @Override
    public boolean triangulateAndTexturize(EleConnectorGroupFinder eleConnectorGroupFinder, TerrainMesh tm) {
        if (material == null) {
            //Sonderlocke, texutils brauchen material
            logger.warn("no material");
            return false;
        }

        if (empty) {
            //error, because it shouldn't be called at all.
            logger.error("triangulation skipped for empty polygon. Shouldn't be called at all.");
            return false;
        }
        /*if (getOsmIds().contains(new Long(8033747))) {
            int z = 99;
        }*/

        vertexData = JtsUtil.createTriangleStripForPolygon(null/*poly.poly gon.getCoordinates()*/, rightline, leftline);
        if (vertexData == null) {
            //already logged.
            //Das ist ja wieder ein Ding ( Zieverich Sued. )
            // Aber super hilft nicht, der wird auch die STRIP_FIT Funktion verwenden, wenn die im Material definiert ist.
            //Mal trifailed setzen, obwohl das ja nicht so ganz stimmt.
            //super.texturize();
            poly.trifailed = true;
            return false;
        }

        vertexData.uvs = TextureUtil.texturizeVertices(vertexData.vertices, material, NamedTexCoordFunction.STRIP_FIT);
        //texturizer = "way Texturizer";
        return true;
    }

    /**
     * 12.6.19: Ging mal ueber polygon metadaten. Geht  jetzt aber direkter.
     *
     * @param elevations
     */
    @Override
    public void registerCoordinatesToElegroups(EleConnectorGroupSet elevations, TerrainMesh tm) {
        /*if (mapWay.getOsmId() == 173191603) {
            int h = 8;
        }
        for (int i = 0; i < mapWay.getMapNodes().size(); i++) {
            MapNode node = mapWay.getMapNodes().get(i);
            //SceneryNodeObject wayconnector = wayconnectors.get(node.getOsmId());
            //if (wayconnector==null) {
            // inner node


            List<EleConnector> coors = flatComponent.poly.polyg onMetadata.getEleConnectors(node);
            if (coors != null ) {
                //EleConnectorGroup egr = new EleConnectorGroup(node, coors);
                //areaElevation.eleconnectorgroups.add(egr);
                EleConnectorGroup.elegroups.get(node.getOsmId()).addAll(coors);
            } else {
                logger.warn("inconsistent coordinate reference for osmid " + node.getOsmId());
            }

        }*/

        //Durch cut kann ein Teil des Polygons ausserhalb liegen. Darum greift die Plausi nicht.
        /*if (2 * elevations.eleconnectorgroups.size() != poly.poly gon[0].getCoordinates().length - 1) {
            logger.error("inconsistent way: " + elevations.eleconnectorgroups.size() + " elegroups do not fit way with " + poly.poly gon[0].getCoordinates().length + " coordinates");
        }*/

        //ob die Logik immer greift? Auch bei inside/outside mix?
        //16.7.19: Hat sich mittlerweile ja viel geaendert. Koennte schon sein.

        if (!SceneryBuilder.FTR_SMARTBG) {
            for (int i = 0; i < elevations.eleconnectorgroups.size(); i++) {
                EleConnectorGroup egr = elevations.eleconnectorgroups.get(i);
                //int[] index = position2pairindex.get(i/*egr*/);
                Integer position = getPosition(egr.mapNode);
                if (position != null) {
                    CoordinatePair[] pairs = getMultiplePair(position);
                    if (pairs != null) {
                        for (CoordinatePair pair : pairs) {
                            //Pair<Coordinate, Coordinate> pair = getPair(index[0]);
                            egr.add(new EleCoordinate(pair.getFirst()));
                            egr.add(new EleCoordinate(pair.getSecond()));
                        }
                    }
                }
            }
        } else {
            //wurde schon beim register mitgemacht. Nein, das ist wegen evtl. split zu frueh.


            /*kein Zusammenhang! if (elevations.size() != getSegmentCount() + 1) {
                throw new RuntimeException("inconsistent elegroups?");
            }*/
            // ueber Segments gehts nicht. Ueber die Nodes. Das ist ja das gleiche wie oben!
            // Eigentlich müssten die Area die Mesh Coordinates verwenden, aber die sind doch wohl identisch?
            for (int i = 0; i < elevations.eleconnectorgroups.size(); i++) {
                EleConnectorGroup egr = elevations.eleconnectorgroups.get(i);
                //int[] index = position2pairindex.get(i/*egr*/);
                Integer position = getPosition(egr.mapNode);
                if (position != null) {
                    CoordinatePair[] pairs = getMultiplePair(position);
                    if (pairs != null) {
                        for (CoordinatePair pair : pairs) {
                            //Pair<Coordinate, Coordinate> pair = getPair(index[0]);
                            //egr.add(new EleCoordinate(pair.getFirst()));
                            //egr.add(new EleCoordinate(pair.getSecond()));
                            registerCoordinateToElegroup(pair.getFirst(), egr, tm);
                            registerCoordinateToElegroup(pair.getSecond(), egr, tm);
                        }
                    }
                }
            }
            //Gegenprobe. left/right lines null sind mögliche Folgefehler.
            // TODO ways die durch Triangulation entstehen sind ein Problem. 9.9.19???
            if (isPartOfMesh) {
                List<MeshLine> leftlines = getLeftLines(tm), rightlines = getRightLines(tm);
                if (leftlines != null && rightlines != null) {
                    for (MeshLine l : leftlines) {
                        for (Coordinate c : l.getCoordinates()) {
                            if (EleConnectorGroup.getGroup(c) == null) {
                                logger.error("way area still has unregistered coordinate " + c + " in line " + l);
                            }
                        }
                    }
                    for (MeshLine l : rightlines) {
                        for (Coordinate c : l.getCoordinates()) {
                            if (EleConnectorGroup.getGroup(c) == null) {
                                logger.error("way area still has unregistered coordinate " + c + " in line " + l);
                            }
                        }
                    }
                } else {
                    logger.error("leftlines or rightlines isType null. unhandled bridge?");
                }
            } else {
                //koennte eine Bridge sein. Ist zwar nicht im TerrainMesh, braucht aber trotzdem eine Elevation (ähnliches Problem wie Decoration Overlays).
                //aehnlich machen: hier nichts registrieren und dann alles im calculateElevation.
            }
        }
    }


    @Override
    public MeshLine findMeshLineWithCoordinates(Coordinate c0, Coordinate c1, TerrainMesh tm) {
        Util.notyet();
        return null;
    }


    private int[] getPairIndex(int logicalindex) {
        int position = 0;

        if (logicalindex >= positionSize.size()) {
            logger.error("invalid logicalindex " + logicalindex);
            return null;
        }
        for (int i = 0; i < logicalindex; i++) {
            position += positionSize.get(i);
        }
        switch (positionSize.get(logicalindex)) {
            case 1:
                return new int[]{position};
            case 2:
                return new int[]{position, position + 1};
            default:
                Util.notyet();
                return null;
        }
    }

    /**
     * Find first or last logical(!) index of a segment.
     *
     * @param segment
     * @param findStartIndex
     * @return
     */
    public int getSegmentIndex(int segment, boolean findStartIndex) {
        //int position = 0;

        /*if (logicalindex >= positionSize.size()) {
            logger.error("invalid logicalindex " + logicalindex);
            return null;
        }*/
        if (findStartIndex && segment == 0) {
            return 0;
        }
        int currsegment = 0;
        for (int i = 0; i < positionSize.size(); i++) {
            boolean wasDouble = false;
            //auch an start/end kann ein Doppel liegen. Das ist dann aber kein Segmentwechsel.
            if (i > 0 && positionSize.get(i) > 1) {
                if (!findStartIndex && currsegment == segment) {
                    return i;
                }
                currsegment++;
                wasDouble = true;
            }
            if (findStartIndex && currsegment == segment) {
                //22.8.19:
                //return i;
                return i;//((wasDouble)?1:0);
            }
        }
        if (!findStartIndex && currsegment == segment) {
            return positionSize.size() - 1;
        }

        logger.warn("Segment " + segment + " not found");
        return -1;
    }

    /**
     * Return pair for logical position.
     * getFirst ist immer right, getSecond left; jeweils ab start.
     * Returns null for MultiplePair locations.
     *
     * @param index
     * @return
     */
    public CoordinatePair getPair(int index) {
        TerrainMesh tm = null;
        if (isEmpty(tm)) {
            //TODO sollte uebrhaupt kein Way sein, oder?
            logger.warn("should not be called on empty ways");
            return null;
        }
        //Coordinate[] coors = poly.poly gon.getCoordinates();
        int[] ii = getPairIndex(index);
        if (ii == null) {
            logger.error("invalid position " + index + ". length=" + getLength());
            return null;
        }
        if (ii.length > 1) {
            return null;
        }
        try {
            //CoordinatePair p = new CoordinatePair(coors[ii[0]], coors[coors.length - 2 - ii[0]]);
            CoordinatePair p = new CoordinatePair(rightline.get(ii[0]), leftline.get(ii[0]));
            return p;
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("invalid index,e");
            return null;
        }
    }

    /**
     * see below.
     */
    public CoordinatePair[] getPairsOfSegment(int segment) {
        TerrainMesh tm = null;
        Object[] os = getPairsOfSegment(segment, false, tm);
        CoordinatePair[] result = new CoordinatePair[os.length];
        for (int i = 0; i < os.length; i++) {
            result[i] = (CoordinatePair) os[i];
        }
        return result;
    }

    /**
     * Returns list of pairs for one segment.
     * Das koennen auch mehr als zwei sein, z.B. durch SIMPLE_SINGLE_JUNCTION. Wieso deswegen? Ganz allgemein kann ein Segment doch beliebig viele Pairs haben.
     * Aber am Anfang und Ende sind Sonderfaelle, weil da ja keine Segmentgrenze ist.
     *
     * @return
     */
    private Object[] getPairsOfSegment(int segment, boolean meshpoints, TerrainMesh tm) {
        int start = getSegmentIndex(segment, true);
        int end = getSegmentIndex(segment, false);
        if (start == -1 || end == -1) {
            //already logged
            return null;
        }

        List<CoordinatePair> result = new ArrayList<>();
        CoordinatePair[] pairs = getMultiplePair(start);
        if (pairs.length > 1) {
            if (segment == 0) {
                //Sonderall zwei am Start
                result.add(pairs[0]);
            }
            result.add(pairs[1]);
        } else {
            result.add(pairs[0]);
        }
        for (int i = start + 1; i < end; i++) {
            CoordinatePair pair = getPair(i);
            if (pair == null) {
                logger.warn("unexpected pair null");
            }
            result.add(pair);
        }
        pairs = getMultiplePair(end);
        if (pairs.length > 1) {
            if (segment == getSegmentCount() - 1) {
                //Sonderall zwei am Ende
                result.add(pairs[0]);
                result.add(pairs[1]);
            } else {
                //23.8.19: 1->0
                result.add(pairs[0]);
            }
        } else {
            result.add(pairs[0]);
        }

        return result.toArray(new CoordinatePair[0]);
    }

    /**
     * Geht auch fuer non multiple pairs.
     * 27.3.24 TerrainMesh really needed?
     * @param index
     * @return
     */
    public CoordinatePair[] getMultiplePair(int index) {
        TerrainMesh tm = null;
        //27.3.24 TerrainMesh isn't yet available here! Is it?

        if (isEmpty(tm)) {
            //TODO sollte uebrhaupt kein Way sein, oder?
            logger.warn("should not be called on empty ways");
            return null;
        }
        //Coordinate[] coors = poly.poly gon.getCoordinates();
        int[] ii = getPairIndex(index);
        if (ii == null) {
            logger.error("invalid position " + index + ". length=" + getLength());
            return null;
        }

        try {
            CoordinatePair[] p = new CoordinatePair[ii.length];
            for (int i = 0; i < ii.length; i++) {
                //p[i] = new CoordinatePair(coors[ii[i]], coors[coors.length - 2 - ii[i]]);
                p[i] = new CoordinatePair(rightline.get(ii[i]), leftline.get(ii[i]));
            }
            return p;
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("invalid index,e");
            return null;
        }
    }

    /**
     * Kann seit SIMPLE_INNER_SINGLE_JUNCTION auch mehrere liefern
     *
     * @return
     */
    public CoordinatePair[] getStartPair(TerrainMesh tm) {
        CoordinatePair[] p = getMultiplePair(0);
        return p;
    }

    /**
     * Kann seit SIMPLE_INNER_SINGLE_JUNCTION auch mehrere liefern
     *
     * @return
     */
    public CoordinatePair[] getEndPair() {
        TerrainMesh tm = null;
        CoordinatePair[] p = getMultiplePair(getLength() - 1);
        return p;
    }

    /**
     * Returns the "logical length"!
     */
    public int getLength() {
        int len = positionSize.size();
        return len;
    }

    public int getRawLength() {
        //Coordinate[] coors = poly.pol ygon.getCoordinates();
        //int len = (coors.length - 1) / 2;
        return rightline.size();
    }

    public Integer getPosition(MapNode mapNode) {
        return node2position.get(mapNode);
    }

    public List<Coordinate> getLeftOutline() {
        /*Coordinate[] coors = poly.poly gon.getCoordinates();
        int len = getRawLength();
        List<Coordinate> l = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            l.add(coors[coors.length - 2 - i]);
        }
        return l;*/
        return leftline.coorlist;
    }

    /**
     * 5.8.19: Es gibt jetzt MeshLine
     *
     * @return
     */
    @Deprecated
    public List<Coordinate> getRightOutline() {
        /*Coordinate[] coors = poly.poly gon.getCoordinates();
        int len = getRawLength();
        List<Coordinate> l = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            l.add(coors[i]);
        }
        return l;*/
        return rightline.coorlist;
    }

    public void replaceStart(CoordinatePair[] pair) {
        replace(0, pair);
    }

    public void replaceEnd(CoordinatePair[] pair) {
        replace(getLength() - 1, pair);
    }

    /**
     * Replace coordinate pair with two other.
     * Das geht nur, wenn dabei ein gültiger Way Polygon bestehen bleibt.
     * Returns false in the case of failure.
     */
    public boolean replace(int index, CoordinatePair[] p) {
        int[] ii = getPairIndex(index);
        if (ii == null) {
            logger.error("invalid position " + index + ". length=" + getLength());
        }
        if (ii.length > 1) {
            Util.notyet();
        }
        /*if (!poly.replace(ii[0], p[0])) {
            return false;
        }*/
        CoordinateList tl = leftline.clone();
        CoordinateList tr = rightline.clone();
        tr.set(ii[0], p[0].right());
        tl.set(ii[0], p[0].left());
        /*if (!poly.add(ii[0] + 1, p[1])) {
            // replace already succeeded. Hmm?
            return false;
        }*/
        tr.add(ii[0] + 1, p[1].right());
        tl.add(ii[0] + 1, p[1].left());

        Polygon polygon = JtsUtil.createPolygonFromWayOutlines(tr, tl);
        if (polygon == null || !polygon.isValid()) {
            return false;
        }
        poly.polygon = polygon;
        rightline = tr;
        leftline = tl;
        positionSize.set(index, 2);
        validate();
        return true;
    }

    /**
     * kann multiple replace (wegen closed way).
     * TODO: Was ist denn, wenn an der position multiple sind?
     */
    public boolean replace(int[] pindex, CoordinatePair p) {
        CoordinateList tl = leftline.clone();
        CoordinateList tr = rightline.clone();
        for (int index : pindex) {
            int[] ii = getPairIndex(index);
            if (ii == null) {
                logger.error("invalid position " + index + ". length=" + getLength());
            }
            if (ii.length > 1) {
                Util.notyet();
            }
        /*if (!poly.replace(ii[0], p)) {
            return false;
        }*/
            tr.set(ii[0], p.right());
            tl.set(ii[0], p.left());
        }
        Polygon polygon = JtsUtil.createPolygonFromWayOutlines(tr, tl);
        if (polygon == null || !polygon.isValid()) {
            return false;
        }
        poly.polygon = polygon;
        rightline = tr;
        leftline = tl;
        validate();
        return true;
    }

    public boolean replaceStart(CoordinatePair pair) {
        return replace(new int[]{0}, pair);
    }

    public boolean replaceEnd(CoordinatePair pair) {
        return replace(new int[]{getLength() - 1}, pair);
    }

    public boolean replaceStartEnd(CoordinatePair pair) {
        return replace(new int[]{0, getLength() - 1}, pair);
    }

    /**
     * Am Anfang etwas kürzen.
     *
     * @return
     */
    public CoordinatePair shiftStart(double offset) {
        return shift(0, offset);
    }

    /**
     * Am Ende etwas kürzen.
     * offset must be negative.
     *
     * @return
     */
    public CoordinatePair shiftEnd(double offset) {
        return shift(getLength() - 1, offset);
    }

    /**
     * Not self modifying but just returning the values.
     */
    public CoordinatePair shift(int index, double offset) {
        TerrainMesh tm = null;
        CoordinatePair[] mp0 = getMultiplePair(index);
        CoordinatePair[] mp1 = getMultiplePair(index + ((offset < 0) ? -1 : 1));
        if (mp0 == null || mp1 == null) {
            logger.error("cannot shift");
            return null;
        }
        CoordinatePair p0, p1;
        if (offset < 0) {
            p0 = mp0[0];
            p1 = mp1[mp1.length - 1];
            offset = -offset;
        } else {
            p0 = mp0[mp0.length - 1];
            p1 = mp1[0];
        }
        Vector2 direction = JtsUtil.getDirection(p0.getFirst(), p1.getFirst());
        p0.setFirst(JtsUtil.add(p0.getFirst(), direction.multiply(offset)));
        direction = JtsUtil.getDirection(p0.getSecond(), p1.getSecond());
        p0.setSecond(JtsUtil.add(p0.getSecond(), direction.multiply(offset)));
        //replace(index, p0);
        return p0;
    }

    /**
     * Not self modifying but just returning the values.
     */
    public CoordinatePair reduce(int index, double offset, TerrainMesh tm) {
        CoordinatePair[] mp0 = getMultiplePair(index);
        //CoordinatePair[] mp1 = getMultiplePair(index + ((offset < 0) ? -1 : 1));
        if (mp0 == null) {
            logger.error("cannot reduce");
            return null;
        }
        if (mp0.length != 1 || offset < 0) {
            logger.error("reduce:not yet");
            return null;
        }
        CoordinatePair p0, p1;
        Vector2 from2dir = JtsUtil.getVector2(mp0[0].right(), mp0[0].left());
        Vector2 dir = from2dir.normalize().multiply(offset);

        p0 = new CoordinatePair(JtsUtil.add(mp0[0].right(), dir), JtsUtil.add(mp0[0].left(), dir.negate()));

        return p0;
    }

    public int getSegmentCount() {
        int segs = 1;
        //dont't consider start and end
        for (int i = 1; i < positionSize.size() - 1; i++) {
            if (positionSize.get(i) > 1) {
                segs++;
            }
        }
        return segs;
    }

    public boolean isClosed() {
        return rightline.get(0).equals2D(rightline.get(rightline.size() - 1));
    }

    @Override
    public Polygon getPolygon(TerrainMesh tm) {
        Polygon polygon = JtsUtil.createPolygonFromWayOutlines(rightline, leftline);
        if (polygon == null || !polygon.isValid()) {
            logger.error("inconsistemt?");
            return null;
        }
        return polygon;
    }

    public boolean isOuterCoordinate(Coordinate coordinate) {
        if (rightline.get(0).equals2D(coordinate)) {
            return true;
        }
        if (leftline.get(0).equals2D(coordinate)) {
            return true;
        }
        if (rightline.get(rightline.size() - 1).equals2D(coordinate)) {
            return true;
        }
        if (leftline.get(leftline.size() - 1).equals2D(coordinate)) {
            return true;
        }
        return false;
    }

    public void validate() {
        isValid();
    }

    public boolean isValid() {
        if (parentInfo != null && parentInfo.contains("107468171")) {
            int h = 9;
        }
        if (rightline.size() != leftline.size()) {
            logger.error("rightline.size(" + rightline.size() + ") != leftline.size(" + leftline.size() + ")");
            return false;
        }
        return true;
    }

    public void initLeftRightLines() {
        leftlines = new ArrayList<>();
        rightlines = new ArrayList<>();


    }

    public List<MeshLine> getLeftLines(TerrainMesh tm) {
        if (leftlines == null) {
            return null;
        }
        List<MeshLine> lines = new ArrayList<>();
        for (MeshLineData l : leftlines) {
            lines.addAll(l.getLines(tm));
        }
        return lines;
    }

    public List<MeshLine> getRightLines(TerrainMesh tm) {
        if (rightlines == null) {
            return null;
        }
        List<MeshLine> lines = new ArrayList<>();
        for (MeshLineData l : rightlines) {
            lines.addAll(l.getLines(tm));
        }
        return lines;
    }

   /* public List<Pair<MeshPoint, MeshPoint>> getLeftlines() {
        return leftlines;
    }

    public List<Pair<MeshPoint, MeshPoint>> getRightlines() {
        return rightlines;
    }*/

    public void addLeftline(MeshLine line) {
        leftlines.add(new MeshLineData(line, this, true));
    }

    public void addRightline(MeshLine line) {
        rightlines.add(new MeshLineData(line, this, false));
    }

   /* public MeshLine getLeftFrom() {
        return leftfrom;
    }

    public void setLeftFrom(MeshLine from) {
        leftfrom = from;
    }

    public void setLeftTo(MeshPoint from) {
        leftto = from;
    }

    public MeshLine getRightFrom() {
        return rightfrom;
    }

    public void setRightFrom(MeshLine from) {
        rightfrom = from;
    }

    public void setRightTo(MeshPoint from) {
        rightto = from;
    }*/
}

/**
 * Metadaten, falls eine line gesplittet wird.
 */
class MeshLineData {
    MeshLine meshLine;
    MeshPoint from, to;
    WayArea wayArea;
    boolean left;

    MeshLineData(MeshLine meshLine, WayArea wayArea, boolean left) {
        this.meshLine = meshLine;
        from = meshLine.getFrom();
        to = meshLine.getTo();
        this.wayArea = wayArea;
        this.left = left;
    }

    List<MeshLine> getLines(TerrainMesh tm) {
        List<MeshLine> lines = new ArrayList<>();
        lines.add(meshLine);
        if (meshLine.getTo() == to) {
            //no split
            return lines;
        }
        //left negieren, weil es hier der Indicator für die line ist, im terrain aber indicator fuer die area.
        return tm.findLineOfWay(meshLine, to, wayArea, !left);

    }
}
