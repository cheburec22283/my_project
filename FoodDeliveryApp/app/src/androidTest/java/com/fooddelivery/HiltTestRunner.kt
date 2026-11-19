package com.fooddelivery

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Кастомный test runner для Hilt — заменяет Application на HiltTestApplication
 * во время инструментальных тестов.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, ctx: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, ctx)
    }
}
