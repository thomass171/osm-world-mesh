package de.yard.owm.services.persistence;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.LatLon;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2world.MetricMapProjection;
import de.yard.threed.osm2world.O2WOriginMapProjection;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.traffic.geodesy.GeoCoordinate;
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
@Table(name = "meshnode")
public class PersistedMeshNode implements MeshNode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meshnode_id_generator")
    @SequenceGenerator(name = "meshnode_id_generator", sequenceName = "meshnode_seq", allocationSize = 1)
    private Long id;

    @Column(name = "lat")
    @Setter
    @Getter
    private double lat;

    @Column(name = "lon")
    @Setter
    @Getter
    private double lon;

    @Transient
    private List<PersistedMeshLine> linesOfPoint = new ArrayList();
    @Transient
    MetricMapProjection projection;

    @Transient
    public EleConnectorGroup group;
    @Transient
    Logger logger = Logger.getLogger(MeshNode.class);

    public PersistedMeshNode() {

    }

    public PersistedMeshNode(Coordinate coordinate, MetricMapProjection projection) {
        //this.coordinate = coordinate;
        setProjection(projection);
        GeoCoordinate latlon = projection.unproject(coordinate);
        lat = latlon.getLatDeg().getDegree();
        lon = latlon.getLonDeg().getDegree();
    }

    public PersistedMeshNode(GeoCoordinate latLon, MetricMapProjection projection) {
        setProjection(projection);
        lat = latLon.getLatDeg().getDegree();
        lon = latLon.getLonDeg().getDegree();
    }

    @Override
    public String toString() {
        return "" + lat + "," + lon;
    }


    public void removeLine(PersistedMeshLine line) {
        linesOfPoint.remove(line);
    }

    @Override
    public Coordinate getCoordinate() {
        return projection.project(getGeoCoordinate());
    }

    @Override
    public void addLine(MeshLine line) {
        linesOfPoint.add((PersistedMeshLine) line);
    }

    @Override
    public void removeLine(MeshLine line) {

    }

    public int getLineCount() {
        return linesOfPoint.size();
    }

    public List<MeshLine> getLines() {
        return Collections.unmodifiableList(linesOfPoint);
    }

    public GeoCoordinate getGeoCoordinate() {
        return GeoCoordinate.fromLatLon(LatLon.fromDegrees(lat, lon), -555);
    }

    public void setProjection(MetricMapProjection projection) {
        this.projection = projection;
    }
}
