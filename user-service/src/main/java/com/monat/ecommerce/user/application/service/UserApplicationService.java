package com.monat.ecommerce.user.application.service;

import com.monat.ecommerce.common.dto.PagedResponse;
import com.monat.ecommerce.common.exception.BusinessException;
import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.user.application.dto.*;
import com.monat.ecommerce.user.domain.model.User;
import com.monat.ecommerce.user.domain.model.UserAddress;
import com.monat.ecommerce.user.domain.model.UserStatus;
import com.monat.ecommerce.user.domain.repository.UserRepository;
import com.monat.ecommerce.common.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Coordinator of User-related business logic.
 * 
 * Educational Note:
 * - @Transactional: Orchestrates database transactions. If any unchecked 
 *   exception is thrown, changes are rolled back automatically.
 * - readOnly = true: Optimizes Hibernate performance by skipping dirty checking 
 *   and reducing lock contention for read-only operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Registering new user with email: {}", request.email());

        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered", "EMAIL_EXISTS", 409);
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already taken", "USERNAME_EXISTS", 409);
        }

        // Create user using MapStruct
        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return userMapper.toUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        log.info("Attempting login for user: {}", request.username());

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException("Invalid username or password", "AUTH_FAILED", 401));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid username or password", "AUTH_FAILED", 401);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("User account is not active", "USER_INACTIVE", 403);
        }

        String token = jwtUtils.generateToken(user.getUsername(), Map.of(
                "email", user.getEmail(),
                "role", "USER" // Default role for now
        ));

        log.info("User {} logged in successfully", request.username());

        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(token)
                .username(user.getUsername())
                .expiresIn(86400000 / 1000) // 24 hours in seconds
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        log.debug("Fetching user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: " + email));

        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<User> userPage = userRepository.findAll(pageable);

        return PagedResponse.<UserResponse>builder()
                .content(userMapper.toUserResponseList(userPage.getContent()))
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }

    @Transactional
    public AddressResponse addAddress(UUID userId, CreateAddressRequest request) {
        log.info("Adding address for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        UserAddress address = userMapper.toUserAddress(request);
        user.addAddress(address);

        // If this is the first address or marked as default, set it as default
        if (user.getAddresses().size() == 1 || Boolean.TRUE.equals(request.isDefault())) {
            // Unset other defaults if this is marked as default
            if (Boolean.TRUE.equals(request.isDefault())) {
                user.getAddresses().forEach(addr -> {
                    if (!addr.equals(address)) {
                        addr.setIsDefault(false);
                    }
                });
            }
            address.setIsDefault(true);
        }

        userRepository.save(user);
        log.info("Address added successfully for user: {}", userId);

        return userMapper.toAddressResponse(address);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(UUID userId) {
        log.debug("Fetching addresses for user: {}", userId);

        User user = userRepository.findByIdWithAddresses(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        return userMapper.toAddressResponseList(user.getAddresses());
    }

    @Transactional
    public void updateUserStatus(UUID userId, UserStatus status) {
        log.info("Updating user {} status to: {}", userId, status);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        user.setStatus(status);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean validateUser(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(false);
    }
}
