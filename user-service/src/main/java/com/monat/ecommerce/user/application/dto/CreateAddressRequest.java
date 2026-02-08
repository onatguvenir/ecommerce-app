package com.monat.ecommerce.user.application.dto;

import com.monat.ecommerce.user.domain.model.AddressType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating address
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAddressRequest {
    
    private AddressType addressType;
    
    @NotBlank(message = "Street is required")
    private String street;
    
    @NotBlank(message = "City is required")
    private String city;
    
    private String state;
    
    private String postalCode;
    
    @NotBlank(message = "Country is required")
    private String country;
    
    private Boolean isDefault;
}
