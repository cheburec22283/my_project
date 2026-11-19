package com.fooddelivery.domain.model

// ─── Доменные модели — независимы от сети и БД ─────────────────────────

data class User(
    val uid: String,
    val name: String,
    val email: String
)

data class Restaurant(
    val id: Int,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val rating: Double,
    val deliveryTimeMin: Int,
    val minOrderAmount: Double
)

data class Category(
    val id: Int,
    val name: String,
    val imageUrl: String?
)

data class Food(
    val id: Long,
    val name: String,
    val description: String,
    val ingredients: String,       // состав
    val price: Double,
    val weightGrams: Int,
    val cookTimeMin: Int,
    val rating: Double,
    val imageUrl: String?,
    val category: Category?,
    val restaurant: Restaurant?
)

data class CartItem(
    val food: Food,
    val quantity: Int
) {
    val total: Double get() = food.price * quantity
}

data class Cart(
    val items: List<CartItem> = emptyList()
) {
    val subtotal: Double get() = items.sumOf { it.total }
    val itemCount: Int get() = items.sumOf { it.quantity }
}

data class Order(
    val id: Long,
    val deliveryAddress: String,
    val subtotal: Double,
    val deliveryFee: Double,
    val totalPrice: Double,
    val status: String,
    val createdAt: String,
    val items: List<OrderItem>?
)

data class OrderItem(
    val food: Food,
    val quantity: Int,
    val priceAtOrder: Double
)
