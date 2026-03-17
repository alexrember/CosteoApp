---
name: costeo-supabase-sync
description: Sincronizacion offline-first Room↔Supabase para CosteoApp — conflict resolution, queue de sync, auth
version: 1.0.0
metadata:
  domain: sync
  triggers: supabase, sync, sincronizar, cloud, backup, online, offline
  role: specialist
  scope: implementation
  output-format: code
---

# CosteoApp Supabase Sync

Guia para implementar sincronizacion bidireccional Room ↔ Supabase (Fase 7+).

## Arquitectura Offline-First

```
Room (fuente de verdad local)
  ↕ SyncManager
Supabase PostgreSQL (cloud backup + multi-device)
```

Principio: **Room siempre es la fuente de verdad**. El usuario nunca espera por red para trabajar.

## Estrategia de Sync

### Campos adicionales para sync (agregar a cada Entity)

```kotlin
@ColumnInfo(name = "sync_status")
val syncStatus: Int = SyncStatus.PENDING,  // 0=SYNCED, 1=PENDING, 2=CONFLICT

@ColumnInfo(name = "remote_id")
val remoteId: String? = null,  // UUID de Supabase

@ColumnInfo(name = "last_synced_at")
val lastSyncedAt: Long? = null
```

### SyncStatus

```kotlin
object SyncStatus {
    const val SYNCED = 0
    const val PENDING = 1
    const val CONFLICT = 2
}
```

## SyncManager

```kotlin
@Singleton
class SyncManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val database: CosteoDatabase,
    private val connectivityManager: ConnectivityManager
) {
    // Push: Room → Supabase
    suspend fun pushPendingChanges() {
        val pending = getAllPendingEntities()
        pending.forEach { entity ->
            try {
                upsertToSupabase(entity)
                markAsSynced(entity)
            } catch (e: Exception) {
                // No bloquear; reintentar en proximo sync
            }
        }
    }

    // Pull: Supabase → Room
    suspend fun pullRemoteChanges(lastSyncTimestamp: Long) {
        val remote = fetchChangedSince(lastSyncTimestamp)
        remote.forEach { remoteEntity ->
            val local = findByRemoteId(remoteEntity.id)
            when {
                local == null -> insertLocal(remoteEntity)
                local.updatedAt < remoteEntity.updatedAt -> updateLocal(remoteEntity)
                local.updatedAt > remoteEntity.updatedAt -> pushLocal(local)
                else -> { /* sin cambios */ }
            }
        }
    }
}
```

## Conflict Resolution

Estrategia: **Last Write Wins** basada en `updated_at`.

```
Si local.updatedAt > remote.updatedAt → local gana, push al server
Si local.updatedAt < remote.updatedAt → remote gana, update local
Si local.updatedAt == remote.updatedAt → sin conflicto
```

Para conflictos complejos (edicion simultanea):
- Marcar syncStatus = CONFLICT
- Mostrar al usuario para resolucion manual

## Trigger de Sync

```kotlin
// WorkManager para sync periodico
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        syncManager.pushPendingChanges()
        syncManager.pullRemoteChanges(lastSyncTimestamp)
        return Result.success()
    }
}

// Sync al abrir app + cada 15 minutos con WiFi
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
    .build()
```

## Schema Supabase (PostgreSQL)

Mapear cada tabla Room a Supabase con:
- `id` UUID primary key (remote_id en Room)
- `user_id` UUID references auth.users
- Mismos campos que Room pero en snake_case
- `created_at` y `updated_at` como timestamptz
- RLS (Row Level Security) por user_id

## MUST DO
- Room siempre es fuente de verdad local
- Sync en background con WorkManager
- Manejar falta de red sin errores visibles
- RLS en Supabase (cada usuario ve solo sus datos)
- Retry con backoff exponencial
- Marcar syncStatus en cada mutacion local

## MUST NOT DO
- Bloquear UI esperando sync
- Asumir que hay red disponible
- Sincronizar sin autenticacion
- Borrar datos locales si falla sync
- Usar sync bidireccional sin conflict resolution
- Exponer credenciales de Supabase en el APK (usar BuildConfig)

## Checklist
- [ ] Campos sync agregados a entities (syncStatus, remoteId, lastSyncedAt)
- [ ] SyncManager con push/pull
- [ ] Conflict resolution (Last Write Wins)
- [ ] WorkManager para sync periodico
- [ ] RLS configurado en Supabase
- [ ] Auth integrado (Fase 7)
- [ ] Tests de sync con mock server
