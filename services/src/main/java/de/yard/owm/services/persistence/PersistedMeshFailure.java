package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.polygon20.GeoPolygon;
import de.yard.threed.osm2scenery.polygon20.MeshFailure;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
@Entity
@Data
@EqualsAndHashCode(of = "id")
@Table(name = "meshfailure")
public class PersistedMeshFailure implements MeshFailure {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meshfailure_id_generator")
    @SequenceGenerator(name = "meshfailure_id_generator", sequenceName = "meshfailure_seq", allocationSize = 1)
    private Long id;

    @Column(name = "sourceref")
    private String sourceRef;

    @Column(name = "polygon")
    private String polygon;

    @Column(name = "message")
    private String message;

    @ManyToOne
    @JoinColumn(name = "mesh_id", referencedColumnName = "id", nullable = false)
    PersistedMesh persistedMesh;

    public PersistedMeshFailure() {

    }

    @Override
    public GeoPolygon getGeoPolygon() {
        if (polygon == null) {
            return null;
        }
        // polygon is in geo coordinates
        Geometry g = JtsUtil.buildFromWKT(polygon);

        // 11.7.26 This is a failure, so not sure how to handle it. For now just wrap in RuntimeException.
        try {
            return GeoPolygon.fromPolygon((Polygon) g);
        } catch (MeshInconsistencyException e) {
            throw new RuntimeException(e);
        }
    }
}
