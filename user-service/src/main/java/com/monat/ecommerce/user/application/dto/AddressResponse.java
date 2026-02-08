package com.monat.ecommerce.user.application.dto;

import com.monat.ecommerce.user.domain.model.AddressType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for address
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private UUID id;
    private AddressType addressType;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Boolean isDefault;
}
