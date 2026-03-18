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
@Table(name = "posts", schema = "social", indexes = {
        @Index(name = "idx_posts_user_id", columnList = "userId"),
        @Index(name = "idx_posts_created_at", columnList = "createdAt DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String userId;

    @Column(length = 255)
    private String authorUsername;

    @Column(length = 2000)
    private String caption;

    @Column(nullable = false)
    private String mediaKey; // S3 object key

    @Column
    private String mediaUrl; // Full S3 URL

    @Column
    private Double locationLat;

    @Column
    private Double locationLon;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
