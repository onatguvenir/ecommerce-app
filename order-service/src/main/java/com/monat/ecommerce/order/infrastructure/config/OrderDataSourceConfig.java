package com.monat.ecommerce.order.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(OrderDataSourceProperties.class)
public class OrderDataSourceConfig {

    private static final String DEFAULT_PRIMARY_POOL_NAME = "order-primary-pool";
    private static final String DEFAULT_REPLICA_POOL_NAME = "order-replica-pool";

    @Bean
    @Primary
    public DataSource dataSource(OrderDataSourceProperties properties) {
        return createDataSource(properties.getPrimary(), DEFAULT_PRIMARY_POOL_NAME);
    }

    @Bean(name = "readReplicaDataSource")
    public DataSource readReplicaDataSource(OrderDataSourceProperties properties) {
        OrderDataSourceProperties.DataSourceConfig primary = properties.getPrimary();
        OrderDataSourceProperties.DataSourceConfig replica = properties.getReplica();
        OrderDataSourceProperties.DataSourceConfig resolvedReplica = new OrderDataSourceProperties.DataSourceConfig();
        resolvedReplica.setUrl(StringUtils.hasText(replica.getUrl()) ? replica.getUrl() : primary.getUrl());
        resolvedReplica.setUsername(StringUtils.hasText(replica.getUsername()) ? replica.getUsername() : primary.getUsername());
        resolvedReplica.setPassword(StringUtils.hasText(replica.getPassword()) ? replica.getPassword() : primary.getPassword());
        resolvedReplica.setDriverClassName(
                StringUtils.hasText(replica.getDriverClassName()) ? replica.getDriverClassName() : primary.getDriverClassName());
        resolvedReplica.setMaximumPoolSize(replica.getMaximumPoolSize());
        resolvedReplica.setMinimumIdle(replica.getMinimumIdle());
        resolvedReplica.setPoolName(StringUtils.hasText(replica.getPoolName()) ? replica.getPoolName() : DEFAULT_REPLICA_POOL_NAME);
        return createDataSource(resolvedReplica, DEFAULT_REPLICA_POOL_NAME);
    }

    @Bean(name = "primaryJdbcTemplate")
    public JdbcTemplate primaryJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "replicaJdbcTemplate")
    public JdbcTemplate replicaJdbcTemplate(@Qualifier("readReplicaDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "replicaNamedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate replicaNamedParameterJdbcTemplate(
            @Qualifier("readReplicaDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    private DataSource createDataSource(
            OrderDataSourceProperties.DataSourceConfig properties,
            String defaultPoolName
    ) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setMaximumPoolSize(properties.getMaximumPoolSize());
        dataSource.setMinimumIdle(properties.getMinimumIdle());
        dataSource.setPoolName(StringUtils.hasText(properties.getPoolName()) ? properties.getPoolName() : defaultPoolName);
        return dataSource;
    }
}
