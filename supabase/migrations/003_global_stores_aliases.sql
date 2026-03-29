-- =============================================================================
-- CosteoApp Migration 003 — Global Stores + User Aliases
-- =============================================================================
-- Adds global_stores catalog, user_store_aliases, renames user_products
-- to user_product_aliases, adds store_id FK to product_prices,
-- drops price_cache.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. global_stores — shared store catalog
-- ---------------------------------------------------------------------------
CREATE TABLE global_stores (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre          TEXT UNIQUE NOT NULL,
    tipo            TEXT,
    logo_url        TEXT,
    website_url     TEXT,
    pais            TEXT DEFAULT 'SV',
    activo          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE global_stores ENABLE ROW LEVEL SECURITY;

CREATE POLICY global_stores_select
    ON global_stores FOR SELECT
    USING (auth.role() = 'authenticated');

CREATE POLICY global_stores_insert
    ON global_stores FOR INSERT
    WITH CHECK (auth.jwt() ->> 'role' = 'service_role');

CREATE POLICY global_stores_update
    ON global_stores FOR UPDATE
    USING (auth.jwt() ->> 'role' = 'service_role');

CREATE POLICY global_stores_delete
    ON global_stores FOR DELETE
    USING (auth.jwt() ->> 'role' = 'service_role');

CREATE INDEX idx_global_stores_nombre ON global_stores (nombre);
CREATE INDEX idx_global_stores_tipo ON global_stores (tipo);
CREATE INDEX idx_global_stores_pais ON global_stores (pais);

CREATE TRIGGER trg_global_stores_updated_at
    BEFORE UPDATE ON global_stores
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 2. Seed known stores
-- ---------------------------------------------------------------------------
INSERT INTO global_stores (nombre, tipo, website_url) VALUES
    ('Walmart SV', 'supermercado', 'https://www.walmart.com.sv'),
    ('PriceSmart', 'mayorista', 'https://www.pricesmart.com'),
    ('Super Selectos', 'supermercado', 'https://www.superselectos.com');

-- ---------------------------------------------------------------------------
-- 3. user_store_aliases — user customization for stores
-- ---------------------------------------------------------------------------
CREATE TABLE user_store_aliases (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    store_id        UUID NOT NULL REFERENCES global_stores(id),
    alias           TEXT,
    is_favorite     BOOLEAN DEFAULT FALSE,
    activo          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, store_id)
);

ALTER TABLE user_store_aliases ENABLE ROW LEVEL SECURITY;

CREATE POLICY user_store_aliases_select
    ON user_store_aliases FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY user_store_aliases_insert
    ON user_store_aliases FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY user_store_aliases_update
    ON user_store_aliases FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY user_store_aliases_delete
    ON user_store_aliases FOR DELETE
    USING (auth.uid() = user_id);

CREATE INDEX idx_user_store_aliases_user_id ON user_store_aliases (user_id);
CREATE INDEX idx_user_store_aliases_store_id ON user_store_aliases (store_id);
CREATE INDEX idx_user_store_aliases_user_active ON user_store_aliases (user_id) WHERE activo = TRUE;

CREATE TRIGGER trg_user_store_aliases_updated_at
    BEFORE UPDATE ON user_store_aliases
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 4. Rename user_products -> user_product_aliases + add alias column
-- ---------------------------------------------------------------------------
ALTER TABLE user_products RENAME TO user_product_aliases;

ALTER TABLE user_product_aliases ADD COLUMN alias TEXT;

-- Rename RLS policies to match new table name
ALTER POLICY user_products_select ON user_product_aliases RENAME TO user_product_aliases_select;
ALTER POLICY user_products_insert ON user_product_aliases RENAME TO user_product_aliases_insert;
ALTER POLICY user_products_update ON user_product_aliases RENAME TO user_product_aliases_update;
ALTER POLICY user_products_delete ON user_product_aliases RENAME TO user_product_aliases_delete;

-- Rename indexes to match new table name
ALTER INDEX idx_user_products_user_id RENAME TO idx_user_product_aliases_user_id;
ALTER INDEX idx_user_products_product_id RENAME TO idx_user_product_aliases_product_id;

-- ---------------------------------------------------------------------------
-- 5. Drop price_cache (replaced by product_prices with fetch_url)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS price_cache CASCADE;

-- ---------------------------------------------------------------------------
-- 6. Add store_id FK to product_prices referencing global_stores
-- ---------------------------------------------------------------------------
ALTER TABLE product_prices ADD COLUMN store_id UUID REFERENCES global_stores(id);

CREATE INDEX idx_product_prices_store_id ON product_prices (store_id);

-- Backfill store_id from store_name where matches exist
UPDATE product_prices
SET store_id = gs.id
FROM global_stores gs
WHERE product_prices.store_name = gs.nombre;
