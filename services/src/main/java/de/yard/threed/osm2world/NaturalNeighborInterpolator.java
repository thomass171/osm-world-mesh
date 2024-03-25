package de.yard.threed.osm2world;



import java.util.Collection;

/**
 * uses natural neighbor interpolation of heights
 */
public class NaturalNeighborInterpolator implements TerrainInterpolator {

	private DelaunayTriangulation triangulation;
	
	@Override
	public void setKnownSites(Collection<VectorXYZ> sites) {
		
		AxisAlignedBoundingBoxXZ boundingBox = new AxisAlignedBoundingBoxXZ(sites);
		boundingBox = boundingBox.pad(100);
		
		triangulation = new DelaunayTriangulation(boundingBox);
		
		int i = 0; //TODO remove
		int total = sites.size();
		long startTime = System.currentTimeMillis();
		
		for (VectorXYZ site : sites) {
			if (++i % 1000 == 0) System.out.println("KS: " + i + "/" + total
					+ " after " + ((System.currentTimeMillis() - startTime) / 1e3));
			triangulation.insert(site);
			
		}
		
	}

	@Override
	public VectorXYZ interpolateEle(VectorXZ pos) {
	
		DelaunayTriangulation.NaturalNeighbors nn = triangulation.probe(pos);
		
		double ele = 0;
		
		for (int i = 0; i < nn.neighbors.length; i++) {
			ele += nn.neighbors[i].y * nn.relativeWeights[i];
		}
		
		return pos.xyz(ele);
		
	}
	
}
