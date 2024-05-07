package de.yard.owm.testutils;

import de.yard.owm.services.persistence.MeshAreaRepository;
import de.yard.owm.services.persistence.MeshLineRepository;
import de.yard.owm.services.persistence.MeshNodeRepository;
import de.yard.owm.services.persistence.OsmNodeRepository;
import de.yard.owm.services.persistence.OsmWayNodeRepository;
import de.yard.owm.services.persistence.OsmWayRepository;
import de.yard.owm.services.persistence.PersistedOsmWay;
import de.yard.owm.services.persistence.TerrainMeshManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestConfiguration
public class TestServices {

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

    public void cleanup() {
        meshLineRepository.deleteAll();
        meshAreaRepository.deleteAll();
        meshNodeRepository.deleteAll();
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
        osmWay.getOsmWayNodes().size();
        return osmWay;
    }
}
