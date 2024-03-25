package de.yard.threed.osm2graph.osm.modules;


/**
 * Für alles, was später als Graph endet bzw. mit Brücken derzeugt wird (und evtl. noch mehr).
 * Also Roads, River, Rails und Bridging.
 * <p>
 * Basiert auf den MapWays, nicht auf Segments.
 * <p>
 * Created on 25.05.18.
 */
/*public class GridModule extends ConfigurableWorldModule {
    public List<Road> roads = new ArrayList<>();
    static Logger logger = Logger.getLogger(GridModule.class);

    @Override
    public void applyTo(MapData mapdata) {
        // logger.debug("apply " + grid);
        // 24.5.18: TODO Irgendwie ist es doch Kokelores, das offenbar jedes Segment als Road definiert wird. Und auch das mit dem Wayset
        for (MapWay mapWay : mapdata.getMapWays()) {
            if (RoadModule.isRoad(mapWay.getTags())) {
                Bucket sbucket = handleNode(mapWay.getStartNode(),mapWay,mapdata);
                if (sbucket != null) {
                    sbucket.add(mapWay);
                }
                Bucket ebucket = handleNode(mapWay.getEndNode(),mapWay,mapdata);
                if (ebucket != null) {
                    ebucket.add(mapWay);
                    if (sbucket != null) {
                        // duplicate buckets.
                        sbucket.merge(ebucket);
                    }
                }
            }
        }
        // make Road from each non empty bucket
        for (Bucket b : buckets) {
            if (b.size() > 0){
            Road road = new Road(b.ways.values(),null);
           roads.add(road);
            }
        }
        logger.debug(""+roads.size()+" roads created.");
    }

    Bucket handleNode(MapNode mapnode,MapWay mapway, MapData mapdata) {
        List<MapWay> next = findSubsequent(mapnode);
        if (next.size() == 1) {
            //gibt es nicht (->dead end) oder 
            return null;
        } else if (next.size() == 2) {
            // single subsequent way. Put both into bucket.
            MapWay way = next.get(0);
            if (way == mapway){
                way=next.get(1);
            }
            Bucket bucket = findBucket(way);
            if (bucket != null) {
                return bucket;
            } else {
                Bucket b = new Bucket();
                buckets.add(b);
                return b;
            }
        } else {
            // intersection
            //Intersection intersection = intersections.get(mapnode.getOsmNode().id);
            return null;
        }
    }

    private List<MapWay> findSubsequent(MapNode mapnode) {
        return mapnode.getMapWays();
    }

    private Bucket findBucket(MapWay way) {
        for (Bucket b : buckets) {
            if (b.contains(way)) {
                return b;
            }
        }
        return null;
    }

    //HashMap<Long,MapWay> buckets = new HashMap<Long,MapWay>();
    List<Bucket> buckets = new ArrayList<>();

    class Bucket {
        HashMap<Long, MapWay> ways = new HashMap<Long, MapWay>();

        Bucket() {

        }

        public void add(MapWay mapWay) {
            ways.put(mapWay.getOsmWay().id, mapWay);
        }

        public boolean contains(MapWay way) {
            return ways.get(way.getOsmWay().id) != null;
        }

        public void merge(Bucket ebucket) {
            for (Long l : ebucket.ways.keySet()) {
                ways.put(l, ebucket.ways.get(l));
            }
            ebucket.ways.clear();
        }

        public int size() {
            return ways.size();
        }
    }

    class Intersection {
        Intersection() {

        }
    }

    private static boolean isRoad(TagGroup tags) {
        if (tags.containsKey("highway")
                && !tags.contains("highway", "construction")
                && !tags.contains("highway", "proposed")) {
            return true;
        } else {
            return tags.contains("railway", "platform")
                    || tags.contains("leisure", "track");
        }
    }
*/
  