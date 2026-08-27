# osm-world-mesh
Create world scenery from OSM data. See also [Draft of a prototype](https://ubuntu-server.udehlavj1efjeuqv.myfritz.net/worldmesh/worldmesh.html).


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

There is no longer a mapnode location inside/outside grid (outside (sub)mesh will always abort)

09.02.26 Again, new approach. Still DB based, but more simple:

* With polygons in DB instead of lines (its hard to derive 
polygons just from points/lines)
* No triangulation before model creation.
* Again try a tile with background to initially avoid
holes in polygons. Gaps might be filled later by supplements.
* Simple configuration just by Spring
* No textures but FG land classes (what about wayland?)
* Viewer2d (benefit file io) or browser client (benefit leaflet)
* Avoid cloned OSM data and projected coordinates in DB
* Even though needed in principle on boundaries for a FG prototype we still refrain from using traditional 'cut's because of it's possible complexity. We'll later use a 'smart cut' on polygon lines during tile export from DB.
* What is the difference between 'cut' and 'clip'? cut' only related to boundaries and 'clip' related to any polygon to make it fit. 

 * Populating a blank tile in steps (order by elevation predictability desc):
 * Add water (river, lake). Maybe channel bridges. Water has fix or constant/predictable elevation
 * Add railways (incl. bridges). Railways have similar predictable elevation like water.
 * Add highways 
 * For all ways create all required connector before creating the way instead of modifying the way later.
 * smaller roads
 * buildings
 * Enhancements (if enabled)
 * Decorations (if enabled)

phrases:

way:  everything from path to highway. A 'road' ...
river: everything from ... to. Rhine

# Refactorings
components that need a platform like 
* PortableModelList for GLTF building will
* graph for traffic graph

get a separate module. But for migration its easier to keep
tcp dependencies for a while.

# Data Model
The base item is a mesh for a tile. It has a clearly
defined outline, eg. a coastline or just some outline.
Important is that the outline defines elevation absolutly?

osmNode is unique and unambiguous always. The location (LatLon) is
that of OSM and thus a fix reference.

MeshNode is the result of some mesh creation and the location is
a vague result including projection. The location isn't suited
for lookup. Lookup needs to be done by the semantics of the point.
Most meshNodes should be related to an osmNode.

# Modules
## osm2mesh
basic OSM utils, no SpringBoot

## Services
The main SpringBoot modules.
### POST

* osm(xml)
* airport()

### GET (might be cached)
* trafficgraph(area,cluster)
* groundnet(icaos)?
* terrain(area, materials={basic,osm2world,wood}): 
* objects(area)
* terrasync(area): compatible with TerraSync 
  object/model/terrain/STG
  and ready to be used from tcp-flightgear:TravelScene[Bluebird]

## Traditional
??

# Build
The geotools are not in maven central.

# Provider

* SRTM?
...

# Helper
## DB queries
```
select p.osmid,p.type,pn.index,pn.meshnode_id from worldmesh_test.meshpolygon p,worldmesh_test.meshpolygonnode pn
where p.id = pn.meshpolygon_id
--and p.osmid=255563538
order by p.osmid,pn.index
```

```
select p.osmid,p.type,np.* from worldmesh_test.meshpolygon p,worldmesh_test.meshnodepair np
where p.id = np.meshpolygon_id
and p.osmid=255563538
```


