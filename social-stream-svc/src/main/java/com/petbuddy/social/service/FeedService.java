package com.petbuddy.social.service;

import com.petbuddy.social.entity.PostEntity;
import com.petbuddy.social.entity.PostLikeEntity;
import com.petbuddy.social.repository.PostLikeRepository;
import com.petbuddy.social.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final S3PresignedUrlService s3Service;
    private final EventPublisherService eventPublisher;

    public static final int DEFAULT_PAGE_SIZE = 20;

    @Transactional
    @CacheEvict(value = "feed", allEntries = true)
    public PostEntity createPost(String userId, String caption, String mediaKey,
            Double locationLat, Double locationLon) {
        String mediaUrl = s3Service.getPublicUrl(mediaKey);

        PostEntity post = PostEntity.builder()
                .userId(userId)
                .caption(caption)
                .mediaKey(mediaKey)
                .mediaUrl(mediaUrl)
                .locationLat(locationLat)
                .locationLon(locationLon)
                .authorUsername("User")
                .build();

        post = postRepository.save(post);
        log.info("Created post: {} for user: {}", post.getId(), userId);

        eventPublisher.publishPostCreated(post);

        return post;
    }

    @Cacheable(value = "feed", key = "'global:' + #page")
    public FeedResult getGlobalFeed(int page, int pageSize, String requestingUserId) {
        log.info("Fetching feed page {} (cache miss)", page);

        Pageable pageable = PageRequest.of(page, pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE);
        Page<PostEntity> postsPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<PostEntity> posts = postsPage.getContent();

        Set<UUID> likedPostIds = getLikedPostIds(requestingUserId, posts);

        return new FeedResult(
                posts,
                likedPostIds,
                postsPage.getTotalPages(),
                postsPage.hasNext());
    }

    public FeedResult getUserPosts(String userId, int page, int pageSize, String requestingUserId) {
        log.info("Fetching posts for user: {} page {}", userId, page);

        Pageable pageable = PageRequest.of(page, pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE);
        Page<PostEntity> postsPage = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<PostEntity> posts = postsPage.getContent();

        Set<UUID> likedPostIds = getLikedPostIds(requestingUserId, posts);

        return new FeedResult(
                posts,
                likedPostIds,
                postsPage.getTotalPages(),
                postsPage.hasNext());
    }

    @Transactional
    public LikeResult toggleLike(String userId, UUID postId) {
        Optional<PostEntity> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }

        PostEntity post = postOpt.get();
        Optional<PostLikeEntity> existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            postRepository.decrementLikeCount(postId);
            log.info("User {} unliked post {}", userId, postId);

            eventPublisher.publishLikeDeleted(
                    userId,
                    postId.toString(),
                    post.getUserId(),
                    post.getCaption(),
                    post.getMediaUrl(),
                    "User");

            return new LikeResult(false, Math.max(0, post.getLikeCount() - 1));
        } else {
            PostLikeEntity like = PostLikeEntity.builder()
                    .userId(userId)
                    .postId(postId)
                    .build();
            postLikeRepository.save(like);
            postRepository.incrementLikeCount(postId);
            log.info("User {} liked post {}", userId, postId);

            eventPublisher.publishLikeCreated(
                    userId,
                    postId.toString(),
                    post.getUserId(),
                    post.getCaption(),
                    post.getMediaUrl(),
                    "User");

            return new LikeResult(true, post.getLikeCount() + 1);
        }
    }

    /**
     * Get a single post by ID.
     */
    public Optional<PostEntity> getPost(UUID postId) {
        return postRepository.findById(postId);
    }

    /**
     * Check if user has liked a post.
     */
    public boolean isPostLikedByUser(String userId, UUID postId) {
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }

    private Set<UUID> getLikedPostIds(String userId, List<PostEntity> posts) {
        if (userId == null || userId.isEmpty() || posts.isEmpty()) {
            return Collections.emptySet();
        }

        List<UUID> postIds = posts.stream()
                .map(PostEntity::getId)
                .collect(Collectors.toList());

        return postLikeRepository.findByUserIdAndPostIdIn(userId, postIds)
                .stream()
                .map(PostLikeEntity::getPostId)
                .collect(Collectors.toSet());
    }

    public record FeedResult(
            List<PostEntity> posts,
            Set<UUID> likedPostIds,
            int totalPages,
            boolean hasMore) {
    }

    public record LikeResult(boolean isLiked, int likeCount) {
    }
}
