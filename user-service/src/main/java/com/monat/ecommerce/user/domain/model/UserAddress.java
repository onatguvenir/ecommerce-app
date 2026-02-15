package com.monat.ecommerce.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * User address domain model - Pure POJO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {

    private UUID id;
    private AddressType addressType;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    @Builder.Default
    private Boolean isDefault = false;
}
