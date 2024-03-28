package de.yard.threed.osm2graph.viewer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jtstest.testbuilder.JTSTestBuilder;
import de.yard.threed.core.Util;
import de.yard.threed.core.Vector2;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import de.yard.threed.osm2graph.RenderData;
import de.yard.threed.osm2graph.SceneryBuilder;
import de.yard.threed.osm2graph.osm.GraphicsTarget;
import de.yard.threed.osm2graph.osm.GridCellBounds;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2graph.osm.PolygonInformation;
import de.yard.threed.osm2graph.osm.Processor;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2graph.osm.Tile;
import de.yard.threed.osm2graph.osm.TileProjection;
import de.yard.threed.osm2graph.osm.TriangleAWT;
import de.yard.threed.osm2graph.osm.VertexData;
import de.yard.threed.osm2graph.viewer.layer.EleGroupLayer;
import de.yard.threed.osm2graph.viewer.layer.Layer;
import de.yard.threed.osm2graph.viewer.layer.OSMLayer;
import de.yard.threed.osm2graph.viewer.model.Data;
import de.yard.threed.osm2scenery.RenderedObject;
import de.yard.threed.osm2scenery.SceneryContext;
import de.yard.threed.osm2scenery.SceneryConversionFacade;
import de.yard.threed.osm2scenery.SceneryMesh;
import de.yard.threed.osm2scenery.elevation.ElevationMap;
import de.yard.owm.services.persistence.MeshLine;
import de.yard.threed.osm2scenery.scenery.SceneryFlatObject;
import de.yard.threed.osm2scenery.scenery.SceneryNodeObject;
import de.yard.threed.osm2scenery.scenery.SceneryObject;
import de.yard.threed.osm2scenery.scenery.SceneryWayConnector;
import de.yard.threed.osm2scenery.scenery.TerrainMesh;
import de.yard.threed.osm2scenery.scenery.WorldElement;
import de.yard.threed.osm2scenery.scenery.components.AbstractArea;
import de.yard.threed.osm2scenery.util.PolygonCollection;
import de.yard.threed.osm2world.Config;
import de.yard.threed.osm2world.MapData;
import de.yard.threed.osm2world.MapNode;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OsmOrigin;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.traffic.geodesy.GeoCoordinate;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.yard.threed.core.testutil.Assert.fail;
import static de.yard.threed.osm2scenery.scenery.SceneryObject.Category.ROAD;
import static de.yard.threed.osm2world.Config.MATERIAL_FLIGHT;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

/**
 * Eine 2D Darstellung der OSM Daten erzeugen. Die ist dann, je nach dem, kartenähnlich oder als Textur verwendbar.
 * Ist einfach hilfreich fuer eine zügige Visualisierung.
 * <p>
 * 10.7.18: Das Zeichnen mit Texturen erfordert kompliziertes Mapping (), dass normalerweise die GPU macht. Darum lasse ich die Option
 * weg und stelle stattdessen bei Markieren eines Triangles den dazugehörigen Ausschnitt aus der Textur in einem eigenen Fenster dar.
 * <p>
 * Working dir should be module scenery.
 * <p>
 * Kopie aus OSM2World, 22.5.18
 */
public class Viewer2D extends JFrame {
    MainPanel mainPanel;
    public static Logger logger = Logger.getLogger(Viewer2D.class);
    public static Data data = new Data();
    //static ConversionFacade cf;
    //private final RenderOptions renderOptions = new RenderOptions();
    static Bound osmbound;
    static Viewer2D instance;
    static Processor processor;
    public static String texturedir = "../osmscenery/textures";
    static GridCellBounds gridCellBounds;
    private String lodconfigfilesuffix,materialconfigfilesuffix=MATERIAL_FLIGHT;
    public static boolean supertexture = true;

    private Viewer2D() {
        instance = this;

        setSize(1200, 900);
        setLocation(300, 30);
        mainPanel = new MainPanel();
        getContentPane().add(mainPanel);

        //Vector3 und logging braucht Platform.
        //PlatformHomeBrew.init(new HashMap<String, String>());
        SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });


    }

    public static Viewer2D getInstance() {
        return instance;
    }

    public static void main(String[] arg) {
        //eigene Configuration. 2.4.19: Was heiss das?
        JTSTestBuilder.main(new String[]{});
        try {
            /**
             * RegBez Motorways (das maingrid) als Graphen/Textur und der Rest klassisch mit Feldern.
             * Für die Darstellung im Viewer ist das maingrid eigentlich zu gross (wegen memory).
             */

            //String inputfile = "/Users/thomas/Projekte/Granada/osmdata/maingrid.osm.xml";
            //inputfile = SceneryBuilder.osmdatadir + "/B55-B477(grid).osm.xml";
            //inputfile = SceneryBuilder.osmdatadir + "/K41-segment.osm.xml";
            Viewer2D viewer2D = new Viewer2D(/*null/*, cf, getBounds(osmData)*/);
            //30.5.18 Maingrid frisst Brot. TODO Optional als Layer
            //MainGrid mainGrid = MainGrid.build();
            //Viewer2D.data.setMainGrid(mainGrid);
            viewer2D.setEnabled(true);
            //viewer2D.createProcessor(inputfile);
            //viewer2D.process(null);
            //18.8.18: Initiales Tile über Index
            //SwingUtilities.invokeAndWait(new Runnable() {
            //  @Override
            //public void run() {
            //4 ist B55-B477, 3 ist B55-B477 small, 1 ist Desdorf, 5 EDDK-Small
            viewer2D.mainPanel.buttonpanel.cbo_source.setSelectedIndex(3);
            //}
            //});

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setEnabled(boolean b) {
        //boolean v = isVisible();
        super.setEnabled(b);
        if (b) {
            setVisible(true);
          /*  SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    mainPanel.dataCompleted();
                }
            });*/
        }
    }

    String defaultconfigfilesuffix = "poc";

    public void createProcessor(String filename, String lodconfigsuffix) throws IOException {
        if (!StringUtils.isEmpty(lodconfigsuffix)) {
            lodconfigfilesuffix = lodconfigsuffix;
        } else {
            lodconfigfilesuffix = getConfigfileSuffix(filename);
        }
        setTitle(filename + "(" + lodconfigfilesuffix + ")");

        processor = new Processor(new File(filename), lodconfigfilesuffix, null);
        processor.useosm2world = false;

        //File inputfile = new File(filename/*"/Users/thomass/Projekte/Granada/osmdata/K41-segment.xml"*/);
        //inputfile = new File("/Users/thomass/Projekte/Granada/osmdata/Munster-K33.osm.xml");
        //inputfile = new File("/Users/thomass/Projekte/Granada/osmdata/map.osm.xml");
        /*OSM2World osm2World = OSM2World.buildInstance(inputfile, null);
        OSMData osmData = osm2World.getData();
        Config.reinit(loadConfig());
        cf = osm2World.getConversionFacade();*/


        // process wird separat aufgerufen
    }

    private String getConfigfileSuffix(String basename) {
        String suffix = defaultconfigfilesuffix;
        //23.8.18: Das ist aber doof, weil man damit die GUI unterläuft
        //5.4.19: Jetzt wirds aber angezeigt
        if (basename.contains("B55") || basename.contains("K41")) {
            suffix = "detailed";
        }
        if (basename.contains("Zieverich")) {
            suffix = "superdetailed";
        }
        return suffix;
    }

    public void process(String gridnameX) {
        String gridname = mainPanel.buttonpanel.getGridname();
        GridCellBounds gridCellBounds = null;
        if (gridname != null && gridname.length() > 0) {
            gridCellBounds = GridCellBounds.buildGrid(gridname, Viewer2D.processor.mapData);
        }

        Configuration customconfig = new BaseConfiguration();
        if (mainPanel.nodepanel.cbo_withelevation.isSelected()) {
            customconfig.setProperty("ElevationProvider", "de.yard.threed.osm2scenery.elevation.FixedElevationProvider68");
        }
        // die Test Bereiche sollen alle Roads darstellen
        /*jetzt ueber detailed if (processor.dataSet.getName().contains("B55")||processor.dataSet.getName().contains("K41")) {
            customconfig.setProperty("modules.RoadModule.tagfilter", "highway=*");
        }*/

        processor.reinitConfig(materialconfigfilesuffix,lodconfigfilesuffix/*getConfigfileSuffix(processor.dataSet.getName())*/, customconfig);
        //5.4.19: Terrain? Wofuer? Gilt das noch? 25.4.19: BG soll aber ja optional sein/werden.
        Config.getCurrentConfiguration().setProperty("createTerrain", new Boolean(mainPanel.nodepanel.cbo_emptyTerrain.isSelected()));

        mainPanel.drawPanel.getGridLayer().setGridCellBounds(gridCellBounds);
        try {
            SceneryBuilder.FTR_SMARTGRID = true;
            SceneryBuilder.FTR_SMARTBG = true;
            processor.process(gridCellBounds);
            RenderData/*ConversionFacade.Results*/ results = processor.getResults();
            // 15.6.18: scale und Bounds sind jetzt erst nach process bekannt
            this.osmbound = SceneryBuilder.getBounds(processor.osmData);
            this.gridCellBounds = gridCellBounds;
            // scale ist nur von Basisdaten abhaengig, darum nicht bei jedem neuen Process reseten.
            //9.9.18 jetzt evtl. abhaengig von LAyer mainPanel.drawPanel.setScale();
            //dann geht scale +/- aber nicht mehr
            mainPanel.drawPanel.setScale();
            mainPanel.dataCompleted(results, processor);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static SceneryProjection getProjection() {
        if (processor == null || (processor.scf == null)) {
            // kann beim starten passieren beim mousemove
            return null;
        }
        /*9.4.19 if (processor.cf != null) {
            return processor.cf.getProjection();
        }*/
        return processor.getProjection();//scf.getProjection();
    }

    /**
     * 5.4.19: Zentralisiert, um auf ein Event(GUI) einen neuen Processor anzulegen und zu processen.
     * Damit die aktuellen GUI Einstellungen wirklich verwendet werden.
     */
    public void launch() {
        String configsuffix = (String) mainPanel.buttonpanel.cbo_subconfig.getSelectedItem();

        String source = (String) mainPanel.buttonpanel.cbo_source.getSelectedItem();
        //nur noch das Suffix "grid" rausfiltern. Vorbelegt in der GUI wurde es schon ueber cbo event.
        if (source.endsWith("(grid)")) {
            source = source.replaceAll("\\(grid\\)", "");
        }
        try {
            createProcessor(SceneryBuilder.osmdatadir + "/" + source, configsuffix);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String gridname = (String) mainPanel.buttonpanel.cbo_grid.getSelectedItem();
        process(gridname);
    }
}


class MainPanel extends JPanel {
    protected final ControlPanel nodepanel;
    DrawPanel drawPanel;
    DataPanel datapanel = new DataPanel();
    VertexDataPanel vertexpanel = new VertexDataPanel();
    MaterialsPanel matpanel;
    MapElementPanel mapelementpanel;
    ButtonPanel buttonpanel;
    SceneryObjectsPanel sceneryObjectsPanel;
    JSplitPane eastsplitPane;
    RenderData results;
    JSplitPane mainplitpane;
    JTextField tf_filterosmid;
    JTextField tf_coordinate;

    public MainPanel() {
        setLayout(new BorderLayout());

        nodepanel = new ControlPanel(this);
        //JSplitPane matpanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        drawPanel = new DrawPanel(this);
        //matpanel.add(nodepanel);
        //matpanel.add(new JScrollPane(drawPanel));
        //matpanel.setResizeWeight(0.7f);

        matpanel = new MaterialsPanel(this);
        buttonpanel = new ButtonPanel(this);
        add("North", buttonpanel);
        //add("West", nodepanel);

        JPanel midpanel = new JPanel();
        //midpanel.setLayout(new BoxLayout(midpanel, BoxLayout.LINE_AXIS));
        midpanel.setLayout(new BorderLayout());
        midpanel.add("West", nodepanel);

        //midpanel.add(new JScrollPane(drawPanel));

        mapelementpanel = new MapElementPanel(this);
        sceneryObjectsPanel = new SceneryObjectsPanel(this);
        JTabbedPane tp = new JTabbedPane();
        tp.add("MapElement", mapelementpanel);
        tp.add("Materials", matpanel);
        tp.add("Scenery Objects", sceneryObjectsPanel);

        eastsplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                datapanel, new JScrollPane(vertexpanel));
        JSplitPane midpanelsplitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(drawPanel, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_ALWAYS), eastsplitPane);
        midpanelsplitpane.setDividerLocation(800);
        midpanel.add("Center", midpanelsplitpane);

        // 6.5.19: Kein ScroillPane um tp, sondern bei den Members, da sceneryobjectpanel durch jtable schon einen hat.
        mainplitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                midpanel, /*new JScrollPane*/(tp));
        mainplitpane.setDividerLocation(600);

        //add("Center", new JScrollPane(drawPanel));
        //add("East", eastsplitPane);
        //add("South", new JScrollPane(tp));
        add("Center", mainplitpane);

        // matpanel.setDividerSize(5);
        //matpanel.setDividerLocation(0.25f);
        //setEnabled(false);
        //setResizeWeight(0.26);
        //setTopComponent(screen);

        //setToInitControls();
    }

    public void incscale(float inc) {
        drawPanel.scale *= inc;
        //drawPanel.revalidate();
        drawPanel.paintResults();
        datapanel.showStatistics(drawPanel.results, drawPanel.tile, Viewer2D.processor);

    }

    public void setScale(float scale) {
        Viewer2D.logger.debug("setting scale" + scale);
        drawPanel.scale = scale;
        //drawPanel.revalidate();
        drawPanel.paintResults();
        datapanel.showStatistics(drawPanel.results, drawPanel.tile, Viewer2D.processor);
    }

    public void setWireframe(boolean wireframe) {
        drawPanel.wireframe = wireframe;
        //drawPanel.revalidate();
        drawPanel.paintResults();
    }

    public void setAA(boolean antialiasing) {
        drawPanel.antialiasing = antialiasing;
        drawPanel.paintResults();
    }

    public void setTerrainProviderOnly(boolean to) {
        drawPanel.terrainProviderOnly = to;
        drawPanel.paintResults();
    }

    public void setGrid(boolean enabled) {
        drawPanel.getGridLayer().enabled = enabled;
        //drawPanel.revalidate();
        drawPanel.paintResults();
    }

    public void setPolytest(boolean enabled) {
        drawPanel.getPolytestLayer().enabled = enabled;
        drawPanel.paintResults();
    }

    public void setTriangulator(boolean enabled) {
        drawPanel.getTriangulatorLayer().enabled = enabled;
        drawPanel.paintResults();
    }

    public void setIndicator(boolean enabled) {
        drawPanel.getIndicatorLayer().enabled = enabled;
        drawPanel.paintResults();
    }

    public void setTerrainMesh(boolean enabled) {
        drawPanel.getTerrainMeshLayer().enabled = enabled;
        drawPanel.paintResults();
    }

    public void setOSM(boolean enabled) {
        drawPanel.getOSMLayer().enabled = enabled;
        drawPanel.paintResults();
    }

    public void setEleGroup(boolean enabled) {
        drawPanel.getEleGroupLayer().enabled = enabled;
        drawPanel.paintResults();
    }

    public void setTriangulated(boolean e) {
        drawPanel.triangulated = e;
        drawPanel.paintResults();
    }

    public void setDrawVolumes(boolean e) {
        drawPanel.drawVolumes = e;
        drawPanel.paintResults();
    }

    public void setVisualizeNodes(boolean e) {
        drawPanel.visualizeNodes = e;
        drawPanel.paintResults();
    }

    public void setElegroups(boolean e) {
        //drawPanel.visualizeElegroups = e;
        drawPanel.getEleGroupLayer().enabled = e;
        drawPanel.paintResults();
    }

    /**
     * Daten sind verarbeitet und koennen gezeichnet werden.
     */
    public void dataCompleted(RenderData/*ConversionFacade.Results*/ results, Processor processor) {
        //ConversionFacade.Results results = Viewer2D.data.getConversionResults();
        this.results = results;
        drawPanel.paintData(results);
        datapanel.showStatistics(results, drawPanel.tile, processor);
        matpanel.displayMaterial();
        sceneryObjectsPanel.setData(results.sceneryresults.sceneryMesh.sceneryObjects.objects);
        // 28.9.18: eigentlich kann er nur eine Liste darstellen TODO improve
        if (ElevationMap.hasInstance()) {
            drawPanel.getIndicatorLayer().setCoordinates(ElevationMap.getInstance().problemlist);
        }
        drawPanel.getTerrainMeshLayer().setTerrainMesh(processor.getTerrainMesh());
        drawPanel.getOSMLayer().setOSMData(Viewer2D.getProjection(), Viewer2D.processor.getFileUsed());
    }

    public void showVertexData(int objectid, char elementtag, int subindex) {
        SceneryObject so = results.sceneryresults.sceneryMesh.sceneryObjects.findObjectById(objectid);
        if (so instanceof SceneryFlatObject) {
            SceneryFlatObject ao = ((SceneryFlatObject) so);
            if (subindex == -1) {
                if (ao.getArea()[0].getVertexData() != null) {
                    vertexpanel.fill(ao.getArea()[0].getVertexData());
                }
            } else {
                if (elementtag == 'V') {
                    vertexpanel.fill(ao.volumeProvider.getWorldElements().get(subindex).vertexData);
                }
                if (elementtag == 'D') {
                    vertexpanel.fill(ao.getDecorations().get(subindex).getVertexData());
                }
            }
        }
    }
}

class ControlPanel extends JPanel {
    //  JList list = new JList(new DefaultListModel());
    JCheckBox cbo_emptyTerrain, cbo_withelevation;

    ControlPanel(final MainPanel mainpanel) {
        //top = new SceneNodeTreeNode();
        JScrollPane treeView = new JScrollPane(new JLabel("kkk"));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new JLabel("Build Options:"));

        cbo_emptyTerrain = new JCheckBox("Empty Terrain");
        add(cbo_emptyTerrain);
        cbo_emptyTerrain.setToolTipText("Das OSM2World empty terrain, nicht das Grid");
        cbo_emptyTerrain.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Viewer2D.getInstance().process(null);
            }
        });
        cbo_withelevation = addBox("Elevation", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Viewer2D.getInstance().process(null);
            }
        }, "Evaluation Calculation (mit FixedElevationProvider68)");

        add(new JLabel("Render Options:"));
        final JCheckBox c = new JCheckBox("wireframe");
        add(c);
        c.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainpanel.setWireframe(c.isSelected());
            }
        });
        final JCheckBox a = new JCheckBox("AA");
        add(a);
        a.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainpanel.setAA(a.isSelected());
            }
        });
        addBox("TerrainProviderOnly", e -> mainpanel.setTerrainProviderOnly(((JCheckBox) e.getSource()).isSelected()), "Only render terrain provider");

        addBox("Triangulated", e -> mainpanel.setTriangulated(((JCheckBox) e.getSource()).isSelected()), "Triangulated and textured. Show triangles (only in combination with wireframe).");

        addBox("Volumes", e -> mainpanel.setDrawVolumes(((JCheckBox) e.getSource()).isSelected()), "Render volumes, eg. bridges, buildings. Only triangulated with wireframe. Option ist aber witzlos wegen 2D Darstellung.");

        addBox("Node Objects", e -> mainpanel.setVisualizeNodes(((JCheckBox) e.getSource()).isSelected()), "Visualize node objects, eg. junctions");
        //jetzt als Layer.
        //addBox("Ele Groups", e -> mainpanel.setElegroups(((JCheckBox) e.getSource()).isSelected()), "Render ele connect groups. Gibt es auch, wenn ohne Elevation gebaut wird.");

        add(new JLabel("Layer Options:"));
        addBox("Grid/Connector", e -> mainpanel.setGrid(((JCheckBox) e.getSource()).isSelected()), null);
        addBox("Poly-Test", e -> mainpanel.setPolytest(((JCheckBox) e.getSource()).isSelected()), null);
        addBox("Triangulator-Test", e -> mainpanel.setTriangulator(((JCheckBox) e.getSource()).isSelected()), null);
        addBox("Indicator", e -> mainpanel.setIndicator(((JCheckBox) e.getSource()).isSelected()), null);
        addBox("TerrainMesh", e -> mainpanel.setTerrainMesh(((JCheckBox) e.getSource()).isSelected()), null);
        addBox("OSM", e -> mainpanel.setOSM(((JCheckBox) e.getSource()).isSelected()), null);
        addBox("Ele Groups", e -> mainpanel.setElegroups(((JCheckBox) e.getSource()).isSelected()), "Render ele connect groups. Gibt es auch, wenn ohne Elevation gebaut wird.");
    }

    JCheckBox addBox(String label, ActionListener lis, String tooltip) {
        return addBox(this, label, lis, tooltip);
    }

    static JCheckBox addBox(JPanel p, String label, ActionListener lis, String tooltip) {
        JCheckBox b = new JCheckBox(label);
        p.add(b);
        b.addActionListener(lis);
        if (tooltip != null) {
            b.setToolTipText(tooltip);
        }
        return b;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, 600);
    }
}

class MapElementPanel extends JPanel {
    JTextArea ta = new JTextArea();
    JCheckBox cbo_vertextdata;

    MapElementPanel(final MainPanel mainpanel) {
        //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setLayout(new BorderLayout());
        add("Center", new JScrollPane(ta));
        add("East", new TexturePanel());
        add("West", new MapElementControlPanel(this));

    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, 120);
    }

    class MapElementControlPanel extends JPanel {
        JComboBox cbo_grid;

        MapElementControlPanel(final JPanel parent) {
            setLayout(new FlowLayout());

            cbo_vertextdata = ControlPanel.addBox(this, "VertexData", new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            }, "list VertexData");
        }
    }

    boolean showVertexData() {
        return cbo_vertextdata.isSelected();
    }
}

class TexturePanel extends JPanel {
    JLabel l = new JLabel();
    static TexturePanel instance;
    BufferedImage img, tex;
    TriangleAWT tri;

    TexturePanel() {
        l.setText("");
        // links oben ins Label, damit klar ist, wo das Image ist.
        l.setHorizontalAlignment(SwingConstants.LEFT);
        l.setVerticalAlignment(SwingConstants.TOP);

        setLayout(new BorderLayout());
        add("Center", l);
        instance = this;
        img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        l.setIcon(new ImageIcon(img));

    }

    public void setTexture(BufferedImage texture) {
        tex = texture;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(128, 128);
    }

    public void drawTriangle(TriangleAWT tri) {
        this.tri = tri;
        revalidate();
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        //super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (tex != null) {
            g2d.drawImage(tex, 0, 0, 128, 128, 0, 0, tex.getWidth(), tex.getHeight(), null);
        }
        g2d.setColor(Color.RED);
        //g2d.drawLine(0, 0, 300, 300);
        if (tri != null) {
            Tile.drawMarkedPolygon(g2d, tri.p, false);
            if (tri.uvs != null && tri.uvs.size() > 1) {
                g2d.setColor(Color.BLUE);
                //g2d.drawLine(50, 0, 300, 300);
                Point p0 = uv2Point(tri.uvs.get(0));
                Point plast = p0;
                for (int i = 1; i < tri.uvs.size(); i++) {
                    Point p = uv2Point(tri.uvs.get(i));
                    g2d.drawLine(plast.x, plast.y, p.x, p.y);
                    plast = p;
                }
                g2d.drawLine(plast.x, plast.y, p0.x, p0.y);
            }
        }
        //g2d.dispose();
    }

    /**
     * TODO repeated bzw. ueberhaupt uvs ausserhalb [0,1] geht nicht.
     *
     * @param uv
     * @return
     */
    private Point uv2Point(Vector2 uv) {
        return new Point((int) (128f * uv.getX()), (int) (128f * uv.getY()));
    }
}

/**
 * 26.4.19: Zeigt jetzt auch die Volumes/WorldElements in den Objects an.
 */
class SceneryObjectsPanel extends JPanel {
    JTable table = new JTable();
    DefaultTableModel model;
    List<SceneryObject> sceneryObjects;
    // JCheckBox cbo_vertextdata;
    final MainPanel mainpanel;

    SceneryObjectsPanel(final MainPanel mainpanel) {
        this.mainpanel = mainpanel;
        Object[][] data = {};
        String[] columnNames = {"Id", "Creatortag", "Category", "Osm ID", "OSM Name", "Vertices", "Triangles", "Material"};
        model = new DefaultTableModel(data, columnNames);
        table = new JTable(model);

        setLayout(new BorderLayout());
        add("Center", new JScrollPane(table));

        table.getSelectionModel().addListSelectionListener(event -> {
            //possible to select below?
            int selrow = table.getSelectedRow();
            if (selrow >= 0 && selrow < table.getRowCount()) {
                String id = (String) table.getValueAt(selrow, 0);
                int objectid = Integer.parseInt(StringUtils.substringBefore(id, "-"));
                int subindex = -1;
                char elementtag = ' ';
                if (id.contains("-")) {
                    String subpart = StringUtils.substringAfterLast(id, "-");
                    elementtag = subpart.charAt(0);
                    subindex = Integer.parseInt(subpart.substring(1));
                }
                mainpanel.drawPanel.markSceneryObject(objectid, elementtag, subindex);
                mainpanel.showVertexData(objectid, elementtag, subindex);
            }
        });
    }

    /*6.5.19: Passt besser, wenn man hier nichts festlegt. Aber nur wenn tabbedpane darüber keine Scrollpane hat. @Override
    public Dimension getPreferredSize() {
        return new Dimension(1024, 120);
    }*/

    /*4.6.19: Brauchts das nicht mehr? class SceneryObjectsControlPanel extends JPanel {
        JComboBox cbo_grid;

        SceneryObjectsControlPanel(final JPanel parent) {
            setLayout(new FlowLayout());

            /*cbo_vertextdata = ControlPanel.addBox(this,"VertexData", new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            }, "list VertexData");* /
        }
    }*/

    void setData(List<SceneryObject> sceneryObjects) {
        this.sceneryObjects = sceneryObjects;
        refresh();
    }

    void refresh() {
        Viewer2D.logger.debug("refresh");
        model.setRowCount(0);
        int row = 0;
        for (int i = 0; i < sceneryObjects.size(); i++) {
            SceneryObject so = sceneryObjects.get(i);
            String filter = mainpanel.tf_filterosmid.getText().trim();
            if (filter.length() > 0 && !so.getOsmIdsAsString().contains(filter)) {
                continue;
            }
            String category = "";
            if (so.getCategory() != null) {
                category = so.getCategory().toString();
            }
            String vertices = "-";
            String triangles = "-";
            String material = "";
            String name = "";
            List<WorldElement> worldelements = null;
            List<AbstractArea> decorations = null;
            if (so instanceof SceneryFlatObject) {
                SceneryFlatObject ao = ((SceneryFlatObject) so);
                if (ao.getArea()[0].getVertexData() != null) {
                    vertices = "" + ao.getArea()[0].getVertexData().vertices.size();
                    triangles = "" + ao.getArea()[0].getVertexData().indices.length / 3;
                }
                if (ao.volumeProvider != null) {
                    worldelements = ao.volumeProvider.getWorldElements();
                }
                if (ao.getArea() != null && ao.getArea()[0].material != null) {
                    material = ao.getArea()[0].material.getName();
                }
                decorations = ao.getDecorations();
            }
            model.insertRow(row++, new String[]{Integer.toString(so.id), so.creatortag, category, so.getOsmIdsAsString(), so.getName(), vertices, triangles, material});
            if (worldelements != null) {
                //eg. buildings
                int index = 0;
                for (WorldElement we : worldelements) {
                    vertices = "-";
                    triangles = "-";
                    name = "";
                    material = we.material.getName();
                    if (we.vertexData != null) {
                        vertices = "" + we.vertexData.vertices.size();
                        triangles = "" + we.vertexData.indices.length / 3;
                    }
                    if (we.getName() != null) {
                        name = we.getName();
                    }
                    model.insertRow(row++, new String[]{Integer.toString(so.id) + "-V" + index, "", "WE", "", name, vertices, triangles, material});
                    index++;
                }
            }
            if (decorations != null) {
                int index = 0;
                for (AbstractArea deco : decorations) {
                    vertices = "-";
                    triangles = "-";
                    name = "";
                    material = deco.material.getName();
                    if (deco.getVertexData() != null) {
                        vertices = "" + deco.getVertexData().vertices.size();
                        triangles = "" + deco.getVertexData().indices.length / 3;
                    }
                    model.insertRow(row++, new String[]{Integer.toString(so.id) + "-D" + index, "", "Decoration", "", name, vertices, triangles, material});
                    index++;
                }
            }
        }


    }
}

class ButtonPanel extends JPanel {
    JComboBox cbo_grid;
    JComboBox cbo_source, cbo_subconfig;

    ButtonPanel(final MainPanel mainpanel) {
        setLayout(new FlowLayout(FlowLayout.LEADING));

        add(new JLabel("Sub-Config:"));
        cbo_subconfig = new JComboBox(new String[]{"", "poc", "detailed", "superdetailed"});
        add(cbo_subconfig);
        //cbo_source.setSelectedIndex(0/*2*/);
        cbo_subconfig.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String configname = (String) cb.getSelectedItem();
                //5.4.19 SceneryBuilder.loadConfig(configname);
                Viewer2D.getInstance().launch();
            }
        });

        add(new JLabel("Source:"));
        cbo_source = new JComboBox(new String[]{"K41-segment.osm.xml", "Desdorf.osm.xml", "Munster-K33.osm.xml",
                "B55-B477-small.osm.xml(grid)", "B55-B477.osm.xml(grid)",
                "EDDK-Small.osm.xml(grid)", "EDDK-Complete-Large.osm.xml(grid)",
                "A4A3A1.osm.xml", "A4A3A1.osm.xml(grid)",
                "Zieverich-Sued.osm.xml(grid)", "TestData-Simplified.osm.xml(grid)", "BIKF.osm.xml(grid)", "Wayland.osm.xml(grid)"});
        add(cbo_source);
        //cbo_source.setSelectedIndex(0/*2*/);
        cbo_source.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String s = (String) cb.getSelectedItem();
                try {
                    // Aus Conveniencegruenden das default Grid sofort mit einstellen. Kann immer noch durch User wieder
                    // veraendert werden.
                    String gridname = null;
                    if (s.endsWith("(grid)")) {
                        s = s.replaceAll("\\(grid\\)", "");
                        gridname = StringUtils.substringBefore(s, ".");
                    }
                    //5.4.19 erst spaeter Viewer2D.getInstance().createProcessor(SceneryBuilder.osmdatadir + "/" + s);
                    // process triggered through cbo_grid, so don't call process from here.
                    if (gridname == null) {
                        cbo_grid.setSelectedIndex(0);
                    } else {
                        cbo_grid.setSelectedItem(gridname);
                    }

                    //Viewer2D.getInstance().process(null);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        add(new JLabel("Grid:"));
        //9.4.19: Ohne Grid ist doch völlig witzlos
        cbo_grid = new JComboBox(new String[]{/*"",*/ "Desdorf", "B55-B477-small", "B55-B477", "B55-B477-smallcut",
                "A4A3A1", "EDDK-Small", "EDDK-Complete-Large", "Zieverich-Sued", "TestData-Simplified", "BIKF", "Wayland"});
        add(cbo_grid);
        //cbo_source.setSelectedIndex(0/*2*/);
        cbo_grid.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String gridname = (String) cb.getSelectedItem();
                //5.4.19 erst spaeter Viewer2D.getInstance().process(gridname);
                Viewer2D.getInstance().launch();
            }
        });

        /*3.9.19 den brauchen wir doch nicht mehr
        addButton("Save", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Tile tile = mainpanel.drawPanel.tile;
                if (tile != null) {
                    try {
                        ImageIO.write(tile.getImage(), "png", new File("tmp/t.png"));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });*/

        mainpanel.tf_filterosmid = new JTextField();
        mainpanel.tf_filterosmid.setText("   ");
        mainpanel.tf_filterosmid.setPreferredSize(new Dimension(100, 18));
        mainpanel.tf_filterosmid.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                warn();
            }

            public void removeUpdate(DocumentEvent e) {
                warn();
            }

            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            public void warn() {
                mainpanel.sceneryObjectsPanel.refresh();
            }
        });
        add(mainpanel.tf_filterosmid);

        /*2.6.18 addButton("Refresh", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainpanel.refresh();
            }
        });*/
        // 800 und 1600 sind zu gross -> OOM
        JComboBox cbo_scale = new JComboBox(new String[]{"50 %", "75 %", "100 %", "150 %", "200 %", "300 %", "400 %", "800 %!", "1600 %!"});
        add(cbo_scale);
        // Initial 100%, jaetzt 50
        cbo_scale.setSelectedIndex(0/*2*/);
        cbo_scale.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String s = (String) cb.getSelectedItem();
                float scale = Integer.parseInt(s.substring(0, s.indexOf(" ")));
                mainpanel.setScale(scale / 100f);
            }
        });
        JButton btn_dec = new JButton("-");
        add(btn_dec);
        btn_dec.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainpanel.incscale(0.8f);
            }
        });
        JButton btn_inc = new JButton("+");
        add(btn_inc);
        btn_inc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainpanel.incscale(1.2f);
            }
        });

        mainpanel.tf_coordinate = new JTextField();
        mainpanel.tf_coordinate.setText("   ");
        mainpanel.tf_coordinate.setPreferredSize(new Dimension(100, 18));
        mainpanel.tf_coordinate.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                warn();
            }

            public void removeUpdate(DocumentEvent e) {
                warn();
            }

            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            public void warn() {
                mainpanel.drawPanel.paintResults();
            }
        });
        add(mainpanel.tf_coordinate);
    }

    void addButton(String label, ActionListener lis) {
        JButton btn = new JButton(label);
        add(btn);
        btn.addActionListener(lis);
    }

    String getGridname() {
        String gridname = (String) cbo_grid.getSelectedItem();
        return gridname;
    }
}

/**
 * statische MapDaten, draw Daten, Mouseaction Daten
 * Besser einspaltig als dreispaltig mit drei Rubriken und rechts statt Textarea
 */
class DataPanel extends JPanel {
    HashMap<String, JLabel> fields = new HashMap<String, JLabel>();

    DataPanel() {
        setLayout(new GridBagLayout());
        //setBorder(new TitledBorder("Engine " + nr + s));
        int row0 = 0;
        int row1 = 0;
        int row2 = 0;
        addLabel("OSM Data:", 0, row0++);
        addPair("OSM-Nodes", 0, row0++);
        addPair("OSM-Ways", 0, row0++);

        addLabel("Map Data:", 0, row0++);
        addPair("Nodes", 0, row0++);
        addPair("Way Segments", 0, row0++);
        addPair("Areas", 0, row0++);
        addPair("Polygon Count", 0, row0++);
        addPair("Tile Size", 0, row0++);
        addPair("Triangle Strips", 0, row0++);

        addLabel("Scenery Data:", 0, row0++);
        addPair("SC-Objects", 0, row0++);
        addPair("SC-BG-Polys", 0, row0++);

        addPair("Image Size", 0, row0++);
        addPair("Projection Location", 0, row0++);

        addPair("Geod", 0, row0++);
        addPair("Scale", 0, row0++);
    }

    void addPair(String label, int col, int row) {
        add(new FixedLabel(label), new GridBagConstraints(2 * col, row, 1, 1, 1.0, 1.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        JLabel data = new FixedLabel();
        add(data, new GridBagConstraints(2 * col + 1, row, 1, 1, 1.0, 1.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        fields.put(label, data);
    }

    void addLabel(String label, int col, int row) {
        add(new FixedLabel(label), new GridBagConstraints(2 * col, row, 1, 1, 1.0, 1.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    }

    public void showStatistics(RenderData/*ConversionFacade.Results*/ rresults, Tile tile, Processor processor) {
        int cnt = 0;//results.getMapData().Renderables().size(
        OSMData osmData = processor.osmData;
        setField("OSM-Ways", "" + osmData.getWays().size());
        setField("OSM-Nodes", "" + osmData.getNodes().size());

        /*9.4.19 Die Daten muessen woanders herkommen if (rresults.osm2worldresults != null) {
            ConversionFacade.Results results = rresults.osm2worldresults;
            MapData mapData = results.getMapData();
            setField("Way Segments", "" + mapData.getMapWaySegments().size());
            setField("Nodes", "" + mapData.getMapNodes().size());
            setField("Areas", "" + mapData.getMapAreas().size());
            setField("Polygon Count", "" + cnt);
        }*/
        if (rresults.sceneryresults != null) {
            SceneryConversionFacade.Results results = rresults.sceneryresults;
            MapData mapData = results.getMapData();
            setField("SC-Objects", "" + results.sceneryMesh.sceneryObjects.objects.size());
            setField("SC-BG-Polys", "" + ((results.sceneryMesh.getBackground() == null) ? 0 : results.sceneryMesh.getBackground().background.size()));

        }
    }

    public void setField(String label, Object coor) {
        fields.get(label).setText(coor.toString());
    }

    class FixedLabel extends JLabel {
        FixedLabel(String s) {
            super(s);
        }

        FixedLabel() {

        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(150, d.height);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 600);
    }
}

class VertexDataPanel extends JPanel {
    JTable table = new JTable();
    DefaultTableModel model;

    VertexDataPanel() {
        Object[][] data = {
        };
        String[] columnNames = {"v0", "v1", "v2"};
        model = new DefaultTableModel(data, columnNames);
        table = new JTable(model);

        setLayout(new BorderLayout());
        //table has its own Scrollpane
        add("Center", (table));

        table.getSelectionModel().addListSelectionListener(event -> {
            //possible to select below?
            if (table.getSelectedRow() < table.getRowCount()) {
                String id = (String) table.getValueAt(table.getSelectedRow(), 0);

            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 600);
    }

    class VertexDataControlPanel extends JPanel {
        JComboBox cbo_grid;

        VertexDataControlPanel(final JPanel parent) {
            setLayout(new FlowLayout());

            /*cbo_vertextdata = ControlPanel.addBox(this,"VertexData", new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            }, "list VertexData");*/
        }
    }

    /**
     * Pro Zeile ein Triangle
     */
    void fill(VertexData vertexData) {
        model.setRowCount(0);
        int row = 0;
        for (int i = 0; i < vertexData.indices.length; i += 3) {
            String vo = JtsUtil.toRoundedString(vertexData.vertices.get(vertexData.indices[i]));
            String v1 = JtsUtil.toRoundedString(vertexData.vertices.get(vertexData.indices[i + 1]));
            String v2 = JtsUtil.toRoundedString(vertexData.vertices.get(vertexData.indices[i + 2]));
            model.insertRow(row++, new String[]{vo, v1, v2});
        }
    }
}

/**
 * Alle Materialen. Als List?
 */
class MaterialsPanel extends JPanel {
    //  JList list = new JList(new DefaultListModel());

    MaterialsPanel(final MainPanel mainpanel) {
        //JScrollPane treeView = new JScrollPane(new JLabel("kkk"));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
       /* c.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainpanel.setWireframe(c.isSelected());
            }
        });
*/

    }

    public void displayMaterial() {
      /*  add(new MaterialPanel("WATER", Materials.WATER));
        add(new MaterialPanel("ASPHALT", Materials.ASPHALT));
        add(new MaterialPanel("GRAVEL", Materials.GRAVEL));
        add(new MaterialPanel("EARTH", Materials.EARTH));
        add(new MaterialPanel("FOREST", Materials.EARTH));
*/
    }
}

/**
 * Fuer ein Material
 */
class MaterialPanel extends JPanel {
  /*  JTextField tf_name = new JTextField();
    JLabel l_texture = new JLabel();

    MaterialPanel(String name, Material material) {
        BufferedImage texture = material.getTexture(0);
        setLayout(new GridBagLayout());
        //setBorder(new TitledBorder("Engine " + nr + s));
        int row = 0;
        add(new JLabel("Name"), new GridBagConstraints(0, row, 1, 1, 1.0, 1.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        add(new JLabel(name), new GridBagConstraints(1, row, 1, 1, 1.0, 1.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        row++;
        if (texture != null) {
            String size = "" + texture.getWidth() + "x" + texture.getHeight();
            add(new JLabel(material.getTextureDataList().get(0).file.getName() + "(" + size + ")"), new GridBagConstraints(0, row, 2, 1, 1.0, 5.0
                    , GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            row++;
            //l_texture.setIcon(new ImageIcon(texture));
            add(l_texture, new GridBagConstraints(0, row, 2, 1, 1.0, 5.0
                    , GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            row++;
        }
    }*/
}

/**
 * Ein Scaling per AffineTransform fuehrt schnell zu Artefakten. Daher immer neu zeichnen.
 * Dies Panel arbeitet nicht wie eigentlich üblich über paintComponent() zum Zeichnen aller Elemente.
 * Stattdessen wird alles in ein BufferedImage gezeichnet (Tile) und das wird hier dargestellt.
 * Es scheint besser, das Image per paintComponent() zu zeichnen statt setIcon. Sonst kann man nichts dazumalen.
 * <p>
 * Das mit dem Zoom ist aus https://stackoverflow.com/questions/18158550/zoom-box-for-area-around-mouse-location-on-screen
 */
class DrawPanel extends JPanel {
    //JLabel l = new JLabel();
    //Graphics2D g2d;
    //BufferedImage img = null;
    RenderData results;
    // 0.6 gut fuer Dahlem. Aber 1 passt besser zu Combobox Scaling. 7.6.17: jetzt mal 0.3f;
    // 24.5.18: 0.03 statt 0.3 wehen besserer Sichtbarkeit (initial ganzes Tile). Widerspreicht aber eigentlicher Intention. Naja.
    // 31.5.18. Kann man nicht statisch initialisieren, sondern muss abhaengig der Bound sein.
    double scale = 1;
    boolean wireframe = false;
    boolean antialiasing = false;
    //  10.7.18 geht nicht siehe header boolean textured = false;
    Tile tile;
    GraphicsTarget target;
    // JPanel innerp;
    MainPanel mainpanel;
    public boolean terrainProviderOnly = false;
    // MapProjection projection;
    Layer[] layers = new Layer[]{new GridLayer(), new PolygonLayer(), new TriangulatorLayer(), new IndicatorLayer(), new TerrainMeshLayer(), new OSMLayer(), new EleGroupLayer()};
    public boolean triangulated = false, drawVolumes = false;
    BufferedImage image;
    Point startpoint = null, endpoint = null;
    JPopupMenu popupMenu = new JPopupMenu();
    //wasDragging ist unabhaengig von normal/Layer
    boolean wasPopup, wasDragging;
    public boolean visualizeNodes/*, visualizeElegroups*/;

    DrawPanel(final MainPanel mainpanel) {
        this.mainpanel = mainpanel;
        // set a preferred size for the custom panel. Wichtig fuer Initialgroesse.
        setPreferredSize(new Dimension(1024, 1024));

        //JPanel innerp = new JPanel();
//innerp.setLayout(new FlowLayout());
        //      innerp.add(l);
       /* l.setText("");
        // links oben ins Label, damit der Mouselistener weiss wo das Image ist.
        l.setHorizontalAlignment(SwingConstants.LEFT);
        l.setVerticalAlignment(SwingConstants.TOP);*/

        setLayout(new BorderLayout());
        //add("Center", l);

        popupMenu.add(new JMenuItem("Example Item"));

        /*mousewheel ist doof, da kann man nicht scrollen addMouseWheelListener(new MouseAdapter() { */
        /*l.*/
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                SceneryProjection projection = Viewer2D.getProjection();
                Point p = new Point(e.getX(), e.getY());
                if (tile != null && projection != null) {
                    //System.out.println("mouse moved to "+p);
                    VectorXZ xz = tile.getProjectionLocation(p);
                    GeoCoordinate coor = OsmUtil.toSGGeod(OsmUtil.unproject(projection, xz));
                    mainpanel.datapanel.setField("Projection Location", OsmUtil.round(xz, 2));
                    mainpanel.datapanel.setField("Geod", OsmUtil.round(coor, 5));
                    List<PolygonInformation> polyids = tile.getPolygonsAtLocation(p);
                    if (polyids.size() > 0) {
                        String tooltip = "";// + polyids.size() + " polygons found:\n";
                        // es könnten evtl. sogar mehrere Polies sein.
                        for (int i = 0; i < polyids.size(); i++) {
                            PolygonInformation pinfo = polyids.get(i);
                            //tooltip += id + "\n";
                            OsmOrigin mapelement = pinfo.polygonOrigin;
                            // polygonOrigin soll  nie null sein.
                            if (mapelement != null) {
                                tooltip += "" + mapelement.getInfo() + ",";
                            }
                            // wenn nahe an einem Vertex, den mit anzeigen
                            tooltip += pinfo.getNearVertexInfo(xz);
                        }
                        /*l.*/
                        setToolTipText(tooltip);
                        //System.out.println(tooltip);
                    } else {
                        setToolTipText("");
                    }
                }
            }
        });

        //clicked=pressed+released
        //11.9.19 bei clicked kommt wohl keine popupTrigger Info. Darum hier nichts mehr machen, sondern nur im release
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = new Point(e.getX(), e.getY());
               /* Viewer2D.logger.debug("clicked. popupTrigger="+e.isPopupTrigger());
                if (e.isPopupTrigger()){

                }else {
                    java.util.List<PolygonInformation> polys = tile.getPolygonsAtLocation(p);
                    markPolygons(polys, p);
                    VectorXZ mappedClick = tile.getProjectionLocation(p);
                    for (Layer layer : layers) {
                        if (layer.isEnabled()) {
                            String infotext = layer.mouseClicked(tile, mappedClick);
                            if (infotext != null) {
                                mainpanel.mapelementpanel.ta.append(infotext);
                            }
                        }
                    }
                }*/
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = new Point(e.getX(), e.getY());
                //Viewer2D.logger.debug("pressed.popuptrigger=" + e.isPopupTrigger());
                wasPopup = false;
                if (e.isPopupTrigger()) {
                    if (withControl(e)) {
                        layerLoop(layer -> {
                            layer.openPopup(e.getComponent(), p, tile.getProjectionLocation(p));
                        });
                    } else {
                        //Viewer2D.logger.debug("opening standard popup");
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                    wasPopup = true;
                } else {
                    if (withControl(e)) {
                        layerLoop(layer -> {
                            layer.mousePressed(tile, tile.getProjectionLocation(p));
                        });
                    }
                    //startpoint wird beim drag immer gebraucht
                    startpoint = p;
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = new Point(e.getX(), e.getY());
                //Viewer2D.logger.debug("released. popup=" + e.isPopupTrigger());
                Rectangle r = getDragRectangle();
                // vier moegliche Konstellationen: Click normal/Layer und DragEnde normal/Layer
                // oder 8? Kann auch Popup release sein.
                if (wasPopup) {
                    //nothing to do
                } else {
                    if (wasDragging) {
                        if (r != null && r.width > 0 && r.height > 0) {
                            //dragging completed
                            Viewer2D.logger.debug("setting Magnifier");
                            // Die Area in projected Coordinaten umrechnen. from muss links unten sein.
                            VectorXZ from = tile.pointToVectorXZ(new Point(r.x, r.y + r.height));
                            // und to rechts oben
                            VectorXZ to = tile.pointToVectorXZ(new Point(r.x + r.width, r.y));

                            paintResults(from, to);
                        }

                        /*java.util.List<PolygonInformation> polys = tile.getPolygonsAtLocation(p);
                        markPolygons(polys, p);
                        VectorXZ mappedClick = tile.getProjectionLocation(p);
                        layerLoop(layer -> {
                            String infotext = layer.mouseClicked(tile, mappedClick);
                            if (infotext != null) {
                                mainpanel.mapelementpanel.ta.append(infotext);
                            }
                        });*/
                    } else {
                        //kein Dragging. Dann muss es markieren(einfach clicken) sein. Da wird nicht normal und Layer unterschieden.
                        List<PolygonInformation> polys = tile.getPolygonsAtLocation(p);
                        markPolygons(polys, p);
                        VectorXZ mappedClick = tile.getProjectionLocation(p);
                        for (Layer layer : layers) {
                            if (layer.isEnabled()) {
                                String infotext = layer.mouseReleased(tile, mappedClick);
                                if (infotext != null) {
                                    mainpanel.mapelementpanel.ta.append(infotext);
                                }
                            }
                        }
                    }
                }
                startpoint = endpoint = null;
                wasDragging = wasPopup = false;
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = new Point(e.getX(), e.getY());
                if (withControl(e)) {
                    //wieso kann startpoint null sein?
                    if (startpoint != null) {
                        layerLoop(layer -> {
                            layer.mouseDragged(tile.getProjectionLocation(startpoint), tile.getProjectionLocation(p));
                        });
                    }
                } else {
                    endpoint = p;

                }
                wasDragging = true;
                //Viewer2D.logger.debug("dragged");
                repaint();
            }
        });
    }

    private void layerLoop(LayerHandler layerHandler) {
        for (Layer layer : layers) {
            if (layer.isEnabled()) {
                layerHandler.handle(layer);
            }
        }
    }

    @FunctionalInterface
    static interface LayerHandler {
        void handle(Layer layer);
    }

    /**
     * ctrl löst auch popuptrigger aus. Darum alt
     *
     * @param e
     * @return
     */
    private boolean withControl(MouseEvent e) {
        boolean withControl = false;
        if ((e.getModifiers() & ActionEvent./*CTRL_MASK*/ALT_MASK) == ActionEvent.ALT_MASK) {
            //Viewer2D.logger.debug("CTRL KEY PRESSED");
            withControl = true;
        }
        return withControl;
    }

    private void markPolygons(List<PolygonInformation> polys, Point mouseclickloc) {
        if (polys.size() > 0) {
            String info = "" + polys.size() + " polygons found:\n";
            for (int i = 0; i < polys.size(); i++) {
                String id = polys.get(i).id;

                //info += id + "\n";
                PolygonInformation pi = polys.get(i);
                //info += ((MapArea) mapelement).toString() + "\n";
                info += pi.creatortag + ": ";
                if (pi.polygonOrigin != null) {
                    // 17.8.18: nicht mit newline abschliessem, damit Elevations noch dahinter kommen. Das ist natürlich immer so eine Sache.
                    // Manchmal wäre umgebrochen auch besser
                    info += "(" + pi.polygonOrigin.getInfo() + "),";
                }
                info += "\n   material=" + pi.getMaterialAsString() + "\n   ";
                info += pi.getPolygonDetails("\n    ", mainpanel.mapelementpanel.showVertexData());

                if (pi.getMaterial() != null) {
                    BufferedImage texture = target.textures.get(GraphicsTarget.getTextureNameFromMaterial(pi.getMaterial()));
                    if (texture != null) {
                        TexturePanel.instance.setTexture(texture);
                    }
                }
                TriangleAWT tri = null;
                if (mouseclickloc != null) {
                    //markiertes Polygon/Triangle hervorheben
                    tri = pi.getTriangle(mouseclickloc);
                }
                if (tri != null) {
                    tile.drawMarkedPolygon(tile.getGraphics(), tri.p, true);
                    repaint();
                    TexturePanel.instance.drawTriangle(tri);
                    // 5.9.18: Triangle mit ausgeben
                    info += "Triangle:" + tri.ov[0] + "," + tri.ov[1] + "," + tri.ov[2];
                } else {
                    // kein Triangle oder mouse click da, dann Polygon versuchen
                    if (pi.getPolygon() != null) {
                        tile.drawMarkedPolygon(tile.getGraphics(), pi.getPolygon(), true);
                        repaint();
                    }
                }
                for (VectorXZ poi : pi.pois) {
                    //zur Unterscheidung vielleicht besser Rectangle oder Triangle?
                    tile.drawCircle(poi, 10, Color.cyan);
                }

                info += "\n";
            }
            mainpanel.mapelementpanel.ta.setText(info);
            //paintResults();
            //System.out.println(tooltip);
        } else {
            mainpanel.mapelementpanel.ta.setText("");
        }
    }

    void setScale() {
        // 31.5.18: Scale abhängig von Bound so initialisieren, dass nach dem Start mögliuchst viel auf einen Blick sichtbar ist.
        scale = 1024.0f / get2DRectangle(Viewer2D.getProjection()).getBounds().width;

    }

    /**
     * Indirekt Callback fuer den LoadOSMThread
     */
    public void paintData(RenderData/*ConversionFacade.Results*/ r) {
        Viewer2D.logger.debug("starting paint");
        results = r;
        paintResults();
        // revalidate();
        //repaint();

    }

    public void paintResults() {
        paintResults(null, null);
    }

    /**
     * Ein BufferedImage für das Tile erstellen.
     * Auch fuer Neuzeichnen.
     * Das im paintComponent() zu machen führt zu ständigem repaint.
     * 24.7.18: Der arbeitet eh anders, siehe oben. Zeichnen in ein BufferedImage und das darstellen.
     * 10.4.19: Jetzt wird die Imagegroesse ueber Gridbounds vorgegeben. scale wurde dann schon beim Erstellen beachtet. Hier beim Zeichnen ist es zu
     * spät.
     * 11.9.19: Warum Layer/Indicator direkt ins Tile zeichnen? Gehört doch da nicht.
     */
    private void paintResults(VectorXZ zoomfrom, VectorXZ zoomto) {
        //29.5.18 Bound osmbound = projection.getBound();
        //29.5.18 Viewer2D.logger.debug("osmbound=" + osmbound);
        if (Viewer2D.supertexture) {
            // scale auf der GUI halbwegs uebernehmen.
            int size = (int) (scale * 512);
            double imgscale = Viewer2D.gridCellBounds.getScale((size));
            tile = new Tile(size, size, imgscale, zoomfrom, zoomto);
            mainpanel.datapanel.setField("Tile Size", "" + size + "x" + size);
            mainpanel.datapanel.setField("Image Size", "" + tile.size.width + "x" + tile.size.height);
        } else {
            SceneryProjection projection = Viewer2D.getProjection();
            Rectangle2D projectionarea = get2DRectangle(projection);
            //9.8.18: scale immer neu ermitteln, z.B. wegen PolytestLayer
            //dann geht +/- aber nicht mehr setScale();
            //Viewer2D.logger.debug("origin=" + projection.getOrigin());
            //Viewer2D.logger.debug("rectangle=" + rec);
            tile = new Tile(projectionarea, (float) scale, zoomfrom, zoomto);
            mainpanel.datapanel.setField("Tile Size", "" + Math.round(projectionarea.getWidth()) + "x" + Math.round(projectionarea.getHeight()));
            mainpanel.datapanel.setField("Image Size", "" + tile.size.width + "x" + tile.size.height);

        }
        mainpanel.datapanel.setField("Scale", "" + Util.roundDouble(scale, 2));
        if (results != null) {
            //18.8.18 Bei Polytest kein Image.Dann fehlet aber auch das Layer. 
            if (false && layers[1].enabled) {
                image = null;
            } else {
                // target sollte hier besser nicht gebraucht werden.
                target = tile.paintToImage(results, wireframe, triangulated, drawVolumes, terrainProviderOnly);
                if (visualizeNodes) {
                    Map<Long, SceneryWayConnector> connector = SceneryContext.getInstance().wayMap.getConnectorMapForCategory(ROAD);
                    for (SceneryNodeObject no : connector.values()) {
                        tile.drawCircle(no.node.getPos(), 20, Color.red);
                    }
                }
                //TODO: kann weg, das ist jetzt als Layer
                /*if (visualizeElegroups) {
                    for (EleConnectorGroup no : EleConnectorGroup.elegroups.values()) {
                        tile.drawCircle(no.mapNode.getPos(), 15, Color.orange);
                    }
                }*/
                BufferedImage img = tile.getImage();
                image = img;
            }
            setPreferredSize(tile.size);

            // l.setIcon(new ImageIcon(img));
            mainpanel.datapanel.setField("Triangle Strips", "" + target.trianglestrips);

            //Layer kommt ins Image. 24.7.18: Überraschend das das sichtbar ist. 11.9.19 Warum? und warum kommt das ins Image?
            /*nicht mehr ins Image for (Layer layer : layers) {
                if (layer.isEnabled()) {
                    layer.draw(tile);
                }
            }*/

            /*String coordinate = mainpanel.tf_coordinate.getText().trim();
            if (coordinate.length() > 0 && coordinate.contains(",")) {
                String[] part = coordinate.split(",");
                if (part.length == 2 && part[0].trim().length() > 0 && part[1].trim().length() > 0) {


                    double x = Util.parseDouble(part[0]);
                    double y = Util.parseDouble(part[1]);
                    Coordinate c = new Coordinate(x, y);
                    tile.drawCircle(c, 10, Color.black);
                }
            }*/
        }
        repaint();
        //tile.drawLine(new Point(0, 0), new Point(300, 300), Color.RED);
        /*if (Viewer2D.data.mainGrid != null) {
            drawMainGrid(tile);
        }*/
    }

    /**
     * deprecated wegen supertexture und grid.
     *
     * @param projection
     * @return
     */
    @Deprecated
    public Rectangle2D get2DRectangle(SceneryProjection projection) {
        if (getPolytestLayer().enabled && false) {
            // das ist jetzt aber mal ne Kruecke
            //return new Rectangle2D.Double(-12000, -12000, 24000, 24000);
            return new Rectangle2D.Double(-120, -120, 240, 240);
        }
        //29.5.18: Tja?
        double bottom = Viewer2D.osmbound.getBottom();
        double right = Viewer2D.osmbound.getRight();
        if (Viewer2D.gridCellBounds != null && Viewer2D.gridCellBounds.getRight() > right) {
            right = Viewer2D.gridCellBounds.getRight();
        }
        VectorXZ lowerleft = OsmUtil.calcPos(projection, bottom, Viewer2D.osmbound.getLeft());
        VectorXZ upperright = OsmUtil.calcPos(projection, Viewer2D.osmbound.getTop(), right);
        //
        double x = lowerleft.getX();
        double y = lowerleft.getZ();
        //18.8.18: manhcmal fehlt rechts was, irgendwas stimmt da was nicht
        double rectwidth = (upperright.getX() + 400) - x;
        double rectheight = upperright.getZ() - y;
        Rectangle2D r = new Rectangle2D.Double(x, y,
                rectwidth,
                rectheight);
        //Bound geht nicht
        return r;


    }

    /*private void drawMainGrid(Tile tile) {
        for (SGGeod coor : Viewer2D.data.mainGrid.connector.coors) {
            VectorXZ v = projection.calcPos(coor);
            tile.drawCircle(v, 20, Color.BLUE);
        }
    }*/

    /**
     * 24.7.18: Jetzt doch verwendet, denn das Hauptzeichenwerk ist ja ein Image/Tile. Das wird nicht repainted.
     *
     * @param g
     */
    @Override
    protected void paintComponent(Graphics g) {
        //Viewer2D.logger.debug("paintComponent");
        // Das Image?
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int x, y;
        if (image != null) {
            int imageWidth = image.getWidth(this);
            int imageHeight = image.getHeight(this);
            x = (w - imageWidth) / 2;
            y = (h - imageHeight) / 2;
            g2.drawImage(image, 0, 0, this);
        }
        // und jetzt die Zuseaetze
        drawCoordinateIndicator(g2);
        drawDragIndicator(g2);
        drawLayer(g2);
        g2.dispose();
    }

    /**
     * 11.9.19: Jetzt im repaint, dafuer wird scrollen langsamer
     *
     * @param g2
     */
    void drawLayer(Graphics2D g2) {
        for (Layer layer : layers) {
            if (layer.isEnabled()) {
                layer.draw(g2, tile);
            }
        }
    }

    void drawCoordinateIndicator(Graphics2D g2) {
        String coordinate = mainpanel.tf_coordinate.getText().trim();
        if (coordinate.length() > 0 && coordinate.contains(",")) {
            String[] part = coordinate.split(",");
            if (part.length == 2 && part[0].trim().length() > 0 && part[1].trim().length() > 0) {
                double x = Util.parseDouble(part[0]);
                double y = Util.parseDouble(part[1]);
                Coordinate c = new Coordinate(x, y);
                //tile.drawCircle(c, 10, Color.black);
                DrawHelper.drawCircle(g2, tile.coordinateToPoint(c), 10, Color.black);
            }
        }
    }

    void drawDragIndicator(Graphics2D g2) {
        Graphics2D g2d = g2;//(Graphics2D) g;
        g2d.setColor(Color.RED);
        //g2d.drawLine(0, 0, 300, 300);
        Rectangle r = getDragRectangle();
        if (r != null) {
            g2d.drawRect(r.x, r.y, r.width, r.height);
        }
    }

    Rectangle getDragRectangle() {
        if (startpoint != null && endpoint != null) {
            int w = Math.abs(startpoint.x - endpoint.x);
            int h = Math.abs(startpoint.y - endpoint.y);
            int x = Math.min(startpoint.x, endpoint.x);
            int y = Math.min(startpoint.y, endpoint.y);
            return new Rectangle(x, y, w, h);
        }
        return null;
    }

    /*17.7.19: Wird doch oben ueber tile size gesetzt. @Override
    public Dimension getPreferredSize() {
        return new Dimension(1024, 1024);
    }*/

    //Dimension size = new Dimension(200, 200);
    //size = new Dimension(Tile.SIZE + 24, Tile.SIZE + 24);
        /*if (img != null) {
            size.width = Math.round(img.getWidth() * scale);
            size.height = Math.round(img.getHeight() * scale);
        }* /
        return size;
    }
    
   /* @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.drawString("BLAHg", 60, 60);

        g2d.setColor(Color.red);
        g2d.drawString("BLAH", 20, 20);
        g2d.drawRect(200, 200, 200, 200);
        Geometry geo;
             geo = Sample.buildSampleA();
       drawGeometry(g2d,geo);
        drawGeometry(g2d,Sample.buildSampleB());

    }*/

    GridLayer getGridLayer() {
        return (GridLayer) layers[0];
    }

    PolygonLayer getPolytestLayer() {
        return (PolygonLayer) layers[1];
    }

    TriangulatorLayer getTriangulatorLayer() {
        return (TriangulatorLayer) layers[2];
    }

    IndicatorLayer getIndicatorLayer() {
        return (IndicatorLayer) layers[3];
    }

    TerrainMeshLayer getTerrainMeshLayer() {
        return (TerrainMeshLayer) layers[4];
    }

    OSMLayer getOSMLayer() {
        return (OSMLayer) layers[5];
    }

    EleGroupLayer getEleGroupLayer() {
        return (EleGroupLayer) layers[6];
    }

    /**
     * Polygon des Obects bzw. Component hervorheben.
     */
    void markSceneryObject(int objectid, char elementtag, int subindex) {
        RenderedObject ro = target.rendermap.get(objectid);
        if (ro == null) {
            Viewer2D.logger.error("Object id not found: " + objectid);
            return;
        }
        if (subindex == -1) {
            markPolygons(ro.pinfo, null);
        } else {
            if (elementtag == 'V') {
                markPolygons(ro.volumeinfo.subList(subindex, subindex + 1), null);
            }
            if (elementtag == 'D') {
                markPolygons(ro.decorationinfo.subList(subindex, subindex + 1), null);
            }
        }
    }
}


class GridLayer extends Layer {
    private GridCellBounds gridCellBounds;

    GridLayer() {
    }

    @Override
    public void draw(Graphics2D g, TileProjection tileProjection) {
        //tile.drawLine(new Point(0, 0), new Point(200, 300), Color.GREEN);
        if (gridCellBounds != null) {
            List<VectorXZ> points = new ArrayList<>();
            /*for (int i = 0; i < gridCellBounds.boundNodes.length; i++) {
                points.add(gridCellBounds.boundNodes[i].getPos());
            }*/
            /*for (int i = 0; i < gridCellBounds.simplePolygonXZ.size(); i++) {
                points.add(gridCellBounds.simplePolygonXZ.getVertex(i));
            }*/
            //tile.drawPolygon("Grid", gridCellBounds.getPolygon(), null, Color.ORANGE, false);
            DrawHelper.drawArea(g, tileProjection.toArea(gridCellBounds.getPolygon()), Color.ORANGE, false);

            for (MapNode n : gridCellBounds.additionalGridnodes) {
                //tile.drawCircle(n.getPos(), 20, Color.BLUE);
                DrawHelper.drawCircle(g, tileProjection.vectorxzToPoint(n.getPos()), 20, Color.BLUE);
            }
        }
    }

    public void setGridCellBounds(GridCellBounds gridCellBounds) {
        this.gridCellBounds = gridCellBounds;
    }
}

/**
 * Darstellung irgendeines Polygons zu Test/Analysezwecken.
 */
class PolygonLayer extends Layer {

    PolygonLayer() {
    }

    public void draw(Graphics2D g, TileProjection tileProjection) {
        com.vividsolutions.jts.geom.Polygon monsterpolygonausmaingrid = null;
        try {
            monsterpolygonausmaingrid = (com.vividsolutions.jts.geom.Polygon) JtsUtil.buildFromWKT(IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("monsterpolygonfrommaingrid.txt")));
        } catch (Exception e) {
            e.printStackTrace();
            fail("file not found");
            return;
        }
        //tile.drawGeometry(monsterpolygonausmaingrid, null, Color.ORANGE, false, null, null);
        com.vividsolutions.jts.geom.Polygon polygon = SceneryMesh.buildRectangleWithHole();

        //List<com.vividsolutions.jts.geom.Polygon> result = JtsUtil.triangulateIncremental(polygon);
        //for (com.vividsolutions.jts.geom.Polygon p : result) {
        //    tile.drawGeometry(p, null, Color.ORANGE, false, null, null);
        //}

        com.vividsolutions.jts.geom.Polygon p = PolygonCollection.getSplitFailPolygon();
        p = PolygonCollection.getTriFailPolygon();
        if (!p.isValid()) {
            Viewer2D.logger.error("Polygon not valid");
        }
        //tile.drawGeometry(p, null, Color.ORANGE, false, null, null);
        //tile.drawMarkedPolygon(tile.getGraphics(), p, false,Color.RED);

        com.vividsolutions.jts.geom.Polygon[] splitresult = JtsUtil.splitPolygon(p);
        //TODO 11.9.19 tile.drawMarkedPolygon(tile.getGraphics(), splitresult[1], false, Color.RED);

    }

}

/**
 * Darstellung des Triangulators zu Test/Analysezwecken.
 */
class TriangulatorLayer extends Layer {

    TriangulatorLayer() {
    }

    public void draw(Graphics2D g, TileProjection tileProjection) {

        //tile.drawGeometry(monsterpolygonausmaingrid, null, Color.ORANGE, false, null, null);
        com.vividsolutions.jts.geom.Polygon polygon = SceneryMesh.buildRectangleWithHole();

        //List<com.vividsolutions.jts.geom.Polygon> result = JtsUtil.triangulateIncremental(polygon);
        //Triangulator triangulator = new Triangulator(SceneryMesh.buildRectangleWithHole());
        //List<LineSegment> result = triangulator.buildGrid();
        com.vividsolutions.jts.geom.Polygon rectangleWithHole = SceneryMesh.buildRectangleWithHole();
        com.vividsolutions.jts.geom.Polygon[] splitresult = JtsUtil.removeHoleFromPolygonBySplitting(rectangleWithHole);
        //11.9.19 TODO tile.drawGeometry(splitresult[0], null, Color.ORANGE, false, null, null);
        //11.9.19 TODO tile.drawGeometry(splitresult[1], null, Color.GREEN, false, null, null);
//        for (LineSegment p : result) {
        //          tile.drawLine(p.p0,p.p1, Color.ORANGE);
        //    }

    }

}

/**
 * Zur Darstellung von z.B. Problempunkten.
 */
class IndicatorLayer extends Layer {

    private List<Coordinate> coordinates;

    IndicatorLayer() {
    }

    public void draw(Graphics2D g2d, TileProjection tileProjection) {
        if (coordinates == null) {
            return;
        }
        for (Coordinate n : coordinates) {
            //tile.drawCircle(n, 10, Color.YELLOW);
            DrawHelper.drawCircle(g2d, tileProjection.coordinateToPoint(n), 10, Color.YELLOW);
        }
    }

    public void setCoordinates(List<Coordinate> coordinates) {
        this.coordinates = coordinates;
    }
}

class TerrainMeshLayer extends Layer {
    private TerrainMesh terrainMesh;

    TerrainMeshLayer() {
    }

    public void draw(Graphics2D g, TileProjection tileProjection) {
        if (terrainMesh != null) {
            for (MeshLine line : terrainMesh.lines) {
                //blau ist vor road hintergrund schlecht sichtbar
                boolean broken = false;
                if (line.getLeft() != null && terrainMesh.getPolygon(line.getLeft()) == null) {
                    broken = true;
                }
                if (line.getRight() != null && terrainMesh.getPolygon(line.getRight()) == null) {
                    broken = true;
                }
                double width = Math.max(4, 4 / tileProjection.getScale());
                double len = Math.max(8, 8 / tileProjection.getScale());

                Coordinate[] coors = line.getCoordinates();
                for (int i = 0; i < coors.length - 1; i++) {
                    Point p0 = tileProjection.coordinateToPoint(coors[i]);
                    Point p1 = tileProjection.coordinateToPoint(coors[i + 1]);

                    if (i == coors.length - 2) {
                        //tile.drawArrow(coors[i], coors[i + 1], (broken) ? Color.red : Color.orange);
                        DrawHelper.drawArrow(g, p0, p1, (broken) ? Color.red : Color.orange, width, len);
                    } else {
                        //tile.drawLine(coors[i], coors[i + 1], (broken) ? Color.red : Color.orange);
                        DrawHelper.drawLine(g, p0, p1, (broken) ? Color.red : Color.orange);
                    }
                }
            }
        }
    }

    public void setTerrainMesh(TerrainMesh terrainMesh) {
        this.terrainMesh = terrainMesh;
    }

    @Override
    public String mouseReleased(TileProjection tileProjection, VectorXZ mappedClick) {
        Coordinate c = JtsUtil.toCoordinate(mappedClick);
        MeshLine line = terrainMesh.findClosestLine(c);
        if (line != null) {
            String s = "meshline: " + line + ". left=" + line.getLeft().parentInfo + ",right=" + line.getRight().parentInfo;
            return s;
        }

        return "line not found for coordinates " + mappedClick;
    }
}
