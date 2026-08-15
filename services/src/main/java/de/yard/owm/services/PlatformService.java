package de.yard.owm.services;

import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@Slf4j
public class PlatformService {

    public PlatformService() {
        initPlatform();
    }

    /**
     * SimpleHeadlessPlatform requires module-java-common, but well, that seems the platform to go ("tools" no longer exists).
     */
    public static void initPlatform() {
        log.info("initPlatform");
        SimpleHeadlessPlatform.init(ConfigurationByEnv.buildDefaultConfigurationWithEnv(new HashMap<String, String>()));
    }
}
