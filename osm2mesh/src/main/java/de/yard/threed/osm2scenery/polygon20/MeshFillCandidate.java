package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2scenery.polygon20.MeshLine;
import de.yard.threed.core.Util;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Fuer sowas wie WayToAreaFiller.
 */
public class MeshFillCandidate {
    Logger logger = Logger.getLogger(MeshFillCandidate.class);
    //Als Hilfsmittel
    public Polygon polygon;
    public MeshLine wayLine;
    public MeshLine targetLine;
    //C0 corresponds to way start, C1 to end
    public int targetC0;
    public int targetC1;
    public boolean rightOfWay;
    public List<MeshLine> lines;
    public List<Boolean> leftIndicator;
    //MeshPolygon meshPolygon = null;
    /*@Deprecated
    public MeshFillCandidate(Polygon cp) {
        polygon = cp;
    }*/

    public MeshFillCandidate(Polygon polygon, MeshLine wayLine, MeshLine targetLine, int targetC0, int targetC1, boolean rightOfWay) {
        this.polygon = polygon;
        this.wayLine = wayLine;
        this.targetLine = targetLine;
        this.targetC0 = targetC0;
        this.targetC1 = targetC1;
        this.rightOfWay = rightOfWay;
    }


    public void register(TerrainMesh terrainMesh) {

        if (targetC0 == targetC1) {
            Util.notyet();
        }
        if (targetC0 == 0/*way.getOsmIdsAsString().contains("107468171")*/) {
            logger.warn("9.9.19: weils wegen c0=0 im split unten noch nicht geht.");
            return;
        }
        if (targetC0 < targetC1) {
            //erst hinten splitten. CCW
            /*MeshLine[] secondsplit = terrainMesh.split(targetLine, targetC1);
            MeshLine[] firstsplit = terrainMesh.split(targetLine, targetC0);
            if (firstsplit == null || secondsplit == null) {
                logger.error("failure");
                return;
            }*/
            MeshLineSplitCandidate msc = new MeshLineSplitCandidate(targetLine);
            msc.fromIsCoordinate = true;
            msc.from = targetC0;
            msc.toIsCoordinate = true;
            msc.to = targetC1;
            msc.newcoors = new ArrayList<>();
            if (targetC1 >= targetLine.getCoordinates().length) {
                logger.error("c1 too large");
                return;
            }

            msc.newcoors.add(targetLine.getCoordinates()[targetC0]);
            msc.newcoors.add(targetLine.getCoordinates()[targetC1]);
            //MeshLine[] split = terrainMesh.split(targetLine, targetC0, targetC1);
            MeshLine[] split = terrainMesh.split(msc);
            // die Lines werden CCW abgelegt
            lines = new ArrayList<>();
            leftIndicator = new ArrayList<>();
            if (split == null) {
                logger.error("missing error handling/not yat");
                return;
            }
            lines.add(terrainMesh.registerLine(wayLine.getFrom(), split[1].getFrom(), null, null));
            leftIndicator.add(true);

            lines.add(split[1]);
            leftIndicator.add(true);

            lines.add(terrainMesh.registerLine(split[1].getTo(), wayLine.getTo(), null, null));
            leftIndicator.add(true);

            lines.add(wayLine);
            leftIndicator.add(false);

        } else {
            logger.error("mesh fill:not yet");
        }

    }

    public MeshPolygon getMeshPolygon() {
        try {
            return new MeshPolygon(lines);
        } catch (MeshInconsistencyException e) {
            throw new RuntimeException(e);
        }
    }
}
