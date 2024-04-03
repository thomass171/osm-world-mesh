package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;

public interface MeshFactory {
   MeshNode  buildMeshNode(Coordinate coordinate);
   MeshLine buildMeshLine(Coordinate[] coordinates, LineString line);

   public static MeshLine buildMeshLine(Coordinate[] coordinates) {
      LineString line = JtsUtil.createLine(coordinates);
      if (line == null) {
         //already logged
         return null;
      }
      return TerrainMesh.meshFactoryInstance.buildMeshLine(coordinates, line);
   }

}
