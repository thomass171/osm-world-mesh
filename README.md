# osm-world-mesh
Create world scenery from OSM data

# History
The most wrong estimation for building a world scenery from OSM data
is the assumption it is just the iterative creation of textured geometries from
OSM lines. The main challanges are:

* keeping the mesh gapless
* keeping the mesh overlap free
* keeping the mesh seamless
* use correct and visually acceptable elevations

This project started some years ago as fork of https://github.com/tordanik/OSM2World, 
which already provides a very sophisticated implementation for creating 
three-dimensional models of the world from OpenStreetMap data.

Unfortunately OSM2World is a wide range project which covers the complete
pipeline from OSM data to different renderer, thus causing many dependencies and thus
high complexity.

So the next step was to remove unneeded components (eg. some renderer)
and the introduction of GridBoundaries combined with a polygon based
approach, which finally led to separation
from the initial fork.

The steps for scenery building are
* Create polygons for all OSM areas and supplements
* Create background to fill all gaps not covered by OSM (Phase.BACKGROUND with FTR_SMARTBG enabled)
* Create decorations
* Handle overlaps of decorations(?)


However, even GridBoundaries couldn't provide a useful solution for
the "gapless mesh" requirement.

26.3.24 new gridless DB approach

# Refactorings
components that need a platform like 
* PortableModelList for GLTF building will
* graph for traffic graph

get a separate module. But for migration its easier to keep
tcp dependencies for a while.

# Data Model
osmNode is unique and unambiguous always. The location (LatLon) is
that of OSM and thus a fix reference.

MeshNode is the result of some mesh creation and the location is
a vague result including projection. The location isn't suited
for lookup. Lookup needs to be done by the semantics of the point.
Most meshNodes should be related to an osmNode.

# Services

## POST

* osm(xml)
* airport()

## GET (might be cached)
* trafficgraph(area,cluster)
* groundnet(icaos)?
* terrain(area, materials={basic,osm2world,wood}): 
* objects(area)
* terrasync(area): compatible with TerraSync 
  object/model/terrain/STG
  and ready to be used from tcp-flightgear:TravelScene[Bluebird]

# Provider

* SRTM?
...
