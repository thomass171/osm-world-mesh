package de.yard.owm.services.persistence;

import de.yard.threed.osm2scenery.polygon20.OsmWay;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@Entity
@EqualsAndHashCode(of = "id")
@Table(name = "osmway")
public class PersistedOsmWay implements OsmWay {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "osmway_id_generator")
    @SequenceGenerator(name = "osmway_id_generator", sequenceName = "osmway_seq", allocationSize = 1)
    private Long id;

    @Column(name = "osm_id")
    @Setter
    @Getter
    private long osmId;

    @Column(name = "category")
    @Setter
    @Getter
    private String category;

    //  @JoinTable only works with pure mapping tables without additional attributes?
    @OneToMany(mappedBy = "osmWay", cascade = CascadeType.ALL)
    @OrderBy(value = "index")
    @Getter
    private List<PersistedOsmWayNode> osmWayNodes = new ArrayList<>();

    public PersistedOsmWay() {

    }

    public void add(PersistedOsmNode osmNode, int index) {
        PersistedOsmWayNode osmWayNode = new PersistedOsmWayNode(this, osmNode,index);
        osmWayNodes.add(osmWayNode);
       // osmNode.getOsmWays().add(this);
    }

    @Override
    public SceneryWayObject buildSceneryWayObject() {
        return null;
    }
}
