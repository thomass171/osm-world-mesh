package de.yard.threed.osm2scenery.modules;

import com.google.common.base.Function;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2scenery.SceneryObjectList;
import de.yard.threed.osm2scenery.scenery.SceneryAreaObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryObjectFactory;
import de.yard.threed.osm2scenery.scenery.components.BuildingComponent;
import de.yard.threed.osm2world.*;
import org.apache.commons.configuration2.Configuration;
import org.apache.log4j.Logger;

import java.awt.*;
import java.util.List;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static de.yard.threed.osm2world.EleConstraintEnforcer.ConstraintType.EXACT;
import static de.yard.threed.osm2world.EleConstraintEnforcer.ConstraintType.MIN;
import static de.yard.threed.osm2world.GeometryUtil.*;
import static de.yard.threed.osm2world.GroundState.*;
import static de.yard.threed.osm2world.NamedTexCoordFunction.GLOBAL_X_Z;
import static de.yard.threed.osm2world.TexCoordUtil.triangleTexCoordLists;
import static de.yard.threed.osm2world.ValueStringParser.*;
import static de.yard.threed.osm2world.WorldModuleParseUtil.parseHeight;
import static de.yard.threed.osm2world.WorldModuleParseUtil.parseWidth;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;


/**
 * adds buildings to the world
 */
public class BuildingModule extends SceneryModule {
    static Logger logger = Logger.getLogger(BuildingModule.class);
    Map<MapArea, Building> area2building = new HashMap<>();
    Map<MapArea, BuildingCluster> area2cluster = new HashMap<>();
    private SceneryObjectList buildingobjects;

    @Override
    public SceneryObjectList applyTo(MapData mapData) {
        buildingobjects = new SceneryObjectList();

        boolean useBuildingColors = Config.getCurrentConfiguration().getBoolean("useBuildingColors", true);
        boolean drawBuildingWindows = Config.getCurrentConfiguration().getBoolean("drawBuildingWindows", true);

        for (MapArea area : mapData.getMapAreas()) {

            if (!area.getRepresentations().isEmpty()) continue;

            String buildingValue = area.getTags().getValue("building");

            if (buildingValue != null && !buildingValue.equals("no")) {

                Building building = new Building(area, useBuildingColors, drawBuildingWindows);
                BuildingComponent buildingComponent = new BuildingComponent(building);
                SceneryAreaObject buildingobject = SceneryObjectFactory.createBuilding(area, null, buildingComponent);
                //area.addRepresentation(building);
                buildingobjects.add(buildingobject);
                area2building.put(area, building);
            }

        }


        return buildingobjects;
    }

    @Override
    public void classify(MapData mapData) {
        //doofe Pruefungen Phase.CLASSIFY.assertCurrent();
        classifyBuildings(buildingobjects);
    }


    /**
     * Eine halbwegs schlüssige Klassifizierung (z.B. Garage) geht erst jetzt, wenn der Kontext des Building bekannt ist.
     * Dabei werden sich dann auch die Defaultwerte des Building aendern. Aber nicht das, was durch OSM vorgegeben ist!
     *
     * @param buildingobjects
     */
    private void classifyBuildings(SceneryObjectList buildingobjects) {
        for (SceneryObject so : buildingobjects.objects) {
            Building building = ((BuildingComponent) ((SceneryAreaObject) so).volumeProvider).building;
            //Evtl. gab es schon eine Klassifizierung ueber OSM
            if (building.classification != null) {
                continue;
            }
            MapArea area = building.area;
            double areasize = area.getPolygon().getArea();
            if (area.getOsmId() == 336523597) {
                int h = 9;
            }
            // garage nur mit gemeinsamer Node zu Haus.
            if (area.getBoundaryNodes().size() == 5 && areasize < 30 && building.getNeighbors().size() > 0) {
                building.classification = BuildingClassification.GARAGE;
            } else if (areasize > 20 && areasize < 130 /*&& area.getBoundaryNodes().size() < 9*/) {
                // Mal 9 als Maximum. Komplexe Häuser bilden mit 45 Grad Satteldach Artefakte, Das greift aber nicht.
                // Satteldächer sind doch üblicher als Flachdächer
                // Erfordert aber rechteckige Grundflächen? Zumindest nicht verwinkelt.
                building.classification = BuildingClassification.SINGLEFAMILIYHOUSE;
                BuildingPart part = building.parts.get(0);
                part.proposeRoof("gabled", Materials.PANTILE_ROOF_DARK);
                part.proposeWall(Materials.WALL_BRICK_RED);
            }
        }
    }

    /**
     * 6.5.19: Was genau ein Building ist, ist unklar.  Eigentlich muss man sagen: BuildingPart ist das Building.
     */
    public /*16.5.19static*/ class Building/* implements AreaWorldObject,
            WorldObjectWithOutline, RenderableToAllTargets */ {

        private final MapArea area;
        private final List<BuildingPart> parts =
                new ArrayList<BuildingPart>();

        private final O2WEleConnectorGroup outlineConnectors;
        private BuildingCluster buildingCluster;
        public BuildingClassification classification;

        public Building(MapArea area, boolean useBuildingColors,
                        boolean drawBuildingWindows) {

            this.area = area;
            if (area.getOsmId() == 218058763) {
                int h = 9;
            }

            // Hier werden alle überlappenden OSM areas, die als "building:part" definiert sind als Buildingpart abgebildet.
            // Oder nur komplett enthaltene?
            // Aber werden  -vor allem - nachher
            // deren Polygone rausgerechnet? Das blick ich nicht. Warum??. Was bringt das, ausser Confusion?
            // dann hat man nachher Polygone, die mit den OSM Ways nichts mehr zu tun haben. Mal weglassen. Naja, vielleicht taeuscht das.

            // Naja, die "building:part" sind wohl absichtlich so definiert (https://wiki.openstreetmap.org/wiki/Key:building:part).
            //Zumindest klappt das bei StarC ganz gut.
            //Aneinandergrenzende Buildings erkennt er nicht, als Part.
            boolean subtractparts = true;
            if (subtractparts) {
                for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
                    // die eigenen AreaNodes findet er auch als Overlap. Das scheint mir irgendwie Unsinn. Naja.
                    // Grosse Areas wie Ortschaften tauchen auch hier auf.
                    MapElement other = overlap.getOther(area);
                    if (other instanceof MapArea) {
                        MapArea otherArea = (MapArea) other;

                        if (other.getTags().containsKey("building:part")) {


                            //TODO: check whether the building contains the part (instead of just touching it)
                            if (area.getPolygon().contains(
                                    otherArea.getPolygon().getOuter())) {
                                parts.add(new BuildingPart(this, otherArea,
                                        otherArea.getPolygon(), useBuildingColors,
                                        drawBuildingWindows));
                            }
                        } else {
                            if (other.getTags().containsKey("building")) {
                                // overlapping Buildings? Oder angrenzend.
                                int h = 9;
                                BuildingCluster cluster = area2cluster.get(area);
                                BuildingCluster othercluster = area2cluster.get(otherArea);

                                if (cluster == null) {
                                    if (othercluster == null) {
                                        cluster = new BuildingCluster();
                                        cluster.add(area, otherArea);
                                        area2cluster.put(area, cluster);
                                        area2cluster.put(otherArea, cluster);
                                    } else {
                                        othercluster.add(area, otherArea);
                                        area2cluster.put(area, othercluster);
                                        cluster = othercluster;
                                    }
                                } else {
                                    if (othercluster == null) {
                                        cluster.add(otherArea, area);
                                        area2cluster.put(otherArea, cluster);
                                    } else {
                                        //nothing to do
                                    }
                                }
                                buildingCluster = cluster;
                            }
                        }
                    }
                }
            }

            /* add part(s) for area not covered by building:part polygons */
            boolean isBuildingPart = false;
            if (area.getTags().containsKey("building:part"))
                isBuildingPart = !("no".equals(area.getTags().getValue("building:part")));

            if (parts.isEmpty() || isBuildingPart) {
                parts.add(new BuildingPart(this, area,
                        area.getPolygon(), useBuildingColors, drawBuildingWindows));
            } else {
                List<SimplePolygonXZ> subtractPolygons = new ArrayList<SimplePolygonXZ>();

                for (BuildingPart part : parts) {
                    subtractPolygons.add(part.getPolygon().getOuter());
                }
                subtractPolygons.addAll(area.getPolygon().getHoles());

                Collection<PolygonWithHolesXZ> remainingPolys;
                try {
                    //12.7.19: Scheitert z.B. bei tile 3056443
                    remainingPolys =
                            CAGUtil.subtractPolygons(
                                    area.getPolygon().getOuter(),
                                    subtractPolygons);
                } catch (/*11.11.21 org.osm2world.core.math.*/InvalidGeometryException e) {
                    logger.error("CAGUtil.subtractPolygon() failed", e);
                    remainingPolys = new ArrayList<>();
                }
                for (PolygonWithHolesXZ remainingPoly : remainingPolys) {
                    parts.add(new BuildingPart(this, area, remainingPoly,
                            useBuildingColors, drawBuildingWindows));
                }

            }

            /* create connectors along the outline.
             * Because the ground around buildings isType not necessarily plane,
             * they aren't directly used for ele, but instead their minimum.
             */

            outlineConnectors = new O2WEleConnectorGroup();
            outlineConnectors.addConnectorsFor(area.getPolygon(), null, ON);

        }

        public MapArea getArea() { //TODO: redundant because of getPrimaryMapElement
            return area;
        }

        public List<BuildingPart> getParts() {
            return parts;
        }

        // @Override
        public MapArea getPrimaryMapElement() {
            return area;
        }

        //@Override
        public GroundState getGroundState() {
            return ON;
        }

        // @Override
        public O2WEleConnectorGroup getEleConnectors() {
            return outlineConnectors;
        }

        // @Override
        public void defineEleConstraints(EleConstraintEnforcer enforcer) {

            List<O2WEleConnector> groundLevelEntrances = new ArrayList<O2WEleConnector>();

            /* add constraints between entrances with different levels */

            for (BuildingPart part : parts) {

                // add vertical distances

                for (int i = 0; i < part.entrances.size(); i++) {

                    BuildingEntrance e1 = part.entrances.get(i);

                    for (int j = i + 1; j < part.entrances.size(); j++) {

                        BuildingEntrance e2 = part.entrances.get(j);

                        double heightPerLevel = part.heightWithoutRoof / part.buildingLevels;

                        if (e1.getLevel() > e2.getLevel()) {

                            enforcer.requireVerticalDistance(EXACT,
                                    heightPerLevel * (e1.getLevel() - e2.getLevel()),
                                    e1.connector, e2.connector);

                        } else if (e1.getLevel() < e2.getLevel()) {

                            enforcer.requireVerticalDistance(EXACT,
                                    heightPerLevel * (e2.getLevel() - e1.getLevel()),
                                    e2.connector, e1.connector);

                        }

                    }

                    // collect entrances for next step

                    if (e1.getLevel() == 0 && e1.getGroundState() == ON) {
                        groundLevelEntrances.add(e1.connector);
                    }

                }

            }

            /* make sure that a level=0 ground entrance isType the building's lowest point */

            for (O2WEleConnector outlineConnector : outlineConnectors) {
                for (O2WEleConnector entranceConnector : groundLevelEntrances) {
                    enforcer.requireVerticalDistance(
                            MIN, 0, outlineConnector, entranceConnector);
                }
            }

        }

        //TODO
//		@Override
//		public double getClearingAbove(VectorXZ pos) {
//			double maxClearingAbove = 0;
//			for (BuildingPart part : parts) {
//				double clearing = part.getClearingAbove(pos);
//				maxClearingAbove = max(clearing, maxClearingAbove);
//			}
//			return maxClearingAbove;
//		}

        //@Override
        public SimplePolygonXZ getOutlinePolygonXZ() {
            return area.getPolygon().getOuter().makeCounterclockwise();
        }

        /**
         * 26.4.19: GEht nicht, solange die OSM2World Elevation noch nicht berechnet wurde. daher immer 0.
         *
         * @return
         */
        public double getGroundLevelEle() {

            double minEle = POSITIVE_INFINITY;

            /*for (EleConnector c : outlineConnectors) {
                if (c.getPosXYZ().y < minEle) {
                    minEle = c.getPosXYZ().y;
                }
            }

            return minEle;*/
            return 0;

        }

        //@Override
        public PolygonXYZ getOutlinePolygon() {
            return getOutlinePolygonXZ().xyz(getGroundLevelEle());
        }

        //@Override
        public void renderTo(Target<?> target) {
            if (area.getOsmId() == 218058763) {
                int h = 9;
            }
            for (BuildingPart part : parts) {
                part.renderTo(target);
            }
        }

        public boolean isGarage() {
            return classification == BuildingClassification.GARAGE;
        }

        public BuildingCluster getBuildingCluster() {
            return buildingCluster;
        }

        public List<Building> getNeighbors() {
            List<Building> neighbors = new ArrayList<>();
            BuildingCluster cluster = area2cluster.get(area);
            if (cluster == null) {
                return neighbors;
            }
            Map<MapArea, Void> l = cluster.neighbors.get(area);
            if (l != null) {
                for (MapArea n : l.keySet()) {
                    neighbors.add(area2building.get(n));
                }
            }
            return neighbors;
        }
    }

    /**
     * BuildingPart ist wohl kein Part im logischen Sinne wie Dach,Tür, Fenster,sondern im Sinne
     * von Grundflächen(??). Oder rein im Sinne von Polygonen(??).
     * 6.5.19: Das Konzept erschliesst sich mir nicht.Dass Walls verschiedene Parts sein können, OK. Aber jeder BuildingPart
     * hat sein eigenes Roof? Und der setAttributes() wird für jeden BuildingPart statt im Building gemacht, obwohl es doch zum
     * Building gehört. Nee, ich weiss nicht. Eigentlich muss man sagen: BuildingPart ist das Building.
     * NeeNee, und zwei verschiedene Materialen (wall+roof) in jedem Buildingpart!
     * Wahrscheinlich ist BuildingPart das in OSM definierte. Building ist dann nur eine logische Sammlung von Parts die zusammengehören.
     * Bei (grossen) Gebäuden, die um die Ecke gehen, macht das wohl Sinn, jeweils ein Buildingpart mit eigenem Dach.
     */
    public static class BuildingPart /*implements RenderableToAllTargets*/ {

        private final Building building;
        private final MapArea area;
        private final PolygonWithHolesXZ polygon;

        private int buildingLevels;
        private int minLevel;

        private double heightWithoutRoof;

        boolean explicitWallTagging = false;//TODO setzen
        private Material materialWall;
        private Material materialWallWithWindows;
        private Material materialRoof;

        private List<BuildingEntrance> entrances = new ArrayList<BuildingEntrance>();
        boolean explicitRoofTagging = true;

        private Roof roof;

        public BuildingPart(Building building,
                            MapArea area, PolygonWithHolesXZ polygon,
                            boolean useBuildingColors, boolean drawBuildingWindows) {

            this.building = building;
            this.area = area;
            this.polygon = polygon;


            setAttributes(useBuildingColors, drawBuildingWindows);
            // 6.5.19: Check optional OSM details.
            Configuration extension;
            if ((extension = SceneryBuilder.loadExtensionConfig(area.getOsmId())) != null) {
                ConfMaterial specificMaterial = new ConfMaterial("OSM" + area.getOsmId(), Material.Interpolation.FLAT,
                        Color.BLUE);
                OsmUtil.loadMaterialConfiguration(extension, specificMaterial, false);
                //Das mit dem Pfad ist erstmal nur so.
                specificMaterial.setBasePath("../extensions");
                //TODO nicht einfach stumpf das wallmaterial setzen
                materialWall = specificMaterial;
                //das materialWallWithWindows scheint Prio zu haben (in drawWallOnPolygon).
                materialWallWithWindows = specificMaterial;
            }
            for (MapNode node : area.getBoundaryNodes()) {
                if ((node.getTags().contains("building", "entrance")
                        || node.getTags().containsKey("entrance"))
                        && node.getRepresentations().isEmpty()) {

                    BuildingEntrance entrance = new BuildingEntrance(this, node);
                    entrances.add(entrance);
                    node.addRepresentation(entrance);

                }
            }

        }

        public PolygonWithHolesXZ getPolygon() {
            return polygon;
        }

        public Roof getRoof() {
            return roof;
        }

        public double getClearingAbove(VectorXZ pos) {
            return heightWithoutRoof + roof.getRoofHeight();
        }

        //@Override
        public void renderTo(Target<?> target) {

            renderWalls(target, roof);

            roof.renderTo(target);

        }

        private void renderFloor(Target<?> target, double floorEle) {

            Collection<TriangleXZ> triangles =
                    TriangulationUtil.triangulate(polygon);

            List<TriangleXYZ> trianglesXYZ =
                    new ArrayList<TriangleXYZ>(triangles.size());

            for (TriangleXZ triangle : triangles) {
                trianglesXYZ.add(triangle.makeClockwise().xyz(floorEle));
            }

            target.drawTriangles(materialWall, trianglesXYZ,
                    triangleTexCoordLists(trianglesXYZ, materialWall, GLOBAL_X_Z), new OsmOrigin(getPolygon()));

        }

        private void renderWalls(Target<?> target, Roof roof) {

            double baseEle = building.getGroundLevelEle();
            double floorHeight = calculateFloorHeight(roof);
            boolean renderFloor = (floorHeight > 0);

            if (area.getOverlaps().isEmpty()) {
                renderWalls(target, roof.getPolygon(), false,
                        baseEle, floorHeight, roof);
            } else {
                /* find terrain boundaries on the ground
                 * that overlap with the building */
                List<TerrainBoundaryWorldObject> tbWorldObjects = new ArrayList<TerrainBoundaryWorldObject>();

                for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
                    MapElement other = overlap.getOther(area);
                    if (other.getPrimaryRepresentation() instanceof TerrainBoundaryWorldObject
                            && other.getPrimaryRepresentation().getGroundState() == ON
                            && (other.getTags().contains("tunnel", "passage")
                            || other.getTags().contains("tunnel", "building_passage"))) {
                        tbWorldObjects.add((TerrainBoundaryWorldObject)
                                other.getPrimaryRepresentation());
                    }
                }

                /* render building parts where the building polygon does not overlap with terrain boundaries */

                List<SimplePolygonXZ> subtractPolygons = new ArrayList<SimplePolygonXZ>();

                for (TerrainBoundaryWorldObject o : tbWorldObjects) {

                    SimplePolygonXZ subtractPoly = o.getOutlinePolygonXZ();

                    subtractPolygons.add(subtractPoly);

                    if (o instanceof WaySegmentWorldObject) {

                        // extend the subtract polygon for segments that end
                        // at a common node with this building part's outline.
                        // (otherwise, the subtract polygon will probably
                        // not exactly line up with the polygon boundary)

                        WaySegmentWorldObject waySegmentWO = (WaySegmentWorldObject) o;
                        VectorXZ start = waySegmentWO.getStartPosition();
                        VectorXZ end = waySegmentWO.getEndPosition();

                        boolean startCommonNode = false;
                        boolean endCommonNode = false;

                        for (SimplePolygonXZ p : polygon.getPolygons()) {
                            startCommonNode |= p.getVertexCollection().contains(start);
                            endCommonNode |= p.getVertexCollection().contains(end);
                        }

                        VectorXZ direction = end.subtract(start).normalize();

                        if (startCommonNode) {
                            subtractPolygons.add(subtractPoly.shift(direction));
                        }

                        if (endCommonNode) {
                            subtractPolygons.add(subtractPoly.shift(direction.invert()));
                        }

                    }

                }

                //16.5.19: Was ist das für ein Use Case? Löcher im Dach? Und dann kommt da keine Wand hin? Naja, mag gehen. Beispiel?
                subtractPolygons.addAll(roof.getPolygon().getHoles());

                //16.5.19: Aus dem roof Outline ergeben sich jetzt die Walls! Und bei garbled roof hat das Roof zwei zusätzliche Vertices.
                Collection<PolygonWithHolesXZ> buildingPartPolys =
                        CAGUtil.subtractPolygons(
                                roof.getPolygon().getOuter(),
                                subtractPolygons);

                for (PolygonWithHolesXZ p : buildingPartPolys) {
                    renderWalls(target, p, false, baseEle, floorHeight, roof);
                    if (renderFloor) {
                        renderFloor(target, baseEle + floorHeight);
                    }
                }

                /* render building parts above the terrain boundaries */

                for (TerrainBoundaryWorldObject o : tbWorldObjects) {

                    Collection<PolygonWithHolesXZ> raisedBuildingPartPolys;

                    Collection<PolygonWithHolesXZ> polysAboveTBWOs =
                            CAGUtil.intersectPolygons(Arrays.asList(
                                    polygon.getOuter(), o.getOutlinePolygon().getSimpleXZPolygon()));


                    if (polygon.getHoles().isEmpty()) {
                        raisedBuildingPartPolys = polysAboveTBWOs;
                    } else {
                        raisedBuildingPartPolys = new ArrayList<PolygonWithHolesXZ>();
                        for (PolygonWithHolesXZ p : polysAboveTBWOs) {
                            List<SimplePolygonXZ> subPolys = new ArrayList<SimplePolygonXZ>();
                            subPolys.addAll(polygon.getHoles());
                            subPolys.addAll(p.getHoles());
                            raisedBuildingPartPolys.addAll(
                                    CAGUtil.subtractPolygons(p.getOuter(), subPolys));
                        }
                    }

                    for (PolygonWithHolesXZ p : raisedBuildingPartPolys) {
                        double newFloorHeight = 3;
                        //TODO restore clearing - o.getClearingAbove(o.getOutlinePolygon().getSimpleXZPolygon().getCenter());
                        if (newFloorHeight < floorHeight) {
                            newFloorHeight = floorHeight;
                        }
                        renderWalls(target, p, false, baseEle, newFloorHeight, roof);
                        renderFloor(target, baseEle);
                    }

                }

            }

        }

        private double calculateFloorHeight(Roof roof) {

            if (getValue("min_height") != null) {

                Float minHeight = ValueStringParser.parseMeasure(getValue("min_height"));
                if (minHeight != null) {
                    return minHeight;
                }

            }

            if (minLevel > 0) {

                double he = (heightWithoutRoof / buildingLevels) * minLevel;
                if (area.getOsmId() == 218058763 && he > 0.1) {
                    int h = 9;
                }
                return he;

            }

            if (area.getTags().contains("building", "roof")
                    || area.getTags().contains("building:part", "roof")) {

                return heightWithoutRoof - 0.3;

            }

            return 0;

        }

        private void renderWalls(Target<?> target, PolygonWithHolesXZ p,
                                 boolean renderFloor, double baseEle, double floorHeight,
                                 Roof roof) {

            drawWallOnPolygon(target, baseEle, floorHeight,
                    roof, p.getOuter().makeCounterclockwise());

            for (SimplePolygonXZ polygon : p.getHoles()) {
                drawWallOnPolygon(target, baseEle, floorHeight,
                        roof, polygon.makeClockwise());
            }

        }

        private void drawWallOnPolygon(Target<?> target, double baseEle,
                                       double floorHeight, Roof roof, SimplePolygonXZ polygon) {

            double floorEle = baseEle + floorHeight;

            if (baseEle > 4 || floorHeight > 4) {
                int h = 9;
            }
            List<TextureData> textureDataList = materialWallWithWindows.getTextureDataList();
            List<VectorXZ> vertices = polygon.getVertexLoop();

            List<VectorXYZ> mainWallVectors = new ArrayList<VectorXYZ>(vertices.size() * 2);
            List<VectorXYZ> roofWallVectors = new ArrayList<VectorXYZ>(vertices.size() * 2);

            List<List<VectorXZ>> mainWallTexCoordLists = new ArrayList<List<VectorXZ>>(
                    textureDataList.size());

            for (int texLayer = 0; texLayer < textureDataList.size(); texLayer++) {
                mainWallTexCoordLists.add(new ArrayList<VectorXZ>());
            }

            double accumulatedLength = 0;
            double[] previousS = new double[textureDataList.size()];

            for (int i = 0; i < vertices.size(); i++) {

                final VectorXZ coord = vertices.get(i);

                /* update accumulated wall length */

                if (i > 0) {
                    accumulatedLength += coord.distanceTo(vertices.get(i - 1));
                }

                /* add wall vectors */

                final VectorXYZ upperVector = coord.xyz(roof.getRoofEleAt(coord));
                final VectorXYZ middleVector = coord.xyz(baseEle + heightWithoutRoof);

                double upperEle = upperVector.y;
                double middleEle = middleVector.y;

                mainWallVectors.add(middleVector);
                mainWallVectors.add(new VectorXYZ(coord.x,
                        min(floorEle, middleEle), coord.z));

                roofWallVectors.add(upperVector);
                roofWallVectors.add(new VectorXYZ(coord.x,
                        min(middleEle, upperEle), coord.z));


                /* add texture coordinates */

                for (int texLayer = 0; texLayer < textureDataList.size(); texLayer++) {

                    TextureData textureData = textureDataList.get(texLayer);
                    List<VectorXZ> texCoordList = mainWallTexCoordLists.get(texLayer);

                    double s, lowerT, middleT;

                    // determine s (width dimension) coordinate

                    if (textureData.height > 0) {
                        s = accumulatedLength / textureData.width;
                    } else {
                        if (i == 0) {
                            s = 0;
                        } else {
                            s = previousS[texLayer] + round(vertices.get(i - 1)
                                    .distanceTo(coord) / textureData.width);
                        }
                    }

                    previousS[texLayer] = s;

                    // determine t (height dimension) coordinates

                    if (textureData.height > 0) {

                        lowerT = (floorEle - baseEle) / textureData.height;
                        middleT = (middleEle - baseEle) / textureData.height;

                    } else {

                        lowerT = buildingLevels *
                                (floorEle - baseEle) / (middleEle - baseEle);
                        middleT = buildingLevels;

                    }

                    // set texture coordinates

                    texCoordList.add(new VectorXZ(s, middleT));
                    texCoordList.add(new VectorXZ(s, lowerT));

                }

            }

            target.drawTriangleStrip(materialWallWithWindows, new VectorXYZList(mainWallVectors),
                    mainWallTexCoordLists, null);

            //6.5.19: roofWallVectors ist wohl der Giebel

            drawStripWithoutDegenerates(target, materialWall, roofWallVectors,
                    TexCoordUtil.texCoordLists(roofWallVectors, materialWall, NamedTexCoordFunction.STRIP_WALL));

        }

        /**
         * sets the building part attributes (height, colors) depending on
         * the building's and building part's tags.
         * If available, explicitly tagged data isType used,
         * with tags of the building part overriding building tags.
         * Otherwise, the values depend on indirect assumptions
         * (level height) or ultimately the building class as determined
         * by the "building" key.
         */
        private void setAttributes(boolean useBuildingColors,
                                   boolean drawBuildingWindows) {

            TagGroup tags = area.getTags();
            TagGroup buildingTags = building.area.getTags();

            if (area.getOsmId() == 336523788) {
                int h = 9;
            }
            BuildingDefaults buildingDefaults = new BuildingDefaults(area);
            if (building.classification == null) {
                building.classification = buildingDefaults.classification;
            } else {
                logger.warn("inconsistent classification");
            }
            /* determine defaults for building type */
            // 3.5.19: 3 ist doch zu hoch(!).
            int defaultLevels = 3;
            defaultLevels = buildingDefaults.defaultLevels;

            double defaultHeightPerLevel = 2.5;
            Material defaultMaterialWall = buildingDefaults.defaultMaterialWall;
            Material defaultMaterialRoof = buildingDefaults.defaultMaterialRoof;
            Material defaultMaterialWindows = buildingDefaults.defaultMaterialWindows;
            String defaultRoofShape = buildingDefaults.defaultRoofShape;

            if ("multi-storey".equals(getValue("parking"))) {
                defaultLevels = 5;
                defaultMaterialWindows = null;
            }

            /* determine levels */

            buildingLevels = defaultLevels;

            Float parsedLevels = null;

            if (getValue("building:levels") != null) {
                parsedLevels = parseOsmDecimal(
                        getValue("building:levels"), false);
            }

            if (parsedLevels != null) {
                buildingLevels = (int) (float) parsedLevels;
            } else if (parseHeight(tags, parseHeight(buildingTags, -1)) > 0) {
                buildingLevels = max(1, (int) (parseHeight(tags, parseHeight(
                        buildingTags, -1)) / defaultHeightPerLevel));
            }

            minLevel = 0;

            if (getValue("building:min_level") != null) {
                Float parsedMinLevel = parseOsmDecimal(
                        getValue("building:min_level"), false);
                if (parsedMinLevel != null) {
                    minLevel = (int) (float) parsedMinLevel;
                }
            }

            /* determine roof shape */


            if (!("no".equals(area.getTags().getValue("roof:lines"))) && hasComplexRoof(area)) {
                roof = new ComplexRoof();
            } else {

                String roofShape = getValue("roof:shape");
                if (roofShape == null) {
                    roofShape = getValue("building:roof:shape");
                }

                if (roofShape == null) {
                    roofShape = defaultRoofShape;
                    explicitRoofTagging = false;
                }

                try {

                    if ("pyramidal".equals(roofShape)) {
                        roof = new PyramidalRoof();
                    } else if ("onion".equals(roofShape)) {
                        roof = new OnionRoof();
                    } else if ("skillion".equals(roofShape)) {
                        roof = new SkillionRoof();
                    } else if ("gabled".equals(roofShape)) {
                        roof = new GabledRoof();
                    } else if ("hipped".equals(roofShape)) {
                        roof = new HippedRoof();
                    } else if ("half-hipped".equals(roofShape)) {
                        roof = new HalfHippedRoof();
                    } else if ("gambrel".equals(roofShape)) {
                        roof = new GambrelRoof();
                    } else if ("mansard".equals(roofShape)) {
                        roof = new MansardRoof();
                    } else if ("dome".equals(roofShape)) {
                        roof = new DomeRoof();
                    } else if ("round".equals(roofShape)) {
                        roof = new RoundRoof();
                    } else {
                        roof = new FlatRoof();
                    }

                } catch (InvalidGeometryException e) {
                    logger.warn("falling back to FlatRoof: " + e);
                    roof = new FlatRoof();
                    explicitRoofTagging = false;
                } catch (RoofNotPossibleException e) {
                    logger.warn("RoofNotPossibleException: falling back to FlatRoof: " + e);
                    roof = new FlatRoof();
                    explicitRoofTagging = false;
                }

            }

            /* determine height */

            double fallbackHeight = buildingLevels * defaultHeightPerLevel;
            fallbackHeight += roof.getRoofHeight();

            fallbackHeight = parseHeight(buildingTags, (float) fallbackHeight);

            double height = parseHeight(tags, (float) fallbackHeight);

            // Make sure buildings have at least some height
            height = Math.max(height, 0.001);

            heightWithoutRoof = height - roof.getRoofHeight();

            /* determine materials */

            if (defaultMaterialRoof == Materials.ROOF_DEFAULT
                    && explicitRoofTagging && roof instanceof FlatRoof) {
                defaultMaterialRoof = Materials.CONCRETE;
            }

            if (useBuildingColors) {

                materialWall = buildMaterial(
                        getValue("building:material"),
                        getValue("building:colour"),
                        defaultMaterialWall, false);
                materialRoof = buildMaterial(
                        getValue("roof:material"),
                        getValue("roof:colour"),
                        defaultMaterialRoof, true);

            } else {

                materialWall = defaultMaterialWall;
                materialRoof = defaultMaterialRoof;

            }

            if (materialWall == Materials.GLASS) {
                // avoid placing windows into a glass front
                // TODO: the == currently only works if GLASS isType not colorable
                defaultMaterialWindows = null;
            }

            materialWallWithWindows = materialWall;

            if (drawBuildingWindows) {

                Material materialWindows = defaultMaterialWindows;

                if (materialWindows != null) {

                    materialWallWithWindows = materialWallWithWindows.
                            withAddedLayers(materialWindows.getTextureDataList());

                }

            }

        }

        private Material buildMaterial(String materialString,
                                       String colorString, Material defaultMaterial,
                                       boolean roof) {

            Material material = defaultMaterial;

            if (materialString != null) {
                if ("brick".equals(materialString)) {
                    material = Materials.BRICK;
                } else if ("glass".equals(materialString)) {
                    material = roof ? Materials.GLASS_ROOF : Materials.GLASS;
                } else if ("wood".equals(materialString)) {
                    material = Materials.WOOD_WALL;
                } else if (Materials.getSurfaceMaterial(materialString) != null) {
                    material = Materials.getSurfaceMaterial(materialString);
                }
            }

            boolean colorable = material.getNumTextureLayers() == 0
                    || material.getTextureDataList().get(0).colorable;

            if (colorString != null && colorable) {

                Color color;

                if ("white".equals(colorString)) {
                    color = new Color(240, 240, 240);
                } else if ("black".equals(colorString)) {
                    color = new Color(76, 76, 76);
                } else if ("grey".equals(colorString) || "gray".equals(colorString)) {
                    color = new Color(100, 100, 100);
                } else if ("red".equals(colorString)) {
                    if (roof) {
                        color = new Color(204, 0, 0);
                    } else {
                        color = new Color(255, 190, 190);
                    }
                } else if ("green".equals(colorString)) {
                    if (roof) {
                        color = new Color(150, 200, 130);
                    } else {
                        color = new Color(190, 255, 190);
                    }
                } else if ("blue".equals(colorString)) {
                    if (roof) {
                        color = new Color(100, 50, 200);
                    } else {
                        color = new Color(190, 190, 255);
                    }
                } else if ("yellow".equals(colorString)) {
                    color = new Color(255, 255, 175);
                } else if ("pink".equals(colorString)) {
                    color = new Color(225, 175, 225);
                } else if ("orange".equals(colorString)) {
                    color = new Color(255, 225, 150);
                } else if ("brown".equals(colorString)) {
                    if (roof) {
                        color = new Color(120, 110, 110);
                    } else {
                        color = new Color(170, 130, 80);
                    }
                } else if (CSSColors.colorMap.containsKey(colorString)) {
                    color = CSSColors.colorMap.get(colorString);
                } else {
                    color = parseColor(colorString);
                }

                if (color != null) {
                    material = new ImmutableMaterial(
                            material.getInterpolation(), color,
                            material.getAmbientFactor(),
                            material.getDiffuseFactor(),
                            material.getSpecularFactor(),
                            material.getShininess(),
                            material.getTransparency(),
                            material.getShadow(),
                            material.getAmbientOcclusion(),
                            material.getTextureDataList());
                }

            }

            return material;

        }

        /**
         * returns the value for a key from the building part's tags or the
         * building's tags (if the part doesn't have a tag with this key)
         */
        private String getValue(String key) {

            if (area.getTags().containsKey(key)) {
                return area.getTags().getValue(key);
            } else {
                return building.area.getTags().getValue(key);
            }

        }

        /**
         * draws a triangle strip, but omits degenerate triangles
         */
        private static final void drawStripWithoutDegenerates(
                Target<?> target, Material material, List<VectorXYZ> vectors,
                List<List<VectorXZ>> texCoordLists) {

            List<TriangleXYZ> triangles = new ArrayList<TriangleXYZ>();
            List<List<VectorXZ>> triangleTexCoordLists = new ArrayList<List<VectorXZ>>(texCoordLists.size());

            for (int i = 0; i < texCoordLists.size(); i++) {
                triangleTexCoordLists.add(new ArrayList<VectorXZ>());
            }

            for (int triangle = 0; triangle < vectors.size() - 2; triangle++) {

                int indexA = triangle % 2 == 0 ? triangle : triangle + 1;
                int indexB = triangle % 2 == 0 ? triangle + 1 : triangle;
                int indexC = triangle + 2;

                TriangleXYZ t = new TriangleXYZ(
                        vectors.get(indexA),
                        vectors.get(indexB),
                        vectors.get(indexC));

                if (!t.isDegenerate()) {

                    triangles.add(t);

                    for (int i = 0; i < texCoordLists.size(); i++) {

                        triangleTexCoordLists.get(i).add(texCoordLists.get(i).get(indexA));
                        triangleTexCoordLists.get(i).add(texCoordLists.get(i).get(indexB));
                        triangleTexCoordLists.get(i).add(texCoordLists.get(i).get(indexC));

                    }
                }
            }

            if (triangles.size() > 0) {
                target.drawTriangles(material, triangles, triangleTexCoordLists, null);
            }

        }

        private static final float DEFAULT_RIDGE_HEIGHT = 5;

        public Material getMaterial() {
            //hier wall zu liefern ist unsauber. Siehe Header wegen Konzeptproblem.
            return materialWall;
        }

        public void proposeRoof(String rooftype, ConfMaterial materialRoof) {
            if (!explicitRoofTagging) {
                if (rooftype.equals("gabled")) {
                    try {
                        //45 nur als Indikator
                        this.roof = new GabledRoof(45f);
                    } catch (RoofNotPossibleException e) {
                        logger.debug("" + e.getMessage());
                    }
                    this.materialRoof = materialRoof;
                }
            }
        }

        public void proposeWall(ConfMaterial materialWall) {
            if (!explicitWallTagging) {
                this.materialWall = materialWall;
                this.materialWallWithWindows = materialWall;
            }
        }

        public static interface Roof extends RenderableToAllTargets {

            /**
             * returns the outline (with holes) of the roof.
             * The shape will be generally identical to that of the
             * building itself, but additional vertices might have
             * been inserted into segments.
             */
            PolygonWithHolesXZ getPolygon();

            /**
             * returns roof elevation at a position.
             */
            double getRoofEleAt(VectorXZ coord);

            /**
             * returns maximum roof height
             */
            double getRoofHeight();

            /**
             * returns maximum roof elevation
             */
            double getMaxRoofEle();

            void renderTo(Target<?> target);
        }

        /**
         * superclass for roofs based on roof:type tags.
         * Contains common functionality, such as roof height parsing.
         */
        abstract private class TaggedRoof implements Roof {
            //private um getter zu erzwingen.
            private final double roofHeight;
            Float taggedHeight = null;

            /*
             * default roof height if no value isType tagged explicitly.
             * Can optionally be overwritten by subclasses.
             * 17.5.19: Diesen Defaultwert kann man ja getrost vergessen.
             */
            protected float getDefaultRoofHeight() {
                if (buildingLevels == 1) {
                    return 1;
                } else {
                    return DEFAULT_RIDGE_HEIGHT;
                }
            }

            TaggedRoof() {
                if (area.getTags().containsKey("roof:height")) {
                    String valueString = getValue("roof:height");
                    taggedHeight = ValueStringParser.parseMeasure(valueString);
                } else if (getValue("roof:levels") != null) {
                    try {
                        taggedHeight = 2.5f * Integer.parseInt(getValue("roof:levels"));
                    } catch (NumberFormatException e) {
                    }
                }

                roofHeight =
                        taggedHeight != null ? taggedHeight : getDefaultRoofHeight();

            }

            @Override
            public double getRoofHeight() {
                return roofHeight;
            }

            @Override
            public double getMaxRoofEle() {
                return building.getGroundLevelEle() +
                        heightWithoutRoof + getRoofHeight();
            }

        }

        private abstract class SpindleRoof extends TaggedRoof {

            @Override
            public PolygonWithHolesXZ getPolygon() {
                return polygon;
            }

            @Override
            public double getRoofEleAt(VectorXZ pos) {
                return getMaxRoofEle() - getRoofHeight();
            }

            protected void renderSpindle(
                    Target<?> target, Material material,
                    SimplePolygonXZ polygon,
                    List<Double> heights, List<Double> scaleFactors) {

                checkArgument(heights.size() == scaleFactors.size(),
                        "heights and scaleFactors must have same size");

                VectorXZ center = polygon.getCenter();

                /* calculate the polygon relative to the center */

                List<VectorXZ> vertexLoop = new ArrayList<VectorXZ>();

                for (VectorXZ v : polygon.makeCounterclockwise().getVertexList()) {
                    vertexLoop.add(v.subtract(center));
                }

                ShapeXZ spindleShape = new SimplePolygonXZ(vertexLoop);

                /* construct a path from the heights */

                List<VectorXYZ> path = new ArrayList<VectorXYZ>();

                for (double height : heights) {
                    path.add(center.xyz(height));
                }

                /* render the roof using shape extrusion */

                target.drawExtrudedShape(materialRoof, spindleShape, path,
                        nCopies(path.size(), VectorXYZ.Z_UNIT), scaleFactors,
                        spindleTexCoordLists(path, spindleShape.getVertexList().size(),
                                polygon.getOutlineLength(), material),
                        null);

            }

            protected List<List<VectorXZ>> spindleTexCoordLists(
                    List<VectorXYZ> path, int shapeVertexCount,
                    double polygonLength, Material material) {

                List<TextureData> textureDataList =
                        material.getTextureDataList();

                switch (textureDataList.size()) {

                    case 0:
                        return emptyList();

                    case 1:
                        return singletonList(spindleTexCoordList(path,
                                shapeVertexCount, polygonLength, textureDataList.get(0)));

                    default:

                        List<List<VectorXZ>> result = new ArrayList<List<VectorXZ>>();

                        for (TextureData textureData : textureDataList) {
                            result.add(spindleTexCoordList(path,
                                    shapeVertexCount, polygonLength, textureData));
                        }

                        return result;

                }

            }

            protected List<VectorXZ> spindleTexCoordList(
                    List<VectorXYZ> path, int shapeVertexCount,
                    double polygonLength, TextureData textureData) {

                List<VectorXZ> result = new ArrayList<VectorXZ>();

                double accumulatedTexHeight = 0;

                for (int i = 0; i < path.size(); i++) {

                    if (i > 0) {

                        accumulatedTexHeight += path.get(i - 1).distanceTo(path.get(i));

                        //TODO use the distance on the extruded surface instead of on the path,
                        //e.g. += rings[i-1].get(0).distanceTo(rings[i].get(0));
                    }

                    result.addAll(spindleTexCoordListForRing(shapeVertexCount,
                            polygonLength, accumulatedTexHeight, textureData));

                }

                return result;

            }

            private List<VectorXZ> spindleTexCoordListForRing(
                    int shapeVertexCount, double polygonLength,
                    double accumulatedTexHeight, TextureData textureData) {

                double textureRepeats = max(1,
                        round(polygonLength / textureData.width));

                double texWidthSteps = textureRepeats / (shapeVertexCount - 1);

                double texZ = accumulatedTexHeight / textureData.height;

                VectorXZ[] texCoords = new VectorXZ[shapeVertexCount];

                for (int i = 0; i < shapeVertexCount; i++) {
                    texCoords[i] = new VectorXZ(i * texWidthSteps, texZ);
                }

                return asList(texCoords);

            }

            @Override
            protected float getDefaultRoofHeight() {
                return (float) polygon.getOuter().getDiameter() / 2;
            }

        }

        private class OnionRoof extends SpindleRoof {

            //@Override
            public void renderTo(Target<?> target) {

                double roofY = getMaxRoofEle() - getRoofHeight();

                renderSpindle(target, materialRoof,
                        polygon.getOuter().makeClockwise(),
                        asList(roofY,
                                roofY + 0.15 * getRoofHeight(),
                                roofY + 0.52 * getRoofHeight(),
                                roofY + 0.72 * getRoofHeight(),
                                roofY + 0.82 * getRoofHeight(),
                                roofY + 1.0 * getRoofHeight()),
                        asList(1.0, 0.8, 1.0, 0.7, 0.15, 0.0));

            }

            @Override
            protected float getDefaultRoofHeight() {
                return (float) polygon.getOuter().getDiameter();
            }

        }

        private class DomeRoof extends SpindleRoof {

            /**
             * number of height rings to approximate the round dome shape
             */
            private static final int HEIGHT_RINGS = 10;

            //@Override
            public void renderTo(Target<?> target) {

                double roofY = getMaxRoofEle() - getRoofHeight();

                List<Double> heights = new ArrayList<Double>();
                List<Double> scales = new ArrayList<Double>();

                for (int ring = 0; ring < HEIGHT_RINGS; ++ring) {
                    double relativeHeight = (double) ring / (HEIGHT_RINGS - 1);
                    heights.add(roofY + relativeHeight * getRoofHeight());
                    scales.add(sqrt(1.0 - relativeHeight * relativeHeight));
                }

                renderSpindle(target, materialRoof,
                        polygon.getOuter().makeClockwise(),
                        heights, scales);

            }

        }

        /**
         * superclass for roofs that have exactly one height value
         * for each point within their XZ polygon
         */
        public abstract class HeightfieldRoof extends TaggedRoof {

            /**
             * returns segments within the roof polygon
             * that define ridges or edges of the roof
             */
            public abstract Collection<LineSegmentXZ> getInnerSegments();

            /**
             * returns segments within the roof polygon
             * that define apex nodes of the roof
             */
            public abstract Collection<VectorXZ> getInnerPoints();

            /**
             * returns roof elevation at a position.
             * Only required to work for positions that are part of the
             * polygon, segments or points for the roof.
             *
             * @return elevation, null if unknown
             */
            protected abstract Double getRoofEleAt_noInterpolation(VectorXZ pos);

            @Override
            public double getRoofEleAt(VectorXZ v) {

                Double ele = getRoofEleAt_noInterpolation(v);

                if (ele != null) {
                    return ele;
                } else {

                    // get all segments from the roof

                    //TODO (performance): avoid doing this for every node

                    Collection<LineSegmentXZ> segments =
                            new ArrayList<LineSegmentXZ>();

                    segments.addAll(this.getInnerSegments());
                    segments.addAll(this.getPolygon().getOuter().getSegments());
                    for (SimplePolygonXZ hole : this.getPolygon().getHoles()) {
                        segments.addAll(hole.getSegments());
                    }

                    // find the segment with the closest distance to the node

                    LineSegmentXZ closestSegment = null;
                    double closestSegmentDistance = Double.MAX_VALUE;

                    for (LineSegmentXZ segment : segments) {
                        double segmentDistance = distanceFromLineSegment(v, segment);
                        if (segmentDistance < closestSegmentDistance) {
                            closestSegment = segment;
                            closestSegmentDistance = segmentDistance;
                        }
                    }

                    // use that segment for height interpolation

                    return interpolateValue(v,
                            closestSegment.p1,
                            getRoofEleAt_noInterpolation(closestSegment.p1),
                            closestSegment.p2,
                            getRoofEleAt_noInterpolation(closestSegment.p2));

                }
            }

            //@Override
            public void renderTo(Target<?> target) {

                /* create the triangulation of the roof */

                Collection<TriangleXZ> triangles;

                try {

                    triangles = Poly2TriUtil.triangulate(
                            getPolygon().getOuter(),
                            getPolygon().getHoles(),
                            getInnerSegments(),
                            getInnerPoints());

                } catch (TriangulationException e) {

                    triangles = JTSTriangulationUtil.triangulate(
                            getPolygon().getOuter(),
                            getPolygon().getHoles(),
                            getInnerSegments(),
                            getInnerPoints());

                }

                List<TriangleXYZ> trianglesXYZ =
                        new ArrayList<TriangleXYZ>(triangles.size());

                for (TriangleXZ triangle : triangles) {
                    TriangleXZ tCCW = triangle.makeCounterclockwise();
                    trianglesXYZ.add(new TriangleXYZ(
                            withRoofEle(tCCW.v1),
                            withRoofEle(tCCW.v2),
                            withRoofEle(tCCW.v3)));
                    //TODO: avoid duplicate objects for points in more than one triangle
                }

                /* draw triangles */

                target.drawTriangles(materialRoof, trianglesXYZ,
                        triangleTexCoordLists(trianglesXYZ,
                                materialRoof, NamedTexCoordFunction.SLOPED_TRIANGLES), new OsmOrigin(getPolygon()));

            }

            private VectorXYZ withRoofEle(VectorXZ v) {
                return v.xyz(getRoofEleAt(v));
            }

        }

        private class FlatRoof extends HeightfieldRoof {

            @Override
            public PolygonWithHolesXZ getPolygon() {
                return polygon;
            }

            @Override
            public Collection<VectorXZ> getInnerPoints() {
                return emptyList();
            }

            @Override
            public Collection<LineSegmentXZ> getInnerSegments() {
                return emptyList();
            }

            @Override
            public double getRoofHeight() {
                return 0;
            }

            @Override
            public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
                return getMaxRoofEle();
            }

            @Override
            public double getMaxRoofEle() {
                return building.getGroundLevelEle() + heightWithoutRoof;
            }

        }

        private class PyramidalRoof extends HeightfieldRoof {

            private final VectorXZ apex;
            private final List<LineSegmentXZ> innerSegments;

            public PyramidalRoof() {

                super();

                SimplePolygonXZ outerPoly = polygon.getOuter();

                apex = outerPoly.getCentroid();

                innerSegments = new ArrayList<LineSegmentXZ>();
                for (VectorXZ v : outerPoly.getVertices()) {
                    innerSegments.add(new LineSegmentXZ(v, apex));
                }

            }

            @Override
            public PolygonWithHolesXZ getPolygon() {
                return polygon;
            }

            @Override
            public Collection<VectorXZ> getInnerPoints() {
                return singletonList(apex);
            }

            @Override
            public Collection<LineSegmentXZ> getInnerSegments() {
                return innerSegments;
            }

            @Override
            public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
                if (apex.equals(pos)) {
                    return getMaxRoofEle();
                } else if (polygon.getOuter().getVertices().contains(pos)) {
                    return getMaxRoofEle() - getRoofHeight();
                } else {
                    return null;
                }
            }

        }

        private class SkillionRoof extends HeightfieldRoof {

            private final LineSegmentXZ ridge;
            private final double roofLength;

            public SkillionRoof() {

                /* parse slope direction */

                VectorXZ slopeDirection = null;

                if (getValue("roof:direction") != null) {
                    Float angle = parseAngle(
                            getValue("roof:direction"));
                    if (angle != null) {
                        slopeDirection = VectorXZ.fromAngle(toRadians(angle));
                    }
                }

                // fallback from roof:direction to roof:slope:direction
                if (slopeDirection == null
                        && getValue("roof:slope:direction") != null) {
                    Float angle = parseAngle(
                            getValue("roof:slope:direction"));
                    if (angle != null) {
                        slopeDirection = VectorXZ.fromAngle(toRadians(angle));
                    }
                }

                if (slopeDirection != null) {

                    SimplePolygonXZ simplifiedOuter =
                            polygon.getOuter().getSimplifiedPolygon();

                    /* find ridge by calculating the outermost intersections of
                     * the quasi-infinite slope "line" towards the centroid vector
                     * with segments of the polygon */

                    VectorXZ center = simplifiedOuter.getCentroid();

                    Collection<LineSegmentXZ> intersections =
                            simplifiedOuter.intersectionSegments(new LineSegmentXZ(
                                    center.add(slopeDirection.mult(-1000)), center));

                    LineSegmentXZ outermostIntersection = null;
                    double distanceOutermostIntersection = -1;

                    for (LineSegmentXZ i : intersections) {
                        double distance = distanceFromLineSegment(center, i);
                        if (distance > distanceOutermostIntersection) {
                            outermostIntersection = i;
                            distanceOutermostIntersection = distance;
                        }
                    }

                    ridge = outermostIntersection;

                    /* calculate maximum distance from ridge */

                    double maxDistance = 0.1;

                    for (VectorXZ v : polygon.getOuter().getVertexLoop()) {
                        double distance = distanceFromLine(v, ridge.p1, ridge.p2);
                        if (distance > maxDistance) {
                            maxDistance = distance;
                        }
                    }

                    roofLength = maxDistance;

                } else {

                    ridge = null;
                    roofLength = Double.NaN;

                }

            }

            @Override
            public PolygonWithHolesXZ getPolygon() {
                return polygon;
            }

            @Override
            public Collection<LineSegmentXZ> getInnerSegments() {
                return emptyList();
            }

            @Override
            public Collection<VectorXZ> getInnerPoints() {
                return emptyList();
            }

            @Override
            protected Double getRoofEleAt_noInterpolation(VectorXZ pos) {
                if (ridge == null) {
                    return getMaxRoofEle();
                } else {
                    double distance = distanceFromLineSegment(pos, ridge);
                    double relativeDistance = distance / roofLength;
                    return getMaxRoofEle() - relativeDistance * getRoofHeight();
                }
            }


        }

        /**
         * tagged roof with a ridge.
         * Deals with ridge calculation for various subclasses.
         */
        abstract private class RoofWithRidge extends HeightfieldRoof {

            /**
             * absolute distance of ridge to outline
             */
            protected final double ridgeOffset;

            protected final LineSegmentXZ ridge;

            /**
             * the roof cap that isType closer to the getFirst vertex of the ridge
             */
            protected final LineSegmentXZ cap1;
            /**
             * the roof cap that isType closer to the getSecond vertex of the ridge
             */
            protected final LineSegmentXZ cap2;

            /**
             * maximum distance of any outline vertex to the ridge
             */
            protected final double maxDistanceToRidge;

            /**
             * creates an instance and calculates the final fields
             *
             * @param relativeRoofOffset distance of ridge to outline
             *                           relative to length of roof cap; 0 if ridge ends at outline
             */
            public RoofWithRidge(double relativeRoofOffset) throws RoofNotPossibleException {

                super();

                SimplePolygonXZ outerPoly = polygon.getOuter();

                SimplePolygonXZ simplifiedPolygon =
                        outerPoly.getSimplifiedPolygon();

                /* determine ridge direction based on tag if it exists,
                 * otherwise choose direction of longest polygon segment */

                VectorXZ ridgeDirection = null;

                if (getValue("roof:direction") != null) {
                    Float angle = parseAngle(
                            getValue("roof:direction"));
                    if (angle != null) {
                        ridgeDirection = VectorXZ.fromAngle(toRadians(angle)).rightNormal();
                    }
                }

                if (ridgeDirection == null && getValue("roof:ridge:direction") != null) {
                    Float angle = parseAngle(
                            getValue("roof:ridge:direction"));
                    if (angle != null) {
                        ridgeDirection = VectorXZ.fromAngle(toRadians(angle));
                    }
                }

                if (ridgeDirection == null) {

                    LineSegmentXZ longestSeg = MinMaxUtil.max(
                            simplifiedPolygon.getSegments(),
                            new Function<LineSegmentXZ, Double>() {
                                public Double apply(LineSegmentXZ s) {
                                    return s.getLength();
                                }

                                ;
                            });

                    ridgeDirection =
                            longestSeg.p2.subtract(longestSeg.p1).normalize();

                    if (area.getTags().contains("roof:orientation", "across")) {
                        ridgeDirection = ridgeDirection.rightNormal();
                    }

                }

                /* calculate the two outermost intersections of the
                 * quasi-infinite ridge line with segments of the polygon */

                VectorXZ p1 = outerPoly.getCentroid();

                Collection<LineSegmentXZ> intersections =
                        simplifiedPolygon.intersectionSegments(new LineSegmentXZ(
                                p1.add(ridgeDirection.mult(-1000)),
                                p1.add(ridgeDirection.mult(1000))
                        ));

                if (intersections.size() < 2) {
                    throw new RoofNotPossibleException(
                            "RoofWithRidge: cannot handle roof geometry for id " + area.getOsmObject().id + ". interections=" + intersections.size());
                }

                //TODO choose outermost instead of any pair of intersections
                Iterator<LineSegmentXZ> it = intersections.iterator();
                cap1 = it.next();
                cap2 = it.next();

                /* base ridge on the centers of the intersected segments
                 * (the intersections itself are not used because the
                 * tagged ridge direction isType likely not precise)       */

                VectorXZ c1 = cap1.getCenter();
                VectorXZ c2 = cap2.getCenter();

                ridgeOffset = min(
                        cap1.getLength() * relativeRoofOffset,
                        0.4 * c1.distanceTo(c2));

                if (relativeRoofOffset == 0) {

                    ridge = new LineSegmentXZ(c1, c2);

                } else {

                    ridge = new LineSegmentXZ(
                            c1.add(p1.subtract(c1).normalize().mult(ridgeOffset)),
                            c2.add(p1.subtract(c2).normalize().mult(ridgeOffset)));

                }

                /* calculate maxDistanceToRidge */

                double maxDistance = 0;

                for (VectorXZ v : outerPoly.getVertices()) {
                    maxDistance = max(maxDistance,
                            distanceFromLineSegment(v, ridge));
                }

                maxDistanceToRidge = maxDistance;

            }

        }

        private class GabledRoof extends RoofWithRidge {
            //erstmal nur als Indicator
            private Float neigung;

            public GabledRoof() throws RoofNotPossibleException {
                super(0);
            }

            public GabledRoof(Float neigung) throws RoofNotPossibleException {
                super(0);
                this.neigung = neigung;
            }

            @Override
            public PolygonWithHolesXZ getPolygon() {

                PolygonXZ newOuter = polygon.getOuter();

                newOuter = insertIntoPolygon(newOuter, ridge.p1, 0.2);
                newOuter = insertIntoPolygon(newOuter, ridge.p2, 0.2);

                return new PolygonWithHolesXZ(
                        newOuter.asSimplePolygon(),
                        polygon.getHoles());

            }

            @Override
            public Collection<VectorXZ> getInnerPoints() {
                return emptyList();
            }

            @Override
            public Collection<LineSegmentXZ> getInnerSegments() {
                return singleton(ridge);
            }

            @Override
            public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
                double distRidge = distanceFromLineSegment(pos, ridge);
                double relativePlacement = distRidge / maxDistanceToRidge;
                return getMaxRoofEle() - /*17.5.19roofHeight*/getRoofHeight() * relativePlacement;
            }

            /**
             * 17.5.19: Der Default von 1 ist doch ungeeignet. Aber bei komplexen Buildings irgendwie besser, weil dann starke Neigungen Artefakte erzeugen.
             *
             * @return
             */
            @Override
            public double getRoofHeight() {
                if (taggedHeight != null) {
                    // dann wird das wohl stimmen
                    return super.getRoofHeight();
                }
                if (neigung == null) {
                    return super.getRoofHeight();
                }
                // Mal 45 Grad annehmen. Ob das so passt mit maxDistanceToRidge
                double height = maxDistanceToRidge;
                return height;
                //return super.getRoofHeight();
            }
        }

        private class HippedRoof extends RoofWithRidge {

            public HippedRoof() throws RoofNotPossibleException {
                super(1 / 3.0);
            }

            @Override
            public PolygonWithHolesXZ getPolygon() {
                return polygon;
            }

            @Override
            public Collection<VectorXZ> getInnerPoints() {
                return emptyList();
            }

            @Override
            public Collection<LineSegmentXZ> getInnerSegments() {
                return asList(
                        ridge,
                        new LineSegmentXZ(ridge.p1, cap1.p1),
                        new LineSegmentXZ(ridge.p1, cap1.p2),
                        new LineSegmentXZ(ridge.p2, cap2.p1),
                        new LineSegmentXZ(ridge.p2, cap2.p2));
            }

            @Override
            public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
                if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
                    return getMaxRoofEle();
                } else if (getPolygon().getOuter().getVertexLoop().contains(pos)) {
                    return getMaxRoofEle() - getRoofHeight();
                } else {
                    return null;
                }
            }

        }

        private class HalfHippedRoof extends RoofWithRidge {

            private final LineSegmentXZ cap1part, cap2part;

            public HalfHippedRoof() throws RoofNotPossibleException {

                super(1 / 6.0);

                cap1part = new LineSegmentXZ(
                        interpolateBetween(cap1.p1, cap1.p2,
                                0.5 - ridgeOffset / cap1.getLength()),
                        interpolateBetween(cap1.p1, cap1.p2,
                                0.5 + ridgeOffset / cap1.getLength()));

                cap2part = new LineSegmentXZ(
                        interpolateBetween(cap2.p1, cap2.p2,
                                0.5 - ridgeOffset / cap1.getLength()),
                        interpolateBetween(cap2.p1, cap2.p2,
                                0.5 + ridgeOffset / cap1.getLength()));

            }

            @Override
            public PolygonWithHolesXZ getPolygon() {

                PolygonXZ newOuter = polygon.getOuter();

                newOuter = insertIntoPolygon(newOuter, cap1part.p1, 0.2);
                newOuter = insertIntoPolygon(newOuter, cap1part.p2, 0.2);
                newOuter = insertIntoPolygon(newOuter, cap2part.p1, 0.2);
                newOuter = insertIntoPolygon(newOuter, cap2part.p2, 0.2);

                return new PolygonWithHolesXZ(
                        newOuter.asSimplePolygon(),
                        polygon.getHoles());

            }

            @Override
            public Collection<VectorXZ> getInnerPoints() {
                return emptyList();
            }

            @Override
            public Collection<LineSegmentXZ> getInnerSegments() {
                return asList(ridge,
                        new LineSegmentXZ(ridge.p1, cap1part.p1),
                        new LineSegmentXZ(ridge.p1, cap1part.p2),
                        new LineSegmentXZ(ridge.p2, cap2part.p1),
                        new LineSegmentXZ(ridge.p2, cap2part.p2));
            }

            @Override
            public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
                if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
                    return getMaxRoofEle();
                } else if (getPolygon().getOuter().getVertexLoop().contains(pos)) {
                    if (distanceFromLineSegment(pos, cap1part) < 0.05) {
                        return getMaxRoofEle()
                                - getRoofHeight() * ridgeOffset / (cap1.getLength() / 2);
                    } else if (distanceFromLineSegment(pos, cap2part) < 0.05) {
                        return getMaxRoofEle()
                                - getRoofHeight() * ridgeOffset / (cap2.getLength() / 2);
                    } else {
                        return getMaxRoofEle() - getRoofHeight();
                    }
                } else {
                    return null;
                }
            }

        }

        private class GambrelRoof extends RoofWithRidge {

            private final LineSegmentXZ cap1part, cap2part;

            public GambrelRoof() throws RoofNotPossibleException {

                super(0);

                cap1part = new LineSegmentXZ(
                        interpolateBetween(cap1.p1, cap1.p2, 1 / 6.0),
                        interpolateBetween(cap1.p1, cap1.p2, 5 / 6.0));

                cap2part = new LineSegmentXZ(
                        interpolateBetween(cap2.p1, cap2.p2, 1 / 6.0),
                        interpolateBetween(cap2.p1, cap2.p2, 5 / 6.0));

            }

            @Override
            public PolygonWithHolesXZ getPolygon() {

                PolygonXZ newOuter = polygon.getOuter();

                newOuter = insertIntoPolygon(newOuter, ridge.p1, 0.2);
                newOuter = insertIntoPolygon(newOuter, ridge.p2, 0.2);
                newOuter = insertIntoPolygon(newOuter, cap1part.p1, 0.2);
                newOuter = insertIntoPolygon(newOuter, cap1part.p2, 0.2);
                newOuter = insertIntoPolygon(newOuter, cap2part.p1, 0.2);
                newOuter = insertIntoPolygon(newOuter, cap2part.p2, 0.2);

                //TODO: add intersections of additional edges with outline?

                return new PolygonWithHolesXZ(
                        newOuter.asSimplePolygon(),
                        polygon.getHoles());

            }

            @Override
            public Collection<VectorXZ> getInnerPoints() {
                return emptyList();
            }

            @Override
            public Collection<LineSegmentXZ> getInnerSegments() {
                return asList(ridge,
                        new LineSegmentXZ(cap1part.p1, cap2part.p2),
                        new LineSegmentXZ(cap1part.p2, cap2part.p1));
            }

            @Override
            public Double getRoofEleAt_noInterpolation(VectorXZ pos) {

                double distRidge = distanceFromLineSegment(pos, ridge);
                double relativePlacement = distRidge / maxDistanceToRidge;

                if (relativePlacement < 2 / 3.0) {
                    return getMaxRoofEle()
                            - 1 / 2.0 * getRoofHeight() * relativePlacement;
                } else {
                    return getMaxRoofEle() - 1 / 3.0 * getRoofHeight()
                            - 2 * getRoofHeight() * (relativePlacement - 2 / 3.0);
                }

            }

        }

        private class RoundRoof extends RoofWithRidge {
            private final static double ROOF_SUBDIVISION_METER = 2.5;

            private final List<LineSegmentXZ> capParts;
            private final int rings;
            private final double radius;

            public RoundRoof() throws RoofNotPossibleException {

                super(0);

                if (getRoofHeight() < maxDistanceToRidge) {
                    double squaredHeight = getRoofHeight() * getRoofHeight();
                    double squaredDist = maxDistanceToRidge * maxDistanceToRidge;
                    double centerY = (squaredDist - squaredHeight) / (2 * getRoofHeight());
                    radius = sqrt(squaredDist + centerY * centerY);
                } else {
                    radius = 0;
                }

                rings = (int) Math.max(3, getRoofHeight() / ROOF_SUBDIVISION_METER);
                capParts = new ArrayList<LineSegmentXZ>(rings * 2);
                // TODO: would be good to vary step size with slope
                float step = 0.5f / (rings + 1);
                for (int i = 1; i <= rings; i++) {
                    capParts.add(new LineSegmentXZ(
                            interpolateBetween(cap1.p1, cap1.p2, i * step),
                            interpolateBetween(cap1.p1, cap1.p2, 1 - i * step)));

                    capParts.add(new LineSegmentXZ(
                            interpolateBetween(cap2.p1, cap2.p2, i * step),
                            interpolateBetween(cap2.p1, cap2.p2, 1 - i * step)));
                }
            }

            @Override
            public PolygonWithHolesXZ getPolygon() {

                PolygonXZ newOuter = polygon.getOuter();

                newOuter = insertIntoPolygon(newOuter, ridge.p1, 0.2);
                newOuter = insertIntoPolygon(newOuter, ridge.p2, 0.2);

                for (LineSegmentXZ capPart : capParts) {
                    newOuter = insertIntoPolygon(newOuter, capPart.p1, 0.2);
                    newOuter = insertIntoPolygon(newOuter, capPart.p2, 0.2);
                }

                //TODO: add intersections of additional edges with outline?
                return new PolygonWithHolesXZ(
                        newOuter.asSimplePolygon(),
                        polygon.getHoles());

            }

            @Override
            public Collection<VectorXZ> getInnerPoints() {
                return emptyList();
            }

            @Override
            public Collection<LineSegmentXZ> getInnerSegments() {

                List<LineSegmentXZ> innerSegments = new ArrayList<LineSegmentXZ>(rings * 2 + 1);
                innerSegments.add(ridge);
                for (int i = 0; i < rings * 2; i += 2) {
                    LineSegmentXZ cap1part = capParts.get(i);
                    LineSegmentXZ cap2part = capParts.get(i + 1);
                    innerSegments.add(new LineSegmentXZ(cap1part.p1, cap2part.p2));
                    innerSegments.add(new LineSegmentXZ(cap1part.p2, cap2part.p1));
                }

                return innerSegments;
            }

            @Override
            public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
                double distRidge = distanceFromLineSegment(pos, ridge);
                double ele;

                if (radius > 0) {
                    double relativePlacement = distRidge / radius;
                    ele = getMaxRoofEle() - radius
                            + sqrt(1.0 - relativePlacement * relativePlacement) * radius;
                } else {
                    // This could be any interpolator
                    double relativePlacement = distRidge / maxDistanceToRidge;
                    ele = getMaxRoofEle() - getRoofHeight() +
                            (1 - (Math.pow(relativePlacement, 2.5))) * getRoofHeight();
                }

                return Math.max(ele, getMaxRoofEle() - getRoofHeight());
            }
        }

        private class MansardRoof extends RoofWithRidge {

            private final LineSegmentXZ mansardEdge1, mansardEdge2;

            public MansardRoof() throws RoofNotPossibleException {

                super(1 / 3.0);

                mansardEdge1 = new LineSegmentXZ(
                        interpolateBetween(cap1.p1, ridge.p1, 1 / 3.0),
                        interpolateBetween(cap2.p2, ridge.p2, 1 / 3.0));

                mansardEdge2 = new LineSegmentXZ(
                        interpolateBetween(cap1.p2, ridge.p1, 1 / 3.0),
                        interpolateBetween(cap2.p1, ridge.p2, 1 / 3.0));

            }

            @Override
            public PolygonWithHolesXZ getPolygon() {
                return polygon;
            }

            @Override
            public Collection<VectorXZ> getInnerPoints() {
                return emptyList();
            }

            @Override
            public Collection<LineSegmentXZ> getInnerSegments() {
                return asList(ridge,
                        mansardEdge1,
                        mansardEdge2,
                        new LineSegmentXZ(ridge.p1, mansardEdge1.p1),
                        new LineSegmentXZ(ridge.p1, mansardEdge2.p1),
                        new LineSegmentXZ(ridge.p2, mansardEdge1.p2),
                        new LineSegmentXZ(ridge.p2, mansardEdge2.p2),
                        new LineSegmentXZ(cap1.p1, mansardEdge1.p1),
                        new LineSegmentXZ(cap2.p2, mansardEdge1.p2),
                        new LineSegmentXZ(cap1.p2, mansardEdge2.p1),
                        new LineSegmentXZ(cap2.p1, mansardEdge2.p2),
                        new LineSegmentXZ(mansardEdge1.p1, mansardEdge2.p1),
                        new LineSegmentXZ(mansardEdge1.p2, mansardEdge2.p2));
            }

            @Override
            public Double getRoofEleAt_noInterpolation(VectorXZ pos) {

                if (ridge.p1.equals(pos) || ridge.p2.equals(pos)) {
                    return getMaxRoofEle();
                } else if (getPolygon().getOuter().getVertexLoop().contains(pos)) {
                    return getMaxRoofEle() - getRoofHeight();
                } else if (mansardEdge1.p1.equals(pos)
                        || mansardEdge1.p2.equals(pos)
                        || mansardEdge2.p1.equals(pos)
                        || mansardEdge2.p2.equals(pos)) {
                    return getMaxRoofEle() - 1 / 3.0 * getRoofHeight();
                } else {
                    return null;
                }

            }

        }

        /**
         * roof that has been mapped with explicit roof edge/ridge/apex elements
         */
        private class ComplexRoof extends HeightfieldRoof {

            private double roofHeight = 0;
            private final Map<VectorXZ, Double> roofHeightMap;
            private PolygonWithHolesXZ simplePolygon;
            private final Collection<LineSegmentXZ> ridgeAndEdgeSegments;

            public ComplexRoof() {

                /* find ridge and/or edges
                 * (apex nodes don't need to be handled separately
                 *  as they should always be part of an edge segment) */

                roofHeightMap = new HashMap<VectorXZ, Double>();
                Set<VectorXZ> nodeSet = new HashSet<VectorXZ>();

                ridgeAndEdgeSegments = new ArrayList<LineSegmentXZ>();

                List<MapNode> nodes = area.getBoundaryNodes();
                boolean usePartRoofHeight = false;

                if (area.getTags().containsKey("roof:height")) {
                    roofHeight = ValueStringParser.parseMeasure(area.getTags().getValue("roof:height"));
                    usePartRoofHeight = true;
                } else
                    roofHeight = DEFAULT_RIDGE_HEIGHT;

                List<MapWaySegment> edges = new ArrayList<MapWaySegment>();
                List<MapWaySegment> ridges = new ArrayList<MapWaySegment>();

                for (MapOverlap<?, ?> overlap : area.getOverlaps()) {

                    if (overlap instanceof MapOverlapWA) {

                        MapWaySegment waySegment = ((MapOverlapWA) overlap).e1;

                        boolean isRidge = waySegment.getTags().contains("roof:ridge", "yes");
                        boolean isEdge = waySegment.getTags().contains("roof:edge", "yes");

                        if (!(isRidge || isEdge))
                            continue;

                        boolean inside = polygon.contains(waySegment.getCenter());

                        // check also endpoints as pnpoly algo isType not reliable when
                        // segment lies on the polygon edge
                        boolean containsStart = nodes.contains(waySegment.getStartNode());
                        boolean containsEnd = nodes.contains(waySegment.getEndNode());

                        if (!inside && !(containsStart && containsEnd))
                            continue;

                        if (isEdge)
                            edges.add(waySegment);
                        else
                            ridges.add(waySegment);

                        ridgeAndEdgeSegments.add(waySegment.getLineSegment());
                    }
                }

                for (MapWaySegment waySegment : edges) {
                    for (MapNode node : waySegment.getStartEndNodes()) {

                        // height of node (above roof base)
                        Float nodeHeight = null;

                        if (node.getTags().containsKey("roof:height")) {
                            nodeHeight = ValueStringParser.parseMeasure(node.getTags()
                                    .getValue("roof:height"));
                            // hmm, shouldnt edges be interpolated? some seem to think they dont
                        } else if (waySegment.getTags().containsKey("roof:height")) {
                            nodeHeight = ValueStringParser.parseMeasure(waySegment.getTags()
                                    .getValue("roof:height"));
                        } else if (node.getTags().contains("roof:apex", "yes")) {
                            nodeHeight = (float) roofHeight;
                        }

                        if (nodeHeight == null) {
                            nodeSet.add(node.getPos());
                            continue;
                        }

                        roofHeightMap.put(node.getPos(), (double) nodeHeight);

                        if (usePartRoofHeight)
                            roofHeight = max(roofHeight, nodeHeight);
                    }
                }

                for (MapWaySegment waySegment : ridges) {
                    // height of node (above roof base)
                    Float nodeHeight = null;

                    if (waySegment.getTags().containsKey("roof:height")) {
                        nodeHeight = ValueStringParser.parseMeasure(waySegment.getTags()
                                .getValue("roof:height"));
                    } else {
                        nodeHeight = (float) roofHeight;
                    }

                    if (usePartRoofHeight)
                        roofHeight = max(roofHeight, nodeHeight);

                    for (MapNode node : waySegment.getStartEndNodes())
                        roofHeightMap.put(node.getPos(), (double) nodeHeight);
                }

                /* join colinear segments, but not the nodes that are connected to ridge/edges
                 * often there are nodes that are only added to join one building to another
                 * but these interfere with proper triangulation.
                 * TODO: do the same for holes */
                List<VectorXZ> vertices = polygon.getOuter().getVertexLoop();
                List<VectorXZ> simplified = new ArrayList<VectorXZ>();
                VectorXZ vPrev = vertices.get(vertices.size() - 2);

                for (int i = 0, size = vertices.size() - 1; i < size; i++) {
                    VectorXZ v = vertices.get(i);

                    if (i == 0 || roofHeightMap.containsKey(v) || nodeSet.contains(v)) {
                        simplified.add(v);
                        vPrev = v;
                        continue;
                    }
                    VectorXZ vNext = vertices.get(i + 1);
                    LineSegmentXZ l = new LineSegmentXZ(vPrev, vNext);

                    // TODO define as static somewhere: 10 cm tolerance
                    if (distanceFromLineSegment(v, l) < 0.01) {
                        continue;
                    }

                    roofHeightMap.put(v, 0.0);
                    simplified.add(v);
                    vPrev = v;
                }

                if (simplified.size() > 2) {
                    try {
                        simplified.add(simplified.get(0));
                        simplePolygon = new PolygonWithHolesXZ(new SimplePolygonXZ(simplified),
                                polygon.getHoles());
                    } catch (InvalidGeometryException e) {
                        System.err.print(e.getMessage());
                        simplePolygon = polygon;
                    }
                } else
                    simplePolygon = polygon;

                /* add heights for outline nodes that don't have one yet */

                for (VectorXZ v : simplePolygon.getOuter().getVertices()) {
                    if (!roofHeightMap.containsKey(v)) {
                        roofHeightMap.put(v, 0.0);
                    }
                }

                for (SimplePolygonXZ hole : simplePolygon.getHoles()) {
                    for (VectorXZ v : hole.getVertices()) {
                        if (!roofHeightMap.containsKey(v)) {
                            roofHeightMap.put(v, 0.0);
                        }
                    }
                }

                /* add heights for edge nodes that are not also
                 * ridge/outline/apex nodes. This will just use base height
                 * for them instead of trying to interpolate heights along
                 * chains of edge segments. Results are therefore wrong,
                 * but there's no reason to map them like that anyway. */

                for (LineSegmentXZ segment : ridgeAndEdgeSegments) {
                    if (!roofHeightMap.containsKey(segment.p1)) {
                        roofHeightMap.put(segment.p1, 0.0);
                    }
                    if (!roofHeightMap.containsKey(segment.p2)) {
                        roofHeightMap.put(segment.p2, 0.0);
                    }
                }

            }

            @Override
            public PolygonWithHolesXZ getPolygon() {
                return simplePolygon;
            }

            @Override
            public Collection<VectorXZ> getInnerPoints() {
                return emptyList();
            }

            @Override
            public Collection<LineSegmentXZ> getInnerSegments() {
                return ridgeAndEdgeSegments;
            }

            @Override
            public double getRoofHeight() {
                return roofHeight;
            }

            @Override
            public Double getRoofEleAt_noInterpolation(VectorXZ pos) {
                if (roofHeightMap.containsKey(pos)) {
                    return building.getGroundLevelEle()
                            + heightWithoutRoof + roofHeightMap.get(pos);
                } else {
                    return null;
                }
            }

            @Override
            public double getMaxRoofEle() {
                return building.getGroundLevelEle()
                        + heightWithoutRoof + roofHeight;
            }

        }

        private class RoofNotPossibleException extends Exception {
            public RoofNotPossibleException(String msg) {
                super(msg);
            }
        }

        public static boolean hasComplexRoof(MapArea area) {
            for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
                if (overlap instanceof MapOverlapWA) {
                    TagGroup tags = overlap.e1.getTags();
                    if (tags.contains("roof:ridge", "yes")
                            || tags.contains("roof:edge", "yes")) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    /**
     * Verwendet area statt Building(Part), weil das eindeutiger ist.
     */
    public static class BuildingCluster {
        //List<MapArea> buildings = new ArrayList<>();
        Map<MapArea, Map<MapArea, Void>> neighbors = new HashMap<>();

        BuildingCluster() {
        }

        public void add(MapArea a0, MapArea a1) {
            Map<MapArea, Void> list = register(a0);
            list.put(a1, null);
            list = register(a1);
            list.put(a0, null);
        }

        public int size() {
            return neighbors.size();
        }

        private Map<MapArea, Void> register(MapArea mapArea) {
            if (neighbors.get(mapArea) == null) {
                neighbors.put(mapArea, new HashMap<>());
            }
            return neighbors.get(mapArea);
        }
    }

    private static class BuildingEntrance implements NodeWorldObject,
            RenderableToAllTargets {

        private final BuildingPart buildingPart;
        private final MapNode node;

        private final O2WEleConnector connector;

        public BuildingEntrance(BuildingPart buildingPart, MapNode node) {
            this.buildingPart = buildingPart;
            this.node = node;
            this.connector = new O2WEleConnector(node.getPos(), node, getGroundState());
        }

        @Override
        public MapNode getPrimaryMapElement() {
            return node;
        }

        @Override
        public Iterable<O2WEleConnector> getEleConnectors() {
            return singleton(connector);
        }

        @Override
        public void defineEleConstraints(EleConstraintEnforcer enforcer) {

            /* TODO for level != null and ABOVE/BELO, add vertical distance to ground */

        }

        @Override
        public GroundState getGroundState() {

            boolean onlyOn = true;
            boolean onlyAbove = true;
            boolean onlyBelow = true;

            for (MapWaySegment waySegment : node.getConnectedWaySegments()) {

                if (waySegment.getPrimaryRepresentation() instanceof
                        AbstractNetworkWaySegmentWorldObject) {

                    switch (waySegment.getPrimaryRepresentation().getGroundState()) {
                        case ABOVE:
                            onlyOn = false;
                            onlyBelow = false;
                            break;
                        case BELOW:
                            onlyOn = false;
                            onlyAbove = false;
                            break;
                        case ON:
                            onlyBelow = false;
                            onlyAbove = false;
                            break;
                    }

                }

            }

            if (onlyOn) {
                return ON;
            } else if (onlyAbove) {
                return ABOVE;
            } else if (onlyBelow) {
                return BELOW;
            } else {
                return ON;
            }

        }

        public int getLevel() {

            try {
                return Integer.parseInt(node.getTags().getValue("level"));
            } catch (NumberFormatException e) {
                return 0;
            }

        }

        @Override
        public void renderTo(Target<?> target) {

            /* calculate a vector that points out of the building */

            VectorXZ outOfBuilding = VectorXZ.Z_UNIT;

            for (SimplePolygonXZ polygon :
                    buildingPart.polygon.getPolygons()) {

                final List<VectorXZ> vs = polygon.getVertexLoop();
                int entranceI = vs.indexOf(node.getPos());

                if (entranceI != -1) {

                    VectorXZ posBefore = vs.get((vs.size() + entranceI - 1) % vs.size());
                    VectorXZ posAfter = vs.get((vs.size() + entranceI + 1) % vs.size());

                    outOfBuilding = posBefore.subtract(posAfter).rightNormal();
                    if (!polygon.isClockwise()) {
                        outOfBuilding = outOfBuilding.invert();
                    }

                    break;

                }

            }

            /* draw the entrance as a box protruding from the building */

            VectorXYZ center = connector.getPosXYZ();

            float height = parseHeight(node.getTags(), 2);
            float width = parseWidth(node.getTags(), 1);

            target.drawBox(Materials.ENTRANCE_DEFAULT,
                    center, outOfBuilding, height, width, 0.1);

        }

    }

    public static enum BuildingClassification {
        GARAGE, SINGLEFAMILIYHOUSE
    }
}
