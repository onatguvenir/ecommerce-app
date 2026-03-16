package com.monat.ecommerce.order.application.dto;

import lombok.Builder;

@Builder
public record AddressResponse(
    String street,
    String city,
    String state,
    String postalCode,
    String country
) {}
