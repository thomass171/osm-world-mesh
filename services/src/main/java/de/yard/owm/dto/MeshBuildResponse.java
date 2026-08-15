package de.yard.owm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Maybe MeshResponse is sufficient for now
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeshBuildResponse extends BaseResponse {
    String name;
    List<PolygonResponse> polygons;


    public static MeshBuildResponse buildFromTile(/*Tile tile*/){
        MeshBuildResponse response=new MeshBuildResponse();
     /*   response.setName(tile.getName());
        Polygon<LatLon> outline = tile.getOutline();
        if (outline != null){
            response.setPolygon(new PolygonResponse());
            for (int i=0;i<outline.getPointCount();i++){
                response.getPolygon().points.add(WebLatLon.buildFromLatLon(outline.getPoint(i)));
            }
            response.getPolygon().setClosed(outline.closed);
        }*/
        return  response;
    }
}
