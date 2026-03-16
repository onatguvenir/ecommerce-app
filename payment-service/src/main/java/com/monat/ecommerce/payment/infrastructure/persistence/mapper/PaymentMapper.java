package com.monat.ecommerce.payment.infrastructure.persistence.mapper;

import com.monat.ecommerce.payment.domain.model.Payment;
import com.monat.ecommerce.payment.domain.model.PaymentOutboxEvent;
import com.monat.ecommerce.payment.infrastructure.persistence.entity.PaymentEntity;
import com.monat.ecommerce.payment.infrastructure.persistence.entity.PaymentOutboxEventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

    Payment toDomain(PaymentEntity entity);

    PaymentEntity toEntity(Payment domain);

    PaymentOutboxEvent toOutboxDomain(PaymentOutboxEventEntity entity);

    PaymentOutboxEventEntity toOutboxEntity(PaymentOutboxEvent domain);
}
