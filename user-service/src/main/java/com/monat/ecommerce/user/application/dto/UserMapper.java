package com.monat.ecommerce.user.application.dto;

import com.monat.ecommerce.user.domain.model.User;
import com.monat.ecommerce.user.domain.model.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for User and Address entities
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true) // Set manually after encoding
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    User toUser(UserRegistrationRequest request);

    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "addresses", source = "addresses")
    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponseList(List<User> users);

    AddressResponse toAddressResponse(UserAddress address);

    List<AddressResponse> toAddressResponseList(List<UserAddress> addresses);

    @Mapping(target = "id", ignore = true)
    UserAddress toUserAddress(CreateAddressRequest request);
}
