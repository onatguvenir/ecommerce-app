package com.monat.ecommerce.order.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {
    @NotBlank
    private String street;
    @NotBlank
    private String city;
    private String state;
    private String postalCode;
    @NotBlank
    private String country;
}
