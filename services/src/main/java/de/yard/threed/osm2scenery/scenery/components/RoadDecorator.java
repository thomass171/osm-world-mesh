package de.yard.threed.osm2scenery.scenery.components;

import de.yard.threed.core.Degree;
import de.yard.threed.core.OutlineBuilder;
import de.yard.threed.core.Util;
import de.yard.threed.core.Vector2;
import de.yard.threed.core.geometry.Shape;
import de.yard.threed.engine.ShapeFactory;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.util.DecorationFactory;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.Materials;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * TODO rename DecorationFactory
 */
public class RoadDecorator implements DecoratorComponent {
    @Override
    public Decoration getDecoration() {
        return DecorationFactory.buildRoadArrow(10,3,0.5);
    }

    public AbstractArea createLineDecoration(List<Vector2> centerLine) {
        double offset=-1;
        List<Vector2> outline = OutlineBuilder.getOutline(centerLine, offset);
//erstmal was zu breit
        double width=0.6;

        AbstractArea area = WayArea.buildOutlinePolygonFromCenterLine(outline, null,  width, null, Materials.ROAD_MARKING);
        return area;
    }

    /**
     * Haltelinie
     * @return
     */
    public AbstractArea createStopLine(Vector2 centerpoint, Vector2 outerpoint) {
        double offset=-1;
        List<Vector2> line = Util.<Vector2>buildList(centerpoint, outerpoint);
//erstmal was zu breit
        double width=0.6;

        AbstractArea area = WayArea.buildOutlinePolygonFromCenterLine(line, null,  width, null, Materials.ROAD_MARKING);
        return area;
    }

    /**
     * Das muss eine WayArea werden, damit Texturierung einfach per TriangleStrip geht.
     */
    public AbstractArea createRectangle(Vector2 centerpoint, double width, double height, Degree rotation, Material material) {
        Shape centerline = ShapeFactory.buildLine(width,1);
        centerline = centerline.rotate(rotation.toRad());
        centerline = centerline.translate(centerpoint);
        List<Vector2> line = Util.<Vector2>buildList(centerline.getPoints().get(0), centerline.getPoints().get(1));
        AbstractArea area = WayArea.buildOutlinePolygonFromCenterLine(line, null,  height, null, material);
        return area;
    }

    /**
     * Das muss eine WayArea werden, damit Texturierung einfach per TriangleStrip geht.
     */
    public AbstractArea createParking(SceneryWayObject parkingtaxiway,Material material) {
        // end node isType parking position
        String parkposname = StringUtils.defaultString(parkingtaxiway.mapWay.getTags().getValue("ref"), "");
        Vector2 centerpoint = parkingtaxiway.getCenterLine().get(parkingtaxiway.getCenterLine().size()-1);
        Degree heading = OsmUtil.getHeadingAtEnd(parkingtaxiway.mapWay);
        // MathUtil2.getDegreeFromHeading() not appropriate due to texture Atlas layout
        AbstractArea marking = createRectangle(centerpoint, 8, 8,new Degree(-heading.getDegree()), material);
        marking.setName(parkposname);
        return marking;
    }
}
