---
name: costeo-audit-phase
description: Auditoria multi-experto al completar cada fase de CosteoApp — compilacion, convenciones, rendimiento, seguridad, UI/UX
version: 1.0.0
metadata:
  domain: quality
  triggers: auditoria, audit, review, fase completa, revision, calidad, hallazgos
  role: reviewer
  scope: review
  output-format: documentation
---

# CosteoApp Auditoria por Fase

Checklist de auditoria multi-dimension para ejecutar al completar cada fase del proyecto.

## Cuando Ejecutar

Al terminar cada fase (Fase 0-8) antes de hacer merge a main. Lanzar con: "auditar fase N".

## Core Workflow

1. **Compilar** — Build limpio sin warnings
2. **Convenciones** — Verificar adherencia a reglas del proyecto
3. **Arquitectura** — Revisar capas, DI, separacion de concerns
4. **Rendimiento** — Compose recomposition, queries N+1, memory leaks
5. **Seguridad** — Input validation, SQL injection, data exposure
6. **UI/UX** — Material Design 3, accesibilidad, textos espanol
7. **Tests** — Cobertura de logica critica
8. **Reporte** — Tabla de hallazgos con severidad

## 1. Compilacion y Build

```bash
./gradlew assembleDebug 2>&1 | grep -E "(error|warning|FAILED)"
./gradlew lintDebug
```

- [ ] Build exitoso sin errores
- [ ] Sin warnings de deprecacion
- [ ] Lint sin errores criticos
- [ ] Version catalog (libs.versions.toml) sin dependencias sin usar

## 2. Convenciones del Proyecto

- [ ] Tablas en snake_case plural
- [ ] Propiedades Kotlin en camelCase
- [ ] @ColumnInfo con name en snake_case en TODOS los campos
- [ ] IDs tipo Long (no Int, no UUID)
- [ ] Timestamps tipo Long (epoch millis)
- [ ] Soft delete con activo = false (no @Delete fisico)
- [ ] StateFlow (no LiveData)
- [ ] Navegacion por callbacks (no navController en Screen)
- [ ] Textos UI en espanol
- [ ] Sin comentarios innecesarios

## 3. Arquitectura

- [ ] Entidades en `core.database.entity`
- [ ] DAOs en `core.database.dao`
- [ ] Repositories: interface en `feature.{x}.data`, impl con @Singleton
- [ ] ViewModels con @HiltViewModel
- [ ] Motores/engines en `core.domain.engine`
- [ ] DI correctamente configurado (DatabaseModule, FeatureModules)
- [ ] Sin logica de negocio en Composables
- [ ] Sin acceso directo a DAOs desde ViewModels (usar Repository)

## 4. Rendimiento

- [ ] LazyColumn (no Column con forEach para listas)
- [ ] key() en items de LazyColumn
- [ ] Composables sin side effects no controlados
- [ ] remember/derivedStateOf donde aplica
- [ ] Flow collection con WhileSubscribed(5000)
- [ ] Sin queries N+1 (usar JOINs o @Transaction)
- [ ] Sin operaciones de DB en main thread

## 5. Seguridad

- [ ] Queries parametrizadas (no concatenacion de strings en SQL)
- [ ] Input validation en Repository antes de DB
- [ ] Sin datos sensibles en logs
- [ ] Sin API keys hardcodeadas
- [ ] ProGuard/R8 rules para release

## 6. UI/UX

- [ ] Material Design 3 consistente
- [ ] Colores del tema (no hardcodeados)
- [ ] Empty states para listas vacias
- [ ] Loading indicators durante operaciones async
- [ ] Error messages claros al usuario
- [ ] Scaffold con TopAppBar en todas las pantallas
- [ ] FAB para acciones principales de creacion
- [ ] Padding consistente (16.dp horizontal, 8.dp entre items)

## 7. Tests

- [ ] Tests unitarios para motores/engines
- [ ] Tests para validaciones de Repository
- [ ] Tests para conversiones/mappers
- [ ] Compilacion de tests exitosa

## Formato del Reporte

```markdown
## Auditoria Fase N — [Fecha]

### Resumen
- Hallazgos criticos: X
- Hallazgos medios: X
- Hallazgos menores: X

### Hallazgos

| # | Severidad | Categoria | Archivo | Descripcion | Estado |
|---|-----------|-----------|---------|-------------|--------|
| 1 | CRITICO   | Convencion| Foo.kt:23 | ID tipo Int | Pendiente |
| 2 | MEDIO     | Rendimiento| Bar.kt:45 | Column en vez de LazyColumn | Pendiente |
| 3 | MENOR     | UI | Baz.kt:12 | Texto en ingles | Pendiente |
```

## Severidades

- **CRITICO** — Rompe convenciones fundamentales, bug potencial, seguridad
- **MEDIO** — Rendimiento, arquitectura suboptima, inconsistencia
- **MENOR** — Cosmetico, textos, padding, nombres

## Post-Auditoria

1. Corregir todos los hallazgos CRITICOS antes de merge
2. Corregir MEDIOS si es posible
3. MENORES pueden quedar como deuda tecnica documentada
4. Compilar y verificar despues de correcciones
5. Instalar APK en telefono para prueba manual
