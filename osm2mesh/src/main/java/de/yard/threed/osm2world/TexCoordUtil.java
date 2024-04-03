package de.yard.threed.osm2world;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * utility class for texture coordinate calculation
 */
public final class TexCoordUtil {
    static Logger logger = Logger.getLogger(TexCoordUtil.class.getName());

    /** prevents instantiation */
	private TexCoordUtil() {}
	
	/**
	 * calculates the texture coordinate lists based on the
	 * {@link TexCoordFunction} associated with each {@link TextureData} layer
	 */
	public static final List<List<VectorXZ>> texCoordLists(
			List<VectorXYZ> vs, Material material,
			TexCoordFunction defaultCoordFunction) {
		
		List<TextureData> textureDataList = material.getTextureDataList();
		//logger.debug("texCoordLists: material="+material.name+", textureDataList.size="+textureDataList.size());
		
		if (textureDataList.size() == 0) {
			
			return emptyList();
			
		} else if (textureDataList.size() == 1) {
			
			TextureData textureData = textureDataList.get(0);
			TexCoordFunction coordFunction = textureData.coordFunction;
			if (coordFunction == null) { coordFunction = defaultCoordFunction; }
			//logger.debug("texCoordLists: material="+material.name+", coordFunction="+coordFunction);
			return singletonList(coordFunction.apply(vs, textureData));
			
		} else {
			
			List<List<VectorXZ>> result = new ArrayList<List<VectorXZ>>();
			
			for (TextureData textureData : textureDataList) {
				
				TexCoordFunction coordFunction = textureData.coordFunction;
				if (coordFunction == null) { coordFunction = defaultCoordFunction; }
				
				result.add(coordFunction.apply(vs, textureData));
				
			}
			
			return result;
			
		}
		
	}
	
	/**
	 * equivalent of {@link #texCoordLists(List, Material, TexCoordFunction)}
	 * for a collection of triangle objects.
	 */
	public static final List<List<VectorXZ>> triangleTexCoordLists(
			Collection<TriangleXYZ> triangles, Material material,
			TexCoordFunction defaultCoordFunction) {
		
		List<VectorXYZ> vs = new ArrayList<VectorXYZ>(triangles.size() * 3);
		
		for (TriangleXYZ triangle : triangles) {
			vs.add(triangle.v1);
			vs.add(triangle.v2);
			vs.add(triangle.v3);
		}
		
		return texCoordLists(vs, material, defaultCoordFunction);
		
	}
	
}
