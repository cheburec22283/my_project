package com.foodserver

import com.foodserver.plugins.*
import io.ktor.server.application.*
import io.ktor.server.cio.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureSerialization()    // JSON, CORS, Logging, StatusPages
    configureAuthentication()   // Firebase Admin SDK + bearer-auth
    configureDatabases()        // PostgreSQL (neon.tech)
    configureRouting()          // REST API маршруты
}
