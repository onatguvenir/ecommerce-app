package com.monat.ecommerce.payment.application.mapper;

import com.monat.ecommerce.payment.application.dto.PaymentResponse;
import com.monat.ecommerce.payment.domain.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentDtoMapper {

    PaymentResponse toResponse(Payment payment);
}
