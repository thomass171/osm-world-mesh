package de.yard.owm.services.util;

import de.yard.threed.osm2scenery.util.OsmXmlParser;
import de.yard.threed.osm2world.OSMData;
import org.junit.jupiter.api.Test;

import static de.yard.threed.TestUtil.loadFileFromClasspath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OsmXmlParserTest {

    @Test
    public void testK41Segment() throws Exception {

        String xml = loadFileFromClasspath("K41/K41-segment.osm.xml");
        OsmXmlParser parser = new OsmXmlParser(xml);
        OSMData osmData = parser.getData();
        assertNotNull(osmData);
        assertEquals(5, osmData.getNodes().size());
        assertEquals(1, osmData.getWays().size());
    }

    /**
     * Kenten-South has unused attributes (changeset, timestamp, user, uid, visible) stripped. 'version' is required by osmosis.
     */
    @Test
    public void testKentenSouth() throws Exception {

        String xml = loadFileFromClasspath("Kenten/Kenten-South.osm.xml");
        OsmXmlParser parser = new OsmXmlParser(xml);
        OSMData osmData = parser.getData();
        assertNotNull(osmData);
        assertEquals(1030, osmData.getNodes().size());
        assertEquals(75, osmData.getWays().size());
        assertEquals(13, osmData.getRelations().size());
    }

}