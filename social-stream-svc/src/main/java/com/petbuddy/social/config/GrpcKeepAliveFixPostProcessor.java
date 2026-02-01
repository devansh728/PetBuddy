package com.petbuddy.social.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * EnvironmentPostProcessor to inject default gRPC keepAlive properties.
 * Works around Spring gRPC 1.0.1 NPE bug where null duration values
 * in keepAlive configuration cause NullPointerException.
 * 
 * Runs BEFORE application context is created, ensuring properties
 * are available when gRPC autoconfigure binds them.
 */
@Configuration
public class GrpcKeepAliveFixPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "grpcKeepAliveDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Only add if not already present
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        Map<String, Object> props = new HashMap<>();

        // Set all keepAlive properties to safe defaults to prevent NPE
        // These will be overridden by any user-defined properties
        props.put("spring.grpc.server.keep-alive.time", "2h");
        props.put("spring.grpc.server.keep-alive.timeout", "20s");
        props.put("spring.grpc.server.keep-alive.permit-time", "5m");
        props.put("spring.grpc.server.keep-alive.permit-without-calls", "false");
        props.put("spring.grpc.server.keep-alive.max-connection-idle", "0s");
        props.put("spring.grpc.server.keep-alive.max-connection-age", "0s");
        props.put("spring.grpc.server.keep-alive.max-connection-age-grace", "0s");

        // Add at LAST (lowest priority) so user properties can override
        environment.getPropertySources().addLast(
                new MapPropertySource(PROPERTY_SOURCE_NAME, props));

        System.out.println("[GrpcKeepAliveFix] Injected default keepAlive properties via EnvironmentPostProcessor");
    }
}
