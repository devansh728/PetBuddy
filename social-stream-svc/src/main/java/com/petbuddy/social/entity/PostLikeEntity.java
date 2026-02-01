package com.petbuddy.social.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_likes", schema = "social", uniqueConstraints = @UniqueConstraint(columnNames = { "userId",
        "postId" }), indexes = {
                @Index(name = "idx_likes_user_id", columnList = "userId"),
                @Index(name = "idx_likes_post_id", columnList = "postId")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String userId; // Firebase UID (not UUID)

    @Column(nullable = false)
    private UUID postId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
