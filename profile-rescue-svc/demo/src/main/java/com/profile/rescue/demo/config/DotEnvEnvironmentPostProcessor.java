package com.profile.rescue.demo.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * EnvironmentPostProcessor to load .env file variables into Spring Environment.
 * For Spring Boot 4.x, uses org.springframework.boot.EnvironmentPostProcessor
 * (the old org.springframework.boot.env.EnvironmentPostProcessor is
 * deprecated).
 * 
 * Register in META-INF/spring.factories:
 * org.springframework.boot.EnvironmentPostProcessor=com.profile.rescue.demo.config.DotEnvEnvironmentPostProcessor
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String ENV_FILE = ".env";
    private static final String PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();

        // Try to load .env from project root (parent of demo folder)
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

        try (InputStream inputStream = resource.getInputStream()) {
            Properties properties = new Properties();

            // Read line by line to handle .env format (KEY=VALUE)
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(inputStream));

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

                    // Convert to Spring property format (SNAKE_CASE to kebab-case)
                    // e.g., SUPABASE_PASSWORD becomes supabase.password
                    String springKey = convertToSpringPropertyKey(key);
                    envProperties.put(springKey, value);

                    // Also keep original key for ${ENV_VAR} references
                    envProperties.put(key, value);
                }
            }
        }

        return envProperties;
    }

    /**
     * Convert environment variable name to Spring property format.
     * SUPABASE_PASSWORD -> supabase.password
     */
    private String convertToSpringPropertyKey(String envKey) {
        return envKey.toLowerCase().replace('_', '.');
    }
}
