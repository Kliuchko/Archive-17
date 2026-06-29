package com.kliuchko.archive17.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.repository.BookRepository
import com.kliuchko.archive17.domain.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookDetailsViewModel(
    private val workId: String,
    private val repository: BookRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookDetailsUiState(workId = workId))
    val uiState: StateFlow<BookDetailsUiState> = _uiState.asStateFlow()

    init {
        observeDetails()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    errorMessage = null,
                    message = null,
                )
            }

            when (val result = repository.refreshWorkDetails(workId)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            message = null,
                            errorMessage = null,
                        )
                    }
                }

                is RepositoryResult.Cached -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            message = result.message,
                            errorMessage = null,
                        )
                    }
                }

                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = result.message,
                            message = null,
                        )
                    }
                }
            }
        }
    }

    fun saveStatus(status: ReadingStatus) {
        val work = uiState.value.work ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    message = null,
                    errorMessage = null,
                )
            }

            when (val result = repository.saveWorkToLibrary(work, status)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            selectedStatus = result.data.entry.readingStatus,
                            message = "Saved as ${result.data.entry.readingStatus.displayName()}.",
                        )
                    }
                }

                is RepositoryResult.Cached -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            selectedStatus = result.data.entry.readingStatus,
                            message = result.message,
                        )
                    }
                }

                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    private fun observeDetails() {
        viewModelScope.launch {
            repository.observeWorkDetails(workId).collect { details ->
                _uiState.update {
                    it.copy(
                        work = details.work,
                        selectedStatus = details.libraryEntry?.readingStatus,
                        isLoading = false,
                        isCached = details.isCached,
                        isStale = details.isStale,
                        errorMessage = if (details.work == null) it.errorMessage else null,
                    )
                }
            }
        }
    }
}
