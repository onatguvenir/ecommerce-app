package com.monat.ecommerce.user.infrastructure.controller;

import com.monat.ecommerce.common.dto.ApiResponse;
import com.monat.ecommerce.common.dto.PagedResponse;
import com.monat.ecommerce.user.application.dto.*;
import com.monat.ecommerce.user.application.service.UserApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user operations
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user registration and management")
public class UserController {

    private final UserApplicationService userApplicationService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        
        UserResponse response = userApplicationService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        UserResponse response = userApplicationService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        UserResponse response = userApplicationService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<UserResponse> response = userApplicationService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{userId}/addresses")
    @Operation(summary = "Add address to user")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateAddressRequest request) {
        
        AddressResponse response = userApplicationService.addAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Address added successfully"));
    }

    @GetMapping("/{userId}/addresses")
    @Operation(summary = "Get user addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getUserAddresses(
            @PathVariable UUID userId) {
        
        List<AddressResponse> response = userApplicationService.getUserAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{userId}/validate")
    @Operation(summary = "Validate if user is active")
    public ResponseEntity<ApiResponse<Boolean>> validateUser(@PathVariable UUID userId) {
        boolean isValid = userApplicationService.validateUser(userId);
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }
}
