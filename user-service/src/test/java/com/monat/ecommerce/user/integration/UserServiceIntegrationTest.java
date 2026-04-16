package com.monat.ecommerce.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monat.ecommerce.user.application.dto.UserRegistrationRequest;
import com.monat.ecommerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for User Service using a real PostgreSQL container.
 *
 * <p>Skip Strategy:
 * {@code @ExtendWith(DockerRequiredExtension.class)} runs as a JUnit 5 ExecutionCondition,
 * which is evaluated BEFORE any BeforeAllCallback (including Testcontainers and Spring context
 * loading). When Docker is not available, the entire test class is marked SKIPPED –
 * no container start is attempted and no Spring context is loaded.
 */
@ExtendWith(DockerRequiredExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:${wiremock.server.port}/realms/test",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:${wiremock.server.port}/realms/test/protocol/openid-connect/certs"
    }
)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
class UserServiceIntegrationTest {

    private static final String TEST_SCHEMA = "it_user_test";

    // Lazy singleton — started once per JVM in @DynamicPropertySource (which only runs
    // when DockerRequiredExtension allows: i.e. Docker IS available).
    private static PostgreSQLContainer<?> postgres;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private Integer port;

    /**
     * Spring invokes this static method during context initialization.
     * At this point DockerRequiredExtension has already confirmed Docker is up,
     * so starting the container here is safe.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Lazy-start the container exactly once per JVM
        if (postgres == null) {
            postgres = new PostgreSQLContainer<>("postgres:15-alpine");
            postgres.start();
        }

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Disable distributed tracing to avoid network calls during tests
        registry.add("management.tracing.enabled", () -> "false");
        registry.add("management.zipkin.tracing.endpoint", () -> "http://localhost:9411/api/v2/spans");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("grpc.server.port", () -> "0");
    }

    @AfterAll
    static void stopContainers() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
            postgres = null;
        }
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Stub JWKS request just in case Spring Security requests it
        stubFor(get(urlEqualTo("/realms/test/protocol/openid-connect/certs"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"keys\":[]}")));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void shouldRegisterUser() throws Exception {
        // Arrange
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("integration@example.com")
                .username("integrationuser")
                .password("Password123!")
                .firstName("Integration")
                .lastName("Test")
                .phone("1234567890")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/users/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("integration@example.com"))
                .andExpect(jsonPath("$.data.id").exists());

        // Verify DB persistence
        assert userRepository.existsByEmail("integration@example.com");
    }
}
