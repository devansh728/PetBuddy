package com.petbuddy.gateway.context;

import lombok.Builder;
import lombok.Data;

/**
 * Authentication context holding Firebase user information.
 * Stored as a request attribute after successful token verification.
 */
@Data
@Builder
public class AuthContext {

    private String uid;
    private String email;
    private String name;
    private boolean emailVerified;

    /**
     * Creates a mock auth context for local development.
     */
    public static AuthContext mockContext(String mockUid) {
        return AuthContext.builder()
                .uid(mockUid != null ? mockUid : "mock-user-" + System.currentTimeMillis())
                .email("mock@example.com")
                .name("Mock User")
                .emailVerified(true)
                .build();
    }
}
