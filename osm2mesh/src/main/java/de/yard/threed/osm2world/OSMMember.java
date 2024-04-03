package de.yard.threed.osm2world;

public class OSMMember {
	
	static final boolean useDebugLabels = true;
	
	public final String role;
	public final OSMElement member;
	
	public OSMMember(String role, OSMElement member) {
		assert role != null && member != null;
		this.role = role;
		this.member = member;
	}
	
	@Override
	public String toString() {
		return role + ":" + member;
	}
	
}