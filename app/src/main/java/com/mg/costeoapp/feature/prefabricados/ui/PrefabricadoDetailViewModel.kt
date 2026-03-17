package com.mg.costeoapp.feature.prefabricados.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.domain.engine.PricingEngine
import com.mg.costeoapp.core.domain.model.FuentePrecio
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.feature.prefabricados.data.PrefabricadoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.roundToLong
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrefabricadoDetailViewModel @Inject constructor(
    private val repository: PrefabricadoRepository,
    private val pricingEngine: PricingEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrefabricadoDetailUiState())
    val uiState: StateFlow<PrefabricadoDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val prefabricadoId: Long = savedStateHandle.get<Long>("recetaId") ?: 0L
    private var observeJob: Job? = null
    private var variantesJob: Job? = null

    init {
        if (prefabricadoId != 0L) {
            loadDetail()
        }
    }

    fun refresh() {
        loadDetail()
    }

    private fun loadDetail() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            try {
                repository.observeConIngredientes(prefabricadoId).collect { data ->
                    if (data != null) {
                        val costeo = repository.calculateCost(prefabricadoId)
                        val nutricion = repository.calculateNutricion(prefabricadoId)

                        val ingredientesCosto = data.ingredientes.map { item ->
                            val precio = pricingEngine.resolvePrice(item.producto.id)
                            val costoTotal = if (precio.precioUnitario != null && item.producto.cantidadPorEmpaque > 0) {
                                val ppu = precio.precioUnitario.toDouble() / item.producto.cantidadPorEmpaque
                                val costo = ppu * item.ingrediente.cantidadUsada
                                val merma = item.producto.factorMerma
                                if (merma in 1..99) {
                                    (costo / (1.0 - merma / 100.0)).roundToLong()
                                } else {
                                    costo.roundToLong()
                                }
                            } else null

                            IngredienteCostoDetalle(
                                productoNombre = item.producto.nombre,
                                cantidadUsada = item.ingrediente.cantidadUsada,
                                unidadUsada = item.ingrediente.unidadUsada,
                                precioUnitario = precio.precioUnitario,
                                costoTotal = costoTotal,
                                fuente = precio.fuente
                            )
                        }

                        _uiState.update {
                            it.copy(
                                prefabricado = data.prefabricado,
                                ingredientes = data.ingredientes,
                                ingredientesCosto = ingredientesCosto,
                                costeo = costeo,
                                nutricion = nutricion,
                                isLoading = false,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar receta") }
            }
        }

        variantesJob?.cancel()
        variantesJob = viewModelScope.launch {
            repository.getVariantes(prefabricadoId).collect { variantes ->
                _uiState.update { it.copy(variantes = variantes) }
            }
        }
    }

    fun softDelete() {
        viewModelScope.launch {
            repository.softDelete(prefabricadoId)
            _events.send(UiEvent.NavigateBack)
        }
    }
}
