package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2scenery.polygon20.MeshLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Information über eine Splitmöglichkeit einer line "auf freier Strecke" oder an einer Coordinate.
 * Zwei Fälle (eigentlich noch mehr wegen on line oder nicht):
 * 1) der Regelfall: es wird ein neues Segment eingefügt.
 * 2) der Sonderfall: Die Line bekommt nur einen neuen SplitPoint (z.B. Desdorf Farmland/Boundary). Dann enthält newcoors nur den einen Punkt und nur from ist gesetzt.
 */
public class MeshLineSplitCandidate {
    public MeshLine meshLineToSplit;
    //jeweils Indices des Segments oder der Coordinate,auch ein Mix. Die Segmentindizes muessen z.Z. gleich sein, d.h. nicht auf verschiedene Segment zeigen.
    public int from=-1,to=-1;
    public List<Coordinate> newcoors;
    public List<Coordinate> remaining;
    public boolean fromIsCoordinate=false,toIsCoordinate=false;
    // durch den Split entstehen zwei oder drei Segmente. Dies ist der
    // Index des gemeinsamen Segments oder -1 wenns nur einen common point gibt (Desdorf Farmland).
    public int commonsegment;
    //Die Coordinateindizes, die gefunden wurden. Die sind hier nur zwsichengeparkt.
    public int commonfirst = -1, commonsecond = -1;
public    List<Coordinate> result = new ArrayList<>();


    public MeshLineSplitCandidate(MeshLine meshLine) {
        this.meshLineToSplit=meshLine;
    }

    /**
     * umstaendlich?
     * @return
     */
   /* public static MeshLineSplitCandidate buildMeshLineSplitCandidateForSinglePoint(MeshLine meshLine, Coordinate splitpoint, int from, List<Coordinate> remaining){
        MeshLineSplitCandidate meshLineSplitCandidate=new MeshLineSplitCandidate(meshLine);
        meshLineSplitCandidate.newcoors=new ArrayList<>();
        meshLineSplitCandidate.newcoors.add(splitpoint);
        meshLineSplitCandidate.remaining=remaining;
        meshLineSplitCandidate.from=from;
        meshLineSplitCandidate.to=-1;
        return meshLineSplitCandidate;
    }*/

    /**
     * Check if split isType a onew single point only resulting in only two lines instead of three
     * @return
     */
    public boolean isPointSplit() {
        //return newcoors.size()==1 || (areCoordinates && to==meshLineToSplit.coordinates.length-1);
        return to==-1;
    }
}
