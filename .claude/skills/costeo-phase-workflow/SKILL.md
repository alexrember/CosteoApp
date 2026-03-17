---
name: costeo-phase-workflow
description: Workflow completo para implementar cada fase de CosteoApp — desde analisis del plan hasta auditoria final
version: 1.0.0
metadata:
  domain: workflow
  triggers: nueva fase, comenzar fase, empezar fase, workflow fase, implementar fase
  role: developer
  scope: implementation
  output-format: code
---

# CosteoApp — Workflow de Implementacion por Fase

Pasos estandarizados para implementar cada fase del proyecto.

## Paso 1: Analisis del Plan

1. Leer el plan completo de la fase en `Costeo Plan/docs/FaseN-Plan.md`
2. Lanzar subagente Explore para resumir:
   - Objetivo
   - Entidades/tablas nuevas
   - Pantallas nuevas
   - Dependencias de fases anteriores
   - Checklist de implementacion
3. Analizar mejoras vs el plan original (como Fase 1 y 2)
4. Presentar resumen al usuario y pedir confirmacion

## Paso 2: Capa de Datos

1. Crear entities Room en `core/database/entity/`
2. Crear DAOs en `core/database/dao/` (preferir JOIN queries sobre @Relation multiple)
3. Crear relaciones en `core/database/relation/` si necesario
4. Actualizar `CosteoDatabase.kt` (nueva version, agregar entities y DAOs)
5. Crear migracion Room (MIGRATION_N_N+1)
6. Actualizar `DatabaseModule.kt` (agregar migracion y DAO providers)
7. Compilar para verificar: `./gradlew compileDebugKotlin`
8. **Commit**: `feat: Fase N - entities, DAOs, migracion Room`

## Paso 3: Logica de Negocio

1. Crear engines/services en `core/domain/engine/` si aplica
2. Crear modelos de dominio en `core/domain/model/`
3. Crear Repository interface + Impl en `feature/{name}/data/`
4. Crear Hilt modules en `feature/{name}/di/` y `core/di/`
5. Compilar para verificar
6. **Commit**: `feat: Fase N - repository y logica de negocio`

## Paso 4: UI (ViewModels + Screens)

1. Crear UiState data classes
2. Crear ViewModels con:
   - Channel<UiEvent> para eventos one-shot
   - StateFlow para estado reactivo
   - SavedStateHandle para args de navegacion
   - Debounce en search (300ms)
3. Crear Screens con:
   - collectAsStateWithLifecycle()
   - LifecycleResumeEffect para refrescar al volver
   - LaunchedEffect(Unit) para colectar events
   - Empty states diferenciados (sin datos vs sin resultados)
4. Actualizar NavGraph.kt y Screen.kt con nuevas rutas
5. Agregar tab al bottom nav si es pantalla principal
6. Compilar + unit tests: `./gradlew assembleDebug testDebugUnitTest`
7. Instalar en telefono: `adb -s 58251FDCR00D6P install -r app/build/outputs/apk/debug/app-debug.apk`
8. **Commit**: `feat: Fase N - pantallas y navegacion`

## Paso 5: Pruebas Manuales

1. El usuario prueba cada funcionalidad en su telefono fisico
2. Dar un listado de pruebas paso a paso, una por una
3. Esperar feedback del usuario antes de pasar a la siguiente
4. Corregir bugs reportados inmediatamente
5. Automatizar pruebas DESPUES de que el usuario confirme que funciona

## Paso 6: Automatizar Tests

1. Unit tests para engines/logica de negocio
2. Unit tests para mappers/parsers/formatters
3. Tests de integracion para APIs externas (guardar JSON de referencia)
4. UI flow tests en Gradle Managed Device (`testDeviceDebugAndroidTest`)
5. NUNCA correr tests en telefono fisico

## Paso 7: Auditoria Multi-Experto

Usar skill `costeo-audit-phase`:
1. Lanzar 6 subagentes en paralelo (QA, Security, Android, Architecture, SQL, UX)
2. Consolidar hallazgos
3. Corregir HIGH y MEDIUM
4. Agregar LOW a pendientes en `PENDIENTES-MCP.md`
5. Commit de fixes

## Paso 8: Cierre de Fase

1. Actualizar plan en `Costeo Plan/docs/` marcando items como completados
2. Agregar notas de cambios vs plan original
3. Marcar items diferidos como "DIFERIDO" con razon
4. NO renombrar a `done-FaseN-Plan.md` hasta que el usuario lo pida
5. Commit: `docs: actualizar checklist Fase N`

## Convenciones Importantes

- **Commits**: Sin Co-Authored-By, en espanol, conventional commits
- **Build**: `./gradlew assembleDebug testDebugUnitTest` en una sola invocacion
- **Install**: `adb -s 58251FDCR00D6P install -r app/build/outputs/apk/debug/app-debug.apk`
- **Tests instrumentados**: `./gradlew testDeviceDebugAndroidTest` (managed device)
- **No mostrar diffs**: Solo resumen breve de cambios
- **Pruebas manuales primero**: El usuario prueba, despues se automatiza
- **Seed data**: Agregar en `DatabaseSeeder.kt` para desarrollo
