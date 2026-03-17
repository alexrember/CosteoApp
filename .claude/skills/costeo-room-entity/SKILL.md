---
name: costeo-room-entity
description: Genera Entity + DAO + Repository + ViewModel + DI para CosteoApp siguiendo convenciones del proyecto (snake_case DB, soft delete, Long IDs, StateFlow)
version: 1.0.0
metadata:
  domain: database
  triggers: entity, dao, repository, room, tabla, entidad, base de datos
  role: specialist
  scope: implementation
  output-format: code
---

# CosteoApp Room Entity Generator

Genera la capa de datos completa para una nueva entidad en CosteoApp.

## Core Workflow

1. **Analizar** — Preguntar campos especificos si no fueron proporcionados
2. **Disenar** — Definir entity, relaciones FK, indices unicos
3. **Implementar** — Generar Entity → DAO → Repository → Module → Migration
4. **Registrar** — Agregar DAO en DatabaseModule, Repository en FeatureModule, entity en CosteoDatabase
5. **Validar** — Compilar proyecto para verificar

## 1. Entity (@Entity)

Package: `com.mg.costeoapp.core.database.entity`

```kotlin
@Entity(
    tableName = "{tabla_plural_snake}",
    indices = [Index(value = ["nombre"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = OtraEntidad::class,
            parentColumns = ["id"],
            childColumns = ["otra_entidad_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NombreEntidad(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    // ... campos especificos

    @ColumnInfo(name = "activo")
    val activo: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

## 2. DAO (@Dao)

Package: `com.mg.costeoapp.core.database.dao`

```kotlin
@Dao
interface NombreEntidadDao {

    @Query("SELECT * FROM {tabla} WHERE activo = 1 ORDER BY nombre ASC")
    fun getAll(): Flow<List<NombreEntidad>>

    @Query("SELECT * FROM {tabla} WHERE id = :id")
    suspend fun getById(id: Long): NombreEntidad?

    @Query("SELECT * FROM {tabla} WHERE id = :id")
    fun observeById(id: Long): Flow<NombreEntidad?>

    @Query("""
        SELECT * FROM {tabla}
        WHERE activo = 1 AND LOWER(nombre) LIKE '%' || LOWER(:query) || '%'
        ORDER BY nombre ASC
    """)
    fun search(query: String): Flow<List<NombreEntidad>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: NombreEntidad): Long

    @Update
    suspend fun update(entity: NombreEntidad)

    @Query("UPDATE {tabla} SET activo = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE {tabla} SET activo = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long = System.currentTimeMillis())
}
```

## 3. Repository

Package: `com.mg.costeoapp.feature.{feature}/data`

```kotlin
// Interface
interface NombreEntidadRepository {
    fun getAll(): Flow<List<NombreEntidad>>
    suspend fun getById(id: Long): NombreEntidad?
    fun search(query: String): Flow<List<NombreEntidad>>
    suspend fun insert(entity: NombreEntidad): Result<Long>
    suspend fun update(entity: NombreEntidad): Result<Unit>
    suspend fun softDelete(id: Long)
    suspend fun restore(id: Long)
}

// Implementation
@Singleton
class NombreEntidadRepositoryImpl @Inject constructor(
    private val dao: NombreEntidadDao
) : NombreEntidadRepository {
    // insert/update retornan Result<T> con validacion de duplicados
    // update hace .copy(updatedAt = System.currentTimeMillis())
}
```

## 4. Hilt Modules

```kotlin
// DatabaseModule: agregar @Provides para el DAO
@Provides
fun provideNombreEntidadDao(database: CosteoDatabase): NombreEntidadDao =
    database.nombreEntidadDao()

// FeatureModule: agregar @Binds para el Repository
@Module
@InstallIn(SingletonComponent::class)
abstract class NombreEntidadModule {
    @Binds @Singleton
    abstract fun bindRepository(impl: NombreEntidadRepositoryImpl): NombreEntidadRepository
}
```

## 5. Migration

```kotlin
// En CosteoDatabase companion object
val MIGRATION_N_N1 = object : Migration(N, N+1) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS {tabla} (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombre TEXT NOT NULL,
                activo INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_{tabla}_nombre ON {tabla}(nombre)")
    }
}
```

## MUST DO
- Tabla en snake_case plural: `Tienda` → `tiendas`, `ProductoTienda` → `productos_tiendas`
- Propiedades Kotlin en camelCase
- @ColumnInfo(name = "snake_case") en TODOS los campos
- IDs siempre Long con autoGenerate
- Timestamps siempre Long (epoch millis)
- Soft delete con `activo = false`, nunca @Delete fisico
- Flow para queries de lectura, suspend fun para mutaciones
- Result<T> para insert/update con validacion de duplicados
- Registrar en CosteoDatabase abstract fun + DatabaseModule @Provides

## MUST NOT DO
- Usar Int para IDs
- Usar UUID
- Usar String o Date para timestamps
- Usar LiveData
- Usar @Delete fisico para entidades principales
- Usar OnConflictStrategy.REPLACE (usar ABORT + validacion manual)
- Olvidar agregar la migration
- Olvidar incrementar version en @Database

## Checklist
- [ ] Entity con campos base (id, activo, createdAt, updatedAt)
- [ ] @ColumnInfo en snake_case para todos los campos
- [ ] DAO con getAll, getById, search, insert, update, softDelete, restore
- [ ] Repository interface + Impl con Result<T>
- [ ] DAO registrado en DatabaseModule
- [ ] Repository binding en FeatureModule
- [ ] Entity y DAO registrados en CosteoDatabase
- [ ] Migration creada y registrada
- [ ] Version de DB incrementada
- [ ] Proyecto compila sin errores
