0# CosteoApp — Instrucciones para Claude Code

## Proyecto
App Android nativa (Kotlin + Jetpack Compose) para calcular el costo real de platos de comida en El Salvador. Usa Room (SQLite) local y Supabase (PostgreSQL) para cloud sync.

## Stack tecnologico
- Kotlin 2.1+ con Jetpack Compose (Material Design 3)
- Room (base de datos local SQLite)
- Hilt (inyeccion de dependencias)
- Navigation Compose
- Coroutines + Flow/StateFlow (NO LiveData)
- CameraX + ML Kit (barcode scanning, Fase 2+)
- Supabase (auth + cloud sync, Fase 7+)

## Convenciones del proyecto
- **DB:** snake_case para tablas y columnas, camelCase para propiedades Kotlin
- **Soft delete:** Siempre usar `activo = false` en vez de DELETE fisico
- **IDs:** Long (autoGenerate), nunca Int ni UUID
- **Timestamps:** Long (epoch millis), nunca String ni Date
- **State:** StateFlow + collectAsStateWithLifecycle, nunca LiveData
- **Navegacion:** Callbacks (onNavigateBack), nunca acceder navController directo en Screen
- **Textos UI:** En espanol

## Skills instaladas — Cuando usar cada una
Las skills en `.claude/skills/` se cargan automaticamente. Priorizar segun la tarea:

### Al escribir codigo Kotlin/Compose
- `compose-ui`, `compose-navigation`, `compose-performance-audit`
- `android-architecture`, `android-viewmodel`, `android-coroutines`
- `kotlin-specialist`, `material-design-3-*`, `material-theme-builder`
- `sleek-design-mobile-apps`

### Al disenar base de datos
- `database-schema-design`, `sqlite-database-expert`
- `supabase-postgres-best-practices`, `postgresql-table-design`
- `sql-optimization`, `database-migration`

### Al planificar/disenar
- `brainstorming`, `writing-plans`, `executing-plans`, `prd`
- `architecture-patterns`, `file-organization`

### Al hacer code review/refactor
- `code-review`, `code-refactoring`, `refactor`
- `requesting-code-review`, `verification-before-completion`

### Al hacer testing/debugging
- `test-driven-development`, `systematic-debugging`
- `security-best-practices`

### Al trabajar con Git
- `git-commit`, `conventional-commit`, `finishing-a-development-branch`

### Al hacer scraping/APIs (Fase 6+)
- `firecrawl`, `agent-browser`

## Slash commands disponibles
- `/project:new-entity NombreEntidad` — Genera Entity + DAO + Repository + ViewModel
- `/project:new-screen NombreScreen` — Genera Screen + ViewModel + Navigation route
- `/project:new-form NombreEntidad` — Genera formulario crear/editar
- `/project:generate-er-diagram` — Genera diagrama ER Mermaid de toda la BD

## MCP servers configurados (.mcp.json)
- **sqlite** — Prototipar schema, test queries
- **fetch** — Test APIs de tiendas
- **playwright** — Scraping Super Selectos
- **mermaid** — Diagramas
- **supabase** — DB cloud (Fase 7+, pendiente auth)
- **github** — Issues/PRs (pendiente token)

## Desarrollo por fases
El plan completo esta en el repo hermano: `../Costeo Plan/docs/`
- Fase 0: Esqueleto del proyecto
- Fase 1: Tiendas + Productos (Room, CRUD, Bottom Nav)
- Fase 2: Inventario + Barcode scanning
- Fase 3: Prefabricados/Recetas + Motores de costeo
- Fase 4: Platos + Simulador + Propagacion de precios
- Fase 5: Dashboard + Historial + Export
- Fase 6: Integracion tiendas SV + Voz + OCR
- Fase 7: Auth + Supabase sync
- Fase 8: Backend centralizado + Comercializacion
