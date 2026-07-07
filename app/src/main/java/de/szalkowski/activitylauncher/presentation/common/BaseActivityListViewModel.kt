package de.szalkowski.activitylauncher.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseActivityListViewModel(
    private val getFlow: () -> Flow<List<ShortcutRequest>>,
    private val onRemoveItem: suspend (ShortcutRequest) -> Unit,
) : ViewModel() {

    private val _items = MutableStateFlow<List<ShortcutRequest>>(emptyList())
    val items: StateFlow<List<ShortcutRequest>> = _items.asStateFlow()

    private var dispatcher: CoroutineDispatcher = Dispatchers.Main

    fun setDispatcher(dispatcher: CoroutineDispatcher) {
        this.dispatcher = dispatcher
    }

    init {
        viewModelScope.launch(dispatcher) {
            getFlow().collectLatest {
                _items.value = it
            }
        }
    }

    fun removeItem(item: ShortcutRequest) {
        viewModelScope.launch(dispatcher) {
            onRemoveItem(item)
        }
    }
}
