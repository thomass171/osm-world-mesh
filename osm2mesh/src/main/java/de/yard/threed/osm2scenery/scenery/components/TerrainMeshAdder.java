package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.MeshServiceFacade;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import de.yard.threed.osm2scenery.polygon20.MeshPolygonType;
import de.yard.threed.osm2scenery.scenery.OsmProcessException;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2world.MapWaySegmentAtConnector;

import java.util.List;
import java.util.Map;

public abstract class TerrainMeshAdder implements SceneryObjectComponent {
    // 19.8.26 Moved here from TerrainMesh
    String meshName;
    public MeshServiceFacade meshService;
    public SceneryProjection projection;
    // Now disabled for 2026 DB persist
    // public void addToTerrainMesh(AbstractArea[] areas, TerrainMesh tm) throws OsmProcessException, MeshInconsistencyException;
    //void addToTerrainMesh(MeshServiceFacade meshService) throws OsmProcessException, MeshInconsistencyException;

    public TerrainMeshAdder(String meshName, MeshServiceFacade meshService, SceneryProjection projection) {
        this.meshName = meshName;
        this.meshService = meshService;
        this.projection = projection;
    }

    /**
     * 5.4.24: Three additional register instead of registerLine, which isn't ready for polygons.
     * lanes is used leter to detect ways for triangulateAndTexturize
     * connector might be null, otherwise the connecting part must have been created before.
     * 6.4.24: Well, maybe its easier to keep existing registerLine for a while?
     * 24.8.26: No longer returns a value to avoid confusion.
     * @return
     */
    public void registerWay(long osmWayId, Pair<Coordinate, Coordinate> fromConnector, List<Coordinate> leftLine, List<Coordinate> rightLine, Pair<Coordinate, Coordinate> toConnector, int lanes) throws MeshInconsistencyException {

        meshService.addWay(meshName, osmWayId, JtsUtil.unproject(fromConnector, projection), JtsUtil.unproject(leftLine, projection), JtsUtil.unproject(rightLine, projection), JtsUtil.unproject(toConnector, projection), lanes);
        //return null;
        /*12.2.26 moved to service Polygon polygon = JtsUtil.createPolygonFromWayOutlines(new CoordinateList(rightLine), new CoordinateList(leftLine));

        List<MeshLine> linesToDelete = new ArrayList<>();
        for (MeshLine line : lines) {
            if (crosses(line, polygon)) {
                // intersection found. If it is not a BG line, this is a failure. Either the way overlaps some existing area or the (sub)mesh is too small.
                if (!MeshLine.isBackgroundTriangulation(line.getType())) {
                    throw new OsmProcessException("polygon crosses unremovable line " + line);
                }
                linesToDelete.add(line);
            }
        }
        linesToDelete.forEach(l -> deleteLineFromMesh(l));

        AbstractArea/*SceneryFlatObject* / leftArea = null;
        AbstractArea/*SceneryFlatObject* / rightArea = null;

        MeshArea meshArea = addArea();
        meshArea.setOsmWay(osmWay);

        List<MeshLine> newLines = new ArrayList<>();
        MeshNode n = meshFactoryInstance.buildMeshNode(leftLine.get(0));
        points.add(n);
        MeshLine l;
        for (int i = 1; i < leftLine.size(); i++) {
            l = addLine(n, leftLine.get(i));
            n = l.getTo();
            newLines.add(l);
            l.setRight(meshArea);
        }
        l = addLine(n, rightLine.get(rightLine.size() - 1));
        n = l.getTo();
        newLines.add(l);

        for (int i = rightLine.size() - 2; i >= 0; i--) {
            l = addLine(n, rightLine.get(i));
            n = l.getTo();
            newLines.add(l);
            // polygon continues, so 'right' is correct.
            l.setRight(meshArea);
        }

        // lines.addAll(registerLineNonPreDB(JtsUtil.toList(leftLine.get(0), rightLine.get(0)), null, null));
        l = meshFactoryInstance.buildMeshLine(n, newLines.get(0).getFrom());
        newLines.add(l);
        lines.addAll(newLines);

        MeshPolygon newArea = null;
        try {
            newArea = new MeshPolygon(newLines);

            // the new area has no connection to the mesh yet.
            MeshLine someLine = findSomeEnclosingLine(newArea);
            if (someLine == null) {
                throw new OsmProcessException("no enclosing line");
            }
            MeshPolygon enclosingPolygon = traversePolygon(someLine, null, true);
            if (enclosingPolygon == null) {
                throw new OsmProcessException("no enclosingPolygon");
            }

            // connect ot background mesh
            MeshLine startConnectingLine = newLines.get(newLines.size() - 1);
            connectAreaNodeToPolygon(newLines.get(0), newLines.get(0).getFrom(), newArea, enclosingPolygon);
            MeshLine endConnectingLine = newLines.get(leftLine.size() - 1);
            connectAreaNodeToPolygon(endConnectingLine, endConnectingLine.getFrom(), newArea, enclosingPolygon);
            connectAreaNodeToPolygon(endConnectingLine, endConnectingLine.getTo(), newArea, enclosingPolygon);
            connectAreaNodeToPolygon(startConnectingLine, startConnectingLine.getFrom(), newArea, enclosingPolygon);
            // the caller should call validate before persist. Don't remember the reason.
            return newArea;
        } catch (MeshInconsistencyException e) {
            throw new OsmProcessException(e);
        }*/
    }

    /**
     * Analog to registerWay. "pair.second" is attached way id
     * 24.8.26: No longer returns a value to avoid confusion.
     * 1.9.26 TODO should expect Coordinate like registerWay and registerArea, not GeoCoordinate. But for now it is easier to keep it like this.
     */
    public void/*MeshWayConnector*/ registerConnector(long osmNodeId, List<Pair<GeoCoordinate, Long>> line, Map<MapWaySegmentAtConnector, Pair<Integer, Integer>> wayAttachPoints) throws MeshInconsistencyException {
        TerrainMesh tm = meshService.addConnector(meshName, osmNodeId, line, wayAttachPoints);
        //24.8.26 return tm.getConnector(osmNodeId);

    }

    /**
     * Analog to registerWay.
     *
     */
    public void/*MeshWayConnector*/ registerArea(long osmNodeId, MeshPolygonType type, List<Coordinate> coordinates) throws MeshInconsistencyException {
        TerrainMesh tm = meshService.addArea(meshName, osmNodeId, type, JtsUtil.unproject(coordinates, projection));

    }
}
