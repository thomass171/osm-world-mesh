package de.yard.owm.services.persistence;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * From
 * https://www.baeldung.com/jpa-many-to-many
 */
@EqualsAndHashCode(of = "osmWayId, osmNodeId")
@Embeddable
public class PersistedOsmWayNodeKey implements Serializable {

    @Column(name = "osmway_id")
    @Setter
    @Getter
    private long osmWayId;

    @Column(name = "osmnode_id")
    @Setter
    @Getter
    private long osmNodeId;

}
