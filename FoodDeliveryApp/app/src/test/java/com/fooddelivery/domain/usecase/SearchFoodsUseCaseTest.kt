package com.fooddelivery.domain.usecase

import com.fooddelivery.domain.model.Food
import com.fooddelivery.domain.repository.FoodRepository
import com.fooddelivery.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class SearchFoodsUseCaseTest {

    private lateinit var foodRepository: FoodRepository
    private lateinit var historyRepository: SearchHistoryRepository
    private lateinit var useCase: SearchFoodsUseCase

    @Before
    fun setUp() {
        foodRepository = mock()
        historyRepository = mock()
        useCase = SearchFoodsUseCase(foodRepository, historyRepository)
    }

    @Test
    fun `успешный поиск с непустым результатом добавляет запрос в историю`() = runTest {
        val foods = listOf(
            sampleFood(1L, "Пицца Маргарита"),
            sampleFood(2L, "Пицца Пепперони")
        )
        whenever(foodRepository.searchFoods("пицца")).thenReturn(Result.success(foods))

        val result = useCase("пицца")

        assertTrue(result.isSuccess)
        assertEquals(foods, result.getOrNull())
        verify(historyRepository).add("пицца")
    }

    @Test
    fun `пустой результат поиска не добавляется в историю`() = runTest {
        whenever(foodRepository.searchFoods("xyzabc"))
            .thenReturn(Result.success(emptyList()))

        val result = useCase("xyzabc")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
        verify(historyRepository, never()).add(any())
    }

    @Test
    fun `ошибка при поиске возвращает Result_failure и не пишет в историю`() = runTest {
        val exception = RuntimeException("Network error")
        whenever(foodRepository.searchFoods("любая"))
            .thenReturn(Result.failure(exception))

        val result = useCase("любая")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(historyRepository, never()).add(any())
    }

    private fun sampleFood(id: Long, name: String) = Food(
        id = id, name = name, description = "desc",
        ingredients = "тесто, томаты, моцарелла",
        price = 499.0, weightGrams = 470, cookTimeMin = 18,
        rating = 4.8, imageUrl = null, category = null, restaurant = null
    )
}
