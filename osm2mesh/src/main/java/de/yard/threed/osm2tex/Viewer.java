package de.yard.threed.osm2tex;


import javax.swing.*;
import java.awt.*;

/**
 * Liegt jetzt erstmal in osm2world.
 * Created by thomass on 30.01.17.
 */
@Deprecated
public class Viewer extends JFrame {
    MainPanel mainPanel;

    
    Viewer() {

        setSize(1024, 768);
        setLocation(300, 30);
        mainPanel = new MainPanel();
        getContentPane().add(mainPanel);
        setVisible(true);
    }

    public static void main(String[] arg) {
        new Viewer();
    }
}


class MainPanel extends JPanel {
    protected final NodePanel nodepanel;
    DrawPanel drawPanel;

    public MainPanel() {
        setLayout(new BorderLayout());

        nodepanel = new NodePanel();
        JSplitPane matpanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        drawPanel = new DrawPanel(TileBuilder.buildSample());
        matpanel.add(nodepanel);
        matpanel.add(drawPanel);
        matpanel.setResizeWeight(0.7f);

        add("Center", matpanel);

        matpanel.setDividerSize(5);
        //setEnabled(false);
        //setResizeWeight(0.26);
        //setTopComponent(screen);

        //setToInitControls();
    }
}

class NodePanel extends JPanel {
    JList list = new JList(new DefaultListModel());
    
    NodePanel() {
        //top = new SceneNodeTreeNode();
        JScrollPane treeView = new JScrollPane(new JLabel("kkk"));
        setLayout(new BorderLayout());
        add(treeView, BorderLayout.CENTER);


    }
}


class DrawPanel extends JPanel {
    JLabel l = new JLabel();
    Graphics2D g2d ;
    
    DrawPanel(Tile tile) {
        // set a preferred size for the custom panel.
        setPreferredSize(new Dimension(420, 420));

    
    l.setIcon(new ImageIcon(tile.getImage()));
    l.setText("");
        setLayout(new BorderLayout());
        add("Center",l);
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
    
   
}
