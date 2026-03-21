package com.mg.costeoapp.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.dao.InventarioDao
import com.mg.costeoapp.core.database.dao.PlatoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoIngredienteDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.TiendaDao
import com.mg.costeoapp.feature.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val productosConStockBajo: Int = 0,
    val recetasConIngredientesInactivos: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tiendaDao: TiendaDao,
    private val productoDao: ProductoDao,
    private val prefabricadoDao: PrefabricadoDao,
    private val platoDao: PlatoDao,
    private val inventarioDao: InventarioDao,
    private val prefabricadoIngredienteDao: PrefabricadoIngredienteDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    fun refresh() = loadMetrics()

    fun loadMetrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val threshold = settingsRepository.stockBajoThreshold.first()

            val tiendasDeferred = async { tiendaDao.countActive() }
            val productosDeferred = async { productoDao.countActive() }
            val recetasDeferred = async { prefabricadoDao.countActive() }
            val platosDeferred = async { platoDao.countActive() }
            val sinPrecioDeferred = async { productoDao.countSinPrecio() }
            val mermaAltaDeferred = async { productoDao.countConMermaAlta() }
            val stockBajoDeferred = async { inventarioDao.countProductosConStockBajo(threshold) }
            val ingredientesInactivosDeferred = async { prefabricadoIngredienteDao.countRecetasConIngredientesInactivos() }

            val results = awaitAll(
                tiendasDeferred, productosDeferred, recetasDeferred,
                platosDeferred, sinPrecioDeferred, mermaAltaDeferred,
                stockBajoDeferred, ingredientesInactivosDeferred
            )

            _uiState.update {
                it.copy(
                    totalTiendas = results[0],
                    totalProductos = results[1],
                    totalRecetas = results[2],
                    totalPlatos = results[3],
                    productosSinPrecio = results[4],
                    productosConMermaAlta = results[5],
                    productosConStockBajo = results[6],
                    recetasConIngredientesInactivos = results[7],
                    isLoading = false
                )
            }
        }
    }
}
