package com.petbuddy.social.service;

import com.petbuddy.social.entity.NotificationEntity;
import com.petbuddy.social.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public static final int DEFAULT_PAGE_SIZE = 20;

    @Transactional
    public NotificationEntity createNotification(String recipientUserId, String actorUserId, String actorUsername,
                                                  String notificationType, String postId, String postCaption, String mediaUrl) {
        NotificationEntity notification = NotificationEntity.builder()
                .recipientUserId(recipientUserId)
                .actorUserId(actorUserId)
                .actorUsername(actorUsername)
                .notificationType(notificationType)
                .postId(postId)
                .postCaption(postCaption)
                .mediaUrl(mediaUrl)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Created notification for user: {} from actor: {}", recipientUserId, actorUserId);
        return notification;
    }

    public Page<NotificationEntity> getNotifications(String userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE);
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.markAsRead(notificationId);
        log.info("Marked notification as read: {}", notificationId);
    }

    @Transactional
    public void deleteOldNotifications(String userId, int daysOld) {
        Instant threshold = Instant.now().minus(daysOld, ChronoUnit.DAYS);
        notificationRepository.deleteOldNotifications(userId, threshold);
        log.info("Deleted notifications older than {} days for user: {}", daysOld, userId);
    }
}
