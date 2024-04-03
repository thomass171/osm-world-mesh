package de.yard.threed.osm2world;


import java.util.Collections;
import java.util.List;

public class OSMWay extends OSMElement {
	
	private final List<OSMNode> nodes;
	
	public OSMWay(TagGroup tags, long id, List<OSMNode> nodes) {
		super(tags, id);
		for (OSMNode node : nodes) assert node != null;
		this.nodes = nodes;
	}
	
	public boolean isClosed() {
		return nodes.size() > 0 &&
			nodes.get(0).equals(nodes.get(nodes.size()-1));
	}

	/**
	 * Ein Way der auf sich selber beginnt/endet. Looks like a "P".
	 * Return index of inner node, negative if its the end node.
	 *
	 * @return
	 */
	public Integer isP() {
		for (int i=1;i<nodes.size()-1;i++){
			if (nodes.get(i).equals(nodes.get(nodes.size()-1))){
				//end on itself
				return -i;
			}
			if (nodes.get(i).equals(nodes.get(0))){
				//starts on itself
				return i;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "w" + id;
	}

	public List<OSMNode> getNodes(){
		return Collections.unmodifiableList(nodes);
	}

	public void addNode(OSMNode node){
		nodes.add(node);
	}

	public void addNode(int index, OSMNode node){
		nodes.add(index,node);
	}

	public OSMNode removeNode(int index){
		return nodes.remove(index);
	}
}