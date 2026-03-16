package com.monat.ecommerce.notification.infrastructure.grpc;

import com.monat.ecommerce.grpc.user.GetUserResponse;
import com.monat.ecommerce.grpc.user.User;
import com.monat.ecommerce.grpc.user.UserServiceGrpc;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

    @InjectMocks
    private UserServiceClient userServiceClient;

    @Mock
    private ManagedChannel channel;

    @Mock
    private UserServiceGrpc.UserServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userServiceClient, "stub", stub);
    }

    @Test
    void testGetUser_Success() {
        // Arrange
        String userId = "user-123";
        User user = User.newBuilder()
                .setId(userId)
                .setEmail("test@example.com")
                .setFirstName("John")
                .setLastName("Doe")
                .build();

        GetUserResponse response = GetUserResponse.newBuilder()
                .setFound(true)
                .setUser(user)
                .build();

        when(stub.getUser(any())).thenReturn(response);

        // Act
        Optional<User> result = userServiceClient.getUser(userId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        assertEquals("John", result.get().getFirstName());
    }

    @Test
    void testGetUser_NotFound() {
        // Arrange
        String userId = "user-123";
        GetUserResponse response = GetUserResponse.newBuilder()
                .setFound(false)
                .build();

        when(stub.getUser(any())).thenReturn(response);

        // Act
        Optional<User> result = userServiceClient.getUser(userId);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserFallback() {
        // Arrange
        String userId = "user-123";
        Exception ex = new RuntimeException("Simulated error");

        // Act (Call fallback method via reflection since it's private and typically invoked by Resilience4j)
        Optional<User> result = ReflectionTestUtils.invokeMethod(userServiceClient, "getUserFallback", userId, ex);

        // Assert
        assertTrue(result.isEmpty());
    }
}
