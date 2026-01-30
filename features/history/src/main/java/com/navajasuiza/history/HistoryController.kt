package com.navajasuiza.history

import com.navajasuiza.data.model.CopiedTextEntity
import com.navajasuiza.data.repository.ClipboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryController(
    private val repository: ClipboardRepository,
    private val scope: CoroutineScope
) {
    private val searchQuery = MutableStateFlow("")

    val filteredHistory = combine(repository.allHistory, searchQuery) { history, query ->
        if (query.isBlank()) {
            history
        } else {
            history.filter { it.text.contains(query, ignoreCase = true) }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearch(query: String) {
        searchQuery.value = query
    }

    fun delete(item: CopiedTextEntity) {
        scope.launch(Dispatchers.IO) {
            repository.deleteItem(item.id)
        }
    }
    
    fun clearHistory() {
        scope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }
}
