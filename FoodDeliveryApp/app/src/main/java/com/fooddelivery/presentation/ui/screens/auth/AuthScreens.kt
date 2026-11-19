package com.fooddelivery.presentation.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fooddelivery.presentation.ui.components.AppButton
import com.fooddelivery.presentation.ui.components.appFieldColors

@Composable
private fun BrandMark() {
    Surface(
        modifier = Modifier.size(88.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.RestaurantMenu,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ─── Login ──────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state) {
        if (state is AuthUiState.Success) {
            vm.reset(); onLoginSuccess()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandMark()
        Spacer(Modifier.height(22.dp))
        Text(
            "Вкусно рядом", style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Любимые блюда — в пару касаний",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))

        OutlinedTextField(
            value = vm.email,
            onValueChange = { vm.email = it },
            label = { Text("Email") },
            placeholder = { Text("example@mail.ru") },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = MaterialTheme.shapes.medium,
            colors = appFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email")
        )
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = vm.password,
            onValueChange = { vm.password = it },
            label = { Text("Пароль") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = MaterialTheme.shapes.medium,
            colors = appFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password")
        )
        Spacer(Modifier.height(20.dp))

        if (state is AuthUiState.Error) {
            Text(
                (state as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
        }

        AppButton(
            onClick = vm::signIn,
            enabled = state !is AuthUiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_button")
        ) {
            if (state is AuthUiState.Loading) {
                CircularProgressIndicator(
                    Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else Text("Войти", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text(
                "Нет аккаунта? Зарегистрироваться",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ─── Register ────────────────────────────────────────────────────────────

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state) {
        if (state is AuthUiState.Success) {
            vm.reset(); onRegisterSuccess()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandMark()
        Spacer(Modifier.height(22.dp))
        Text(
            "Создать аккаунт", style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Пусть вкусное будет ещё ближе",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = vm.name, onValueChange = { vm.name = it },
            label = { Text("Имя") }, placeholder = { Text("Иван") },
            leadingIcon = { Icon(Icons.Rounded.Person, null) },
            singleLine = true, shape = MaterialTheme.shapes.medium,
            colors = appFieldColors(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = vm.email, onValueChange = { vm.email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Rounded.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true, shape = MaterialTheme.shapes.medium,
            colors = appFieldColors(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = vm.password, onValueChange = { vm.password = it },
            label = { Text("Пароль") },
            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, shape = MaterialTheme.shapes.medium,
            colors = appFieldColors(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = vm.confirmPassword, onValueChange = { vm.confirmPassword = it },
            label = { Text("Повторите пароль") },
            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, shape = MaterialTheme.shapes.medium,
            colors = appFieldColors(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        if (state is AuthUiState.Error) {
            Text(
                (state as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
        }

        AppButton(
            onClick = vm::signUp,
            enabled = state !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state is AuthUiState.Loading) {
                CircularProgressIndicator(
                    Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp
                )
            } else Text("Зарегистрироваться", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text(
                "Уже есть аккаунт? Войти", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
