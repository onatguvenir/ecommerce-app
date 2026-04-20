-- Expand seed data to cover all 20 test products
INSERT INTO inventory (id, product_id, product_name, available_quantity, reserved_quantity, total_quantity, created_at, version)
VALUES
    (gen_random_uuid(), 'PROD-006', 'Gaming Headset',         150, 0, 150, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-007', 'Webcam HD 1080p',        300, 0, 300, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-008', 'External SSD 1TB',        80, 0,  80, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-009', 'USB Hub 7-Port',         400, 0, 400, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-010', 'Ergonomic Chair',         30, 0,  30, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-011', 'Standing Desk',           20, 0,  20, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-012', 'Desk Lamp LED',          250, 0, 250, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-013', 'Mousepad XL',            600, 0, 600, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-014', 'Laptop Stand',           350, 0, 350, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-015', 'Wireless Keyboard',      200, 0, 200, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-016', 'Noise-Cancelling Headphones', 120, 0, 120, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-017', 'Smart Watch',            100, 0, 100, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-018', 'Portable Charger 20000mAh', 500, 0, 500, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-019', 'HDMI Cable 2m',          800, 0, 800, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'PROD-020', 'Screen Privacy Filter',  150, 0, 150, CURRENT_TIMESTAMP, 0);
