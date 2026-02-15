package com.monat.ecommerce.user.infrastructure.bootstrap;

import com.monat.ecommerce.user.domain.model.AddressType;
import com.monat.ecommerce.user.domain.model.User;
import com.monat.ecommerce.user.domain.model.UserAddress;
import com.monat.ecommerce.user.domain.model.UserStatus;
import com.monat.ecommerce.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * Seeds the database with dummy users for testing purposes.
 * Only runs when 'docker' profile is active and database is empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class UserDataSeeder implements CommandLineRunner {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                if (userRepository.count() > 0) {
                        log.info("Users already exist. Skipping seeding.");
                        return;
                }

                log.info("Seeding users...");

                // Admin User
                User admin = User.builder()
                                .email("admin@monat.com")
                                .username("admin")
                                .passwordHash(passwordEncoder.encode("password"))
                                .firstName("Admin")
                                .lastName("User")
                                .phone("555-000-0001")
                                .status(UserStatus.ACTIVE)
                                .createdAt(LocalDateTime.now())
                                .build();

                userRepository.save(admin);

                // Customer 1
                User user1 = User.builder()
                                .email("john.doe@monat.com")
                                .username("johndoe")
                                .passwordHash(passwordEncoder.encode("password"))
                                .firstName("John")
                                .lastName("Doe")
                                .phone("555-000-0002")
                                .status(UserStatus.ACTIVE)
                                .createdAt(LocalDateTime.now())
                                .build();

                UserAddress address1 = UserAddress.builder()
                                .addressType(AddressType.SHIPPING)
                                .street("123 Main St")
                                .city("New York")
                                .state("NY")
                                .country("USA")
                                .postalCode("10001")
                                .isDefault(true)
                                .build();

                user1.addAddress(address1);
                userRepository.save(user1);

                // Customer 2
                User user2 = User.builder()
                                .email("jane.smith@monat.com")
                                .username("janesmith")
                                .passwordHash(passwordEncoder.encode("password"))
                                .firstName("Jane")
                                .lastName("Smith")
                                .phone("555-000-0003")
                                .status(UserStatus.ACTIVE)
                                .createdAt(LocalDateTime.now())
                                .build();

                UserAddress address2 = UserAddress.builder()
                                .addressType(AddressType.SHIPPING)
                                .street("456 Oak Ave")
                                .city("Los Angeles")
                                .state("CA")
                                .country("USA")
                                .postalCode("90001")
                                .isDefault(true)
                                .build();

                user2.addAddress(address2);
                userRepository.save(user2);

                log.info("Seeding users completed. Created {} users.", userRepository.count());
        }
}
