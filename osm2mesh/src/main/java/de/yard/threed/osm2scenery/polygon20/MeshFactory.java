package de.yard.threed.osm2scenery.polygon20;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;

import java.util.List;

public interface MeshFactory {
   MeshNode  buildMeshNode(Coordinate coordinate);
   MeshLine  buildMeshLine(MeshNode from, MeshNode to);
   MeshArea  buildMeshArea();
   OsmNode  buildOsmNode(long osmId);
   OsmWay  buildOsmWay(long osmId, List<Long> osmnodeIds);

   List<MeshLine> buildMeshLines(Coordinate[] coordinates, LineString line);

   void deleteMeshLine(MeshLine line);

   public static List<MeshLine> buildMeshLines(Coordinate[] coordinates) {
      LineString line = JtsUtil.createLine(coordinates);
      if (line == null) {
         //already logged
         return null;
      }
      return TerrainMesh.meshFactoryInstance.buildMeshLines(coordinates, line);
   }


}
