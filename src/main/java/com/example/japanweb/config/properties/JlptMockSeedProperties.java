package com.example.japanweb.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jlpt.mock-seed")
public class JlptMockSeedProperties {

    /**
     * Load mock_tests/*.json into the database on application startup when needed.
     */
    private boolean enabled = true;

    /**
     * Classpath location of mock exam JSON files (e.g. classpath:mock_tests/).
     */
    private String location = "classpath:mock_tests/";
}
