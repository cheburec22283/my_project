package com.fooddelivery.presentation.ui.screens.search

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fooddelivery.domain.model.Food
import com.fooddelivery.domain.repository.SearchHistoryRepository
import com.fooddelivery.domain.usecase.SearchFoodsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

sealed class SearchUiState {
    data object Idle    : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val foods: List<Food>) : SearchUiState()
    data object Empty   : SearchUiState()
    data class Error(val message: String?) : SearchUiState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchFoodsUseCase: SearchFoodsUseCase,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    // searchQuery сохраняется при повороте через ViewModel
    var searchQuery by mutableStateOf("")
        private set

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val searchHistory: StateFlow<List<String>> = searchHistoryRepository.getHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var searchJob: Job? = null
    private var lastQuery = ""

    fun onQueryChange(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            searchJob?.cancel()
            _uiState.value = SearchUiState.Idle
            return
        }
        // Debounce 500 мс
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(query)
        }
    }

    fun clearQuery() {
        searchQuery = ""
        searchJob?.cancel()
        _uiState.value = SearchUiState.Idle
    }

    /** Повторить последний запрос (кнопка «Обновить»). */
    fun retrySearch() {
        if (lastQuery.isNotBlank()) {
            viewModelScope.launch { performSearch(lastQuery) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { searchHistoryRepository.clear() }
    }

    fun selectHistoryItem(query: String) = onQueryChange(query)

    private suspend fun performSearch(query: String) {
        lastQuery = query
        _uiState.value = SearchUiState.Loading
        searchFoodsUseCase(query)
            .onSuccess { foods ->
                _uiState.value = if (foods.isEmpty()) SearchUiState.Empty
                                 else SearchUiState.Success(foods)
            }
            .onFailure { _uiState.value = SearchUiState.Error(it.message) }
    }
}
