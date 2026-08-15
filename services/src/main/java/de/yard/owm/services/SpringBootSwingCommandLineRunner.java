package de.yard.owm.services;

import de.yard.threed.osm2graph.viewer.Viewer2D;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.awt.*;

/**
 * This CommandLineRunner fires off at runtime and boots up our GUI.
 * From https://github.com/mightychip/spring-boot-swing/blob/master/src/main/java/ca/purpleowl/examples/swing/SpringBootSwingCommandLineRunner.java
 */
@Component
public class SpringBootSwingCommandLineRunner implements CommandLineRunner {
    private static Viewer2D viewer2D;

    // not yet injected??
    //@Value("${viewer2d.enabled}")
    //boolean viewer2dEnabled;

    @Autowired
    public SpringBootSwingCommandLineRunner() {
        String viewer2dEnabled = System.getProperty("viewer2d.enabled");
        // instantiation needs to be here (this thread?). In run() doesn't work.
        if (viewer2dEnabled != null && viewer2dEnabled.equals("true") && viewer2D == null) {
            Viewer2D.mainEntry(false);
            viewer2D = Viewer2D.getInstance();
        }
    }


    @Override
    public void run(String... args) {
        //This boots up the GUI.
        EventQueue.invokeLater(() -> {
            if (viewer2D != null) {
                viewer2D.setEnabled(true);
            }
        });
    }
}