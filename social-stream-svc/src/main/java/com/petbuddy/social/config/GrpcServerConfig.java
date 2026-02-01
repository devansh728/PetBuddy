package com.petbuddy.social.config;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.protobuf.services.HealthStatusManager;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manual gRPC Server configuration to bypass Spring gRPC 1.0.1 keepAlive NPE
 * bug.
 * Uses ApplicationRunner to start server after all beans are initialized,
 * and blocks thread to keep the application running.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class GrpcServerConfig implements ApplicationRunner {

    private final List<BindableService> grpcServices;

    @Value("${spring.grpc.server.port:9091}")
    private int grpcPort;

    private Server server;
    private HealthStatusManager healthStatusManager;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        startGrpcServer();
        // Block main thread to keep app running
        blockUntilShutdown();
    }

    private void startGrpcServer() throws IOException {
        healthStatusManager = new HealthStatusManager();

        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(grpcPort)
                .addService(ProtoReflectionService.newInstance())
                .addService(healthStatusManager.getHealthService());

        // Register all discovered gRPC services
        for (BindableService service : grpcServices) {
            serverBuilder.addService(service);
            log.info("Registered gRPC service: {}", service.bindService().getServiceDescriptor().getName());
        }

        server = serverBuilder.build().start();
        log.info("gRPC server started on port {}", grpcPort);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server...");
            stopGrpcServer();
        }));
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    @PreDestroy
    public void stopGrpcServer() {
        if (server != null) {
            try {
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
