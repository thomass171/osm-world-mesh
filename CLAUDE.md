# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

osm-world-mesh (OWM) builds gapless, overlap-free, seamless 3D terrain meshes from OpenStreetMap data. It started
as a fork of [OSM2World](https://github.com/tordanik/OSM2World) with unneeded components (renderers, etc.) removed,
then diverged significantly: OSM data and derived mesh geometry are now persisted in PostgreSQL and built up
incrementally per named mesh/tile, rather than converted in one in-memory pass. See `README.md` for the full
history/rationale of this evolution — it documents several superseded approaches (GridBoundaries, gridless DB,
current polygon-in-DB approach) that explain *why* the code looks the way it does in places.

## Build system

Multi-module Maven project (parent `pom.xml`, Java 17 for `osm2mesh`/`services`, Java 11 for `traditional`):

- **`osm2mesh`** — core OSM-to-mesh conversion logic, no Spring/DB dependency. Packages under
  `de.yard.threed.osm2graph`, `osm2scenery`, `osm2tex`, `osm2world` (largest, ported from OSM2World), `jts`, `scenery`.
- **`services`** — the main Spring Boot application (`de.yard.owm`), exposing REST endpoints and persisting
  meshes to Postgres. Depends on `osm2mesh`.
- **`traditional`** — deprecated pre-DB code, scheduled for removal (see `traditional/Readme.txt`).

### External dependency requirement

Several dependencies (`de.yard.tcp-22:module-core/module-traffic/module-tools/module-java-common/module-java-native`,
`de.yard.tcp-flightgear:*`) come from **sibling projects that live outside this repo** and must already be
`mvn install`-ed into the local `~/.m2` repository. If a build fails with "could not resolve dependency" for any
`de.yard.tcp-*` artifact, that sibling project needs to be built first — this is not something fixable from within
this repo.

GeoTools artifacts are pulled from the `osgeo` repository (declared in the parent `pom.xml`), not Maven Central.

### Common commands

```bash
# Build everything (from repo root)
mvn install

# Build/test a single module
mvn -pl osm2mesh install
mvn -pl services install

# Run a single test class
mvn -pl services test -Dtest=MeshControllerTest
mvn -pl osm2mesh test -Dtest=SomeTest#someMethod

# Run the services Spring Boot app locally (also settable via bin/launchServer.sh)
bin/launchServer.sh
```

`bin/launchServer.sh` points at a specific dev Postgres instance (`192.168.98.151:5432/worldmesh`, schema
`worldmesh`, user/db `worldmesh`) — a Postgres instance reachable at that address/credentials is required both to
run the service and to run `services` module tests (`services/src/test/resources/application.properties` connects
to the same host, schema `worldmesh_test`). Flyway manages the schema (`services/src/main/resources/db/migration`);
there's currently a single migration, `V1__initial_setup.sql`.

## Architecture

### Data model (Postgres, see `V1__initial_setup.sql`)

Two layers of tables:

- **OSM layer** (near-verbatim clone of source data): `osmnode`, `osmway` (category ROAD/RIVER/RAILWAY),
  `osmwaynode` (ordered node membership). `osmnode.osm_id`/`osmway.osm_id` are the stable OSM references.
- **Mesh layer** (derived geometry, scoped to a named `mesh`): `meshnode` (optionally linked back to an `osmnode`
  via `osmnode_id`), `mesharea` (bit-encoded `material`, optional `osmway_id`), `meshline` (edges with `left_area`/
  `right_area` — area is represented purely as the left/right sides of lines, not as an independent polygon list,
  to guarantee a line belongs to at most two areas), `meshpolygon`/`meshpolygonnode` (closed polygon rings),
  `meshnodepair`, `meshfailure` (records places where mesh building failed, kept rather than silently dropped).

Key distinction documented in `README.md`: an **osmNode** location is a fixed OSM lat/lon reference; a
**MeshNode** location is a *derived/projected* result of mesh building and unsuited for spatial lookup — lookups
on mesh data must go through OSM/semantic identity, not mesh coordinates.

### Mesh build pipeline

Meshes are built for a named tile/area (see `WellKnownMesh` — currently only `Desdorf` is fully defined) in a
fixed order chosen so elevation of each layer is derivable from what's already placed (see `README.md` "Populating
a blank tile in steps"): water → railways → highways (with connectors created *before* modifying a way, never
after) → smaller roads → buildings → optional enhancements/decorations.

`OsmXmlParser` parses incoming OSM XML into `OSMData`, `OSMToSceneryDataConverter` turns that into `MapData` using
a `GridCellBounds`/projection, and `OsmService`/`MeshService` persist the result against the tables above.
`TerrainMesh` is the in-memory representation of a mesh's polygons + failures; `PersistedMeshFactory` /
`TerrainMeshManager` bridge it to the JPA repositories in `de.yard.owm.services.persistence`.

### Service layer (`services/src/main/java/de/yard/owm`)

- `controller/` — REST controllers: `MeshController` (`/owm/mesh` CRUD: create from a well-known mesh's grid
  bounds, PUT to feed OSM XML into an existing mesh, GET/DELETE), `OsmController`, `TileController` (`/owm/tile/image`:
  returns a PNG for a bounding box, currently a placeholder image rather than a real tile rendering).
- `services/` — business logic (`MeshService`, `OsmService`/`OsmElementService`, `OsmDataService`, `TileService`,
  `PlatformService`), plus `services/persistence` (Spring Data JPA repositories/entities mirroring the schema
  above) and `services/maze` (an unrelated bundled Spring Data REST resource with its own validator/deserializer).
- Mesh operations are currently restricted to "well known meshes" (`WellKnownMesh` enum) — arbitrary mesh creation
  from raw OSM without a predefined `GridCellBounds` is not yet supported.
- `dto/` — web-facing request/response models (e.g. `WebLatLon`, `TileImageRequest`). Controllers convert between
  these and the domain `de.yard.threed.core.LatLon` type at the boundary; the service layer only deals in domain
  types, never in `dto` classes, to keep persistence/business logic decoupled from the web representation.

### Frontend

`services/src/main/resources/public/worldmesh/` is a minimal static JS/Leaflet-style client (`worldmesh.html`,
`worldmesh.js`, `httputil.js`) served directly by Spring Boot for previewing mesh data — no separate build step.
The map supports drawing a rectangular selection (`btn_selectarea` toggles selection mode, disabling map dragging
while a mousedown+drag draws a `L.rectangle`); on mouseup the bounds are sent to `/owm/tile/image` and the
returned PNG is shown as an `L.imageOverlay` positioned over the selected area. `httputil.js`'s other helpers
(`doGet`/`doPost`/...) all assume JSON responses, so binary responses go through the separate `doGetBlob()`.

## Terminology (from `README.md`)

- **way**: anything from a path to a highway (a generic OSM "road").
- **river**: a distinct category from generic ways (e.g. the Rhine).
- **cut** vs **clip**: "cut" applies specifically to tile/grid boundary handling; "clip" is the general term for
  making any polygon fit/conform. The project deliberately avoids traditional boundary "cuts" for now in favor of
  a planned "smart cut" applied to polygon lines during tile export from the DB.
