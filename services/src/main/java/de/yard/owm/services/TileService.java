package de.yard.owm.services;

import de.yard.threed.core.LatLon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TileService {

    @Value("${terrasync.basedir:.}")
    private String terrasyncbasedir;

    /**
     * Placeholder image for the tile covered by the given bounding box. Just a colored
     * rectangle with the bounding box printed on it, no real tile rendering yet.
     */
    public byte[] buildExampleImage(LatLon min, LatLon max) throws IOException {
        int size = 256;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(120, 170, 220));
        g.fillRect(0, 0, size, size);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, size - 1, size - 1);
        g.drawString(String.format("%.5f,%.5f", min.getLatDeg().getDegree(), min.getLonDeg().getDegree()), 8, size - 24);
        g.drawString(String.format("%.5f,%.5f", max.getLatDeg().getDegree(), max.getLonDeg().getDegree()), 8, size - 8);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

   /* public List<Tile> getTiles() throws IOException {

        Path path = getTerraSyncPath();
        path = path.resolve("Terrain");
        log.debug("Using 'Terrain' from '{}'", path);

        log.info("Tiles from {}", path);
        // getFileName() returns a (Unix)Path, so toString() is needed.
        List<Path> paths = listFiles(path, p -> p.getFileName().toString().endsWith(".stg"));
        paths.forEach(x -> log.debug("path={}", x));
        return paths.stream().map(p -> FgTile.buildFromPath(p)).collect(Collectors.toUnmodifiableList());
    }*/

    /*private Path getTerraSyncPath() {

        Path workingDir;

        if (terrasyncbasedir.startsWith("/")) {
            workingDir = Paths.get(terrasyncbasedir);
        } else {
            Path userDir = Paths.get(System.getProperty("user.dir"));
            workingDir = userDir.resolve(terrasyncbasedir);
        }
        if (!Files.exists(workingDir)){
            log.error("Does not exist: {}", workingDir);
        }
        return workingDir;
    }*/
}

