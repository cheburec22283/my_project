package com.fooddelivery.presentation.ui.screens.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fooddelivery.domain.model.Order
import com.fooddelivery.domain.usecase.GetOrderUseCase
import com.fooddelivery.presentation.ui.components.AppCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TrackingUiState {
    data object Loading : TrackingUiState()
    data class Success(val order: Order) : TrackingUiState()
    data class Error(val message: String?) : TrackingUiState()
}

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val getOrderUseCase: GetOrderUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<TrackingUiState>(TrackingUiState.Loading)
    val state: StateFlow<TrackingUiState> = _state

    fun loadOrder(orderId: Long) {
        viewModelScope.launch {
            _state.value = TrackingUiState.Loading
            // Имитация опроса сервера для обновлений статуса
            while (true) {
                getOrderUseCase(orderId)
                    .onSuccess { _state.value = TrackingUiState.Success(it) }
                    .onFailure { _state.value = TrackingUiState.Error(it.message); return@launch }

                delay(10000) // Опрос каждые 10 секунд
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    orderId: Long,
    onClose: () -> Unit,
    vm: TrackingViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(orderId) {
        vm.loadOrder(orderId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Заказ #$orderId", style = MaterialTheme.typography.titleLarge)
                },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(Modifier
            .fillMaxSize()
            .padding(padding)) {
            when (val s = state) {
                is TrackingUiState.Loading -> CircularProgressIndicator(
                    Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary
                )

                is TrackingUiState.Error -> Text(
                    s.message ?: "Ошибка",
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )

                is TrackingUiState.Success -> TrackingContent(s.order)
            }
        }
    }
}

@Composable
private fun TrackingContent(order: Order) {
    val statuses = listOf(
        "PENDING" to Triple("Заказ принят", Icons.Rounded.Schedule, "Мы получили ваш заказ"),
        "PREPARING" to Triple("Готовится", Icons.Rounded.Restaurant, "Повар уже вовсю старается"),
        "SHIPPING" to Triple("В пути", Icons.Rounded.LocalShipping, "Курьер мчится к вам"),
        "DELIVERED" to Triple("Доставлен", Icons.Rounded.Check, "Приятного аппетита!")
    )

    val currentStatusIndex = statuses.indexOfFirst { it.first == order.status }
    val activeColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.surfaceVariant

    Column(Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "Статус: ${order.status}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        Spacer(Modifier.height(32.dp))

        statuses.forEachIndexed { index, (code, info) ->
            val (title, icon, desc) = info
            val isCompleted = index <= currentStatusIndex
            val isCurrent = index == currentStatusIndex

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.tertiary
                                else if (isCompleted) activeColor else idleColor,
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon, null,
                            tint = if (isCurrent) MaterialTheme.colorScheme.onTertiary
                            else if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (index < statuses.size - 1) {
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .background(
                                    if (index < currentStatusIndex) activeColor else idleColor,
                                    RoundedCornerShape(16.dp)
                                )
                        )
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        desc, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Адрес доставки", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(order.deliveryAddress, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Сумма заказа: ${order.totalPrice.toInt()} ₽",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
