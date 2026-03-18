package com.profile.rescue.demo.controller;

import com.profile.rescue.demo.entity.IncidentEntity;
import com.profile.rescue.demo.entity.PetEntity;
import com.profile.rescue.demo.entity.UserEntity;
import com.profile.rescue.demo.repository.IncidentRepository;
import com.profile.rescue.demo.repository.PetRepository;
import com.profile.rescue.demo.repository.UserRepository;
import com.profile.rescue.demo.service.GeometryService;
import com.profile.rescue.demo.service.NotificationDispatchService;
import com.profile.rescue.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified gRPC Service for Profile and Rescue Module operations.
 * Handles user/pet management, location updates, incident reporting, and
 * volunteer dispatch.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class RescueGrpcService extends ProfileServiceGrpc.ProfileServiceImplBase {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final IncidentRepository incidentRepository;
    private final GeometryService geometryService;
    private final NotificationDispatchService notificationService;

    // Default search radius: 5 kilometers
    private static final double DEFAULT_RADIUS_METERS = 5000.0;

    // ==================== PROFILE OPERATIONS ====================

    @Override
    @Transactional
    public void createUserProfile(CreateUserRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        try {
            log.info("Creating user profile for firebaseUid: {}", request.getFirebaseUid());

            Optional<UserEntity> existingUser = userRepository.findByFirebaseUid(request.getFirebaseUid());
            if (existingUser.isPresent()) {
                log.info("User already exists, returning existing profile");
                responseObserver.onNext(mapToUserProfileResponse(existingUser.get()));
                responseObserver.onCompleted();
                return;
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                responseObserver.onError(Status.ALREADY_EXISTS
                        .withDescription("Email already registered: " + request.getEmail())
                        .asRuntimeException());
                return;
            }

            UserEntity user = UserEntity.builder()
                    .firebaseUid(request.getFirebaseUid())
                    .email(request.getEmail())
                    .fullName(request.getFullName())
                    .fcmToken(request.getFcmToken().isEmpty() ? null : request.getFcmToken())
                    .isVolunteer(false)
                    .build();

            user = userRepository.save(user);
            log.info("User created: {}", user.getId());

            responseObserver.onNext(mapToUserProfileResponse(user));
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error creating user profile", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to create user profile: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getUserProfile(GetUserRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        try {
            Optional<UserEntity> userOpt = userRepository.findByFirebaseUid(request.getFirebaseUid());
            if (userOpt.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("User not found")
                        .asRuntimeException());
                return;
            }

            responseObserver.onNext(mapToUserProfileResponse(userOpt.get()));
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting user profile", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get user profile")
                    .asRuntimeException());
        }
    }

    // ==================== VOLUNTEER UPDATE ===========================

    @Override
    @Transactional
    public void updateVolunteerStatus(UpdateVolunteerStatusRequest request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Updating volunteer status for user: {}", request.getUserId());

            UUID userId;
            try {
                userId = UUID.fromString(request.getUserId());
            } catch (IllegalArgumentException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid user_id format")
                        .asRuntimeException());
                return;
            }

            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("User not found")
                        .asRuntimeException());
                return;
            }

            UserEntity user = userOpt.get();
            user.setIsVolunteer(request.getIsVolunteer());
            userRepository.save(user);

            log.info("Volunteer status updated for user: {}", userId);

            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error updating volunteer status", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update volunteer status: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ==================== RESCUE MODULE OPERATIONS ====================

    /**
     * Update user's location for volunteer tracking.
     * 
     * Senior-Level Resume Enhancements (Optional for the future):
If an interviewer asks, "What if the user moves 10km while the app is open?"

I tell them:

"For Version 1, location is synced upon app launch to save mobile battery and network I/O. However, the architecture 
is designed to support gRPC Client Streaming. By upgrading the updateUserLocation RPC to rpc StreamUserLocation (stream LocationRequest) 
returns (Empty), the React Native app could use Location.watchPositionAsync to continuously stream coordinates
through the GraphQL Subscription/WebSocket Gateway directly to the PostGIS database, enabling real-time Uber-like 
volunteer tracking."
     */
    @Override
    @Transactional
    public void updateUserLocation(LocationRequest request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Updating location for user: {}", request.getUserId());

            UUID userId;
            try {
                userId = UUID.fromString(request.getUserId());
            } catch (IllegalArgumentException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid user_id format")
                        .asRuntimeException());
                return;
            }

            if (!geometryService.isValidCoordinates(request.getLatitude(), request.getLongitude())) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid coordinates")
                        .asRuntimeException());
                return;
            }

            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("User not found")
                        .asRuntimeException());
                return;
            }

            UserEntity user = userOpt.get();
            Point location = geometryService.createPoint(request.getLatitude(), request.getLongitude());

            user.setLocation(location);
            user.setLocationUpdatedAt(Instant.now());
            user.setIsVolunteer(request.getIsVolunteer());

            userRepository.save(user);
            log.info("Location updated for user: {}, volunteer: {}", userId, request.getIsVolunteer());

            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error updating user location", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update location: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * Report a rescue incident and dispatch alerts to nearby volunteers.
     */
    @Override
    @Transactional
    public void reportIncident(IncidentRequest request, StreamObserver<IncidentResponse> responseObserver) {
        try {
            log.info("Reporting incident from user: {}, type: {}",
                    request.getReporterId(), request.getType());

            UUID reporterId;
            try {
                reporterId = UUID.fromString(request.getReporterId());
            } catch (IllegalArgumentException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid reporter_id format")
                        .asRuntimeException());
                return;
            }

            if (!geometryService.isValidCoordinates(request.getLatitude(), request.getLongitude())) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid incident coordinates")
                        .asRuntimeException());
                return;
            }

            // Create incident location point
            Point incidentLocation = geometryService.createPoint(
                    request.getLatitude(),
                    request.getLongitude());

            // Save incident to database
            IncidentEntity incident = IncidentEntity.builder()
                    .reporterId(reporterId)
                    .location(incidentLocation)
                    .type(mapIncidentType(request.getType()))
                    .status(IncidentEntity.IncidentStatus.OPEN)
                    .description(request.getDescription())
                    .animalType(request.getAnimalType())
                    .imageUrl(request.getImageUrl().isEmpty() ? null : request.getImageUrl())
                    .build();

            incident = incidentRepository.save(incident);
            log.info("Incident saved: {}", incident.getId());

            // Find nearby volunteers within 5km radius
            List<UserEntity> nearbyVolunteers = userRepository.findVolunteersWithinRadius(
                    incidentLocation,
                    DEFAULT_RADIUS_METERS);

            log.info("Found {} volunteers within {}m of incident",
                    nearbyVolunteers.size(), DEFAULT_RADIUS_METERS);

            // Dispatch alerts to volunteers via RabbitMQ
            int notified = notificationService.dispatchVolunteerAlerts(incident, nearbyVolunteers);

            // Update incident with notification count
            incident.setVolunteersNotified(notified);
            incident = incidentRepository.save(incident);

            // Publish incident created event
            notificationService.publishIncidentUpdate(incident, "created");

            responseObserver.onNext(mapToIncidentResponse(incident));
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error reporting incident", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to report incident: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * Get nearby incidents within a radius.
     */
    @Override
    @Transactional(readOnly = true)
    public void getNearbyIncidents(NearbyRequest request, StreamObserver<IncidentListResponse> responseObserver) {
        try {
            log.info("Getting nearby incidents at ({}, {}), radius: {}m",
                    request.getLatitude(), request.getLongitude(), request.getRadiusMeters());

            if (!geometryService.isValidCoordinates(request.getLatitude(), request.getLongitude())) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid coordinates")
                        .asRuntimeException());
                return;
            }

            Point location = geometryService.createPoint(request.getLatitude(), request.getLongitude());
            double radius = request.getRadiusMeters() > 0 ? request.getRadiusMeters() : DEFAULT_RADIUS_METERS;

            List<IncidentEntity> incidents = incidentRepository.findOpenIncidentsWithinRadius(location, radius);

            IncidentListResponse.Builder responseBuilder = IncidentListResponse.newBuilder();
            for (IncidentEntity incident : incidents) {
                responseBuilder.addIncidents(mapToIncidentResponse(incident));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting nearby incidents", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get nearby incidents")
                    .asRuntimeException());
        }
    }

    // ==================== PET OPERATIONS ====================

    @Override
    @Transactional
    public void addPet(AddPetRequest request, StreamObserver<PetListResponse> responseObserver) {
        try {
            log.info("Adding pet for userId: {}", request.getUserId());

            UUID userId;
            try {
                userId = UUID.fromString(request.getUserId());
            } catch (IllegalArgumentException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid userId format: " + request.getUserId())
                        .asRuntimeException());
                return;
            }

            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("User not found with id: " + request.getUserId())
                        .asRuntimeException());
                return;
            }
            UserEntity owner = userOpt.get();

            List<PetEntity> petsToSave = request.getPetsList().stream().map(petData -> PetEntity.builder()
                    .name(petData.getName())
                    .breed(petData.getBreed().isEmpty() ? null : petData.getBreed())
                    .ageMonths(petData.getAgeMonths() > 0 ? petData.getAgeMonths() : null)
                    .imageUrl(petData.getImageUrl().isEmpty() ? null : petData.getImageUrl())
                    .owner(owner)
                    .build()).toList();

            List<PetEntity> savedPets = petRepository.saveAll(petsToSave);
            log.info("Successfully batch saved {} pets", savedPets.size());

            PetListResponse.Builder responseBuilder = PetListResponse.newBuilder();
            for (PetEntity pet : savedPets) {
                responseBuilder.addPets(mapToPetResponse(pet));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error adding pet", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to add pet: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getPetsByUser(GetPetsRequest request, StreamObserver<PetListResponse> responseObserver) {
        try {
            log.info("Getting pets for userId: {}", request.getUserId());

            UUID userId;
            try {
                userId = UUID.fromString(request.getUserId());
            } catch (IllegalArgumentException e) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid userId format: " + request.getUserId())
                        .asRuntimeException());
                return;
            }

            if (!userRepository.existsById(userId)) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("User not found with id: " + request.getUserId())
                        .asRuntimeException());
                return;
            }

            List<PetEntity> pets = petRepository.findByOwnerId(userId);

            PetListResponse.Builder responseBuilder = PetListResponse.newBuilder();
            for (PetEntity pet : pets) {
                responseBuilder.addPets(mapToPetResponse(pet));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting pets", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get pets: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ==================== MAPPERS ====================

    private UserProfileResponse mapToUserProfileResponse(UserEntity user) {
        UserProfileResponse.Builder builder = UserProfileResponse.newBuilder()
                .setUserId(user.getId().toString())
                .setFullName(user.getFullName())
                .setEmail(user.getEmail())
                .setIsVerified(true)
                .setIsVolunteer(user.getIsVolunteer() != null && user.getIsVolunteer());

        if (user.getCreatedAt() != null) {
            builder.setCreatedAt(user.getCreatedAt().toString());
        }
        if (user.getLocation() != null) {
            builder.setLatitude(geometryService.getLatitude(user.getLocation()));
            builder.setLongitude(geometryService.getLongitude(user.getLocation()));
        }

        return builder.build();
    }

    private PetResponse mapToPetResponse(PetEntity pet) {
        PetResponse.Builder builder = PetResponse.newBuilder()
                .setPetId(pet.getId().toString())
                .setName(pet.getName());

        if (pet.getBreed() != null)
            builder.setBreed(pet.getBreed());
        if (pet.getAgeMonths() != null)
            builder.setAgeMonths(pet.getAgeMonths());
        if (pet.getImageUrl() != null)
            builder.setImageUrl(pet.getImageUrl());
        if (pet.getCreatedAt() != null)
            builder.setCreatedAt(pet.getCreatedAt().toString());

        return builder.build();
    }

    private IncidentResponse mapToIncidentResponse(IncidentEntity incident) {
        IncidentResponse.Builder builder = IncidentResponse.newBuilder()
                .setIncidentId(incident.getId().toString())
                .setReporterId(incident.getReporterId().toString())
                .setLatitude(geometryService.getLatitude(incident.getLocation()))
                .setLongitude(geometryService.getLongitude(incident.getLocation()))
                .setType(mapToProtoIncidentType(incident.getType()))
                .setStatus(mapToProtoIncidentStatus(incident.getStatus()))
                .setVolunteersNotified(incident.getVolunteersNotified() != null ? incident.getVolunteersNotified() : 0);

        if (incident.getDescription() != null)
            builder.setDescription(incident.getDescription());
        if (incident.getAnimalType() != null)
            builder.setAnimalType(incident.getAnimalType());
        if (incident.getImageUrl() != null)
            builder.setImageUrl(incident.getImageUrl());
        if (incident.getCreatedAt() != null)
            builder.setCreatedAt(incident.getCreatedAt().toString());

        return builder.build();
    }

    private IncidentEntity.IncidentType mapIncidentType(IncidentType protoType) {
        return switch (protoType) {
            case ACCIDENT -> IncidentEntity.IncidentType.ACCIDENT;
            case ABANDONED -> IncidentEntity.IncidentType.ABANDONED;
            case INJURED -> IncidentEntity.IncidentType.INJURED;
            case LOST -> IncidentEntity.IncidentType.LOST;
            case ABUSE -> IncidentEntity.IncidentType.ABUSE;
            default -> IncidentEntity.IncidentType.UNKNOWN;
        };
    }

    private IncidentType mapToProtoIncidentType(IncidentEntity.IncidentType type) {
        return switch (type) {
            case ACCIDENT -> IncidentType.ACCIDENT;
            case ABANDONED -> IncidentType.ABANDONED;
            case INJURED -> IncidentType.INJURED;
            case LOST -> IncidentType.LOST;
            case ABUSE -> IncidentType.ABUSE;
            default -> IncidentType.UNKNOWN;
        };
    }

    private IncidentStatus mapToProtoIncidentStatus(IncidentEntity.IncidentStatus status) {
        return switch (status) {
            case OPEN -> IncidentStatus.OPEN;
            case ASSIGNED -> IncidentStatus.ASSIGNED;
            case IN_PROGRESS -> IncidentStatus.IN_PROGRESS;
            case RESOLVED -> IncidentStatus.RESOLVED;
            case CANCELLED -> IncidentStatus.CANCELLED;
        };
    }
}
