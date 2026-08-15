package de.yard.owm.services;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Needs to reside below 'de.yard.owm.services' to be found by component scanner
 */
@Configuration
public class TestConfig {

    /**
     * Cleanup DB once before tests (application context?) start
     */
    @Bean
    public FlywayMigrationStrategy clean() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}

