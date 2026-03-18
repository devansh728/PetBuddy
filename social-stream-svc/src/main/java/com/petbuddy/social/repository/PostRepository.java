package com.petbuddy.social.repository;

import com.petbuddy.social.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, UUID> {

    // Feed query - ordered by createdAt descending
    Page<PostEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // User's posts (userId is now String for Firebase UID)
    List<PostEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<PostEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Increment like count
    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLikeCount(@Param("postId") UUID postId);

    // Decrement like count
    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :postId")
    void decrementLikeCount(@Param("postId") UUID postId);

    // Increment comment count
    @Modifying
    @Query("UPDATE PostEntity p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") UUID postId);
}
