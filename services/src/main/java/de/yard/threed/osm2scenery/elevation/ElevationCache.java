package de.yard.threed.osm2scenery.elevation;

import de.yard.threed.traffic.geodesy.ElevationProvider;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 26.07.18.
 */
public class ElevationCache implements ElevationProvider {
    Logger logger = Logger.getLogger(ElevationCache.class);

    private static ElevationCache instance = null;
    private ElevationProvider baseelevationProvider;
    private Map<String, Double> cache = new HashMap<>();
    private static String cachefile = "/Users/thomas/Projekte/XXX/osmdata/elevationcache.txt";

    private ElevationCache(ElevationProvider baseelevationProvider) throws IOException {
        this.baseelevationProvider = baseelevationProvider;
        loadCache();
    }


    public static ElevationCache buildInstance(ElevationProvider baseelevationProvider) throws IOException {
        instance = new ElevationCache(baseelevationProvider);
        return instance;
    }


    @Override
    public Double getElevation(double latitudedeg, double longitudedeg) {
        String key=getKey(latitudedeg,longitudedeg);
        Double elevation = cache.get(key);
        if (elevation==null){
            elevation = baseelevationProvider.getElevation(latitudedeg,longitudedeg);
            cache.put(key,elevation);
            saveCache();
        }
        return elevation;
    }

    /**
     * Kein "-" als Separator um Verwirrung mit negativen Werten zu vermeiden.
     * Einfach ein Blank, dann kann ma sofort so speichern. Besser ";" wegen split.
     * @param latitudedeg
     * @param longitudedeg
     * @return
     */
    private String getKey(double latitudedeg, double longitudedeg) {
        return format(latitudedeg) + ";" + format(longitudedeg);
    }

    private String format(double f) {
        return String.format("%.5f", f).replaceAll(",",".");
    }

    private void loadCache() throws IOException {
        File f = new File(cachefile);
        BufferedReader b = new BufferedReader(new FileReader(f));
        String readLine = "";
        while ((readLine = b.readLine()) != null) {
            //System.out.println(readLine);
            String[] parts = readLine.split(" ");
            cache.put(parts[0], Double.parseDouble(parts[1]));
        }
        b.close();
        logger.debug(""+cache.size()+" cache entries loaded.");
    }

    private void saveCache() {
        try {
            PrintWriter writer = new PrintWriter(cachefile, "UTF-8");
            for (String key : cache.keySet()) {
                writer.println("" + key + " " + cache.get(key));
            }
            writer.close();
        }catch (IOException e){
            logger.error("saving cache failed:"+e);
        }
    }
}
