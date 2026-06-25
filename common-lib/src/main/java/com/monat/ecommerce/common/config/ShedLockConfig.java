package com.monat.ecommerce.common.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * Configuration for ShedLock to handle distributed scheduling.
 * Periodically synchronized task execution across multiple service instances.
 *
 * <p>The {@link LockProvider} resolves its {@link DataSource} lazily via
 * {@link ObjectProvider}. This makes bean creation deterministic: the {@code DataSource}
 * is looked up at instantiation time — after Spring Boot's
 * {@code DataSourceAutoConfiguration} has registered it — instead of relying on
 * {@code @ConditionalOnBean(DataSource.class)}, which in a component-scanned user
 * {@code @Configuration} is evaluated during scanning (before the auto-configured
 * {@code DataSource} exists) and is therefore order-dependent. That fragility silently
 * produced <em>no</em> {@code LockProvider} in services that rely on the auto-configured
 * {@code DataSource} (e.g. payment-service), breaking their {@code @SchedulerLock} pollers
 * with {@code NoSuchBeanDefinitionException}; services with an explicit early
 * {@code DataSource} bean (order-service) happened to pass the condition by luck.
 *
 * <p>Services with a relational {@code DataSource} (order, payment, inventory) get a real
 * JDBC-backed provider. Services without one (e.g. cart-service, Redis-only) get a no-op
 * provider; they declare no {@code @SchedulerLock} methods, so it is never exercised.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(ObjectProvider<DataSource> dataSourceProvider) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return lockConfiguration -> Optional.of(() -> { });
        }
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime() // Works with PostgreSQL
                        .build()
        );
    }
}
