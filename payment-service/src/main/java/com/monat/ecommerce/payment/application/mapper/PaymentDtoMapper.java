package com.monat.ecommerce.payment.application.mapper;

import com.monat.ecommerce.payment.domain.model.PaymentMethod;

import com.monat.ecommerce.payment.application.dto.PaymentResponse;
import com.monat.ecommerce.payment.application.dto.ProcessPaymentRequest;
import com.monat.ecommerce.payment.domain.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = PaymentMethod.class)
public interface PaymentDtoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "paymentMethod", expression = "java(PaymentMethod.valueOf(request.paymentMethod()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "paymentReference", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    Payment toPayment(ProcessPaymentRequest request);

    PaymentResponse toResponse(Payment payment);
}
