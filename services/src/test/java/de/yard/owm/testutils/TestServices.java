package de.yard.owm.testutils;

import de.yard.owm.services.persistence.*;
import de.yard.threed.TestUtil;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.Pair;
import de.yard.threed.osm2mesh.testutils.ExpectedMeshNodePair;
import de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon;
import de.yard.threed.osm2mesh.testutils.ValidatorServiceFacade;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestConfiguration
public class TestServices implements ValidatorServiceFacade {

    @Autowired
    private MeshNodeRepository meshNodeRepository;

    @Autowired
    private MeshLineRepository meshLineRepository;

    @Autowired
    private MeshAreaRepository meshAreaRepository;

    @Autowired
    private OsmWayRepository osmWayRepository;

    @Autowired
    private OsmNodeRepository osmNodeRepository;

    @Autowired
    private OsmWayNodeRepository osmWayNodeRepository;

    @Autowired
    private MeshPolygonRepository meshPolygonRepository;

    @Autowired
    private MeshPolygonNodeRepository meshPolygonNodeRepository;

    @Autowired
    private MeshRepository meshRepository;

    @Autowired
    private MeshFailureRepository meshFailureRepository;

    public void cleanup() {
        meshFailureRepository.deleteAll();
        meshPolygonNodeRepository.deleteAll();
        meshPolygonRepository.deleteAll();
        meshLineRepository.deleteAll();
        meshAreaRepository.deleteAll();
        meshNodeRepository.deleteAll();
        meshRepository.deleteAll();
        //osmWayRepository.findAll().forEach(w->w.g);deleteAll();
        osmWayNodeRepository.deleteAll();
        osmWayRepository.deleteAll();
        osmNodeRepository.deleteAll();

    }

    /**
     * Transactional to avoid LazyInitializationException
     */
    @Transactional(readOnly = true)
    public PersistedOsmWay loadOsmWay() {
        PersistedOsmWay osmWay = osmWayRepository.findAll().get(0);
        /*24.3.26 osmWay.getOsmWayNodes().size();*/
        return osmWay;
    }

    /**
     * 11.7.26: No longer require expectedMeshPolygons to cover the full mesh. Only
     * validate the expected ones. So the mesh can contain more polygons than expected.
     */
    @Override
    public void validateMesh(TerrainMesh terrainMesh, ExpectedMeshPolygon... expectedMeshPolygons) throws MeshInconsistencyException {

       // assertEquals(expectedMeshPolygons.length, terrainMesh.polygons.size(), "polygons");
       // assertEquals(expectedMeshPolygons.length, meshPolygonRepository.count());

        int totalNodeCnt = 0;
        for (int i = 0; i < expectedMeshPolygons.length; i++) {
            ExpectedMeshPolygon ep = expectedMeshPolygons[i];

            if (ep.osmId==null){
                int h=9;
            }
            PersistedMeshPolygon p = (PersistedMeshPolygon) terrainMesh.findPolygonsByOsmId(ep.osmId);
            if (p==null){
                fail("No polygon for osmid found:"+ep.osmId);
            }
            if (ep.osmId == null) {
                assertNull(p.getOsmId());
            } else {
                assertEquals(ep.osmId, p.getOsmId(), "polygon osmid at i=" + i);
            }
            TestUtil.validateMeshPolygon(ep, p);


            totalNodeCnt += ep.nodes;
        }

        // nodes are not yet shared, so easy to count.
        //10.8.26 no longer validate this, too unspecific assertEquals(totalNodeCnt, meshNodeRepository.count(), "nodes in DB");
        //10.8.26 no longer validate this, too unspecific assertEquals(totalNodeCnt, terrainMesh.points.size(), "points");

        //TODO assertEquals(4 + 2 * 5 + 1, meshPolygonNodeRepository.count());
    }

}
