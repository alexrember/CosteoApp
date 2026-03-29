import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { corsHeaders } from '../_shared/cors.ts'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface ContributionRequest {
  ean: string
  nombre: string
  marca?: string
  unidad_medida?: string
  cantidad_por_empaque?: number
  imagen_url?: string
}

// ---------------------------------------------------------------------------
// Main handler
// ---------------------------------------------------------------------------

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Authenticate the user via the JWT in the Authorization header
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: 'Se requiere autenticacion' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseAnonKey = Deno.env.get('SUPABASE_ANON_KEY')!
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

    // Client scoped to the calling user (respects RLS)
    const supabaseUser = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    })

    // Service client for writes that bypass RLS
    const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey)

    // Verify the user
    const {
      data: { user },
      error: authError,
    } = await supabaseUser.auth.getUser()

    if (authError || !user) {
      return new Response(
        JSON.stringify({ error: 'Token invalido o expirado' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    const body = (await req.json()) as ContributionRequest

    if (!body.ean || !body.nombre) {
      return new Response(
        JSON.stringify({ error: 'Se requiere ean y nombre' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    // Input validation
    if (!/^\d{8,14}$/.test(body.ean)) {
      return new Response(
        JSON.stringify({ error: 'EAN debe ser entre 8 y 14 digitos numericos' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    body.nombre = body.nombre.trim()
    if (body.nombre.length > 500) {
      return new Response(
        JSON.stringify({ error: 'Nombre no puede exceder 500 caracteres' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    if (body.imagen_url && !body.imagen_url.startsWith('https://')) {
      return new Response(
        JSON.stringify({ error: 'imagen_url debe iniciar con https://' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    if (body.cantidad_por_empaque != null && body.cantidad_por_empaque <= 0) {
      return new Response(
        JSON.stringify({ error: 'cantidad_por_empaque debe ser mayor a 0' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    // Rate limiting: max 50 contributions per user per 24 hours
    const twentyFourHoursAgo = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString()
    const { count: recentCount } = await supabaseAdmin
      .from('product_contributions')
      .select('id', { count: 'exact', head: true })
      .eq('user_id', user.id)
      .gte('created_at', twentyFourHoursAgo)

    if (recentCount != null && recentCount >= 50) {
      return new Response(
        JSON.stringify({ error: 'Limite de contribuciones alcanzado. Intenta de nuevo en 24 horas.' }),
        { status: 429, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    // 1. Check if this user already contributed this EAN
    const { data: existing } = await supabaseAdmin
      .from('product_contributions')
      .select('id')
      .eq('user_id', user.id)
      .eq('ean', body.ean)
      .limit(1)

    if (existing && existing.length > 0) {
      return new Response(
        JSON.stringify({ error: 'Ya contribuiste este producto', code: 'DUPLICATE' }),
        { status: 409, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    // 2. Insert contribution record (always keep for audit trail)
    const { error: insertError } = await supabaseAdmin
      .from('product_contributions')
      .insert({
        user_id: user.id,
        ean: body.ean,
        nombre: body.nombre,
        marca: body.marca ?? null,
        unidad_medida: body.unidad_medida ?? null,
        cantidad_por_empaque: body.cantidad_por_empaque ?? null,
        imagen_url: body.imagen_url ?? null,
        status: 'pending',
      })

    if (insertError) {
      console.error('Insert contribution error:', insertError.message)
      return new Response(
        JSON.stringify({ error: 'Error al guardar contribucion' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    // 3. Check if EAN already exists in global_products
    const { data: globalRow } = await supabaseAdmin
      .from('global_products')
      .select('id, confirmations')
      .eq('ean', body.ean)
      .single()

    let created = false

    if (globalRow) {
      // Product already exists — increment confirmations
      const newCount = (globalRow.confirmations as number) + 1
      await supabaseAdmin
        .from('global_products')
        .update({ confirmations: newCount })
        .eq('id', globalRow.id)

      // Mark this contribution as approved
      await supabaseAdmin
        .from('product_contributions')
        .update({ status: 'approved' })
        .eq('user_id', user.id)
        .eq('ean', body.ean)
    } else {
      // Product doesn't exist — create it directly in global_products
      const { error: createError } = await supabaseAdmin
        .from('global_products')
        .insert({
          ean: body.ean,
          nombre: body.nombre,
          marca: body.marca ?? null,
          unidad_medida: body.unidad_medida ?? 'unidad',
          cantidad_por_empaque: body.cantidad_por_empaque ?? 1,
          imagen_url: body.imagen_url ?? null,
          confirmations: 1,
        })

      if (!createError) {
        created = true

        // Mark this contribution as approved
        await supabaseAdmin
          .from('product_contributions')
          .update({ status: 'approved' })
          .eq('user_id', user.id)
          .eq('ean', body.ean)

        // Also approve any other pending contributions for this EAN
        const { data: otherPending } = await supabaseAdmin
          .from('product_contributions')
          .select('id')
          .eq('ean', body.ean)
          .eq('status', 'pending')

        if (otherPending && otherPending.length > 0) {
          await supabaseAdmin
            .from('product_contributions')
            .update({ status: 'approved' })
            .eq('ean', body.ean)
            .eq('status', 'pending')

          // Update confirmations count
          await supabaseAdmin
            .from('global_products')
            .update({ confirmations: 1 + otherPending.length })
            .eq('ean', body.ean)
        }
      } else {
        console.error('Create global_product error:', createError.message)
      }
    }

    return new Response(
      JSON.stringify({
        ok: true,
        created,
        existed: !!globalRow,
        message: created
          ? 'Producto creado en catalogo global'
          : globalRow
            ? 'Confirmacion registrada'
            : 'Contribucion registrada',
      }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
    )
  } catch (e) {
    console.error('contribute-product error:', e)
    return new Response(
      JSON.stringify({ error: 'Error interno del servidor' }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
    )
  }
})
