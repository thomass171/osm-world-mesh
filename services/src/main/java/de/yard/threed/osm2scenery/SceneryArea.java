package de.yard.threed.osm2scenery;

/**
 * 27.7.18: Moved to AreaSceneObject. Obwohl das eigentlich was anderes ist, denn es kann mehrere OSM Objekte darstellen.
 * 
 * Created on 11.07.18.
 */

/*public class SceneryArea {
    Logger logger = Logger.getLogger(SceneryArea.class);
    //Polygon statt Geometry wegen möglicher Holes. Holes lassen sich nie vermeiden, die können immer beim union entstehen.
    private Polygon poly;
    List<Long> osmIds = new ArrayList<>();
    boolean wasmerged = false;
    String creatortag;
    Material material;
    public VertexData vertexData;
    public boolean trifailed=false;

    /*public SceneryArea(String creatortag, SimplePolygonXZ polygon, Material material, long osmId) {
        poly = JTSConversionUtil.polygonXZToJTSPolygon(polygon);
        osmIds.add(osmId);
        this.creatortag = creatortag;
        this.material = material;
        validate();
    }* /

    public SceneryArea(String creatortag, Polygon polygon, Material material, long osmId) {
        poly = polygon;
        osmIds.add(osmId);
        this.creatortag = creatortag;
        this.material = material;
        validate();
    }
    
    /*public SceneryArea(Polygon polygon) {
        poly = polygon;
        validate();
    }* /

    /**
     * Sollte nur gemacht werden, wenn die beiden auch intersecten.
     * 24.7.18: Doof dass leere entstehen. Die werden spaeter aber entfernt.
     * @param 
     */
    /*Komplikationen? public void merge(SceneryArea area) {
        if (!poly.intersects(area.poly)){
            logger.warn("merge: no intersection");
        }
        // Evtl. liefert der union() ein MultiPolygon
        poly = (Polygon) poly.union(area.poly);
        osmIds.addAll(area.osmIds);
        //Aus der gemrgten area den Inhalt entfernen
        area.osmIds.clear();
        area.poly = null;
        area.wasmerged = true;
    }* /

    public List<Long> getOsmIds() {
        return osmIds;
    }

    /**
     * zuschneiden auf die GridBounds. Wenns dumm laeuft zerschneidet das die area. Erstmal ignorieren. TODO
     *
     * @param gridbounds
     * /
    public void cut(Geometry gridbounds) {
        Geometry cut = poly.intersection(gridbounds);
        if (!(cut instanceof Polygon)) {
            logger.error("area cut by grid bounds. ignored.");
            return;
        }
        poly = (Polygon) cut;
        //validate();
    }

    /**
     * Dabei werden auch die Vertices final festgelegt, damit die Zuordnung passt.
     * Und wenn ich grad dabei bin, auch triangulate.
     * Erst triangluate und dann texturieren, denn triangulate kann weitere Vertices anlegen.
     * /
    public void texturize() {
        if (material==null){
            //Sonderlocke, texutils brauchen material
            return;
        }
        List<VectorXZ> uvs;
        //Coordinate[] vertices;
        int[] indices;
        //vertices = poly.getCoordinates();
        vertexData = TextureUtil.triangulate(null,poly);
        if (vertexData==null){
            logger.error("Triangulation failed for area "+getOsmOrigin());
            trifailed=true;
            return;
        }
        
        uvs = TextureUtil.texturizePolygon(poly, vertexData.vertices, material);
        
        vertexData.uvs = uvs;
    }

    
    void validate(){
        if (poly.getCoordinates().length < 4){
            //kein warn sondern error, weil ja nichts erzeugt wird
            //es kann zwischenzeitlich durcuas leere geben. Die werden nachher entfernt.
            //logger.error("SceneryArea:inconsistent? empty polygon");
        }

    }

    public Polygon getPolygon() {
        return poly;
    }

    public boolean isEmpty() {
        return poly.isEmpty();
    }
}*/
