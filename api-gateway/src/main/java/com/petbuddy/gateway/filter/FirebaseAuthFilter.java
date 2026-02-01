package com.petbuddy.gateway.filter;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.petbuddy.gateway.context.AuthContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class FirebaseAuthFilter extends OncePerRequestFilter {

    public static final String AUTH_CONTEXT_ATTRIBUTE = "AUTH_CONTEXT";

    @Value("${firebase.mock-auth:true}")
    private boolean mockAuth;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip CORS preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip auth for GraphiQL and introspection queries
        String path = request.getRequestURI();
        if (path.contains("graphiql") || path.contains("favicon")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // If mock mode, create mock context
        if (mockAuth) {
            String mockUid = authHeader != null ? authHeader.replace("Bearer ", "") : null;
            AuthContext mockContext = AuthContext.mockContext(mockUid);
            request.setAttribute(AUTH_CONTEXT_ATTRIBUTE, mockContext);
            log.debug("Mock auth context created: {}", mockContext.getUid());
            filterChain.doFilter(request, response);
            return;
        }

        // Validate Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.error("Firebase is not initialized");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\":\"Firebase not configured\"}");
                return;
            }

            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);

            AuthContext authContext = AuthContext.builder()
                    .uid(decodedToken.getUid())
                    .email(decodedToken.getEmail())
                    .name(decodedToken.getName())
                    .emailVerified(decodedToken.isEmailVerified())
                    .build();

            request.setAttribute(AUTH_CONTEXT_ATTRIBUTE, authContext);
            log.debug("Authenticated user: {}", authContext.getUid());

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Token verification failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
        }
    }
}
