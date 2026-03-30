package com.mg.costeoapp.feature.tiendas.ui

import com.mg.costeoapp.core.database.entity.Tienda
import com.mg.costeoapp.feature.sync.data.SyncManager
import com.mg.costeoapp.feature.tiendas.data.TiendaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TiendaListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TiendaRepository
    private lateinit var syncManager: SyncManager
    private lateinit var viewModel: TiendaListViewModel

    private val tiendas = listOf(
        Tienda(id = 1, nombre = "Walmart", activo = true),
        Tienda(id = 2, nombre = "PriceSmart", activo = true),
        Tienda(id = 3, nombre = "Super Selectos", activo = false)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        every { repository.getAllIncludingInactive() } returns flowOf(tiendas)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init carga todas las tiendas incluyendo inactivas`() = runTest(testDispatcher) {
        viewModel = TiendaListViewModel(repository, syncManager)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.tiendas.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `toggle tienda cambia activo y sincroniza`() = runTest(testDispatcher) {
        viewModel = TiendaListViewModel(repository, syncManager)
        advanceUntilIdle()

        viewModel.toggleTienda(3L)
        advanceUntilIdle()

        coVerify { repository.update(match { it.id == 3L && it.activo == true }) }
    }
}
