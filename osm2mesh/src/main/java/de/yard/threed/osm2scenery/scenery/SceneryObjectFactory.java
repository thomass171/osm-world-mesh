package de.yard.threed.osm2scenery.scenery;

import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.modules.AerowayModule;
import de.yard.threed.osm2scenery.polygon20.MeshFillCandidate;
import de.yard.threed.osm2scenery.scenery.components.Area;
import de.yard.threed.osm2scenery.scenery.components.BuildingComponent;
import de.yard.threed.osm2scenery.scenery.components.DecoratorComponent;
import de.yard.threed.osm2scenery.util.TagMap;
import de.yard.threed.osm2world.*;

public class SceneryObjectFactory {
    /**
     * 23.5.19: Return Area statt SceneryFlatObject.Gehoert dann auch nicht mehr in diese Factory. Das brauchts eh nicht mehr.
     *
     * @param decoratorComponent
     * @return
     */
    public static Area/*SceneryFlatObject*/ createDecoration(DecoratorComponent decoratorComponent) {
        //flatComponent.poly = new SmartPolygon((Polygon) decoratorComponent.getDecoration().getGeometry(), new PolygonMetadata(this));
        //marking = new SceneryDecoration("Marking", GRASS);
        Area area = new Area((Polygon) decoratorComponent.getDecoration().getGeometry(), Materials.ROAD_MARKING);
        //SceneryFlatObject marking = new SceneryFlatObject("Marking", WATER, null, area, true);
        return area;
    }

    /**
     * Ein Taxiway ist sowohl Way (wegen graph) als auch eine Decoration (zum Ausschneiden aus dem Apron oder aufkleben)
     * <p>
     * 20.4.19: Ob wirklich als Way, ist noch unklar. Decorator/Supplement?
     * Nö, das ist doch klar ein Way, hat eine OsmId wie River,Rails,Road. Auch wenn es im Apron eingeschnitten
     * ist wie RoadMarkings, ist es trotzdem kein Supplement. Eher wie eine Road über einen Parkplatz.
     * 23.4.19: Muss wohl schon "breit" sein, weil Apron nicht unbedingt auch Taxiway abdeckt.
     * Das ist aber unguenstig, weil es dann zu vielen Überschneidungen kommen kann/wird, die schlecht handhabbar sind.
     * Lieber doch schmaler Taxiway; das fehlende Areal wird dann später dazugebaut.
     */
    public static SceneryWayObject createTaxiway(MapWay line, TagMap materialmap, AerowayModule.TaxiWayCustomData taxiWayCustomData, SceneryContext sceneryContext) {

        SceneryWayObject taxiway = new SceneryWayObject("Taxiway", line, Materials.TAXIWAY_YELLOW, SceneryObject.Category.TAXIWAY, new FixedWidthProvider(AerowayModule.GROUNDNETMARKERWIDTH), sceneryContext);
        return taxiway;
    }

    /**
     * Ein Building ist eine Grundfläche (Area) mit einem Volumen.
     * SceneryAreaObject statt AbstractSceneryFlatObject versuchen.
     */
    public static SceneryAreaObject createBuilding(MapArea mapArea, TagMap materialmap, BuildingComponent buildingComponent) {

        SceneryAreaObject building = new SceneryAreaObject(mapArea, "Building", Materials.WALL_DEFAULT, SceneryObject.Category.BUILDING);
        building.volumeProvider = buildingComponent;
        building.cycle = SceneryObject.Cycle.BUILDING;
        return building;
    }

    public static ScenerySupplementAreaObject createWayToAreaFiller(SceneryWayObject way, MeshFillCandidate candidate) {
        ScenerySupplementAreaObject supplement = new ScenerySupplementAreaObject("WayToAreaFiller", candidate, Materials.TERRAIN_DEFAULT);
        supplement.flatComponent[0].parentInfo = "WayToAreaFiller";
        return supplement;
    }

    /*public static ScenerySupplementAreaObject createBackgroundFiller(MeshPolygon meshPolygon) {
        ScenerySupplementAreaObject supplement = new ScenerySupplementAreaObject("BackgroundFiller", meshPolygon, Materials.TERRAIN_DEFAULT);
        return supplement;
    }*/
}
