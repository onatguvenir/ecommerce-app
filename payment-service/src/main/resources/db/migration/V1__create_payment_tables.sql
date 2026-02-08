-- V1__create_payment_tables.sql
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    order_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    payment_reference VARCHAR(255) UNIQUE,
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    failure_reason VARCHAR(500),
    refund_reference VARCHAR(255),
    refunded_amount DECIMAL(15, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_idempotency_key ON payments(idempotency_key);
CREATE INDEX idx_order_id ON payments(order_id);
CREATE INDEX idx_user_id ON payments(user_id);
CREATE INDEX idx_payment_reference ON payments(payment_reference);
CREATE INDEX idx_status ON payments(status);
CREATE INDEX idx_created_at ON payments(created_at);