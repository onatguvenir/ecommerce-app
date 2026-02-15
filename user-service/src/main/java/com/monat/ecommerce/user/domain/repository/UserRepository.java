package com.monat.ecommerce.user.domain.repository;

import com.monat.ecommerce.user.domain.model.User;
import com.monat.ecommerce.user.domain.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User domain model
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findAll(Pageable pageable);

    Optional<User> findActiveUserByEmail(String email);

    Optional<User> findByIdWithAddresses(UUID id);

    void deleteById(UUID id);

    void deleteAll();

    long count();
}
