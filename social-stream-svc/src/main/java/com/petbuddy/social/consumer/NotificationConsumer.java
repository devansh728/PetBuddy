package com.petbuddy.social.consumer;

// import com.fasterxml.jackson.databind.ObjectMapper;
import com.petbuddy.social.config.RabbitMQConfig;
import com.petbuddy.social.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;
    // private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.POST_CREATED_QUEUE)
    public void handlePostCreated(Map<String, Object> event) {
        try {
            String postId = (String) event.get("postId");
            String userId = (String) event.get("userId");
            String caption = (String) event.get("caption");
            String mediaUrl = (String) event.get("mediaUrl");

            log.info("Processing post.created event for postId: {}", postId);

            notificationService.createNotification(
                    userId,
                    userId,
                    "You",
                    "POST_CREATED",
                    postId,
                    caption,
                    mediaUrl);

        } catch (Exception e) {
            log.error("Error handling post.created event", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.LIKE_CREATED_QUEUE)
    public void handleLikeCreated(Map<String, Object> event) {
        try {
            String userId = (String) event.get("userId");
            String postId = (String) event.get("postId");
            String postOwnerId = (String) event.get("postOwnerId");
            String username = (String) event.get("username");
            String postCaption = (String) event.get("postCaption");
            String mediaUrl = (String) event.get("mediaUrl");

            log.info("Processing like.created event for postId: {} by user: {}", postId, userId);

            if (!userId.equals(postOwnerId)) {
                notificationService.createNotification(
                        postOwnerId,
                        userId,
                        username,
                        "POST_LIKED",
                        postId,
                        postCaption,
                        mediaUrl);
            }

        } catch (Exception e) {
            log.error("Error handling like.created event", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.LIKE_DELETED_QUEUE)
    public void handleLikeDeleted(Map<String, Object> event) {
        try {
            String userId = (String) event.get("userId");
            String postId = (String) event.get("postId");
            String postOwnerId = (String) event.get("postOwnerId");
            String username = (String) event.get("username");
            String postCaption = (String) event.get("postCaption");
            String mediaUrl = (String) event.get("mediaUrl");

            log.info("Processing like.deleted event for postId: {} by user: {}", postId, userId);

            if (!userId.equals(postOwnerId)) {
                notificationService.createNotification(
                        postOwnerId,
                        userId,
                        username,
                        "POST_UNLIKED",
                        postId,
                        postCaption,
                        mediaUrl);
            }

        } catch (Exception e) {
            log.error("Error handling like.deleted event", e);
        }
    }
}
