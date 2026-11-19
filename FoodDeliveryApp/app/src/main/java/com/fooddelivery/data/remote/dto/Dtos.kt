package com.fooddelivery.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(val data: T?, val error: String?)

// ─── Auth ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class SyncUserRequest(
    val name: String,
    val email: String
)

@JsonClass(generateAdapter = true)
data class UserDto(val uid: String, val name: String, val email: String)

// ─── Каталог ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CategoryDto(
    val id: Int,
    val name: String,
    @Json(name = "image_url") val imageUrl: String?
)

@JsonClass(generateAdapter = true)
data class RestaurantDto(
    val id: Int,
    val name: String,
    val description: String,
    @Json(name = "image_url") val imageUrl: String?,
    val rating: Double,
    @Json(name = "delivery_time_min") val deliveryTimeMin: Int,
    @Json(name = "min_order_amount") val minOrderAmount: Double
)

@JsonClass(generateAdapter = true)
data class FoodDto(
    val id: Long,
    val name: String,
    val description: String,
    val ingredients: String,
    val price: Double,
    @Json(name = "weight_grams") val weightGrams: Int,
    @Json(name = "cook_time_min") val cookTimeMin: Int,
    val rating: Double,
    @Json(name = "image_url") val imageUrl: String?,
    val category: CategoryDto?,
    val restaurant: RestaurantDto?
)

// ─── Заказы ──────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CartItemRequest(
    @Json(name = "food_id") val foodId: Long,
    val quantity: Int
)

@JsonClass(generateAdapter = true)
data class CreateOrderRequest(
    @Json(name = "delivery_address") val deliveryAddress: String,
    val items: List<CartItemRequest>
)

@JsonClass(generateAdapter = true)
data class OrderDto(
    val id: Long,
    @Json(name = "delivery_address") val deliveryAddress: String,
    val subtotal: Double,
    @Json(name = "delivery_fee") val deliveryFee: Double,
    @Json(name = "total_price") val totalPrice: Double,
    val status: String,
    @Json(name = "created_at") val createdAt: String,
    val items: List<OrderItemDto>?
)

@JsonClass(generateAdapter = true)
data class OrderItemDto(
    val food: FoodDto,
    val quantity: Int,
    @Json(name = "price_at_order") val priceAtOrder: Double
)
