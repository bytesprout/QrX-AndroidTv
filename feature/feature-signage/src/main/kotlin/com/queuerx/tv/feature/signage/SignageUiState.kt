package com.queuerx.tv.feature.signage

sealed interface SignageUiState {
    data object Loading : SignageUiState
    data class Success(val mode: DisplayMode, val emergencyMessage: String? = null) : SignageUiState
    data class Error(val message: String) : SignageUiState
    data object Empty : SignageUiState
    data object Offline : SignageUiState
}

enum class DisplayMode {
    WAITING_HALL,
    RECEPTION,
    PHARMACY,
    FULLSCREEN_TOKEN,
    EMERGENCY_OVERRIDE,
    MULTI_DEPARTMENT_SPLIT
}
