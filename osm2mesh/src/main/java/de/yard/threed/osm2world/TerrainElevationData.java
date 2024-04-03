package de.yard.threed.osm2world;

import java.io.IOException;
import java.util.Collection;

/**
 * a source of terrain elevation data. Implementations may range from raster
 * data such as SRTM to sparsely distributed points with known elevation.
 */
public interface TerrainElevationData {

	Collection<VectorXYZ> getSites(double minLon, double minLat,
			double maxLon, double maxLat) throws IOException;

	Collection<VectorXYZ> getSites(MapData mapData) throws IOException;
	
}
