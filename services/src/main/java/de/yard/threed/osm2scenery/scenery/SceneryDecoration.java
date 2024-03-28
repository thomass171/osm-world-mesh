package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroup;
import de.yard.threed.osm2scenery.elevation.EleConnectorGroupSet;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2scenery.scenery.components.DecoratorComponent;
import de.yard.threed.osm2scenery.scenery.components.RoadDecorator;
import de.yard.threed.osm2scenery.util.PolygonMetadata;
import de.yard.threed.osm2scenery.util.SmartPolygon;
import de.yard.threed.osm2world.Material;

import java.util.List;

/**
 * <p>
 * Schon deprecated zugunsten {@link SceneryObjectFactory.createMarking( DecoratorComponent decoratorComponent)}
 * Created on 18.04.19.
 */
@Deprecated
public class SceneryDecoration extends SceneryFlatObject {
    Polygon basepolygon;

    /**
     *
     */
    private SceneryDecoration(String creatortag, Material material) {
        super(creatortag, material, null, new Area(null, null));
        //Q&D Versuch
        decoratorComponent = new RoadDecorator();
    }


    @Override
    public void buildEleGroups() {
        //erstmal einfach eine einzelne Group ohne mapnode
        elevations = new EleConnectorGroupSet();
        EleConnectorGroup egr = new EleConnectorGroup(null);
        getEleConnectorGroups().eleconnectorgroups.add(egr);
    }

    /**
     *
     */
    @Override
    public List<ScenerySupplementAreaObject> createPolygon(List<SceneryObject> objects, GridCellBounds gridbounds, TerrainMesh tm) {


        flatComponent[0].poly = new SmartPolygon((Polygon) decoratorComponent.getDecoration().getGeometry(), new PolygonMetadata(this));
        return null;
    }


    @Override
    protected void registerCoordinatesToElegroups(TerrainMesh tm) {

    }

    @Override
    public boolean isPartOfMesh(TerrainMesh tm) {
        //TODO irgendwie erkennen
        return false;
    }
}
