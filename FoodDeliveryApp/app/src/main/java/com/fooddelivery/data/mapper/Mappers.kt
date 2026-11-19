package com.fooddelivery.data.mapper

import com.fooddelivery.data.remote.dto.*
import com.fooddelivery.domain.model.*

fun CategoryDto.toDomain() = Category(id, name, imageUrl)

fun RestaurantDto.toDomain() = Restaurant(
    id = id, name = name.toDisplayRestaurantName(), description = description,
    imageUrl = imageUrl, rating = rating,
    deliveryTimeMin = deliveryTimeMin, minOrderAmount = minOrderAmount
)

private fun String.toDisplayRestaurantName(): String {
    val normalized = lowercase().filter { it.isLetterOrDigit() }
    return if (normalized == "шашлыкмашлык") "Шашлычная" else this
}

fun FoodDto.toDomain() = Food(
    id = id, name = name, description = description,
    ingredients = ingredients, price = price,
    weightGrams = weightGrams, cookTimeMin = cookTimeMin,
    rating = rating, imageUrl = imageUrl,
    category = category?.toDomain(),
    restaurant = restaurant?.toDomain()
)

fun OrderItemDto.toDomain() = OrderItem(
    food = food.toDomain(),
    quantity = quantity,
    priceAtOrder = priceAtOrder
)

fun OrderDto.toDomain() = Order(
    id = id, deliveryAddress = deliveryAddress,
    subtotal = subtotal, deliveryFee = deliveryFee, totalPrice = totalPrice,
    status = status, createdAt = createdAt,
    items = items?.map { it.toDomain() }
)
