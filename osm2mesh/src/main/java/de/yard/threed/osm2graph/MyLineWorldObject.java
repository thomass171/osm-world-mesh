package de.yard.threed.osm2graph;

/**
 * Created by thomass on 07.06.17.
 */
/*public abstract class MyLineWorldObject implements MyWorldObject {
    Logger logger = Logger.getLogger(MyLineWorldObject.class);
    //SimplePolygonXZ outlinePolygonXZ;
    private SimplePolygonXZ outlinePolygonXZ = null;
    MyMapGraph segment;
    private List<VectorXZ> centerlineXZ = null;

    private List<VectorXZ> leftOutlineXZ = null;
    private List<VectorXZ> rightOutlineXZ = null;
    boolean broken;

    protected MyLineWorldObject(MyMapGraph segment) {

        this.segment = segment;


    }


    public SimplePolygonXZ getOutlinePolygonXZ() {

        if (outlinePolygonXZ == null) {
            calculateXZGeometry();
        }

        if (broken) {
            return null;
        } else {
            return outlinePolygonXZ;
        }

    }

    private void calculateXZGeometry() {
        GraphOrientation graphorientation = segment.orientation;
        centerlineXZ = toXZList(graphorientation.getOutlineFromNode(segment.getNode(0),0));
        double halfWidth = getWidth() * 0.5f;

        leftOutlineXZ = toXZList(graphorientation.getOutlineFromNode(segment.getNode(0),(float) -halfWidth));
        rightOutlineXZ = toXZList(graphorientation.getOutlineFromNode(segment.getNode(0),(float) halfWidth));


        List<VectorXZ> outlineLoopXZ =
                new ArrayList<VectorXZ>(centerlineXZ.size() * 2 + 1);

        outlineLoopXZ.addAll(rightOutlineXZ);

        List<VectorXZ> left = new ArrayList<VectorXZ>(leftOutlineXZ);
        Collections.reverse(left);
        outlineLoopXZ.addAll(left);

        if (outlineLoopXZ.size() > 0) {
            outlineLoopXZ.add(outlineLoopXZ.get(0));
        } else {
            logger.warn("empty outline");
        }

        // check for brokenness

        try {
            outlinePolygonXZ = new SimplePolygonXZ(outlineLoopXZ);
            broken = false;
            //broken = outlinePolygonXZ.isClockwise();
        } catch (InvalidGeometryException e) {
            logger.error(e.getMessage(), e);
            broken = true;
            //connectors = EleConnectorGroup.EMPTY;
        }


    }

    private List<VectorXZ> toXZList(List<Vector3> outline) {
        List<VectorXZ> list = new ArrayList<VectorXZ>();
        for (Vector3 v : outline) {
            list.add(new VectorXZ(v.getX(), v.getZ()));
        }
        return list;
    }

    public float getWidth() {
        return 3.5f;
    }
}
*/