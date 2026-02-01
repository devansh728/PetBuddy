package com.profile.rescue.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange for rescue operations
    public static final String RESCUE_EXCHANGE = "rescue.exchange";

    // Queue and routing key for volunteer alerts
    public static final String VOLUNTEER_ALERT_QUEUE = "rescue.alert.volunteer";
    public static final String VOLUNTEER_ALERT_ROUTING_KEY = "alert.volunteer";

    // Queue and routing key for incident updates
    public static final String INCIDENT_UPDATE_QUEUE = "rescue.incident.update";
    public static final String INCIDENT_UPDATE_ROUTING_KEY = "incident.update";

    @Bean
    public TopicExchange rescueExchange() {
        return new TopicExchange(RESCUE_EXCHANGE);
    }

    @Bean
    public Queue volunteerAlertQueue() {
        return QueueBuilder.durable(VOLUNTEER_ALERT_QUEUE).build();
    }

    @Bean
    public Queue incidentUpdateQueue() {
        return QueueBuilder.durable(INCIDENT_UPDATE_QUEUE).build();
    }

    @Bean
    public Binding volunteerAlertBinding(Queue volunteerAlertQueue, TopicExchange rescueExchange) {
        return BindingBuilder
                .bind(volunteerAlertQueue)
                .to(rescueExchange)
                .with(VOLUNTEER_ALERT_ROUTING_KEY);
    }

    @Bean
    public Binding incidentUpdateBinding(Queue incidentUpdateQueue, TopicExchange rescueExchange) {
        return BindingBuilder
                .bind(incidentUpdateQueue)
                .to(rescueExchange)
                .with(INCIDENT_UPDATE_ROUTING_KEY);
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
