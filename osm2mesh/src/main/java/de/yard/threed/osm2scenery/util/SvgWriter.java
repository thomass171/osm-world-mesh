package de.yard.threed.osm2scenery.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import de.yard.threed.core.Vector2;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2scenery.polygon20.MeshHelper;
import de.yard.threed.osm2scenery.polygon20.MeshPolygon;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Using SVG appears easier then HTML because HTML needs Javascript for painting
 */
public class SvgWriter {

    //16.3.26 GridCellBounds gridCellBounds;
    String svgHeader;
    String svgContent = "";
    String svgTrailer = "</g>" + "</svg>";
    String fontSize10px = "10px";
    String fontSize6px = "6px";
    String fontSize4px = "4px";
    private List<SvgPolygon> svgPolygons = new ArrayList<>();
    double minx = Integer.MAX_VALUE;
    double miny = Integer.MAX_VALUE;
    double maxx = Integer.MIN_VALUE;
    double maxy = Integer.MIN_VALUE;
    // should have same sizes to make scaling successful in both span cases?
    static final int width = 800;
    static final int height = 800;

    /*public SvgWriter(GridCellBounds gridCellBounds) {
        this(gridCellBounds.getProjection().getBaseProjection().calcPos(gridCellBounds.getBottomLeft()),
                gridCellBounds.getProjection().getBaseProjection().calcPos(gridCellBounds.getTopRight()));
    }*/

    public SvgWriter(/*VectorXZ bottomLeft, VectorXZ topRight*/) {
        // this.gridCellBounds = gridCellBounds;


    }

    public static SvgWriter build(/*GridCellBounds gridCellBounds*/) {
        return new SvgWriter(/*gridCellBounds*/);
    }

    /**
     * TODO adjust coordinates, but how??
     *
     * @param polygon
     * @return
     */
    public static SvgWriter forSinglePolygon(Polygon polygon) {


        return new SvgWriter(/*new VectorXZ(minx, miny), new VectorXZ(maxx, maxy)*/);
    }

    public void toSvg() {


        /*for (MeshLine line : lines) {
            int x1 = (int) (line.getFrom().getCoordinate().x * scale);
            int y1 = -(int) (line.getFrom().getCoordinate().y * scale);
            int x2 = (int) (line.getTo().getCoordinate().x * scale);
            int y2 = -(int) (line.getTo().getCoordinate().y * scale);
            svg += " <line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" stroke=\"black\"/>\n";
            int tx = x1 + (x2 - x1) / 2;
            int ty = y1 + (y2 - y1) / 2;
            // line label
            svg += svgText(tx, ty, line.getLabel(), fontSize10px);
        }*/

        /*for (MeshNode node : points) {
            int x = (int) (node.getCoordinate().x * scale);
            int y = -(int) (node.getCoordinate().y * scale);
            svg += svgText(x, y, node.getLabel(), fontSize6px);
        }*/

    }


    public SvgWriter addPolygon(Polygon polygon) {
        return addPolygon(polygon, "black");
    }

    public SvgWriter addPolygon(Polygon polygon, String color) {
        return addPolygon(polygon, color, null);
    }

    public SvgWriter addPolygon(Polygon polygon,  LabelMode labelMode) {
        return addPolygon(polygon, "black", labelMode);
    }

    public SvgWriter addPolygon(Polygon polygon, String color, LabelMode labelMode) {
        svgPolygons.add(new SvgPolygon(polygon, color, labelMode));
        return this;
    }

    public SvgWriter addMeshPolygons(List<MeshPolygon> polygons, SceneryProjection projection) {
        for (MeshPolygon mp : polygons) {
            // don't check for null for revealing problems in the data
            addPolygon(MeshHelper.projectPolygon(mp, projection));
        }

        /*for (MeshPolygon polygon : polygons) {
            List<MeshNode> nodes = polygon.getNodes();
            for (int i = 0; i < nodes.size() - 1; i++) {
                svgContent += labelledLine(nodes.get(i).getCoordinate(), nodes.get(i + 1).getCoordinate(), scale, ""/*line.getLabel()* /, fontSize10px, "black");
            }
        }*/
        return this;
    }

    public void writeTmpFile() {
        writeTmpFile("tmp");
    }

    public void writeTmpFile(String basefilename) {
        buildContent();
        String svg = svgHeader + svgContent + svgTrailer;
        // string -> bytes
        try {
            Files.write(Paths.get("/Users/thomas/tmp/" + basefilename + ".svg"), svg.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildContent() {
        for (SvgPolygon svgPolygon : svgPolygons) {
            Coordinate[] coors = svgPolygon.polygon.getCoordinates();

            for (Coordinate c : coors) {
                if (c.x < minx) {
                    minx = c.x;
                }
                if (c.x > maxx) {
                    maxx = c.x;
                }
                if (c.y < miny) {
                    miny = c.y;
                }
                if (c.y > maxy) {
                    maxy = c.y;
                }
            }
        }
        svgHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" baseProfile=\"full\" width=\"" + width + "px\" height=\"" + height + "px\" viewBox=\"0 0 " + width + " " + height + "\">\n";

        // green border
        svgHeader += " <rect x=\"0\" y=\"0\" width=\"" + width + "\" height=\"" + height +
                "\" stroke=\"green\" stroke-width=\"1px\" fill=\"white\"/>\n";

        svgHeader += "<g transform=\"translate(" + width / 2 + "," + height / 2 + ")\">";

        double spanX = maxx - minx;
        double spanY = maxy - miny;

        double scale;
        if (spanX > spanY) {
            scale = (double) width / spanX;
        } else {
            scale = (double) height / spanY;
        }
        Vector2 offset = new Vector2(minx + (maxx - minx) / 2, miny + (maxy - miny) / 2).negate();

        for (SvgPolygon svgPolygon : svgPolygons) {
            Coordinate[] nodes = svgPolygon.polygon.getCoordinates();
            for (int i = 0; i < nodes.length - 1; i++) {
                String lineLabel = "";
                svgContent += labelledLine(nodes[i], nodes[i + 1], offset, scale, lineLabel, fontSize10px, svgPolygon.color);
                if (svgPolygon.labelMode == LabelMode.NODEBYINDEX) {
                    svgContent += svgText(nodes[i], offset, scale, "" + i, fontSize10px);
                }
            }
        }
    }

    private String labelledLine(Coordinate from, Coordinate to, Vector2 offset, double scale, String label, String font, String color) {
        int x1 = (int) ((offset.x + from.x) * scale);
        int y1 = -(int) ((offset.y + from.y) * scale);
        int x2 = (int) ((offset.x + to.x) * scale);
        int y2 = -(int) ((offset.y + to.y) * scale);
        String svg = " <line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" stroke=\"" + color + "\"/>\n";
        int tx = x1 + (x2 - x1) / 2;
        int ty = y1 + (y2 - y1) / 2;
        // line label
        svg += svgText(tx, ty, label, font);
        return svg;
    }

    private String svgText(Coordinate c, Vector2 offset, double scale, String text, String fontSize) {
        return svgText((int) ((offset.x + c.x) * scale), -(int) ((offset.y + c.y) * scale), text, fontSize);
    }

    private String svgText(int x, int y, String text, String fontSize) {
        // text scale also applies to position
        return " <text x=\"" + x + "\" y=\"" + y + "\" font-size=\"" + fontSize + "\" fill=\"" + "black" + "\" transform=\"" + "scale(1.0)" + "\">"
                + text + "</text>\n";

    }

    @Data
    @AllArgsConstructor
    class SvgPolygon {
        Polygon polygon;
        String color;
        LabelMode labelMode;
    }

    public static enum LabelMode {
        NODEBYINDEX
    }
}
