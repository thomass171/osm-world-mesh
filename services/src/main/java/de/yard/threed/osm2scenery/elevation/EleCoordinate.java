package de.yard.threed.osm2scenery.elevation;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2world.VectorXYZ;
import de.yard.threed.osm2world.VectorXZ;


/**
 * Ein Punkt, an dem mindestens zwei Polygone zusammenkommen. Dieser Punkt legt fuer
 * alle zusammenkommenden Polygone an dieser Stelle die Elevation fest (ueber EleConnectorGroup).
 * 22.8.18: Die Klasse koennte obselet sein, zumindest so wie ich sie verwende. Bei
 * mir ist die Group eher fuer diesen Zweck. Im Moment komme ich ueber diese Klasse aber noch an die Group.
 * 12.6.19: Umbenannt EleConnector->EleCoordinate

 */
public class EleCoordinate {

    public VectorXZ pos;

    /**
     * TODO document - MapNode or Intersection object, for example
     */
    public  Object reference;

    public Coordinate coordinate;

    private VectorXYZ posXYZ;
    // Ist das mandatory?
    private EleConnectorGroup group;
    public String label="";
    /**
     * creates an EleConnector at the given xz coordinates.
     *
     */
    /*public EleConnector(VectorXZ pos, Object reference/*, GroundState groundState* /) {
        assert pos != null;
        this.pos = pos;
        this.reference = reference;
        //this.groundState = groundState;
    }*/

    public EleCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;

    }

    public EleCoordinate(Coordinate coordinate,EleConnectorGroup eleConnectorGroup,String label) {
        this(coordinate);
        this.group=eleConnectorGroup;
        this.label=label;
    }

    /**
     * assigns the elevation that has been calculated for this connector.
     * Only for use by an {@link ElevationCalculator}.
     * <p>
     * TODO make package-visible
     */
    /*public void setPosXYZ(VectorXYZ posXYZ) {

        assert posXYZ.xz().equals(this.pos);

        this.posXYZ = posXYZ;

    }*/

    /**
     * returns the 3d position after it has been calculated.
     * <p>
     * The elevation, and therefore this {@link VectorXYZ}, isType the only
     * property which changes (exactly once) over the lifetime of an
     * {@link EleCoordinate}: It isType null before elevation calculation,
     * and assigned its ultimate value afterwards.
     */
    public VectorXYZ getPosXYZ() {
        return posXYZ;
    }

    /**
     * returns true if this connector isType to be joined with the other one.
     * It isType possible that connectors are joined even if this method returns
     * false - that can happen when they are both joined to a third connector.
     */
    /*public boolean connectsTo(EleConnector other) {
        return pos.equals(other.pos)
                && ((reference != null && reference == other.reference)
                /*|| (groundState == ON && other.groundState == ON)* /);
    }*/

    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", pos, reference, ""/*groundState*/);
    }

    public void setGroup(EleConnectorGroup group) {
        this.group = group;
    }

    public EleConnectorGroup getGroup() {
        return group;
    }
}
