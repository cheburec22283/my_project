package com.fooddelivery.domain.model

/** Локальный ресторан для демонстрации каталога без изменений серверной БД. */
object DemoMenu {
    val restaurant = Restaurant(
        id = -7007,
        name = "Urban Bowl",
        description = "Боулы, вок и яркие азиатские вкусы",
        imageUrl = null,
        rating = 4.9,
        deliveryTimeMin = 25,
        minOrderAmount = 390.0
    )

    private val category = Category(
        id = -7007,
        name = "Боулы и вок",
        imageUrl = null
    )

    val foods = listOf(
        Food(
            id = -70071L,
            name = "Куриный боул",
            description = "Рис, курица терияки, огурец, морковь и кунжут",
            ingredients = "Рис, курица, соус терияки, огурец, морковь, кунжут",
            price = 390.0,
            weightGrams = 380,
            cookTimeMin = 15,
            rating = 4.9,
            imageUrl = null,
            category = category,
            restaurant = restaurant
        ),
        Food(
            id = -70072L,
            name = "Говядина терияки",
            description = "Нежная говядина с овощами и рисом",
            ingredients = "Говядина, рис, сладкий перец, брокколи, соус терияки",
            price = 520.0,
            weightGrams = 420,
            cookTimeMin = 20,
            rating = 4.8,
            imageUrl = null,
            category = category,
            restaurant = restaurant
        ),
        Food(
            id = -70073L,
            name = "Овощной вок",
            description = "Лапша с хрустящими овощами в пикантном соусе",
            ingredients = "Лапша, брокколи, морковь, перец, соевый соус, кунжут",
            price = 410.0,
            weightGrams = 350,
            cookTimeMin = 12,
            rating = 4.7,
            imageUrl = null,
            category = category,
            restaurant = restaurant
        )
    )

    fun foodById(id: Long): Food? = foods.firstOrNull { it.id == id }
}
