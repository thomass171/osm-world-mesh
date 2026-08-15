package de.yard.threed.osm2mesh;

import de.yard.threed.core.Degree;
import de.yard.threed.osm2mesh.testutils.ExpectedMeshNodePair;
import de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon;
import de.yard.threed.osm2scenery.SceneryWayConnectorTest;

import static de.yard.threed.osm2mesh.testutils.ExpectedMeshPolygon.expectedConnector;
import static de.yard.threed.osm2scenery.SceneryWayConnectorTest.validateAttachPoint;

public class Expectations {

    public static ExpectedMeshPolygon expectedConnector255563537 = expectedConnector(255563537L, 4,
            new ExpectedMeshNodePair(225794271, new Degree(149), 1, 0),
            new ExpectedMeshNodePair(107468171, new Degree(332), 3, 2),
            new ExpectedMeshNodePair(107468171, new Degree(240), 0, 3));

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


}
