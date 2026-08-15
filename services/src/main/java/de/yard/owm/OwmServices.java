package de.yard.owm;

import de.yard.owm.services.maze.MazeValidator;
import de.yard.threed.core.InitMethod;
import de.yard.threed.core.configuration.ConfigurationByProperties;
import de.yard.threed.core.platform.Platform;
import de.yard.threed.core.platform.PlatformFactory;
import de.yard.threed.javacommon.ConfigurationByEnv;
import de.yard.threed.javacommon.SimpleHeadlessPlatform;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.Validator;

import java.util.HashMap;

@SpringBootApplication
public class OwmServices implements RepositoryRestConfigurer {
    /*public static void main(String[] args) {
        SpringApplication.run(Services.class, args);
    }*/

    public static void main(String[] args) {

        new SpringApplicationBuilder(OwmServices.class)
                .headless(false)
                // not sure SERVLET is correct
                .web(WebApplicationType.SERVLET)
                .run(args);
    }

    @Bean
    public Validator validator() {
        return new MazeValidator();
    }

    /**
     * auto registration not working?
     */
    @Override
    public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener v) {
        v.addValidator("beforeSave", validator());
    }
}
