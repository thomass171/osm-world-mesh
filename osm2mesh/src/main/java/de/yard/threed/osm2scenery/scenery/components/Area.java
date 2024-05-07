package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.TextureUtil;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupFinder;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.elevation.EleCoordinate;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshLineSplitCandidate;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.scenery.AreaSeam;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.PolygonMetadata;
import de.yard.threed.osm2scenery.util.SmartPolygon;
import de.yard.threed.osm2world.Material;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Ein beliebiger Polygon als Area.
 * Als TerrainProvider Teil des Mesh, als Overlay z.B. aber nicht.
 */
public class Area extends AbstractArea {
    static Logger logger = Logger.getLogger(Area.class);
    // Ein MeshPolygon ist wegen moeglicher Splits flüchtig. Darum startline.Tja?? Dafuer gibt es jetzt Flag isPartOfMesh
    //public MeshPolygon meshpolygon;
    //public MeshLine[] meshStartLine;

    /**
     * Deprecated wegen MeshPolygon? Brauchts aber fuer Overlays. Ist aber auch nicht sauber.
     */
    @Deprecated
    public Area(Polygon basepolygon, Material material) {
        super(material);
        if (basepolygon != null) {
            uncutcoord = basepolygon.getCoordinates();
            poly = new SmartPolygon(basepolygon, new PolygonMetadata(this));
        }
    }

    /**
     * Ein MeshPolygon ist wegen moeglicher Splits flüchtig. Aber wenn er im Mesh ist, kann er jederzeit dort retrieved werden.
     */
    public Area(/*MeshLine startLine*/MeshPolygon meshpolygon, Material material, boolean dummy) {
        super(material);
        //this.meshpolygon = meshpolygon/*new MeshPolygon[]{meshpolygon};*/;
        isPartOfMesh = true;
    }

    /**
     * 20.4.19 Aus DefaultCutter hier hin.
     * 15.8.19: Nicht mehr selbstaendernd.
     *
     * @param gridbounds
     * @param abstractSceneryFlatObject
     */
    @Override
    public CutResult cut(Geometry gridbounds, SceneryFlatObject abstractSceneryFlatObject, EleConnectorGroupSet elevations) {
        if (isPartOfMesh) {
            throw new RuntimeException("no cut on MeshPolygon");
        }
        if (poly == null) {
            return null;
        }
        /*if (poly.wascut) {
            //23.7.19:Nicht mehrfach
            return null;
        }*/

        //23.7.19: uncutPolygon wird nicht mehr immer gesetzt
        Polygon polygonToCut = poly.uncutPolygon;
        if (polygonToCut == null) {
            polygonToCut = poly.polygon;
        }

        Object[] result = Area.cut(polygonToCut, gridbounds, getParentInfo());
        if (result == null) {
            // completely inside grid. nothing to do.
            return null;
        }
        // poly.polygon = (Polygon[]) result[0];
        Coordinate[] intersectcoorinates = (Coordinate[]) result[1];
        CutResult cutResult = new CutResult((Polygon[]) result[0], intersectcoorinates);
        //poly.wascut = true;


        //31.8.18: Die neuen Coordinates auch einer group zuordnen. Erstmal nur die erstbeste. TODO
        //25.4.19: Der connect der Eles an die Groups erfolgt jetzt ohnehin erst später. Darum wird hier erstmal nichts mehr genacht
        for (Polygon p : cutResult/*poly*/.polygons) {
            for (Coordinate c : p.getCoordinates()) {
                if (false && !EleConnectorGroup.hasGroup(c)) {
                    /*if (getOsmIds().contains(new Long(8033747))) {
                        int z = 99;
                    }*/
                    if (abstractSceneryFlatObject.getEleConnectorGroups().eleconnectorgroups.size() > 0) {
                        abstractSceneryFlatObject.getEleConnectorGroups().eleconnectorgroups.get(0).add(new EleCoordinate(c));
                    } else {
                        logger.warn("no group");
                    }
                }
            }
        }
        return cutResult;
    }

    /**
     * 9.8.19: Im Zuge von Precut und MeshPolygon die cut Logik auf Polygonebene extrahiert.
     */
    public static Object[] cut(Polygon polygonToCut, Geometry gridbounds, String parentinfo) {
        if (gridbounds.contains(polygonToCut)) {
            // completely inside grid. nothing to do.
            return null;
        }
        Geometry cut = null;
        // hier kann es eine "TopologyException: found non-noded intersection between LINESTRING..." geben, wahrscheinlich wenn
        // zwei Lines zu parallel verlaufen oder eine ähnliche Anomalie. Rundungs/Precision Effekte spielen da auch mit rein.
        // z.B. superdetailed ZieverichSued
        Coordinate[] intersectcoorinates = null;
        try {
            cut = polygonToCut.intersection(gridbounds);
            /*wird z.Z. nicht verwendet java.lang.ClassCastException: com.vividsolutions.jts.geom.MultiLineString cannot be cast to com.vividsolutions.jts.geom.LineString
            LineString gridboundary = (LineString) gridbounds.getBoundary();

            LineString polyboundary = (LineString) polygonToCut.getBoundary();
            Geometry result = gridboundary.intersection(polyboundary);
            if (!result.isEmpty()) {
                if (result instanceof MultiPoint) {
                    intersectcoorinates = result.getCoordinates();
                } else {
                    logger.warn("unknown cut intersection type " + result.getClass().getName());
                }
            }*/
        } catch (TopologyException topologyException) {
            // Wenn man die Coordinate kennt, könnte man die wohl etwas verschieben oder sonst was machen. Aber erstmal ignorieren. TODO
            logger.error("intersection exception. area not cut: " + topologyException);
            return null;
        }

        Polygon[] polygon;
        if (cut instanceof MultiPolygon) {
            logger.info("area " + parentinfo + " split by grid bounds");
            /*poly.*/
            polygon = new Polygon[((MultiPolygon) cut).getNumGeometries()];
            for (int i = 0; i < /*poly.*/polygon.length; i++) {
                /*poly.*/
                polygon[i] = (Polygon) cut.getGeometryN(i);
            }
        } else {
            if (cut instanceof Polygon) {
                /*poly.*/
                polygon = new Polygon[]{(Polygon) cut};
            } else {
                logger.error("area  split by grid bounds returned result class " + cut.getClass().getSimpleName());
                return intersectcoorinates;
            }
        }
        return new Object[]{polygon, intersectcoorinates};
    }

    /**
     * 11.4.19: Das ist ziemlich generisch für alle möglichen Polygone. Das könnten Ways z.B. anders machen.
     */
    @Override
    public boolean triangulateAndTexturize(EleConnectorGroupFinder eleConnectorGroupFinder, TerrainMesh tm) {
        if (material == null) {
            //Sonderlocke, texutils brauchen material
            return false;
        }
       /* if (getOsmIds().contains(new Long(8033747))) {
            int z = 99;
        }*/


        vertexData = null;
        MeshPolygon meshPolygon;
        Polygon p;
        if (isPartOfMesh) {
            meshPolygon = getMeshPolygon(tm);
            if (meshPolygon == null) {
                logger.error("no mesh polygon found");
                return false;
            }
            p = meshPolygon.getPolygon();
            if (p == null) {
                logger.error("invalid mesh polygon found");
                return false;
            }
        } else {
            if (poly == null || poly.polygon == null) {
                logger.error("polygon not yet created");
                return false;
            }
            p = poly.polygon;
        }
        if (p.isEmpty()) {
            //kommt wohl schon mal vor
            //error, because it shouldn't be called at all.
            logger.error("triangulateAndTexturize: skipping empty polygon");
        } else {
            VertexData vd;
            int cntr = 0;
            do {
                vd = TextureUtil.triangulateAndTexturizePolygon(p, material);
                if (vd == null) {
                    poly.trifailed = true;
                    vertexData = null;
                    return false;
                }
                // Nur fuer Areas im Mesh muessen neue Coordinates im Mesh hinterlegt werden, fuer anderes (z.B. Overlay) nicht.
                // Und registriert werden muessen die auch; mit EleGroup Zuordnung.
                if (isPartOfMesh) {
                    if (vd.vertices.size() != p.getCoordinates().length - 1) {
                        logger.debug("Triangulation created " + (vd.vertices.size() - p.getCoordinates().length + 1) + " additional vertices for " + getParentInfo() + ". Adding and retrying");
                        for (int i = 0; i < vd.vertices.size(); i++) {
                            Coordinate c = vd.vertices.get(i);
                            int index = JtsUtil.findVertexIndex(c, p.getCoordinates());
                            if (index == -1) {
                                meshPolygon = getMeshPolygon(tm);
                                if (meshPolygon == null) {
                                    logger.error("no mesh polygon");
                                } else {
                                    if (meshPolygon.insert(c, tm) == null) {
                                        logger.error("adding new vertex failed:" + c);
                                    } else {
                                        //logger.debug("added new vertex to mesh:" + c);
                                    }
                                }

                                if (eleConnectorGroupFinder != null/* && vertexRegistry!=null*/) {
                                    //logger.debug("registering new coordinate");
                                    EleConnectorGroup egr = eleConnectorGroupFinder.findGroupForCoordinate(c);
                                    egr.add(new EleCoordinate(c));
                                }
                            }
                        }
                        p = getMeshPolygon(tm).getPolygon();
                        vd = null;
                    }
                }
            }
            while (vd == null && cntr++ < 100);

            //if (vertexData == null) {
            vertexData = vd;
            //} else {
            //  vertexData.add(vd);
            //}
        }


        return true;
    }

    /**
     * 12.6.19: Das soll mal ohne Polygonmetadaten gehn. Erstmal alle Coordinates an die erste Group.
     *
     * @param elevations
     */
    @Override
    public void registerCoordinatesToElegroups(EleConnectorGroupSet elevations, TerrainMesh tm) {
        //for (EleConnectorGroup egr : elevations.eleconnectorgroups) {
        //List<EleConnector> eleconns = md.getEleConnectors(egr.mapNode);
        //egr.addAll(eleconns);
        //}
        EleConnectorGroup egr = elevations.eleconnectorgroups.get(0);
        Polygon p = getPolygon(tm);
        if (p != null) {
            Coordinate[] coors = p.getCoordinates();
            //last Coordinate isType duplicate to getFirst
            for (int i = 0; i < coors.length - 1; i++) {
                registerCoordinateToElegroup(coors[i], egr, tm);
            }
        }
    }

    @Override
    public Polygon getPolygon(TerrainMesh tm) {
        if (parentInfo != null && parentInfo.contains("WayTo")) {
            int h = 9;
        }
        if (isPartOfMesh) {
            //logger.error("should no longer we called. mesh should be used. Naja, viellcith doch, z.B. wegen Overlaps.");
            // Polygon[] p = new Polygon[meshpolygon.length];
            MeshPolygon meshPolygon = getMeshPolygon(tm);
            if (meshPolygon != null) {
                return meshPolygon.getPolygon();
            }
            //fall through. Naja
            /*MeshPolygon[] mp = getMeshPolygon();
            for (int i = 0; i < meshpolygon.length; i++) {
                p[i] = mp[i].getPolygon();
                if (p[i] == null) {
                    //Das ist natürlich der Hammer, aber sonst gibt es noch zu viele NPE
                    return super.getPolygons();
                }
            }
            return p;*/
        }
        return super.getPolygon(tm);
    }

    @Override
    public MeshLine findMeshLineWithCoordinates(Coordinate c0, Coordinate c1, TerrainMesh tm) {
        if (!isPartOfMesh) {
            throw new RuntimeException("invalid usage");
        }
        MeshPolygon meshPolygon = getMeshPolygon(tm);
        //for (int i = 0; i < meshpolygon.length; i++) {
        for (MeshLine result : meshPolygon/*[i]*/.lines) {
            if (result.contains(c0) && result.contains(c1)) {
                return result;
            }
        }
        //}
        return null;
    }

    /*public void setMeshPolygon(/*int index,* / MeshPolygon meshPolygon) {
        if (this.meshpolygon == null) {
            this.meshpolygon = meshpolygon;//new MeshPolygon[poly.polygon.length];
        }
        this.meshpolygon/*[index]* / = meshPolygon;
    }*/


    /**
     * Das ist umfangreich. Der Polygon (bzw. alle Polygone) muss in Segmente aufgeteilt werden,
     * die die Area mit anderen Areas oder Boundary shared.
     * <p>
     * Das ist nicht fuer WayArea! Die hat eine ganz andere Loesung.
     * Die Seams koennen, wenn bekannt, reingesteckt werden. Sonst werden sie hier ermittelt.
     * Hier ermitteln ist zwar schick, aber auch anfällig für Fehler (z.B. Rundungsfehler?) und nicht bedachte Konstellationen. Reinstecken ist zuverlaessiger.
     * Man denke nur an Bridges. Aber die verwenden das hier gar nicht!
     */
    public static void addAreaToTerrainMesh(Area abstractArea, SceneryFlatObject parent, List<AreaSeam> areaSeams, TerrainMesh tm) {

        Polygon polygonOfArea = abstractArea.getPolygon(tm);
        List<MeshLine> existingShares = new ArrayList();
        MeshLineSplitCandidate boundaryintersections;

        if (parent != null && parent.getOsmIdsAsString().contains("87818511")) {
            int h = 9;
        }
        if (abstractArea.parentInfo != null && abstractArea.parentInfo .contains("parent: bridge ramp to outer (-564.33")) {
            int h = 9;
        }
        int cntr = 0;
        LineString lineWithoutBoundary = polygonOfArea.getExteriorRing();
        if (SceneryBuilder.TerrainMeshDebugLog) {
            logger.debug("Adding area to mesh for " + parent.getOsmIdsAsString() + ". area polygon=" + polygonOfArea);
        }
        //die Area kann ja mehrere Boundaries schneiden (z.B. B55B477small), darum als Schleife.
        while ((boundaryintersections = TerrainMesh.findBestSplitCandidate(tm.findBoundaryCommon(lineWithoutBoundary))) != null && cntr++ < 10) {
            //Eine Edge oder Point des Polygon liegt auf der Boundary.
            if (SceneryBuilder.TerrainMeshDebugLog) {
                logger.debug("Splitting mesh boundary line (pointSplit=" + boundaryintersections.isPointSplit() + ") for " + parent.getOsmIdsAsString());
            }
            MeshLine[] splitresult = tm.split(boundaryintersections);
            if (splitresult == null) {
                logger.error("split failed.");
                return;
            }
            if (boundaryintersections.commonsegment == -1) {
                //Sonderfall one common point only. Nur boundary betroffen. Die Area selber nicht. remaining enthaelt ganzen Polygon.
            } else {
                //typische common edge mit split in zwei oder drei Teile.
                MeshLine shared = splitresult[boundaryintersections.commonsegment];
                //21.8.19: Hier schon area setzen erzeugt ein (temporary) invalid mesh und validation scheitert vor next split.
                //shared.left = abstractArea;
                existingShares.add(shared);
            }
            //MeshLine remaining = TerrainMesh.getInstance().registerLine(boundaryintersections.remaining, null, this, false, false);
            //lines.add(new MeshPolygon(splitresult[1], remaining));
            lineWithoutBoundary = JtsUtil.createLineFromCoordinates(boundaryintersections.remaining);

        }
        if (cntr >= 10) {
            logger.error("Not aus");
        }

        //jetzt die remaining lineWithoutBoundary nach shared mit anderen Areas checken. Das ist jetzt nur noch ein LineString, kein Polygon mehr.
        List<LineString> segments = new ArrayList<>();
        segments.add(lineWithoutBoundary);
        //In adjacentmapareas koennen zu viele adjacent drin sein, die gar keine area bilden.
        //Das ermitteln der Seams vorher ist irgendiwe nicht das wahre (baisert nur auf MapNodes, knifflig bei Runway).
        //Es hier aus dem Mesh ermitteln ist zwar möglich, dann muesste aber ein wohl ein Split gemacht werden. Auch aufwändig. Aber mal versuchen.
        if (areaSeams == null) {
            areaSeams = new ArrayList<>();
            MeshLineSplitCandidate commomCandidate = TerrainMesh.findBestSplitCandidate(tm.findCommon(lineWithoutBoundary, false));
            if (commomCandidate != null) {
                if (SceneryBuilder.TerrainMeshDebugLog) {
                    logger.debug("Splitting mesh (non) boundary line (pointSplit=" + commomCandidate.isPointSplit() + ") for " + parent.getOsmIdsAsString());
                }
                int sharedindex = 0;
                MeshLine[] splitresult = tm.split(commomCandidate);
                if (commomCandidate.commonsegment == -1) {
                    logger.error("huch??. not yet handled");
                    return;
                }
                MeshLine shared = splitresult[commomCandidate.commonsegment];

                AreaSeam areaSeam = new AreaSeam(null, null);
                areaSeam.shareCandidate = JtsUtil.createLine(shared/*splitresult[sharedindex]*/.getCoordinates());
                //das ist ja nicht nur ein Candidate, sondern existiert auch schon im Mesh.
                areaSeam.meshLine = shared;//splitresult[sharedindex];
                areaSeams.add(areaSeam);
            }

        }
        for (AreaSeam areaSeam : areaSeams) {
            //AreaSeam areaSeam = adjacentareas.get(adjacentXX);
            //newsegments sind die, die neu im Mesh angelegt werden müssen.
            List<LineString> newsegments = new ArrayList<>();
            for (LineString segment : segments) {
                LineString shareCandidate = /*JtsUtil.createLineFromCoordinates(adjacentmapareas.get(adjacent.maparea));*/                areaSeam.shareCandidate;
                int[] common = JtsUtil.findCommon(segment, shareCandidate);
                if (common != null) {
                    LineString[] splitted = JtsUtil.removeCoordinatesFromLine(lineWithoutBoundary, common);
                    if (splitted.length > 2) {
                        logger.error("unexpected split result");
                        return;
                    }
                    newsegments.add(splitted[0]);
                    MeshLine existingShare = areaSeam.meshLine;

                    if (existingShare == null) {
                        //share Segment neu anlegen
                        existingShare = null;//2.5.24tm.addSegmentToTerrainMesh(shareCandidate, polygonOfArea, abstractArea);

                        areaSeam.meshLine = existingShare;
                        //waere vielleicht besser, in newsegments aufzunehmen statt direkt anzulegen. Naja
                        //newsegments.add(shareCandidate);
                        existingShares.add(existingShare);
                    } else {
                        // share gibts schon. Nur dranhaengen.
                        Boolean areaIsLeft = JtsUtil.isPolygonLeft(shareCandidate, polygonOfArea);
                        if (areaIsLeft == null) {
                            logger.error("doing Kappes");
                            areaIsLeft = false;
                        }
                        if (areaIsLeft) {
                            //2.5.24existingShare.setLeft(abstractArea);
                        } else {
                            //2.5.24existingShare.setRight(abstractArea);
                        }
                        if (existingShare.getLeft() == null || existingShare.getRight() == null) {
                            logger.error("inconsistent?");
                        }
                        existingShares.add(existingShare);
                    }
                    if (splitted.length > 1) {
                        newsegments.add(splitted[1]);
                    }
                } else {
                    newsegments.add(segment);
                }
            }
            segments = newsegments;
        }
      /*2.5.24  try {
            tm.createMeshPolygon(segments, existingShares, polygonOfArea, abstractArea);
        } catch (MeshInconsistencyException e) {
            throw new RuntimeException(e);
        }*/
    }



}
