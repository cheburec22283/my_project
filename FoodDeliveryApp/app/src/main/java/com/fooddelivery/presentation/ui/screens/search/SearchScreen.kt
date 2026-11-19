package com.fooddelivery.presentation.ui.screens.search

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fooddelivery.domain.model.Food
import com.fooddelivery.presentation.ui.components.AppButton
import com.fooddelivery.presentation.ui.components.AppCard
import com.fooddelivery.presentation.ui.components.EmptyState
import com.fooddelivery.presentation.ui.components.FoodImage
import com.fooddelivery.presentation.ui.components.appFieldColors
import com.fooddelivery.presentation.ui.screens.home.CenteredLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onFoodClick: (Long) -> Unit,
    vm: SearchViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val history by vm.searchHistory.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize()) {

        // ─── Поисковая строка ─────────────────────────────────────────
        OutlinedTextField(
            value = vm.searchQuery,
            onValueChange = vm::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("search_field"),
            placeholder = { Text("Найти блюдо...") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                // Кнопка «Очистить» видна только при наличии текста
                if (vm.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            vm.clearQuery()
                            keyboard?.hide()
                        },
                        modifier = Modifier.testTag("clear_button")
                    ) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = appFieldColors()
        )

        // ─── Контент ──────────────────────────────────────────────────
        when (val state = uiState) {
            is SearchUiState.Idle -> {
                // История поиска
                if (history.isNotEmpty()) {
                    HistorySection(
                        history,
                        onItemClick = vm::selectHistoryItem,
                        onClearAll = vm::clearHistory
                    )
                } else {
                    EmptyState(
                        Icons.Rounded.Search, "Начните поиск",
                        "Пиццу, суши или что-нибудь домашнее"
                    )
                }
            }

            is SearchUiState.Loading -> CenteredLoader()
            is SearchUiState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.foods) { food ->
                        SearchResultCard(food) { onFoodClick(food.id) }
                    }
                }
            }

            is SearchUiState.Empty -> {
                EmptyState(
                    Icons.Rounded.SearchOff,
                    "Ничего не найдено",
                    "Попробуйте изменить запрос"
                )
            }

            is SearchUiState.Error -> {
                Box(Modifier
                    .fillMaxSize()
                    .padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
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
                            state.message ?: "Ошибка загрузки",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        AppButton(
                            onClick = vm::retrySearch,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) { Text("Обновить") }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySection(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("История поиска", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClearAll) {
                Text(
                    "Очистить", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        LazyColumn {
            items(history) { query ->
                ListItem(
                    headlineContent = { Text(query, style = MaterialTheme.typography.bodyLarge) },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.History, null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    modifier = Modifier.clickable { onItemClick(query) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(food: Food, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            FoodImage(
                imageUrl = food.imageUrl,
                contentDescription = food.name,
                modifier = Modifier.size(84.dp),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    food.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                food.restaurant?.let {
                    Text(
                        it.name, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    food.description, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${food.price.toInt()} ₽", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
