package com.petbuddy.social.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3PresignedUrlService {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${aws.s3.region:ap-south-1}")
    private String region;

    @Value("${aws.s3.endpoint:http://localhost:9000}")
    private String s3Endpoint;

    /**
     * Generates a presigned PUT URL for direct client upload to S3.
     * 
     * @param userId      User performing the upload
     * @param fileName    Original file name
     * @param contentType MIME type (e.g., "image/jpeg")
     * @return PresignedUrlResult with upload URL and media key
     */
    public PresignedUrlResult generatePresignedUploadUrl(String userId, String fileName, String contentType) {
        // Generate unique media key: users/{userId}/media/{uuid}/{filename}
        String mediaKey = String.format("users/%s/media/%s/%s",
                userId, UUID.randomUUID(), sanitizeFileName(fileName));

        log.info("Generating presigned URL for bucket: {}, key: {}", bucketName, mediaKey);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(mediaKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        String uploadUrl = presignedRequest.url().toString();
        log.info("Generated presigned URL (expires in {} min) for key: {}", expiryMinutes, mediaKey);

        return new PresignedUrlResult(uploadUrl, mediaKey, expiryMinutes * 60L);
    }

    /**
     * Builds the public S3 URL for a media key.
     * Uses endpoint from config to support both AWS S3 and MinIO.
     */
    public String getPublicUrl(String mediaKey) {
        // Construct URL: {endpoint}/{bucketName}/{mediaKey}
        // e.g., http://localhost:9000/petbuddy-media/users/userId/media/...
        String baseUrl = s3Endpoint.replaceAll("/$", ""); // Remove trailing slash if present
        return String.format("%s/%s/%s", baseUrl, bucketName, mediaKey);
    }

    private String sanitizeFileName(String fileName) {
        // Remove special characters, keep alphanumeric, dots, dashes, underscores
        return fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }

    public record PresignedUrlResult(String uploadUrl, String mediaKey, long expiresInSeconds) {
    }
}
