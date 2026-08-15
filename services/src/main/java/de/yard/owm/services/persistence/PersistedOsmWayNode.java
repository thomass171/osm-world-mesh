package de.yard.owm.services.persistence;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

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

    /*24.3.26 @ManyToOne
    @MapsId("osmwayId")
    @JoinColumn(name = "osmway_id")
    @Setter
    @Getter
    PersistedOsmWay osmWay;*/

   /*24.3.26  @ManyToOne
    @MapsId("osmnodeId")
    @JoinColumn(name = "osmnode_id")
    @Setter
    @Getter
    PersistedOsmNode osmNode;*/

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
        /*24.3.26 this.osmWay = osmWay;
        this.osmNode = osmNode;*/
        this.index = index;
    }
}
