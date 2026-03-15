package com.mg.costeoapp.feature.tiendas.ui

import androidx.lifecycle.SavedStateHandle
import com.mg.costeoapp.core.database.entity.Tienda
import com.mg.costeoapp.feature.tiendas.data.TiendaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TiendaFormViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        tiendaId: Long? = null,
        repository: TiendaRepository = FakeTiendaRepository()
    ): TiendaFormViewModel {
        val savedState = SavedStateHandle().apply {
            if (tiendaId != null) set("tiendaId", tiendaId)
        }
        return TiendaFormViewModel(repository, savedState)
    }

    @Test
    fun `modo crear inicia vacio`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value
        assertFalse(state.isEditMode)
        assertEquals("", state.nombre)
    }

    @Test
    fun `modo editar carga tienda`() = runTest {
        val tienda = Tienda(id = 1, nombre = "Test")
        val repo = FakeTiendaRepository(tiendas = mutableListOf(tienda))

        val viewModel = createViewModel(tiendaId = 1L, repository = repo)

        val state = viewModel.uiState.value
        assertTrue(state.isEditMode)
        assertEquals("Test", state.nombre)
    }

    @Test
    fun `validacion falla con nombre corto`() = runTest {
        val viewModel = createViewModel()
        viewModel.onNombreChanged("A")
        viewModel.save()

        assertTrue(viewModel.uiState.value.fieldErrors.containsKey("nombre"))
    }

    @Test
    fun `save exitoso en modo crear`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onNombreChanged("Nueva Tienda")
        viewModel.save()

        assertTrue(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun `save muestra error de nombre duplicado`() = runTest(testDispatcher) {
        val repo = FakeTiendaRepository(
            tiendas = mutableListOf(Tienda(id = 1, nombre = "Duplicada"))
        )
        val viewModel = createViewModel(repository = repo)
        viewModel.onNombreChanged("Duplicada")
        viewModel.save()

        val state = viewModel.uiState.value
        assertFalse(state.saveSuccess)
        assertEquals("Ya existe una tienda con ese nombre", state.error)
    }

    @Test
    fun `onNombreChanged limpia error de nombre`() = runTest {
        val viewModel = createViewModel()
        viewModel.onNombreChanged("A")
        viewModel.save()
        assertTrue(viewModel.uiState.value.fieldErrors.containsKey("nombre"))

        viewModel.onNombreChanged("AB")
        assertFalse(viewModel.uiState.value.fieldErrors.containsKey("nombre"))
    }
}

private class FakeTiendaRepository(
    private val tiendas: MutableList<Tienda> = mutableListOf()
) : TiendaRepository {
    private var nextId = 100L

    override fun getAll(): Flow<List<Tienda>> = flowOf(tiendas.filter { it.activo })
    override suspend fun getById(id: Long): Tienda? = tiendas.find { it.id == id }
    override fun search(query: String): Flow<List<Tienda>> = flowOf(
        tiendas.filter { it.activo && it.nombre.contains(query, ignoreCase = true) }
    )
    override suspend fun insert(tienda: Tienda): Result<Long> {
        val existing = tiendas.find { it.nombre.equals(tienda.nombre, ignoreCase = true) && it.activo }
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Ya existe una tienda con ese nombre"))
        }
        val id = nextId++
        tiendas.add(tienda.copy(id = id))
        return Result.success(id)
    }
    override suspend fun update(tienda: Tienda): Result<Unit> {
        val idx = tiendas.indexOfFirst { it.id == tienda.id }
        if (idx >= 0) tiendas[idx] = tienda
        return Result.success(Unit)
    }
    override suspend fun softDelete(id: Long) {
        val idx = tiendas.indexOfFirst { it.id == id }
        if (idx >= 0) tiendas[idx] = tiendas[idx].copy(activo = false)
    }
    override suspend fun restore(id: Long) {
        val idx = tiendas.indexOfFirst { it.id == id }
        if (idx >= 0) tiendas[idx] = tiendas[idx].copy(activo = true)
    }
}
