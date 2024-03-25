package de.yard.threed.osm2scenery.scenery.components;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2scenery.modules.BuildingModule;
import de.yard.threed.osm2scenery.scenery.WorldElement;
import de.yard.threed.osm2world.Material;
import de.yard.threed.osm2world.Primitive;
import de.yard.threed.osm2world.PrimitiveBuffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * All volumetric parts created from OSM.
 * Also alles, was nicht Flat ist und auf einer Grundfläche steht. Z.B. Buildings.
 * Ach, lieber BuildingComponent statt VolumeComponent
 * 26.4.19: Lieber auch einen VolumeProvider
 */

public class BuildingComponent implements VolumeProvider {
    public BuildingModule.Building building;
    List<WorldElement> elements;
    public PrimitiveBuffer primitiveBuffer;

    public BuildingComponent(BuildingModule.Building building) {
        this.building = building;

    }

    @Override
    public void triangulateAndTexturize() {

        primitiveBuffer = new PrimitiveBuffer();
        building.renderTo(primitiveBuffer);

        elements = new ArrayList<>();
        Set<Material> materials = primitiveBuffer.getMaterials();
        for (Material m : materials) {
            Collection<Primitive> primitives = primitiveBuffer.getPrimitives(m);
            elements.add(new WorldElement(OsmUtil.getNameFromOsm(building.getArea().getTags()), m, primitives));
        }
    }

    @Override
    public List<WorldElement> getWorldElements() {
        return elements;
    }

    @Override
    public void adjustElevation(double baseelevation) {
        for (WorldElement we : elements) {
            for (int i = 0; i < we.vertexData.vertices.size(); i++) {
                Coordinate v = we.vertexData.vertices.get(i);
                if (Double.isNaN(v.z)) {
                    v.z = baseelevation;
                } else {
                    v.z += baseelevation;
                }
            }
        }
    }

    @Override
    public String getName() {
        String name = building.getArea().getOsmObject().tags.getValue("name");
        return name;
    }


    public WorldElement getWorldElementByMaterialName(String matname) {
        for (WorldElement we : elements) {
            if (we.material.getName().equals(matname)) {
                return we;
            }
        }
        return null;
    }

    public BuildingModule.Building getBuilding() {
        return building;
    }

    /*public WorldElement getRenderedBuilding() {
        return building;
    }*/
}
