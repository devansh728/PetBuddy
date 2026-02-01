package com.profile.rescue.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents", indexes = {
        @Index(name = "idx_incidents_status", columnList = "status"),
        @Index(name = "idx_incidents_reporter", columnList = "reporterId"),
        @Index(name = "idx_incidents_created", columnList = "createdAt DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID reporterId;

    // Geospatial location (PostGIS Point with SRID 4326 = WGS84)
    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(length = 2000)
    private String description;

    @Column
    private String animalType;

    @Column
    private String imageUrl;

    // Number of volunteers notified for this incident
    @Column(nullable = false)
    @Builder.Default
    private Integer volunteersNotified = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // Enum definitions
    public enum IncidentType {
        UNKNOWN,
        ACCIDENT,
        ABANDONED,
        INJURED,
        LOST,
        ABUSE
    }

    public enum IncidentStatus {
        OPEN,
        ASSIGNED,
        IN_PROGRESS,
        RESOLVED,
        CANCELLED
    }
}
