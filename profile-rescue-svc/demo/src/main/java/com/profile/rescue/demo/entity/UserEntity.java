package com.profile.rescue.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_firebase_uid", columnList = "firebaseUid"),
        @Index(name = "idx_users_is_volunteer", columnList = "isVolunteer")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String firebaseUid;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String fcmToken;

    // Volunteer status for rescue dispatch
    @Column(nullable = false)
    @Builder.Default
    private Boolean isVolunteer = false;

    // Geospatial location (PostGIS Point with SRID 4326 = WGS84)
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column
    private Instant locationUpdatedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PetEntity> pets;
}
