package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.owm.services.persistence.PersistedMeshLine;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.polygon20.OsmNode;
import de.yard.threed.osm2world.MetricMapProjection;
import de.yard.threed.traffic.geodesy.GeoCoordinate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
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
    @OneToMany(mappedBy = "osmNode", cascade = CascadeType.ALL)
    @Getter
    Set<PersistedOsmWayNode> osmWayNodes = new HashSet<>();

    public PersistedOsmNode() {

    }

    public PersistedOsmNode(long osmId) {
        this.osmId = osmId;
    }
}
