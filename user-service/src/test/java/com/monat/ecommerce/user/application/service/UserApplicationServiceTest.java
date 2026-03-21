package com.monat.ecommerce.user.application.service;

import com.monat.ecommerce.common.exception.BusinessException;
import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.user.application.dto.UserMapper;
import com.monat.ecommerce.user.application.dto.UserRegistrationRequest;
import com.monat.ecommerce.user.application.dto.UserResponse;
import com.monat.ecommerce.user.domain.model.User;
import com.monat.ecommerce.user.domain.model.UserStatus;
import com.monat.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserApplicationService userApplicationService;

    @Test
    @DisplayName("Should register user successfully when email and username are unique")
    void shouldRegisterUserSuccessfully() {
        // Arrange
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(request.email())
                .username(request.username())
                .status(UserStatus.ACTIVE)
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userMapper.toUser(any(UserRegistrationRequest.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        UserResponse actualResponse = userApplicationService.registerUser(request);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.id(), actualResponse.id());
        assertEquals(expectedResponse.email(), actualResponse.email());

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).existsByUsername(request.username());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Arrange
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("existing@example.com")
                .username("newuser")
                .build();

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userApplicationService.registerUser(request));

        assertEquals("EMAIL_EXISTS", exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void shouldGetUserByIdSuccessfully() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("test@example.com").build();
        UserResponse response = UserResponse.builder()
                .id(userId)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        // Act
        UserResponse result = userApplicationService.getUserById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.id());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user ID not found")
    void shouldThrowExceptionWhenUserIdNotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userApplicationService.getUserById(userId));
    }
}
