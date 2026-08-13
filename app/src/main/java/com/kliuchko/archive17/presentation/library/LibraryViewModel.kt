package com.kliuchko.archive17.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kliuchko.archive17.domain.repository.BookRepository
import com.kliuchko.archive17.domain.repository.LocalBookRepository
import com.kliuchko.archive17.domain.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: BookRepository,
    private val localBookRepository: LocalBookRepository,
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

    fun onFileSelected(sourceUri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, message = null) }
            when (val result = localBookRepository.importBook(sourceUri)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isImporting = false, message = "«${result.data.title}» добавлена на Полку")
                }
                is RepositoryResult.Cached -> _uiState.update { it.copy(isImporting = false) }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isImporting = false, message = result.message)
                }
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeLibrary() {
        viewModelScope.launch {
            selectedFilter
                .flatMapLatest { filter ->
                    combine(
                        repository.observeLibrary(filter.readingStatus),
                        localBookRepository.observeLocalBooks(filter.readingStatus),
                    ) { books, localBooks -> books to localBooks }
                }
                .collectLatest { (books, localBooks) ->
                    val locallyStoredWorkIds = localBooks.mapNotNullTo(mutableSetOf()) { book ->
                        book.workId
                    }
                    _uiState.update {
                        it.copy(
                            selectedFilter = selectedFilter.value,
                            books = books.filterNot { book ->
                                book.work.id in locallyStoredWorkIds
                            },
                            localBooks = localBooks,
                            isLoading = false,
                        )
                    }
                }
        }
    }
}
