package de.yard.threed.osm2scenery.modules;

import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.scenery.FixedWidthProvider;
import de.yard.threed.osm2scenery.scenery.SceneryWayObject;
import de.yard.threed.osm2world.*;

import java.util.List;

import static com.google.common.collect.Iterables.any;
import static de.yard.threed.osm2graph.osm.MapDataHelper.getOutlinePolygon;
import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.RAILWAY;
import static de.yard.threed.osm2world.Materials.RAIL;
import static de.yard.threed.osm2world.NamedTexCoordFunction.GLOBAL_X_Z;
import static de.yard.threed.osm2world.Predicates.hasType;
import static de.yard.threed.osm2world.TexCoordUtil.texCoordLists;
import static de.yard.threed.osm2world.WorldModuleParseUtil.parseInt;
import static java.util.Arrays.asList;


/**
 * adds rails to the world
 */
public class RailwayModule extends SceneryModule {

	/** accepted values of the railway key */
	private static final List<String> RAILWAY_VALUES = asList(
			"rail", "light_rail", "tram", "subway", "disused");

	@Override
	public SceneryObjectList applyTo(MapData grid) {
		SceneryObjectList rails = new SceneryObjectList();

		for (MapWay/*Segment*/ segment : grid.getMapWays/*Segments*/()) {
			if (segment.getTags().containsAny("railway", RAILWAY_VALUES)) {
				//segment.addRepresentation(new Rail(segment));
				rails.add(new Rail(segment));
			}
		}
		
		//TODO: the following for loop isType copied from water module and should be in a common superclass
		for (MapNode node : grid.getMapNodes()) {
			
			int connectedRails = 0;
			
			for (MapWaySegment line : node.getConnectedWaySegments()) {
				if (any(line.getRepresentations(), hasType(Rail.class))) {
					connectedRails += 1;
				}
			}
			
			if (connectedRails > 2) {
				// node.addRepresentation(new RailJunction(node));
				// TODO: reactivate after implementing proper rendering for rail junctions
			}
			
		}
		return rails;
	}
	
	public static class Rail extends SceneryWayObject/*AbstractNetworkWaySegmentWorldObject
		implements RenderableToAllTargets, TerrainBoundaryWorldObject*/ {
		
		private static final int DEFAULT_GAUGE_MM = 1435;
	
		/** by how much the ballast goes beyond the ends of the sleeper (on each side) */
		private static final float GROUND_EXTRA_WIDTH = 0.2f;
		
		/** by how much the sleeper goes beyond the rail (on each side) */
		private static final float SLEEPER_EXTRA_WIDTH = 0.5f;
		
		private static final float SLEEPER_LENGTH = 0.26f;
		private static final float SLEEPER_HEIGHT = 0.16f * 0.4f; //extra factor to model sinking into the ballast
		
		private static final float SLEEPER_DISTANCE = 0.6f + SLEEPER_LENGTH;
		
		private static final float RAIL_HEAD_WIDTH = 0.067f; //must match RAIL_SHAPE
		private static final ShapeXZ RAIL_SHAPE;
		
		static {
			
			List<VectorXZ> railShape = asList(
					new VectorXZ(-0.45, 0), new VectorXZ(-0.1, 0.1),
					new VectorXZ(-0.1, 0.5), new VectorXZ(-0.25, 0.55),
					new VectorXZ(-0.25, 0.75), new VectorXZ(+0.25, 0.75),
					new VectorXZ(+0.25, 0.55), new VectorXZ(+0.1, 0.5),
					new VectorXZ(+0.1, 0.1), new VectorXZ(+0.45, 0));
			
			for (int i=0; i < railShape.size(); i++) {
				VectorXZ v = railShape.get(i);
				v = v.mult(0.1117f);
				v = new VectorXZ(-v.x, v.z + SLEEPER_HEIGHT);
				railShape.set(i, v);
			}
			
			RAIL_SHAPE = new PolylineXZ(railShape);
			
		}
		
		final float gaugeMeters;
		final float railDist;
		
		final float sleeperWidth;
		final float groundWidth;
		
		public Rail(MapWay/*Segment*/ segment) {
			
			super("Railway",segment, RAIL,RAILWAY,new FixedWidthProvider(5/*groundwidht*/));
			
			gaugeMeters = parseInt(segment.getTags(), DEFAULT_GAUGE_MM, "gauge") / 1000.0f;
			railDist = gaugeMeters + 2 * (0.5f * RAIL_HEAD_WIDTH);
			
			sleeperWidth = gaugeMeters + 2 * RAIL_HEAD_WIDTH + 2 * SLEEPER_EXTRA_WIDTH;
			groundWidth = sleeperWidth + 2 * GROUND_EXTRA_WIDTH;
            //16.8.18 super.createPolygon(7);//TODO /*getWidth());
		}

		/*@Override
		public GroundState getGroundState() {
			
			if (segment.getTags().contains("railway", "subway")
					&& !segment.getTags().contains("tunnel", "no")){
				return GroundState.BELOW;
			}
			else if ( segment.getTags().contains("tunnel", "yes"))
			{
				return GroundState.BELOW;
			}
			
			return super.getGroundState();
			
		}
		
		@Override
		public void renderTo(Target<?> target) {

			/* draw ground * /

			VectorXYZList groundVs = WorldModuleGeometryUtil.createTriangleStripBetween(
					getOutline(false), getOutline(true));
			
			target.drawTriangleStrip(Materials.RAIL_BALLAST_DEFAULT, groundVs,
					texCoordLists(groundVs.vs, Materials.RAIL_BALLAST_DEFAULT, GLOBAL_X_Z),null);
			
			
			/* draw rails * /

			@SuppressWarnings("unchecked")
			List<VectorXYZ>[] railLines = new List[2];
			
			railLines[0] = WorldModuleGeometryUtil.createLineBetween(
					getOutline(false), getOutline(true),
					((groundWidth - railDist) / groundWidth) / 2);

			railLines[1] = WorldModuleGeometryUtil.createLineBetween(
					getOutline(false), getOutline(true),
					1 - ((groundWidth - railDist) / groundWidth) / 2);

			for (List<VectorXYZ> railLine : railLines) {
				
				target.drawExtrudedShape(RAIL_DEFAULT, RAIL_SHAPE, railLine,
						nCopies(railLine.size(), Y_UNIT), null, null, null);
				
			}
			
			
			/* draw railway ties/sleepers * /
						
			List<VectorXYZ> sleeperPositions = equallyDistributePointsAlong(
					SLEEPER_DISTANCE, false, getCenterline());
			
			for (VectorXYZ sleeperPosition : sleeperPositions) {
				
				target.drawBox(Materials.RAIL_SLEEPER_DEFAULT,
						sleeperPosition, segment.getDirection(),
						SLEEPER_HEIGHT, sleeperWidth, SLEEPER_LENGTH);
				
			}
			
		}*/


		
	}
	
	public static class RailJunction
		extends JunctionNodeWorldObject
		implements RenderableToAllTargets, TerrainBoundaryWorldObject {
		
		public RailJunction(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			//TODO (code duplication): copied from RoadModule
			GroundState currentGroundState = null;
			checkEachLine: {
				for (MapWaySegment line : this.node.getConnectedWaySegments()) {
					if (line.getPrimaryRepresentation() == null) continue;
					GroundState lineGroundState = line.getPrimaryRepresentation().getGroundState();
					if (currentGroundState == null) {
						currentGroundState = lineGroundState;
					} else if (currentGroundState != lineGroundState) {
						currentGroundState = GroundState.ON;
						break checkEachLine;
					}
				}
			}
			return currentGroundState;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			if (getOutlinePolygon() == null) return;
			
			/* draw ground */

			List<VectorXYZ> vectors = getOutlinePolygon().getVertexLoop();

			Material material = Materials.RAIL_BALLAST_DEFAULT;
			
			target.drawConvexPolygon(material, vectors,
					texCoordLists(vectors, material, GLOBAL_X_Z));

			/* draw connection between each pair of rails */

			/* TODO: use node.getConnectedLines() instead?
			 * (allows access to information from there,
			 *  such as getOutline!)
			 */

			for (int i=0; i<cutCenters.size(); i++) {
				for (int j=0; j<i; j++) {

					/* connect those rails with an obtuse angle between them */


				}
			}
			
		}

	}
	
}
