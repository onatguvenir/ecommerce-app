-- V1__create_inventory_tables.sql
CREATE TABLE IF NOT EXISTS inventory (
    id UUID PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL UNIQUE,
    product_name VARCHAR(255),
    available_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    total_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_product_id ON inventory(product_id);
CREATE INDEX idx_available_qty ON inventory(available_quantity);
CREATE TABLE IF NOT EXISTS stock_reservations (
    id UUID PRIMARY KEY,
    reservation_id VARCHAR(255) NOT NULL UNIQUE,
    order_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_reservation_id ON stock_reservations(reservation_id);
CREATE INDEX idx_order_id ON stock_reservations(order_id);
CREATE INDEX idx_status ON stock_reservations(status);
CREATE INDEX idx_expires_at ON stock_reservations(expires_at);
-- Insert some test inventory data
INSERT INTO inventory (
        id,
        product_id,
        product_name,
        available_quantity,
        reserved_quantity,
        total_quantity,
        created_at,
        version
    )
VALUES (
        gen_random_uuid(),
        'PROD-001',
        'Laptop Pro 15',
        100,
        0,
        100,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        gen_random_uuid(),
        'PROD-002',
        'Wireless Mouse',
        500,
        0,
        500,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        gen_random_uuid(),
        'PROD-003',
        'USB-C Cable',
        1000,
        0,
        1000,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        gen_random_uuid(),
        'PROD-004',
        'Monitor 27"',
        50,
        0,
        50,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        gen_random_uuid(),
        'PROD-005',
        'Keyboard Mechanical',
        200,
        0,
        200,
        CURRENT_TIMESTAMP,
        0
    );