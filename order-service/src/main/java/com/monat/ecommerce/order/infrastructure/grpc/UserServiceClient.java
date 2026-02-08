package com.monat.ecommerce.order.infrastructure.grpc;

import com.monat.ecommerce.grpc.user.GetUserRequest;
import com.monat.ecommerce.grpc.user.GetUserResponse;
import com.monat.ecommerce.grpc.user.UserServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * gRPC client for User Service with Resilience4j
 */
@Service
@Slf4j
public class UserServiceClient {

    @Value("${grpc.client.user-service.host:localhost}")
    private String host;

    @Value("${grpc.client.user-service.port:9081}")
    private int port;

    private ManagedChannel channel;
    private UserServiceGrpc.UserServiceBlockingStub stub;

    @PostConstruct
    public void init() {
        channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        stub = UserServiceGrpc.newBlockingStub(channel);
        log.info("User Service gRPC client initialized: {}:{}", host, port);
    }

    @PreDestroy
    public void destroy() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            log.info("User Service gRPC channel shut down");
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserFallback")
    @Retry(name = "user-service")
    public GetUserResponse getUser(String userId) {
        log.info("Fetching user details: userId={}", userId);

        GetUserRequest request = GetUserRequest.newBuilder()
                .setUserId(userId)
                .build();

        GetUserResponse response = stub.getUser(request);
        if (response.hasUser()) {
            log.info("User retrieved: userId={}, username={}", userId, response.getUser().getUsername());
        }

        return response;
    }

    /**
     * Fallback method when user service is unavailable
     */
    private GetUserResponse getUserFallback(String userId, Exception ex) {
        log.error("User Service circuit breaker activated for userId: {}. Error: {}",
                userId, ex.getMessage());

        return GetUserResponse.newBuilder()
                .setFound(false)
                .build();
    }
}
