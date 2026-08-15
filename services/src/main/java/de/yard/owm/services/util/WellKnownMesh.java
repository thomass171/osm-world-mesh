package de.yard.owm.services.util;

import de.yard.owm.misc.GeneralOwmException;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.MainGrid;
import lombok.AllArgsConstructor;

/**
 * For having more consistent data, tests aso.
 */
@AllArgsConstructor
public enum WellKnownMesh {
    //String[] wellKnownMes
    Desdorf(MainGrid.buildDesdorf()),
    Zieverich(null/*TODO*/);

    private GridCellBounds getGridCellBounds;

    public static WellKnownMesh of(String meshName) throws GeneralOwmException {
        if (meshName.equals(Desdorf.name())) {
            return Desdorf;
        }
        throw new GeneralOwmException("No well known mesh: " + meshName);
    }

    public GridCellBounds getGridCellBounds() {
        return getGridCellBounds;
    }
}
