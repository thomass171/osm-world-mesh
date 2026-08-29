package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Degree;
import de.yard.threed.core.GeoCoordinate;
import de.yard.threed.core.LatLon;
import de.yard.threed.core.Util;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.polygon20.MeshNodePair;
import de.yard.threed.osm2scenery.util.GeoCoordinatePair;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
@Entity
@EqualsAndHashCode(of = "id")
@Table(name = "meshnodepair")
@Data
public class PersistedMeshNodePair extends AuditedEntity implements MeshNodePair {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meshnodepair_id_generator")
    @SequenceGenerator(name = "meshnodepair_id_generator", sequenceName = "meshnodepair_seq", allocationSize = 1)
    private Long id;

    //@ManyToOne
    @Column(name = "leftnode", nullable = false)
    /*PersistedMeshNode*/int left;

    //@ManyToOne
    @Column(name = "rightnode", nullable = false)
    /*PersistedMeshNode*/int right;

    /**
     * Typically the way, not the node (which is in the polygon)
     */
    @Column(name = "osmid")
    private Long osmId;

    @Column(name = "heading")
    private double heading;

    @ManyToOne
    @JoinColumn(name = "meshpolygon_id", referencedColumnName = "id", nullable = false)
    PersistedMeshPolygon meshPolygon;

    /**
     * Typically the node where the way segment ends.
     */
    /*@Column(name = "opposite_node_osm_id", nullable = false)
    private long oppositeNodeOsmId;**/

    public PersistedMeshNodePair() {

    }

    /**
     * 'right' is 'first'
     */
    @Override
    public GeoCoordinatePair getCoordinates() {
        return new GeoCoordinatePair(meshPolygon.getNodesSortedByIndex().get(right).getGeoCoordinate(),
                meshPolygon.getNodesSortedByIndex().get(left).getGeoCoordinate());
    }

    @Override
    public long getOsmWayId() {
        return osmId;
    }

    @Override
    public Degree getHeading() {
        return new Degree(heading);
    }


    /**
     * As this is for logging/debugging, also include coordinates).
     * getCoordinate() already provides parantheses.
     *
     * @return
     */
    /*@Override
    public String toString() {
        return "" + lat + "," + lon + getCoordinate();
    }*/


}
