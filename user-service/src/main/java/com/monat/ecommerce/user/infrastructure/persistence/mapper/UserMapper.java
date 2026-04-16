package com.monat.ecommerce.user.infrastructure.persistence.mapper;

import com.monat.ecommerce.user.domain.model.User;
import com.monat.ecommerce.user.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = UserAddressMapper.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        implementationName = "UserPersistenceMapperImpl"
)
public interface UserMapper {

    User toDomain(UserEntity entity);

    UserEntity toEntity(User domain);
}
