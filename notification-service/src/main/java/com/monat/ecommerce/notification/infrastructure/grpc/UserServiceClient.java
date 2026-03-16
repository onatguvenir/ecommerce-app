package com.monat.ecommerce.notification.infrastructure.grpc;

import com.monat.ecommerce.grpc.user.GetUserRequest;
import com.monat.ecommerce.grpc.user.GetUserResponse;
import com.monat.ecommerce.grpc.user.User;
import com.monat.ecommerce.grpc.user.UserServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * gRPC client for User Service.
 *
 * Notification Service, Kafka event'inden gelen userId ile User Service'e
 * gRPC çağrısı yaparak müşterinin e-posta ve adını alır.
 *
 * CircuitBreaker: User Service erişilemez olduğunda devre kırılır;
 *   fallback boş Optional döndürerek mail sessizce atlanır.
 *
 * Retry: Geçici ağ problemleri (5xx) için 3 kez exponential backoff ile denenir.
 */
@Slf4j
@Service
public class UserServiceClient {

    @Value("${grpc.client.user-service.host:localhost}")
    private String host;

    @Value("${grpc.client.user-service.port:9081}")
    private int port;

    private ManagedChannel channel;
    private UserServiceGrpc.UserServiceBlockingStub stub;

    @PostConstruct
    public void init() {
        // Plaintext bağlantı — production'da TLS kullanılmalı
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

    /**
     * Verilen userId için User Service'ten kullanıcı bilgilerini çeker.
     *
     * @param userId UUID formatında kullanıcı kimliği
     * @return bulunan kullanıcı; User Service erişilemez ise empty Optional
     */
    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserFallback")
    @Retry(name = "user-service")
    public Optional<User> getUser(String userId) {
        log.debug("Fetching user details for notification: userId={}", userId);

        GetUserRequest request = GetUserRequest.newBuilder()
                .setUserId(userId)
                .build();

        GetUserResponse response = stub.getUser(request);

        if (response.getFound()) {
            log.debug("User details retrieved: userId={}, email={}", userId, response.getUser().getEmail());
            return Optional.of(response.getUser());
        }

        log.warn("User not found in User Service: userId={}", userId);
        return Optional.empty();
    }

    /**
     * Fallback: User Service erişilemez veya hata verirse çağrılır.
     * Mail gönderimini engellemeden akışın devam etmesi için empty döner.
     */
    private Optional<User> getUserFallback(String userId, Exception ex) {
        log.error("User Service circuit breaker triggered for userId={}. Notification will be skipped. Error: {}",
                userId, ex.getMessage());
        return Optional.empty();
    }
}
