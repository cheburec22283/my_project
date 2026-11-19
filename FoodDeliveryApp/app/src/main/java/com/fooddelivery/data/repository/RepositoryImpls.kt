package com.fooddelivery.data.repository

import com.fooddelivery.data.datastore.AppPreferences
import com.fooddelivery.data.firebase.FirebaseAuthDataSource
import com.fooddelivery.data.local.dao.SearchHistoryDao
import com.fooddelivery.data.local.entity.SearchHistoryEntity
import com.fooddelivery.data.mapper.toDomain
import com.fooddelivery.data.remote.api.FoodApi
import com.fooddelivery.data.remote.dto.CartItemRequest
import com.fooddelivery.data.remote.dto.CreateOrderRequest
import com.fooddelivery.data.remote.dto.SyncUserRequest
import com.fooddelivery.domain.model.*
import com.fooddelivery.domain.repository.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

// ─── AuthRepository ──────────────────────────────────────────────────────

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuthDataSource,
    private val api: FoodApi
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        firebaseAuth.signIn(email, password)
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<User> =
        runCatching {
            val user = firebaseAuth.signUp(name, email, password)
            // Синхронизируем профиль с собственной БД на сервере
            runCatching { api.syncUser(SyncUserRequest(name, email)) }
            user
        }

    override suspend fun signOut() { firebaseAuth.signOut() }
    override fun currentUser(): User? = firebaseAuth.currentUser()
    override fun isAuthenticated() = firebaseAuth.isAuthenticated()
}

// ─── RestaurantRepository ────────────────────────────────────────────────

@Singleton
class RestaurantRepositoryImpl @Inject constructor(
    private val api: FoodApi
) : RestaurantRepository {

    override suspend fun getRestaurants(): Result<List<Restaurant>> = runCatching {
        api.getRestaurants().data?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getRestaurantFoods(restaurantId: Int): Result<List<Food>> = runCatching {
        api.getRestaurantFoods(restaurantId).data?.map { it.toDomain() } ?: emptyList()
    }
}

// ─── FoodRepository ──────────────────────────────────────────────────────

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val api: FoodApi
) : FoodRepository {

    override suspend fun getAllFoods(): Result<List<Food>> = runCatching {
        api.getFoods().data?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getFoodById(id: Long): Result<Food> = runCatching {
        api.getFoodById(id).data?.toDomain() ?: error("Блюдо не найдено")
    }

    override suspend fun searchFoods(query: String): Result<List<Food>> = runCatching {
        api.searchFoods(query).data?.map { it.toDomain() } ?: emptyList()
    }
}

// ─── OrderRepository ─────────────────────────────────────────────────────

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val api: FoodApi
) : OrderRepository {

    @Volatile
    private var localDemoOrder: Order? = null

    override suspend fun placeOrder(address: String, items: List<CartItem>): Result<Order> =
        runCatching {
            if (items.any { DemoMenu.foodById(it.food.id) != null }) {
                val subtotal = items.sumOf { it.total }
                val deliveryFee = when {
                    subtotal >= 1500.0 -> 0.0
                    subtotal >= 1000.0 -> 100.0
                    subtotal >= 500.0 -> 200.0
                    else -> 300.0
                }
                Order(
                    id = System.currentTimeMillis(),
                    deliveryAddress = address,
                    subtotal = subtotal,
                    deliveryFee = deliveryFee,
                    totalPrice = subtotal + deliveryFee,
                    status = "PREPARING",
                    createdAt = java.time.Instant.now().toString(),
                    items = items.map { OrderItem(it.food, it.quantity, it.food.price) }
                ).also { localDemoOrder = it }
            } else {
                val resp = api.createOrder(CreateOrderRequest(
                    deliveryAddress = address,
                    items = items.map { CartItemRequest(it.food.id, it.quantity) }
                ))
                resp.data?.toDomain() ?: error(resp.error ?: "Ошибка оформления заказа")
            }
        }

    override suspend fun getMyOrders(): Result<List<Order>> = runCatching {
        val remoteOrders = api.getMyOrders().data?.map { it.toDomain() } ?: emptyList()
        remoteOrders + listOfNotNull(localDemoOrder)
    }

    override suspend fun getOrderById(orderId: Long): Result<Order> {
        val localOrder = localDemoOrder
        if (localOrder?.id == orderId) {
            return Result.success(localOrder)
        }
        return runCatching {
            val resp = api.getOrderById(orderId)
            resp.data?.toDomain() ?: error(resp.error ?: "Заказ не найден")
        }
    }
}

// ─── SearchHistoryRepository ─────────────────────────────────────────────

@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val dao: SearchHistoryDao
) : SearchHistoryRepository {

    override fun getHistory(): Flow<List<String>> = dao.getHistory()

    override suspend fun add(query: String) {
        if (query.isBlank()) return
        dao.insert(SearchHistoryEntity(query.trim()))
        dao.trimOldEntries()  // Оставляем только 10 самых новых
    }

    override suspend fun clear() {
        dao.clearAll()
    }
}

// ─── CartRepository (in-memory) ──────────────────────────────────────────

@Singleton
class CartRepositoryImpl @Inject constructor() : CartRepository {

    private val _cart = MutableStateFlow(Cart())

    override fun observeCart(): Flow<Cart> = _cart.asStateFlow()

    override fun addItem(food: Food) {
        _cart.update { cart ->
            val existing = cart.items.find { it.food.id == food.id }
            val newItems = if (existing != null) {
                cart.items.map {
                    if (it.food.id == food.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else cart.items + CartItem(food, 1)
            cart.copy(items = newItems)
        }
    }

    override fun decreaseItem(foodId: Long) {
        _cart.update { cart ->
            val newItems = cart.items.mapNotNull { item ->
                when {
                    item.food.id != foodId -> item
                    item.quantity > 1 -> item.copy(quantity = item.quantity - 1)
                    else -> null
                }
            }
            cart.copy(items = newItems)
        }
    }

    override fun removeItem(foodId: Long) {
        _cart.update { it.copy(items = it.items.filter { item -> item.food.id != foodId }) }
    }

    override fun clear() { _cart.value = Cart() }
}

// ─── ThemeRepository ─────────────────────────────────────────────────────

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val prefs: AppPreferences
) : ThemeRepository {
    override val isDarkMode: Flow<Boolean> = prefs.isDarkMode
    override suspend fun setDarkMode(enabled: Boolean) = prefs.setDarkMode(enabled)
}
