# Вкусно рядом — Android Client

Клиентская часть мобильного приложения для заказа и доставки еды.

## Визуальный стиль

Интерфейс оформлен в современной фиолетово-лавандовой стилистике: согласованные светлая и тёмная темы, мягкие скругления, обновлённая нижняя навигация и единые компоненты. Для ресторанов используются тематические локальные значки, а название «Шашлык Машлык» отображается как «Шашлычная».

Курсовая работа по дисциплине «Разработка клиент-серверных мобильных приложений», МИРЭА.

## 📱 Скриншоты

| Главная | Меню ресторана | Корзина |
|---------|----------------|---------|
| _(добавьте сюда скриншот)_ | _(скриншот)_ | _(скриншот)_ |

## 🏗️ Архитектура

Приложение построено по принципам **Clean Architecture** с тремя слоями:

```
com.fooddelivery/
├── domain/                  # Бизнес-логика, не зависит от Android
│   ├── model/               # Доменные модели (Food, Restaurant, Cart, Order...)
│   ├── repository/          # Интерфейсы репозиториев
│   └── usecase/             # Бизнес-сценарии (CalculateDeliveryFee, SearchFoods, PlaceOrder)
│
├── data/                    # Реализации источников данных
│   ├── remote/              # Retrofit API + DTO + AuthInterceptor (Firebase ID Token)
│   ├── local/               # Room (история поиска)
│   ├── firebase/            # Firebase Auth обёртка
│   ├── datastore/           # DataStore Preferences (тёмная тема)
│   ├── mapper/              # DTO → Domain маппинги
│   └── repository/          # Реализации интерфейсов из domain
│
├── presentation/            # UI слой
│   ├── navigation/          # NavGraph + bottom bar
│   ├── theme/               # Material 3 темы
│   └── ui/screens/          # Compose-экраны и ViewModel-ы
│
└── di/                      # Hilt модули
```

## 🛠️ Технологический стек

- **Язык**: Kotlin 1.9
- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt
- **Сеть**: Retrofit 2 + OkHttp + Moshi
- **Локальная БД**: Room (история поиска, до 10 элементов)
- **Настройки**: DataStore Preferences
- **Аутентификация**: Firebase Authentication
- **Изображения**: Coil
- **Навигация**: Navigation Compose
- **Архитектура**: Clean Architecture + MVVM
- **Тесты**: JUnit 4 + Mockito Kotlin + Compose UI Testing

## 🚀 Запуск проекта

### 1. Настройте Firebase

1. Откройте [Firebase Console](https://console.firebase.google.com/)
2. Создайте новый проект (или используйте существующий)
3. Добавьте приложение Android с package name **`com.fooddelivery`**
4. Скачайте `google-services.json` и замените им заглушку в `app/google-services.json`
5. В разделе **Authentication → Sign-in method** включите **Email/Password**

### 2. Запустите сервер

Серверная часть размещена в отдельном репозитории:
**https://github.com/<your-username>/food-delivery-server**

Следуйте инструкциям в README сервера для запуска (Ktor + PostgreSQL на neon.tech).

### 3. Запустите Android-приложение

1. Откройте проект в **Android Studio Iguana** (или новее)
2. Дождитесь синхронизации Gradle (5–10 минут при первом запуске)
3. Запустите на эмуляторе или физическом устройстве (API 26+)

> ⚠️ **Адрес сервера** прописан в `app/build.gradle.kts` через `BASE_URL`. По умолчанию `http://10.0.2.2:8080/` — это адрес локального сервера со стороны эмулятора. Если у вас сервер на другом хосте, измените значение.

## 🧪 Тесты

### Запустить модульные тесты (unit)
```bash
./gradlew test
```

Покрывают:
- `CalculateDeliveryFeeUseCaseTest` — 5 тестов на ступенчатую шкалу расчёта доставки
- `SearchFoodsUseCaseTest` — 3 теста с Mockito
- `PlaceOrderUseCaseTest` — 4 теста на валидацию и очистку корзины
- `CartRepositoryImplTest` — 7 тестов на логику корзины

### Запустить UI-тесты
```bash
./gradlew connectedAndroidTest
```

Покрывают:
- `LoginScreenUiTest` — 3 теста экрана авторизации
- `SearchScreenUiTest` — 2 теста экрана поиска

## 📦 Структура релиза

Приложение содержит 9 основных экранов:

1. **LoginScreen** — вход через Firebase Auth
2. **RegisterScreen** — регистрация через Firebase Auth
3. **HomeScreen** — список ресторанов с рейтингом, временем доставки и минимальной суммой заказа
4. **RestaurantMenuScreen** — меню выбранного ресторана, разбитое по категориям
5. **FoodDetailScreen** — детальная информация о блюде, включая **состав ингредиентов**, выбор количества
6. **SearchScreen** — поиск с историей (Room, до 10 элементов), debounce 500 мс, кнопка «Очистить»
7. **CartScreen** — корзина с **автоматическим расчётом стоимости доставки** по ступенчатой шкале
8. **OrderScreen** — оформление заказа с указанием адреса
9. **ProfileScreen** — переключатель тёмной темы, выход из аккаунта

## 💰 Тарифы доставки

| Сумма заказа | Стоимость доставки |
|--------------|--------------------|
| ≥ 1500 ₽     | **Бесплатно**      |
| 1000–1499 ₽  | 100 ₽              |
| 500–999 ₽    | 200 ₽              |
| < 500 ₽      | 300 ₽              |

## 🏷️ Лицензия

Проект разработан в учебных целях.
