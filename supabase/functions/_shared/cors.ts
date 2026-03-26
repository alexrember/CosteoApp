// TODO(production): Restrict Access-Control-Allow-Origin to the app's domain
// instead of '*'. Wildcard is acceptable for now since clients are mobile apps.
export const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers':
    'authorization, x-client-info, apikey, content-type',
}
