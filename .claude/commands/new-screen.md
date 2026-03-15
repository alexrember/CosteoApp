Genera una pantalla Jetpack Compose completa para CosteoApp con el nombre: $ARGUMENTS

Sigue EXACTAMENTE estos patrones del proyecto:

## 1. Screen Composable
- Package: `com.costeoapp.feature.{feature}`
- Nombre: `{Nombre}Screen.kt`
- Usar `CosteoAppTheme` y `MaterialTheme` (Material3)
- Parametros del composable:
  ```kotlin
  @Composable
  fun {Nombre}Screen(
      viewModel: {Nombre}ViewModel = hiltViewModel(),
      onNavigateBack: () -> Unit = {},
      // otros callbacks de navegacion segun necesidad
  )
  ```
- Usar `val uiState by viewModel.uiState.collectAsStateWithLifecycle()`
- Scaffold con TopAppBar si tiene titulo
- Colores del tema: `MaterialTheme.colorScheme.primary` (verde #0D7C66), `.secondary` (naranja #F57C00)

## 2. ViewModel (@HiltViewModel)
- Package: `com.costeoapp.feature.{feature}`
- Nombre: `{Nombre}ViewModel.kt`
- Patron:
  ```kotlin
  @HiltViewModel
  class {Nombre}ViewModel @Inject constructor(
      private val repository: {Entidad}Repository
  ) : ViewModel() {
      private val _uiState = MutableStateFlow({Nombre}UiState())
      val uiState: StateFlow<{Nombre}UiState> = _uiState.asStateFlow()
  }
  ```
- UiState como data class con valores por defecto
- Usar `viewModelScope.launch` para operaciones async
- Colectar Flows del repository con `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())`

## 3. Navigation Route
- Agregar ruta en `com.costeoapp.navigation.Screen`
- Agregar `composable()` en `NavGraph.kt`

## 4. Tipos de pantalla comunes
- **List**: LazyColumn con items, FAB para crear, swipe to soft-delete
- **Form**: Campos TextField, boton guardar, validacion basica
- **Detail**: Datos de solo lectura con opciones editar/eliminar

## Reglas estrictas
- NO usar LiveData (solo StateFlow + collectAsStateWithLifecycle)
- Navegacion con callbacks (onNavigateBack, onNavigateTo), NO acceder a navController directamente en el Screen
- Padding consistente: 16.dp horizontal, 8.dp entre items
- Textos en espanol
- Import `androidx.lifecycle.compose.collectAsStateWithLifecycle`
