CREATE TABLE IF NOT EXISTS orders_read_model (
    id UUID NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    payment_reference VARCHAR(255),
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE TABLE IF NOT EXISTS orders_read_model_default
    PARTITION OF orders_read_model DEFAULT;

CREATE TABLE IF NOT EXISTS orders_read_model_2026
    PARTITION OF orders_read_model
    FOR VALUES FROM ('2026-01-01 00:00:00') TO ('2027-01-01 00:00:00');

CREATE TABLE IF NOT EXISTS orders_read_model_2027
    PARTITION OF orders_read_model
    FOR VALUES FROM ('2027-01-01 00:00:00') TO ('2028-01-01 00:00:00');

CREATE INDEX IF NOT EXISTS idx_orders_read_model_user_created_at ON orders_read_model (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_read_model_status_created_at ON orders_read_model (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_read_model_order_number ON orders_read_model (order_number);

INSERT INTO orders_read_model (
    id,
    order_number,
    user_id,
    status,
    total_amount,
    currency,
    payment_reference,
    cancellation_reason,
    created_at,
    updated_at
)
SELECT
    o.id,
    o.order_number,
    o.user_id,
    o.status,
    o.total_amount,
    o.currency,
    o.payment_reference,
    o.cancellation_reason,
    o.created_at,
    o.updated_at
FROM orders o
ON CONFLICT (id, created_at) DO UPDATE SET
    order_number = EXCLUDED.order_number,
    user_id = EXCLUDED.user_id,
    status = EXCLUDED.status,
    total_amount = EXCLUDED.total_amount,
    currency = EXCLUDED.currency,
    payment_reference = EXCLUDED.payment_reference,
    cancellation_reason = EXCLUDED.cancellation_reason,
    updated_at = EXCLUDED.updated_at;

CREATE OR REPLACE FUNCTION sync_orders_read_model()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM orders_read_model
        WHERE id = OLD.id AND created_at = OLD.created_at;
        RETURN OLD;
    END IF;

    INSERT INTO orders_read_model (
        id,
        order_number,
        user_id,
        status,
        total_amount,
        currency,
        payment_reference,
        cancellation_reason,
        created_at,
        updated_at
    )
    VALUES (
        NEW.id,
        NEW.order_number,
        NEW.user_id,
        NEW.status,
        NEW.total_amount,
        NEW.currency,
        NEW.payment_reference,
        NEW.cancellation_reason,
        NEW.created_at,
        NEW.updated_at
    )
    ON CONFLICT (id, created_at) DO UPDATE SET
        order_number = EXCLUDED.order_number,
        user_id = EXCLUDED.user_id,
        status = EXCLUDED.status,
        total_amount = EXCLUDED.total_amount,
        currency = EXCLUDED.currency,
        payment_reference = EXCLUDED.payment_reference,
        cancellation_reason = EXCLUDED.cancellation_reason,
        updated_at = EXCLUDED.updated_at;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_orders_read_model ON orders;
CREATE TRIGGER trg_sync_orders_read_model
AFTER INSERT OR UPDATE OR DELETE ON orders
FOR EACH ROW
EXECUTE FUNCTION sync_orders_read_model();

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_daily_sales_report AS
SELECT
    DATE(created_at) AS sales_date,
    status,
    currency,
    COUNT(*) AS order_count,
    COUNT(DISTINCT user_id) AS unique_customers,
    COALESCE(SUM(total_amount), 0) AS total_sales,
    COALESCE(AVG(total_amount), 0) AS average_order_value
FROM orders_read_model
GROUP BY DATE(created_at), status, currency;

CREATE UNIQUE INDEX IF NOT EXISTS ux_mv_daily_sales_report
    ON mv_daily_sales_report (sales_date, status, currency);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_order_status_distribution AS
WITH status_totals AS (
    SELECT
        status,
        COUNT(*) AS order_count,
        COALESCE(SUM(total_amount), 0) AS total_sales
    FROM orders_read_model
    GROUP BY status
),
all_orders AS (
    SELECT COUNT(*)::DECIMAL AS total_count
    FROM orders_read_model
)
SELECT
    st.status,
    st.order_count,
    st.total_sales,
    CASE
        WHEN ao.total_count = 0 THEN 0
        ELSE ROUND((st.order_count::DECIMAL / ao.total_count) * 100, 2)
    END AS share_percentage
FROM status_totals st
CROSS JOIN all_orders ao;

CREATE UNIQUE INDEX IF NOT EXISTS ux_mv_order_status_distribution
    ON mv_order_status_distribution (status);
