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

interface GlobalProduct {
  id: string
  ean: string
  nombre: string
  marca: string | null
  unidad_medida: string
  cantidad_por_empaque: number
  unidades_por_empaque: number
  imagen_url: string | null
  categoria: string | null
}

interface ProductPrice {
  id: string
  product_id: string
  store_name: string
  price: number | null
  list_price: number | null
  is_available: boolean
  fetch_url: string | null
  fetch_params: Record<string, unknown> | null
  source: string
  fetched_at: string
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const PRICE_TTL_MS = 5 * 60 * 60 * 1000 // 5 hours

// ---------------------------------------------------------------------------
// Walmart VTEX helpers
// ---------------------------------------------------------------------------

const WALMART_BASE = 'https://www.walmart.com.sv/api/catalog_system/pub/products/search'

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

interface WalmartPriceResult {
  storeName: string
  price: number | null
  listPrice: number | null
  isAvailable: boolean
  fetchUrl: string
  source: string
  // Product metadata (only used when creating new global_products)
  productName?: string
  brand?: string
  imageUrl?: string
  measurementUnit?: string
  unitMultiplier?: number
}

async function fetchWalmartByBarcode(barcode: string): Promise<WalmartPriceResult | null> {
  const url = `${WALMART_BASE}?fq=alternateIds_Ean:${encodeURIComponent(barcode)}`

  const res = await fetch(url, {
    headers: { Accept: 'application/json' },
  })

  if (!res.ok) {
    console.error(`Walmart API error: ${res.status}`)
    return null
  }

  const products: VtexProduct[] = await res.json()
  if (products.length === 0) return null

  const product = products[0]
  const item = product.items?.[0]
  const seller = item?.sellers?.[0]
  const offer = seller?.commertialOffer

  if (!offer) return null

  return {
    storeName: seller?.sellerName ?? 'Walmart SV',
    price: offer.Price != null ? Math.round(offer.Price * 100) : null,
    listPrice: offer.ListPrice != null ? Math.round(offer.ListPrice * 100) : null,
    isAvailable: offer.IsAvailable ?? false,
    fetchUrl: url,
    source: 'walmart_vtex',
    productName: product.productName ?? item?.nameComplete ?? undefined,
    brand: product.brand ?? undefined,
    imageUrl: item?.images?.[0]?.imageUrl ?? undefined,
    measurementUnit: item?.measurementUnit ?? undefined,
    unitMultiplier: item?.unitMultiplier ?? undefined,
  }
}

async function searchWalmartGeneral(query: string): Promise<UnifiedResult[]> {
  const url = `${WALMART_BASE}?ft=${encodeURIComponent(query)}`

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

function buildPriceSmartParams(query: string): URLSearchParams {
  return new URLSearchParams({
    account_id: '7024',
    auth_key: Deno.env.get('BLOOMREACH_AUTH_KEY') ?? 'ev7libhybjg5h1d1',
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
}

interface PriceSmartPriceResult {
  storeName: string
  price: number | null
  listPrice: number | null
  isAvailable: boolean
  fetchUrl: string
  fetchParams: Record<string, string>
  source: string
  productName?: string
  brand?: string
  imageUrl?: string
}

async function fetchPriceSmartByQuery(query: string): Promise<PriceSmartPriceResult | null> {
  const params = buildPriceSmartParams(query)
  const fullUrl = `${PRICESMART_BASE}?${params.toString()}`

  const res = await fetch(fullUrl, {
    headers: { Accept: 'application/json' },
  })

  if (!res.ok) {
    console.error(`PriceSmart API error: ${res.status}`)
    return null
  }

  const data: BloomreachResponse = await res.json()
  const doc = data.response?.docs?.[0]
  if (!doc) return null

  const paramsObj: Record<string, string> = {}
  params.forEach((value, key) => { paramsObj[key] = value })

  return {
    storeName: 'PriceSmart SV',
    price: doc.price_SV != null ? Math.round(doc.price_SV * 100) : null,
    listPrice: null,
    isAvailable: doc.availability_SV === 'true' || doc.availability_SV === 'in_stock',
    fetchUrl: fullUrl,
    fetchParams: paramsObj,
    source: 'pricesmart_bloomreach',
    productName: doc.title ?? undefined,
    brand: doc.brand ?? undefined,
    imageUrl: doc.thumb_image ?? undefined,
  }
}

async function searchPriceSmartGeneral(query: string): Promise<UnifiedResult[]> {
  const params = buildPriceSmartParams(query)

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
    ean: null,
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
// Super Selectos HTML scraping helpers
// ---------------------------------------------------------------------------

const SELECTOS_BASE = 'https://www.superselectos.com/products'

interface SelectosResult {
  storeName: string
  productName: string
  price: number | null
  imageUrl: string | null
  productId: string | null
  fetchUrl: string
  source: string
}

function parseSelectosHtml(html: string): SelectosResult[] {
  const results: SelectosResult[] = []
  const blocks = html.split('item-producto')

  for (let i = 1; i < blocks.length && results.length < 10; i++) {
    const block = blocks[i]

    // Extract price
    const priceMatch = block.match(/class="precio"[^>]*>\$([0-9.]+)/)
    if (!priceMatch) continue

    // Extract product ID
    const pidMatch = block.match(/productId=(\d+)/)

    // Extract image
    const imgMatch = block.match(/prod-images.*?<img[^>]*src="([^"]+)"/s)

    // Extract product name: remove SVG and HTML tags, find first substantial text
    const noSvg = block.replace(/<svg.*?<\/svg>/gs, '')
    const textLines = noSvg.replace(/<[^>]+>/g, '\n').split('\n')
      .map(l => l.trim())
      .filter(l => l.length > 5 && !l.startsWith('$') && !l.includes('1.00') && !l.includes('Agregar'))

    const productName = textLines[0] || 'Sin nombre'

    const price = parseFloat(priceMatch[1])
    if (isNaN(price) || price <= 0) continue

    results.push({
      storeName: 'Super Selectos',
      productName,
      price: Math.round(price * 100),
      imageUrl: imgMatch ? imgMatch[1] : null,
      productId: pidMatch ? pidMatch[1] : null,
      fetchUrl: `${SELECTOS_BASE}?keyword=${encodeURIComponent(productName.split(' ').slice(0, 3).join(' '))}`,
      source: 'superselectos_web',
    })
  }

  return results
}

async function fetchSelectosByBarcode(barcode: string): Promise<SelectosResult | null> {
  try {
    const url = `${SELECTOS_BASE}?keyword=${encodeURIComponent(barcode)}`
    const res = await fetch(url, {
      headers: { 'User-Agent': 'Mozilla/5.0 CosteoApp/1.0', Accept: 'text/html' },
    })
    if (!res.ok) return null
    const html = await res.text()
    const results = parseSelectosHtml(html)
    return results[0] ?? null
  } catch (e) {
    console.error('Super Selectos fetch error:', e)
    return null
  }
}

async function searchSelectosGeneral(query: string): Promise<UnifiedResult[]> {
  try {
    const url = `${SELECTOS_BASE}?keyword=${encodeURIComponent(query)}`
    const res = await fetch(url, {
      headers: { 'User-Agent': 'Mozilla/5.0 CosteoApp/1.0', Accept: 'text/html' },
    })
    if (!res.ok) return []
    const html = await res.text()
    const parsed = parseSelectosHtml(html)

    return parsed.map(r => ({
      storeName: r.storeName,
      productName: r.productName,
      brand: null,
      ean: null,
      price: r.price,
      listPrice: null,
      isAvailable: true,
      imageUrl: r.imageUrl,
      measurementUnit: null,
      unitMultiplier: null,
      source: r.source,
    }))
  } catch (e) {
    console.error('Super Selectos search error:', e)
    return []
  }
}

// ---------------------------------------------------------------------------
// Refresh price using stored fetch_url
// ---------------------------------------------------------------------------

async function refreshPriceFromUrl(
  cachedPrice: ProductPrice,
): Promise<{ price: number | null; listPrice: number | null; isAvailable: boolean } | null> {
  if (!cachedPrice.fetch_url) return null

  try {
    if (cachedPrice.source === 'walmart_vtex') {
      const res = await fetch(cachedPrice.fetch_url, {
        headers: { Accept: 'application/json' },
      })
      if (!res.ok) return null
      const products: VtexProduct[] = await res.json()
      const offer = products[0]?.items?.[0]?.sellers?.[0]?.commertialOffer
      if (!offer) return null
      return {
        price: offer.Price != null ? Math.round(offer.Price * 100) : null,
        listPrice: offer.ListPrice != null ? Math.round(offer.ListPrice * 100) : null,
        isAvailable: offer.IsAvailable ?? false,
      }
    }

    if (cachedPrice.source === 'pricesmart_bloomreach') {
      const res = await fetch(cachedPrice.fetch_url, {
        headers: { Accept: 'application/json' },
      })
      if (!res.ok) return null
      const data: BloomreachResponse = await res.json()
      const doc = data.response?.docs?.[0]
      if (!doc) return null
      return {
        price: doc.price_SV != null ? Math.round(doc.price_SV * 100) : null,
        listPrice: null,
        isAvailable: doc.availability_SV === 'true' || doc.availability_SV === 'in_stock',
      }
    }
    if (cachedPrice.source === 'superselectos_web') {
      const res = await fetch(cachedPrice.fetch_url, {
        headers: { 'User-Agent': 'Mozilla/5.0 CosteoApp/1.0', Accept: 'text/html' },
      })
      if (!res.ok) return null
      const html = await res.text()
      const parsed = parseSelectosHtml(html)
      if (parsed.length === 0) return null
      return {
        price: parsed[0].price,
        listPrice: null,
        isAvailable: true,
      }
    }
  } catch (e) {
    console.error(`Error refreshing price from ${cachedPrice.fetch_url}:`, e)
  }

  return null
}

// ---------------------------------------------------------------------------
// Global product + price helpers
// ---------------------------------------------------------------------------

async function getGlobalProduct(
  supabase: ReturnType<typeof createClient>,
  barcode: string,
): Promise<GlobalProduct | null> {
  const { data, error } = await supabase
    .from('global_products')
    .select('*')
    .eq('ean', barcode)
    .single()

  if (error || !data) return null
  return data as GlobalProduct
}

async function getCachedPrices(
  supabase: ReturnType<typeof createClient>,
  productId: string,
): Promise<ProductPrice[]> {
  const { data, error } = await supabase
    .from('product_prices')
    .select('*')
    .eq('product_id', productId)

  if (error || !data) return []
  return data as ProductPrice[]
}

function isPriceFresh(price: ProductPrice): boolean {
  const fetchedAt = new Date(price.fetched_at).getTime()
  return (Date.now() - fetchedAt) < PRICE_TTL_MS
}

async function upsertProductPrice(
  supabase: ReturnType<typeof createClient>,
  productId: string,
  storeName: string,
  price: number | null,
  listPrice: number | null,
  isAvailable: boolean,
  fetchUrl: string | null,
  fetchParams: Record<string, unknown> | null,
  source: string,
): Promise<void> {
  const now = new Date().toISOString()
  const { error } = await supabase
    .from('product_prices')
    .upsert(
      {
        product_id: productId,
        store_name: storeName,
        price,
        list_price: listPrice,
        is_available: isAvailable,
        fetch_url: fetchUrl,
        fetch_params: fetchParams,
        source,
        fetched_at: now,
        updated_at: now,
      },
      { onConflict: 'product_id,store_name' },
    )

  if (error) {
    console.error(`Error upserting product_price for ${storeName}:`, error.message)
  }
}

async function createGlobalProduct(
  supabase: ReturnType<typeof createClient>,
  ean: string,
  nombre: string,
  marca: string | null,
  imagenUrl: string | null,
  unidadMedida: string | null,
  cantidadPorEmpaque: number | null,
): Promise<GlobalProduct | null> {
  const { data, error } = await supabase
    .from('global_products')
    .insert({
      ean,
      nombre,
      marca,
      imagen_url: imagenUrl,
      unidad_medida: unidadMedida ?? 'unidad',
      cantidad_por_empaque: cantidadPorEmpaque ?? 1,
      confirmations: 1,
    })
    .select()
    .single()

  if (error) {
    console.error('Error creating global_product:', error.message)
    return null
  }

  return data as GlobalProduct
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
// Barcode search flow (global_products + product_prices)
// ---------------------------------------------------------------------------

async function handleBarcodeSearch(
  supabase: ReturnType<typeof createClient>,
  barcode: string,
): Promise<{ results: UnifiedResult[]; fromCache: boolean }> {
  // 1. Check if product exists in global_products
  let globalProduct = await getGlobalProduct(supabase, barcode)

  if (globalProduct) {
    // 2. Check cached prices
    const cachedPrices = await getCachedPrices(supabase, globalProduct.id)
    const freshPrices = cachedPrices.filter(isPriceFresh)
    const stalePrices = cachedPrices.filter((p) => !isPriceFresh(p))

    // If all prices are fresh, return cached
    if (freshPrices.length > 0 && stalePrices.length === 0) {
      const results: UnifiedResult[] = freshPrices.map((p) => ({
        storeName: p.store_name,
        productName: globalProduct!.nombre,
        brand: globalProduct!.marca,
        ean: globalProduct!.ean,
        price: p.price != null ? Number(p.price) : null,
        listPrice: p.list_price != null ? Number(p.list_price) : null,
        isAvailable: p.is_available ?? true,
        imageUrl: globalProduct!.imagen_url,
        measurementUnit: globalProduct!.unidad_medida,
        unitMultiplier: globalProduct!.cantidad_por_empaque,
        source: p.source,
      }))

      return { results, fromCache: true }
    }

    // 3. Refresh stale prices using stored fetch_url
    const refreshPromises: Promise<void>[] = []

    for (const stalePrice of stalePrices) {
      refreshPromises.push(
        (async () => {
          const refreshed = await refreshPriceFromUrl(stalePrice)
          if (refreshed) {
            await upsertProductPrice(
              supabase,
              globalProduct!.id,
              stalePrice.store_name,
              refreshed.price,
              refreshed.listPrice,
              refreshed.isAvailable,
              stalePrice.fetch_url,
              stalePrice.fetch_params,
              stalePrice.source,
            )
          }
        })(),
      )
    }

    // Also fetch from stores that don't have a cached price yet
    const cachedStores = new Set(cachedPrices.map((p) => p.store_name))
    const fetchNewPromises = fetchMissingStorePrices(supabase, barcode, globalProduct, cachedStores)

    await Promise.all([...refreshPromises, ...fetchNewPromises])

    // 4. Re-read all prices after refresh
    const updatedPrices = await getCachedPrices(supabase, globalProduct.id)
    const results: UnifiedResult[] = updatedPrices.map((p) => ({
      storeName: p.store_name,
      productName: globalProduct!.nombre,
      brand: globalProduct!.marca,
      ean: globalProduct!.ean,
      price: p.price != null ? Number(p.price) : null,
      listPrice: p.list_price != null ? Number(p.list_price) : null,
      isAvailable: p.is_available ?? true,
      imageUrl: globalProduct!.imagen_url,
      measurementUnit: globalProduct!.unidad_medida,
      unitMultiplier: globalProduct!.cantidad_por_empaque,
      source: p.source,
    }))

    // Include fresh cached prices in the "fromCache" determination
    return { results, fromCache: freshPrices.length > 0 && stalePrices.length === 0 }
  }

  // 5. Product NOT in global_products — call store APIs to discover it
  const [walmartResult, priceSmartResult, selectosResult] = await Promise.all([
    (async () => {
      const start = Date.now()
      try {
        const r = await fetchWalmartByBarcode(barcode)
        await updateScraperStatus(supabase, 'walmart_vtex', true, Date.now() - start)
        return r
      } catch (e) {
        console.error('Walmart fetch failed:', e)
        await updateScraperStatus(supabase, 'walmart_vtex', false, Date.now() - start)
        return null
      }
    })(),
    (async () => {
      const start = Date.now()
      try {
        const r = await fetchPriceSmartByQuery(barcode)
        await updateScraperStatus(supabase, 'pricesmart_bloomreach', true, Date.now() - start)
        return r
      } catch (e) {
        console.error('PriceSmart fetch failed:', e)
        await updateScraperStatus(supabase, 'pricesmart_bloomreach', false, Date.now() - start)
        return null
      }
    })(),
  ])

  // Try Super Selectos with barcode directly (it supports barcode keyword search)
  let selectosResult: SelectosResult | null = null
  if (!walmartResult && !priceSmartResult) {
    try {
      const start = Date.now()
      selectosResult = await fetchSelectosByBarcode(barcode)
      await updateScraperStatus(supabase, 'superselectos_web', selectosResult != null, Date.now() - start)
    } catch (e) {
      console.error('Super Selectos barcode fetch failed:', e)
    }
  }

  // Pick best data source to create the global product
  const bestSource = walmartResult ?? priceSmartResult
  const selectosFallback = !bestSource && selectosResult ? {
    productName: selectosResult.productName,
    brand: null as string | null,
    imageUrl: selectosResult.imageUrl,
    measurementUnit: null as string | null,
    unitMultiplier: null as number | null,
  } : null

  if (!bestSource && !selectosFallback) {
    return { results: [], fromCache: false }
  }

  // Create global_product from API data
  const source = bestSource ?? selectosFallback!
  globalProduct = await createGlobalProduct(
    supabase,
    barcode,
    source.productName ?? 'Sin nombre',
    source.brand ?? null,
    source.imageUrl ?? null,
    source.measurementUnit ?? null,
    source.unitMultiplier ?? null,
  )

  if (!globalProduct) {
    return { results: [], fromCache: false }
  }

  // Save prices from both stores
  const priceUpserts: Promise<void>[] = []
  const results: UnifiedResult[] = []

  if (walmartResult) {
    priceUpserts.push(
      upsertProductPrice(
        supabase,
        globalProduct.id,
        walmartResult.storeName,
        walmartResult.price,
        walmartResult.listPrice,
        walmartResult.isAvailable,
        walmartResult.fetchUrl,
        null,
        walmartResult.source,
      ),
    )
    results.push({
      storeName: walmartResult.storeName,
      productName: globalProduct.nombre,
      brand: globalProduct.marca,
      ean: globalProduct.ean,
      price: walmartResult.price,
      listPrice: walmartResult.listPrice,
      isAvailable: walmartResult.isAvailable,
      imageUrl: globalProduct.imagen_url,
      measurementUnit: globalProduct.unidad_medida,
      unitMultiplier: globalProduct.cantidad_por_empaque,
      source: walmartResult.source,
    })
  }

  if (priceSmartResult) {
    priceUpserts.push(
      upsertProductPrice(
        supabase,
        globalProduct.id,
        priceSmartResult.storeName,
        priceSmartResult.price,
        priceSmartResult.listPrice,
        priceSmartResult.isAvailable,
        priceSmartResult.fetchUrl,
        priceSmartResult.fetchParams,
        priceSmartResult.source,
      ),
    )
    results.push({
      storeName: priceSmartResult.storeName,
      productName: globalProduct.nombre,
      brand: globalProduct.marca,
      ean: globalProduct.ean,
      price: priceSmartResult.price,
      listPrice: priceSmartResult.listPrice,
      isAvailable: priceSmartResult.isAvailable,
      imageUrl: globalProduct.imagen_url,
      measurementUnit: globalProduct.unidad_medida,
      unitMultiplier: globalProduct.cantidad_por_empaque,
      source: priceSmartResult.source,
    })
  }

  if (selectosResult && selectosResult.price && globalProduct) {
    priceUpserts.push(
      upsertProductPrice(
        supabase,
        globalProduct.id,
        selectosResult.storeName,
        selectosResult.price,
        null,
        true,
        selectosResult.fetchUrl,
        null,
        selectosResult.source,
      ),
    )
    results.push({
      storeName: selectosResult.storeName,
      productName: selectosResult.productName,
      brand: null,
      ean: globalProduct.ean,
      price: selectosResult.price,
      listPrice: null,
      isAvailable: true,
      imageUrl: selectosResult.imageUrl,
      measurementUnit: null,
      unitMultiplier: null,
      source: selectosResult.source,
    })
  }

  await Promise.all(priceUpserts)

  // Search Super Selectos by product name (doesn't support barcode)
  if (globalProduct && globalProduct.nombre) {
    try {
      const searchName = globalProduct.nombre.split(' ').slice(0, 3).join(' ')
      const start = Date.now()
      const selectosResult = await fetchSelectosByBarcode(searchName)
      if (selectosResult && selectosResult.price) {
        await upsertProductPrice(
          supabase,
          globalProduct.id,
          selectosResult.storeName,
          selectosResult.price,
          null,
          true,
          selectosResult.fetchUrl,
          null,
          selectosResult.source,
        )
        results.push({
          storeName: selectosResult.storeName,
          productName: selectosResult.productName,
          brand: null,
          ean: globalProduct.ean,
          price: selectosResult.price,
          listPrice: null,
          isAvailable: true,
          imageUrl: selectosResult.imageUrl,
          measurementUnit: null,
          unitMultiplier: null,
          source: selectosResult.source,
        })
      }
      await updateScraperStatus(supabase, 'superselectos_web', true, Date.now() - start)
    } catch (e) {
      console.error('Super Selectos search by name failed:', e)
    }
  }

  return { results, fromCache: false }
}

function fetchMissingStorePrices(
  supabase: ReturnType<typeof createClient>,
  barcode: string,
  globalProduct: GlobalProduct,
  cachedStores: Set<string>,
): Promise<void>[] {
  const promises: Promise<void>[] = []

  // Walmart — fetch if no cached price exists for any Walmart seller
  const hasWalmart = Array.from(cachedStores).some((s) => s.toLowerCase().includes('walmart'))
  if (!hasWalmart) {
    promises.push(
      (async () => {
        try {
          const result = await fetchWalmartByBarcode(barcode)
          if (result) {
            await upsertProductPrice(
              supabase,
              globalProduct.id,
              result.storeName,
              result.price,
              result.listPrice,
              result.isAvailable,
              result.fetchUrl,
              null,
              result.source,
            )
          }
        } catch (e) {
          console.error('Walmart missing-store fetch failed:', e)
        }
      })(),
    )
  }

  // Super Selectos — fetch by product name if no cached price
  if (!cachedStores.has('Super Selectos')) {
    promises.push(
      (async () => {
        try {
          const searchName = globalProduct.nombre.split(' ').slice(0, 3).join(' ')
          const result = await fetchSelectosByBarcode(searchName)
          if (result && result.price) {
            await upsertProductPrice(
              supabase,
              globalProduct.id,
              result.storeName,
              result.price,
              null,
              true,
              result.fetchUrl,
              null,
              result.source,
            )
          }
        } catch (e) {
          console.error('Super Selectos missing-store fetch failed:', e)
        }
      })(),
    )
  }

  // PriceSmart — fetch if no cached price exists
  if (!cachedStores.has('PriceSmart SV')) {
    promises.push(
      (async () => {
        try {
          const result = await fetchPriceSmartByQuery(barcode)
          if (result) {
            await upsertProductPrice(
              supabase,
              globalProduct.id,
              result.storeName,
              result.price,
              result.listPrice,
              result.isAvailable,
              result.fetchUrl,
              result.fetchParams,
              result.source,
            )
          }
        } catch (e) {
          console.error('PriceSmart missing-store fetch failed:', e)
        }
      })(),
    )
  }

  return promises
}

// ---------------------------------------------------------------------------
// Main handler
// ---------------------------------------------------------------------------

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Auth is optional for search — products are public data
    const authHeader = req.headers.get('Authorization')
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseAnonKey = Deno.env.get('SUPABASE_ANON_KEY')!

    let userId: string | null = null
    if (authHeader) {
      try {
        const supabaseUser = createClient(supabaseUrl, supabaseAnonKey, {
          global: { headers: { Authorization: authHeader } },
        })
        const { data: { user } } = await supabaseUser.auth.getUser()
        userId = user?.id ?? null
      } catch (_) { /* auth failed, continue as anonymous */ }
    }

    const { query, barcode } = (await req.json()) as SearchRequest

    if (!query && !barcode) {
      return new Response(
        JSON.stringify({ error: 'Se requiere query o barcode' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    // Service-role client for DB reads/writes
    const supabase = createClient(
      supabaseUrl,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,
    )

    const searchStart = Date.now()

    // --- BARCODE SEARCH: global_products + product_prices flow ---
    if (barcode) {
      const { results, fromCache } = await handleBarcodeSearch(supabase, barcode)

      const storesFound = [...new Set(results.map(r => r.storeName))]
      await supabase.from('search_logs').insert({
        user_id: userId,
        barcode,
        stores_searched: ['walmart_vtex', 'pricesmart_bloomreach', 'superselectos_web'],
        stores_found: storesFound,
        results_count: results.length,
        from_cache: fromCache,
        response_ms: Date.now() - searchStart,
      })

      return new Response(
        JSON.stringify({ results, fromCache }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    // --- TEXT SEARCH: call store APIs directly (no global caching for text) ---
    const storePromises: Promise<{ store: string; results: UnifiedResult[]; ms: number; ok: boolean }>[] = []

    storePromises.push(
      (async () => {
        const start = Date.now()
        try {
          const results = await searchWalmartGeneral(query)
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

    storePromises.push(
      (async () => {
        const start = Date.now()
        try {
          const results = await searchPriceSmartGeneral(query)
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

    storePromises.push(
      (async () => {
        const start = Date.now()
        try {
          const results = await searchSelectosGeneral(query)
          const ms = Date.now() - start
          await updateScraperStatus(supabase, 'superselectos_web', true, ms)
          return { store: 'superselectos_web', results, ms, ok: true }
        } catch (e) {
          const ms = Date.now() - start
          console.error('Super Selectos fetch failed:', e)
          await updateScraperStatus(supabase, 'superselectos_web', false, ms)
          return { store: 'superselectos_web', results: [], ms, ok: false }
        }
      })(),
    )

    const storeResults = await Promise.all(storePromises)
    const allStoreResults = storeResults.flatMap((s) => s.results)

    const storesFound = storeResults.filter(s => s.ok && s.results.length > 0).map(s => s.store)
    await supabase.from('search_logs').insert({
      user_id: userId,
      query,
      stores_searched: storeResults.map(s => s.store),
      stores_found: storesFound,
      results_count: allStoreResults.length,
      from_cache: false,
      response_ms: Date.now() - searchStart,
    })

    return new Response(
      JSON.stringify({
        results: allStoreResults,
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
