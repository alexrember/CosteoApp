Genera un formulario Jetpack Compose para CosteoApp para la entidad: $ARGUMENTS

Sigue EXACTAMENTE estos patrones del proyecto:

## Estructura del Form
- Reutiliza el mismo Screen para crear y editar (detectar por ID nullable)
- Si `entityId != null` → modo edicion (cargar datos existentes)
- Si `entityId == null` → modo creacion

## ViewModel del Form
```kotlin
@HiltViewModel
class {Entidad}FormViewModel @Inject constructor(
    private val repository: {Entidad}Repository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val entityId: Long? = savedStateHandle.get<Long>("{entidad}Id")
    val isEditing = entityId != null

    private val _uiState = MutableStateFlow({Entidad}FormUiState())
    val uiState: StateFlow<{Entidad}FormUiState> = _uiState.asStateFlow()

    init {
        if (isEditing) loadEntity()
    }
}
```

## UiState del Form
```kotlin
data class {Entidad}FormUiState(
    val nombre: String = "",
    // ... campos editables
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)
```

## Composable del Form
- Scaffold con TopAppBar (titulo "Crear {Entidad}" o "Editar {Entidad}")
- Column con verticalScroll dentro de padding 16.dp
- OutlinedTextField para cada campo editable
- Validacion: nombre no vacio (minimo)
- Boton "Guardar" al final (FilledButton con colores del tema)
- Al guardar exitosamente: callback `onSaved()` para navegar atras

## Campos especiales
- Numeros (Double): usar `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)`
- Booleanos: Switch o Checkbox
- Seleccion (ej: unidad de medida): ExposedDropdownMenuBox
- Campos opcionales: mostrar hint "(Opcional)" en label

## Reglas estrictas
- Validacion en ViewModel, no en el Composable
- NO bloquear el UI durante guardado (mostrar loading indicator)
- updatedAt se actualiza automaticamente al guardar
- Textos en espanol
