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
 * User domain model - Pure POJO
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
