Genera una entidad Room completa para CosteoApp con el nombre: $ARGUMENTS

Sigue EXACTAMENTE estos patrones del proyecto:

## 1. Entity (@Entity)
- Package: `com.costeoapp.core.database.entity`
- Tabla en snake_case (plural): ej. "Tienda" → tableName = "tiendas"
- Propiedades en camelCase
- @ColumnInfo con name en snake_case
- SIEMPRE incluir estos campos base:
  ```kotlin
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  @ColumnInfo(name = "activo") val activo: Boolean = true,
  @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
  @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
  ```
- Preguntame los campos especificos que necesita la entidad antes de generar

## 2. DAO (@Dao)
- Package: `com.costeoapp.core.database.dao`
- Nombre: `{Entidad}Dao`
- SIEMPRE incluir:
  - `@Query("SELECT * FROM {tabla} WHERE activo = 1 ORDER BY nombre ASC")` → Flow<List<Entidad>>
  - `@Query("SELECT * FROM {tabla} WHERE id = :id")` → Flow<Entidad?>
  - `@Insert(onConflict = OnConflictStrategy.REPLACE)` → suspend fun insert(): Long
  - `@Update` → suspend fun update()
  - Soft delete: `@Query("UPDATE {tabla} SET activo = 0, updated_at = :now WHERE id = :id")` → suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())
- Return types: Flow para queries, suspend fun para mutations

## 3. Repository
- Package: `com.costeoapp.feature.{feature}/data`
- Interface `{Entidad}Repository` con metodos que mapean al DAO
- `{Entidad}RepositoryImpl` con `@Inject constructor(private val dao: {Entidad}Dao)`
- Anotado con `@Singleton`

## 4. ViewModel
- Package: `com.costeoapp.feature.{feature}`
- `@HiltViewModel` con `@Inject constructor`
- UiState como data class dentro del ViewModel o en archivo separado
- Usar `StateFlow` (no LiveData)
- Patron: `private val _uiState = MutableStateFlow(UiState())` + `val uiState: StateFlow<UiState> = _uiState.asStateFlow()`

## 5. Registrar en DatabaseModule
- Agregar el DAO como @Provides en `com.costeoapp.core.di.DatabaseModule`

## Reglas estrictas
- NO usar LiveData (solo Flow/StateFlow)
- NO agregar comentarios innecesarios
- Soft delete SIEMPRE (nunca @Delete fisico para entidades principales)
- IDs tipo Long (no Int, no UUID)
- Timestamps como Long (epoch millis), no String ni Date
