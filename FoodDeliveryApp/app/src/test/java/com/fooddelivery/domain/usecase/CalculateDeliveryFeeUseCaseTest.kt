package com.fooddelivery.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Модульные тесты для бизнес-логики расчёта стоимости доставки.
 *
 * Правила:
 *  - ≥ 1500 ₽ → бесплатно
 *  - 1000–1499 → 100 ₽
 *  - 500–999  → 200 ₽
 *  - < 500    → 300 ₽
 */
class CalculateDeliveryFeeUseCaseTest {

    private lateinit var useCase: CalculateDeliveryFeeUseCase

    @Before
    fun setUp() {
        useCase = CalculateDeliveryFeeUseCase()
    }

    @Test
    fun `доставка бесплатна при сумме 1500 и выше`() {
        assertEquals(0.0, useCase(1500.0), 0.0)
        assertEquals(0.0, useCase(2500.0), 0.0)
        assertEquals(0.0, useCase(10_000.0), 0.0)
    }

    @Test
    fun `доставка 100 рублей при сумме от 1000 до 1499`() {
        assertEquals(100.0, useCase(1000.0), 0.0)
        assertEquals(100.0, useCase(1250.0), 0.0)
        assertEquals(100.0, useCase(1499.99), 0.0)
    }

    @Test
    fun `доставка 200 рублей при сумме от 500 до 999`() {
        assertEquals(200.0, useCase(500.0), 0.0)
        assertEquals(200.0, useCase(750.0), 0.0)
        assertEquals(200.0, useCase(999.99), 0.0)
    }

    @Test
    fun `доставка 300 рублей при сумме менее 500`() {
        assertEquals(300.0, useCase(0.0), 0.0)
        assertEquals(300.0, useCase(100.0), 0.0)
        assertEquals(300.0, useCase(499.99), 0.0)
    }

    @Test
    fun `сумма до бесплатной доставки рассчитывается корректно`() {
        assertEquals(1500.0, useCase.amountUntilFreeDelivery(0.0), 0.0)
        assertEquals(500.0, useCase.amountUntilFreeDelivery(1000.0), 0.0)
        assertEquals(0.0, useCase.amountUntilFreeDelivery(1500.0), 0.0)
        assertEquals(0.0, useCase.amountUntilFreeDelivery(2000.0), 0.0)
    }
}
