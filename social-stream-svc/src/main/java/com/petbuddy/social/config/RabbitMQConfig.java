package com.petbuddy.social.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SOCIAL_EXCHANGE = "social.exchange";
    public static final String POST_CREATED_QUEUE = "social.post.created";
    public static final String POST_CREATED_ROUTING_KEY = "post.created";

    public static final String LIKE_CREATED_QUEUE = "social.like.created";
    public static final String LIKE_CREATED_ROUTING_KEY = "like.created";

    public static final String LIKE_DELETED_QUEUE = "social.like.deleted";
    public static final String LIKE_DELETED_ROUTING_KEY = "like.deleted";

    public static final String NOTIFICATION_QUEUE = "social.notification";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.*";

    @Bean
    public TopicExchange socialExchange() {
        return new TopicExchange(SOCIAL_EXCHANGE);
    }

    @Bean
    public Queue postCreatedQueue() {
        return QueueBuilder.durable(POST_CREATED_QUEUE).build();
    }

    @Bean
    public Queue likeCreatedQueue() {
        return QueueBuilder.durable(LIKE_CREATED_QUEUE).build();
    }

    @Bean
    public Queue likeDeletedQueue() {
        return QueueBuilder.durable(LIKE_DELETED_QUEUE).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding postCreatedBinding(Queue postCreatedQueue, TopicExchange socialExchange) {
        return BindingBuilder
                .bind(postCreatedQueue)
                .to(socialExchange)
                .with(POST_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding likeCreatedBinding(Queue likeCreatedQueue, TopicExchange socialExchange) {
        return BindingBuilder
                .bind(likeCreatedQueue)
                .to(socialExchange)
                .with(LIKE_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding likeDeletedBinding(Queue likeDeletedQueue, TopicExchange socialExchange) {
        return BindingBuilder
                .bind(likeDeletedQueue)
                .to(socialExchange)
                .with(LIKE_DELETED_ROUTING_KEY);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange socialExchange) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(socialExchange)
                .with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
