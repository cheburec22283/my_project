package com.fooddelivery.presentation.ui.screens.food_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fooddelivery.domain.model.DemoMenu
import com.fooddelivery.domain.model.Food
import com.fooddelivery.domain.repository.CartRepository
import com.fooddelivery.domain.repository.FoodRepository
import com.fooddelivery.presentation.ui.components.AppButton
import com.fooddelivery.presentation.ui.components.FoodImage
import com.fooddelivery.presentation.ui.components.InfoChip
import com.fooddelivery.presentation.ui.components.QuantityStepper
import com.fooddelivery.presentation.ui.components.RoundBackButton
import com.fooddelivery.presentation.ui.screens.home.CenteredLoader
import com.fooddelivery.presentation.ui.screens.home.ErrorRetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DetailState {
    data object Loading : DetailState()
    data class Success(val food: Food) : DetailState()
    data class Error(val msg: String?) : DetailState()
}

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val cartRepository: CartRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val foodId: Long = savedState["foodId"] ?: 0L
    private val _state = MutableStateFlow<DetailState>(DetailState.Loading)
    val state: StateFlow<DetailState> = _state

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _state.value = DetailState.Loading
        val demoFood = DemoMenu.foodById(foodId)
        if (demoFood != null) {
            _state.value = DetailState.Success(demoFood)
        } else {
            foodRepository.getFoodById(foodId)
                .onSuccess { _state.value = DetailState.Success(it) }
                .onFailure { _state.value = DetailState.Error(it.message) }
        }
    }

    fun addToCart(food: Food, quantity: Int) {
        repeat(quantity) { cartRepository.addItem(food) }
    }
}

@Composable
fun FoodDetailScreen(
    onBack: () -> Unit,
    onAddedToCart: () -> Unit,
    vm: FoodDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var quantity by remember { mutableIntStateOf(1) }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is DetailState.Loading -> CenteredLoader()
            is DetailState.Error -> ErrorRetry(s.msg, vm::load)
            is DetailState.Success -> {
                val food = s.food
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 120.dp)
                ) {
                    // Крупное аппетитное фото
                    Box {
                        FoodImage(
                            imageUrl = food.imageUrl,
                            contentDescription = food.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 36.dp),
                            emoji = "🥘",
                            emojiSize = 96.sp
                        )
                        RoundBackButton(onClick = onBack, modifier = Modifier.padding(16.dp))
                    }

                    Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                        Text(food.name, style = MaterialTheme.typography.headlineMedium)
                        food.restaurant?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${it.name} • ${it.deliveryTimeMin} мин",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoChip(Icons.Rounded.MonitorWeight, "${food.weightGrams} г")
                            InfoChip(Icons.Rounded.Schedule, "${food.cookTimeMin} мин")
                            InfoChip(Icons.Rounded.Star, "${food.rating}")
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Описание", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            food.description, style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(24.dp))

                        // ─── СОСТАВ ─────────────────────────────────────
                        Text("Состав", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                food.ingredients,
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // ─── Нижняя плашка с ценой и кнопкой ─────────────────────
                val sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    shape = sheetShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuantityStepper(
                            quantity = quantity,
                            onDecrease = { if (quantity > 1) quantity-- },
                            onIncrease = { quantity++ }
                        )
                        Spacer(Modifier.width(12.dp))
                        AppButton(
                            onClick = {
                                vm.addToCart(food, quantity)
                                onAddedToCart()
                            },
                            trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "В корзину • ${(food.price * quantity).toInt()} ₽",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
