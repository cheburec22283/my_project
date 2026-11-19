package com.fooddelivery.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fooddelivery.domain.repository.AuthRepository
import com.fooddelivery.domain.repository.CartRepository
import com.fooddelivery.domain.repository.ThemeRepository
import com.fooddelivery.presentation.theme.FoodDeliveryTheme
import com.fooddelivery.presentation.ui.screens.auth.LoginScreen
import com.fooddelivery.presentation.ui.screens.auth.RegisterScreen
import com.fooddelivery.presentation.ui.screens.cart.CartScreen
import com.fooddelivery.presentation.ui.screens.food_detail.FoodDetailScreen
import com.fooddelivery.presentation.ui.screens.home.DashboardScreen
import com.fooddelivery.presentation.ui.screens.home.HomeScreen
import com.fooddelivery.presentation.ui.screens.order.OrderScreen
import com.fooddelivery.presentation.ui.screens.order.OrderTrackingScreen
import com.fooddelivery.presentation.ui.screens.profile.ProfileScreen
import com.fooddelivery.presentation.ui.screens.restaurant.RestaurantMenuScreen
import com.fooddelivery.presentation.ui.screens.search.SearchScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val RESTAURANTS = "restaurants"
    const val RESTAURANT_MENU = "restaurant_menu/{restaurantId}"
    const val FOOD_DETAIL = "food_detail/{foodId}"
    const val SEARCH = "search"
    const val CART = "cart"
    const val ORDER = "order"
    const val ORDER_TRACKING = "order_tracking/{orderId}"
    const val PROFILE = "profile"

    fun restaurantMenu(id: Int) = "restaurant_menu/$id"
    fun foodDetail(id: Long) = "food_detail/$id"
    fun orderTracking(id: Long) = "order_tracking/$id"
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
    private val cartRepository: CartRepository
) : ViewModel() {
    val isDarkMode: StateFlow<Boolean> = themeRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val cartItemsCount: StateFlow<Int> = cartRepository.observeCart()
        .map { it.itemCount }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun isAuthenticated(): Boolean = authRepository.isAuthenticated()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDeliveryApp(rootVm: RootViewModel = hiltViewModel()) {
    val isDark by rootVm.isDarkMode.collectAsState()
    val cartCount by rootVm.cartItemsCount.collectAsState()
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value
        ?.destination?.route
    val routesWithHomeBar = listOf(
        Routes.RESTAURANTS,
        Routes.SEARCH,
        Routes.CART,
        Routes.PROFILE
    )
    val topBarTitle = when (currentRoute) {
        Routes.RESTAURANTS -> "Рестораны"
        Routes.SEARCH -> "Поиск блюд"
        Routes.CART -> "Корзина"
        Routes.PROFILE -> "Профиль"
        else -> "Главная"
    }

    FoodDeliveryTheme(darkTheme = isDark) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (currentRoute in routesWithHomeBar) {
                    TopAppBar(
                        title = { Text(topBarTitle) },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.HOME) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Вернуться к главному меню"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        ) { padding ->
            val startDestination = if (rootVm.isAuthenticated()) {
                Routes.HOME
            } else {
                Routes.LOGIN
            }

            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate(Routes.REGISTER)
                        }
                    )
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(
                        onRegisterSuccess = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = { navController.popBackStack() }
                    )
                }
                composable(Routes.HOME) {
                    DashboardScreen(
                        onRestaurantsClick = {
                            navController.navigate(Routes.RESTAURANTS)
                        },
                        onSearchClick = { navController.navigate(Routes.SEARCH) },
                        onCartClick = { navController.navigate(Routes.CART) },
                        onProfileClick = { navController.navigate(Routes.PROFILE) },
                        cartCount = cartCount,
                        onFeaturedRestaurantClick = { id ->
                            navController.navigate(Routes.restaurantMenu(id))
                        }
                    )
                }
                composable(Routes.RESTAURANTS) {
                    HomeScreen(onRestaurantClick = { id ->
                        navController.navigate(Routes.restaurantMenu(id))
                    })
                }
                composable(
                    route = Routes.RESTAURANT_MENU,
                    arguments = listOf(navArgument("restaurantId") {
                        type = NavType.IntType
                    })
                ) {
                    RestaurantMenuScreen(
                        onFoodClick = { id ->
                            navController.navigate(Routes.foodDetail(id))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Routes.FOOD_DETAIL,
                    arguments = listOf(navArgument("foodId") {
                        type = NavType.LongType
                    })
                ) {
                    FoodDetailScreen(
                        onBack = { navController.popBackStack() },
                        onAddedToCart = { navController.navigate(Routes.CART) }
                    )
                }
                composable(Routes.SEARCH) {
                    SearchScreen(onFoodClick = { id ->
                        navController.navigate(Routes.foodDetail(id))
                    })
                }
                composable(Routes.CART) {
                    CartScreen(onCheckout = { navController.navigate(Routes.ORDER) })
                }
                composable(Routes.ORDER) {
                    OrderScreen(
                        onBack = { navController.popBackStack() },
                        onOrderPlaced = { orderId ->
                            navController.navigate(Routes.orderTracking(orderId)) {
                                popUpTo(Routes.HOME)
                            }
                        }
                    )
                }
                composable(
                    route = Routes.ORDER_TRACKING,
                    arguments = listOf(navArgument("orderId") {
                        type = NavType.LongType
                    })
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
                    OrderTrackingScreen(
                        orderId = orderId,
                        onClose = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    })
                }
            }
        }
    }
}
