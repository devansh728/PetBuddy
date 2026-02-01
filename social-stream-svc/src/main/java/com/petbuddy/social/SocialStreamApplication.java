package com.petbuddy.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.springframework.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration.class,
        org.springframework.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration.class
})
public class SocialStreamApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialStreamApplication.class, args);
    }
}
