package com.petbuddy.social.controller;

import com.petbuddy.social.entity.PostEntity;
import com.petbuddy.social.grpc.*;
import com.petbuddy.social.service.FeedService;
import com.petbuddy.social.service.S3PresignedUrlService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class SocialGrpcService extends SocialServiceGrpc.SocialServiceImplBase {

    private final S3PresignedUrlService s3Service;
    private final FeedService feedService;
    private final com.petbuddy.social.service.NotificationService notificationService;

    @Override
    public void getPresignedUploadUrl(UploadRequest request, StreamObserver<UploadResponse> responseObserver) {
        try {
            log.info("Generating presigned URL for user: {}, file: {}",
                    request.getUserId(), request.getFileName());

            if (request.getFileName().isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("file_name is required")
                        .asRuntimeException());
                return;
            }

            S3PresignedUrlService.PresignedUrlResult result = s3Service.generatePresignedUploadUrl(
                    request.getUserId(),
                    request.getFileName(),
                    request.getContentType().isEmpty() ? "application/octet-stream" : request.getContentType());

            UploadResponse response = UploadResponse.newBuilder()
                    .setUploadUrl(result.uploadUrl())
                    .setMediaKey(result.mediaKey())
                    .setExpiresInSeconds(result.expiresInSeconds())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error generating presigned URL", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to generate upload URL: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void createPost(CreatePostRequest request, StreamObserver<PostResponse> responseObserver) {
        try {
            log.info("Creating post for user: {}", request.getUserId());

            String userId = request.getUserId();
            if (userId == null || userId.isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("user_id is required")
                        .asRuntimeException());
                return;
            }

            if (request.getMediaKey().isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("media_key is required")
                        .asRuntimeException());
                return;
            }

            PostEntity post = feedService.createPost(
                    userId,
                    request.getCaption(),
                    request.getMediaKey(),
                    request.getLocationLat() != 0 ? request.getLocationLat() : null,
                    request.getLocationLon() != 0 ? request.getLocationLon() : null);

            responseObserver.onNext(mapToPostResponse(post, false));
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error creating post", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to create post: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getFeed(FeedRequest request, StreamObserver<FeedResponse> responseObserver) {
        try {
            log.info("Getting feed page {} for user: {}", request.getPageNumber(), request.getUserId());

            String requestingUserId = request.getUserId();

            FeedService.FeedResult feedResult = feedService.getGlobalFeed(
                    request.getPageNumber(),
                    request.getPageSize(),
                    requestingUserId);

            FeedResponse.Builder responseBuilder = FeedResponse.newBuilder()
                    .setTotalPages(feedResult.totalPages())
                    .setHasMore(feedResult.hasMore());

            for (PostEntity post : feedResult.posts()) {
                boolean isLiked = feedResult.likedPostIds().contains(post.getId());
                responseBuilder.addPosts(mapToPostResponse(post, isLiked));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting feed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get feed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserPosts(GetUserPostsRequest request, StreamObserver<FeedResponse> responseObserver) {
        try {
            log.info("Getting posts for user: {}", request.getUserId());

            FeedService.FeedResult feedResult = feedService.getUserPosts(
                    request.getUserId(),
                    request.getPageNumber(),
                    request.getPageSize(),
                    request.getRequestingUserId());

            FeedResponse.Builder responseBuilder = FeedResponse.newBuilder()
                    .setTotalPages(feedResult.totalPages())
                    .setHasMore(feedResult.hasMore());

            for (PostEntity post : feedResult.posts()) {
                boolean isLiked = feedResult.likedPostIds().contains(post.getId());
                responseBuilder.addPosts(mapToPostResponse(post, isLiked));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting user posts", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get user posts: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void toggleLike(LikeRequest request, StreamObserver<LikeResponse> responseObserver) {
        try {
            log.info("Toggle like: user {} on post {}", request.getUserId(), request.getPostId());

            String userId = request.getUserId();
            if (userId == null || userId.isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("user_id is required")
                        .asRuntimeException());
                return;
            }

            UUID postId;
            try {
                postId = UUID.fromString(request.getPostId());
            } catch (IllegalArgumentException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid post_id format")
                        .asRuntimeException());
                return;
            }

            FeedService.LikeResult result = feedService.toggleLike(userId, postId);

            LikeResponse response = LikeResponse.newBuilder()
                    .setIsLiked(result.isLiked())
                    .setLikeCount(result.likeCount())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error toggling like", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to toggle like: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getPost(GetPostRequest request, StreamObserver<PostResponse> responseObserver) {
        try {
            UUID postId;
            try {
                postId = UUID.fromString(request.getPostId());
            } catch (IllegalArgumentException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid post_id format")
                        .asRuntimeException());
                return;
            }

            Optional<PostEntity> postOpt = feedService.getPost(postId);
            if (postOpt.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Post not found: " + request.getPostId())
                        .asRuntimeException());
                return;
            }

            boolean isLiked = false;
            if (!request.getRequestingUserId().isEmpty()) {
                isLiked = feedService.isPostLikedByUser(request.getRequestingUserId(), postId);
            }

            responseObserver.onNext(mapToPostResponse(postOpt.get(), isLiked));
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting post", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get post: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getNotifications(GetNotificationsRequest request, StreamObserver<NotificationsResponse> responseObserver) {
        try {
            log.info("Getting notifications for user: {}", request.getUserId());

            org.springframework.data.domain.Page<com.petbuddy.social.entity.NotificationEntity> page = 
                notificationService.getNotifications(request.getUserId(), request.getPageNumber(), request.getPageSize());

            NotificationsResponse.Builder responseBuilder = NotificationsResponse.newBuilder();

            for (com.petbuddy.social.entity.NotificationEntity notification : page.getContent()) {
                NotificationMessage message = NotificationMessage.newBuilder()
                        .setNotificationId(notification.getId().toString())
                        .setActorUsername(notification.getActorUsername() != null ? notification.getActorUsername() : "")
                        .setNotificationType(notification.getNotificationType() != null ? notification.getNotificationType() : "")
                        .setPostId(notification.getPostId() != null ? notification.getPostId() : "")
                        .setPostCaption(notification.getPostCaption() != null ? notification.getPostCaption() : "")
                        .setMediaUrl(notification.getMediaUrl() != null ? notification.getMediaUrl() : "")
                        .setIsRead(notification.getIsRead())
                        .setCreatedAt(notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : "")
                        .build();
                responseBuilder.addNotifications(message);
            }

            long unreadCount = notificationService.getUnreadCount(request.getUserId());
            responseBuilder.setTotalPages(page.getTotalPages());
            responseBuilder.setHasMore(page.hasNext());
            responseBuilder.setUnreadCount((int) unreadCount);

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting notifications", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get notifications: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void markNotificationAsRead(MarkNotificationReadRequest request, StreamObserver<EmptyResponse> responseObserver) {
        try {
            log.info("Marking notification as read: {}", request.getNotificationId());

            try {
                UUID notificationId = UUID.fromString(request.getNotificationId());
                notificationService.markAsRead(notificationId);
                responseObserver.onNext(EmptyResponse.newBuilder().build());
                responseObserver.onCompleted();
            } catch (IllegalArgumentException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid notification_id format")
                        .asRuntimeException());
            }

        } catch (Exception e) {
            log.error("Error marking notification as read", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to mark notification as read: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUnreadNotificationCount(EmptyRequest request, StreamObserver<UnreadCountResponse> responseObserver) {
        try {
            log.info("Getting unread notification count for user: {}", request.getUserId());

            long count = notificationService.getUnreadCount(request.getUserId());

            UnreadCountResponse response = UnreadCountResponse.newBuilder()
                    .setCount((int) count)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting unread notification count", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get unread notification count: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private PostResponse mapToPostResponse(PostEntity post, boolean isLiked) {
        PostResponse.Builder builder = PostResponse.newBuilder()
                .setPostId(post.getId().toString())
                .setUserId(post.getUserId())
                .setLikeCount(post.getLikeCount())
                .setCommentCount(post.getCommentCount())
                .setIsLiked(isLiked);

        if (post.getAuthorUsername() != null)
            builder.setAuthorUsername(post.getAuthorUsername());
        if (post.getCaption() != null)
            builder.setCaption(post.getCaption());
        if (post.getMediaUrl() != null)
            builder.setMediaUrl(post.getMediaUrl());
        if (post.getLocationLat() != null)
            builder.setLocationLat(post.getLocationLat());
        if (post.getLocationLon() != null)
            builder.setLocationLon(post.getLocationLon());
        if (post.getCreatedAt() != null)
            builder.setCreatedAt(post.getCreatedAt().toString());

        return builder.build();
    }
}
