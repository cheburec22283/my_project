package com.fooddelivery.data.remote.interceptor

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Добавляет в каждый исходящий HTTP-запрос заголовок
 * Authorization: Bearer <Firebase ID Token>.
 *
 * Токен получается из FirebaseAuth.currentUser. Если пользователь не залогинен,
 * заголовок не добавляется (для публичных эндпоинтов).
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Получаем Firebase ID Token (если пользователь авторизован).
        // runBlocking допустим только внутри Interceptor — OkHttp работает в фоне.
        val token: String? = try {
            FirebaseAuth.getInstance().currentUser
                ?.let { runBlocking { it.getIdToken(false).await().token } }
        } catch (e: Exception) {
            null
        }

        val newRequest = if (token != null) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else request

        return chain.proceed(newRequest)
    }
}
