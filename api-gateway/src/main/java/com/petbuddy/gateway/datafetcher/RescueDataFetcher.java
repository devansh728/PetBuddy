package com.petbuddy.gateway.datafetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import com.petbuddy.gateway.context.AuthContext;
import com.petbuddy.gateway.filter.FirebaseAuthFilter;
import com.profile.rescue.grpc.*;
import graphql.GraphQLException;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@DgsComponent
@RequiredArgsConstructor
@Slf4j
public class RescueDataFetcher {

    private final ProfileServiceGrpc.ProfileServiceBlockingStub profileService;

    private String getInternalUserId(String firebaseUid) {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setFirebaseUid(firebaseUid)
                .build();
        return profileService.getUserProfile(request).getUserId();
    }

    @DgsMutation
    public Boolean updateLocation(
            @InputArgument("latitude") Double latitude,
            @InputArgument("longitude") Double longitude,
            @InputArgument("isVolunteer") Boolean isVolunteer) {

        AuthContext auth = getAuthContext();

        try {
            LocationRequest request = LocationRequest.newBuilder()
                    .setUserId(getInternalUserId(auth.getUid()))
                    .setLatitude(latitude)
                    .setLongitude(longitude)
                    .setIsVolunteer(isVolunteer != null && isVolunteer)
                    .build();

            log.info("Updating location for user: {}", auth.getUid());
            profileService.updateUserLocation(request);

            return true;

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to update location: " + e.getStatus().getDescription());
        }
    }

    @DgsMutation
    public Map<String, Object> reportIncident(@InputArgument("input") Map<String, Object> input) {
        AuthContext auth = getAuthContext();

        Double latitude = ((Number) input.get("latitude")).doubleValue();
        Double longitude = ((Number) input.get("longitude")).doubleValue();
        String typeStr = (String) input.get("type");
        String description = (String) input.getOrDefault("description", "");
        String animalType = (String) input.getOrDefault("animalType", "");
        String imageUrl = (String) input.getOrDefault("imageUrl", "");

        try {
            IncidentRequest.Builder requestBuilder = IncidentRequest.newBuilder()
                    .setReporterId(auth.getUid())
                    .setLatitude(latitude)
                    .setLongitude(longitude)
                    .setType(mapIncidentType(typeStr))
                    .setDescription(description != null ? description : "")
                    .setAnimalType(animalType != null ? animalType : "")
                    .setImageUrl(imageUrl != null ? imageUrl : "");

            log.info("Reporting incident at ({}, {}) by user: {}", latitude, longitude, auth.getUid());
            IncidentResponse response = profileService.reportIncident(requestBuilder.build());

            return mapIncidentToMap(response);

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to report incident: " + e.getStatus().getDescription());
        }
    }

    @DgsQuery
    public List<Map<String, Object>> getNearbyIncidents(
            @InputArgument("latitude") Double latitude,
            @InputArgument("longitude") Double longitude,
            @InputArgument("radiusMeters") Double radiusMeters) {

        getAuthContext(); // Ensure authenticated

        try {
            NearbyRequest request = NearbyRequest.newBuilder()
                    .setLatitude(latitude)
                    .setLongitude(longitude)
                    .setRadiusMeters(radiusMeters != null ? radiusMeters : 5000.0)
                    .build();

            log.info("Getting nearby incidents at ({}, {})", latitude, longitude);
            IncidentListResponse response = profileService.getNearbyIncidents(request);

            List<Map<String, Object>> incidents = new ArrayList<>();
            for (IncidentResponse incident : response.getIncidentsList()) {
                incidents.add(mapIncidentToMap(incident));
            }

            return incidents;

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to get nearby incidents: " + e.getStatus().getDescription());
        }
    }

    private Map<String, Object> mapIncidentToMap(IncidentResponse incident) {
        return Map.ofEntries(
                Map.entry("incidentId", incident.getIncidentId()),
                Map.entry("reporterId", incident.getReporterId()),
                Map.entry("latitude", incident.getLatitude()),
                Map.entry("longitude", incident.getLongitude()),
                Map.entry("type", incident.getType().name()),
                Map.entry("status", incident.getStatus().name()),
                Map.entry("description", incident.getDescription()),
                Map.entry("animalType", incident.getAnimalType()),
                Map.entry("imageUrl", incident.getImageUrl()),
                Map.entry("volunteersNotified", incident.getVolunteersNotified()),
                Map.entry("createdAt", incident.getCreatedAt()));
    }

    private IncidentType mapIncidentType(String type) {
        if (type == null)
            return IncidentType.UNKNOWN;
        return switch (type.toUpperCase()) {
            case "ACCIDENT" -> IncidentType.ACCIDENT;
            case "ABANDONED" -> IncidentType.ABANDONED;
            case "INJURED" -> IncidentType.INJURED;
            case "LOST" -> IncidentType.LOST;
            case "ABUSE" -> IncidentType.ABUSE;
            default -> IncidentType.UNKNOWN;
        };
    }

    private AuthContext getAuthContext() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new GraphQLException("No request context available");
        }

        HttpServletRequest request = attrs.getRequest();
        AuthContext auth = (AuthContext) request.getAttribute(FirebaseAuthFilter.AUTH_CONTEXT_ATTRIBUTE);

        if (auth == null) {
            throw new GraphQLException("Authentication required");
        }

        return auth;
    }
}
