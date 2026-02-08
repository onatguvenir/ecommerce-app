#!/bin/bash
set -e

# Create multiple PostgreSQL databases
# This script is executed when the PostgreSQL container first starts

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE userdb;
    CREATE DATABASE orderdb;
    CREATE DATABASE inventorydb;
    CREATE DATABASE paymentdb;
    
    GRANT ALL PRIVILEGES ON DATABASE userdb TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE orderdb TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE inventorydb TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE paymentdb TO $POSTGRES_USER;
    
    -- Optional: Create separate users for each service (recommended for production)
    -- CREATE USER user_service WITH PASSWORD 'user_pass';
    -- CREATE USER order_service WITH PASSWORD 'order_pass';
    -- CREATE USER inventory_service WITH PASSWORD 'inventory_pass';
    -- CREATE USER payment_service WITH PASSWORD 'payment_pass';
    
    -- GRANT ALL PRIVILEGES ON DATABASE userdb TO user_service;
    -- GRANT ALL PRIVILEGES ON DATABASE orderdb TO order_service;
    -- GRANT ALL PRIVILEGES ON DATABASE inventorydb TO inventory_service;
    -- GRANT ALL PRIVILEGES ON DATABASE paymentdb TO payment_service;
EOSQL

echo "Multiple databases created successfully!"
