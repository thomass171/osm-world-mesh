package de.yard.threed.osm2scenery.util;

import com.vividsolutions.jts.geom.Polygon;

/**
 * A Polygon, that knows Meta-,Osm, or Mappingdata of its coordinates.
 * And optionally knows a Graph.
 * 16.8.18: Nein, kein Graph. Sonst aber wider aktiviert sowohl für Area base als auch Way based Polygone. Ob Vertexdata auch,
 * muss sich nocht zeigen.
 * <p>
 * Aufgegangen in SceneAreaObject. 16.8.18: wieder etwas aehnlich aktiviert.
 * <p>
 * 10.7.19: TODO: Die MetaData sind echt hinderlich für sowas wie cut/clip
 * <p>
 * Created on 25.07.18.
 */
public class SmartPolygon {
    //26.9.18: Durch den cut kann ein Polygon in mehrere zerfallen
    public Polygon/*[]*/ polygon;
    public PolygonMetadata polygonMetadata;
    //original polygon not cut by grid cell or clip
    public Polygon uncutPolygon;
    //flag for cut to fit tile
   // public boolean wascut = false;
    public boolean trifailed = false;

    /**
     * 23.7.19: Deprecated, um von uncutpolygon und metadaten loszukommen.
     *
     * @param polygon
     * @param polygonMetadata
     */
    @Deprecated
    public SmartPolygon(Polygon polygon, PolygonMetadata polygonMetadata) {
        this.polygon = polygon;//new Polygon[]{polygon};
        this.polygonMetadata = polygonMetadata;
        this.uncutPolygon = polygon;
    }

    public SmartPolygon(Polygon polygon) {
        this.polygon = polygon;//new Polygon[]{polygon};
    }

    public double getArea() {
        double area = 0;
       // for (Polygon p : polygon) {
            area += polygon.getArea();
        //}
        return area;
    }

    /**
     * Returns false in the case of failure.
     */
    /*29.8.19 public boolean replace(int i, Pair<Coordinate, Coordinate> p) {
        if (/*polygon.length != 1 ||* / p==null) {
            throw new RuntimeException("inconsistent");
        }
        Coordinate[] coors = polygon.getCoordinates();
        int len = coors.length;
        // getFirst ist immer right, getSecond left; jeweils ab start.
        coors[i] = p.getFirst();
        coors[len - 2 - i] = p.getSecond();
        if (i == 0) {
            coors[len - 1] = coors[0];
        }
        Polygon newpoly = JtsUtil.GF.createPolygon(coors);
        if (!newpoly.isValid()){
            return false;
        }
        polygon = newpoly;
        return true;
    }*/

    /**
     * Returns false in the case of failure.
     */
   /*29.8.19 public boolean add(int i, Pair<Coordinate, Coordinate> p) {
        /*if (polygon.length != 1) {
            throw new RuntimeException("inconsistent");
        }* /
        ArrayList<Coordinate> coors = new ArrayList<>(Arrays.asList(polygon.getCoordinates()));
        int len = coors.size();
        // getFirst ist immer right, getSecond left; jeweils ab start.
        // erst weiter hinten adden
        coors.add(len - 2 - i+1, p.getSecond());
        coors.add(i, p.getFirst());
        len = coors.size();
        if (i == 0) {
            coors.set(len - 1, coors.get(0));
        }
        Polygon newpoly  = JtsUtil.createPolygon(coors);
        if (!newpoly.isValid()){
            return false;
        }
        polygon = newpoly;
        return true;
    }*/

   /* public SmartPolygon(Polygon polygon, Map<Long, List<VectorXZ>> nodemap) {
        this.polygon = polygon;
        this.nodemap = nodemap;
    }* /

    public SmartPolygon(Polygon polygon,  PolygonMetadata polygonMetadata,List<EleConnectorGroup> eleconnectors) {
        this.polygon = polygon;
        this.polygonMetadata = polygonMetadata;
        this.eleconnectors=eleconnectors;
    }*/

    /*public Coordinate[] getCoordinatesForOsmNode(long osmid) {
        //List<VectorXZ> v = nodemap.get(osmid);
        List<Coordinate> v = polygonMetadata.getCoordinated(osmid);
        if (v == null) {
            return new Coordinate[0];
        }
        Coordinate[] pcoor = polygon.getCoordinates();
        Coordinate[] coor = new Coordinate[v.size()];
        for (int i = 0; i < coor.length; i++) {
            coor[i] = pcoor[JtsUtil.findVertexIndex(/*new Coordinate(v.get(i).getX(), v.get(i).getZ()* /v.get(i), pcoor)];
        }
        return coor;
    }*/

    /**
     * Fuer jeden Polygonpunkt die Elevation eintragen.
     * Dafuer muss ich fuer jeden Polygonpunkt wissen, auf welche OSM Node er zurückgeht.
     * /
     public void fixElevationGroups(ElevationProvider elevationProvider) {
     Coordinate[] coor = polygon.getCoordinates();
     for (Coordinate c:coor){
     MapNode mapNode = polygonMetadata.getOsmId(c);
     if (mapNode != null){
     float elevation = elevationProvider.getElevation((float)mapNode.getOsmNode().lat,(float)mapNode.getOsmNode().lon);
     }
     }
     }*/


}
