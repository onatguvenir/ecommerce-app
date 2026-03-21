package com.monat.ecommerce.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User Domain Model.
 * 
 * Educational Note:
 * This is a 'Pure' domain model (POJO). In Domain-Driven Design (DDD), 
 * the domain model should be free from infrastructure concerns (like JPA annotations) 
 * as much as possible to preserve its business meaning.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private UUID id;
    private String email;
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String phone;

    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    private List<UserAddress> addresses = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long version;

    // Helper methods
    public void addAddress(UserAddress address) {
        addresses.add(address);
    }

    public void removeAddress(UserAddress address) {
        addresses.remove(address);
    }
}
