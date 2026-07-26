-- Add new enum types
ALTER TYPE gender ADD VALUE IF NOT EXISTS 'OTHER';
ALTER TYPE gender ADD VALUE IF NOT EXISTS 'PREFER_NOT_TO_SAY';

CREATE TYPE user_role AS ENUM('CUSTOMER', 'ADMIN', 'MANAGER');
CREATE TYPE account_status AS ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION');

-- Add new columns to users table
ALTER TABLE users 
ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT '',
ADD COLUMN first_name VARCHAR(100),
ADD COLUMN last_name VARCHAR(100),
ADD COLUMN role user_role NOT NULL DEFAULT 'CUSTOMER',
ADD COLUMN status account_status NOT NULL DEFAULT 'ACTIVE',
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Rename existing columns
ALTER TABLE users RENAME COLUMN name TO temp_name;
ALTER TABLE users RENAME COLUMN dob TO birth_date;

-- Update first_name and last_name from existing name field
UPDATE users SET first_name = temp_name WHERE temp_name IS NOT NULL;

-- Drop the temporary name column
ALTER TABLE users DROP COLUMN temp_name;

-- Remove the default from password column now that we've added it
ALTER TABLE users ALTER COLUMN password DROP DEFAULT;