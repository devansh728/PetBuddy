package com.petbuddy.social.config;

import org.springframework.context.annotation.Configuration;

/**
 * gRPC configuration placeholder.
 * The actual keepAlive fix is handled by GrpcKeepAliveFixPostProcessor
 * which runs before Spring context creation.
 */
@Configuration
public class GrpcBugFixConfig {
    // All configuration handled via GrpcKeepAliveFixPostProcessor
    // EnvironmentPostProcessor
}
