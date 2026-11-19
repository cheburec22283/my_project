package com.foodserver.plugins

import com.foodserver.dao.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.math.BigDecimal

// ─── DTOs (отправляются клиенту/принимаются от клиента) ──────────────────

@Serializable data class ApiResponse<T>(val data: T? = null, val error: String? = null)

@Serializable data class SyncUserRequest(val name: String, val email: String)
@Serializable data class UserDto(val uid: String, val name: String, val email: String)

@Serializable data class CategoryDto(
    val id: Int, val name: String, val image_url: String?
)

@Serializable data class RestaurantDto(
    val id: Int, val name: String, val description: String,
    val image_url: String?, val rating: Double,
    val delivery_time_min: Int, val min_order_amount: Double
)

@Serializable data class FoodDto(
    val id: Long, val name: String, val description: String,
    val ingredients: String,
    val price: Double, val weight_grams: Int, val cook_time_min: Int,
    val rating: Double, val image_url: String?,
    val category: CategoryDto?, val restaurant: RestaurantDto?
)

@Serializable data class CartItemReq(val food_id: Long, val quantity: Int)

@Serializable data class CreateOrderRequest(
    val delivery_address: String,
    val items: List<CartItemReq>
)

@Serializable data class OrderItemDto(
    val food: FoodDto, val quantity: Int, val price_at_order: Double
)

@Serializable data class OrderDto(
    val id: Long, val delivery_address: String,
    val subtotal: Double, val delivery_fee: Double, val total_price: Double,
    val status: String, val created_at: String,
    val items: List<OrderItemDto>? = null
)

@Serializable data class DeliveryFeeResponse(
    val subtotal: Double, val delivery_fee: Double, val total: Double,
    val free_delivery_from: Double = 1500.0
)

// ─── Сериализация JSON / CORS / Логирование / Status pages ──────────────

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
    install(CallLogging) { level = Level.INFO }

    // CORS — для удобства разработки и веб-клиентов
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }

    // Глобальная обработка ошибок — возвращаем JSON вместо HTML
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(error = "Ошибка сервера: ${cause.message}"))
        }
    }
}

// ─── Routing ─────────────────────────────────────────────────────────────

fun Application.configureRouting() {
    routing {
        // Health-check для проверки, что сервер жив
        get("/") {
            call.respondText("FoodDelivery Server is running ✅", ContentType.Text.Plain)
        }
        get("/health") {
            call.respond(mapOf("status" to "ok", "firebase" to firebaseReady))
        }

        route("api") {

            // ─── Категории ────────────────────────────────────────────────
            get("categories") {
                val cats = CategoryDao.getAll()
                call.respond(ApiResponse(data = cats.map {
                    CategoryDto(it.id, it.name, it.imageUrl)
                }))
            }

            // ─── Рестораны ────────────────────────────────────────────────
            get("restaurants") {
                val list = RestaurantDao.getAll()
                call.respond(ApiResponse(data = list.map { it.toDto() }))
            }

            get("restaurants/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val r = RestaurantDao.getById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound,
                        ApiResponse<RestaurantDto>(error = "Ресторан не найден"))
                call.respond(ApiResponse(data = r.toDto()))
            }

            get("restaurants/{id}/foods") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val foods = FoodDao.getByRestaurant(id)
                call.respond(ApiResponse(data = foods.map { it.toDto() }))
            }

            // ─── Блюда ────────────────────────────────────────────────────
            get("foods") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 100
                val foods = FoodDao.getAll(page, size)
                call.respond(ApiResponse(data = foods.map { it.toDto() }))
            }

            get("foods/search") {
                val q = call.request.queryParameters["q"] ?: ""
                val foods = FoodDao.search(q)
                call.respond(ApiResponse(data = foods.map { it.toDto() }))
            }

            get("foods/{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val food = FoodDao.getById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound,
                        ApiResponse<FoodDto>(error = "Блюдо не найдено"))
                call.respond(ApiResponse(data = food.toDto()))
            }

            // ─── Расчёт стоимости доставки ───────────────────────────────
            get("delivery-fee") {
                val subtotal = call.request.queryParameters["subtotal"]?.toDoubleOrNull() ?: 0.0
                val sub = BigDecimal(subtotal)
                val fee = DeliveryFeeCalculator.calculate(sub)
                call.respond(ApiResponse(data = DeliveryFeeResponse(
                    subtotal = sub.toDouble(),
                    delivery_fee = fee.toDouble(),
                    total = (sub + fee).toDouble()
                )))
            }

            // ─── Защищённые эндпоинты (требуют Firebase ID Token) ────────
            authenticate("firebase-auth") {

                // Синхронизация пользователя после Firebase-регистрации
                post("auth/sync") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val req = call.receive<SyncUserRequest>()
                    val user = UserDao.upsert(principal.uid, req.name, req.email)
                    call.respond(ApiResponse(data = UserDto(user.firebaseUid, user.name, user.email)))
                }

                post("orders") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val user = UserDao.findOrCreate(principal.uid, principal.name, principal.email)

                    val req = call.receive<CreateOrderRequest>()
                    if (req.delivery_address.isBlank() || req.items.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest,
                            ApiResponse<OrderDto>(error = "Укажите адрес и выберите блюда"))
                        return@post
                    }
                    val order = OrderDao.createOrder(user.id, req.delivery_address, req.items)
                    call.respond(HttpStatusCode.Created, ApiResponse(data = order.toDto()))
                }

                get("orders") {
                    val principal = call.principal<FirebasePrincipal>()!!
                    val user = UserDao.findByFirebaseUid(principal.uid)
                        ?: return@get call.respond(ApiResponse(data = emptyList<OrderDto>()))
                    val orders = OrderDao.getUserOrders(user.id)
                    call.respond(ApiResponse(data = orders.map { it.toDto() }))
                }

                get("orders/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val order = OrderDao.getOrderById(id)
                        ?: return@get call.respond(HttpStatusCode.NotFound,
                            ApiResponse<OrderDto>(error = "Заказ не найден"))
                    call.respond(ApiResponse(data = order.toDto()))
                }
            }
        }
    }
}
