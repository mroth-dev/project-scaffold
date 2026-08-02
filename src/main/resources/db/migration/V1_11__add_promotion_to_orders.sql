ALTER TABLE orders ADD COLUMN promotion_id BIGINT REFERENCES promotions(id);
ALTER TABLE orders ADD COLUMN discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;
