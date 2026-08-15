package de.yard.owm.services.persistence;

import de.yard.threed.osm2scenery.polygon20.MeshArea;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.polygon20.OsmWay;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@EqualsAndHashCode(of = "id")
@Table(name = "mesharea")
public class PersistedMeshArea implements MeshArea {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mesharea_id_generator")
    @SequenceGenerator(name = "mesharea_id_generator", sequenceName = "mesharea_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "osmway_id", nullable = true)
    private PersistedOsmWay osmWay;

    @Override
    public void setOsmWay(OsmWay osmWay) {
        this.osmWay = (PersistedOsmWay) osmWay;
    }

    @Override
    public OsmWay getOsmWay() {
        return null;// osmWay;
    }
}
