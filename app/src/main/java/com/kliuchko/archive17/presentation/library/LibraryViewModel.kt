package com.kliuchko.archive17.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kliuchko.archive17.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: BookRepository,
) : ViewModel() {
    private val selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    private val _uiState = MutableStateFlow(LibraryUiState())

    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeLibrary()
    }

    fun onFilterSelected(filter: LibraryFilter) {
        selectedFilter.value = filter
        _uiState.update {
            it.copy(
                selectedFilter = filter,
                isLoading = true,
            )
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeLibrary() {
        viewModelScope.launch {
            selectedFilter
                .flatMapLatest { filter ->
                    repository.observeLibrary(filter.readingStatus)
                }
                .collectLatest { books ->
                    _uiState.update {
                        it.copy(
                            selectedFilter = selectedFilter.value,
                            books = books,
                            isLoading = false,
                        )
                    }
                }
        }
    }
}
