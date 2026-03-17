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

## Skills — Cuando usar cada una

### Skills custom de CosteoApp (prioridad maxima)
- `costeo-room-entity` — Generar Entity + DAO + Repository + DI completo
- `costeo-compose-screen` — Generar Screen + ViewModel + Form + Navigation
- `costeo-motor-calculo` — Motores de costeo, nutricion, propagacion de precios
- `costeo-supabase-sync` — Sync Room↔Supabase (Fase 7+)
- `costeo-audit-phase` — Auditoria multi-experto al completar cada fase

### Skills genericas (37 complementarias)
- **Kotlin/Compose:** `compose-ui`, `compose-navigation`, `compose-performance-audit`, `android-architecture`, `android-viewmodel`, `android-coroutines`, `kotlin-specialist`, `kotlin-concurrency-expert`, `coil-compose`, `m3-web-android`
- **Material Design 3:** `material-design-3-guide`, `material-design-3-color`, `material-design-3-components`, `material-theme-builder`
- **Base de datos:** `database-schema-design`, `supabase-postgres-best-practices`, `sql-optimization`, `database-migration`
- **Planificacion:** `brainstorming`, `writing-plans`, `executing-plans`
- **Code review:** `code-review`, `refactor`, `verification-before-completion`
- **Testing/Debug:** `test-driven-development`, `systematic-debugging`, `security-best-practices`, `android-testing`
- **Git:** `git-commit`, `conventional-commit`, `finishing-a-development-branch`
- **Build:** `gradle-build-performance`, `android-gradle-logic`
- **Android misc:** `android-data-layer`, `android-retrofit`, `android-emulator-skill`, `android-accessibility`

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
