package de.yard.threed.osm2graph.backup;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomass on 06.02.17.
 */
public class Util {
    public static BufferedImage loadImage(File file){
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * gibts das nicht bei Apachhe?
     * @param arr
     * @param <T>
     * @return
     */
    public static <T> List<T> toList(T... arr) {
        List<T> list = new ArrayList<T>();
        for (T elt : arr) list.add(elt);
        return list;
    }
}
