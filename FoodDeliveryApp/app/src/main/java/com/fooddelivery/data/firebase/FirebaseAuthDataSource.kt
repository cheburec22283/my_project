package com.fooddelivery.data.firebase

import com.fooddelivery.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Обёртка над Firebase Authentication.
 * Изолирует все вызовы Firebase SDK в одном классе,
 * который реализует доменный интерфейс AuthRepository.
 */
@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth
) {

    suspend fun signIn(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val u = result.user ?: error("Не удалось войти")
        return u.toDomain()
    }

    suspend fun signUp(name: String, email: String, password: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val u = result.user ?: error("Не удалось создать аккаунт")
        // Сохраняем имя пользователя в Firebase
        u.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name).build()
        ).await()
        return User(uid = u.uid, name = name, email = email)
    }

    fun signOut() { auth.signOut() }
    fun currentUser(): User? = auth.currentUser?.toDomain()
    fun isAuthenticated(): Boolean = auth.currentUser != null

    private fun com.google.firebase.auth.FirebaseUser.toDomain() = User(
        uid = uid,
        name = displayName ?: email?.substringBefore('@') ?: "Пользователь",
        email = email ?: ""
    )
}
