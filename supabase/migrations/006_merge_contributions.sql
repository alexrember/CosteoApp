ALTER TABLE user_product_aliases ADD COLUMN IF NOT EXISTS contributed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE user_product_aliases ADD COLUMN IF NOT EXISTS contribution_status TEXT DEFAULT NULL;
DROP TABLE IF EXISTS product_contributions CASCADE;
