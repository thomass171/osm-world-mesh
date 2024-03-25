package de.yard.threed.osm2scenery.scenery;

/**
 * All volumetric parts created from OSM. (aus TerrainSceneryObject)
 * Work as overlay to terrain.
 *
 * 11.4.19: Was genau ist das?
 * 11.4.19: Das mit Volumen scheint fragwürdig, aber Overlay könnte Sinn machen. Ein Overlay könnte eine
 * Fläche sein, die sich auf andere legt, wie z.B. Brückenoberteile, Fahrbahnmarkierungen. NeeNee, die sind zu unterschiedlich.
 * Fahrahnmarkierungen könnten schon gehen, aber warum sollten die nicht einfach ein Supplement sein.
 * Für Volume gibt es jetzt eine Compoentn.
 * 3.6.19:Kann das weg?
 */
/*public abstract class SceneryVolumeOverlayObject extends SceneryObject {
    Logger logger = Logger.getLogger(SceneryVolumeOverlayObject.class);
    
    public SceneryVolumeOverlayObject(String creatortag,Category category){
        super(creatortag,category);
        isTerrainProvider=false;
    }
}*/
