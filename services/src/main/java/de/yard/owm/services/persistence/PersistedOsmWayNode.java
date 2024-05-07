package de.yard.owm.services.persistence;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

/**
 *
 */
@Entity
//TODO @EqualsAndHashCode(of = "id")
@Table(name = "osmwaynode")
public class PersistedOsmWayNode {

    @EmbeddedId
    @Setter
    @Getter
    PersistedOsmWayNodeKey id;

    @ManyToOne
    @MapsId("osmwayId")
    @JoinColumn(name = "osmway_id")
    @Setter
    @Getter
    PersistedOsmWay osmWay;

    @ManyToOne
    @MapsId("osmnodeId")
    @JoinColumn(name = "osmnode_id")
    @Setter
    @Getter
    PersistedOsmNode osmNode;

    @Column(name = "index")
    @Setter
    @Getter
    private int index;

    public PersistedOsmWayNode() {

    }

    public PersistedOsmWayNode(PersistedOsmWay osmWay, PersistedOsmNode osmNode, int index) {
        this.id = new PersistedOsmWayNodeKey();
        this.id.setOsmWayId(osmWay.getId());
        this.id.setOsmNodeId(osmNode.getId());
        this.osmWay = osmWay;
        this.osmNode = osmNode;
        this.index = index;
    }
}
