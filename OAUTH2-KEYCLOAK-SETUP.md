# OAuth2 & Keycloak Integration Guide

## Option 1: Using Keycloak (Recommended for Production)

### 1. Add Keycloak to Docker Compose

```yaml
# Add to docker-compose.yml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    container_name: monat-keycloak
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: postgres
      KC_DB_PASSWORD: postgres
    command: start-dev
    ports:
      - "8180:8080"
    depends_on:
      - postgres
    networks:
      - monat-network
```

### 2. Configure Services for OAuth2

#### API Gateway Configuration

```yaml
# api-gateway/src/main/resources/application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/ecommerce
          jwk-set-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/certs

  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - TokenRelay= # Forward OAuth2 token
```

#### User Service Dependencies

```xml
<!-- Add to user-service/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

#### User Service Configuration

```yaml
# user-service/src/main/resources/application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/ecommerce
          jwk-set-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/certs
      
      client:
        registration:
          keycloak:
            client-id: ecommerce-client
            client-secret: your-client-secret
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid, profile, email
        provider:
          keycloak:
            issuer-uri: http://localhost:8180/realms/ecommerce
            user-name-attribute: preferred_username
```

### 3. Security Configuration

```java
package com.monat.ecommerce.user.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class OAuth2SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/**", "/api/users/register").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .oauth2Client();
        
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = 
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = 
            new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            grantedAuthoritiesConverter);
        
        return jwtAuthenticationConverter;
    }
}
```

### 4. Keycloak Realm Setup

**Access Keycloak Admin Console:**
- URL: http://localhost:8180
- Username: admin
- Password: admin

**Create Realm:**
1. Click "Create Realm"
2. Name: `ecommerce`
3. Click "Create"

**Create Client:**
1. Go to Clients → Create Client
2. Client ID: `ecommerce-client`
3. Client authentication: ON
4. Authorization: ON
5. Valid redirect URIs: `http://localhost:8080/*`
6. Web origins: `http://localhost:8080`

**Create Roles:**
1. Go to Realm Roles
2. Create roles: `USER`, `ADMIN`, `MANAGER`

**Create Users:**
1. Go to Users → Add User
2. Username: `testuser`
3. Email: `test@example.com`
4. Save
5. Go to Credentials tab
6. Set password: `password123`
7. Temporary: OFF
8. Go to Role Mappings
9. Assign `USER` role

### 5. Testing OAuth2 Login

```bash
# Get access token
curl -X POST 'http://localhost:8180/realms/ecommerce/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=ecommerce-client' \
  -d 'client_secret=YOUR_CLIENT_SECRET' \
  -d 'username=testuser' \
  -d 'password=password123'

# Use token for API calls
curl http://localhost:8080/api/products \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Option 2: OAuth2 with Spring Authorization Server

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>
```

### 2. Configuration

```java
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults());
        
        http.exceptionHandling((exceptions) -> exceptions
            .authenticationEntryPoint(
                new LoginUrlAuthenticationEntryPoint("/login"))
        );

        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("ecommerce-client")
            .clientSecret("{noop}secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:8080/login/oauth2/code/ecommerce")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope("read")
            .scope("write")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .build())
            .build();

        return new InMemoryRegisteredClientRepository(registeredClient);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }
}
```

## Summary

**For Development:** Use existing JWT implementation  
**For Production:** Use Keycloak (Option 1) - Industry standard, feature-rich  
**For Custom Needs:** Spring Authorization Server (Option 2) - More control

**Current Status:** ✅ JWT authentication fully functional  
**OAuth2/Keycloak:** Configuration examples provided, ready to implement when needed
