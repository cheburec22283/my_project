package com.fooddelivery.data.repository

import com.fooddelivery.domain.model.Food
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Тесты логики работы с корзиной — добавление, увеличение количества, удаление.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CartRepositoryImplTest {

    private lateinit var cart: CartRepositoryImpl

    @Before
    fun setUp() { cart = CartRepositoryImpl() }

    @Test
    fun `добавление блюда создаёт позицию с количеством 1`() = runTest {
        cart.addItem(pizza())

        val state = cart.observeCart().first()
        assertEquals(1, state.items.size)
        assertEquals(1, state.items[0].quantity)
        assertEquals(499.0, state.subtotal, 0.0)
    }

    @Test
    fun `повторное добавление того же блюда увеличивает количество`() = runTest {
        cart.addItem(pizza())
        cart.addItem(pizza())
        cart.addItem(pizza())

        val state = cart.observeCart().first()
        assertEquals(1, state.items.size)
        assertEquals(3, state.items[0].quantity)
        assertEquals(1497.0, state.subtotal, 0.0)
    }

    @Test
    fun `decrease уменьшает количество на 1`() = runTest {
        cart.addItem(pizza())
        cart.addItem(pizza())
        cart.decreaseItem(1L)

        val state = cart.observeCart().first()
        assertEquals(1, state.items[0].quantity)
    }

    @Test
    fun `decrease удаляет позицию когда количество становится 0`() = runTest {
        cart.addItem(pizza())
        cart.decreaseItem(1L)

        val state = cart.observeCart().first()
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `removeItem удаляет позицию полностью`() = runTest {
        cart.addItem(pizza())
        cart.addItem(pizza())
        cart.removeItem(1L)

        val state = cart.observeCart().first()
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `clear очищает всю корзину`() = runTest {
        cart.addItem(pizza())
        cart.addItem(burger())
        cart.clear()

        val state = cart.observeCart().first()
        assertTrue(state.items.isEmpty())
        assertEquals(0.0, state.subtotal, 0.0)
    }

    @Test
    fun `subtotal корректно считается для нескольких разных блюд`() = runTest {
        cart.addItem(pizza())     // 499
        cart.addItem(pizza())     // 499
        cart.addItem(burger())    // 350

        val state = cart.observeCart().first()
        assertEquals(2, state.items.size)
        assertEquals(1348.0, state.subtotal, 0.0)
        assertEquals(3, state.itemCount)
    }

    private fun pizza() = Food(
        id = 1L, name = "Пицца", description = "", ingredients = "",
        price = 499.0, weightGrams = 470, cookTimeMin = 18,
        rating = 4.8, imageUrl = null, category = null, restaurant = null
    )

    private fun burger() = Food(
        id = 2L, name = "Бургер", description = "", ingredients = "",
        price = 350.0, weightGrams = 280, cookTimeMin = 12,
        rating = 4.7, imageUrl = null, category = null, restaurant = null
    )
}
