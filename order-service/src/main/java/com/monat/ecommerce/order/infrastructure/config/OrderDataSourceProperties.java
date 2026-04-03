package com.monat.ecommerce.order.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.datasource")
public class OrderDataSourceProperties {

    private DataSourceConfig primary = new DataSourceConfig();
    private DataSourceConfig replica = new DataSourceConfig();

    public DataSourceConfig getPrimary() {
        return primary;
    }

    public void setPrimary(DataSourceConfig primary) {
        this.primary = primary;
    }

    public DataSourceConfig getReplica() {
        return replica;
    }

    public void setReplica(DataSourceConfig replica) {
        this.replica = replica;
    }

    public static class DataSourceConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";
        private int maximumPoolSize = 10;
        private int minimumIdle = 5;
        private String poolName;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }
    }
}
