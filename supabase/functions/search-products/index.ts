import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { corsHeaders } from '../_shared/cors.ts'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface SearchRequest {
  query: string
  barcode?: string
}

interface UnifiedResult {
  storeName: string
  productName: string
  brand: string | null
  ean: string | null
  price: number | null       // cents
  listPrice: number | null   // cents
  isAvailable: boolean
  imageUrl: string | null
  measurementUnit: string | null
  unitMultiplier: number | null
  source: string
}

// ---------------------------------------------------------------------------
// Walmart VTEX helpers
// ---------------------------------------------------------------------------

const WALMART_BASE = 'https://www.walmart.com.sv/api/catalog_system/pub/products/search'
const WALMART_TTL_MS = 60 * 60 * 1000 // 1 hour

interface VtexProduct {
  productName?: string
  brand?: string
  items?: VtexItem[]
}

interface VtexItem {
  itemId?: string
  nameComplete?: string
  ean?: string
  measurementUnit?: string
  unitMultiplier?: number
  images?: { imageUrl?: string }[]
  sellers?: VtexSeller[]
}

interface VtexSeller {
  sellerId?: string
  sellerName?: string
  commertialOffer?: {
    Price?: number
    ListPrice?: number
    IsAvailable?: boolean
  }
}

async function searchWalmart(
  query: string,
  barcode?: string,
): Promise<UnifiedResult[]> {
  const url = barcode
    ? `${WALMART_BASE}?fq=alternateIds_Ean:${encodeURIComponent(barcode)}`
    : `${WALMART_BASE}?ft=${encodeURIComponent(query)}`

  const res = await fetch(url, {
    headers: { Accept: 'application/json' },
  })

  if (!res.ok) {
    console.error(`Walmart API error: ${res.status}`)
    return []
  }

  const products: VtexProduct[] = await res.json()
  const results: UnifiedResult[] = []

  for (const product of products) {
    for (const item of product.items ?? []) {
      for (const seller of item.sellers ?? []) {
        const offer = seller.commertialOffer
        if (!offer) continue
        results.push({
          storeName: seller.sellerName ?? 'Walmart SV',
          productName: product.productName ?? item.nameComplete ?? 'Sin nombre',
          brand: product.brand ?? null,
          ean: item.ean ?? null,
          price: offer.Price != null ? Math.round(offer.Price * 100) : null,
          listPrice: offer.ListPrice != null ? Math.round(offer.ListPrice * 100) : null,
          isAvailable: offer.IsAvailable ?? false,
          imageUrl: item.images?.[0]?.imageUrl ?? null,
          measurementUnit: item.measurementUnit ?? null,
          unitMultiplier: item.unitMultiplier ?? null,
          source: 'walmart_vtex',
        })
      }
    }
  }

  return results
}

// ---------------------------------------------------------------------------
// PriceSmart Bloomreach helpers
// ---------------------------------------------------------------------------

const PRICESMART_BASE = 'https://core.dxpapi.com/api/v1/core/'
const PRICESMART_TTL_MS = 4 * 60 * 60 * 1000 // 4 hours

interface BloomreachResponse {
  response?: {
    numFound?: number
    docs?: BloomreachProduct[]
  }
}

interface BloomreachProduct {
  title?: string
  brand?: string
  pid?: string
  price_SV?: number
  thumb_image?: string
  availability_SV?: string
}

async function searchPriceSmart(query: string): Promise<UnifiedResult[]> {
  const params = new URLSearchParams({
    account_id: '7024',
    auth_key: 'ev7libhybjg5h1d1',
    domain_key: 'pricesmart_bloomreach_io_es',
    view_id: 'SV',
    request_type: 'search',
    search_type: 'keyword',
    q: query,
    rows: '10',
    fl: 'pid,title,brand,price_SV,thumb_image,availability_SV',
    _br_uid_2: 'costeoapp',
    url: 'https://www.pricesmart.com/es-sv/busqueda',
    ref_url: 'https://www.pricesmart.com/es-sv',
  })

  const res = await fetch(`${PRICESMART_BASE}?${params.toString()}`, {
    headers: { Accept: 'application/json' },
  })

  if (!res.ok) {
    console.error(`PriceSmart API error: ${res.status}`)
    return []
  }

  const data: BloomreachResponse = await res.json()
  const docs = data.response?.docs ?? []

  return docs.map((doc) => ({
    storeName: 'PriceSmart SV',
    productName: doc.title ?? 'Sin nombre',
    brand: doc.brand ?? null,
    ean: null, // Bloomreach does not return EAN
    price: doc.price_SV != null ? Math.round(doc.price_SV * 100) : null,
    listPrice: null,
    isAvailable: doc.availability_SV === 'true' || doc.availability_SV === 'in_stock',
    imageUrl: doc.thumb_image ?? null,
    measurementUnit: null,
    unitMultiplier: null,
    source: 'pricesmart_bloomreach',
  }))
}

// ---------------------------------------------------------------------------
// Cache helpers
// ---------------------------------------------------------------------------

async function getCachedResults(
  supabase: ReturnType<typeof createClient>,
  query: string,
  barcode?: string,
): Promise<UnifiedResult[]> {
  let builder = supabase
    .from('price_cache')
    .select('*')
    .gt('expires_at', new Date().toISOString())

  if (barcode) {
    builder = builder.eq('ean', barcode)
  } else {
    builder = builder.eq('search_query', query)
  }

  const { data, error } = await builder

  if (error || !data || data.length === 0) return []

  return data.map((row: Record<string, unknown>) => ({
    storeName: row.store_name as string,
    productName: row.product_name as string,
    brand: (row.brand as string) ?? null,
    ean: (row.ean as string) ?? null,
    price: row.price != null ? Number(row.price) : null,
    listPrice: row.list_price != null ? Number(row.list_price) : null,
    isAvailable: (row.is_available as boolean) ?? true,
    imageUrl: (row.image_url as string) ?? null,
    measurementUnit: (row.measurement_unit as string) ?? null,
    unitMultiplier: (row.unit_multiplier as number) ?? null,
    source: row.source as string,
  }))
}

async function cacheResults(
  supabase: ReturnType<typeof createClient>,
  results: UnifiedResult[],
  query: string,
  barcode?: string,
): Promise<void> {
  if (results.length === 0) return

  const rows = results.map((r) => {
    const ttlMs = r.source === 'walmart_vtex' ? WALMART_TTL_MS : PRICESMART_TTL_MS
    const expiresAt = new Date(Date.now() + ttlMs).toISOString()

    return {
      ean: barcode ?? r.ean ?? null,
      search_query: query,
      store_name: r.storeName,
      product_name: r.productName,
      brand: r.brand,
      price: r.price,
      list_price: r.listPrice,
      is_available: r.isAvailable,
      image_url: r.imageUrl,
      measurement_unit: r.measurementUnit,
      unit_multiplier: r.unitMultiplier,
      source: r.source,
      expires_at: expiresAt,
      updated_at: new Date().toISOString(),
    }
  })

  const { error } = await supabase.from('price_cache').upsert(rows, {
    onConflict: 'id',
    ignoreDuplicates: false,
  })

  if (error) {
    console.error('Cache write error:', error.message)
  }
}

// ---------------------------------------------------------------------------
// Shared products lookup
// ---------------------------------------------------------------------------

async function getSharedProducts(
  supabase: ReturnType<typeof createClient>,
  barcode: string,
): Promise<UnifiedResult[]> {
  const { data, error } = await supabase
    .from('shared_products')
    .select('*')
    .eq('ean', barcode)

  if (error || !data || data.length === 0) return []

  return data.map((row: Record<string, unknown>) => ({
    storeName: 'CosteoApp Comunidad',
    productName: row.nombre as string,
    brand: (row.marca as string) ?? null,
    ean: row.ean as string,
    price: null,
    listPrice: null,
    isAvailable: true,
    imageUrl: (row.imagen_url as string) ?? null,
    measurementUnit: (row.unidad_medida as string) ?? null,
    unitMultiplier: null,
    source: 'shared_products',
  }))
}

// ---------------------------------------------------------------------------
// Scraper status tracking
// ---------------------------------------------------------------------------

async function updateScraperStatus(
  supabase: ReturnType<typeof createClient>,
  storeName: string,
  success: boolean,
  responseMs: number,
): Promise<void> {
  const now = new Date().toISOString()

  if (success) {
    await supabase
      .from('scraper_status')
      .upsert(
        {
          store_name: storeName,
          last_success_at: now,
          consecutive_failures: 0,
          is_healthy: true,
          avg_response_ms: Math.round(responseMs),
          updated_at: now,
        },
        { onConflict: 'store_name' },
      )
  } else {
    // Increment failures — read current first
    const { data } = await supabase
      .from('scraper_status')
      .select('consecutive_failures')
      .eq('store_name', storeName)
      .single()

    const failures = ((data?.consecutive_failures as number) ?? 0) + 1

    await supabase
      .from('scraper_status')
      .upsert(
        {
          store_name: storeName,
          last_failure_at: now,
          consecutive_failures: failures,
          is_healthy: failures < 5,
          updated_at: now,
        },
        { onConflict: 'store_name' },
      )
  }
}

// ---------------------------------------------------------------------------
// Main handler
// ---------------------------------------------------------------------------

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { query, barcode } = (await req.json()) as SearchRequest

    if (!query && !barcode) {
      return new Response(
        JSON.stringify({ error: 'Se requiere query o barcode' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,
    )

    const searchTerm = barcode ?? query

    // 1. Check cache first
    const cached = await getCachedResults(supabase, searchTerm, barcode)
    if (cached.length > 0) {
      // If barcode, also attach shared_products info
      let shared: UnifiedResult[] = []
      if (barcode) {
        shared = await getSharedProducts(supabase, barcode)
      }
      const all = [...cached, ...shared]

      return new Response(
        JSON.stringify({ results: all, fromCache: true }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    // 2. Cache miss — call store APIs in parallel
    const storePromises: Promise<{ store: string; results: UnifiedResult[]; ms: number; ok: boolean }>[] = []

    // Walmart
    storePromises.push(
      (async () => {
        const start = Date.now()
        try {
          const results = await searchWalmart(searchTerm, barcode)
          const ms = Date.now() - start
          await updateScraperStatus(supabase, 'walmart_vtex', true, ms)
          return { store: 'walmart_vtex', results, ms, ok: true }
        } catch (e) {
          const ms = Date.now() - start
          console.error('Walmart fetch failed:', e)
          await updateScraperStatus(supabase, 'walmart_vtex', false, ms)
          return { store: 'walmart_vtex', results: [], ms, ok: false }
        }
      })(),
    )

    // PriceSmart
    storePromises.push(
      (async () => {
        const start = Date.now()
        try {
          const results = await searchPriceSmart(searchTerm)
          const ms = Date.now() - start
          await updateScraperStatus(supabase, 'pricesmart_bloomreach', true, ms)
          return { store: 'pricesmart_bloomreach', results, ms, ok: true }
        } catch (e) {
          const ms = Date.now() - start
          console.error('PriceSmart fetch failed:', e)
          await updateScraperStatus(supabase, 'pricesmart_bloomreach', false, ms)
          return { store: 'pricesmart_bloomreach', results: [], ms, ok: false }
        }
      })(),
    )

    const storeResults = await Promise.all(storePromises)

    // Flatten all store results
    const allStoreResults = storeResults.flatMap((s) => s.results)

    // 3. Cache fresh results
    await cacheResults(supabase, allStoreResults, searchTerm, barcode)

    // 4. If barcode, also look up shared_products
    let shared: UnifiedResult[] = []
    if (barcode) {
      shared = await getSharedProducts(supabase, barcode)
    }

    const finalResults = [...allStoreResults, ...shared]

    return new Response(
      JSON.stringify({
        results: finalResults,
        fromCache: false,
        stores: storeResults.map((s) => ({
          store: s.store,
          count: s.results.length,
          responseMs: s.ms,
          ok: s.ok,
        })),
      }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
    )
  } catch (e) {
    console.error('search-products error:', e)
    return new Response(
      JSON.stringify({ error: 'Error interno del servidor' }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
    )
  }
})
