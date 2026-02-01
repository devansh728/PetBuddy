package com.petbuddy.gateway.datafetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import com.petbuddy.gateway.context.AuthContext;
import com.petbuddy.gateway.filter.FirebaseAuthFilter;
import com.petbuddy.social.grpc.*;
import graphql.GraphQLException;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DgsComponent
@RequiredArgsConstructor
@Slf4j
public class SocialDataFetcher {

    private final SocialServiceGrpc.SocialServiceBlockingStub socialService;

    @DgsQuery
    public Map<String, Object> getUploadUrl(
            @InputArgument("fileName") String fileName,
            @InputArgument("contentType") String contentType) {

        AuthContext auth = getAuthContext();

        try {
            UploadRequest request = UploadRequest.newBuilder()
                    .setUserId(auth.getUid())
                    .setFileName(fileName)
                    .setContentType(contentType != null ? contentType : "application/octet-stream")
                    .build();

            log.info("Getting upload URL for user: {}", auth.getUid());
            UploadResponse response = socialService.getPresignedUploadUrl(request);

            return Map.of(
                    "uploadUrl", response.getUploadUrl(),
                    "mediaKey", response.getMediaKey(),
                    "expiresInSeconds", (int) response.getExpiresInSeconds());

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to get upload URL: " + e.getStatus().getDescription());
        }
    }

    @DgsMutation
    public Map<String, Object> createPost(@InputArgument("input") Map<String, Object> input) {
        AuthContext auth = getAuthContext();

        String caption = (String) input.getOrDefault("caption", "");
        String mediaKey = (String) input.get("mediaKey");
        Double locationLat = input.get("locationLat") != null ? ((Number) input.get("locationLat")).doubleValue() : 0.0;
        Double locationLon = input.get("locationLon") != null ? ((Number) input.get("locationLon")).doubleValue() : 0.0;

        if (mediaKey == null || mediaKey.isBlank()) {
            throw new GraphQLException("mediaKey is required");
        }

        try {
            // Note: We need the user's UUID from profile-service, but for simplicity
            // using Firebase UID as user reference (in production, lookup user ID first)
            CreatePostRequest request = CreatePostRequest.newBuilder()
                    .setUserId(auth.getUid()) // This should be the DB user UUID
                    .setCaption(caption)
                    .setMediaKey(mediaKey)
                    .setLocationLat(locationLat)
                    .setLocationLon(locationLon)
                    .build();

            log.info("Creating post for user: {}", auth.getUid());
            PostResponse response = socialService.createPost(request);

            return mapPostToMap(response);

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to create post: " + e.getStatus().getDescription());
        }
    }

    @DgsQuery
    public Map<String, Object> getFeed(
            @InputArgument("page") Integer page,
            @InputArgument("pageSize") Integer pageSize) {

        AuthContext auth = getAuthContext();

        try {
            FeedRequest request = FeedRequest.newBuilder()
                    .setUserId(auth.getUid())
                    .setPageNumber(page != null ? page : 0)
                    .setPageSize(pageSize != null ? pageSize : 20)
                    .build();

            log.info("Getting feed page {} for user: {}", page, auth.getUid());
            FeedResponse response = socialService.getFeed(request);

            List<Map<String, Object>> posts = response.getPostsList().stream()
                    .map(this::mapPostToMap)
                    .collect(Collectors.toList());

            return Map.of(
                    "posts", posts,
                    "totalPages", response.getTotalPages(),
                    "hasMore", response.getHasMore());

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to get feed: " + e.getStatus().getDescription());
        }
    }

    @DgsMutation
    public Map<String, Object> toggleLike(@InputArgument("postId") String postId) {
        AuthContext auth = getAuthContext();

        try {
            LikeRequest request = LikeRequest.newBuilder()
                    .setUserId(auth.getUid())
                    .setPostId(postId)
                    .build();

            log.info("Toggling like on post {} for user: {}", postId, auth.getUid());
            LikeResponse response = socialService.toggleLike(request);

            return Map.of(
                    "isLiked", response.getIsLiked(),
                    "likeCount", response.getLikeCount());

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to toggle like: " + e.getStatus().getDescription());
        }
    }

    @DgsQuery
    public Map<String, Object> getPost(@InputArgument("postId") String postId) {
        AuthContext auth = getAuthContext();

        try {
            GetPostRequest request = GetPostRequest.newBuilder()
                    .setPostId(postId)
                    .setRequestingUserId(auth.getUid())
                    .build();

            PostResponse response = socialService.getPost(request);
            return mapPostToMap(response);

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to get post: " + e.getStatus().getDescription());
        }
    }

    private Map<String, Object> mapPostToMap(PostResponse post) {
        return Map.of(
                "postId", post.getPostId(),
                "userId", post.getUserId(),
                "caption", post.getCaption(),
                "mediaUrl", post.getMediaUrl(),
                "locationLat", post.getLocationLat(),
                "locationLon", post.getLocationLon(),
                "likeCount", post.getLikeCount(),
                "commentCount", post.getCommentCount(),
                "isLiked", post.getIsLiked(),
                "createdAt", post.getCreatedAt());
    }

    private AuthContext getAuthContext() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new GraphQLException("No request context available");
        }

        HttpServletRequest request = attrs.getRequest();
        AuthContext auth = (AuthContext) request.getAttribute(FirebaseAuthFilter.AUTH_CONTEXT_ATTRIBUTE);

        if (auth == null) {
            throw new GraphQLException("Authentication required");
        }

        return auth;
    }
}
