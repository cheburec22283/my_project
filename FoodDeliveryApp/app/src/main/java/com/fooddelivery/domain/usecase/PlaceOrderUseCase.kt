package com.fooddelivery.domain.usecase

import com.fooddelivery.domain.model.CartItem
import com.fooddelivery.domain.model.Order
import com.fooddelivery.domain.repository.CartRepository
import com.fooddelivery.domain.repository.OrderRepository
import javax.inject.Inject

class PlaceOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository
) {
    suspend operator fun invoke(address: String, items: List<CartItem>): Result<Order> {
        if (address.isBlank()) return Result.failure(IllegalArgumentException("Укажите адрес доставки"))
        if (items.isEmpty())   return Result.failure(IllegalArgumentException("Корзина пуста"))

        return orderRepository.placeOrder(address, items)
            .onSuccess { cartRepository.clear() }
    }
}
