package de.yard.threed.osm2scenery.modules;

import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.scenery.SceneryAreaObject;
import de.yard.threed.osm2world.MapArea;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.Materials;
import de.yard.threed.osm2world.Tag;
import de.yard.threed.osm2world.TagGroup;
import de.yard.threed.osm2world.TriangleXZ;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.GENERICAREA;

/**
 * Eine Oberfläche (direkt oder indirekt als area in OSM definiert), z.B. (Forests, farmland, lakes, aso.).
 * Kann auch eine Dummyarea sein (Taxiway?)?
 * Wird vorab in den Background eingebaut.
 *
 * adds generic areas with surface information to the world.
 * Is based on surface information on otherwise unknown/unspecified areas.
 *
 * 20.4.19: Wirklich nur für Areas, die sicher nicht in einen höheren Kontext gehören, wie z.B. Lakes (WaterModule), Parkplätze (HighWayModule),
 * Aprons (AerowayModule).
 * 3.6.19: (was ist "höherer Kontext"?).
 */
public class SurfaceAreaModule extends SceneryModule {
    static Logger logger = Logger.getLogger(SurfaceAreaModule.class.getName());

    /**
     * assumptions about default surfaces for certain tags
     */
    private static final Map<Tag, String> defaultSurfaceMap
            = new HashMap<Tag, String>();

    static {
        defaultSurfaceMap.put(new Tag("leisure", "pitch"), "ground");
        defaultSurfaceMap.put(new Tag("landuse", "construction"), "ground");
        defaultSurfaceMap.put(new Tag("golf", "bunker"), "sand");
        defaultSurfaceMap.put(new Tag("golf", "green"), "grass");
        defaultSurfaceMap.put(new Tag("natural", "sand"), "sand");
        defaultSurfaceMap.put(new Tag("natural", "beach"), "sand");
        defaultSurfaceMap.put(new Tag("landuse", "meadow"), "grass");
        defaultSurfaceMap.put(new Tag("landuse", "grass"), "grass");
        defaultSurfaceMap.put(new Tag("natural", "scrub"), "scrub");
        //additional to OSM2World. River duerfte nicht faelschlicherweise erkannt werden, weil er keine Area ist.
        defaultSurfaceMap.put(new Tag("landuse", "farmland"), "farmland");
        defaultSurfaceMap.put(new Tag("natural", "water"), "water");

    }
    
    @Override
    public SceneryObjectList applyTo(MapData grid) {
        SceneryObjectList l = new SceneryObjectList();

        for (MapArea area : grid.getMapAreas()) {

            if (!area.getRepresentations().isEmpty()) return l;

            TagGroup tags = area.getTags();

            if (tags.containsKey("surface")) {
                l.add(new SurfaceArea(area, tags.getValue("surface")));
            } else {

                for (Tag tagWithDefault : defaultSurfaceMap.keySet()) {
                    if (tags.contains(tagWithDefault)) {
                        l.add(new SurfaceArea(
                                area, defaultSurfaceMap.get(tagWithDefault)));
                    }
                }

            }
        }
        return l;
    }

   

    public static class SurfaceArea extends SceneryAreaObject {

        private final String surface;

        private Collection<TriangleXZ> triangulationXZ;

        public SurfaceArea(MapArea area, String surface) {
            super(area,"Area-"+surface, Materials.getSurfaceMaterial(surface),GENERICAREA);
            this.surface = surface;
            cycle = Cycle.GENERICAREA;
        }

        /*@Override
        public void renderTo(Target<?> target) {

            Material material = null;

            if (surface.equals(EMPTY_SURFACE_TAG.value)) {
                material = Materials.TERRAIN_DEFAULT;
            } else {
                material = Materials.getSurfaceMaterial(surface);
            }

            if (material != null) {

                Collection<TriangleXYZ> triangles = getTriangulation();
                target.drawTriangles(material, triangles,
                        triangleTexCoordLists(triangles, material, GLOBAL_X_Z), new OsmOrigin("SurfaceArea",area,getOutlinePolygonXZ()));

            }

        }*/

        /**
         * calculates the true ground footprint of this area by removing
         * area covered by other overlapping features, then triangulates it
         * into counterclockwise triangles.
         */
        /*@Override
        protected Collection<TriangleXZ> getTriangulationXZ() {

            if (triangulationXZ != null) {
                return triangulationXZ;
            }

            boolean isEmptyTerrain = surface.equals(EMPTY_SURFACE_TAG.value);
            
			/* collect the outlines of overlapping ground polygons and other polygons,
             * and EleConnectors within the area * /

            List<SimplePolygonXZ> subtractPolys = new ArrayList<SimplePolygonXZ>();
            List<SimplePolygonXZ> allPolys = new ArrayList<SimplePolygonXZ>();

            List<VectorXZ> eleConnectorPoints = new ArrayList<VectorXZ>();

            for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
                for (WorldObject otherWO : overlap.getOther(area).getRepresentations()) {

                    if (otherWO instanceof TerrainBoundaryWorldObject
                            && otherWO.getGroundState() == GroundState.ON) {

                        if (otherWO instanceof SurfaceArea && !isEmptyTerrain) {
                            // empty terrain has lowest priority
                            continue;
                        }

                        if (overlap.type == MapOverlapType.CONTAIN
                                && overlap.e1 == this.area) {
                            // completely within other element, no ground area left
                            return emptyList();
                        }

                        TerrainBoundaryWorldObject terrainBoundary =
                                (TerrainBoundaryWorldObject) otherWO;

                        SimplePolygonXZ outlinePolygon = terrainBoundary.getOutlinePolygonXZ();

                        if (outlinePolygon != null) {

                            subtractPolys.add(outlinePolygon);
                            allPolys.add(outlinePolygon);

                            for (EleConnector eleConnector : otherWO.getEleConnectors()) {

                                if (!outlinePolygon.getVertexCollection().contains(eleConnector.pos)) {
                                    eleConnectorPoints.add(eleConnector.pos);
                                }

                            }

                        }

                    } else {

                        for (EleConnector eleConnector : otherWO.getEleConnectors()) {

                            if (eleConnector.reference == null) {
							/* workaround to avoid using connectors at intersections,
							 * which might fall on area segments
							 * //TODO cleaner solution
							 * /
                                continue;
                            }

                            eleConnectorPoints.add(eleConnector.pos);
                        }

                        if (otherWO instanceof WorldObjectWithOutline) {

                            SimplePolygonXZ outlinePolygon =
                                    ((WorldObjectWithOutline) otherWO).getOutlinePolygonXZ();

                            if (outlinePolygon != null) {

                                allPolys.add(outlinePolygon);

                            }

                        }

                    }

                }
            }
			
			/* add a grid of points within the area for smoother surface shapes * /

            VectorGridXZ pointGrid = new VectorGridXZ(
                    area.getAxisAlignedBoundingBoxXZ(),
                    EmptyTerrainBuilder.POINT_GRID_DIST);

            for (VectorXZ point : pointGrid) {

                //don't insert if it isType e.g. on top of a tunnel;
                //otherwise there would be no minimum vertical distance

                boolean safe = true;

                for (SimplePolygonXZ polygon : allPolys) {
                    if (polygon.contains(point)) {
                        safe = false;
                        break;
                    }
                }

                if (safe) {
                    eleConnectorPoints.add(point);
                }

            }
			
			/* create "leftover" polygons by subtracting the existing ones * /

            Collection<PolygonWithHolesXZ> polygons;

            if (subtractPolys.isEmpty()) {
				
				/* SUGGEST (performance) handle the common "empty terrain cell"
				 * special case more efficiently, also regarding point raster? * /

                polygons = singleton(area.getPolygon());

            } else {

                polygons = CAGUtil.subtractPolygons(
                        area.getOuterPolygon(), subtractPolys);

            }
						
			/* triangulate, using elevation information from all participants * /

            triangulationXZ = new ArrayList<TriangleXZ>();

            for (PolygonWithHolesXZ polygon : polygons) {

                List<VectorXZ> points = new ArrayList<VectorXZ>();

                for (VectorXZ point : eleConnectorPoints) {
                    if (polygon.contains(point)) {
                        points.add(point);
                    }
                }

                // both methods run into exceptions from time to time.
                boolean usePoly2Tri = Config.getCurrentConfiguration().getBoolean("usePoly2Tri", true);
                boolean succeeded = false;
                if (usePoly2Tri) {
                    try {

                        triangulationXZ.addAll(Poly2TriTriangulationUtil.triangulate(
                                polygon.getOuter(),
                                polygon.getHoles(),
                                Collections.<LineSegmentXZ>emptyList(),
                                points));
                        succeeded = true;
                    } catch (TriangulationException e) {
                        String msg = e.getMessage();
                        if (e.getCause() != null && e.getCause() instanceof NullPointerException) {
                            msg = "NullPointerException";
                        }
                        logger.warn("Poly2Tri exception for " + this + ":" + msg + "... falling back to JTS triangulation.");
                        //e.printStackTrace();
                        //System.err.println("... falling back to JTS triangulation.");
                    }
                }
                if (!succeeded) {
                    triangulationXZ.addAll(JTSTriangulationUtil.triangulate(
                            polygon.getOuter(),
                            polygon.getHoles(),
                            Collections.<LineSegmentXZ>emptyList(),
                            points));
                }
            }

            return triangulationXZ;

        }*/

        /*@Override
        public void defineEleConstraints(EleConstraintEnforcer enforcer) {

            super.defineEleConstraints(enforcer);

            /** add vertical distance to connectors above and below * /

            for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
                for (WorldObject otherWO : overlap.getOther(area).getRepresentations()) {

                    for (EleConnector eleConnector : otherWO.getEleConnectors()) {

                        EleConnector ownConnector = getEleConnectors().getConnector(eleConnector.pos);

                        if (ownConnector == null) continue;

                        if (eleConnector.groundState == ABOVE) {

                            enforcer.requireVerticalDistance(
                                    MIN, 1,
                                    eleConnector, ownConnector); //TODO actual clearing

                        } else if (eleConnector.groundState == BELOW) {

                            enforcer.requireVerticalDistance(
                                    MIN, 10,
                                    ownConnector, eleConnector); //TODO actual clearing

                        }

                    }

                }
            }

        }*/

        /*@Override
        public PolygonXYZ getOutlinePolygon() {
            if (surface.equals(EMPTY_SURFACE_TAG.value)) {
                // avoid interfering with e.g. tree placement
                return null;
            } else {
                return super.getOutlinePolygon();
            }
        }*/

       /* @Override
        public SimplePolygonXZ getOutlinePolygonXZ() {
            if (surface.equals(EMPTY_SURFACE_TAG.value)) {
                // avoid interfering with e.g. tree placement
                return null;
            } else {
                return super.getOutlinePolygonXZ();
            }
        }*/

       /* @Override
        public GroundState getGroundState() {
            if (BridgeModule.isBridge(area.getTags())) {
                return GroundState.ABOVE;
            } else if (TunnelModule.isTunnel(area.getTags())) {
                return GroundState.BELOW;
            } else {
                return GroundState.ON;
            }
        }*/

    }

}
