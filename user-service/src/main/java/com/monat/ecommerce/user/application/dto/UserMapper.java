package com.monat.ecommerce.user.application.dto;

import com.monat.ecommerce.user.domain.model.AddressType;
import com.monat.ecommerce.user.domain.model.User;
import com.monat.ecommerce.user.domain.model.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * MapStruct mapper for User and Address entities
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "addresses", source = "addresses")
    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponseList(List<User> users);

    AddressResponse toAddressResponse(UserAddress address);

    List<AddressResponse> toAddressResponseList(List<UserAddress> addresses);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    UserAddress toUserAddress(CreateAddressRequest request);
}
