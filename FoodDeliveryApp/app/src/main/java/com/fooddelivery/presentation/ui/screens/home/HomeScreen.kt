package com.fooddelivery.presentation.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fooddelivery.domain.model.DemoMenu
import com.fooddelivery.domain.model.Restaurant
import com.fooddelivery.domain.repository.RestaurantRepository
import com.fooddelivery.presentation.ui.components.AppButton
import com.fooddelivery.presentation.ui.components.AppCard
import com.fooddelivery.presentation.ui.components.InfoChip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RestaurantsUiState {
    data object Loading : RestaurantsUiState()
    data class Success(val list: List<Restaurant>) : RestaurantsUiState()
    data class Error(val message: String?) : RestaurantsUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository
) : ViewModel() {

    private val _state = MutableStateFlow<RestaurantsUiState>(RestaurantsUiState.Loading)
    val state: StateFlow<RestaurantsUiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = RestaurantsUiState.Loading
            restaurantRepository.getRestaurants()
                .onSuccess { remoteRestaurants ->
                    val serverRestaurants = remoteRestaurants
                        .filterNot { it.id == DemoMenu.restaurant.id }
                    val hasServerUrbanBowl = serverRestaurants.any {
                        it.name.equals(DemoMenu.restaurant.name, ignoreCase = true)
                    }
                    val restaurants = if (hasServerUrbanBowl) {
                        serverRestaurants
                    } else {
                        serverRestaurants + DemoMenu.restaurant
                    }
                    _state.value = RestaurantsUiState.Success(restaurants)
                }
                .onFailure {
                    _state.value = RestaurantsUiState.Success(listOf(DemoMenu.restaurant))
                }
        }
    }
}

@Composable
fun HomeScreen(
    onRestaurantClick: (Int) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        HomePromoBanner()

        when (val s = state) {
            is RestaurantsUiState.Loading -> CenteredLoader()
            is RestaurantsUiState.Error -> ErrorRetry(s.message, vm::load)
            is RestaurantsUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(s.list) { restaurant ->
                    RestaurantCard(restaurant) { onRestaurantClick(restaurant.id) }
                }
            }
        }
    }
}


@Composable
private fun HomePromoBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 17.dp, bottom = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ХОРОШИЙ ВКУС — БЛИЖЕ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Что-то вкусное\nуже ждёт вас",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Выбирайте, мы доставим",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                )
            }
            Surface(
                modifier = Modifier.size(78.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🍜", fontSize = 43.sp)
                }
            }
        }
    }
}

@Composable
private fun RestaurantCard(r: Restaurant, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Box(Modifier.padding(10.dp)) {
            RestaurantArtwork(
                name = r.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Star, null, Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${r.rating}", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 18.dp)) {
            Text(
                r.name, style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                r.description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(Icons.Rounded.Schedule, "${r.deliveryTimeMin} мин")
                InfoChip(Icons.Rounded.ShoppingBag, "от ${r.minOrderAmount.toInt()} ₽")
            }
        }
    }
}

@Composable
private fun RestaurantArtwork(name: String, modifier: Modifier = Modifier) {
    val icon = restaurantIconFor(name)
    val backgroundColor = when (icon) {
        "🍢", "🔥" -> MaterialTheme.colorScheme.primaryContainer
        "🍕", "🥐", "🍰" -> MaterialTheme.colorScheme.tertiaryContainer
        "🍣", "🍱", "🐟" -> MaterialTheme.colorScheme.secondaryContainer
        "🍔", "🥪", "🌮" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(116.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
            tonalElevation = 3.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 58.sp)
            }
        }
    }
}

private fun restaurantIconFor(name: String): String {
    val value = name.lowercase()
    return when {
        "шашл" in value || "гриль" in value || "барбекю" in value -> "🍢"
        "пицц" in value -> "🍕"
        "urban bowl" in value || "боул" in value -> "🥗"
        "суши" in value || "ролл" in value || "япон" in value -> "🍣"
        "бургер" in value || "бургерная" in value -> "🍔"
        "кофе" in value || "кофейн" in value -> "☕"
        "пекар" in value || "выпеч" in value -> "🥐"
        "кондитер" in value || "десерт" in value -> "🍰"
        "вок" in value || "лапш" in value || "азиат" in value -> "🍜"
        "салат" in value || "веган" in value -> "🥗"
        "тако" in value || "мексик" in value -> "🌮"
        "рыб" in value || "морепродукт" in value -> "🐟"
        else -> listOf("🍽️", "🍱", "🥙", "🥘", "🥪", "🍲")
            .let { icons -> icons[name.hashCode().and(Int.MAX_VALUE) % icons.size] }
    }
}

@Composable
fun CenteredLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ErrorRetry(message: String?, onRetry: () -> Unit) {
    Box(Modifier
        .fillMaxSize()
        .padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.WifiOff, null, Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                message ?: "Не удалось загрузить",
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            AppButton(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) { Text("Обновить") }
        }
    }
}
