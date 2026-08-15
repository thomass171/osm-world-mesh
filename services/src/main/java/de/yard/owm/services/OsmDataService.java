package de.yard.owm.services;

import de.yard.owm.misc.GeneralOwmException;
import de.yard.owm.services.util.WellKnownMesh;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OsmDataService {

    @Value("${owm.osm.datapath:.}")
    private String osmDataPath;

    public String loadXml(WellKnownMesh wellKnownMesh, String fileName) throws IOException {
        Path path = getOsmDataPath().resolve(wellKnownMesh + "/" + fileName);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public List<String> findDatasetsByWellKnownMesh(WellKnownMesh wellKnownMesh) throws GeneralOwmException {
        Path path = getOsmDataPath().resolve(wellKnownMesh.name());
        try {
            return Files.list(path)
                    .map(e -> e.getFileName().toString())
                    .filter(filename -> filename.endsWith(".osm.xml"))
                    .collect(Collectors.toUnmodifiableList());
        } catch (IOException e) {
            throw new GeneralOwmException(e.getMessage());
        }
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

    private Path getOsmDataPath() {

        Path workingDir;

        if (osmDataPath.startsWith("/")) {
            workingDir = Paths.get(osmDataPath);
        } else {
            Path userDir = Paths.get(System.getProperty("user.dir"));
            workingDir = userDir.resolve(osmDataPath);
        }
        if (!Files.exists(workingDir)) {
            log.error("Does not exist: {}", workingDir);
        }
        return workingDir;
    }


}

