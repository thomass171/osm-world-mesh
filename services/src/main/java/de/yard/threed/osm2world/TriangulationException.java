package de.yard.threed.osm2world;

/**
 * error caused by an unsuccessful triangulation attempt
 */
public class TriangulationException extends Exception {
	
	public TriangulationException(String message, Throwable cause) {
		super(message, cause);
	}

	public TriangulationException(String message) {
		super(message);
	}

	public TriangulationException(Throwable cause) {
		super(cause);
	}
	
	
	
}
