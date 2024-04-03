package de.yard.threed.osm2world;



import java.util.ArrayList;
import java.util.List;

public class OSMRelation extends OSMElement {
		
	public final List<OSMMember> relationMembers;
		// content added after constructor call
	
	public OSMRelation(TagGroup tags, long id, int initialMemberSize) {
		super(tags, id);
		this.relationMembers =
			new ArrayList<OSMMember>(initialMemberSize);
	}
	
	@Override
	public String toString() {
		return "r" + id;
	}
	
}