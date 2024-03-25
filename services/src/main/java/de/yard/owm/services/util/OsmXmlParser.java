package de.yard.owm.services.util;

import de.yard.threed.osm2world.MapBasedTagGroup;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMDataReader;
import de.yard.threed.osm2world.OSMElement;
import de.yard.threed.osm2world.OSMMember;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.OSMRelation;
import de.yard.threed.osm2world.OSMWay;
import de.yard.threed.osm2world.TagGroup;
import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.SaxParserFactory;
import org.openstreetmap.osmosis.xml.v0_6.impl.OsmHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.SAXParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.yard.threed.osm2world.EmptyTagGroup.EMPTY_TAG_GROUP;

/**
 * Clone of file based OsmosisReader that reads from string
 */
public class OsmXmlParser implements OSMDataReader {

    private boolean complete = false;

    private synchronized boolean isComplete() {
        return complete;
    }

    private synchronized void setCompleteTrue() {
        this.complete = true;
    }

    private List<Bound> bounds = new ArrayList<Bound>();
    private Map<Long, Node> nodesById = new HashMap<Long, Node>();
    private Map<Long, Way> waysById = new HashMap<Long, Way>();
    private Map<Long, Relation> relationsById = new HashMap<Long, Relation>();

    private Collection<OSMNode> ownNodes;
    private Collection<OSMWay> ownWays;
    private Collection<OSMRelation> ownRelations;

    String xmlInput;

    private final Sink sinkImplementation = new Sink() {
        public void initialize(Map<String, Object> arg0) {
            /* do nothing */
        }

        public void close() {
            /* do nothing */
        }

        public void complete() {
            setCompleteTrue();
        }

        public void process(EntityContainer entityContainer) {
            Entity entity = entityContainer.getEntity();
            if (entity instanceof Node) {
                nodesById.put(entity.getId(), ((Node) entity));
            } else if (entity instanceof Way) {
                waysById.put(entity.getId(), ((Way) entity));
            } else if (entity instanceof Relation) {
                relationsById.put(entity.getId(), ((Relation) entity));
            } else if (entity instanceof Bound) {
                bounds.add((Bound) entity);
            }
        }
    };

    /*
     * @param source
     * 		a source providing the input data for the conversion
     */
    public OsmXmlParser(String xmlInput) {
        this.xmlInput = xmlInput;
    }

    private void convertToOwnRepresentation() {

        ownNodes = new ArrayList<OSMNode>(nodesById.size());
        ownWays = new ArrayList<OSMWay>(waysById.size());
        ownRelations = new ArrayList<OSMRelation>(relationsById.size());

        Map<Node, OSMNode> nodeMap = new HashMap<Node, OSMNode>();
        Map<Way, OSMWay> wayMap = new HashMap<Way, OSMWay>();
        Map<Relation, OSMRelation> relationMap = new HashMap<Relation, OSMRelation>();

        for (Node node : nodesById.values()) {

            OSMNode ownNode = new OSMNode(node.getLatitude(), node
                    .getLongitude(), tagGroupForEntity(node), node.getId());

            ownNodes.add(ownNode);
            nodeMap.put(node, ownNode);

        }

        for (Way way : waysById.values()) {

            List<WayNode> origWayNodes = way.getWayNodes();
            List<OSMNode> wayNodes = new ArrayList<OSMNode>(origWayNodes.size());
            for (WayNode origWayNode : origWayNodes) {
                Node origNode = nodesById.get(origWayNode.getNodeId());
                if (origNode != null) {
                    wayNodes.add(nodeMap.get(origNode));
                }
            }

            OSMWay ownWay = new OSMWay(tagGroupForEntity(way),
                    way.getId(), wayNodes);

            ownWays.add(ownWay);
            wayMap.put(way, ownWay);

        }

        for (Relation relation : relationsById.values()) {

            OSMRelation ownRelation = new OSMRelation(
                    tagGroupForEntity(relation), relation.getId(),
                    relation.getMembers().size());

            ownRelations.add(ownRelation);
            relationMap.put(relation, ownRelation);

        }

        // add relation members
        // (needs to be done *after* creation because relations can be members
        // of other relations)

        for (Relation relation : relationMap.keySet()) {

            OSMRelation ownRelation = relationMap.get(relation);

            for (org.openstreetmap.osmosis.core.domain.v0_6.RelationMember member : relation
                    .getMembers()) {

                OSMElement memberObject = null;
                if (member.getMemberType() == EntityType.Node) {
                    memberObject = nodeMap.get(nodesById.get(member
                            .getMemberId()));
                } else if (member.getMemberType() == EntityType.Way) {
                    memberObject = wayMap.get(waysById
                            .get(member.getMemberId()));
                } else if (member.getMemberType() == EntityType.Relation) {
                    memberObject = relationMap.get(relationsById.get(member
                            .getMemberId()));
                } else {
                    continue;
                }

                if (memberObject != null) {

                    OSMMember ownMember = new OSMMember(member
                            .getMemberRole(), memberObject);

                    ownRelation.relationMembers.add(ownMember);

                }

            }

        }

        // give up references to original collections

        nodesById = null;
        waysById = null;
        relationsById = null;

    }

    private TagGroup tagGroupForEntity(Entity entity) {
        if (entity.getTags().isEmpty()) {
            return EMPTY_TAG_GROUP;
        } else {
            Map<String, String> tagMap = new HashMap<String, String>(entity.getTags().size());
            for (Tag tag : entity.getTags()) {
                tagMap.put(tag.getKey(), tag.getValue());
            }
            return new MapBasedTagGroup(tagMap);
        }
    }

    @Override
    public OSMData getData() throws IOException {

        read(xmlInput, sinkImplementation);
        /*source.setSink(sinkImplementation);

        Thread readerThread = new Thread(source);
        readerThread.start();

        while (readerThread.isAlive()) {
            try {
                readerThread.join();
            } catch (InterruptedException e) {
            }
        }

        if (!isComplete()) {
            throw new IOException("couldn't read from data source");
        }*/

        convertToOwnRepresentation();

        return new OSMData(bounds, ownNodes, ownWays, ownRelations);
    }

    // Extrcated from file based org.openstreetmap.osmosis.xml.v0_6.XmlReader

    public void read(String xmlInput, Sink sink) {

        boolean enableDateParsing = false;

        InputStream inputStream = null;
        try {
            inputStream = new StringBufferInputStream(xmlInput);
            SAXParser parser = SaxParserFactory.createParser();
            parser.parse(inputStream, new OsmHandler(sink, enableDateParsing));
            sink.complete();
        } catch (SAXParseException var12) {
            throw new OsmosisRuntimeException("Unable to parse xml file " + ".  publicId=(" + var12.getPublicId() + "), systemId=(" + var12.getSystemId() + "), lineNumber=" + var12.getLineNumber() + ", columnNumber=" + var12.getColumnNumber() + ".", var12);
        } catch (SAXException var13) {
            throw new OsmosisRuntimeException("Unable to parse XML.", var13);
        } catch (IOException var14) {
            throw new OsmosisRuntimeException("Unable to read XML file " + ".", var14);
        } finally {
            sink.close();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException var11) {
                    //log.error(Level.SEVERE, "Unable to close input stream.", var11);
                }

                inputStream = null;
            }

        }

    }
}