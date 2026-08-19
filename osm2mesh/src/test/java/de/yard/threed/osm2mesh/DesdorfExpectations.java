package de.yard.threed.osm2mesh;

import de.yard.threed.core.Degree;
import de.yard.threed.osm2mesh.testutils.ExpectedMeshNodePair;
import de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon;

import static de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon.expectedConnector;
import static de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon.expectedWay;
import static de.yard.threed.osm2scenery.SceneryWayConnectorTest.validateAttachPoint;

public class DesdorfExpectations {

    public static ExpectedMeshPolygon expectedConnector255563537 = expectedConnector(255563537L, 4,
            new ExpectedMeshNodePair(225794271, new Degree(37), 0, 3),
            new ExpectedMeshNodePair(107468171, new Degree(60), 3, 2),
            new ExpectedMeshNodePair(107468171, new Degree(240), 1, 0));

    public static ExpectedMeshPolygon expectedConnector255563538 = expectedConnector(255563538L, 4,
            new ExpectedMeshNodePair(24927839L, new Degree(149), 1, 0),
            new ExpectedMeshNodePair(182152619L, new Degree(332), 3, 2),
            new ExpectedMeshNodePair(107468171, new Degree(240), 0, 3));

    public static ExpectedMeshPolygon expectedConnector270353278 = expectedConnector(270353278, 4,
            new ExpectedMeshNodePair(24879711, new Degree(327), 0, 3),
            new ExpectedMeshNodePair(107468171, new Degree(59), 3, 2),
            new ExpectedMeshNodePair(107468171, new Degree(237), 1, 0));

    public static ExpectedMeshPolygon expectedConnector445410497 = expectedConnector(445410497, 4,
            // TODO correct?
            new ExpectedMeshNodePair(107468169, new Degree(176), 2, 1),
            new ExpectedMeshNodePair(23696493, new Degree(249), 1, 0),
            new ExpectedMeshNodePair(107468171, new Degree(70), 3, 2),
            new ExpectedMeshNodePair(37935654, new Degree(336), 0, 3));

    public static ExpectedMeshPolygon expectedConnector387409892 = expectedConnector(387409892, 6,
            new ExpectedMeshNodePair(33817500, new Degree(28), 5, 4),
            new ExpectedMeshNodePair(33817499, new Degree(198), 3, 2),
            new ExpectedMeshNodePair(33817499, new Degree(241), 1, 0));

    public static ExpectedMeshPolygon expectedConnector251517906 = expectedConnector(251517906, 4,
            new ExpectedMeshNodePair(182152619, new Degree(152), 1, 0),
            new ExpectedMeshNodePair(182152619, new Degree(335), 3, 2),
            new ExpectedMeshNodePair(225794271, new Degree(175), 0, 3));

    public static ExpectedMeshPolygon expectedConnector387409895 = expectedConnector(387409895, 4,
            new ExpectedMeshNodePair(33817500, new Degree(219), 1, 0),
            new ExpectedMeshNodePair(33817501, new Degree(346), 3, 2));

    public static ExpectedMeshPolygon expectedConnector445409643 = expectedConnector(445409643, 6,
            new ExpectedMeshNodePair(107468171, new Degree(237), 1, 0),
            new ExpectedMeshNodePair(107468171, new Degree(59), 5, 4),
            new ExpectedMeshNodePair(37935545, new Degree(143), 3, 2));

    public static ExpectedMeshPolygon expectedConnector387409890 = expectedConnector(387409890, 6,
            new ExpectedMeshNodePair(107468171, new Degree(239), 1, 0),
            new ExpectedMeshNodePair(107468171, new Degree(60), 3, 2),
            new ExpectedMeshNodePair(33817499, new Degree(2), 5, 4));

    // osmway has 5 nodes
    public static ExpectedMeshPolygon expectedLowerK41 = expectedWay(24927839L, 0, 10);

    // osmway has 6 nodes
    public static ExpectedMeshPolygon expectedUpperK41s0 = expectedWay(182152619L, 0, 4);
    public static ExpectedMeshPolygon expectedUpperK41s1 = expectedWay(182152619L, 0, 4/*10.8.26 10*/);

    // osmway 107468171 has 11 nodes
    ExpectedMeshPolygon expectedK43s4 = expectedWay(107468171L, 0, 20);

    public static ExpectedMeshPolygon[] expectedK43 = new ExpectedMeshPolygon[]{
            expectedWay(107468171L, 0, 10),
            expectedWay(107468171L, 1, 10/*TODO fix 6*/),
            expectedWay(107468171L, 2, 10/*??4*/),
            expectedWay(107468171L, 3, 10/*??4*/),
            expectedWay(107468171L, 4, 4)
    };
}
