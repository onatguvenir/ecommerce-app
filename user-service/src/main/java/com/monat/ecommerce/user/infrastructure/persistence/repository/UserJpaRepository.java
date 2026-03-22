package com.monat.ecommerce.user.infrastructure.persistence.repository;

import com.monat.ecommerce.user.domain.model.UserStatus;
import com.monat.ecommerce.user.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * User JPA Repository.
 * <p>
 * This interface provides data access operations for the UserEntity.
 * </p>
 * 
 * @Repository is a Spring annotation that indicates that the decorated class is
 *             a repository.
 *             It also enables exception translation from persistence exceptions
 *             to Spring's DataAccessException hierarchy.
 * 
 *             Extends JpaRepository<UserEntity, UUID>:
 *             - Inherits standard CRUD operations (save, findById, delete,
 *             etc.)
 *             - Allows defining custom query methods by simply declaring method
 *             signatures (e.g., findByEmail).
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Page<UserEntity> findByStatus(UserStatus status, Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<UserEntity> findActiveUserByEmail(@Param("email") String email);

    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserEntity> findByIdWithLock(@Param("id") UUID id);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.addresses WHERE u.id = :id")
    Optional<UserEntity> findByIdWithAddresses(@Param("id") UUID id);
}
