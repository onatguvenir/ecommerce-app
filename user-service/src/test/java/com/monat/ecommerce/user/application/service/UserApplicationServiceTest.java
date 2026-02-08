package com.monat.ecommerce.user.application.service;

import com.monat.ecommerce.common.exception.BusinessException;
import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.user.application.dto.UserMapper;
import com.monat.ecommerce.user.application.dto.UserRegistrationRequest;
import com.monat.ecommerce.user.application.dto.UserResponse;
import com.monat.ecommerce.user.domain.model.User;
import com.monat.ecommerce.user.domain.model.UserStatus;
import com.monat.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserApplicationService
 */
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

    private UserRegistrationRequest registrationRequest;
    private User user;
    private UserResponse userResponse;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        registrationRequest = UserRegistrationRequest.builder()
                .username("john")
                .email("john@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .build();

        user = User.builder()
                .id(userId)
                .username("john")
                .email("john@example.com")
                .passwordHash("$2a$10$encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .status(UserStatus.ACTIVE)
                .build();

        userResponse = UserResponse.builder()
                .id(userId)
                .username("john")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .status("ACTIVE")
                .build();
    }

    @Test
    void registerUser_Success() {
        // Given
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        // When
        UserResponse response = userApplicationService.registerUser(registrationRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("john");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        verify(userRepository, times(1)).existsByEmail("john@example.com");
        verify(userRepository, times(1)).existsByUsername("john");
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    void registerUser_EmailAlreadyExists() {
        // Given
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userApplicationService.registerUser(registrationRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, times(1)).existsByEmail("john@example.com");
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_UsernameAlreadyExists() {
        // Given
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("john")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userApplicationService.registerUser(registrationRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, times(1)).existsByEmail("john@example.com");
        verify(userRepository, times(1)).existsByUsername("john");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_Found() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        // When
        UserResponse response = userApplicationService.getUserById(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo("john");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserById_NotFound() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userApplicationService.getUserById(nonExistentUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(userRepository, times(1)).findById(nonExistentUserId);
    }

    @Test
    void getUserByEmail_Found() {
        // Given
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        // When
        UserResponse response = userApplicationService.getUserByEmail("john@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        verify(userRepository, times(1)).findByEmail("john@example.com");
    }

    @Test
    void getUserByEmail_NotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userApplicationService.getUserByEmail("nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with email");

        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void validateUser_ActiveUser() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        boolean isValid = userApplicationService.validateUser(userId);

        // Then
        assertThat(isValid).isTrue();
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void validateUser_InactiveUser() {
        // Given
        user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        boolean isValid = userApplicationService.validateUser(userId);

        // Then
        assertThat(isValid).isFalse();
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void validateUser_UserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        boolean isValid = userApplicationService.validateUser(userId);

        // Then
        assertThat(isValid).isFalse();
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void updateUserStatus_Success() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userApplicationService.updateUserStatus(userId, UserStatus.INACTIVE);

        // Then
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(user);
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void updateUserStatus_UserNotFound() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userApplicationService.updateUserStatus(nonExistentUserId, UserStatus.INACTIVE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");

        verify(userRepository, times(1)).findById(nonExistentUserId);
        verify(userRepository, never()).save(any(User.class));
    }
}
