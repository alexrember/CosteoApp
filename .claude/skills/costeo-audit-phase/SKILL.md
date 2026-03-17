---
name: costeo-audit-phase
description: Auditoria multi-experto al completar cada fase de CosteoApp — lanza 6 agentes en paralelo con diferentes perfiles
version: 2.0.0
metadata:
  domain: quality
  triggers: auditoria, audit, review, fase completa, revision, calidad, hallazgos
  role: reviewer
  scope: review
  output-format: documentation
---

# CosteoApp Auditoria Multi-Experto por Fase

Al completar cada fase, lanzar 6 subagentes en paralelo con perfiles especializados.
Cada agente revisa el codigo de la fase completada y reporta hallazgos priorizados.

## Cuando Ejecutar

Al terminar cada fase (Fase 0-8) antes de pasar a la siguiente. Lanzar con: "auditar fase N" o "lanzar auditoria".

## Agentes a Lanzar (TODOS en paralelo)

### 1. QA Expert
- Cobertura de tests, edge cases, regression risks, missing negative tests
- Archivos: tests existentes + codigo fuente de la fase

### 2. Security Expert
- OWASP Mobile Top 10, SQL injection, data leakage, input sanitization
- Archivos: codigo fuente + AndroidManifest.xml + build.gradle.kts

### 3. Android Expert
- Compose best practices, lifecycle, navigation, Hilt/DI, performance, memory leaks
- Archivos: Screens, ViewModels, NavGraph

### 4. Architecture Expert
- Clean Architecture, SOLID, feature modularity, code duplication, scalability
- Archivos: todo el codigo fuente de la fase

### 5. SQL/Database Expert
- Schema design, index strategy, query performance, migration, transaction safety
- Archivos: entities, DAOs, migrations, repositories

### 6. UX Expert
- User flow friction, error handling UX, empty states, loading states, accessibility
- Archivos: Screens, components, NavGraph

## Instrucciones para cada agente

```
You are a [ROLE] expert auditing Phase [N] of CosteoApp (Android Kotlin + Jetpack Compose + Room).
Read ALL relevant files in the phase directories.
Provide a SHORT prioritized list (max 10 items) with severity (HIGH/MEDIUM/LOW).
Research only — do NOT edit files.
```

## Post-Auditoria

1. Consolidar hallazgos de los 6 agentes en una tabla unica
2. Deduplicar (mismo hallazgo reportado por multiples agentes = mas urgente)
3. Presentar resumen al usuario
4. Corregir todos los HIGH antes de pasar a siguiente fase
5. MEDIUM corregir si es rapido, sino agregar a PENDIENTES-MCP.md
6. LOW documentar como deuda tecnica

## Reglas importantes

- NUNCA correr tests en telefono fisico (usar `testDeviceDebugAndroidTest` para managed device)
- NUNCA incluir Co-Authored-By en commits
- Despues de corregir, compilar + correr unit tests
- Instalar APK en telefono: `adb -s 58251FDCR00D6P install -r app/build/outputs/apk/debug/app-debug.apk`
