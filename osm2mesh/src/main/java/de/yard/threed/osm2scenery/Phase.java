package de.yard.threed.osm2scenery;

import org.apache.log4j.Logger;

/**
 * Wohl doch sinnvoll, sowas zu haben.
 * <p>
 * 24.4.19
 */
public enum Phase {
    //CUT ist eigene, weil es so lange läuft
    MAP_DATA(10),
    OBJECTS(20),
    CLASSIFY(23),
    ELEGROUPS(25),
    WAYS(27),
    //CLIPWAYS(28),
    //POLYGONS(30),
    GRIDREARRANGE(29),
    BUILDINGSANDAREAS(31),
    TERRAINMESH(32),
    //CLIP(31),
    //CUT(32),
    SUPPLEMENTS(33),
    BACKGROUND(34),
    DECORATION(35),
    OVERLAPS(37),
    POLYGONSREADY(40),
    TRIANGULATION(50),
    ELEVATION(60);
    
    public int level;
    static Logger logger = Logger.getLogger(Phase.class.getName());

    public static Phase current = null;
    private static long currentPhaseStartmillis = 0;

    Phase(int level) {
        this.level = level;
    }

    public static void updatePhase(Phase phase) {
        if (current != null) {
            logger.info("Completed phase " + current + ". Took " + ((System.currentTimeMillis() - currentPhaseStartmillis) / 1000) + " seconds. Starting "+phase);
        }
        current = phase;
        currentPhaseStartmillis = System.currentTimeMillis();
    }

    public boolean reached() {
        return (Phase.current.level >= this.level);
    }

    public void assertCurrent() {
        if (Phase.current.level != this.level) {
            throw new RuntimeException("invalid phase");
        }
    }


}
