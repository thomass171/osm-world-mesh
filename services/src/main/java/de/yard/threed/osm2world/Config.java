package de.yard.threed.osm2world;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Singleton for holding the current configuration and make it available globally.
 * 9.4.19: Es gibt hier jetzt keine Defaultconfig mehr. Die muss von aussen rein kommen.
 *
 * <p>
 * Created on 30.05.18.
 */
public class Config {
    Logger logger = Logger.getLogger(Config.class.getName());
    private CompositeConfiguration compositeConfiguration;
    private Configuration defaultconfig = null;
    private static Config instance = null;

    public static String MATERIAL_FLIGHT = "flight";
    public static String MATERIAL_MODEL = "model";

    //9.4.19 das ist doof String defaultconfigfile = "config/configuration-default.properties";
    // private TargetBounds targetBounds = null;
    // 17.7.19: Lieber nicht loggen was nicht gefunden wurde, sondern das, was gefunden wurde und verwendet wird.
    private Config(String configfile) {
        Reader reader = null;
        String source = "";
        if (configfile == null) {
            throw new IllegalArgumentException("no config file define");
        }
        try {
            reader = new FileReader(configfile);
            source = configfile;
            logger.info("loaded configuration from filesystem file" + configfile);
        } catch (FileNotFoundException e) {

            //logger.warn("file not found: " + configfile + ". Trying classpath");
        }

        if (reader == null) {
            //9.4.19  InputStream inputstream = Thread.currentThread().getContextClassLoader().getResourceAsStream(defaultconfigfile);
            InputStream inputstream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configfile);
            reader = new InputStreamReader(inputstream);
            source = configfile;
            logger.info("loaded configuration from classpath file " + configfile);
        }
        defaultconfig = loadConfig(reader, source);
        merge(null, null,null);
    }

    public static Config getInstance() {
        /*if (instance == null) {
            instance = new Config(null);
        }*/
        return instance;
    }

    public static Configuration getCurrentConfiguration() {
        return getInstance().compositeConfiguration;
    }

    public static void init(String defaultconfigfile) {
        instance = new Config(defaultconfigfile);
    }

    /**
     * keeps only default config and merges new userconfig
     * 9.4.19: default config kommt jetzt hier mit rein.
     * 11.11.21: Und auch eine material config
     */
    public static void reinit(String defaultconfigfile,Configuration materialconfig, Configuration lodconfig, Configuration customconfig) {
        //instance = null;
        init(defaultconfigfile);
        getInstance().merge(materialconfig, lodconfig, customconfig);

    }

    /*9.4.19 public static void reset() {
        instance = new Config(null);
    }*/

    /**
     *
     * @param userconfig set of parameters that controls various aspects
     *                   of the modules' behavior; null to use defaults only
     */
    private void merge(Configuration materialconfig,Configuration userconfig, Configuration customconfig) {
        compositeConfiguration = new CompositeConfiguration();
        if (customconfig != null) {
            compositeConfiguration.addConfiguration(customconfig);
        }
        if (userconfig != null) {
            compositeConfiguration.addConfiguration(userconfig);
        }
        if (materialconfig != null) {
            compositeConfiguration.addConfiguration(materialconfig);
        }
        compositeConfiguration.addConfiguration(defaultconfig);
    }

    private Configuration loadConfig(Reader inputstream, String source) {
        Configuration config = new BaseConfiguration();

        try {
            PropertiesConfiguration fileConfig = new PropertiesConfiguration();
            fileConfig.read((inputstream));
            //TODO really needed? fileConfig.setListDelimiter(';');
            config = fileConfig;
        } catch (Exception e) {
            logger.error("could not read config from " + source + ", ignoring it: " + e.getMessage());
        }
        return config;
    }

    /*9.4.19 public void setTargetBounds(TargetBounds targetBounds){
        this.targetBounds = targetBounds;
    }
    
    public TargetBounds getTargetBounds() {
        return targetBounds;
    }*/

    public String[] getModules() {
        Map<String, String> modules = new HashMap<>();
        loopModules((String property, String modulename) -> {
            modules.put(modulename, "");
        });
        return modules.keySet().toArray(new String[0]);
    }

    /**
     * Disables all modules not in list.
     */
    public void enableModules(String[] modulenames) {
        loopModules((String property, String modulename) -> {
            Boolean b;
            if (Arrays.asList(modulenames).contains(modulename)) {
                b = Boolean.TRUE;
            } else {
                b = Boolean.FALSE;
            }
            compositeConfiguration.setProperty(property + ".enabled", b);
        });
    }

    private void loopModules(ModuleHandler moduleHandler) {
        Iterator<String> keys = compositeConfiguration.getKeys("modules");
        while (keys.hasNext()) {
            String key = keys.next();
            String[] parts = key.split("\\.");
            moduleHandler.handleModule("modules." + parts[1], parts[1]);
        }

    }
}

@FunctionalInterface
interface ModuleHandler {
    void handleModule(String property, String modulename);
}


