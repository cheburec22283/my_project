package com.fooddelivery.presentation.ui.screens.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fooddelivery.domain.model.DemoMenu
import com.fooddelivery.domain.model.Food
import com.fooddelivery.domain.repository.RestaurantRepository
import com.fooddelivery.presentation.ui.components.BackHeader
import com.fooddelivery.presentation.ui.components.FoodImage
import com.fooddelivery.presentation.ui.components.AppCard
import com.fooddelivery.presentation.ui.screens.home.CenteredLoader
import com.fooddelivery.presentation.ui.screens.home.ErrorRetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MenuState {
    data object Loading : MenuState()
    data class Success(val foods: List<Food>) : MenuState()
    data class Error(val msg: String?) : MenuState()
}

@HiltViewModel
class RestaurantMenuViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val restaurantId: Int = savedState["restaurantId"] ?: 0
    private val _state = MutableStateFlow<MenuState>(MenuState.Loading)
    val state: StateFlow<MenuState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = MenuState.Loading
            if (restaurantId == DemoMenu.restaurant.id) {
                _state.value = MenuState.Success(DemoMenu.foods)
            } else {
                restaurantRepository.getRestaurantFoods(restaurantId)
                    .onSuccess { _state.value = MenuState.Success(it) }
                    .onFailure { _state.value = MenuState.Error(it.message) }
            }
        }
    }
}

@Composable
fun RestaurantMenuScreen(
    onFoodClick: (Long) -> Unit,
    onBack: () -> Unit,
    vm: RestaurantMenuViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        BackHeader(title = "Меню ресторана", onBack = onBack)

        when (val s = state) {
            is MenuState.Loading -> CenteredLoader()
            is MenuState.Error   -> ErrorRetry(s.msg, vm::load)
            is MenuState.Success -> {
                val grouped = s.foods.groupBy { it.category?.name ?: "Прочее" }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    grouped.forEach { (categoryName, foods) ->
                        item {
                            Text(categoryName, style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(top = 12.dp, bottom = 0.dp))
                        }
                        items(foods) { food ->
                            FoodMenuItem(food) { onFoodClick(food.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodMenuItem(food: Food, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            FoodImage(
                imageUrl = food.imageUrl,
                contentDescription = food.name,
                modifier = Modifier.size(96.dp),
                shape = MaterialTheme.shapes.medium,
                emojiSize = 40.sp
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(food.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text("${food.price.toInt()} ₽", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
