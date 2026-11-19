package com.fooddelivery.domain.usecase

import com.fooddelivery.domain.model.Order
import com.fooddelivery.domain.repository.OrderRepository
import javax.inject.Inject

class GetOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: Long): Result<Order> {
        return orderRepository.getOrderById(orderId)
    }
}
