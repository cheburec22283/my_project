package com.fooddelivery.presentation.ui.screens.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fooddelivery.domain.repository.CartRepository
import com.fooddelivery.domain.usecase.CalculateDeliveryFeeUseCase
import com.fooddelivery.domain.usecase.PlaceOrderUseCase
import com.fooddelivery.presentation.ui.components.BackHeader
import com.fooddelivery.presentation.ui.components.AppButton
import com.fooddelivery.presentation.ui.components.AppCard
import com.fooddelivery.presentation.ui.components.appFieldColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OrderUiState {
    data object Idle : OrderUiState()
    data object Loading : OrderUiState()
    data class Success(val orderId: Long) : OrderUiState()
    data class Error(val message: String?) : OrderUiState()
}

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val cartRepository: CartRepository,
    private val calculateDeliveryFee: CalculateDeliveryFeeUseCase
) : ViewModel() {

    val cart = cartRepository.observeCart()
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.fooddelivery.domain.model.Cart())

    val deliveryFee = cart.map { calculateDeliveryFee(it.subtotal) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val total = combine(cart, deliveryFee) { c, fee -> c.subtotal + fee }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    private val _state = MutableStateFlow<OrderUiState>(OrderUiState.Idle)
    val state: StateFlow<OrderUiState> = _state

    fun placeOrder(address: String) {
        viewModelScope.launch {
            _state.value = OrderUiState.Loading
            placeOrderUseCase(address, cart.value.items)
                .onSuccess { _state.value = OrderUiState.Success(it.id) }
                .onFailure { _state.value = OrderUiState.Error(it.message) }
        }
    }
}

@Composable
fun OrderScreen(
    onBack: () -> Unit,
    onOrderPlaced: (Long) -> Unit,
    vm: OrderViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val cart by vm.cart.collectAsState()
    val deliveryFee by vm.deliveryFee.collectAsState()
    val total by vm.total.collectAsState()
    var address by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is OrderUiState.Success) {
            onOrderPlaced((state as OrderUiState.Success).orderId)
        }
    }

    Column(Modifier.fillMaxSize()) {
        BackHeader(title = "Оформление заказа", onBack = onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Адрес доставки") },
                placeholder = { Text("ул. Пушкина, д. 1, кв. 2") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                colors = appFieldColors()
            )
            Spacer(Modifier.height(28.dp))

            Text("Сводка заказа", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    cart.items.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.food.name} × ${item.quantity}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(12.dp))
                            Text("${item.total.toInt()} ₽",
                                style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Доставка", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (deliveryFee == 0.0) "Бесплатно" else "${deliveryFee.toInt()} ₽",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (deliveryFee == 0.0) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Итого", style = MaterialTheme.typography.titleLarge)
                        Text("${total.toInt()} ₽", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (state is OrderUiState.Error) {
                Spacer(Modifier.height(12.dp))
                Text((state as OrderUiState.Error).message ?: "Ошибка",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }

        AppButton(
            onClick = { vm.placeOrder(address) },
            enabled = address.isNotBlank() && state !is OrderUiState.Loading,
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            if (state is OrderUiState.Loading) {
                CircularProgressIndicator(Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else Text("Подтвердить заказ", style = MaterialTheme.typography.labelLarge)
        }
    }
}
