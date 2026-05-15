package com.inglada.battleship.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inglada.battleship.data.GameMatchEntity
import com.inglada.battleship.data.MatchRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel responsible for providing the match history data to the UI.
 * Connects to the [MatchRepository] to observe the database in real-time.
 *
 * @property repository The data source for historical matches.
 */
class HistoryViewModel(private val repository: MatchRepository) : ViewModel() {

    /**
     * A reactive state flow containing the list of all played matches.
     * It automatically updates whenever a new match is saved to the database.
     */
    val matches: StateFlow<List<GameMatchEntity>> = repository.allMatches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}

/**
 * Factory class to instantiate [HistoryViewModel] with its required repository dependency.
 */
class HistoryViewModelFactory(private val repository: MatchRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}