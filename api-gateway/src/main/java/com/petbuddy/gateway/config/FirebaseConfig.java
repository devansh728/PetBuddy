package com.petbuddy.gateway.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${firebase.mock-auth:true}")
    private boolean mockAuth;

    @PostConstruct
    public void initialize() {
        if (mockAuth) {
            log.warn("Firebase Auth is in MOCK mode. Set firebase.mock-auth=false for production.");
            return;
        }

        if (credentialsPath == null || credentialsPath.isEmpty()) {
            log.warn("Firebase credentials path not configured. Auth will be mocked.");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream(credentialsPath);
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
            throw new RuntimeException("Could not initialize Firebase", e);
        }
    }
}
