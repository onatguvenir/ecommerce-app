package com.monat.ecommerce.user.infrastructure.grpc;

import com.monat.ecommerce.grpc.user.*;
import com.monat.ecommerce.user.domain.model.User;
import com.monat.ecommerce.user.domain.model.UserAddress;
import com.monat.ecommerce.user.domain.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * gRPC service implementation for User Service
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        log.debug("gRPC GetUser called for userId: {}", request.getUserId());

        try {
            UUID userId = UUID.fromString(request.getUserId());
            User user = userRepository.findById(userId).orElse(null);

            GetUserResponse.Builder responseBuilder = GetUserResponse.newBuilder();

            if (user != null) {
                com.monat.ecommerce.grpc.user.User grpcUser = mapToGrpcUser(user);
                responseBuilder.setUser(grpcUser).setFound(true);
            } else {
                responseBuilder.setFound(false);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in gRPC GetUser", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void validateUser(ValidateUserRequest request, StreamObserver<ValidateUserResponse> responseObserver) {
        log.debug("gRPC ValidateUser called for userId: {}", request.getUserId());

        try {
            UUID userId = UUID.fromString(request.getUserId());
            User user = userRepository.findById(userId).orElse(null);

            ValidateUserResponse.Builder responseBuilder = ValidateUserResponse.newBuilder();

            if (user != null) {
                boolean isActive = user.getStatus().name().equals("ACTIVE");
                responseBuilder
                        .setIsValid(true)
                        .setIsActive(isActive)
                        .setMessage(isActive ? "User is active" : "User is not active");
            } else {
                responseBuilder
                        .setIsValid(false)
                        .setIsActive(false)
                        .setMessage("User not found");
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in gRPC ValidateUser", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getUserAddresses(GetUserAddressesRequest request, StreamObserver<GetUserAddressesResponse> responseObserver) {
        log.debug("gRPC GetUserAddresses called for userId: {}", request.getUserId());

        try {
            UUID userId = UUID.fromString(request.getUserId());
            User user = userRepository.findByIdWithAddresses(userId).orElse(null);

            GetUserAddressesResponse.Builder responseBuilder = GetUserAddressesResponse.newBuilder();

            if (user != null && user.getAddresses() != null) {
                List<Address> addresses = user.getAddresses().stream()
                        .map(this::mapToGrpcAddress)
                        .collect(Collectors.toList());
                responseBuilder.addAllAddresses(addresses);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in gRPC GetUserAddresses", e);
            responseObserver.onError(e);
        }
    }

    private com.monat.ecommerce.grpc.user.User mapToGrpcUser(User user) {
        return com.monat.ecommerce.grpc.user.User.newBuilder()
                .setId(user.getId().toString())
                .setEmail(user.getEmail())
                .setUsername(user.getUsername())
                .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                .setLastName(user.getLastName() != null ? user.getLastName() : "")
                .setPhone(user.getPhone() != null ? user.getPhone() : "")
                .setStatus(user.getStatus().name())
                .build();
    }

    private Address mapToGrpcAddress(UserAddress address) {
        return Address.newBuilder()
                .setId(address.getId().toString())
                .setAddressType(address.getAddressType() != null ? address.getAddressType().name() : "")
                .setStreet(address.getStreet())
                .setCity(address.getCity())
                .setState(address.getState() != null ? address.getState() : "")
                .setPostalCode(address.getPostalCode() != null ? address.getPostalCode() : "")
                .setCountry(address.getCountry())
                .setIsDefault(address.getIsDefault() != null ? address.getIsDefault() : false)
                .build();
    }
}
