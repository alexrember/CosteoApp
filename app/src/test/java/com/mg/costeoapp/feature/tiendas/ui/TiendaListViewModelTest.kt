package com.mg.costeoapp.feature.tiendas.ui

import com.mg.costeoapp.core.database.entity.Tienda
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
    private lateinit var viewModel: TiendaListViewModel

    private val tiendas = listOf(
        Tienda(id = 1, nombre = "Super Selectos"),
        Tienda(id = 2, nombre = "La Despensa")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        every { repository.getAll() } returns flowOf(tiendas)
        every { repository.search(any()) } returns flowOf(listOf(tiendas[0]))
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init carga tiendas`() = runTest(testDispatcher) {
        viewModel = TiendaListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.tiendas.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `search filtra resultados`() = runTest(testDispatcher) {
        viewModel = TiendaListViewModel(repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("super")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("super", state.searchQuery)
        assertEquals(1, state.tiendas.size)
    }

    @Test
    fun `softDelete llama al repository`() = runTest(testDispatcher) {
        viewModel = TiendaListViewModel(repository)
        coEvery { repository.softDelete(1L) } returns Unit

        viewModel.softDelete(1L)
        advanceUntilIdle()

        coVerify { repository.softDelete(1L) }
    }
}
