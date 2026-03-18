package com.petbuddy.social.service;

import com.petbuddy.social.config.RabbitMQConfig;
import com.petbuddy.social.entity.PostEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    public void publishPostCreated(PostEntity post) {
        Map<String, Object> event = Map.of(
                "eventType", "post.created",
                "postId", post.getId().toString(),
                "userId", post.getUserId(),
                "mediaKey", post.getMediaKey(),
                "caption", post.getCaption() != null ? post.getCaption() : "",
                "mediaUrl", post.getMediaUrl() != null ? post.getMediaUrl() : "",
                "timestamp", System.currentTimeMillis());

        log.info("Publishing post.created event for postId: {}", post.getId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SOCIAL_EXCHANGE,
                RabbitMQConfig.POST_CREATED_ROUTING_KEY,
                event);
    }

    public void publishLikeCreated(String userId, String postId, String postOwnerId, String postCaption, String mediaUrl, String username) {
        Map<String, Object> event = Map.of(
                "eventType", "like.created",
                "userId", userId,
                "postId", postId,
                "postOwnerId", postOwnerId,
                "postCaption", postCaption != null ? postCaption : "",
                "mediaUrl", mediaUrl != null ? mediaUrl : "",
                "username", username,
                "timestamp", System.currentTimeMillis());

        log.info("Publishing like.created event for postId: {} by userId: {}", postId, userId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SOCIAL_EXCHANGE,
                RabbitMQConfig.LIKE_CREATED_ROUTING_KEY,
                event);
    }

    public void publishLikeDeleted(String userId, String postId, String postOwnerId, String postCaption, String mediaUrl, String username) {
        Map<String, Object> event = Map.of(
                "eventType", "like.deleted",
                "userId", userId,
                "postId", postId,
                "postOwnerId", postOwnerId,
                "postCaption", postCaption != null ? postCaption : "",
                "mediaUrl", mediaUrl != null ? mediaUrl : "",
                "username", username,
                "timestamp", System.currentTimeMillis());

        log.info("Publishing like.deleted event for postId: {} by userId: {}", postId, userId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SOCIAL_EXCHANGE,
                RabbitMQConfig.LIKE_DELETED_ROUTING_KEY,
                event);
    }
}
