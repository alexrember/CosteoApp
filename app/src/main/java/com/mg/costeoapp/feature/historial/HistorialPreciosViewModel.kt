package com.mg.costeoapp.feature.historial

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.dao.PrecioHistoricoRaw
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistorialUiState(
    val productoNombre: String = "",
    val precios: List<PrecioHistoricoRaw> = emptyList(),
    val precioMin: Long? = null,
    val precioMax: Long? = null,
    val precioActual: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HistorialPreciosViewModel @Inject constructor(
    private val productoTiendaDao: ProductoTiendaDao,
    private val productoDao: ProductoDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    private val productoId: Long = savedStateHandle.get<Long>("productoId") ?: 0L

    init {
        if (productoId != 0L) loadHistorial()
    }

    private fun loadHistorial() {
        viewModelScope.launch {
            val producto = productoDao.getById(productoId)
            val precios = productoTiendaDao.getHistorialPrecios(productoId)

            _uiState.update {
                it.copy(
                    productoNombre = producto?.nombre ?: "",
                    precios = precios,
                    precioMin = precios.minByOrNull { p -> p.precio }?.precio,
                    precioMax = precios.maxByOrNull { p -> p.precio }?.precio,
                    precioActual = precios.firstOrNull()?.precio,
                    isLoading = false
                )
            }
        }
    }
}
