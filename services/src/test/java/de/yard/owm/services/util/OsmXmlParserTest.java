package de.yard.owm.services.util;

import de.yard.threed.osm2world.OSMData;
import org.junit.jupiter.api.Test;

import static de.yard.owm.testutils.TestUtils.loadFileFromClasspath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OsmXmlParserTest {

    @Test
    public void testK41Segment() throws Exception {

        String xml = loadFileFromClasspath("K41-segment.osm.xml");
        OsmXmlParser parser = new OsmXmlParser(xml);
        OSMData osmData = parser.getData();
        assertNotNull(osmData);
        assertEquals(5, osmData.getNodes().size());
        assertEquals(1, osmData.getWays().size());
    }

}