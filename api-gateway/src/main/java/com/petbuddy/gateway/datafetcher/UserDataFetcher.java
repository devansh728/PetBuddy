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

import java.util.Map;

@DgsComponent
@RequiredArgsConstructor
@Slf4j
public class UserDataFetcher {

    private final ProfileServiceGrpc.ProfileServiceBlockingStub profileService;

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

            return Map.of(
                    "userId", response.getUserId(),
                    "fullName", response.getFullName(),
                    "email", response.getEmail(),
                    "isVerified", response.getIsVerified(),
                    "createdAt", response.getCreatedAt());

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: {}", e.getStatus().getDescription());
            throw new GraphQLException("Failed to register user: " + e.getStatus().getDescription());
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

            return Map.of(
                    "userId", response.getUserId(),
                    "fullName", response.getFullName(),
                    "email", response.getEmail(),
                    "isVerified", response.getIsVerified(),
                    "createdAt", response.getCreatedAt());

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
}
