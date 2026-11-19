# Food Delivery — Server (Ktor)

Серверная часть мобильного приложения для заказа и доставки еды.

Курсовая работа по дисциплине «Разработка клиент-серверных мобильных приложений», МИРЭА.

## 🛠️ Технологический стек

- **Язык**: Kotlin 1.9 + Java 21
- **Web-фреймворк**: Ktor 2.3 (CIO engine)
- **ORM**: JetBrains Exposed
- **БД**: PostgreSQL на [neon.tech](https://neon.tech)
- **Аутентификация**: Firebase Admin SDK (верификация ID-токенов)
- **Логирование**: Logback
- **Пул соединений**: HikariCP
- **Сериализация**: kotlinx.serialization (JSON)

## 📁 Структура

```
FoodDeliveryServer/
├── build.gradle.kts
├── settings.gradle.kts
└── src/main/
    ├── kotlin/com/foodserver/
    │   ├── Application.kt           ← точка входа
    │   ├── plugins/
    │   │   ├── DatabasePlugin.kt    ← PostgreSQL + схема + seed-данные и миграции каталога (6 ресторанов, 40+ блюд)
    │   │   ├── AuthPlugin.kt        ← Firebase Admin SDK
    │   │   └── Routing.kt           ← REST API маршруты + DTO
    │   └── dao/
    │       └── Dao.kt               ← Data Access Objects
    └── resources/
        ├── application.conf
        └── logback.xml

Секретный ключ Firebase не включён в архив. Его нужно указать отдельно.
```

## Каталог в этой версии

При старте сервер безопасно и без дублирования обновляет каталог существующей базы:
старое название `Шашлык-Машлык` меняется на `Шашлычная`, а `Urban Bowl` и его три блюда
добавляются, если их ещё нет. Существующие пользователи, заказы и прочий каталог не удаляются.

## 🚀 Запуск проекта

### 1. Подготовьте облачную базу данных (neon.tech)

1. Зарегистрируйтесь на https://neon.tech (через Google)
2. Создайте новый проект `fooddb`
3. Скопируйте **Connection string** — она вида:
   ```
   postgresql://USER:PASSWORD@HOST.eu-central-1.aws.neon.tech/DBNAME?sslmode=require
   ```

### 2. Получите Firebase Admin SDK credentials

1. Откройте https://console.firebase.google.com
2. Выберите ваш проект (например, `food-delivery-app`)
3. **Project settings** (шестерёнка ⚙) → вкладка **Service accounts**
4. Нажмите **Generate new private key** → **Generate key**
5. Сохраните JSON-файл вне проекта, например `C:\secrets\firebase-admin-sdk.json`
6. Установите `FIREBASE_SERVICE_ACCOUNT_PATH` в путь к этому файлу.

Например, в IntelliJ IDEA добавьте в Environment variables:
`FIREBASE_SERVICE_ACCOUNT_PATH=C:\secrets\firebase-admin-sdk.json`

Локальный файл `src/main/resources/firebase-admin-sdk.json` тоже поддерживается, но не включайте
его в архивы и репозитории: он содержит приватный ключ.

### 3. Установите переменные окружения

#### В IntelliJ IDEA:
**Run → Edit Configurations…** → выберите `ApplicationKt` → поле **Environment variables**:

```
DB_URL=jdbc:postgresql://HOST.eu-central-1.aws.neon.tech/DBNAME?sslmode=require
DB_USER=ваш_логин_из_neon
DB_PASS=ваш_пароль_из_neon
FIREBASE_SERVICE_ACCOUNT_PATH=C:\secrets\firebase-admin-sdk.json
```

> ⚠️ Обратите внимание: в neon.tech строка начинается с `postgresql://`, а нам нужно **`jdbc:postgresql://`** — добавьте префикс `jdbc:` вручную.

#### Или через командную строку (Windows PowerShell):

```powershell
$env:DB_URL="jdbc:postgresql://HOST/DBNAME?sslmode=require"
$env:DB_USER="your_user"
$env:DB_PASS="your_password"
$env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\secrets\firebase-admin-sdk.json"
.\gradlew run
```

### 4. Запустите сервер

В IntelliJ IDEA: откройте `Application.kt` → нажмите зелёный ▶ слева от `fun main()`.

Или из командной строки:
```bash
./gradlew run
```

При корректно настроенных переменных окружения сервер запустится на порту 8080.
Если Firebase-ключ не настроен, публичный каталог останется доступен, а защищённые запросы
оформления заказа не пройдут аутентификацию.

### 5. Проверьте, что сервер работает

Откройте в браузере:
- http://localhost:8080/ → должно быть «FoodDelivery Server is running ✅»
- http://localhost:8080/health → JSON со статусом
- http://localhost:8080/api/restaurants → JSON со списком ресторанов
- http://localhost:8080/api/foods → JSON со списком блюд

## 🌐 REST API эндпоинты

### Публичные

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/` | Health-check (текст) |
| GET | `/health` | Health-check (JSON) |
| GET | `/api/categories` | Список категорий |
| GET | `/api/restaurants` | Список ресторанов |
| GET | `/api/restaurants/{id}` | Информация о ресторане |
| GET | `/api/restaurants/{id}/foods` | Меню ресторана |
| GET | `/api/foods?page=0&size=100` | Все блюда с пагинацией |
| GET | `/api/foods/search?q=пицца` | Поиск блюд |
| GET | `/api/foods/{id}` | Блюдо по ID (с составом) |
| GET | `/api/delivery-fee?subtotal=1200` | Расчёт стоимости доставки |

### Защищённые (требуют `Authorization: Bearer <Firebase ID Token>`)

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/api/auth/sync` | Синхронизация пользователя после Firebase-регистрации |
| POST | `/api/orders` | Оформление заказа |
| GET | `/api/orders` | История заказов текущего пользователя |

## 💰 Формула стоимости доставки

```
Сумма заказа     →  Стоимость доставки
≥ 1500 ₽         →  Бесплатно
1000–1499 ₽      →  100 ₽
500–999 ₽        →  200 ₽
< 500 ₽          →  300 ₽
```

## 🗄️ Схема базы данных

База создаётся автоматически при первом запуске. Таблицы:

- **users** (id, firebase_uid, name, email, created_at)
- **restaurants** (id, name, description, image_url, rating, delivery_time_min, min_order_amount)
- **categories** (id, name, image_url)
- **foods** (id, restaurant_id, category_id, name, description, ingredients, price, weight_grams, cook_time_min, rating, image_url, is_available)
- **orders** (id, user_id, delivery_address, subtotal, delivery_fee, total_price, status, created_at)
- **order_items** (id, order_id, food_id, quantity, price_at_order)

При первом запуске база автоматически заполняется демо-данными: 5 ресторанов и 40+ блюд с реальными ценами и составом.

## 🏷️ Лицензия

Проект разработан в учебных целях.
