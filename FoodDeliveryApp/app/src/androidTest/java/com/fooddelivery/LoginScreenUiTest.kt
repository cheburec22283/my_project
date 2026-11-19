package com.fooddelivery

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fooddelivery.presentation.theme.FoodDeliveryTheme
import com.fooddelivery.presentation.ui.screens.auth.AuthUiState
import com.fooddelivery.presentation.ui.screens.auth.LoginScreen
import org.junit.Rule
import org.junit.Test

/**
 * UI-тесты экрана авторизации.
 * Тестирует, что элементы интерфейса присутствуют и реагируют на ввод.
 */
class LoginScreenUiTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_hasEmailAndPasswordFields() {
        composeTestRule.setContent {
            FoodDeliveryTheme {
                LoginScreen(
                    onLoginSuccess = {},
                    onNavigateToRegister = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("login_email").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_password").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_button").assertIsDisplayed()
    }

    @Test
    fun loginScreen_emailFieldAcceptsInput() {
        composeTestRule.setContent {
            FoodDeliveryTheme {
                LoginScreen(onLoginSuccess = {}, onNavigateToRegister = {})
            }
        }

        composeTestRule.onNodeWithTag("login_email")
            .performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("login_email")
            .assertTextContains("test@example.com")
    }

    @Test
    fun loginScreen_emptyFieldsShowError() {
        composeTestRule.setContent {
            FoodDeliveryTheme {
                LoginScreen(onLoginSuccess = {}, onNavigateToRegister = {})
            }
        }

        // Нажимаем «Войти» без заполнения полей
        composeTestRule.onNodeWithTag("login_button").performClick()

        // Должно появиться сообщение об ошибке
        composeTestRule.onNodeWithText("Заполните email и пароль", substring = true)
            .assertExists()
    }
}
