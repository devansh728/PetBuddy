package com.petbuddy.social.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 Configuration supporting both AWS S3 and S3-compatible services like
 * Storj.
 * 
 * For Storj, set:
 * - aws.s3.endpoint=https://gateway.storjshare.io
 * - aws.s3.region=us-1 (or your Storj region)
 * - aws.access-key=your-storj-access-key
 * - aws.secret-key=your-storj-secret-key
 * - aws.s3.path-style-access=true
 */
@Configuration
public class S3Config {

    @Value("${aws.s3.region:ap-south-1}")
    private String region;

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    // Custom endpoint for S3-compatible services (Storj, MinIO, etc.)
    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    // Use path-style access (required for most S3-compatible services)
    @Value("${aws.s3.path-style-access:false}")
    private boolean pathStyleAccess;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (accessKey != null && !accessKey.isEmpty()
                && secretKey != null && !secretKey.isEmpty()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey));
        }
        // Fall back to default credentials chain (env vars, ~/.aws, EC2 role, etc.)
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider);

        // Configure for S3-compatible services like Storj
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(pathStyleAccess)
                    .build());
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsCredentialsProvider credentialsProvider) {
        var builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider);

        // Configure for S3-compatible services like Storj
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(pathStyleAccess)
                    .build());
        }

        return builder.build();
    }
}
