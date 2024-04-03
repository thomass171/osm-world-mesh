package de.yard.threed.osm2graph.osm;

import org.apache.log4j.Logger;


/**
 * 31.5.18: Die Klasse aus OSM2World soll verwendent werden
 */
@Deprecated
public final class TargetUtil {
    static Logger logger = Logger.getLogger(TargetUtil.class);

    /*private TargetUtil() {
    }*/

    /**
     * render all world objects to a target instance
     * that are compatible with that target type
     */
    /*30.5.18 public static <R extends Renderable> void renderWorldObjects(
            final Target<R> target, final /*MapData* /Iterable<MapElement> mapData,
            final boolean renderUnderground) {
        logger.debug("renderWorldObjects");
        for (MapElement mapElement : mapData/*mapData.getMapElements()* /) {
            for (WorldObject r : mapElement.getRepresentations()) {
                if (renderUnderground || true/*r.getGroundState() != GroundState.BELOW* /) {

                    try {
                        renderObject(target, r);
                    } catch (Exception e) {
                        //e.printStackTrace();
                        logger.error(e.getMessage()+". This exception occurred for the following input:"                               + mapElement);
                    }

                }
            }
        }
    }*/

  /*  public static <R extends Renderable> void MyrenderWorldObjects(
            final GraphicsTarget target, final RenderData renderdata,
            final boolean renderUnderground, boolean merge) {
        logger.debug("MyrenderWorldObjects");
        /*for (MyWorldObject r : renderdata.apron) {
            renderWO(target, r);
        }
        for (MyWorldObject r : renderdata.taxiway) {
            renderWO(target, r);
        }* /
        for (MyWorldObject r : renderdata.runway) {
            renderWO(target, r);
        }
        for (GridModule.Road r : renderdata.roads) {
            for (MapWay mapway:r.mapways) {
                render(target, mapway);
            }
        }
        if (merge) {
           /* int id0 = 205060259;
            int id1 = 205060260;
            int id2 = 205060261;
            int wid0 = 217392664;
            SimplePolygonXZ p0 = renderdata.getApron(id0);
            SimplePolygonXZ p1 = renderdata.getApron(id1);
            SimplePolygonXZ p2 = renderdata.getApron(id2);
            SimplePolygonXZ w0 = renderdata.getTaxiway(wid0);
            Polygon jtsp0 = JTSConversionUtil.polygonXZToJTSPolygon(p0);
            Polygon jtsp1 = JTSConversionUtil.polygonXZToJTSPolygon(p1);
            Polygon jtsp2 = JTSConversionUtil.polygonXZToJTSPolygon(p2);
            ArrayList<Geometry> geometryCollection = new ArrayList<Geometry>();
            geometryCollection.add(jtsp1);
            geometryCollection.add(JTSConversionUtil.polygonXZToJTSPolygon(w0));
            geometryCollection.add(jtsp0);
            geometryCollection.add(jtsp2);
            Polygon merged = (Polygon) combineIntoOneGeometry(geometryCollection);
            PolygonWithHolesXZ p = JTSConversionUtil.polygonXZFromJTSPolygon(merged);
            target.draw2D("Road", new PolygonOrigin("merge"), Materials.STEEL, p.getOuter().getVertices(), null);* /
        }
    }*/

    /*

    static Geometry combineIntoOneGeometry(ArrayList<Geometry> geometryCollection) {
        GeometryFactory factory = new GeometryFactory();//)FactoryFinder.getGeometryFactory( null );

        // note the following geometry collection may be invalid (say with overlapping polygons)
        GeometryCollection geometrycollection =
                (GeometryCollection) factory.buildGeometry(geometryCollection);

        return geometrycollection.union();
    }*/

    /*private static void renderWO(GraphicsTarget target, MyWorldObject r) {
        //if (renderUnderground || r.getGroundState() != GroundState.BELOW) {
        try {
            r.renderTo(target);
        } catch (Exception e) {
            System.err.println("ignored exception:");
            //TODO proper logging
            e.printStackTrace();
            System.err.println("this exception occurred for the following input:\n");
        }

    }*/

    /*private static void renderWS(GraphicsTarget target, WaySegmentSet r) {
        //if (renderUnderground || r.getGroundState() != GroundState.BELOW) {
     Util.notyet();
        /*MyMapGraph mmg = new MyMapGraph();
        try {
            for (MapWaySegment w:r.waysegments){
               
LineSegmentXZ seg = w.getLineSegment();
seg.
                  //  SimplePolygonXZ p = getOutlinePolygonXZ();
                  //  if (p!=null) {
                        target.draw2D("Road", new PolygonOrigin(this.segment), material,p.getVertices(), null);
                  //  }    
            }
            
        } catch (Exception e) {
            System.err.println("ignored exception:");
            //TODO proper logging
            e.printStackTrace();
            System.err.println("this exception occurred for the following input:\n");
        }* /

    }*/

 /*   private static void render(GraphicsTarget target, MapWay mapway) {
        MapNode start = mapway.getStartNode();
        MapNode end = mapway.getEndNode();
        target.tile.drawLine(start.getPos(),end.getPos(), Color.GREEN);
                
    }*/

            /**
             * render all world objects to a target instances
             * that are compatible with that target type.
             * World objects are added to a target until the number of primitives
             * reaches the primitive threshold, then the next target isType retrieved
             * from the iterator.
             */
    /*public static <R extends RenderableToPrimitiveTarget> void renderWorldObjects(
            final Iterator<? extends Target<R>> targetIterator,
            final MapData mapData, final int primitiveThresholdPerTarget) {

        final StatisticsTarget primitiveCounter = new StatisticsTarget();

        iterate(mapData.getMapElements(), new Operation<MapElement>() {

            Target<R> currentTarget = targetIterator.next();

            @Override
            public void perform(MapElement e) {
                for (WorldObject r : e.getRepresentations()) {

                    renderObject(primitiveCounter, r);

                    renderObject(currentTarget, r);

                    if (primitiveCounter.getGlobalCount(PRIMITIVE_COUNT)
                            >= primitiveThresholdPerTarget) {
                        currentTarget = targetIterator.next();
                        primitiveCounter.clear();
                    }

                }
            }

        });

    }*/

            /**
             * renders any object to a target instance
             * if it isType a renderable compatible with that target type.
             * Also sends {@link Target#beginObject(WorldObject)} calls.
             */
   /* public static final <R extends Renderable> void renderObject(
            final Target<R> target, Object object) {

        Class<R> renderableType = target.getRenderableType();

        if (renderableType.isInstance(object)) {

            if (object instanceof WorldObject) {
                target.beginObject((WorldObject) object);
            } else {
                target.beginObject(null);
            }

            target.render(renderableType.cast(object));

        } else if (object instanceof RenderableToAllTargets) {

            if (object instanceof WorldObject) {
                target.beginObject((WorldObject) object);
            } else {
                target.beginObject(null);
            }

            ((RenderableToAllTargets) object).renderTo(target);

        }

    }*/

}
