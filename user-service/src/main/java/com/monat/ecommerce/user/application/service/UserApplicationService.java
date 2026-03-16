package com.monat.ecommerce.user.application.service;

import com.monat.ecommerce.common.dto.PagedResponse;
import com.monat.ecommerce.common.exception.BusinessException;
import com.monat.ecommerce.common.exception.ResourceNotFoundException;
import com.monat.ecommerce.user.application.dto.*;
import com.monat.ecommerce.user.domain.model.User;
import com.monat.ecommerce.user.domain.model.UserAddress;
import com.monat.ecommerce.user.domain.model.UserStatus;
import com.monat.ecommerce.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for user management
 */
@Slf4j
/**
 * User Application Service.
 * <p>
 * This class contains the business logic for the User Service.
 * It orchestrates the flow of data between the Controller (API layer) and the
 * Repository (Data layer).
 * </p>
 * 
 * @Service indicates that this class is a "Service", which is a
 *          specialized @Component for holding business logic.
 * 
 * @Transactional ensures that methods are executed within a database
 *                transaction.
 *                If a method fails (throws an unchecked exception), the
 *                transaction is rolled back.
 */
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

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
