package com.petbuddy.social.repository;

import com.petbuddy.social.entity.PostLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLikeEntity, UUID> {

    Optional<PostLikeEntity> findByUserIdAndPostId(String userId, UUID postId);

    boolean existsByUserIdAndPostId(String userId, UUID postId);

    void deleteByUserIdAndPostId(String userId, UUID postId);

    // Check if user has liked multiple posts (for feed optimization)
    List<PostLikeEntity> findByUserIdAndPostIdIn(String userId, List<UUID> postIds);
}
