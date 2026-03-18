package com.petbuddy.social.repository;

import com.petbuddy.social.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId, Pageable pageable);

    List<NotificationEntity> findByRecipientUserIdAndIsReadFalse(String recipientUserId);

    long countByRecipientUserIdAndIsReadFalse(String recipientUserId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.id = :notificationId")
    void markAsRead(@Param("notificationId") UUID notificationId);

    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.recipientUserId = :recipientUserId AND n.createdAt < :before")
    void deleteOldNotifications(@Param("recipientUserId") String recipientUserId, @Param("before") Instant before);
}
