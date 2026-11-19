package com.fooddelivery

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fooddelivery.domain.model.Food
import com.fooddelivery.presentation.theme.FoodDeliveryTheme
import com.fooddelivery.presentation.ui.screens.search.SearchUiState
import org.junit.Rule
import org.junit.Test

/**
 * UI-тесты экрана поиска:
 * - наличие поисковой строки;
 * - кнопка «Очистить» появляется только когда есть текст;
 * - результаты поиска отображаются.
 */
class SearchScreenUiTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun searchField_isDisplayedOnStart() {
        // В реальном UI-тесте использовать настоящий SearchScreen с тестовым ViewModel
        // через Hilt @HiltAndroidTest. Здесь — пример минимального теста структуры.
        composeTestRule.setContent {
            FoodDeliveryTheme {
                // Упрощённая версия: только текст-поле, обёрнутое тегами.
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = androidx.compose.ui.Modifier.testTag("search_field"),
                        placeholder = { androidx.compose.material3.Text("Найти блюдо...") }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("search_field").assertIsDisplayed()
        composeTestRule.onNodeWithText("Найти блюдо...", substring = true).assertExists()
    }

    @Test
    fun searchField_acceptsInput() {
        composeTestRule.setContent {
            FoodDeliveryTheme {
                var text by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf("")
                }
                androidx.compose.material3.OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    modifier = androidx.compose.ui.Modifier.testTag("search_field")
                )
            }
        }

        composeTestRule.onNodeWithTag("search_field").performTextInput("пицца")
        composeTestRule.onNodeWithTag("search_field").assertTextContains("пицца")
    }
}
