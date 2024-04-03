package de.yard.threed.osm2scenery.modules;

import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.scenery.FixedWidthProvider;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2scenery.util.TagFilter;
import de.yard.threed.osm2world.AbstractAreaWorldObject;
import de.yard.threed.osm2world.EleConstraintEnforcer;
import de.yard.threed.osm2world.GroundState;
import de.yard.threed.osm2world.JunctionNodeWorldObject;
import de.yard.threed.osm2world.MapArea;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.MapWay;
import de.yard.threed.osm2world.MapWaySegment;
import de.yard.threed.osm2world.NetworkAreaWorldObject;
import de.yard.threed.osm2world.OsmOrigin;
import de.yard.threed.osm2world.PolylineXZ;
import de.yard.threed.osm2world.RenderableToAllTargets;
import de.yard.threed.osm2world.ShapeXZ;
import de.yard.threed.osm2world.Tag;
import de.yard.threed.osm2world.TagGroup;
import de.yard.threed.osm2world.Target;
import de.yard.threed.osm2world.TerrainBoundaryWorldObject;
import de.yard.threed.osm2world.TriangleXYZ;
import de.yard.threed.osm2world.VectorXYZ;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.osm2world.WorldModuleParseUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Iterables.any;
import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.RIVER;
import static de.yard.threed.osm2world.Materials.*;
import static de.yard.threed.osm2world.NamedTexCoordFunction.GLOBAL_X_Z;
import static de.yard.threed.osm2world.Predicates.hasType;
import static de.yard.threed.osm2world.TexCoordUtil.texCoordLists;
import static de.yard.threed.osm2world.TexCoordUtil.triangleTexCoordLists;
import static de.yard.threed.osm2world.VectorXYZ.Y_UNIT;
import static java.util.Collections.nCopies;


/**
 * Fuer flowing water. Based on OSM2Worlds WaterModule.
 * (Lakes are covered by Surface).
 * <p>
 * 20.4.19: Doch auch für z.B. Lakes, halt alles, was zum Kontext Gewässer gehört.
 * 27.5.19: Darum umbenannt RiverModule->WaterModule
 */
public class WaterModule extends SceneryModule {
    List<Waterway> rivers = new ArrayList<>();
    //TODO: add canal, ditch, drain

    private static final Tag WATER_TAG = new Tag("natural", "water");
    private static final Tag RIVERBANK_TAG = new Tag("waterway", "riverbank");

    private static final Map<String, Float> WATERWAY_WIDTHS;

    static {
        WATERWAY_WIDTHS = new HashMap<String, Float>();
        //20.5.19: Why 3? Appears too small. Mal etwas vergrössert.
        WATERWAY_WIDTHS.put("river", 5f/*3f*/);
        WATERWAY_WIDTHS.put("stream", 1.5f/*0.5f*/);
        WATERWAY_WIDTHS.put("canal", 2f);
        WATERWAY_WIDTHS.put("ditch", 1f);
        WATERWAY_WIDTHS.put("drain", 1f);
    }

    //TODO: apply to isType almost always the same! create a superclass handling this!

    @Override
    public SceneryObjectList applyTo(MapData mapData) {
        SceneryObjectList rivers = new SceneryObjectList();

        TagFilter tagfilter = getTagFilter("tagfilter");
        for (MapWay/*Segment*/ line : mapData.getMapWays()/*Segments()*/) {
            for (String value : WATERWAY_WIDTHS.keySet()) {
                if (line.getTags().contains("waterway", value) && tagfilter.isAccepted(line.getTags())) {
                    Waterway waterway = new Waterway(line, SceneryContext.getInstance());
                    //line.addRepresentation(waterway);
                    rivers.add(waterway);
                }
            }
        }

        for (MapNode node : mapData.getMapNodes()) {

            int connectedRivers = 0;

            for (MapWaySegment line : node.getConnectedWaySegments()) {
                if (any(line.getRepresentations(), hasType(Waterway.class))) {
                    connectedRivers += 1;
                }
            }

            if (connectedRivers > 2) {
                node.addRepresentation(new RiverJunction(node));
            }

        }

        for (MapArea area : mapData.getMapAreas()) {
            if (area.getTags().contains(WATER_TAG)
                    || area.getTags().contains(RIVERBANK_TAG)) {
                area.addRepresentation(new Water(area));
            }
            if (area.getTags().contains("amenity", "fountain")) {
                area.addRepresentation(new AreaFountain(area));
            }
        }
        return rivers;
    }

    public static class Waterway extends SceneryWayObject {

        public Waterway(MapWay/*Segment*/ line, SceneryContext sceneryContext) {
            super("River", line, WATER, RIVER, new FixedWidthProvider(getWidth(line.getTags())), sceneryContext);
            //16.8.18 super.createPolygon(getWidth());
        }
		
		/*@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {
			
			super.defineEleConstraints(enforcer);
			
			/* enforce downhill flow * /
			
			if (!segment.getTags().containsKey("incline")) {
				enforcer.requireIncline(MAX, 0, getCenterlineEleConnectors());
			}
			
		}*/

        public static float getWidth(TagGroup tags) {
            return WorldModuleParseUtil.parseWidth(tags, WATERWAY_WIDTHS.get(tags.getValue("waterway")));
        }
		
		/*@Override
		public PolygonXYZ getOutlinePolygon() {
			if (isContainedWithinRiverbank()) {
				return null;
			} else {
				return super.getOutlinePolygon();
			}
		}
		
		@Override
		public SimplePolygonXZ getOutlinePolygonXZ() {
			if (isContainedWithinRiverbank()) {
				return null;
			} else {
				return super.getOutlinePolygonXZ();
			}
		}*/
		
		/*@Override
		public void renderTo(Target<?> target) {
			
			//note: simply "extending" a river cannot work - unlike streets -
			//      because there can be islands within the riverbank polygon.
			//      That's why rivers will be *replaced* with Water areas instead.
			
			/* only draw the river if it doesn't have a riverbank * /
			
			//TODO: handle case where a river isType completely within riverbanks, but not a *single* riverbank
						
			if (! isContainedWithinRiverbank()) {
				
				List<VectorXYZ> leftOutline = getOutline(false);
				List<VectorXYZ> rightOutline = getOutline(true);
				
				List<VectorXYZ> leftWaterBorder = createLineBetween(
						leftOutline, rightOutline, 0.05f);
				List<VectorXYZ> rightWaterBorder = createLineBetween(
						leftOutline, rightOutline, 0.95f);
				
				modifyLineHeight(leftWaterBorder, -0.2f);
				modifyLineHeight(rightWaterBorder, -0.2f);
	
				List<VectorXYZ> leftGround = createLineBetween(
						leftOutline, rightOutline, 0.35f);
				List<VectorXYZ> rightGround = createLineBetween(
						leftOutline, rightOutline, 0.65f);
				
				modifyLineHeight(leftGround, -1);
				modifyLineHeight(rightGround, -1);
				
				/* render ground * /
				
				@SuppressWarnings("unchecked") // generic vararg isType intentional
				List<VectorXYZList> strips = asList(
					createTriangleStripBetween(
							leftOutline, leftWaterBorder),
					createTriangleStripBetween(
							leftWaterBorder, leftGround),
					createTriangleStripBetween(
							leftGround, rightGround),
					createTriangleStripBetween(
							rightGround, rightWaterBorder),
					createTriangleStripBetween(
							rightWaterBorder, rightOutline)
				);
				
				for (VectorXYZList strip : strips) {
					target.drawTriangleStrip(TERRAIN_DEFAULT, strip,
						texCoordLists(strip.vs, TERRAIN_DEFAULT, GLOBAL_X_Z),null);
				}
				
				/* render water * /
				
				VectorXYZList vs = createTriangleStripBetween(
						leftWaterBorder, rightWaterBorder);
				
				target.drawTriangleStrip(WATER, vs,
						texCoordLists(vs.vs, WATER, GLOBAL_X_Z),null);
				
			}
			
		}*/

		/*private boolean isContainedWithinRiverbank() {
			boolean containedWithinRiverbank = false;
			
			for (MapOverlap<?,?> overlap : segment.getOverlaps()) {
				if (overlap.getOther(segment) instanceof MapArea) {
					MapArea area = (MapArea)overlap.getOther(segment);
					if (area.getPrimaryRepresentation() instanceof Water &&
							area.getPolygon().contains(segment.getLineSegment())) {
						containedWithinRiverbank = true;
						break;
					}
				}
			}
			return containedWithinRiverbank;
		}

		private static void modifyLineHeight(List<VectorXYZ> leftWaterBorder, float yMod) {
			for (int i = 0; i < leftWaterBorder.size(); i++) {
				VectorXYZ v = leftWaterBorder.get(i);
				leftWaterBorder.set(i, v.y(v.y+yMod));
			}
		}*/

    }

    public static class RiverJunction
            extends JunctionNodeWorldObject
            implements TerrainBoundaryWorldObject, RenderableToAllTargets {

        public RiverJunction(MapNode node) {
            super(node);
        }

        @Override
        public GroundState getGroundState() {
            return GroundState.ON;
        }

        @Override
        public void renderTo(Target<?> target) {

            //TODO: check whether it's within a riverbank (as with Waterway)

            List<VectorXYZ> vertices = getOutlinePolygon().getVertices();

            target.drawConvexPolygon(WATER, vertices,
                    texCoordLists(vertices, WATER, GLOBAL_X_Z));

            //TODO: only cover with water to 0.95 * distance to center; add land below

        }

    }

    public static class Water extends NetworkAreaWorldObject
            implements RenderableToAllTargets, TerrainBoundaryWorldObject {

        //TODO: only cover with water to 0.95 * distance to center; add land below.
        // possible algorithm: for each node of the outer polygon, check whether it
        // connects to another water surface. If it doesn't move it inwards,
        // where "inwards" isType calculated based on the two adjacent polygon segments.

        public Water(MapArea area) {
            super(area);
        }

        @Override
        public GroundState getGroundState() {
            return GroundState.ON;
        }

        @Override
        public void defineEleConstraints(EleConstraintEnforcer enforcer) {
            enforcer.requireSameEle(getEleConnectors());
        }

        @Override
        public void renderTo(Target<?> target) {
            Collection<TriangleXYZ> triangles = getTriangulation();
            target.drawTriangles(WATER, triangles,
                    triangleTexCoordLists(triangles, WATER, GLOBAL_X_Z), new OsmOrigin("Water", area, getOutlinePolygonXZ()));
        }

    }

    private static class AreaFountain extends AbstractAreaWorldObject
            implements RenderableToAllTargets, TerrainBoundaryWorldObject {

        public AreaFountain(MapArea area) {
            super(area);
        }

        @Override
        public GroundState getGroundState() {
            return GroundState.ON;
        }

        @Override
        public void renderTo(Target<?> target) {

            /* render water */

            Collection<TriangleXYZ> triangles = getTriangulation();
            target.drawTriangles(PURIFIED_WATER, triangles,
                    triangleTexCoordLists(triangles, PURIFIED_WATER, GLOBAL_X_Z), new OsmOrigin("AreaFountain", area, getOutlinePolygonXZ()));

            /* render walls */

            double width = 0.1;
            double height = 0.5;

            ShapeXZ wallShape = new PolylineXZ(
                    new VectorXZ(+width / 2, 0),
                    new VectorXZ(+width / 2, height),
                    new VectorXZ(-width / 2, height),
                    new VectorXZ(-width / 2, 0)
            );

            List<VectorXYZ> path = getOutlinePolygon().getVertexLoop();

            target.drawExtrudedShape(CONCRETE, wallShape, path,
                    nCopies(path.size(), Y_UNIT), null, null, null);

        }

    }
}
