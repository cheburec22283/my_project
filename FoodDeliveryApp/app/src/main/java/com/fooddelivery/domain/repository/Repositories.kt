package com.fooddelivery.domain.repository

import com.fooddelivery.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(name: String, email: String, password: String): Result<User>
    suspend fun signOut()
    fun currentUser(): User?
    fun isAuthenticated(): Boolean
}

interface RestaurantRepository {
    suspend fun getRestaurants(): Result<List<Restaurant>>
    suspend fun getRestaurantFoods(restaurantId: Int): Result<List<Food>>
}

interface FoodRepository {
    suspend fun getAllFoods(): Result<List<Food>>
    suspend fun getFoodById(id: Long): Result<Food>
    suspend fun searchFoods(query: String): Result<List<Food>>
}

interface OrderRepository {
    suspend fun placeOrder(address: String, items: List<CartItem>): Result<Order>
    suspend fun getMyOrders(): Result<List<Order>>
    suspend fun getOrderById(orderId: Long): Result<Order>
}

interface SearchHistoryRepository {
    fun getHistory(): Flow<List<String>>
    suspend fun add(query: String)
    suspend fun clear()
}

interface CartRepository {
    fun observeCart(): Flow<Cart>
    fun addItem(food: Food)
    fun decreaseItem(foodId: Long)
    fun removeItem(foodId: Long)
    fun clear()
}

interface ThemeRepository {
    val isDarkMode: Flow<Boolean>
    suspend fun setDarkMode(enabled: Boolean)
}
