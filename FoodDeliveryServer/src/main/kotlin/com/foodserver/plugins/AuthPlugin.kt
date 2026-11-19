package com.foodserver.plugins

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream

private val logger = LoggerFactory.getLogger("AuthPlugin")

/**
 * Principal, хранящий уникальный идентификатор пользователя Firebase (uid)
 * и его email. Доступен в обработчиках через call.principal<FirebasePrincipal>()
 */
data class FirebasePrincipal(
    val uid: String,
    val email: String?,
    val name: String?
) : Principal

/**
 * Глобальный флаг: удалось ли инициализировать Firebase Admin SDK.
 * Если false — защищённые эндпоинты будут отдавать 503.
 */
@Volatile var firebaseReady: Boolean = false
    private set

fun Application.configureAuthentication() {

    // Prefer an external service-account file so credentials are not shipped
    // inside the source archive. A local resources file remains supported.
    try {
        val credentialPath = sequenceOf(
            System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH"),
            System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        ).firstOrNull { !it.isNullOrBlank() }

        val resourceStream = if (credentialPath.isNullOrBlank()) {
            this::class.java.classLoader
                .getResourceAsStream("firebase-admin-sdk.json")
        } else {
            null
        }

        val credentialStream = if (!credentialPath.isNullOrBlank()) {
            FileInputStream(File(credentialPath))
        } else {
            resourceStream
        }

        if (credentialStream == null) {
            logger.warn("Firebase service account key was not configured.")
            logger.warn(
                "Set FIREBASE_SERVICE_ACCOUNT_PATH or add " +
                    "firebase-admin-sdk.json to src/main/resources."
            )
            logger.warn("Protected order endpoints will remain unavailable.")
        } else {
            credentialStream.use { stream ->
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build()
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options)
                }
            }
            firebaseReady = true
            logger.info("Firebase Admin SDK initialized successfully")
        }
    } catch (e: Exception) {
        logger.error("Firebase Admin SDK initialization failed: ${e.message}", e)
    }

    // ─── Bearer-аутентификация через Firebase ID Token ─────────────────
    install(Authentication) {
        bearer("firebase-auth") {
            realm = "FoodDelivery"
            authenticate { tokenCredential ->
                if (!firebaseReady) {
                    logger.warn("Firebase не готов — отказ в аутентификации")
                    return@authenticate null
                }
                try {
                    val decoded = FirebaseAuth.getInstance()
                        .verifyIdToken(tokenCredential.token)
                    FirebasePrincipal(
                        uid = decoded.uid,
                        email = decoded.email,
                        name = decoded.name
                    )
                } catch (e: Exception) {
                    logger.warn("Невалидный Firebase токен: ${e.message}")
                    null
                }
            }
        }
    }
}
