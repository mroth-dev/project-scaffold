-- V1_6 seeded product image URLs on example.com, which never resolve.
-- Replace with real, verified stock photography so the storefront has
-- working imagery. These stand in for real product photography and
-- should be replaced with genuine shoots when available.
UPDATE product_images
SET url = 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=1200&q=80&auto=format&fit=crop',
    thumbnail_url = 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=400&q=80&auto=format&fit=crop'
WHERE id = 1;

UPDATE product_images
SET url = 'https://images.unsplash.com/photo-1620799139507-2a76f79a2f4d?w=1200&q=80&auto=format&fit=crop',
    thumbnail_url = 'https://images.unsplash.com/photo-1620799139507-2a76f79a2f4d?w=400&q=80&auto=format&fit=crop'
WHERE id = 2;

UPDATE product_images
SET url = 'https://images.unsplash.com/photo-1588260663276-cb3abbb60f96?w=1200&q=80&auto=format&fit=crop',
    thumbnail_url = 'https://images.unsplash.com/photo-1588260663276-cb3abbb60f96?w=400&q=80&auto=format&fit=crop'
WHERE id = 3;

UPDATE product_images
SET url = 'https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=1200&q=80&auto=format&fit=crop',
    thumbnail_url = 'https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=400&q=80&auto=format&fit=crop'
WHERE id = 4;
