package com.mg.costeoapp.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.dao.PlatoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.TiendaDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalTiendas: Int = 0,
    val totalProductos: Int = 0,
    val totalRecetas: Int = 0,
    val totalPlatos: Int = 0,
    val productosSinPrecio: Int = 0,
    val productosConMermaAlta: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tiendaDao: TiendaDao,
    private val productoDao: ProductoDao,
    private val prefabricadoDao: PrefabricadoDao,
    private val platoDao: PlatoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    fun loadMetrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val tiendasDeferred = async { tiendaDao.countActive() }
            val productosDeferred = async { productoDao.countActive() }
            val recetasDeferred = async { prefabricadoDao.countActive() }
            val platosDeferred = async { platoDao.countActive() }
            val sinPrecioDeferred = async { productoDao.countSinPrecio() }
            val mermaAltaDeferred = async { productoDao.countConMermaAlta() }

            val results = awaitAll(
                tiendasDeferred, productosDeferred, recetasDeferred,
                platosDeferred, sinPrecioDeferred, mermaAltaDeferred
            )

            _uiState.update {
                it.copy(
                    totalTiendas = results[0],
                    totalProductos = results[1],
                    totalRecetas = results[2],
                    totalPlatos = results[3],
                    productosSinPrecio = results[4],
                    productosConMermaAlta = results[5],
                    isLoading = false
                )
            }
        }
    }
}
