package de.yard.threed.osm2world;

import de.yard.threed.core.Degree;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
public class MapWaySegmentAtConnector/*Key*/ {
    @Getter
    private long wayOsmId;
private Degree headingAtconnector;
    //private  long startNodeOsmId;
    //private  long endNodeOsmId;

    /*public long getOppositeNodeOsmId(long osmNodeId) throws MeshInconsistencyException {
        if (osmNodeId==startNodeOsmId){
            return endNodeOsmId;
        }
        if (osmNodeId==endNodeOsmId){
            return startNodeOsmId;
        }
        throw new MeshInconsistencyException("xx");
    }*/
}
