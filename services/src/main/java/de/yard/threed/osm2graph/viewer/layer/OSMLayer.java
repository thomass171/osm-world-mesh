package de.yard.threed.osm2graph.viewer.layer;

import com.vividsolutions.jts.geom.Coordinate;
import de.yard.threed.core.Degree;
import de.yard.threed.core.LatLon;
import de.yard.threed.core.Vector2;
import de.yard.threed.osm2graph.osm.JtsUtil;
import de.yard.threed.osm2graph.osm.OsmUtil;
import de.yard.threed.osm2graph.osm.SceneryProjection;
import de.yard.threed.osm2graph.osm.TileProjection;
import de.yard.threed.osm2graph.viewer.DrawHelper;
import de.yard.threed.osm2graph.viewer.Viewer2D;
import de.yard.threed.osm2scenery.util.TagHelper;
import de.yard.threed.osm2world.MapBasedTagGroup;
import de.yard.threed.osm2world.OSMData;
import de.yard.threed.osm2world.OSMDataReader;
import de.yard.threed.osm2world.OSMFileReader;
import de.yard.threed.osm2world.OSMNode;
import de.yard.threed.osm2world.OSMWay;
import de.yard.threed.osm2world.VectorXZ;
import de.yard.threed.scenery.util.OsmWriter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Erlaubt erstmal nur hinzufügen neuer Nodes in einem Way.
 */
public class OSMLayer extends Layer {
    private OSMData osmData;
    SceneryProjection projection;
    //nicht so vorhalten. Positionen koennen sich aendern
    // Map<VectorXZ, OSMNode> myNodes ;
    JPopupMenu popupMenu = new JPopupMenu();
    //Point popupLocation;
    VectorXZ mappedpopupLocation;
    Component component;
    int linePosition;
    JMenuItem addNodeToWayMi, saveMi, addWayMi;
    OSMWay popupWay;
    int nodeRadius = 7;
    File fileUsed;

    public OSMLayer() {
        addNodeToWayMi = new JMenuItem("Add node to way");
        addNodeToWayMi.addActionListener(l -> {
            addNodeToWayAtPopupLocation();
        });
        popupMenu.add(addNodeToWayMi);

        addWayMi = new JMenuItem("Add way...");
        addWayMi.addActionListener(l -> {
            AddWayDialog dialog = new AddWayDialog(Viewer2D.getInstance(), "aa", this);
            dialog.setSize(600, 400);
            dialog.setVisible(true);
        });
        popupMenu.add(addWayMi);

        saveMi = new JMenuItem("Save...");
        saveMi.addActionListener(l -> {
            Viewer2D.logger.debug("Saving file " + fileUsed.getAbsolutePath()+ "to clipboard.");
            //Schreibt bewusst nicht in eine Datei um versehentliches Ueberschreiben zu vermeiden.
            //Jetzt ins Clipboard
            //PrintStream pw = System.out;
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                PrintStream ps = new PrintStream(baos, true, "UTF-8");

                //Bounds fuer "Wayland"
                String bounds = "<bounds minlat=\"50.982\" minlon=\"6.973\" maxlat=\"51\" maxlon=\"7\"/>";

                OsmWriter.write(osmData, ps, bounds);
                String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                StringSelection stringSelection = new StringSelection(data);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        });
        popupMenu.add(saveMi);
    }

    private void addNodeToWayAtPopupLocation() {
        if (mappedpopupLocation == null || linePosition == -1 || popupWay == null) {
            Viewer2D.logger.error("no pop location");
            return;
        }
        long id = OsmUtil.findHighestId(osmData);
        id++;
        OSMNode newNode = createNode(mappedpopupLocation, id);
        if (newNode != null) {
            popupWay.addNode(linePosition + 1, newNode);
        }
        component.repaint();
    }

    private OSMNode createNode(VectorXZ loc, long proposedId) {
        if (OsmUtil.finfWayIndexById(new ArrayList<>(osmData.getWays()), proposedId) != -1 ||
                OsmUtil.finfNodeIndexById(new ArrayList<>(osmData.getNodes()), proposedId) != -1) {
            Viewer2D.logger.error("Id already in use:" + proposedId);
            return null;
        }
        LatLon latLon = projection.unproject(loc);
        MapBasedTagGroup tags = TagHelper.buildTagGroup();
        Viewer2D.logger.debug("Creating new node with id " + proposedId);
        OSMNode newNode = new OSMNode(latLon.getLatDeg().getDegree(), latLon.getLonDeg().getDegree(), tags, proposedId);
        osmData.getNodes().add(newNode);
        return newNode;
    }

    /**
     * Das ist ja auch fuer den Repaint!
     *
     * @param g
     * @param tileProjection
     */
    public void draw(Graphics2D g, TileProjection tileProjection) {
        if (osmData != null) {
            //blau ist vor road hintergrund schlecht sichtbar
            Color color = Color.magenta;
            //Die Nodes vorerst nur als Waybestandteil zeichnen bis es wirklich gebraucht wird
            /*for (OSMNode node : osmData.getNodes()) {
                DrawHelper.drawCircle(g, tileProjection.vectorxzToPoint(getLoc(node)), 10, Color.GRAY);
            }*/
            for (OSMWay way : osmData.getWays()) {
                for (int i = 0; i < way.getNodes().size() - 1; i++) {
                    VectorXZ from = getLoc(way.getNodes().get(i));
                    VectorXZ to = getLoc(way.getNodes().get(i + 1));
                    Point p0 = tileProjection.vectorxzToPoint(from);
                    Point p1 = tileProjection.vectorxzToPoint(to);

                    DrawHelper.drawLine(g, p0, p1, color);
                    DrawHelper.drawCircle(g, p0, nodeRadius, color);
                    if (i == way.getNodes().size() - 2) {
                        DrawHelper.drawCircle(g, p1, nodeRadius, color);
                    }
                }
            }
        }
    }

    VectorXZ getLoc(OSMNode node) {
        VectorXZ loc = projection.project(LatLon.fromDegrees(node.lat, node.lon));
        return loc;
    }

    /**
     * Paint geht indirekt ueber repaint().
     * Die verarbeiteten OSM Daten sind evtl. veraendert, z.B. durch Coastlines oder closedWay/"P" Split.
     * Darum die Daten hier neu einlesen, weil die ja unverfaelscht wieder gespeichert werden können sollen.
     */
    public void setOSMData(/*OSMData osmData,*/ SceneryProjection projection, File fileUsed) {
        OSMDataReader dataReader = null;
        osmData = null;
        try {
            dataReader = new OSMFileReader(fileUsed);
            osmData = dataReader.getData();
        } catch (Exception e) {
            Viewer2D.logger.error("Fail to read " + fileUsed.getAbsolutePath());
        }
        this.projection = projection;
        this.fileUsed = fileUsed;
    }

    OSMNode nodeDragged;

    @Override
    public String mousePressed(TileProjection tileProjection, VectorXZ mappedClick) {
        Viewer2D.logger.debug("pressed in OSM layer at " + mappedClick);

        nodeDragged = null;
        OSMNode node = getNodeAtLocation(tileProjection, mappedClick);
        if (node != null) {
            Viewer2D.logger.debug("pressed node " + node);
            nodeDragged = node;
            return "OSM node " + node.id;
        }
        return "";
    }

    @Override
    public String mouseReleased(TileProjection tileProjection, VectorXZ mappedClick) {
        Viewer2D.logger.debug("released in OSM layer at " + mappedClick);

        nodeDragged = null;
        OSMNode node = getNodeAtLocation(tileProjection, mappedClick);
        if (node != null) {
            Viewer2D.logger.debug("released node " + node);
            return "OSM node " + node.id;
        }
        return "";
    }

    private OSMNode getNodeAtLocation(TileProjection tileProjection, VectorXZ mappedClick) {
        for (OSMNode node : osmData.getNodes()) {
            VectorXZ loc = getLoc(node);
            //Distance besser ueber Pixel, sonst ist das zu scale abhängig
            Point locp = tileProjection.vectorxzToPoint(loc);
            if (locp.distance(tileProjection.vectorxzToPoint(mappedClick)) < 10) {
                return node;
                //tile.drawCircle(loc, 10, Color.RED);
            }
        }
        return null;
    }

    /**
     * der zieht beim Draggen vorerst mal eine Spur hinter sich her. Wohl weil der paintComponent() keine Layer neu zeichnet.
     * Nicht hier drawen, spaeter kommt der repaint.
     */
    @Override
    public void mouseDragged(VectorXZ mappedstartpoint, VectorXZ mappedendpoint) {
        //Viewer2D.logger.debug("dragged in OSM layer to " + mappedendpoint);
        //tile.drawCircle(mappedendpoint, 10, Color.RED);
        //DrawHelper.drawCircle(g,tileProjection.vectorxzToPoint(mappedendpoint), 10, Color.RED);
        if (nodeDragged != null) {
            LatLon latlon = projection.unproject(mappedendpoint);
            //Viewer2D.logger.debug("dragged node to " + mappedendpoint);

            nodeDragged.setLoc(latlon);
        }
        //nodeDragged=null;
    }

    @Override
    public void openPopup(Component component, Point p, VectorXZ mappedClick) {
        mappedpopupLocation = mappedClick;
        this.component = component;
        Coordinate c = JtsUtil.toCoordinate(mappedClick);
        Object[] res = OsmUtil.findClosestLine(mappedClick, new ArrayList<>(osmData.getWays()), projection);
        popupWay = (OSMWay) res[0];
        linePosition = (int) res[1];
        Viewer2D.logger.debug("linePosition=" + linePosition);
        if (linePosition == -1) {
            addNodeToWayMi.setEnabled(false);
        } else {
            addNodeToWayMi.setEnabled(true);
        }
        popupMenu.show(component, p.x, p.y);
    }

    /**
     * Bei closed wird das ein Krei um mappedpopupLocation
     */
    public void addWay(char type, int nodecount, boolean isClosed, double size) {
        Viewer2D.logger.debug("addWay: type=" + type + ",nodecount=" + nodecount + ",closed=" + isClosed + ",size=" + size);

        MapBasedTagGroup wayTags;
        switch (type) {
            case 'H':
                wayTags = TagHelper.buildTagGroup("highway", "primary");
                break;
            case 'R':
                wayTags = TagHelper.buildTagGroup("railway", "rail");
                break;
            default:
                Viewer2D.logger.error("invalid type " + type);
                return;
        }

        long id = OsmUtil.findHighestId(osmData) + 1;

        List<OSMNode> nodes = new ArrayList<>();
        double stepDegree = 360 / nodecount;

        for (int i = 0; i < nodecount; i++) {
            VectorXZ loc;
            if (isClosed) {
                Vector2 v = new Vector2(0, 1);
                v = v.rotate(new Degree(i * stepDegree));
                v = v.multiply(size);
                loc = mappedpopupLocation.add(OsmUtil.toVectorXZ(v));
            } else {
                //north every size meter
                Vector2 v = new Vector2(0, 1);
                v = v.multiply(i*size);
                loc = mappedpopupLocation.add(OsmUtil.toVectorXZ(v));
            }
            OSMNode newNode = createNode(loc, id);
            nodes.add(newNode);
            id++;
        }
        if (isClosed) {
            nodes.add(nodes.get(0));
        }
        long wayId = OsmUtil.findHighestId(osmData) + 1;
        Viewer2D.logger.debug("Creating new way with id " + wayId);

        OSMWay newWay = new OSMWay(wayTags, wayId, nodes);
        osmData.getWays().add(newWay);
        component.repaint();
    }
}

class AddWayDialog extends JDialog implements ActionListener {

    JPanel mainpanel;
    JRadioButton btn_highway, btn_railway;
    JCheckBox cb_closed;
    JTextField tf_nodecount, tf_size;
    JButton btn_cancel, btn_ok;
    ButtonGroup typegroup;
    int nodecount;
    String type;
    double size;
    OSMLayer parent;

    /**
     * Creates the reusable dialog.
     */
    public AddWayDialog(Frame aFrame, String aWord, OSMLayer parent) {
        super(aFrame, true);
        this.parent = parent;

        setTitle("Add Way");

        //textField = new JTextField(10);

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add("Center", createMainPanel());
        p.add("South", createButtonPanel());
        setContentPane(p);

        //Handle window closing correctly.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {

            }
        });


    }

    /**
     * This method handles events for the text field.
     */
    public void actionPerformed(ActionEvent e) {
        //    optionPane.setValue(btnString1);
    }


    /**
     * This method clears the dialog and hides it.
     */
    public void clearAndHide() {
        setVisible(false);
    }

    private JPanel createMainPanel() {
        mainpanel = new JPanel();
        mainpanel.setLayout(new GridBagLayout());
        //setBorder(new TitledBorder("Engine " + nr + s));
        int row = 0;
        //mainpanel.add(new JLabel("Starting Id"), new GridBagConstraints(0, row, 1, 1, 1.0, 1.0
        //        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        //add(tf_name, new GridBagConstraints(1, row, 1, 1, 1.0, 1.0
        //        , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        //row++;
        mainpanel.add(createTypePanel(), new GridBagConstraints(0, row, 2, 1, 1.0, 5.0
                , GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        row++;
        cb_closed = new JCheckBox("Closed");
        mainpanel.add(cb_closed, new GridBagConstraints(0, row, 2, 1, 1.0, 5.0
                , GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        row++;
        mainpanel.add(new JLabel("Anzahl Nodes"), new GridBagConstraints(0, row, 1, 1, 1.0, 1.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        tf_nodecount = createTextField();
        mainpanel.add(tf_nodecount, new GridBagConstraints(1, row, 1, 1, 1.0, 1.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        row++;
        mainpanel.add(new JLabel("Radius"), new GridBagConstraints(0, row, 1, 1, 1.0, 1.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        tf_size = createTextField();
        tf_size.setToolTipText("Typischer Radius ist z.B. 16 in B55B477");
        mainpanel.add(tf_size, new GridBagConstraints(1, row, 1, 1, 1.0, 1.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        row++;
        return mainpanel;
    }

    private JPanel createTypePanel() {
        btn_highway = new JRadioButton("Highway");
        btn_highway.setSelected(false);
        btn_highway.setActionCommand("H");
        btn_railway = new JRadioButton("Railway");
        btn_railway.setSelected(false);
        btn_railway.setActionCommand("R");

        typegroup = new ButtonGroup();
        typegroup.add(btn_highway);
        typegroup.add(btn_railway);

        JPanel radioPanel = new JPanel(new GridLayout(1, 0));
        radioPanel.add(btn_highway);
        radioPanel.add(btn_railway);
        return radioPanel;
    }

    private void validateInput() {
        btn_ok.setEnabled(false);
        try {
            int i = Integer.parseInt(tf_nodecount.getText());
            if (i < 2) {
                return;
            }
            nodecount = i;
            double d = Double.parseDouble(tf_size.getText());
            if (d < 0.0001) {
                return;
            }
            size = d;
        } catch (NumberFormatException e) {
            return;
        }
        if (typegroup.getSelection() == null) {
            return;
        }
        type = typegroup.getSelection().getActionCommand();
        Viewer2D.logger.debug("type=" + type + ",nodecount=" + nodecount + ",size=" + size);
        btn_ok.setEnabled(true);
    }

    private JPanel createButtonPanel() {
        btn_cancel = new JButton("Cancel");
        btn_cancel.setEnabled(true);
        btn_cancel.addActionListener(l -> {
            setVisible(false);
        });
        btn_ok = new JButton("OK");
        btn_ok.addActionListener(l -> {
            parent.addWay(type.charAt(0), nodecount, cb_closed.isSelected(), size);
            setVisible(false);
        });

        btn_ok.setEnabled(false);

        JPanel p = new JPanel(new GridLayout(1, 0));
        p.add(btn_cancel);
        p.add(btn_ok);
        return p;
    }

    private JTextField createTextField() {
        JTextField tf = new JTextField();
        tf.getDocument().addDocumentListener(new DocumentListener() {
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
                validateInput();
            }
        });
        return tf;
    }


}