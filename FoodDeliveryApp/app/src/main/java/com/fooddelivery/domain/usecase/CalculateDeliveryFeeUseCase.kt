package com.fooddelivery.domain.usecase

import javax.inject.Inject

/**
 * Расчёт стоимости доставки по ступенчатой шкале.
 *
 * Правила:
 *  - сумма ≥ 1500 ₽  → бесплатная доставка
 *  - сумма 1000–1499 → 100 ₽
 *  - сумма 500–999   → 200 ₽
 *  - сумма < 500     → 300 ₽
 *
 * Чистая бизнес-логика без зависимостей — легко тестируется unit-тестами.
 */
class CalculateDeliveryFeeUseCase @Inject constructor() {

    operator fun invoke(subtotal: Double): Double = when {
        subtotal >= 1500.0 -> 0.0
        subtotal >= 1000.0 -> 100.0
        subtotal >= 500.0  -> 200.0
        else               -> 300.0
    }

    /** Сколько осталось до бесплатной доставки. */
    fun amountUntilFreeDelivery(subtotal: Double): Double =
        (1500.0 - subtotal).coerceAtLeast(0.0)
}
