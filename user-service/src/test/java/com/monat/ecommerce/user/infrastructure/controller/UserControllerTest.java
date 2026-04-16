package com.monat.ecommerce.user.infrastructure.controller;

import com.monat.ecommerce.common.util.LocalizationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monat.ecommerce.user.application.dto.UserRegistrationRequest;
import com.monat.ecommerce.user.application.dto.UserResponse;
import com.monat.ecommerce.user.application.service.UserApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserApplicationService userApplicationService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private LocalizationUtils localizationUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should register user successfully")
    @WithMockUser // Helper to bypass security for non-protected endpoints or mock user
    void shouldRegisterUserSuccessfully() throws Exception {
        // Arrange
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        UserResponse response = UserResponse.builder()
                .id(UUID.randomUUID())
                .email(request.email())
                .username(request.username())
                .build();

        when(userApplicationService.registerUser(any(UserRegistrationRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                .with(csrf()) // Required for non-GET requests when CSRF is enabled (or mock it)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(request.email()));
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    @WithMockUser
    void shouldGetUserByIdSuccessfully() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserResponse response = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .build();

        when(userApplicationService.getUserById(userId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId.toString()));
    }
}
