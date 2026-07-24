-- Categories
INSERT INTO categories (id, name, slug, description, parent_id, sort_order) VALUES
    (1, 'Clothing', 'clothing', 'Apparel for everyday wear', NULL, 0),
    (2, 'Accessories', 'accessories', 'Bags, belts and other accessories', NULL, 1),
    (3, 'Shirts', 'shirts', 'Casual and formal shirts', 1, 0),
    (4, 'Trousers', 'trousers', 'Trousers and jeans', 1, 1);

-- Products
INSERT INTO products (id, name, description, sku, rrp, base_price, is_active) VALUES
    (1, 'Classic Crew T-Shirt', 'Cotton crew neck t-shirt', 'TSHIRT-001', 29.99, 19.99, TRUE),
    (2, 'Slim Fit Chinos', 'Slim fit cotton chino trousers', 'CHINO-001', 59.99, 39.99, TRUE),
    (3, 'Leather Belt', 'Genuine leather belt', 'BELT-001', 34.99, 24.99, TRUE);

-- Product images
INSERT INTO product_images (id, product_id, url, thumbnail_url, alt_text, sort_order) VALUES
    (1, 1, 'https://example.com/images/tshirt-001-front.jpg', 'https://example.com/images/tshirt-001-front-thumb.jpg', 'Classic Crew T-Shirt front view', 0),
    (2, 1, 'https://example.com/images/tshirt-001-back.jpg', 'https://example.com/images/tshirt-001-back-thumb.jpg', 'Classic Crew T-Shirt back view', 1),
    (3, 2, 'https://example.com/images/chino-001-front.jpg', 'https://example.com/images/chino-001-front-thumb.jpg', 'Slim Fit Chinos front view', 0),
    (4, 3, 'https://example.com/images/belt-001.jpg', 'https://example.com/images/belt-001-thumb.jpg', 'Leather Belt', 0);

-- Product variations
INSERT INTO product_variations (id, product_id, size, color, sku, inventory_count, price_adjustment) VALUES
    (1, 1, 'S', 'White', 'TSHIRT-001-S-WHITE', 25, 0),
    (2, 1, 'M', 'White', 'TSHIRT-001-M-WHITE', 40, 0),
    (3, 1, 'L', 'White', 'TSHIRT-001-L-WHITE', 30, 0),
    (4, 1, 'M', 'Black', 'TSHIRT-001-M-BLACK', 35, 0),
    (5, 2, '30', 'Khaki', 'CHINO-001-30-KHAKI', 15, 0),
    (6, 2, '32', 'Khaki', 'CHINO-001-32-KHAKI', 20, 0),
    (7, 2, '34', 'Navy', 'CHINO-001-34-NAVY', 18, 5.00),
    (8, 3, 'One Size', 'Brown', 'BELT-001-BROWN', 50, 0);

-- Product-category associations
INSERT INTO product_categories (product_id, category_id) VALUES
    (1, 1),
    (1, 3),
    (2, 1),
    (2, 4),
    (3, 2);
