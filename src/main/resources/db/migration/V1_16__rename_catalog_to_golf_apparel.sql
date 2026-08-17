-- Brand pivots to modern performance golf apparel (see PRODUCT.md).
-- Rename the placeholder seed catalog to golf-plausible demo SKUs so the
-- storefront doesn't present generic basics under a golf identity. Still
-- placeholder/demo content -- not real inventory, pricing, or photography.

UPDATE categories
SET name = 'Polos', slug = 'polos', description = 'Performance polos, short and long sleeve'
WHERE id = 3;

UPDATE categories
SET description = 'Golf trousers and shorts built to swing in'
WHERE id = 4;

UPDATE products
SET name = 'Momentum Performance Polo',
    description = 'Four-way stretch polo with a moisture-wicking finish, built for eighteen holes.',
    sku = 'POLO-001',
    rrp = 84.99,
    base_price = 69.99
WHERE id = 1;

UPDATE products
SET name = 'Fairway Stretch Trouser',
    description = 'Tapered stretch trouser with a quiet, technical hand -- fairway to clubhouse.',
    sku = 'TROUSER-001',
    rrp = 99.99,
    base_price = 79.99
WHERE id = 2;

UPDATE products
SET name = 'Full-Grain Leather Belt',
    description = 'Full-grain leather belt with a matte buckle, fairway to clubhouse.'
WHERE id = 3;

UPDATE product_variations SET sku = 'POLO-001-S-WHITE' WHERE id = 1;
UPDATE product_variations SET sku = 'POLO-001-M-WHITE' WHERE id = 2;
UPDATE product_variations SET sku = 'POLO-001-L-WHITE' WHERE id = 3;
UPDATE product_variations SET sku = 'POLO-001-M-BLACK' WHERE id = 4;
UPDATE product_variations SET sku = 'TROUSER-001-30-KHAKI' WHERE id = 5;
UPDATE product_variations SET sku = 'TROUSER-001-32-KHAKI' WHERE id = 6;
UPDATE product_variations SET sku = 'TROUSER-001-34-NAVY' WHERE id = 7;

UPDATE product_images
SET url = 'https://images.unsplash.com/photo-1720514496161-914011a9ee02?w=1200&q=80&auto=format&fit=crop',
    thumbnail_url = 'https://images.unsplash.com/photo-1720514496161-914011a9ee02?w=400&q=80&auto=format&fit=crop',
    alt_text = 'Momentum Performance Polo, front view'
WHERE id = 1;

UPDATE product_images
SET url = 'https://images.unsplash.com/photo-1714317438040-0e8584215699?w=1200&q=80&auto=format&fit=crop',
    thumbnail_url = 'https://images.unsplash.com/photo-1714317438040-0e8584215699?w=400&q=80&auto=format&fit=crop',
    alt_text = 'Momentum Performance Polo, colourway rack'
WHERE id = 2;

UPDATE product_images
SET alt_text = 'Fairway Stretch Trouser, front view'
WHERE id = 3;

UPDATE product_images
SET alt_text = 'Full-Grain Leather Belt'
WHERE id = 4;
