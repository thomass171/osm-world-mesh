package de.yard.owm.testutils;

import de.yard.owm.services.persistence.PersistedMeshLine;
import de.yard.owm.services.persistence.PersistedMeshNode;
import de.yard.owm.services.persistence.TerrainMeshManager;
import de.yard.threed.core.Degree;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.util.TagHelper;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.OSMWay;
import de.yard.threed.scenery.util.SimpleRoundBodyCalculations;
import de.yard.threed.traffic.EllipsoidCalculations;
import de.yard.threed.traffic.geodesy.GeoCoordinate;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static de.yard.threed.osm2scenery.util.TagHelper.buildTagGroup;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Derived from OsmTestDataBuilder.
 */
public class TestData {

    GridCellBounds gridCellBounds;
    private List<OSMWay> ways = new ArrayList();
    private List<OSMNode> nodes = new ArrayList();
    public OSMData osmData;
    int id = 100;
    public TerrainMesh terrainMesh;

    private TestData(GridCellBounds gridCellBounds, boolean addRoad) {
        this.gridCellBounds = gridCellBounds;

        terrainMesh = TerrainMesh.init(gridCellBounds);

        if (addRoad) {
            createUshapedWay(gridCellBounds.getOrigin(), 0.01);
        }
        //String bounds = "<bounds minlat=\"50.9450000\" minlon=\"6.5944000\" maxlat=\"50.9479000\" maxlon=\"6.5997000\"/>";

        Bound bound = new Bound(gridCellBounds.getRight(), gridCellBounds.getLeft(), gridCellBounds.getTop(), gridCellBounds.getBottom(), "");
        osmData = new OSMData(List.of(bound), nodes, ways, Collections.emptySet());

    }

    public static TestData build2024(TerrainMeshManager manager) {
        double centerLat = (51);
        double centerLon = (7.0);
        double widthInDegrees = 0.1;
        double heightInDegrees = 0.1;
        return build2024(manager, centerLat, centerLon, widthInDegrees, heightInDegrees, 0.01, true);
    }

    public static TestData build2024(TerrainMeshManager manager, double centerLat, double centerLon, double widthInDegrees, double heightInDegrees, double marginInDegrees, boolean addRoad) {

        GridCellBounds gridCellBounds = GridCellBounds.buildFromGeos(
                centerLat + heightInDegrees / 2, centerLat - heightInDegrees / 2,
                centerLon - widthInDegrees / 2, centerLon + widthInDegrees / 2);
        TestData testData = new TestData(gridCellBounds, addRoad);

        TestUtils.addTerrainMeshBoundary(testData.terrainMesh, centerLat, centerLon, widthInDegrees, heightInDegrees, gridCellBounds.getProjection().getBaseProjection(), marginInDegrees);

        manager.persist(testData.terrainMesh);

        assertNotNull(testData.terrainMesh);
        assertEquals(4, testData.terrainMesh.points.size());
        assertEquals(4 + 1, testData.terrainMesh.lines.size());

        return testData;
    }

    /**
     * U-shape
     */
    private OSMWay createUshapedWay(LatLon origin, double lenInDegrees) {
        LatLon latloneast = LatLon.fromDegrees(origin.getLatDeg().getDegree(), origin.getLonDeg().getDegree() + lenInDegrees / 2);
        LatLon latlonwest = LatLon.fromDegrees(origin.getLatDeg().getDegree(), origin.getLonDeg().getDegree() - lenInDegrees / 2);
        LatLon latlonuppereast = LatLon.fromDegrees(origin.getLatDeg().getDegree() + lenInDegrees, latloneast.getLonDeg().getDegree());
        LatLon latlonupperwest = LatLon.fromDegrees(origin.getLatDeg().getDegree() + lenInDegrees, latlonwest.getLonDeg().getDegree());
        List<OSMNode> way = new ArrayList<>();
        way.add(createNode(latlonupperwest));
        way.add(createNode(latlonwest));
        way.add(createNode(latloneast));
        way.add(createNode(latlonuppereast));
        OSMWay osmWay = new OSMWay(buildTagGroup("highway", "primary"), id++, way);
        ways.add(osmWay);
        return osmWay;
    }

    private LatLon add(LatLon from, double distance, Degree heading) {
        //22.12.21 SGGeod sgGeod = OsmUtil.toSGGeod(from);
        //sgGeod = sgGeod.applyCourseDistance(heading, distance);
        //sgGeod = sgGeod.applyCourseDistance(heading, distance);
        //return OsmUtil.toLatLon(sgGeod);
        EllipsoidCalculations rbc = new SimpleRoundBodyCalculations();
        return rbc.applyCourseDistance(from, heading, distance);
    }

    private OSMNode createNode(LatLon latlon) {
        OSMNode osmNode = new OSMNode(latlon.getLatDeg().getDegree(), latlon.getLonDeg().getDegree(), buildTagGroup("highway", "primary"), id++);
        nodes.add(osmNode);
        return osmNode;
    }


}
