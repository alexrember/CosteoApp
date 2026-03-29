-- =============================================================================
-- CosteoApp Migration 002 — Global Products Architecture
-- =============================================================================
-- Replaces per-user shared_products with global product catalog.
-- New tables: global_products, user_products, product_prices
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Drop shared_products (replaced by global_products)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS shared_products CASCADE;

-- ---------------------------------------------------------------------------
-- 2. global_products — one row per unique product (by EAN)
-- ---------------------------------------------------------------------------
CREATE TABLE global_products (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ean                         TEXT UNIQUE NOT NULL,
    nombre                      TEXT NOT NULL,
    marca                       TEXT,
    unidad_medida               TEXT NOT NULL DEFAULT 'unidad',
    cantidad_por_empaque        DOUBLE PRECISION NOT NULL DEFAULT 1,
    unidades_por_empaque        INTEGER NOT NULL DEFAULT 1,
    imagen_url                  TEXT,
    categoria                   TEXT,

    -- Nutrition
    nutricion_porcion_g         DOUBLE PRECISION,
    nutricion_calorias          DOUBLE PRECISION,
    nutricion_proteinas_g       DOUBLE PRECISION,
    nutricion_carbohidratos_g   DOUBLE PRECISION,
    nutricion_grasas_g          DOUBLE PRECISION,
    nutricion_fibra_g           DOUBLE PRECISION,
    nutricion_sodio_mg          DOUBLE PRECISION,
    nutricion_fuente            TEXT,

    -- Metadata
    confirmations               INTEGER NOT NULL DEFAULT 1,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE global_products ENABLE ROW LEVEL SECURITY;

-- Any authenticated user can read
CREATE POLICY global_products_select
    ON global_products FOR SELECT
    USING (auth.role() = 'authenticated');

-- Only service_role can insert/update/delete (Edge Functions)
CREATE POLICY global_products_insert
    ON global_products FOR INSERT
    WITH CHECK (auth.jwt() ->> 'role' = 'service_role');

CREATE POLICY global_products_update
    ON global_products FOR UPDATE
    USING (auth.jwt() ->> 'role' = 'service_role');

CREATE POLICY global_products_delete
    ON global_products FOR DELETE
    USING (auth.jwt() ->> 'role' = 'service_role');

CREATE INDEX idx_global_products_ean ON global_products (ean);
CREATE INDEX idx_global_products_nombre ON global_products USING gin (to_tsvector('spanish', nombre));
CREATE INDEX idx_global_products_categoria ON global_products (categoria);

CREATE TRIGGER trg_global_products_updated_at
    BEFORE UPDATE ON global_products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 3. user_products — links users to global products they use
-- ---------------------------------------------------------------------------
CREATE TABLE user_products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    product_id      UUID NOT NULL REFERENCES global_products(id),
    notas           TEXT,
    factor_merma    INTEGER NOT NULL DEFAULT 0,
    es_servicio     BOOLEAN NOT NULL DEFAULT FALSE,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (user_id, product_id)
);

ALTER TABLE user_products ENABLE ROW LEVEL SECURITY;

-- Users can only see/manage their own associations
CREATE POLICY user_products_select
    ON user_products FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY user_products_insert
    ON user_products FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY user_products_update
    ON user_products FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY user_products_delete
    ON user_products FOR DELETE
    USING (auth.uid() = user_id);

CREATE INDEX idx_user_products_user_id ON user_products (user_id);
CREATE INDEX idx_user_products_product_id ON user_products (product_id);
CREATE INDEX idx_user_products_user_active ON user_products (user_id) WHERE activo = TRUE;

CREATE TRIGGER trg_user_products_updated_at
    BEFORE UPDATE ON user_products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 4. product_prices — latest price per product+store (GLOBAL)
-- ---------------------------------------------------------------------------
CREATE TABLE product_prices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL REFERENCES global_products(id),
    store_name      TEXT NOT NULL,
    price           BIGINT,
    list_price      BIGINT,
    is_available    BOOLEAN DEFAULT TRUE,

    -- URL/params to refresh just this price
    fetch_url       TEXT,
    fetch_params    JSONB,

    source          TEXT NOT NULL,
    fetched_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (product_id, store_name)
);

ALTER TABLE product_prices ENABLE ROW LEVEL SECURITY;

-- Any authenticated user can read
CREATE POLICY product_prices_select
    ON product_prices FOR SELECT
    USING (auth.role() = 'authenticated');

-- Only service_role can insert/update/delete (Edge Functions)
CREATE POLICY product_prices_insert
    ON product_prices FOR INSERT
    WITH CHECK (auth.jwt() ->> 'role' = 'service_role');

CREATE POLICY product_prices_update
    ON product_prices FOR UPDATE
    USING (auth.jwt() ->> 'role' = 'service_role');

CREATE POLICY product_prices_delete
    ON product_prices FOR DELETE
    USING (auth.jwt() ->> 'role' = 'service_role');

CREATE INDEX idx_product_prices_product_id ON product_prices (product_id);
CREATE INDEX idx_product_prices_store ON product_prices (store_name);
CREATE INDEX idx_product_prices_fetched_at ON product_prices (fetched_at);
CREATE INDEX idx_product_prices_product_store ON product_prices (product_id, store_name);

CREATE TRIGGER trg_product_prices_updated_at
    BEFORE UPDATE ON product_prices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

COMMIT;
