package com.fooddelivery.presentation.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fooddelivery.domain.model.User
import com.fooddelivery.domain.repository.AuthRepository
import com.fooddelivery.domain.repository.ThemeRepository
import com.fooddelivery.presentation.ui.components.AppButton
import com.fooddelivery.presentation.ui.components.AppCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val user: StateFlow<User?> = MutableStateFlow(authRepository.currentUser())

    val isDarkMode: StateFlow<Boolean> = themeRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setDarkMode(enabled) }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }
}

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val user by vm.user.collectAsState()
    val isDark by vm.isDarkMode.collectAsState()

    Column(Modifier.fillMaxSize()) {

        Column(Modifier
            .weight(1f)
            .padding(horizontal = 20.dp, vertical = 12.dp)) {
            // ─── Карточка пользователя ─────────────────────────────────
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Person, null,
                                modifier = Modifier.size(38.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(user?.name ?: "Гость", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            user?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))

            // ─── Настройки ────────────────────────────────────────────
            Text("Настройки", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.DarkMode, null, Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text("Тёмная тема", style = MaterialTheme.typography.titleSmall)
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = vm::setDarkMode,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }
        }

        AppButton(
            onClick = { vm.logout(onLogout) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ExitToApp, null)
            Spacer(Modifier.width(8.dp))
            Text("Выйти из аккаунта", style = MaterialTheme.typography.labelLarge)
        }
    }
}
