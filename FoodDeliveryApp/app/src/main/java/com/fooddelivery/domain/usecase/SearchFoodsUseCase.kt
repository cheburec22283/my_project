package com.fooddelivery.domain.usecase

import com.fooddelivery.domain.model.Food
import com.fooddelivery.domain.repository.FoodRepository
import com.fooddelivery.domain.repository.SearchHistoryRepository
import javax.inject.Inject

class SearchFoodsUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) {
    suspend operator fun invoke(query: String): Result<List<Food>> {
        val result = foodRepository.searchFoods(query)
        // Сохраняем в историю только успешные непустые запросы
        result.onSuccess { list ->
            if (list.isNotEmpty()) searchHistoryRepository.add(query)
        }
        return result
    }
}
