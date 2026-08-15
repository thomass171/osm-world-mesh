package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.owm.services.persistence.PersistedMeshLine;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.polygon20.OsmNode;
import de.yard.threed.osm2world.MetricMapProjection;
import de.yard.threed.core.GeoCoordinate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@Entity
@EqualsAndHashCode(of = "id")
@Table(name = "osmnode")
public class PersistedOsmNode implements OsmNode {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "osmnode_id_generator")
    @SequenceGenerator(name = "osmnode_id_generator", sequenceName = "osmnode_seq", allocationSize = 1)
    private Long id;

    @Column(name = "osm_id")
    @Setter
    @Getter
    private long osmId;

    @Column(name = "lat")
    @Setter
    @Getter
    private double lat;

    @Column(name = "lon")
    @Setter
    @Getter
    private double lon;

    //  @JoinTable only works with pure mapping tables without additional attributes?
   /*24.3.26  @OneToMany(mappedBy = "osmNode", cascade = CascadeType.ALL)
    @Getter
    Set<PersistedOsmWayNode> osmWayNodes = new HashSet<>();*/

    public PersistedOsmNode() {

    }

    public PersistedOsmNode(long osmId) {
        this.osmId = osmId;
    }
}
