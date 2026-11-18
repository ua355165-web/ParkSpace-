package com.example.core.common

/**
 * Standard Clean Architecture UI state contract for asynchronous screens.
 */
sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Empty(
    val title: String,
    val message: String,
  ) : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(
    val title: String,
    val message: String,
    val retryable: Boolean = true,
  ) : UiState<Nothing>
}
