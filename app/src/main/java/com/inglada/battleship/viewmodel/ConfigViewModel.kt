package com.inglada.battleship.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inglada.battleship.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Represents the immutable UI state for the Configuration Screen.
 *
 * @property isLoading Indicates if the preferences are still being loaded from disk.
 * @property playerName The loaded player alias.
 * @property gridSize The loaded grid dimension.
 * @property isTimeEnabled Whether the time limit is active.
 * @property timeLimit The time limit duration in seconds.
 * @property isHardMode Whether the AI tactical mode is active.
 */
data class ConfigUiState(
    val isLoading: Boolean = true,
    val playerName: String = "",
    val gridSize: Int = 8,
    val isTimeEnabled: Boolean = false,
    val timeLimit: Int = 60,
    val isHardMode: Boolean = false
)

/**
 * ViewModel responsible for managing the configuration UI state and interacting
 * with the [UserPreferencesRepository] to persist user settings asynchronously.
 *
 * @property repository The repository handling DataStore operations.
 */
class ConfigViewModel(private val repository: UserPreferencesRepository) : ViewModel() {

    /** * StateFlow exposing the current configuration preferences to the UI.
     * Combines multiple flows from the repository into a single unified UI state.
     */
    val uiState: StateFlow<ConfigUiState> = combine(
        repository.playerName,
        repository.gridSize,
        repository.isTimeEnabled,
        repository.timeLimit,
        repository.isHardMode
    ) { name, size, timeEnabled, limit, hardMode ->
        ConfigUiState(
            isLoading = false,
            playerName = name,
            gridSize = size,
            isTimeEnabled = timeEnabled,
            timeLimit = limit,
            isHardMode = hardMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConfigUiState(isLoading = true)
    )

    /**
     * Persists the selected game configuration to the DataStore repository.
     */
    fun saveConfig(name: String, size: Int, timeEnabled: Boolean, limit: Int, hardMode: Boolean) {
        viewModelScope.launch {
            repository.savePreferences(name, size, timeEnabled, limit, hardMode)
        }
    }
}

/**
 * Factory class to instantiate [ConfigViewModel] with its required dependencies.
 */
class ConfigViewModelFactory(private val repository: UserPreferencesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConfigViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}