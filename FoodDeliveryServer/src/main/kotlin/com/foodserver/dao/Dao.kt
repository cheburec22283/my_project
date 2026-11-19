package com.foodserver.dao

import com.foodserver.plugins.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

// ─── User ─────────────────────────────────────────────────────────────────

data class UserRecord(
    val id: Long,
    val firebaseUid: String,
    val name: String,
    val email: String
)

object UserDao {
    fun findByFirebaseUid(uid: String): UserRecord? = transaction {
        Users.select { Users.firebaseUid eq uid }.firstOrNull()?.toUser()
    }

    /** Если пользователя нет — создаём; если есть — обновляем имя/email. */
    fun upsert(uid: String, name: String, email: String): UserRecord = transaction {
        val existing = Users.select { Users.firebaseUid eq uid }.firstOrNull()
        if (existing != null) {
            Users.update({ Users.firebaseUid eq uid }) {
                it[Users.name] = name
                it[Users.email] = email
            }
            UserRecord(existing[Users.id].value, uid, name, email)
        } else {
            val id = Users.insertAndGetId {
                it[Users.firebaseUid] = uid
                it[Users.name] = name
                it[Users.email] = email
            }.value
            UserRecord(id, uid, name, email)
        }
    }

    fun findOrCreate(uid: String, name: String?, email: String?): UserRecord =
        findByFirebaseUid(uid) ?: upsert(uid, name ?: "Пользователь", email ?: "")

    private fun ResultRow.toUser() = UserRecord(
        id = this[Users.id].value,
        firebaseUid = this[Users.firebaseUid],
        name = this[Users.name],
        email = this[Users.email]
    )
}

// ─── Restaurant ──────────────────────────────────────────────────────────

data class RestaurantRecord(
    val id: Int, val name: String, val description: String,
    val imageUrl: String?, val rating: BigDecimal,
    val deliveryTimeMin: Int, val minOrderAmount: BigDecimal
)

fun RestaurantRecord.toDto() = RestaurantDto(
    id = id, name = name, description = description,
    image_url = imageUrl, rating = rating.toDouble(),
    delivery_time_min = deliveryTimeMin, min_order_amount = minOrderAmount.toDouble()
)

object RestaurantDao {
    fun getAll(): List<RestaurantRecord> = transaction {
        Restaurants.selectAll().map { it.toRestaurant() }
    }

    fun getById(id: Int): RestaurantRecord? = transaction {
        Restaurants.select { Restaurants.id eq id }.firstOrNull()?.toRestaurant()
    }

    private fun ResultRow.toRestaurant() = RestaurantRecord(
        id = this[Restaurants.id].value,
        name = this[Restaurants.name],
        description = this[Restaurants.description],
        imageUrl = this[Restaurants.imageUrl],
        rating = this[Restaurants.rating],
        deliveryTimeMin = this[Restaurants.deliveryTimeMin],
        minOrderAmount = this[Restaurants.minOrderAmount]
    )
}

// ─── Category ─────────────────────────────────────────────────────────────

data class CategoryRecord(val id: Int, val name: String, val imageUrl: String?)

object CategoryDao {
    fun getAll(): List<CategoryRecord> = transaction {
        Categories.selectAll().map {
            CategoryRecord(it[Categories.id].value, it[Categories.name], it[Categories.imageUrl])
        }
    }
}

// ─── Food ─────────────────────────────────────────────────────────────────

data class FoodRecord(
    val id: Long, val name: String, val description: String,
    val ingredients: String, val price: BigDecimal,
    val weightGrams: Int, val cookTimeMin: Int,
    val rating: BigDecimal, val imageUrl: String?,
    val category: CategoryRecord?, val restaurant: RestaurantRecord?
)

fun FoodRecord.toDto() = FoodDto(
    id = id, name = name, description = description,
    ingredients = ingredients, price = price.toDouble(),
    weight_grams = weightGrams, cook_time_min = cookTimeMin,
    rating = rating.toDouble(), image_url = imageUrl,
    category = category?.let { CategoryDto(it.id, it.name, it.imageUrl) },
    restaurant = restaurant?.toDto()
)

object FoodDao {
    private fun ResultRow.toFood() = FoodRecord(
        id = this[Foods.id].value,
        name = this[Foods.name],
        description = this[Foods.description],
        ingredients = this[Foods.ingredients],
        price = this[Foods.price],
        weightGrams = this[Foods.weightGrams],
        cookTimeMin = this[Foods.cookTimeMin],
        rating = this[Foods.rating],
        imageUrl = this[Foods.imageUrl],
        category = CategoryRecord(this[Categories.id].value, this[Categories.name], this[Categories.imageUrl]),
        restaurant = RestaurantRecord(
            id = this[Restaurants.id].value,
            name = this[Restaurants.name],
            description = this[Restaurants.description],
            imageUrl = this[Restaurants.imageUrl],
            rating = this[Restaurants.rating],
            deliveryTimeMin = this[Restaurants.deliveryTimeMin],
            minOrderAmount = this[Restaurants.minOrderAmount]
        )
    )

    private fun baseQuery() = (Foods innerJoin Categories innerJoin Restaurants)

    fun getAll(page: Int, size: Int): List<FoodRecord> = transaction {
        baseQuery().selectAll().where { Foods.isAvailable eq true }
            .limit(size, offset = (page * size).toLong())
            .map { it.toFood() }
    }

    fun getByRestaurant(restaurantId: Int): List<FoodRecord> = transaction {
        baseQuery().selectAll()
            .where { (Foods.restaurantId eq restaurantId) and (Foods.isAvailable eq true) }
            .map { it.toFood() }
    }

    fun search(query: String): List<FoodRecord> = transaction {
        baseQuery().selectAll()
            .where { (Foods.name.lowerCase() like "%${query.lowercase()}%") and (Foods.isAvailable eq true) }
            .map { it.toFood() }
    }

    fun getById(id: Long): FoodRecord? = transaction {
        baseQuery().selectAll().where { Foods.id eq id }.firstOrNull()?.toFood()
    }
}

// ─── Order ────────────────────────────────────────────────────────────────

data class OrderRecord(
    val id: Long, val deliveryAddress: String,
    val subtotal: BigDecimal, val deliveryFee: BigDecimal, val totalPrice: BigDecimal,
    val status: String, val createdAt: Long,
    val items: List<OrderItemRecord>?
)

data class OrderItemRecord(
    val food: FoodRecord, val quantity: Int, val priceAtOrder: BigDecimal
)

fun OrderRecord.toDto() = OrderDto(
    id = id, delivery_address = deliveryAddress,
    subtotal = subtotal.toDouble(),
    delivery_fee = deliveryFee.toDouble(),
    total_price = totalPrice.toDouble(),
    status = status, created_at = createdAt.toString(),
    items = items?.map {
        OrderItemDto(it.food.toDto(), it.quantity, it.priceAtOrder.toDouble())
    }
)

object OrderDao {

    fun createOrder(
        userId: Long, address: String, items: List<CartItemReq>
    ): OrderRecord = transaction {

        var subtotal = BigDecimal.ZERO
        val foodItems = items.mapNotNull { req ->
            FoodDao.getById(req.food_id)?.also { food ->
                subtotal += food.price * req.quantity.toBigDecimal()
            }?.let { Pair(it, req.quantity) }
        }

        val deliveryFee = DeliveryFeeCalculator.calculate(subtotal)
        val total = subtotal + deliveryFee

        val orderId = Orders.insertAndGetId {
            it[Orders.userId] = userId
            it[Orders.deliveryAddress] = address
            it[Orders.subtotal] = subtotal
            it[Orders.deliveryFee] = deliveryFee
            it[Orders.totalPrice] = total
        }.value

        foodItems.forEach { (food, qty) ->
            OrderItems.insert {
                it[OrderItems.orderId] = orderId
                it[OrderItems.foodId] = food.id
                it[OrderItems.quantity] = qty
                it[OrderItems.priceAtOrder] = food.price
            }
        }

        OrderRecord(
            id = orderId, deliveryAddress = address,
            subtotal = subtotal, deliveryFee = deliveryFee, totalPrice = total,
            status = "PENDING", createdAt = System.currentTimeMillis(),
            items = foodItems.map { (food, qty) -> OrderItemRecord(food, qty, food.price) }
        )
    }

    fun getUserOrders(userId: Long): List<OrderRecord> = transaction {
        Orders.selectAll().where { Orders.userId eq userId }
            .orderBy(Orders.createdAt, SortOrder.DESC)
            .map { row ->
                OrderRecord(
                    id = row[Orders.id].value,
                    deliveryAddress = row[Orders.deliveryAddress],
                    subtotal = row[Orders.subtotal],
                    deliveryFee = row[Orders.deliveryFee],
                    totalPrice = row[Orders.totalPrice],
                    status = row[Orders.status],
                    createdAt = row[Orders.createdAt],
                    items = null
                )
            }
    }

    fun getOrderById(id: Long): OrderRecord? = transaction {
        Orders.selectAll().where { Orders.id eq id }.firstOrNull()?.let { row ->
            val items = (OrderItems innerJoin Foods innerJoin Categories innerJoin Restaurants)
                .selectAll().where { OrderItems.orderId eq id }
                .map { itemRow ->
                    OrderItemRecord(
                        food = FoodRecord(
                            id = itemRow[Foods.id].value,
                            name = itemRow[Foods.name],
                            description = itemRow[Foods.description],
                            ingredients = itemRow[Foods.ingredients],
                            price = itemRow[Foods.price],
                            weightGrams = itemRow[Foods.weightGrams],
                            cookTimeMin = itemRow[Foods.cookTimeMin],
                            rating = itemRow[Foods.rating],
                            imageUrl = itemRow[Foods.imageUrl],
                            category = CategoryRecord(
                                itemRow[Categories.id].value,
                                itemRow[Categories.name],
                                itemRow[Categories.imageUrl]
                            ),
                            restaurant = RestaurantRecord(
                                id = itemRow[Restaurants.id].value,
                                name = itemRow[Restaurants.name],
                                description = itemRow[Restaurants.description],
                                imageUrl = itemRow[Restaurants.imageUrl],
                                rating = itemRow[Restaurants.rating],
                                deliveryTimeMin = itemRow[Restaurants.deliveryTimeMin],
                                minOrderAmount = itemRow[Restaurants.minOrderAmount]
                            )
                        ),
                        quantity = itemRow[OrderItems.quantity],
                        priceAtOrder = itemRow[OrderItems.priceAtOrder]
                    )
                }

            OrderRecord(
                id = row[Orders.id].value,
                deliveryAddress = row[Orders.deliveryAddress],
                subtotal = row[Orders.subtotal],
                deliveryFee = row[Orders.deliveryFee],
                totalPrice = row[Orders.totalPrice],
                status = row[Orders.status],
                createdAt = row[Orders.createdAt],
                items = items
            )
        }
    }
}
