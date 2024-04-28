package de.yard.threed.osm2scenery.scenery;

import de.yard.threed.core.Vector2;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.polygon20.MeshInconsistencyException;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2scenery.polygon20.Sector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * transient info!
 */
public class MeshNodeDetails {

    // Like in math with 0-degree pointing right (+x). corresponds to order of lines
    public List<Vector2> directions = new ArrayList<>();
    MeshNode node;

    MeshNodeDetails(MeshNode node) {
        this.node = node;
        for (MeshLine line : node.getLines()) {
            directions.add(getDirection(line));
        }
    }

    public MeshLine getNeighborLine(MeshLine origin, boolean ccw) throws MeshInconsistencyException {
        List<MeshLine> sortedLines = node.getLines().stream().sorted(new Comparator<MeshLine>() {
            @Override
            public int compare(MeshLine o1, MeshLine o2) {
                // assume angles are normalized, ie. between 0-360
                return Double.compare(getDirection(o1).angle().getDegree(), getDirection(o2).angle().getDegree());
            }
        }).collect(Collectors.toList());
        for (int i = 0; i < sortedLines.size(); i++) {
            MeshLine line = sortedLines.get(i);
            if (line.equals(origin)) {
                if (!ccw) {
                    if (i < sortedLines.size() - 1) {
                        return sortedLines.get(i + 1);
                    } else {
                        return sortedLines.get(0);
                    }
                } else {
                    if (i > 0) {
                        return sortedLines.get(i - 1);
                    } else {
                        return sortedLines.get(sortedLines.size() - 1);
                    }
                }
            }
        }
        throw new MeshInconsistencyException("no neighbor for line " + origin);
    }

    public  Sector getNeighborSector(MeshLine origin, boolean ccw) throws MeshInconsistencyException {
        MeshLine neighbor = getNeighborLine(origin, ccw);
        if (ccw){
            return new Sector(node, getDirection(origin).angle(),getDirection(neighbor).angle());
        }else{
            return new Sector(node, getDirection(neighbor).angle(),getDirection(origin).angle());
        }
    }

    /**
     * direction is always from the nodes view, not the line direction
     */
    private Vector2 getDirection(MeshLine line) {
        if (line.getFrom().equals(node)) {
            return JtsUtil.getDirection(line.getFrom().getCoordinate(), line.getTo().getCoordinate());
        } else {
            return JtsUtil.getDirection(line.getTo().getCoordinate(), line.getFrom().getCoordinate());
        }
    }
}
