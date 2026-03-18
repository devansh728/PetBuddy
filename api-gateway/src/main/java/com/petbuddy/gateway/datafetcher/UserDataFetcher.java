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
import java.util.List;

import java.util.Map;
import java.util.stream.Collectors;

@DgsComponent
@RequiredArgsConstructor
@Slf4j
public class UserDataFetcher {

    private final ProfileServiceGrpc.ProfileServiceBlockingStub profileService;

    private String getInternalUserId(String firebaseUid) {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setFirebaseUid(firebaseUid)
                .build();
        return profileService.getUserProfile(request).getUserId();
    }

    @DgsMutation
    public Map<String, Object> registerUser(@InputArgument("input") Map<String, Object> input) {
        AuthContext auth = getAuthContext();

        String fullName = (String) input.get("fullName");
        String email = (String) input.get("email");
        String fcmToken = (String) input.getOrDefault("fcmToken", "");

        if (fullName == null || fullName.isBlank()) {
            throw new GraphQLException("fullName is required");
        }
        if (email == null || email.isBlank()) {
            throw new GraphQLException("email is required");
        }

        try {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setFirebaseUid(auth.getUid())
                    .setFullName(fullName)
                    .setEmail(email)
                    .setFcmToken(fcmToken != null ? fcmToken : "")
                    .build();

            log.info("Registering user: {} with firebaseUid: {}", email, auth.getUid());
            UserProfileResponse response = profileService.createUserProfile(request);

            return mapProfileResponse(response);

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to register user: " + e.getStatus().getDescription());
        }
    }

    @DgsMutation
    public Boolean updateVolunteerStatus(@InputArgument("isVolunteer") Boolean isVolunteer) {
        AuthContext auth = getAuthContext();

        try {

            String internalUserId = getInternalUserId(auth.getUid());

            UpdateVolunteerStatusRequest request = UpdateVolunteerStatusRequest.newBuilder()
                    .setUserId(internalUserId)
                    .setIsVolunteer(isVolunteer != null && isVolunteer)
                    .build();

            log.info("Updating volunteer status for user: {} to {}", auth.getUid(), request.getIsVolunteer());
            profileService.updateVolunteerStatus(request);

            return true;

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to update volunteer status: " + e.getStatus().getDescription());
        }
    }

    // ==================== PET OPERATIONS ====================

    @DgsMutation
    public List<Map<String, Object>> addPet(@InputArgument("pets") List<Map<String, Object>> petsInput) {
        AuthContext auth = getAuthContext();
        try {
            String internalUserId = getInternalUserId(auth.getUid());

            AddPetRequest.Builder requestBuilder = AddPetRequest.newBuilder()
                    .setUserId(internalUserId);

            for (Map<String, Object> pet : petsInput) {
                PetData.Builder petData = PetData.newBuilder()
                        .setName((String) pet.get("name"))
                        .setBreed(pet.get("breed") != null ? (String) pet.get("breed") : "")
                        .setAgeMonths(pet.get("ageMonths") != null ? (Integer) pet.get("ageMonths") : 0)
                        .setImageUrl(pet.get("imageUrl") != null ? (String) pet.get("imageUrl") : "");
                
                requestBuilder.addPets(petData);
            }

            log.info("Batch adding {} pets for internal user: {}", petsInput.size(), internalUserId);
            PetListResponse response = profileService.addPet(requestBuilder.build());

            return mapPetListResponse(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC addPet failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to add pet(s)");
        }
    }

    @DgsQuery
    public List<Map<String, Object>> getPets() {
        AuthContext auth = getAuthContext();
        try {
            String internalUserId = getInternalUserId(auth.getUid());

            GetPetsRequest request = GetPetsRequest.newBuilder()
                    .setUserId(internalUserId)
                    .build();

            log.info("Fetching pets for internal user: {}", internalUserId);
            PetListResponse response = profileService.getPetsByUser(request);

            return mapPetListResponse(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC getPets failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to fetch pets");
        }
    }

    @DgsQuery
    public Map<String, Object> getProfile() {
        AuthContext auth = getAuthContext();

        try {
            GetUserRequest request = GetUserRequest.newBuilder()
                    .setFirebaseUid(auth.getUid())
                    .build();

            log.info("Getting profile for firebaseUid: {}", auth.getUid());
            UserProfileResponse response = profileService.getUserProfile(request);

            return mapProfileResponse(response);

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to get profile: " + e.getStatus().getDescription());
        }
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

    private Map<String, Object> mapProfileResponse(UserProfileResponse response) {
        return Map.of(
                "userId", response.getUserId(),
                "fullName", response.getFullName(),
                "email", response.getEmail(),
                "isVerified", response.getIsVerified(),
                "isVolunteer", response.getIsVolunteer(), 
                "createdAt", response.getCreatedAt()
        );
    }

    private List<Map<String, Object>> mapPetListResponse(PetListResponse response) {
        return response.getPetsList().stream().map(pet -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("petId", pet.getPetId());
            map.put("name", pet.getName());
            map.put("breed", pet.getBreed());
            map.put("ageMonths", pet.getAgeMonths());
            map.put("imageUrl", pet.getImageUrl());
            map.put("createdAt", pet.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }
}
