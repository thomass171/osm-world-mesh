package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Entity
@Getter
@Setter
@Table(name="meshnode")
public class MeshNode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meshnode_id_generator")
    @SequenceGenerator(name = "meshnode_id_generator", sequenceName = "meshnode_seq", allocationSize = 1)
    private Long id;

    @Column(name = "lat")
    private double lat;

    @Column(name = "lon")
    private double lon;

    @Transient
    private List<MeshLine> linesOfPoint = new ArrayList();
    @Transient
    public Coordinate coordinate;
    @Transient
    public EleConnectorGroup group;
    @Transient
    Logger logger = Logger.getLogger(MeshNode.class);

    public MeshNode() {

    }

    public MeshNode(Coordinate coordinate) {
        this.coordinate = coordinate;
        //  pool.add(this);
        //  this.id=pool.size();
    }

    /*public void addLineToPoint(MeshLine line) {
        /*if (!linesOfPoint.containsKey(point)) {
            linesOfPoint.put(point, new ArrayList<Integer>());
        }
        linesOfPoint.get(point).add(line);* /
        linesOfPoint.add(line);
    }
*/
    @Override
    public String toString() {
        return "" + coordinate;
    }

    public void addLine(MeshLine line) {
        if (linesOfPoint.contains(line)) {
            //convenience
            return;
        }
        //bei Way junctions gibt es auch mal 4, darum > 3.
        if (linesOfPoint.size() > 3) {
            //21.8.19:aber da ist doch was faul
            logger.warn("too many lines at point " + coordinate + "): " + linesOfPoint.size());

        }
        linesOfPoint.add(line);
    }

    public void removeLine(MeshLine line) {
        linesOfPoint.remove(line);
    }

    public int getLineCount() {
        return linesOfPoint.size();
    }

    public List<MeshLine> getLines() {
        return Collections.unmodifiableList(linesOfPoint);
    }
}
