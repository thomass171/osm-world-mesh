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

However, even GridBoundaries couldn't provide a useful solution for
the "gapless mesh" requirement.

26.3.24 new gridless DB approach

# Refactorings
components that need a platform like 
* PortableModelList for GLTF building will
* graph for traffic graph

get a separate module. But for migration its easier to keep
tcp dependencies for a while.

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
