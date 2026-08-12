package com.kliuchko.archive17.presentation.freedetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kliuchko.archive17.domain.repository.FreeBookRepository
import com.kliuchko.archive17.domain.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FreeBookDetailsViewModel(
    private val editionId: String,
    private val repository: FreeBookRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FreeBookDetailsUiState())
    val uiState: StateFlow<FreeBookDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.getBookDetails(editionId)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isLoading = false, details = result.data)
                }
                is RepositoryResult.Cached -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        details = result.data,
                        message = result.message,
                    )
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun downloadBook() {
        val book = _uiState.value.details?.book ?: return
        if (_uiState.value.isDownloading || _uiState.value.isPreparingToRead || !book.isDownloadable) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isDownloading = true, errorMessage = null, message = null)
            }
            when (val result = repository.downloadToShelf(book)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadedBookId = result.data.id,
                    )
                }
                is RepositoryResult.Cached -> _uiState.update {
                    it.copy(isDownloading = false, message = result.message)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isDownloading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun onDownloadedBookHandled() {
        _uiState.update { it.copy(downloadedBookId = null) }
    }

    fun readNow() {
        val book = _uiState.value.details?.book ?: return
        if (_uiState.value.isDownloading || _uiState.value.isPreparingToRead || !book.isDownloadable) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isPreparingToRead = true, errorMessage = null, message = null)
            }
            when (val result = repository.downloadForReading(book)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isPreparingToRead = false, temporaryBook = result.data)
                }
                is RepositoryResult.Cached -> _uiState.update {
                    it.copy(isPreparingToRead = false, message = result.message)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isPreparingToRead = false, errorMessage = result.message)
                }
            }
        }
    }

    fun onTemporaryBookHandled() {
        _uiState.update { it.copy(temporaryBook = null) }
    }
}
