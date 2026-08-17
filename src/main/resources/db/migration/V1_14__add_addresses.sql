ALTER TABLE users
    ADD COLUMN address_line1 VARCHAR(255),
    ADD COLUMN address_line2 VARCHAR(255),
    ADD COLUMN address_city VARCHAR(120),
    ADD COLUMN address_region VARCHAR(120),
    ADD COLUMN address_postcode VARCHAR(20),
    ADD COLUMN address_country VARCHAR(120),
    ADD COLUMN address_phone VARCHAR(30);

ALTER TABLE orders
    ADD COLUMN shipping_line1 VARCHAR(255),
    ADD COLUMN shipping_line2 VARCHAR(255),
    ADD COLUMN shipping_city VARCHAR(120),
    ADD COLUMN shipping_region VARCHAR(120),
    ADD COLUMN shipping_postcode VARCHAR(20),
    ADD COLUMN shipping_country VARCHAR(120),
    ADD COLUMN shipping_phone VARCHAR(30);
