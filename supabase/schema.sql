-- =============================================================================
-- CosteoApp Phase 7B — Supabase PostgreSQL Cloud Schema
-- =============================================================================
-- Convenciones:
--   - UUID para IDs cloud, BIGINT local_id para mapeo con Room
--   - UNIQUE(user_id, local_id) en cada tabla sync
--   - RLS habilitado en todas las tablas
--   - Precios en BIGINT (centavos), igual que Room
--   - Timestamps como TIMESTAMPTZ en Supabase (Room usa epoch millis)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 0. Funcion helper: auto-update updated_at
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------------
-- 1. profiles (extiende auth.users)
-- ---------------------------------------------------------------------------
CREATE TABLE profiles (
    id          UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY profiles_select ON profiles FOR SELECT USING (auth.uid() = id);
CREATE POLICY profiles_insert ON profiles FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY profiles_update ON profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY profiles_delete ON profiles FOR DELETE USING (auth.uid() = id);

CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 2. tiendas
-- ---------------------------------------------------------------------------
CREATE TABLE tiendas (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES auth.users(id),
    local_id    BIGINT NOT NULL,

    nombre      TEXT NOT NULL,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,

    version     INTEGER NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (user_id, local_id)
);

ALTER TABLE tiendas ENABLE ROW LEVEL SECURITY;

CREATE POLICY tiendas_select ON tiendas FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY tiendas_insert ON tiendas FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY tiendas_update ON tiendas FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY tiendas_delete ON tiendas FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX idx_tiendas_user_id ON tiendas (user_id);
CREATE INDEX idx_tiendas_updated_at ON tiendas (updated_at);

CREATE TRIGGER trg_tiendas_updated_at
    BEFORE UPDATE ON tiendas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 3. productos
-- ---------------------------------------------------------------------------
CREATE TABLE productos (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES auth.users(id),
    local_id                BIGINT NOT NULL,

    nombre                  TEXT NOT NULL,
    codigo_barras           TEXT,
    unidad_medida           TEXT NOT NULL,
    cantidad_por_empaque    DOUBLE PRECISION NOT NULL,
    unidades_por_empaque    INTEGER NOT NULL DEFAULT 1,
    es_servicio             BOOLEAN NOT NULL DEFAULT FALSE,
    notas                   TEXT,
    factor_merma            INTEGER NOT NULL DEFAULT 0,

    nutricion_porcion_g     DOUBLE PRECISION,
    nutricion_calorias      DOUBLE PRECISION,
    nutricion_proteinas_g   DOUBLE PRECISION,
    nutricion_carbohidratos_g DOUBLE PRECISION,
    nutricion_grasas_g      DOUBLE PRECISION,
    nutricion_fibra_g       DOUBLE PRECISION,
    nutricion_sodio_mg      DOUBLE PRECISION,
    nutricion_fuente        TEXT,

    activo                  BOOLEAN NOT NULL DEFAULT TRUE,

    version                 INTEGER NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (user_id, local_id)
);

ALTER TABLE productos ENABLE ROW LEVEL SECURITY;

CREATE POLICY productos_select ON productos FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY productos_insert ON productos FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY productos_update ON productos FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY productos_delete ON productos FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX idx_productos_user_id ON productos (user_id);
CREATE INDEX idx_productos_updated_at ON productos (updated_at);
CREATE INDEX idx_productos_codigo_barras ON productos (user_id, codigo_barras);

CREATE TRIGGER trg_productos_updated_at
    BEFORE UPDATE ON productos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 4. producto_tienda
-- ---------------------------------------------------------------------------
CREATE TABLE producto_tienda (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    local_id        BIGINT NOT NULL,

    producto_id     BIGINT NOT NULL,
    tienda_id       BIGINT NOT NULL,
    precio          BIGINT NOT NULL,
    fecha_registro  TIMESTAMPTZ NOT NULL DEFAULT now(),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,

    version         INTEGER NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (user_id, local_id),
    FOREIGN KEY (user_id, producto_id) REFERENCES productos (user_id, local_id),
    FOREIGN KEY (user_id, tienda_id)   REFERENCES tiendas   (user_id, local_id)
);

ALTER TABLE producto_tienda ENABLE ROW LEVEL SECURITY;

CREATE POLICY producto_tienda_select ON producto_tienda FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY producto_tienda_insert ON producto_tienda FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY producto_tienda_update ON producto_tienda FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY producto_tienda_delete ON producto_tienda FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX idx_producto_tienda_user_id ON producto_tienda (user_id);
CREATE INDEX idx_producto_tienda_updated_at ON producto_tienda (updated_at);
CREATE INDEX idx_producto_tienda_producto ON producto_tienda (user_id, producto_id);
CREATE INDEX idx_producto_tienda_tienda ON producto_tienda (user_id, tienda_id);

CREATE TRIGGER trg_producto_tienda_updated_at
    BEFORE UPDATE ON producto_tienda
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 5. inventario
-- ---------------------------------------------------------------------------
CREATE TABLE inventario (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    local_id        BIGINT NOT NULL,

    producto_id     BIGINT NOT NULL,
    tienda_id       BIGINT NOT NULL,
    cantidad        DOUBLE PRECISION NOT NULL,
    precio_compra   BIGINT NOT NULL,
    fecha_compra    TIMESTAMPTZ NOT NULL,
    agotado         BOOLEAN NOT NULL DEFAULT FALSE,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,

    version         INTEGER NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (user_id, local_id),
    FOREIGN KEY (user_id, producto_id) REFERENCES productos (user_id, local_id),
    FOREIGN KEY (user_id, tienda_id)   REFERENCES tiendas   (user_id, local_id)
);

ALTER TABLE inventario ENABLE ROW LEVEL SECURITY;

CREATE POLICY inventario_select ON inventario FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY inventario_insert ON inventario FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY inventario_update ON inventario FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY inventario_delete ON inventario FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX idx_inventario_user_id ON inventario (user_id);
CREATE INDEX idx_inventario_updated_at ON inventario (updated_at);
CREATE INDEX idx_inventario_producto ON inventario (user_id, producto_id);
CREATE INDEX idx_inventario_tienda ON inventario (user_id, tienda_id);

CREATE TRIGGER trg_inventario_updated_at
    BEFORE UPDATE ON inventario
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 6. prefabricados
-- ---------------------------------------------------------------------------
CREATE TABLE prefabricados (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES auth.users(id),
    local_id                BIGINT NOT NULL,

    nombre                  TEXT NOT NULL,
    descripcion             TEXT,
    duplicado_de            BIGINT,
    costo_fijo              BIGINT NOT NULL DEFAULT 0,
    rendimiento_porciones   DOUBLE PRECISION NOT NULL,
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,

    version                 INTEGER NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (user_id, local_id),
    FOREIGN KEY (user_id, duplicado_de) REFERENCES prefabricados (user_id, local_id)
);

ALTER TABLE prefabricados ENABLE ROW LEVEL SECURITY;

CREATE POLICY prefabricados_select ON prefabricados FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY prefabricados_insert ON prefabricados FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY prefabricados_update ON prefabricados FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY prefabricados_delete ON prefabricados FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX idx_prefabricados_user_id ON prefabricados (user_id);
CREATE INDEX idx_prefabricados_updated_at ON prefabricados (updated_at);

CREATE TRIGGER trg_prefabricados_updated_at
    BEFORE UPDATE ON prefabricados
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 7. prefabricado_ingrediente
-- ---------------------------------------------------------------------------
CREATE TABLE prefabricado_ingrediente (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES auth.users(id),
    local_id            BIGINT NOT NULL,

    prefabricado_id     BIGINT NOT NULL,
    producto_id         BIGINT NOT NULL,
    cantidad_usada      DOUBLE PRECISION NOT NULL,
    unidad_usada        TEXT NOT NULL,

    version             INTEGER NOT NULL DEFAULT 1,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (user_id, local_id),
    FOREIGN KEY (user_id, prefabricado_id) REFERENCES prefabricados (user_id, local_id),
    FOREIGN KEY (user_id, producto_id)     REFERENCES productos     (user_id, local_id)
);

ALTER TABLE prefabricado_ingrediente ENABLE ROW LEVEL SECURITY;

CREATE POLICY prefabricado_ingrediente_select ON prefabricado_ingrediente FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY prefabricado_ingrediente_insert ON prefabricado_ingrediente FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY prefabricado_ingrediente_update ON prefabricado_ingrediente FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY prefabricado_ingrediente_delete ON prefabricado_ingrediente FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX idx_prefabricado_ingrediente_user_id ON prefabricado_ingrediente (user_id);
CREATE INDEX idx_prefabricado_ingrediente_updated_at ON prefabricado_ingrediente (updated_at);
CREATE INDEX idx_prefabricado_ingrediente_prefabricado ON prefabricado_ingrediente (user_id, prefabricado_id);
CREATE INDEX idx_prefabricado_ingrediente_producto ON prefabricado_ingrediente (user_id, producto_id);

CREATE TRIGGER trg_prefabricado_ingrediente_updated_at
    BEFORE UPDATE ON prefabricado_ingrediente
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 8. platos
-- ---------------------------------------------------------------------------
CREATE TABLE platos (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES auth.users(id),
    local_id                BIGINT NOT NULL,

    nombre                  TEXT NOT NULL,
    descripcion             TEXT,
    margen_porcentaje       DOUBLE PRECISION,
    precio_venta_manual     BIGINT,
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,

    version                 INTEGER NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (user_id, local_id)
);

ALTER TABLE platos ENABLE ROW LEVEL SECURITY;

CREATE POLICY platos_select ON platos FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY platos_insert ON platos FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY platos_update ON platos FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY platos_delete ON platos FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX idx_platos_user_id ON platos (user_id);
CREATE INDEX idx_platos_updated_at ON platos (updated_at);

CREATE TRIGGER trg_platos_updated_at
    BEFORE UPDATE ON platos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ---------------------------------------------------------------------------
-- 9. plato_componente
-- ---------------------------------------------------------------------------
CREATE TABLE plato_componente (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES auth.users(id),
    local_id            BIGINT NOT NULL,

    plato_id            BIGINT NOT NULL,
    prefabricado_id     BIGINT,
    producto_id         BIGINT,
    cantidad            DOUBLE PRECISION NOT NULL,
    notas               TEXT,

    version             INTEGER NOT NULL DEFAULT 1,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (user_id, local_id),
    FOREIGN KEY (user_id, plato_id)        REFERENCES platos        (user_id, local_id),
    FOREIGN KEY (user_id, prefabricado_id) REFERENCES prefabricados (user_id, local_id),
    FOREIGN KEY (user_id, producto_id)     REFERENCES productos     (user_id, local_id)
);

ALTER TABLE plato_componente ENABLE ROW LEVEL SECURITY;

CREATE POLICY plato_componente_select ON plato_componente FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY plato_componente_insert ON plato_componente FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY plato_componente_update ON plato_componente FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY plato_componente_delete ON plato_componente FOR DELETE USING (auth.uid() = user_id);

CREATE INDEX idx_plato_componente_user_id ON plato_componente (user_id);
CREATE INDEX idx_plato_componente_updated_at ON plato_componente (updated_at);
CREATE INDEX idx_plato_componente_plato ON plato_componente (user_id, plato_id);
CREATE INDEX idx_plato_componente_prefabricado ON plato_componente (user_id, prefabricado_id);
CREATE INDEX idx_plato_componente_producto ON plato_componente (user_id, producto_id);

CREATE TRIGGER trg_plato_componente_updated_at
    BEFORE UPDATE ON plato_componente
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
