package com.fooddelivery.domain.usecase

import com.fooddelivery.domain.model.*
import com.fooddelivery.domain.repository.CartRepository
import com.fooddelivery.domain.repository.OrderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceOrderUseCaseTest {

    private lateinit var orderRepository: OrderRepository
    private lateinit var cartRepository: CartRepository
    private lateinit var useCase: PlaceOrderUseCase

    @Before
    fun setUp() {
        orderRepository = mock()
        cartRepository = mock()
        useCase = PlaceOrderUseCase(orderRepository, cartRepository)
    }

    @Test
    fun `заказ с пустым адресом возвращает ошибку`() = runTest {
        val result = useCase("", listOf(sampleCartItem()))
        assertTrue(result.isFailure)
        verifyNoInteractions(orderRepository)
    }

    @Test
    fun `пустая корзина возвращает ошибку`() = runTest {
        val result = useCase("Москва, ул. Пушкина, 1", emptyList())
        assertTrue(result.isFailure)
        verifyNoInteractions(orderRepository)
    }

    @Test
    fun `успешный заказ очищает корзину`() = runTest {
        val items = listOf(sampleCartItem())
        val order = sampleOrder()
        whenever(orderRepository.placeOrder(any(), any())).thenReturn(Result.success(order))

        val result = useCase("Москва, ул. Пушкина, 1", items)

        assertTrue(result.isSuccess)
        assertEquals(order, result.getOrNull())
        verify(cartRepository).clear()
    }

    @Test
    fun `неудачный заказ не очищает корзину`() = runTest {
        whenever(orderRepository.placeOrder(any(), any()))
            .thenReturn(Result.failure(RuntimeException("Network")))

        val result = useCase("Москва, ул. Пушкина, 1", listOf(sampleCartItem()))

        assertTrue(result.isFailure)
        verify(cartRepository, never()).clear()
    }

    private fun sampleCartItem() = CartItem(
        food = Food(1L, "Пицца", "desc", "ингредиенты",
            499.0, 400, 20, 4.5, null, null, null),
        quantity = 2
    )

    private fun sampleOrder() = Order(
        id = 1L, deliveryAddress = "Москва", subtotal = 998.0,
        deliveryFee = 200.0, totalPrice = 1198.0,
        status = "новый", createdAt = "0", items = null
    )
}
