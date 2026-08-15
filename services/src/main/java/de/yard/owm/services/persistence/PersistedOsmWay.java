package de.yard.owm.services.persistence;

import de.yard.threed.osm2scenery.polygon20.OsmWay;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
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
   /*24.3.26  @OneToMany(mappedBy = "osmWay", cascade = CascadeType.ALL)
    @OrderBy(value = "index")
    @Getter
    private List<PersistedOsmWayNode> osmWayNodes = new ArrayList<>();*/

    public PersistedOsmWay() {

    }

    public void add(PersistedOsmNode osmNode, int index) {
        PersistedOsmWayNode osmWayNode = new PersistedOsmWayNode(this, osmNode,index);
        /*24.3.26 osmWayNodes.add(osmWayNode);*/
       // osmNode.getOsmWays().add(this);
    }

    @Override
    public SceneryWayObject buildSceneryWayObject() {
        return null;
    }
}
