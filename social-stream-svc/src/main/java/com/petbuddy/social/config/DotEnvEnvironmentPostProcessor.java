package com.petbuddy.social.config;

import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * EnvironmentPostProcessor to load .env file variables into Spring Environment.
 * For Spring Boot 3.x, uses
 * org.springframework.boot.env.EnvironmentPostProcessor
 * 
 * Register in META-INF/spring.factories:
 * org.springframework.boot.env.EnvironmentPostProcessor=com.petbuddy.social.config.DotEnvEnvironmentPostProcessor
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();

        // Try to load .env from project root
        String[] locations = {
                "file:../.env",
                "file:.env",
                "classpath:.env"
        };

        for (String location : locations) {
            Resource resource = resourceLoader.getResource(location);
            if (resource.exists()) {
                try {
                    Map<String, Object> envProperties = loadEnvFile(resource);
                    if (!envProperties.isEmpty()) {
                        environment.getPropertySources().addLast(
                                new MapPropertySource(PROPERTY_SOURCE_NAME, envProperties));
                        return;
                    }
                } catch (IOException e) {
                    // Ignore and try next location
                }
            }
        }
    }

    private Map<String, Object> loadEnvFile(Resource resource) throws IOException {
        Map<String, Object> envProperties = new HashMap<>();

        try (InputStream inputStream = resource.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse KEY=VALUE
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();

                    // Remove surrounding quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    // Convert to Spring property format
                    String springKey = key.toLowerCase().replace('_', '.');
                    envProperties.put(springKey, value);

                    // Also keep original key
                    envProperties.put(key, value);
                }
            }
        }

        return envProperties;
    }
}
