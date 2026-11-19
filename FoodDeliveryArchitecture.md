# Архитектура сервиса доставки еды

Актуальная модель составлена по исходному коду Android-клиента и Ktor-сервера в этом репозитории. Она описывает реализованный функционал, а не запланированные роли или операции.

Репозиторий: https://github.com/cheburec22283/my_project

## 1. Варианты использования

В клиенте реализованы вход и регистрация через Firebase Authentication, просмотр каталога и меню, поиск, корзина, оформление заказа, просмотр заказа/его статуса, профиль и выход. Отдельные акторы «Курьер» и «Администратор» не добавлены: сервер не содержит маршрутов изменения статуса доставки, администрирования каталога или получения статистики.

```mermaid
flowchart LR
    Client["Клиент"]
    Firebase["Firebase Authentication"]
    subgraph App["Android-приложение"]
        Auth["Вход и регистрация"]
        Catalog["Просмотр ресторанов и меню"]
        Search["Поиск блюд"]
        Cart["Управление корзиной"]
        Checkout["Оформление заказа"]
        Orders["Просмотр заказов"]
        Profile["Профиль и выход"]
    end
    Api["Ktor API"]
    Client --> Auth
    Client --> Catalog
    Client --> Search
    Client --> Cart
    Client --> Checkout
    Client --> Orders
    Client --> Profile
    Auth <--> Firebase
    Catalog --> Api
    Search --> Api
    Checkout --> Api
    Orders --> Api
```

## 2. Контекст и компоненты

Клиентский интерфейс построен на Jetpack Compose. Навигация ведёт к экранам каталога, поиска, корзины, оформления заказа, отслеживания и профиля. Репозитории обращаются к Ktor через Retrofit и OkHttp. Firebase выдаёт токен; AuthInterceptor передаёт его защищённым запросам. Сервер использует Ktor, Exposed/HikariCP и PostgreSQL. Локальная Room-база хранит историю поиска; DataStore используется для пользовательских настроек темы. Содержимое корзины в Room не сохраняется.

```mermaid
flowchart LR
    UI["Compose UI и навигация"]
    Repos["ViewModel и репозитории"]
    Net["Retrofit и OkHttp"]
    Auth["Firebase Auth и ID token"]
    API["Ktor API"]
    DAO["DAO и Exposed"]
    DB["PostgreSQL"]
    Local["Room: история поиска"]
    Prefs["DataStore: настройки"]
    UI --> Repos
    Repos --> Net
    Auth --> Net
    Net --> API
    API --> DAO
    DAO --> DB
    Repos --> Local
    UI --> Prefs
```

## 3. Модель предметных данных

Серверные таблицы: `users`, `restaurants`, `categories`, `foods`, `orders`, `order_items`. Клиентские модели корзины (`Cart`, `CartItem`) существуют в приложении. Адрес доставки хранится строкой в заказе; самостоятельной таблицы адресов нет. Отдельной сущности отзывов в текущем коде нет.

```mermaid
erDiagram
    USER ||--o{ ORDER : creates
    RESTAURANT ||--o{ FOOD : offers
    CATEGORY ||--o{ FOOD : classifies
    ORDER ||--|{ ORDER_ITEM : contains
    FOOD ||--o{ ORDER_ITEM : referenced_by
    USER {
        string uid
        string name
        string email
    }
    RESTAURANT {
        int id
        string name
        string description
    }
    CATEGORY {
        int id
        string name
    }
    FOOD {
        long id
        int restaurant_id
        int category_id
        string name
        decimal price
    }
    ORDER {
        long id
        string delivery_address
        decimal subtotal
        decimal delivery_fee
        decimal total_price
        string status
    }
    ORDER_ITEM {
        long id
        long order_id
        long food_id
        int quantity
        decimal price_at_order
    }
```

На стороне приложения `Cart` содержит список `CartItem`; это состояние клиентского сценария, отдельной серверной таблицы корзины нет.

## 4. Реализованный обмен при заказе

Защищённый запрос создания заказа содержит адрес доставки и список пар «идентификатор блюда — количество». Сервер проверяет наличие адреса и позиций, создаёт заказ и возвращает его идентификатор, сумму, статус и позиции. Ставка доставки также доступна через отдельный маршрут расчёта. Текущая начальная стадия заказа — `PENDING`. Маршрута смены статуса в опубликованной реализации нет, поэтому смена статусов курьером не показана как реализованная.

```mermaid
sequenceDiagram
    actor Клиент
    participant App as Android-приложение
    participant Auth as Firebase Auth
    participant API as Ktor API
    participant DB as PostgreSQL
    Клиент->>App: Выбирает блюда и адрес
    App->>Auth: Получает ID token
    Auth-->>App: Возвращает ID token
    App->>API: POST /api/orders + token
    API->>DB: Проверяет блюда и сохраняет заказ
    DB-->>API: Заказ со статусом PENDING
    API-->>App: Заказ, сумма и позиции
    App-->>Клиент: Показывает подтверждение
    App->>API: GET /api/orders/{id}
    API-->>App: Текущие данные заказа
```

## 5. Маршруты API, отражённые в диаграммах

| Метод | Маршрут | Назначение |
|---|---|---|
| GET | `/health` | Проверка состояния сервера |
| GET | `/api/categories` | Список категорий |
| GET | `/api/restaurants` | Список ресторанов |
| GET | `/api/restaurants/{id}` | Карточка ресторана |
| GET | `/api/restaurants/{id}/foods` | Меню ресторана |
| GET | `/api/foods` | Список блюд с параметрами страницы |
| GET | `/api/foods/search?q=...` | Поиск блюд |
| GET | `/api/foods/{id}` | Карточка блюда |
| GET | `/api/delivery-fee?subtotal=...` | Расчёт доставки |
| POST | `/api/auth/sync` | Синхронизация профиля после Firebase-аутентификации |
| POST | `/api/orders` | Создание заказа |
| GET | `/api/orders` | Заказы текущего пользователя |
| GET | `/api/orders/{id}` | Получение заказа |

Защищённые маршруты используют Firebase ID token. В модели, диаграммах и описании API не заявлены отдельные роли оператора/курьера, отзывы, таблица адресов, онлайн-оплата, панель статистики или серверная операция смены статуса заказа.
