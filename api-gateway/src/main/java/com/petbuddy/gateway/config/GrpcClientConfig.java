package com.petbuddy.gateway.config;

import com.petbuddy.social.grpc.SocialServiceGrpc;
import com.profile.rescue.grpc.ProfileServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class GrpcClientConfig {

    @Value("${grpc.client.profile-service.address:static://localhost:9090}")
    private String profileServiceAddress;

    @Value("${grpc.client.social-service.address:static://localhost:9091}")
    private String socialServiceAddress;

    private ManagedChannel profileChannel;
    private ManagedChannel socialChannel;

    @Bean
    public ManagedChannel profileServiceChannel() {
        profileChannel = createChannel(profileServiceAddress);
        return profileChannel;
    }

    @Bean
    public ManagedChannel socialServiceChannel() {
        socialChannel = createChannel(socialServiceAddress);
        return socialChannel;
    }

    @Bean
    public ProfileServiceGrpc.ProfileServiceBlockingStub profileServiceStub(ManagedChannel profileServiceChannel) {
        return ProfileServiceGrpc.newBlockingStub(profileServiceChannel);
    }

    @Bean
    public SocialServiceGrpc.SocialServiceBlockingStub socialServiceStub(ManagedChannel socialServiceChannel) {
        return SocialServiceGrpc.newBlockingStub(socialServiceChannel);
    }

    private ManagedChannel createChannel(String address) {
        String cleanAddress = address.replace("static://", "");
        String[] parts = cleanAddress.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9090;

        return ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext() // For local dev; use TLS in production
                .build();
    }

    @PreDestroy
    public void shutdown() {
        shutdownChannel(profileChannel);
        shutdownChannel(socialChannel);
    }

    private void shutdownChannel(ManagedChannel channel) {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                channel.shutdownNow();
            }
        }
    }
}
