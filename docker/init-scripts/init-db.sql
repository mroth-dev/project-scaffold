-- Initial database setup for ecommerce application

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set timezone
SET timezone = 'UTC';

-- Create indexes that might be useful
-- These will be created by Flyway migrations, but having them here ensures consistency