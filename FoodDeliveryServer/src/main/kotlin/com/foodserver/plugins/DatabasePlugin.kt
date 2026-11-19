package com.foodserver.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.server.application.*
import java.math.BigDecimal

// ─── Таблицы ─────────────────────────────────────────────────────────────

object Users : LongIdTable("users") {
    val firebaseUid = varchar("firebase_uid", 128).uniqueIndex()
    val name = varchar("name", 100)
    val email = varchar("email", 255)
    val createdAt = long("created_at").default(System.currentTimeMillis())
}

object Restaurants : IntIdTable("restaurants") {
    val name = varchar("name", 100)
    val description = text("description").default("")
    val imageUrl = text("image_url").nullable()
    val rating = decimal("rating", 3, 2).default(BigDecimal("4.5"))
    val deliveryTimeMin = integer("delivery_time_min").default(45)
    val minOrderAmount = decimal("min_order_amount", 10, 2).default(BigDecimal("400"))
}

object Categories : IntIdTable("categories") {
    val name = varchar("name", 100)
    val imageUrl = text("image_url").nullable()
}

object Foods : LongIdTable("foods") {
    val restaurantId = integer("restaurant_id").references(Restaurants.id)
    val categoryId = integer("category_id").references(Categories.id)
    val name = varchar("name", 200)
    val description = text("description")
    val ingredients = text("ingredients").default("")
    val price = decimal("price", 10, 2)
    val weightGrams = integer("weight_grams").default(300)
    val cookTimeMin = integer("cook_time_min").default(20)
    val rating = decimal("rating", 3, 2).default(BigDecimal("4.5"))
    val imageUrl = text("image_url").nullable()
    val isAvailable = bool("is_available").default(true)
}

object Orders : LongIdTable("orders") {
    val userId = long("user_id").references(Users.id)
    val deliveryAddress = text("delivery_address")
    val subtotal = decimal("subtotal", 10, 2).default(BigDecimal.ZERO)
    val deliveryFee = decimal("delivery_fee", 10, 2).default(BigDecimal.ZERO)
    val totalPrice = decimal("total_price", 10, 2)
    val status = varchar("status", 50).default("PENDING")
    val createdAt = long("created_at").default(System.currentTimeMillis())
}

object OrderItems : LongIdTable("order_items") {
    val orderId = long("order_id").references(Orders.id)
    val foodId = long("food_id").references(Foods.id)
    val quantity = integer("quantity")
    val priceAtOrder = decimal("price_at_order", 10, 2)
}

// ─── Database Plugin ──────────────────────────────────────────────────────

fun Application.configureDatabases() {
    val dbUrlRaw = System.getenv("DB_URL")?.takeIf { it.isNotBlank() }
        ?: "jdbc:postgresql://localhost:5432/fooddb"
    val dbUser = System.getenv("DB_USER")?.takeIf { it.isNotBlank() } ?: "postgres"
    val dbPass = System.getenv("DB_PASS")?.takeIf { it.isNotBlank() } ?: "postgres"

    val dbUrl = if (dbUrlRaw.startsWith("postgresql://")) "jdbc:$dbUrlRaw" else dbUrlRaw

    log.info("Connecting to configured database")

    try {
        val config = HikariConfig().apply {
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPass
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            connectionTimeout = 10000 // Уменьшим до 10 секунд для быстрой ошибки
            validate()
        }
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users, Restaurants, Categories, Foods, Orders, OrderItems,
            )
            seedDataIfEmpty()
            applyCurrentCatalog()
        }
        log.info("✅ Database initialized successfully")
    } catch (e: Exception) {
        log.error("❌ Failed to connect to database: ${e.message}")
        // Не бросаем исключение, чтобы сервер мог запуститься (хотя эндпоинты БД упадут)
    }
}

// ─── Расчёт стоимости доставки ───────────────────────────────────────────

object DeliveryFeeCalculator {
    fun calculate(subtotal: BigDecimal): BigDecimal = when {
        subtotal >= BigDecimal("1500") -> BigDecimal.ZERO
        subtotal >= BigDecimal("1000") -> BigDecimal("100")
        subtotal >= BigDecimal("500")  -> BigDecimal("200")
        else -> BigDecimal("300")
    }
}

// ─── Seed Data ────────────────────────────────────────────────────────────

private fun seedDataIfEmpty() {
    if (Restaurants.selectAll().count() > 0L) return

    val catPizza   = Categories.insertAndGetId { it[name] = "Пицца" }.value
    val catSushi   = Categories.insertAndGetId { it[name] = "Суши и роллы" }.value
    val catBurger  = Categories.insertAndGetId { it[name] = "Бургеры" }.value
    val catKebab   = Categories.insertAndGetId { it[name] = "Шашлык" }.value
    val catSoup    = Categories.insertAndGetId { it[name] = "Супы" }.value
    val catSalad   = Categories.insertAndGetId { it[name] = "Салаты" }.value
    val catDessert = Categories.insertAndGetId { it[name] = "Десерты" }.value
    val catDrink   = Categories.insertAndGetId { it[name] = "Напитки" }.value
    val catSide    = Categories.insertAndGetId { it[name] = "Гарниры" }.value

    val rDodo = Restaurants.insertAndGetId {
        it[name] = "Додо Пицца"
        it[description] = "Пицца из натуральных ингредиентов"
        it[rating] = BigDecimal("4.7")
        it[deliveryTimeMin] = 35
        it[minOrderAmount] = BigDecimal("400")
    }.value

    val rTanuki = Restaurants.insertAndGetId {
        it[name] = "Тануки"
        it[description] = "Японская кухня — суши, роллы, лапша WOK"
        it[rating] = BigDecimal("4.6")
        it[deliveryTimeMin] = 50
        it[minOrderAmount] = BigDecimal("600")
    }.value

    val rBurger = Restaurants.insertAndGetId {
        it[name] = "Шефбургер"
        it[description] = "Бургеры из мраморной говядины"
        it[rating] = BigDecimal("4.8")
        it[deliveryTimeMin] = 40
        it[minOrderAmount] = BigDecimal("500")
    }.value

    val rShashlik = Restaurants.insertAndGetId {
        it[name] = "Шашлычная"
        it[description] = "Кавказская кухня на углях"
        it[rating] = BigDecimal("4.5")
        it[deliveryTimeMin] = 60
        it[minOrderAmount] = BigDecimal("700")
    }.value

    val rSweets = Restaurants.insertAndGetId {
        it[name] = "Сладкоежка"
        it[description] = "Десерты, выпечка, авторские напитки"
        it[rating] = BigDecimal("4.9")
        it[deliveryTimeMin] = 30
        it[minOrderAmount] = BigDecimal("300")
    }.value

    data class FoodSeed(
        val restaurant: Int, val category: Int, val name: String,
        val description: String, val ingredients: String,
        val price: Int, val weight: Int, val cookTime: Int
    )

    val allFoods = listOf(
        FoodSeed(rDodo, catPizza, "Пицца Маргарита",
            "Классическая итальянская пицца с томатным соусом и моцареллой",
            "тесто, томатный соус, моцарелла, помидоры черри, базилик, оливковое масло",
            499, 470, 18),
        FoodSeed(rDodo, catPizza, "Пицца Пепперони",
            "Острая пицца с салями пепперони и расплавленным сыром",
            "тесто, томатный соус, моцарелла, пепперони, орегано",
            649, 510, 20),
        FoodSeed(rDodo, catPizza, "Пицца 4 сыра",
            "Нежная пицца с четырьмя видами сыра без томатного соуса",
            "тесто, сливочный соус, моцарелла, дорблю, пармезан, чеддер",
            749, 530, 22),
        FoodSeed(rDodo, catPizza, "Пицца Гавайская",
            "Сладко-солёное сочетание ветчины и ананасов",
            "тесто, томатный соус, моцарелла, ветчина, ананасы консервированные",
            599, 500, 20),
        FoodSeed(rDodo, catPizza, "Пицца Мясная",
            "Сытная пицца с пятью видами мяса",
            "тесто, томатный соус, моцарелла, ветчина, бекон, пепперони, курица, говядина",
            849, 590, 25),
        FoodSeed(rDodo, catPizza, "Пицца Цыплёнок Барбекю",
            "Пицца с соусом BBQ и сочной курицей",
            "тесто, соус барбекю, моцарелла, куриное филе, красный лук, кукуруза",
            699, 520, 20),
        FoodSeed(rDodo, catSide, "Картофельные дольки",
            "Хрустящие дольки картофеля с пряностями",
            "картофель, паприка, чеснок, розмарин, соль, растительное масло",
            199, 200, 12),
        FoodSeed(rDodo, catDrink, "Coca-Cola 0,5 л",
            "Освежающий газированный напиток",
            "Coca-Cola classic",
            150, 500, 1),

        FoodSeed(rTanuki, catSushi, "Сет Филадельфия",
            "8 классических роллов Филадельфия с лососем и сливочным сыром",
            "рис, нори, лосось, огурец, авокадо, сливочный сыр Philadelphia, кунжут",
            899, 280, 15),
        FoodSeed(rTanuki, catSushi, "Сет Калифорния",
            "8 роллов с крабом и икрой тобико",
            "рис, нори, краб, авокадо, огурец, икра тобико, майонез японский",
            749, 270, 15),
        FoodSeed(rTanuki, catSushi, "Ролл Дракон",
            "8 запечённых роллов с угрём и соусом унаги",
            "рис, нори, угорь копчёный, авокадо, огурец, соус унаги, кунжут",
            999, 290, 18),
        FoodSeed(rTanuki, catSushi, "Сет Сяке Маки",
            "6 классических роллов с лососем",
            "рис, нори, лосось свежий",
            499, 180, 10),
        FoodSeed(rTanuki, catSushi, "Запечённый ролл с курицей",
            "8 горячих роллов с курицей и сырным соусом",
            "рис, нори, куриное филе, болгарский перец, сырный соус, соус терияки",
            699, 260, 15),
        FoodSeed(rTanuki, catSoup, "Том Ям с креветками",
            "Острый тайский суп на кокосовом молоке",
            "креветки тигровые, кокосовое молоко, лемонграсс, перец чили, лайм, кинза, грибы шиитаке",
            649, 350, 15),
        FoodSeed(rTanuki, catSoup, "Мисо-суп",
            "Традиционный японский суп с тофу и водорослями",
            "мисо-паста, тофу, водоросли вакамэ, зелёный лук, бульон даси",
            299, 300, 8),
        FoodSeed(rTanuki, catSalad, "Чука",
            "Салат из водорослей с кунжутным соусом",
            "водоросли чука, ореховый соус, кунжут, лимон",
            349, 150, 5),

        FoodSeed(rBurger, catBurger, "Чизбургер Классик",
            "Сочная котлета из мраморной говядины с двойным чеддером",
            "булочка бриошь, котлета из говядины 150 г, чеддер ×2, томат, салат айсберг, маринованный огурец, фирменный соус",
            499, 280, 12),
        FoodSeed(rBurger, catBurger, "Шефбургер Double",
            "Двойная мясная атака для настоящих ценителей",
            "булочка бриошь, две котлеты по 150 г, бекон, чеддер ×2, томат, салат, лук фри, соус BBQ",
            749, 380, 15),
        FoodSeed(rBurger, catBurger, "Бургер с курицей",
            "Хрустящая куриная котлета в панировке",
            "булочка с кунжутом, куриное филе в панировке, салат айсберг, томат, сыр чеддер, соус ранч",
            449, 270, 12),
        FoodSeed(rBurger, catBurger, "Вегги-бургер",
            "Котлета из чечевицы и грибов",
            "булочка цельнозерновая, котлета из чечевицы и грибов, авокадо, томат, салат, соус из йогурта",
            399, 260, 10),
        FoodSeed(rBurger, catSide, "Картофель фри",
            "Хрустящий золотистый картофель",
            "картофель, растительное масло, соль",
            169, 150, 6),
        FoodSeed(rBurger, catSide, "Луковые кольца",
            "Луковые кольца в хрустящей панировке",
            "лук репчатый, кляр, панировочные сухари, специи",
            229, 180, 8),
        FoodSeed(rBurger, catSide, "Наггетсы 9 шт",
            "Куриные наггетсы с двумя соусами",
            "куриное филе, панировка, специи, соус барбекю, кисло-сладкий соус",
            349, 220, 10),
        FoodSeed(rBurger, catDrink, "Молочный коктейль шоколадный",
            "Густой молочный коктейль с шоколадом",
            "молоко, мороженое пломбир, шоколадный сироп, какао",
            259, 400, 3),

        FoodSeed(rShashlik, catKebab, "Шашлык из свинины",
            "Сочный шашлык из шейки на углях",
            "свиная шея, лук, специи кавказские, уксус винный, соль",
            649, 250, 25),
        FoodSeed(rShashlik, catKebab, "Шашлык из баранины",
            "Классический шашлык из молодой баранины",
            "баранина, лук, чеснок, кинза, специи, гранатовый сок",
            849, 250, 28),
        FoodSeed(rShashlik, catKebab, "Шашлык из курицы",
            "Куриный шашлык с дымком",
            "куриное филе, кефир, лук, паприка, чеснок",
            499, 250, 20),
        FoodSeed(rShashlik, catKebab, "Люля-кебаб",
            "Сочные кебабы из рубленой говядины и баранины",
            "говядина рубленая, баранина рубленая, лук, кинза, специи восточные",
            599, 220, 22),
        FoodSeed(rShashlik, catSalad, "Хачапури по-аджарски",
            "Лодочка из теста с сыром сулугуни и яйцом",
            "тесто дрожжевое, сулугуни, имеретинский сыр, яйцо, сливочное масло",
            549, 350, 20),
        FoodSeed(rShashlik, catSalad, "Овощи на гриле",
            "Запечённые овощи с травами",
            "баклажан, кабачок, перец болгарский, лук красный, помидоры, оливковое масло, тимьян",
            449, 300, 15),
        FoodSeed(rShashlik, catSide, "Лаваш домашний",
            "Тонкий армянский лаваш ручной работы",
            "мука, вода, соль",
            99, 150, 5),
        FoodSeed(rShashlik, catSoup, "Харчо",
            "Острый грузинский суп с говядиной и рисом",
            "говядина, рис, грецкие орехи, ткемали, томатная паста, чеснок, кинза, специи",
            449, 350, 15),

        FoodSeed(rSweets, catDessert, "Чизкейк Нью-Йорк",
            "Классический чизкейк на песочной основе",
            "сливочный сыр Philadelphia, сахар, яйца, сметана, печенье, сливочное масло",
            349, 180, 5),
        FoodSeed(rSweets, catDessert, "Тирамису",
            "Итальянский десерт с маскарпоне и кофе",
            "маскарпоне, печенье савоярди, эспрессо, какао-порошок, яйца, сахар",
            399, 170, 5),
        FoodSeed(rSweets, catDessert, "Брауни шоколадный",
            "Тёплый брауни с шариком пломбира",
            "тёмный шоколад, сливочное масло, мука, яйца, сахар, мороженое пломбир",
            329, 200, 8),
        FoodSeed(rSweets, catDessert, "Эклер с кремом",
            "Воздушный эклер со сливочно-ванильным кремом",
            "заварное тесто, сливочный крем, ваниль, шоколадная глазурь",
            199, 90, 3),
        FoodSeed(rSweets, catDessert, "Макаруны 6 шт",
            "Ассорти французских макаронс",
            "миндальная мука, сахарная пудра, яичный белок, ягодное пюре, шоколадный ганаш",
            499, 120, 5),
        FoodSeed(rSweets, catDrink, "Латте",
            "Кофе с молоком и тонкой молочной пенкой",
            "эспрессо, молоко",
            199, 300, 5),
        FoodSeed(rSweets, catDrink, "Капучино",
            "Кофе с молочной пенкой и какао",
            "эспрессо, молоко, какао",
            189, 250, 5),
        FoodSeed(rSweets, catDrink, "Какао с маршмеллоу",
            "Горячее какао со взбитыми сливками",
            "какао, молоко, сахар, маршмеллоу, взбитые сливки",
            229, 300, 5),
        FoodSeed(rSweets, catDrink, "Свежевыжатый апельсиновый сок",
            "Сок из спелых апельсинов",
            "апельсины",
            299, 300, 3),
    )

    allFoods.forEach { f ->
        Foods.insert {
            it[restaurantId] = f.restaurant
            it[categoryId] = f.category
            it[Foods.name] = f.name
            it[description] = f.description
            it[ingredients] = f.ingredients
            it[price] = BigDecimal(f.price)
            it[weightGrams] = f.weight
            it[cookTimeMin] = f.cookTime
            it[rating] = BigDecimal((4.3 + Math.random() * 0.6).toString())
                .setScale(2, java.math.RoundingMode.HALF_UP)
        }
    }

    // Добавим тестового пользователя и заказ, чтобы "Отслеживание заказа" не выдавало 404
    val testUserId = Users.insertAndGetId {
        it[firebaseUid] = "test-uid-123"
        it[name] = "Тестовый Покупатель"
        it[email] = "test@example.com"
    }.value

    // Создаём 5 заказов, чтобы заказ #5 существовал
    repeat(5) { i ->
        val orderId = Orders.insertAndGetId {
            it[userId] = testUserId
            it[deliveryAddress] = "ул. Пушкина, д. Колотушкина, кв. ${10 + i}"
            it[subtotal] = BigDecimal("1200")
            it[deliveryFee] = BigDecimal("100")
            it[totalPrice] = BigDecimal("1300")
            it[status] = when(i) {
                4 -> "COOKING" // Для заказа #5
                else -> "DELIVERED"
            }
        }.value

        // Привяжем пару блюд к заказу
        val someFood = Foods.selectAll().limit(1).first()
        OrderItems.insert {
            it[OrderItems.orderId] = orderId
            it[OrderItems.foodId] = someFood[Foods.id].value
            it[quantity] = 2
            it[priceAtOrder] = someFood[Foods.price]
        }
    }
}

/** Apply catalog changes on every startup without replacing user data. */
private fun applyCurrentCatalog() {
    val legacyRestaurants = Restaurants.selectAll().map { row ->
        row[Restaurants.id].value to row[Restaurants.name]
    }
    legacyRestaurants.forEach { (restaurantId, restaurantName) ->
        val normalized = restaurantName.lowercase().filter { it.isLetterOrDigit() }
        if (normalized == "шашлыкмашлык") {
            Restaurants.update({ Restaurants.id eq restaurantId }) {
                it[name] = "Шашлычная"
                it[description] = "Кавказская кухня на углях"
            }
        }
    }

    val restaurantRow = Restaurants.select {
        Restaurants.name eq "Urban Bowl"
    }.firstOrNull()

    val urbanBowlId = if (restaurantRow == null) {
        Restaurants.insertAndGetId {
            it[name] = "Urban Bowl"
            it[description] = "Боулы, вок и яркие азиатские вкусы"
            it[imageUrl] = null
            it[rating] = BigDecimal("4.9")
            it[deliveryTimeMin] = 25
            it[minOrderAmount] = BigDecimal("390.00")
        }.value
    } else {
        val id = restaurantRow[Restaurants.id].value
        Restaurants.update({ Restaurants.id eq id }) {
            it[name] = "Urban Bowl"
            it[description] = "Боулы, вок и яркие азиатские вкусы"
            it[imageUrl] = null
            it[rating] = BigDecimal("4.9")
            it[deliveryTimeMin] = 25
            it[minOrderAmount] = BigDecimal("390.00")
        }
        id
    }

    val bowlCategoryId = ensureCategory("Боулы и вок")
    ensureFood(
        restaurantId = urbanBowlId,
        categoryId = bowlCategoryId,
        name = "Куриный боул",
        description = "Рис, курица терияки, огурец, морковь и кунжут",
        ingredients = "Рис, курица, соус терияки, огурец, морковь, кунжут",
        price = 390,
        weight = 380,
        cookTime = 15,
        rating = "4.90"
    )
    ensureFood(
        restaurantId = urbanBowlId,
        categoryId = bowlCategoryId,
        name = "Говядина терияки",
        description = "Нежная говядина с овощами и рисом",
        ingredients = "Говядина, рис, сладкий перец, брокколи, соус терияки",
        price = 520,
        weight = 420,
        cookTime = 20,
        rating = "4.80"
    )
    ensureFood(
        restaurantId = urbanBowlId,
        categoryId = bowlCategoryId,
        name = "Овощной вок",
        description = "Лапша с хрустящими овощами в пикантном соусе",
        ingredients = "Лапша, брокколи, морковь, перец, соевый соус, кунжут",
        price = 410,
        weight = 350,
        cookTime = 12,
        rating = "4.70"
    )
}

private fun ensureCategory(categoryName: String): Int {
    val category = Categories.select {
        Categories.name eq categoryName
    }.firstOrNull()

    return category?.get(Categories.id)?.value
        ?: Categories.insertAndGetId {
            it[name] = categoryName
            it[imageUrl] = null
        }.value
}

private fun ensureFood(
    restaurantId: Int,
    categoryId: Int,
    name: String,
    description: String,
    ingredients: String,
    price: Int,
    weight: Int,
    cookTime: Int,
    rating: String
) {
    val existing = Foods.select {
        (Foods.restaurantId eq restaurantId) and (Foods.name eq name)
    }.firstOrNull()

    if (existing == null) {
        Foods.insert {
            it[Foods.restaurantId] = restaurantId
            it[Foods.categoryId] = categoryId
            it[Foods.name] = name
            it[Foods.description] = description
            it[Foods.ingredients] = ingredients
            it[Foods.price] = BigDecimal(price)
            it[Foods.weightGrams] = weight
            it[Foods.cookTimeMin] = cookTime
            it[Foods.rating] = BigDecimal(rating)
            it[Foods.imageUrl] = null
            it[Foods.isAvailable] = true
        }
    } else {
        Foods.update({ Foods.id eq existing[Foods.id].value }) {
            it[Foods.categoryId] = categoryId
            it[Foods.description] = description
            it[Foods.ingredients] = ingredients
            it[Foods.price] = BigDecimal(price)
            it[Foods.weightGrams] = weight
            it[Foods.cookTimeMin] = cookTime
            it[Foods.rating] = BigDecimal(rating)
            it[Foods.imageUrl] = null
            it[Foods.isAvailable] = true
        }
    }
}

suspend fun <T> dbQuery(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
