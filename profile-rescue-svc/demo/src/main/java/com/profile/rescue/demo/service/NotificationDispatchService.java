package com.profile.rescue.demo.service;

import com.profile.rescue.demo.config.RabbitMQConfig;
import com.profile.rescue.demo.entity.IncidentEntity;
import com.profile.rescue.demo.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Dispatch rescue alerts to a list of volunteers.
     * Publishes individual notification events to RabbitMQ for async processing.
     */
    public int dispatchVolunteerAlerts(IncidentEntity incident, List<UserEntity> volunteers) {
        int dispatched = 0;

        for (UserEntity volunteer : volunteers) {
            try {
                Map<String, Object> alertEvent = createVolunteerAlert(incident, volunteer);

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.RESCUE_EXCHANGE,
                        RabbitMQConfig.VOLUNTEER_ALERT_ROUTING_KEY,
                        alertEvent);

                log.info("Dispatched alert to volunteer {} for incident {}",
                        volunteer.getId(), incident.getId());
                dispatched++;

            } catch (Exception e) {
                log.error("Failed to dispatch alert to volunteer {}: {}",
                        volunteer.getId(), e.getMessage());
            }
        }

        log.info("Dispatched {} alerts for incident {}", dispatched, incident.getId());
        return dispatched;
    }

    /**
     * Create a notification event for a volunteer.
     */
    private Map<String, Object> createVolunteerAlert(IncidentEntity incident, UserEntity volunteer) {
        Map<String, Object> event = new HashMap<>();

        // Event metadata
        event.put("eventType", "volunteer.alert");
        event.put("timestamp", System.currentTimeMillis());

        // Volunteer info
        event.put("volunteerId", volunteer.getId().toString());
        event.put("volunteerEmail", volunteer.getEmail());
        event.put("volunteerFcmToken", volunteer.getFcmToken());

        // Incident info
        event.put("incidentId", incident.getId().toString());
        event.put("incidentType", incident.getType().name());
        event.put("incidentDescription", incident.getDescription());
        event.put("incidentAnimalType", incident.getAnimalType());
        event.put("incidentLatitude", incident.getLocation().getY());
        event.put("incidentLongitude", incident.getLocation().getX());
        event.put("incidentImageUrl", incident.getImageUrl());

        // Calculate approximate distance
        if (volunteer.getLocation() != null) {
            double distance = calculateDistance(
                    incident.getLocation().getY(), incident.getLocation().getX(),
                    volunteer.getLocation().getY(), volunteer.getLocation().getX());
            event.put("distanceMeters", Math.round(distance));
        }

        return event;
    }

    /**
     * Publish incident status update event.
     */
    public void publishIncidentUpdate(IncidentEntity incident, String updateType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "incident." + updateType);
        event.put("timestamp", System.currentTimeMillis());
        event.put("incidentId", incident.getId().toString());
        event.put("status", incident.getStatus().name());
        event.put("volunteersNotified", incident.getVolunteersNotified());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RESCUE_EXCHANGE,
                RabbitMQConfig.INCIDENT_UPDATE_ROUTING_KEY,
                event);

        log.info("Published incident {} update: {}", incident.getId(), updateType);
    }

    /**
     * Haversine formula to calculate distance between two points in meters.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Earth's radius in meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
