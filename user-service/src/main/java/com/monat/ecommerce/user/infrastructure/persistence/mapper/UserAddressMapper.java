package com.monat.ecommerce.user.infrastructure.persistence.mapper;

import com.monat.ecommerce.user.domain.model.UserAddress;
import com.monat.ecommerce.user.infrastructure.persistence.entity.UserAddressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserAddressMapper {

    UserAddress toDomain(UserAddressEntity entity);

    @Mapping(target = "user", ignore = true)
    UserAddressEntity toEntity(UserAddress domain);
}
