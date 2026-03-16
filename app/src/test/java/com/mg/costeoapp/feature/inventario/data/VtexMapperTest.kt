package com.mg.costeoapp.feature.inventario.data

import com.mg.costeoapp.feature.inventario.data.dto.VtexCommertialOffer
import com.mg.costeoapp.feature.inventario.data.dto.VtexImage
import com.mg.costeoapp.feature.inventario.data.dto.VtexItem
import com.mg.costeoapp.feature.inventario.data.dto.VtexProduct
import com.mg.costeoapp.feature.inventario.data.dto.VtexSeller
import com.mg.costeoapp.feature.inventario.data.mapper.toStoreSearchResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VtexMapperTest {

    @Test
    fun `producto completo mapea correctamente`() {
        val product = VtexProduct(
            productName = "Leche Entera",
            brand = "Dos Pinos",
            items = listOf(
                VtexItem(
                    nameComplete = "Leche Entera 946ml",
                    ean = "7441001600012",
                    measurementUnit = "ml",
                    unitMultiplier = 946.0,
                    images = listOf(VtexImage("https://img.com/leche.jpg")),
                    sellers = listOf(
                        VtexSeller(
                            sellerName = "Walmart SV",
                            commertialOffer = VtexCommertialOffer(
                                price = 1.99,
                                listPrice = 2.49,
                                isAvailable = true
                            )
                        )
                    )
                )
            )
        )

        val results = product.toStoreSearchResults()
        assertEquals(1, results.size)

        val r = results[0]
        assertEquals("Walmart SV", r.storeName)
        assertEquals("Leche Entera", r.productName)
        assertEquals("Dos Pinos", r.brand)
        assertEquals("7441001600012", r.ean)
        assertEquals(199L, r.price)
        assertEquals(249L, r.listPrice)
        assertTrue(r.isAvailable)
        assertEquals("https://img.com/leche.jpg", r.imageUrl)
        assertEquals("ml", r.measurementUnit)
        assertEquals(946.0, r.unitMultiplier!!, 0.01)
        assertEquals("walmart_vtex", r.source)
    }

    @Test
    fun `producto con items null retorna lista vacia`() {
        val product = VtexProduct(productName = "Test", items = null)
        val results = product.toStoreSearchResults()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `producto con items vacio retorna lista vacia`() {
        val product = VtexProduct(productName = "Test", items = emptyList())
        val results = product.toStoreSearchResults()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `item con sellers null retorna lista vacia`() {
        val product = VtexProduct(
            productName = "Test",
            items = listOf(VtexItem(sellers = null))
        )
        val results = product.toStoreSearchResults()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `item con sellers vacio retorna lista vacia`() {
        val product = VtexProduct(
            productName = "Test",
            items = listOf(VtexItem(sellers = emptyList()))
        )
        val results = product.toStoreSearchResults()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `seller sin commertialOffer se omite`() {
        val product = VtexProduct(
            productName = "Test",
            items = listOf(
                VtexItem(
                    sellers = listOf(
                        VtexSeller(sellerName = "Store", commertialOffer = null)
                    )
                )
            )
        )
        val results = product.toStoreSearchResults()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `productName null usa nameComplete como fallback`() {
        val product = VtexProduct(
            productName = null,
            items = listOf(
                VtexItem(
                    nameComplete = "Nombre Completo Item",
                    sellers = listOf(
                        VtexSeller(
                            commertialOffer = VtexCommertialOffer(price = 1.0)
                        )
                    )
                )
            )
        )
        val results = product.toStoreSearchResults()
        assertEquals("Nombre Completo Item", results[0].productName)
    }

    @Test
    fun `productName y nameComplete null usa Sin nombre`() {
        val product = VtexProduct(
            productName = null,
            items = listOf(
                VtexItem(
                    nameComplete = null,
                    sellers = listOf(
                        VtexSeller(
                            commertialOffer = VtexCommertialOffer(price = 1.0)
                        )
                    )
                )
            )
        )
        val results = product.toStoreSearchResults()
        assertEquals("Sin nombre", results[0].productName)
    }

    @Test
    fun `sellerName null usa Walmart SV por defecto`() {
        val product = VtexProduct(
            productName = "Test",
            items = listOf(
                VtexItem(
                    sellers = listOf(
                        VtexSeller(
                            sellerName = null,
                            commertialOffer = VtexCommertialOffer(price = 1.0)
                        )
                    )
                )
            )
        )
        val results = product.toStoreSearchResults()
        assertEquals("Walmart SV", results[0].storeName)
    }

    @Test
    fun `price null mapea a null`() {
        val product = VtexProduct(
            productName = "Test",
            items = listOf(
                VtexItem(
                    sellers = listOf(
                        VtexSeller(
                            commertialOffer = VtexCommertialOffer(price = null)
                        )
                    )
                )
            )
        )
        val results = product.toStoreSearchResults()
        assertNull(results[0].price)
    }

    @Test
    fun `isAvailable null mapea a false`() {
        val product = VtexProduct(
            productName = "Test",
            items = listOf(
                VtexItem(
                    sellers = listOf(
                        VtexSeller(
                            commertialOffer = VtexCommertialOffer(isAvailable = null)
                        )
                    )
                )
            )
        )
        val results = product.toStoreSearchResults()
        assertFalse(results[0].isAvailable)
    }

    @Test
    fun `images null mapea imageUrl a null`() {
        val product = VtexProduct(
            productName = "Test",
            items = listOf(
                VtexItem(
                    images = null,
                    sellers = listOf(
                        VtexSeller(
                            commertialOffer = VtexCommertialOffer(price = 1.0)
                        )
                    )
                )
            )
        )
        val results = product.toStoreSearchResults()
        assertNull(results[0].imageUrl)
    }

    @Test
    fun `images vacio mapea imageUrl a null`() {
        val product = VtexProduct(
            productName = "Test",
            items = listOf(
                VtexItem(
                    images = emptyList(),
                    sellers = listOf(
                        VtexSeller(
                            commertialOffer = VtexCommertialOffer(price = 1.0)
                        )
                    )
                )
            )
        )
        val results = product.toStoreSearchResults()
        assertNull(results[0].imageUrl)
    }

    @Test
    fun `multiples items y sellers generan multiples resultados`() {
        val product = VtexProduct(
            productName = "Test",
            items = listOf(
                VtexItem(
                    sellers = listOf(
                        VtexSeller(
                            sellerName = "Store A",
                            commertialOffer = VtexCommertialOffer(price = 1.0)
                        ),
                        VtexSeller(
                            sellerName = "Store B",
                            commertialOffer = VtexCommertialOffer(price = 2.0)
                        )
                    )
                ),
                VtexItem(
                    sellers = listOf(
                        VtexSeller(
                            sellerName = "Store C",
                            commertialOffer = VtexCommertialOffer(price = 3.0)
                        )
                    )
                )
            )
        )
        val results = product.toStoreSearchResults()
        assertEquals(3, results.size)
        assertEquals("Store A", results[0].storeName)
        assertEquals("Store B", results[1].storeName)
        assertEquals("Store C", results[2].storeName)
    }

    @Test
    fun `precio se convierte de dolares a centavos correctamente`() {
        val product = VtexProduct(
            productName = "Test",
            items = listOf(
                VtexItem(
                    sellers = listOf(
                        VtexSeller(
                            commertialOffer = VtexCommertialOffer(
                                price = 3.45,
                                listPrice = 4.99
                            )
                        )
                    )
                )
            )
        )
        val results = product.toStoreSearchResults()
        assertEquals(345L, results[0].price)
        assertEquals(499L, results[0].listPrice)
    }
}
