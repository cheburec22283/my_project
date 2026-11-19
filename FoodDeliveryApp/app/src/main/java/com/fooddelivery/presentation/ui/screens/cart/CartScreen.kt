package com.fooddelivery.presentation.ui.screens.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fooddelivery.domain.model.Cart
import com.fooddelivery.domain.model.CartItem
import com.fooddelivery.domain.model.Food
import com.fooddelivery.domain.repository.CartRepository
import com.fooddelivery.domain.usecase.CalculateDeliveryFeeUseCase
import com.fooddelivery.presentation.ui.components.AppButton
import com.fooddelivery.presentation.ui.components.AppCard
import com.fooddelivery.presentation.ui.components.EmptyState
import com.fooddelivery.presentation.ui.components.FoodImage
import com.fooddelivery.presentation.ui.components.QuantityStepper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    val calculateDeliveryFee: CalculateDeliveryFeeUseCase
) : ViewModel() {

    val cart: StateFlow<Cart> = cartRepository.observeCart()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Cart())

    val deliveryFee: StateFlow<Double> = cart
        .map { calculateDeliveryFee(it.subtotal) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val total: StateFlow<Double> = combine(cart, deliveryFee) { c, fee -> c.subtotal + fee }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val amountUntilFree: StateFlow<Double> = cart
        .map { calculateDeliveryFee.amountUntilFreeDelivery(it.subtotal) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1500.0)

    fun increase(food: Food) = cartRepository.addItem(food)
    fun decrease(foodId: Long) = cartRepository.decreaseItem(foodId)
    fun remove(foodId: Long) = cartRepository.removeItem(foodId)
}

@Composable
fun CartScreen(
    onCheckout: () -> Unit,
    vm: CartViewModel = hiltViewModel()
) {
    val cart by vm.cart.collectAsState()
    val deliveryFee by vm.deliveryFee.collectAsState()
    val total by vm.total.collectAsState()
    val untilFree by vm.amountUntilFree.collectAsState()

    if (cart.items.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.ShoppingCart,
            title = "Корзина пуста",
            subtitle = "Добавьте блюда из меню"
        )
        return
    }

    Column(Modifier.fillMaxSize()) {

        // Подсказка о бесплатной доставке
        if (untilFree > 0) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.LocalShipping, null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Добавьте ещё на ${untilFree.toInt()} ₽ для бесплатной доставки",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(cart.items) { item ->
                CartItemRow(
                    item,
                    onIncrease = { vm.increase(item.food) },
                    onDecrease = { vm.decrease(item.food.id) },
                    onRemove = { vm.remove(item.food.id) })
            }
        }

        // ─── Итоговая плашка ────────────────────────────────────────────
        val sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = sheetShape,
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                SummaryRow("Сумма за блюда", "${cart.subtotal.toInt()} ₽")
                Spacer(Modifier.height(6.dp))
                SummaryRow(
                    label = "Доставка",
                    value = if (deliveryFee == 0.0) "Бесплатно" else "${deliveryFee.toInt()} ₽",
                    valueColor = if (deliveryFee == 0.0)
                        MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(
                    Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Итого", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${total.toInt()} ₽",
                        modifier = Modifier.testTag("cart_total"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(16.dp))
                AppButton(
                    onClick = onCheckout,
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_button")
                ) {
                    Text("Оформить заказ", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            FoodImage(
                imageUrl = item.food.imageUrl,
                contentDescription = item.food.name,
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.food.name, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${item.food.price.toInt()} ₽ × ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                QuantityStepper(
                    quantity = item.quantity,
                    onDecrease = onDecrease,
                    onIncrease = onIncrease
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Rounded.Delete, "Удалить", Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String, value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}
