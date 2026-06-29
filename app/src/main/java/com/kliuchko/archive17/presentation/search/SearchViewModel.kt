package com.kliuchko.archive17.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kliuchko.archive17.domain.repository.BookRepository
import com.kliuchko.archive17.domain.repository.RepositoryResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: BookRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())

    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            query
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
                .collectLatest(::search)
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
        _uiState.update {
            it.copy(
                query = value,
                isLoading = false,
                errorMessage = null,
                hasSearched = false,
                books = if (value.trim().length < SearchUiState.MIN_QUERY_LENGTH) {
                    emptyList()
                } else {
                    it.books
                },
            )
        }
    }

    private suspend fun search(value: String) {
        val normalizedQuery = value.trim()
        if (normalizedQuery.length < SearchUiState.MIN_QUERY_LENGTH) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    books = emptyList(),
                    errorMessage = null,
                    hasSearched = false,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
            )
        }

        when (val result = repository.searchBooks(normalizedQuery)) {
            is RepositoryResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        books = result.data,
                        errorMessage = null,
                        hasSearched = true,
                    )
                }
            }

            is RepositoryResult.Cached -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        books = result.data,
                        errorMessage = result.message,
                        hasSearched = true,
                    )
                }
            }

            is RepositoryResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        books = emptyList(),
                        errorMessage = result.message,
                        hasSearched = true,
                    )
                }
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 350L
    }
}
