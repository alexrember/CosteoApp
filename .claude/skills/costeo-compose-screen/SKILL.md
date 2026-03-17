---
name: costeo-compose-screen
description: Genera Screen + ViewModel + Form + Navigation para CosteoApp con Material Design 3, StateFlow, y navegacion por callbacks
version: 1.0.0
metadata:
  domain: ui
  triggers: screen, pantalla, formulario, form, vista, compose, ui
  role: specialist
  scope: implementation
  output-format: code
---

# CosteoApp Compose Screen Generator

Genera pantallas completas con ViewModel, UiState, y navegacion integrada.

## Core Workflow

1. **Analizar** — Tipo de pantalla (List, Form, Detail) y entidad asociada
2. **Disenar** — UiState, eventos, callbacks de navegacion
3. **Implementar** — Screen + ViewModel + UiState + Navigation route
4. **Integrar** — Agregar a NavGraph.kt
5. **Validar** — Compilar proyecto

## Tipos de Pantalla

### List Screen
- LazyColumn con items
- FloatingActionButton para crear
- SearchBar con debounce (300ms)
- Swipe-to-delete (soft delete) con confirmacion
- Empty state cuando no hay items
- SnackbarHost para errores

### Form Screen (Crear/Editar)
- Mismo Screen para crear y editar (detectar por ID nullable)
- SavedStateHandle para recibir entityId
- OutlinedTextField por campo
- Validacion en ViewModel (no en Composable)
- Boton "Guardar" con loading state
- Channel-based events para navegacion post-guardado

### Detail Screen
- Datos de solo lectura
- Opciones editar/eliminar en TopAppBar
- Navegacion a sub-pantallas relacionadas

## ViewModel Pattern

```kotlin
@HiltViewModel
class NombreViewModel @Inject constructor(
    private val repository: NombreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NombreUiState())
    val uiState: StateFlow<NombreUiState> = _uiState.asStateFlow()

    // Para eventos one-shot (navegacion, snackbar)
    private val _events = Channel<NombreEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getAll().collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }
}
```

## Form ViewModel Pattern

```kotlin
@HiltViewModel
class NombreFormViewModel @Inject constructor(
    private val repository: NombreRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val entityId: Long? = savedStateHandle.get<Long>("entityId")
    val isEditing = entityId != null

    private val _uiState = MutableStateFlow(NombreFormUiState())
    val uiState: StateFlow<NombreFormUiState> = _uiState.asStateFlow()

    private val _events = Channel<FormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { if (isEditing) loadEntity() }

    fun onSave() {
        viewModelScope.launch {
            if (!validate()) return@launch
            _uiState.update { it.copy(isSaving = true) }
            val result = if (isEditing) {
                repository.update(buildEntity())
            } else {
                repository.insert(buildEntity()).map { }
            }
            result.onSuccess { _events.send(FormEvent.Saved) }
                   .onFailure { _uiState.update { s -> s.copy(error = it.message, isSaving = false) } }
        }
    }
}

sealed class FormEvent {
    data object Saved : FormEvent()
}
```

## Screen Composable Pattern

```kotlin
@Composable
fun NombreScreen(
    viewModel: NombreViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToCreate: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Colectar eventos one-shot
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NombreEvent.ShowError -> { /* snackbar */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Titulo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Default.Add, "Crear")
            }
        }
    ) { paddingValues ->
        // Content con paddingValues
    }
}
```

## UiState Pattern

```kotlin
data class NombreUiState(
    val items: List<Entidad> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class NombreFormUiState(
    val nombre: String = "",
    // campos editables con defaults vacios
    val isSaving: Boolean = false,
    val error: String? = null
)
```

## Navigation Integration

```kotlin
// En NavGraph.kt, agregar composable:
composable<Screen.NombreList> {
    NombreListScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToDetail = { id -> navController.navigate(Screen.NombreDetail(id)) },
        onNavigateToCreate = { navController.navigate(Screen.NombreForm()) }
    )
}
composable<Screen.NombreForm> {
    NombreFormScreen(
        onNavigateBack = { navController.popBackStack() },
        onSaved = { navController.popBackStack() }
    )
}
```

## MUST DO
- StateFlow + collectAsStateWithLifecycle (import de androidx.lifecycle.compose)
- Navegacion por callbacks, nunca navController directo en Screen
- Channel para eventos one-shot (guardado, errores transitorios)
- Textos en espanol
- Padding 16.dp horizontal, 8.dp entre items
- Material Design 3 (MaterialTheme.colorScheme)
- TopAppBar con titulo descriptivo
- Empty state cuando lista esta vacia
- Loading indicator mientras carga

## MUST NOT DO
- Usar LiveData
- Acceder navController dentro del Screen
- Validar en el Composable (validar en ViewModel)
- Hardcodear colores (usar MaterialTheme.colorScheme)
- Olvidar collectAsStateWithLifecycle (no usar collectAsState)
- Crear ViewModel sin @HiltViewModel
- Usar remember para estado que debe sobrevivir config changes

## Checklist
- [ ] ViewModel con @HiltViewModel + StateFlow
- [ ] UiState como data class con defaults
- [ ] Screen con callbacks de navegacion
- [ ] collectAsStateWithLifecycle para colectar state
- [ ] Channel para eventos one-shot
- [ ] Scaffold con TopAppBar
- [ ] Ruta agregada en NavGraph.kt
- [ ] Textos en espanol
- [ ] Proyecto compila sin errores
