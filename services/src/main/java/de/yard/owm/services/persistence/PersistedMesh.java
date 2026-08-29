package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2world.MetricMapProjection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

import java.util.*;

/**
 *
 */
@Entity
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Table(name = "mesh")
public class PersistedMesh extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mesh_id_generator")
    @SequenceGenerator(name = "mesh_id_generator", sequenceName = "mesh_seq", allocationSize = 1)
    private Long id;

    @Column(name = "name")
    @Setter
    @Getter
    private String name;

    @OneToMany(mappedBy = "mesh", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Getter
    Set<PersistedMeshPolygon> polygons = new HashSet<>();

    @OneToMany(mappedBy = "persistedMesh")
    @Getter
    Set<PersistedMeshFailure> failures = new HashSet<>();

    public PersistedMesh(String name) {
        this.name = name;
    }
}
