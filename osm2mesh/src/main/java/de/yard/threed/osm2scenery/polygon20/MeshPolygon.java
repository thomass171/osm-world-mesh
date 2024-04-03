package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.osm2scenery.polygon20.MeshNode;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Ein Objekt dieser Klasse ist flüchtig, weil sich Lines ändern koennen!
 */
public class MeshPolygon {
    Logger logger = Logger.getLogger(MeshPolygon.class);

    public List<MeshLine> lines = new ArrayList();

    /**
     * Die lines sind nicht zwingend sortiert. Das passiert erst, wenn es gebraucht wird.
     * Oder doch hier?
     * Und CCW?
     *
     * @param lines
     */
    public MeshPolygon(List<MeshLine> lines) {
        this.lines = TerrainMesh.sort(lines);
        //Konsistenzprüfung
        if (getPolygon() == null) {
            //throw new RuntimeException("invalid MeshPolygon");
            logger.warn("invalid mesh polygon");
            //leeren, weil es sonst nur Folgefehler gibt
            this.lines = new ArrayList<>();
        }
    }

    public MeshPolygon(MeshLine meshLine) {
        lines.add(meshLine);
    }

    public MeshPolygon(MeshLine meshLine, MeshLine remaining) {
        lines.add(meshLine);
        lines.add(remaining);
    }

    /**
     * Die lines sind zwar sortiert, from und to müssen aber in die richtige order gebracht werden.
     *
     * @return
     */
    public Polygon getPolygon() {
        List<Coordinate> coors = new ArrayList<>();

        if (lines.size() == 0) {
            return null;
        }
        coors.addAll(JtsUtil.toList(lines.get(0).getCoordinates()));
        MeshNode point = lines.get(0).getTo();
        //immer den letzten Punkt eine line noch mitnehmen und bei der naechsten 1 dahinter beginnen.
        for (int i = 1; i < lines.size(); i++) {
            MeshLine line = lines.get(i);
            if (line.getFrom() == point) {
                for (int j = 1; j < line.length(); j++) {
                    coors.add(line.get(j));
                }
                point = line.getTo();
            } else {
                for (int j = line.length() - 2; j >= 0; j--) {
                    coors.add(line.get(j));
                }
                point = line.getFrom();
            }

        }
        Polygon p = JtsUtil.createPolygon(coors);
        if (p == null || !p.isValid()) {
            logger.error("invalid:p="+p);
            return null;
        }
        return p;
    }

    /**
     * aber nicht per complete, denn es sind u.U. beide Areas leer. lines sind CCW? Braucht trotzdem leftindicator
     */
    public void setInner(AbstractArea abstractArea, List<Boolean> leftIndicator) {
        for (int i = 0; i < lines.size(); i++) {
            MeshLine meshLine = lines.get(i);

            //TerrainMesh.getInstance().completeLine(meshLine, flatComponent[0]);
            if (leftIndicator.get(i)) {
                if (meshLine.getLeft() != null && meshLine.getLeft() != abstractArea) {
                    logger.error("left already set");
                }
                meshLine.setLeft(abstractArea);
            } else {
                if (meshLine.getRight() != null && meshLine.getRight() != abstractArea) {
                    logger.error("right already set");
                }
                meshLine.setRight(abstractArea);
            }
        }
    }

    /**
     * Add additional coordinate. Must be on any existing line.
     *
     * @param c
     */
    public MeshLine insert(Coordinate c) {
        for (MeshLine line : lines) {
            int index;
            if ((index = line.getCoveringSegment(c)) != -1) {
                line.insert(index + 1, c);
                return line;
            }
        }
        return null;
    }
}
