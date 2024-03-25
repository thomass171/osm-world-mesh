package de.yard.threed.osm2world;


import de.yard.threed.core.LatLon;

public class OSMNode extends OSMElement {
	public  double lat;
	public /*final*/ double lon;
	
	public OSMNode(double lat, double lon, TagGroup tags, long id) {
		super(tags, id);
		this.lat = lat;
		this.lon = lon;
	}
	
	@Override
	public String toString() {
		return "n" + id;
	}

	public void setLoc(LatLon latlon){
		lat=latlon.getLatDeg().getDegree();
		lon=latlon.getLonDeg().getDegree();
	}
}