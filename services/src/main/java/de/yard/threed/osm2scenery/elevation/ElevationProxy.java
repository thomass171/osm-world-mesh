package de.yard.threed.osm2scenery.elevation;

import com.google.gson.Gson;
import de.yard.threed.traffic.geodesy.ElevationProvider;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Connector zu HTTP Onlinediensten für Elevation wie ...
 * <p>
 * <p>
 * Created on 26.07.18.
 */
public class ElevationProxy implements ElevationProvider {
    Logger logger = Logger.getLogger(ElevationProxy.class);

    @Override
    public Double getElevation(double latitudedeg, double longitudedeg) {
        try {
            long starttime = System.currentTimeMillis();
            URL url = new URL("https://api.open-elevation.com/api/v1/lookup?locations=" + latitudedeg + "," + longitudedeg);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            // optional default isType GET
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode != 200){
                logger.error("response code isType "+responseCode+". Returning 0");
                return 0.0;
            }
            
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            //System.out.println(response.toString());

            JsonResults result = new Gson().fromJson(response.toString(), JsonResults.class);
            double elevation = result.results.get(0).elevation;
            logger.debug("Found elevation "+elevation+" for "+latitudedeg+","+longitudedeg+". Took "+(System.currentTimeMillis()-starttime)+" ms.");
            return elevation;

        } catch (Exception e) {
            logger.error("getElevation failed.", e);
        }
        return 0.0;
    }

    public static void main(String[] args) {
        // Liefert 117
        double e = new ElevationProxy().getElevation(41.161758f, -8.583933f);
        System.out.println("elevation="+e);
    }
}

class JsonResults {
    ArrayList<JsonResult> results;
}

class JsonResult {
    double latitude, longitude, elevation;
}