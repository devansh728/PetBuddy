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
@Table(name = "notifications", schema = "social", indexes = {
        @Index(name = "idx_notifications_recipient_created", columnList = "recipientUserId, createdAt DESC"),
        @Index(name = "idx_notifications_recipient_unread", columnList = "recipientUserId, isRead")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String recipientUserId;

    @Column(nullable = false, length = 128)
    private String actorUserId;

    @Column(length = 255)
    private String actorUsername;

    @Column(length = 50)
    private String notificationType;

    @Column(length = 36)
    private String postId;

    @Column(length = 2000)
    private String postCaption;

    @Column(length = 500)
    private String mediaUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
