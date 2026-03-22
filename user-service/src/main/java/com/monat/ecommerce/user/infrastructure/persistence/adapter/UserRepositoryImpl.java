package com.monat.ecommerce.user.infrastructure.persistence.adapter;

import com.monat.ecommerce.user.domain.model.User;
import com.monat.ecommerce.user.domain.model.UserStatus;
import com.monat.ecommerce.user.domain.repository.UserRepository;
import com.monat.ecommerce.user.infrastructure.persistence.entity.UserEntity;
import com.monat.ecommerce.user.infrastructure.persistence.mapper.UserMapper;
import com.monat.ecommerce.user.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);

        // Ensure bidirectional relationship for addresses before saving
        if (entity.getAddresses() != null) {
            entity.getAddresses().forEach(addr -> addr.setUser(entity));
        }

        UserEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public Page<User> findByStatus(UserStatus status, Pageable pageable) {
        return jpaRepository.findByStatus(status, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findActiveUserByEmail(String email) {
        return jpaRepository.findActiveUserByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByIdWithAddresses(UUID id) {
        return jpaRepository.findByIdWithAddresses(id).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    /**
     * Suspends a user account using Pessimistic Write Lock.
     * 
     * Theory (Educational Note):
     * A Pessimistic Write Lock is used here to enforce strict serialization
     * when multiple threads might try to suspend the same user simultaneously
     * (e.g., triggered by concurrent Kafka consumer threads processing the same
     * fraudulent user). Without this lock, a "lost update" anomaly could occur:
     * one thread reads ACTIVE status, another reads ACTIVE status, both try to
     * write SUSPENDED → both writes succeed, but only one update is retained
     * effectively. PESSIMISTIC_WRITE prevents this race condition by blocking
     * concurrent reads until the lock holder commits.
     */
    @Override
    @Transactional
    public void suspendUserById(UUID userId, String reason) {
        jpaRepository.findByIdWithLock(userId).ifPresentOrElse(
                entity -> {
                    if (entity.getStatus() == UserStatus.SUSPENDED) {
                        log.info("User {} is already SUSPENDED. Skipping.", userId);
                        return;
                    }
                    entity.setStatus(UserStatus.SUSPENDED);
                    jpaRepository.save(entity);
                    log.warn("User {} has been SUSPENDED. Reason: {}", userId, reason);
                },
                () -> log.error("Cannot suspend user {}: not found.", userId)
        );
    }
}
