package com.fooddelivery.data.remote.api

import com.fooddelivery.data.remote.dto.*
import retrofit2.http.*

interface FoodApi {

    // ─── Sync пользователя после Firebase-регистрации ────────────────────
    @POST("api/auth/sync")
    suspend fun syncUser(@Body request: SyncUserRequest): ApiResponse<UserDto>

    // ─── Рестораны ───────────────────────────────────────────────────────
    @GET("api/restaurants")
    suspend fun getRestaurants(): ApiResponse<List<RestaurantDto>>

    @GET("api/restaurants/{id}/foods")
    suspend fun getRestaurantFoods(@Path("id") id: Int): ApiResponse<List<FoodDto>>

    // ─── Категории ───────────────────────────────────────────────────────
    @GET("api/categories")
    suspend fun getCategories(): ApiResponse<List<CategoryDto>>

    // ─── Блюда ───────────────────────────────────────────────────────────
    @GET("api/foods")
    suspend fun getFoods(@Query("page") page: Int = 0,
                          @Query("size") size: Int = 100): ApiResponse<List<FoodDto>>

    @GET("api/foods/search")
    suspend fun searchFoods(@Query("q") query: String): ApiResponse<List<FoodDto>>

    @GET("api/foods/{id}")
    suspend fun getFoodById(@Path("id") id: Long): ApiResponse<FoodDto>

    // ─── Заказы (защищённые Firebase-токеном) ────────────────────────────
    @POST("api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): ApiResponse<OrderDto>

    @GET("api/orders")
    suspend fun getMyOrders(): ApiResponse<List<OrderDto>>

    @GET("api/orders/{id}")
    suspend fun getOrderById(@Path("id") id: Long): ApiResponse<OrderDto>
}
